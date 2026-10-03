// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
//! Bounded approval coordination and a separate exact-operation permit trust domain.
use crate::{
    bundles::BundleKeyring,
    extraction::identifier,
    policy::PortFuture,
    validation::{strict_json, Contracts},
};
use base64::{
    engine::general_purpose::{STANDARD, URL_SAFE_NO_PAD},
    Engine,
};
use olo_toolgate_contracts::{
    ApprovalPermitUse, ApprovalResolution, ApprovalSubmission, ExecutionPermitClaims,
    ExecutionPermitHeader, PolicyInput, SignedExecutionPermit,
};
use ring::{rand::SystemRandom, signature::KeyPair};
use serde::{Deserialize, Serialize};
use std::{
    io::Read,
    path::{Path, PathBuf},
    sync::{
        atomic::{AtomicU64, Ordering},
        Arc,
    },
    time::{Duration, Instant},
};
use tokio::io::AsyncReadExt;

const MAX_RESPONSE: usize = 32_768;

/// Administrator-selected origin and external credentials; disabled when absent.
#[derive(Clone, Deserialize, Serialize)]
#[serde(rename_all = "camelCase", deny_unknown_fields)]
pub struct ApprovalConfig {
    pub url: String,
    pub token_path: PathBuf,
    pub private_key_path: PathBuf,
    pub key_id: String,
    pub issuer: String,
    pub audience: String,
    pub request_timeout_ms: u64,
    pub permit_lifetime_ms: u64,
    pub development_loopback_http: bool,
}
impl ApprovalConfig {
    /// A fixed origin only. Endpoint paths are compiled into the adapter.
    pub fn validate(&self) -> Result<(), &'static str> {
        let url = reqwest::Url::parse(&self.url).map_err(|_| "invalid approval origin")?;
        let loopback = matches!(url.host_str(), Some("127.0.0.1" | "[::1]"));
        let unsafe_host = url.host_str().is_none_or(|host| {
            host == "metadata.google.internal"
                || host
                    .trim_matches(['[', ']'])
                    .parse::<std::net::IpAddr>()
                    .is_ok_and(|ip| match ip {
                        std::net::IpAddr::V4(ip) => {
                            ip.is_link_local()
                                || ip.is_multicast()
                                || ip.is_unspecified()
                                || ip.is_broadcast()
                        }
                        std::net::IpAddr::V6(ip) => {
                            ip.is_unicast_link_local() || ip.is_multicast() || ip.is_unspecified()
                        }
                    })
        });
        if self.url.len() > 512
            || !url.username().is_empty()
            || url.password().is_some()
            || url.query().is_some()
            || url.fragment().is_some()
            || url.path() != "/"
            || !(url.scheme() == "https"
                || (url.scheme() == "http" && loopback && self.development_loopback_http))
            || unsafe_host
            || ![&self.key_id, &self.issuer, &self.audience]
                .iter()
                .all(|v| identifier(v))
            || self.token_path.as_os_str().is_empty()
            || self.private_key_path.as_os_str().is_empty()
            || self.token_path == self.private_key_path
            || !(10..=30_000).contains(&self.request_timeout_ms)
            || !(1..=10_000).contains(&self.permit_lifetime_ms)
        {
            return Err("invalid approval configuration");
        }
        Ok(())
    }
}

/// Control is authoritative on each grant and each permit use; no grant cache.
pub trait ApprovalCoordinator: Send + Sync {
    fn resolve<'a>(
        &'a self,
        submission: &'a ApprovalSubmission,
        trace_id: &'a str,
    ) -> PortFuture<'a, Result<ApprovalResolution, &'static str>>;
    /// Fixed cardinality counters; never identity/approval/permit labels.
    fn metrics(&self) -> String {
        String::new()
    }
    fn consume<'a>(
        &'a self,
        request: &'a ApprovalPermitUse,
        trace_id: &'a str,
    ) -> PortFuture<'a, Result<ApprovalResolution, &'static str>>;
}

/// HTTP client inherits neither proxies nor redirect destinations from the host.
pub struct HttpApprovalCoordinator {
    config: ApprovalConfig,
    client: reqwest::Client,
    contracts: Contracts,
    resolve_calls: AtomicU64,
    consume_calls: AtomicU64,
    failed: AtomicU64,
}
impl HttpApprovalCoordinator {
    pub fn new(config: ApprovalConfig) -> Result<Self, &'static str> {
        config.validate()?;
        let _ = rustls::crypto::ring::default_provider().install_default();
        let client = reqwest::Client::builder()
            .no_proxy()
            .tls_sslkeylogfile(false)
            .redirect(reqwest::redirect::Policy::none())
            .timeout(Duration::from_millis(config.request_timeout_ms))
            .connect_timeout(Duration::from_millis(config.request_timeout_ms))
            .pool_max_idle_per_host(1)
            .build()
            .map_err(|_| "approval client unavailable")?;
        Ok(Self {
            config,
            client,
            contracts: Contracts::new()?,
            resolve_calls: AtomicU64::new(0),
            consume_calls: AtomicU64::new(0),
            failed: AtomicU64::new(0),
        })
    }
    async fn post<T: Serialize>(
        &self,
        path: &'static str,
        body: &T,
        trace_id: &str,
    ) -> Result<ApprovalResolution, &'static str> {
        tokio::time::timeout(
            Duration::from_millis(self.config.request_timeout_ms),
            async {
                let file = tokio::fs::File::open(&self.config.token_path)
                    .await
                    .map_err(|_| "approval credential unavailable")?;
                let mut token = Vec::new();
                file.take(16_385)
                    .read_to_end(&mut token)
                    .await
                    .map_err(|_| "approval credential unavailable")?;
                if token.len() > 16_384 {
                    return Err("invalid approval credential");
                }
                let token = std::str::from_utf8(&token)
                    .map_err(|_| "invalid approval credential")?
                    .trim();
                if token.is_empty()
                    || !token
                        .bytes()
                        .all(|b| b.is_ascii_alphanumeric() || b"._-".contains(&b))
                {
                    return Err("invalid approval credential");
                }
                let url = reqwest::Url::parse(&self.config.url)
                    .map_err(|_| "invalid approval origin")?
                    .join(path)
                    .map_err(|_| "invalid approval endpoint")?;
                let trace =
                    if trace_id.len() == 32 && trace_id.bytes().all(|b| b.is_ascii_hexdigit()) {
                        trace_id.to_owned()
                    } else {
                        crate::digest(trace_id.as_bytes())[..32].to_owned()
                    };
                let span = crate::digest(
                    format!("approval-{trace}-{}", crate::unix_ms().unwrap_or(0)).as_bytes(),
                );
                let mut response = self
                    .client
                    .post(url)
                    .bearer_auth(token)
                    .header("Accept", "application/json")
                    .header("traceparent", format!("00-{trace}-{}-00", &span[..16]))
                    .header("Content-Type", "application/json")
                    .body(serde_json::to_vec(body).map_err(|_| "invalid approval request")?)
                    .send()
                    .await
                    .map_err(|_| "approval unavailable")?;
                if response.status() != reqwest::StatusCode::OK
                    || response
                        .content_length()
                        .is_some_and(|v| v > MAX_RESPONSE as u64)
                    || response
                        .headers()
                        .get("content-type")
                        .and_then(|v| v.to_str().ok())
                        .is_none_or(|v| {
                            v.split(';').next().map(str::trim) != Some("application/json")
                        })
                {
                    return Err("approval response rejected");
                }
                let mut bytes = Vec::new();
                while let Some(chunk) =
                    response.chunk().await.map_err(|_| "approval unavailable")?
                {
                    if bytes.len() + chunk.len() > MAX_RESPONSE {
                        return Err("approval response too large");
                    }
                    bytes.extend_from_slice(&chunk);
                }
                let value = strict_json(&bytes).map_err(|_| "invalid approval response")?;
                if !self.contracts.valid("ApprovalResolution", &value) {
                    return Err("invalid approval response");
                }
                serde_json::from_value(value).map_err(|_| "invalid approval response")
            },
        )
        .await
        .map_err(|_| "approval timeout")?
    }
}
impl ApprovalCoordinator for HttpApprovalCoordinator {
    fn resolve<'a>(
        &'a self,
        submission: &'a ApprovalSubmission,
        trace_id: &'a str,
    ) -> PortFuture<'a, Result<ApprovalResolution, &'static str>> {
        Box::pin(async move {
            self.resolve_calls.fetch_add(1, Ordering::Relaxed);
            let result = self
                .post("/api/control/v1/approvals/resolve", submission, trace_id)
                .await;
            if result.is_err() {
                self.failed.fetch_add(1, Ordering::Relaxed);
            }
            result
        })
    }
    fn consume<'a>(
        &'a self,
        request: &'a ApprovalPermitUse,
        trace_id: &'a str,
    ) -> PortFuture<'a, Result<ApprovalResolution, &'static str>> {
        Box::pin(async move {
            self.consume_calls.fetch_add(1, Ordering::Relaxed);
            let result = self
                .post(
                    "/api/control/v1/approvals/permits/consume",
                    request,
                    trace_id,
                )
                .await;
            if result.is_err() {
                self.failed.fetch_add(1, Ordering::Relaxed);
            }
            result
        })
    }
    fn metrics(&self) -> String {
        format!("toolgate_approval_resolve_total {}\ntoolgate_approval_consume_total {}\ntoolgate_approval_failed_total {}\n",
            self.resolve_calls.load(Ordering::Relaxed), self.consume_calls.load(Ordering::Relaxed), self.failed.load(Ordering::Relaxed))
    }
}

fn decode(value: &str) -> Result<Vec<u8>, &'static str> {
    let bytes = URL_SAFE_NO_PAD
        .decode(value)
        .map_err(|_| "invalid base64url")?;
    if URL_SAFE_NO_PAD.encode(&bytes) != value {
        return Err("noncanonical base64url");
    }
    Ok(bytes)
}

/// No Debug implementation and no key bytes in logging or serialized config.
pub struct PermitSigner {
    config: ApprovalConfig,
    key: ring::signature::RsaKeyPair,
    modulus: Vec<u8>,
    exponent: Vec<u8>,
    contracts: Contracts,
    clock_high_water: AtomicU64,
    boot_wall: u64,
    boot_instant: Instant,
    monotonic_clock: Arc<dyn Fn() -> Instant + Send + Sync>,
}

// ring returns PKCS#1 public DER. Parse only the canonical integer sequence needed
// for domain separation and public verification; private DER is handled by ring.
fn der_item<'a>(input: &mut &'a [u8], tag: u8) -> Result<&'a [u8], &'static str> {
    if input.len() < 2 || input[0] != tag {
        return Err("invalid RSA public key");
    }
    let first = input[1];
    let (length, offset) = if first < 128 {
        (usize::from(first), 2)
    } else {
        let count = usize::from(first & 127);
        if !(1..=2).contains(&count) || input.len() < 2 + count || input[2] == 0 {
            return Err("invalid RSA public key");
        }
        let length = input[2..2 + count]
            .iter()
            .fold(0usize, |n, v| n * 256 + usize::from(*v));
        if length < 128 {
            return Err("invalid RSA public key");
        }
        (length, 2 + count)
    };
    if input.len() < offset + length {
        return Err("invalid RSA public key");
    }
    let value = &input[offset..offset + length];
    *input = &input[offset + length..];
    Ok(value)
}
fn public_components(bytes: &[u8]) -> Result<(Vec<u8>, Vec<u8>), &'static str> {
    let mut outer = bytes;
    let mut sequence = der_item(&mut outer, 0x30)?;
    if !outer.is_empty() {
        return Err("invalid RSA public key");
    }
    let modulus = der_item(&mut sequence, 0x02)?;
    let exponent = der_item(&mut sequence, 0x02)?;
    if !sequence.is_empty()
        || modulus.len() < 2
        || modulus[0] != 0
        || modulus[1] < 128
        || ![257, 385, 513].contains(&modulus.len())
        || modulus[modulus.len() - 1] & 1 == 0
        || exponent != [1, 0, 1]
    {
        return Err("unsupported permit RSA parameters");
    }
    Ok((modulus[1..].to_vec(), exponent.to_vec()))
}
fn read_pkcs8(path: &Path) -> Result<Vec<u8>, &'static str> {
    let mut file = std::fs::File::open(path)
        .map_err(|_| "permit signing key unavailable")?
        .take(16_385);
    let mut bytes = Vec::new();
    file.read_to_end(&mut bytes)
        .map_err(|_| "permit signing key unavailable")?;
    if bytes.len() > 16_384 {
        return Err("permit signing key too large");
    }
    let pem = std::str::from_utf8(&bytes)
        .map_err(|_| "invalid permit PKCS8 PEM")?
        .trim();
    // Only whitespace is removable. Other characters remain for strict base64 decoding.
    let encoded: String = pem
        .strip_prefix(concat!("-----BEGIN ", "PRIVATE KEY-----"))
        .and_then(|v| v.strip_suffix(concat!("-----END ", "PRIVATE KEY-----")))
        .ok_or("invalid permit PKCS8 PEM")?
        .chars()
        .filter(|c| !c.is_ascii_whitespace())
        .collect();
    STANDARD
        .decode(encoded)
        .map_err(|_| "invalid permit PKCS8 PEM")
}

impl PermitSigner {
    pub fn new(config: ApprovalConfig, forbidden: &BundleKeyring) -> Result<Self, &'static str> {
        let der = read_pkcs8(&config.private_key_path)?;
        Self::from_pkcs8(config, &der, forbidden)
    }
    /// Test/bootstrap seam accepts PKCS8 bytes; all parameters and key domains checked.
    pub fn from_pkcs8(
        config: ApprovalConfig,
        der: &[u8],
        forbidden: &BundleKeyring,
    ) -> Result<Self, &'static str> {
        Self::with_clock(
            config,
            der,
            forbidden,
            crate::unix_ms().ok_or("permit clock unavailable")?,
            Arc::new(Instant::now),
        )
    }
    /// Inject clocks for deterministic lifetime/regression tests. Time projection
    /// is process state, never a permission/grant cache or authoritative replay store.
    pub fn with_clock(
        config: ApprovalConfig,
        der: &[u8],
        forbidden: &BundleKeyring,
        boot_wall: u64,
        monotonic_clock: Arc<dyn Fn() -> Instant + Send + Sync>,
    ) -> Result<Self, &'static str> {
        config.validate()?;
        let key = ring::signature::RsaKeyPair::from_pkcs8(der)
            .map_err(|_| "invalid permit signing key")?;
        let (modulus, exponent) = public_components(key.public_key().as_ref())?;
        if forbidden
            .keys
            .iter()
            .any(|k| k.key_id == config.key_id || decode(&k.modulus).is_ok_and(|n| n == modulus))
        {
            return Err("permit signing key reused across trust domains");
        }
        Ok(Self {
            config,
            key,
            modulus,
            exponent,
            contracts: Contracts::new()?,
            clock_high_water: AtomicU64::new(boot_wall),
            boot_wall,
            boot_instant: monotonic_clock(),
            monotonic_clock,
        })
    }
    fn effective_time(&self, now: u64) -> Result<u64, &'static str> {
        let elapsed = (self.monotonic_clock)()
            .checked_duration_since(self.boot_instant)
            .ok_or("permit monotonic clock unavailable")?;
        let projected = self
            .boot_wall
            .checked_add(u64::try_from(elapsed.as_millis()).map_err(|_| "permit clock overflow")?)
            .ok_or("permit clock overflow")?;
        let current = now.max(projected);
        Ok(current.max(self.clock_high_water.fetch_max(current, Ordering::AcqRel)))
    }
    /// Public components can be provisioned to future clients without private material.
    pub fn public_key(&self) -> serde_json::Value {
        serde_json::json!({"keyId": self.config.key_id, "modulus": URL_SAFE_NO_PAD.encode(&self.modulus), "exponent": URL_SAFE_NO_PAD.encode(&self.exponent)})
    }
    pub fn issue(
        &self,
        input: &PolicyInput,
        resolution: &ApprovalResolution,
        now: u64,
        deadline: u64,
    ) -> Result<SignedExecutionPermit, &'static str> {
        let now = self.effective_time(now)?;
        let expires = resolution
            .permit_expires_at_unix_ms
            .ok_or("approval lease unavailable")?
            .min(deadline)
            .min(
                now.checked_add(self.config.permit_lifetime_ms)
                    .ok_or("permit deadline invalid")?,
            );
        if expires <= now || resolution.input != *input {
            return Err("approval lease expired or mismatched");
        }
        let claims = ExecutionPermitClaims {
            permit_version: 1,
            issuer: self.config.issuer.clone(),
            audience: self.config.audience.clone(),
            tenant_id: input.context.tenant_id.clone(),
            user_id: input.context.user_id.clone(),
            agent_id: input.context.agent_id.clone(),
            device_id: input.context.device_id.clone(),
            tool_id: input.tool_id.clone(),
            action: input.action.clone(),
            resource: input.resource.clone(),
            arguments_digest: input.arguments_digest.clone(),
            request_id: input.context.request_id.clone(),
            policy_version: resolution.policy_version.clone(),
            approval_id: resolution.approval_id.clone(),
            jti: resolution
                .permit_id
                .clone()
                .ok_or("approval lease unavailable")?,
            issued_at_unix_ms: now,
            expires_at_unix_ms: expires,
        };
        let value = serde_json::to_value(&claims).map_err(|_| "permit claims invalid")?;
        if !self.contracts.valid("ExecutionPermitClaims", &value) {
            return Err("permit claims invalid");
        }
        let header = ExecutionPermitHeader {
            alg: "RS256".into(),
            typ: "toolgate-execution-permit+jws".into(),
            kid: self.config.key_id.clone(),
        };
        let message = format!(
            "{}.{}",
            URL_SAFE_NO_PAD
                .encode(serde_json::to_vec(&header).map_err(|_| "permit header invalid")?),
            URL_SAFE_NO_PAD
                .encode(serde_json::to_vec(&value).map_err(|_| "permit claims invalid")?)
        );
        let mut signature = vec![0; self.key.public().modulus_len()];
        self.key
            .sign(
                &ring::signature::RSA_PKCS1_SHA256,
                &SystemRandom::new(),
                message.as_bytes(),
                &mut signature,
            )
            .map_err(|_| "permit signing unavailable")?;
        Ok(SignedExecutionPermit {
            jws: format!("{message}.{}", URL_SAFE_NO_PAD.encode(signature)),
        })
    }
    /// Exact bytes verify before JSON parsing. Request IDs bind the original issuance;
    /// consume has a fresh authenticated ingress correlation, never a caller ID.
    pub fn verify(
        &self,
        permit: &SignedExecutionPermit,
        input: &PolicyInput,
        now: u64,
    ) -> Result<ExecutionPermitClaims, &'static str> {
        let now = self.effective_time(now)?;
        if !self.contracts.valid(
            "SignedExecutionPermit",
            &serde_json::to_value(permit).map_err(|_| "invalid permit")?,
        ) {
            return Err("invalid permit envelope");
        }
        let parts: Vec<_> = permit.jws.split('.').collect();
        if parts.len() != 3 || parts[0].len() > 1024 || parts[2].len() > 684 {
            return Err("invalid permit JWS");
        }
        let header = strict_json(&decode(parts[0])?).map_err(|_| "invalid permit header")?;
        if !self.contracts.valid("ExecutionPermitHeader", &header) {
            return Err("unsupported permit header");
        }
        let header: ExecutionPermitHeader =
            serde_json::from_value(header).map_err(|_| "invalid permit header")?;
        if header.kid != self.config.key_id {
            return Err("unknown permit key");
        }
        ring::signature::RsaPublicKeyComponents {
            n: self.modulus.as_slice(),
            e: self.exponent.as_slice(),
        }
        .verify(
            &ring::signature::RSA_PKCS1_2048_8192_SHA256,
            format!("{}.{}", parts[0], parts[1]).as_bytes(),
            &decode(parts[2])?,
        )
        .map_err(|_| "permit signature rejected")?;
        let value = strict_json(&decode(parts[1])?).map_err(|_| "invalid permit claims")?;
        if !self.contracts.valid("ExecutionPermitClaims", &value) {
            return Err("unsupported permit claims");
        }
        let claims: ExecutionPermitClaims =
            serde_json::from_value(value).map_err(|_| "invalid permit claims")?;
        if claims.issuer != self.config.issuer
            || claims.audience != self.config.audience
            || claims.tenant_id != input.context.tenant_id
            || claims.user_id != input.context.user_id
            || claims.agent_id != input.context.agent_id
            || claims.device_id != input.context.device_id
            || claims.tool_id != input.tool_id
            || claims.action != input.action
            || claims.resource != input.resource
            || claims.arguments_digest != input.arguments_digest
            || claims.issued_at_unix_ms > now
            || claims.expires_at_unix_ms <= now
            || claims.expires_at_unix_ms <= claims.issued_at_unix_ms
            || claims.expires_at_unix_ms - claims.issued_at_unix_ms > self.config.permit_lifetime_ms
        {
            return Err("permit binding, trust or expiry rejected");
        }
        Ok(claims)
    }
}
