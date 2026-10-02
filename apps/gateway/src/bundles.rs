// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
//! Signed policy ingestion is isolated from the in-memory authorization path.
use crate::{
    digest,
    extraction::identifier,
    policy::{PolicyEvaluator, PortFuture},
    validation::{strict_json, Contracts},
};
use base64::{engine::general_purpose::URL_SAFE_NO_PAD, Engine};
use olo_toolgate_contracts::{
    BundleEffect, BundleHeader, BundlePayload, CompiledPolicy, Decision, DecisionReason,
    PolicyDecision, PolicyInput, SignedPolicyBundle,
};
use serde::{Deserialize, Serialize};
use std::{
    collections::BTreeMap,
    path::PathBuf,
    sync::{
        atomic::{AtomicU64, Ordering},
        Arc, RwLock,
    },
    time::{Duration, Instant},
};
use tokio::{io::AsyncReadExt, sync::watch};

const MAX_WIRE: usize = 1_500_100;

/// Trust inputs are administrator configuration, never accepted from a bundle.
#[derive(Clone, Deserialize, Serialize)]
#[serde(rename_all = "camelCase", deny_unknown_fields)]
pub struct BundleSourceConfig {
    pub url: String,
    pub tenant_id: String,
    pub issuer: String,
    pub audience: String,
    pub keyring_path: PathBuf,
    pub token_path: PathBuf,
    pub minimum_sequence: u64,
    pub max_grace_ms: u64,
    pub poll_interval_ms: u64,
    pub fetch_timeout_ms: u64,
    pub development_loopback_http: bool,
}
impl BundleSourceConfig {
    /// Only a fixed TLS destination, or explicitly selected literal loopback dev URL.
    pub fn validate(&self) -> Result<(), &'static str> {
        let url = reqwest::Url::parse(&self.url).map_err(|_| "invalid bundle URL")?;
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
        let loopback = matches!(url.host_str(), Some("127.0.0.1" | "[::1]"));
        if self.url.len() > 512
            || !url.username().is_empty()
            || url.password().is_some()
            || url.query().is_some()
            || url.fragment().is_some()
            || url.path() != "/api/control/v1/bundles/current"
            || !(url.scheme() == "https"
                || (url.scheme() == "http" && loopback && self.development_loopback_http))
            || unsafe_host
            || ![&self.tenant_id, &self.issuer, &self.audience]
                .iter()
                .all(|v| identifier(v))
            || self.keyring_path.as_os_str().is_empty()
            || self.token_path.as_os_str().is_empty()
            || self.keyring_path == self.token_path
            || self.minimum_sequence > 9_007_199_254_740_991
            || self.max_grace_ms > 300_000
            || !(100..=60_000).contains(&self.poll_interval_ms)
            || !(100..=30_000).contains(&self.fetch_timeout_ms)
        {
            return Err("invalid signed bundle source configuration");
        }
        Ok(())
    }
}

/// Only policy-domain RSA public components. No key URLs or private material.
#[derive(Deserialize)]
#[serde(rename_all = "camelCase", deny_unknown_fields)]
pub struct BundleKey {
    pub key_id: String,
    pub modulus: String,
    pub exponent: String,
}
#[derive(Deserialize)]
#[serde(deny_unknown_fields)]
pub struct BundleKeyring {
    pub keys: Vec<BundleKey>,
}

/// RSA verification and canonical schema validation occur before acquiring a swap lock.
pub struct BundleVerifier {
    config: BundleSourceConfig,
    keys: BTreeMap<String, (Vec<u8>, Vec<u8>)>,
    contracts: Contracts,
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
impl BundleVerifier {
    pub fn new(config: BundleSourceConfig, keyring: BundleKeyring) -> Result<Self, &'static str> {
        config.validate()?;
        if keyring.keys.is_empty() || keyring.keys.len() > 16 {
            return Err("invalid bundle key count");
        }
        let mut keys = BTreeMap::new();
        for key in keyring.keys {
            let n = decode(&key.modulus)?;
            let e = decode(&key.exponent)?;
            if !identifier(&key.key_id)
                || ![256, 384, 512].contains(&n.len())
                || n[0] < 128
                || n[n.len() - 1] & 1 == 0
                || e != [1, 0, 1]
                || keys.values().any(|(old, _)| old == &n)
                || keys.insert(key.key_id, (n, e)).is_some()
            {
                return Err("invalid or duplicated bundle key");
            }
        }
        Ok(Self {
            config,
            keys,
            contracts: Contracts::new()?,
        })
    }
    /// Verify exact JWS bytes, strict header, hash, schema, trust and time claims.
    fn verify(
        &self,
        bytes: &[u8],
        now: u64,
        monotonic_now: Instant,
    ) -> Result<Snapshot, &'static str> {
        if bytes.len() > MAX_WIRE {
            return Err("bundle too large");
        }
        let value = strict_json(bytes).map_err(|_| "invalid bundle JSON")?;
        if !self.contracts.valid("SignedPolicyBundle", &value) {
            return Err("invalid bundle envelope");
        }
        let envelope: SignedPolicyBundle =
            serde_json::from_value(value).map_err(|_| "invalid bundle envelope")?;
        let parts: Vec<_> = envelope.jws.split('.').collect();
        if parts.len() != 3 || parts[0].len() > 1024 || parts[2].len() > 684 {
            return Err("invalid JWS structure");
        }
        let header = strict_json(&decode(parts[0])?).map_err(|_| "invalid protected header")?;
        if !self.contracts.valid("BundleHeader", &header) {
            return Err("unsupported protected header");
        }
        let header: BundleHeader =
            serde_json::from_value(header).map_err(|_| "invalid protected header")?;
        let (n, e) = self.keys.get(&header.kid).ok_or("unknown bundle key")?;
        ring::signature::RsaPublicKeyComponents {
            n: n.as_slice(),
            e: e.as_slice(),
        }
        .verify(
            &ring::signature::RSA_PKCS1_2048_8192_SHA256,
            format!("{}.{}", parts[0], parts[1]).as_bytes(),
            &decode(parts[2])?,
        )
        .map_err(|_| "bundle signature rejected")?;
        let payload = strict_json(&decode(parts[1])?).map_err(|_| "invalid signed payload")?;
        if !self.contracts.valid("BundlePayload", &payload) {
            return Err("unsupported bundle payload");
        }
        let payload: BundlePayload =
            serde_json::from_value(payload).map_err(|_| "invalid signed payload")?;
        if payload.issuer != self.config.issuer
            || payload.audience != self.config.audience
            || payload.tenant_id != self.config.tenant_id
            || payload.sequence < self.config.minimum_sequence
            || payload.version != format!("1.0.{}", payload.sequence)
            || payload.issued_at_unix_ms > now
            || payload.expires_at_unix_ms <= now
            || payload.expires_at_unix_ms <= payload.issued_at_unix_ms
            || payload.expires_at_unix_ms - payload.issued_at_unix_ms > 86_400_000
            || payload.rollback_of.is_some_and(|v| v >= payload.sequence)
        {
            return Err("bundle trust, version or freshness rejected");
        }
        let policy_bytes = decode(&payload.policy)?;
        if policy_bytes.len() > 786_432 || digest(&policy_bytes) != payload.policy_sha256 {
            return Err("bundle hash rejected");
        }
        let policy = strict_json(&policy_bytes).map_err(|_| "invalid compiled policy")?;
        if !self.contracts.valid("CompiledPolicy", &policy) {
            return Err("unsupported compiled policy");
        }
        let policy: CompiledPolicy =
            serde_json::from_value(policy).map_err(|_| "invalid compiled policy")?;
        let mut policy_ids = std::collections::BTreeSet::new();
        if policy
            .rules
            .iter()
            .any(|r| !policy_ids.insert(r.policy_id.clone()))
        {
            return Err("duplicate policy identity");
        }
        let deadline = payload
            .expires_at_unix_ms
            .checked_add(payload.grace_ms.min(self.config.max_grace_ms))
            .ok_or("invalid deadline")?;
        Ok(Snapshot {
            monotonic_expiry: monotonic_now
                + Duration::from_millis(payload.expires_at_unix_ms - now),
            monotonic_deadline: monotonic_now + Duration::from_millis(deadline - now),
            payload,
            policy,
            deadline,
            wire_digest: digest(envelope.jws.as_bytes()),
        })
    }
}

struct Snapshot {
    payload: BundlePayload,
    policy: CompiledPolicy,
    deadline: u64,
    wire_digest: String,
    monotonic_expiry: Instant,
    monotonic_deadline: Instant,
}

/// EMPTY/FRESH/GRACE/EXPIRED state; rejected updates cannot modify last-known-good.
pub struct VerifiedPolicy {
    verifier: BundleVerifier,
    current: RwLock<Option<Arc<Snapshot>>>,
    clock_high_water: AtomicU64,
    monotonic_clock: Arc<dyn Fn() -> Instant + Send + Sync>,
    accepted: AtomicU64,
    rejected: AtomicU64,
    fetch_failed: AtomicU64,
}
impl VerifiedPolicy {
    pub fn new(verifier: BundleVerifier) -> Self {
        Self::with_monotonic_clock(verifier, Arc::new(Instant::now))
    }
    /// Inject a monotonic clock for deterministic expiry/concurrency tests.
    pub fn with_monotonic_clock(
        verifier: BundleVerifier,
        monotonic_clock: Arc<dyn Fn() -> Instant + Send + Sync>,
    ) -> Self {
        Self {
            verifier,
            monotonic_clock,
            current: RwLock::new(None),
            clock_high_water: AtomicU64::new(0),
            accepted: AtomicU64::new(0),
            rejected: AtomicU64::new(0),
            fetch_failed: AtomicU64::new(0),
        }
    }
    fn effective_time(&self, now: u64) -> u64 {
        // Concurrent requests may carry slightly older ingress timestamps. Use the
        // high-water mark without treating scheduling as a wall-clock failure.
        // Independent monotonic deadlines also prevent a clock rollback from
        // extending validity before the wall clock reaches expiry.
        now.max(self.clock_high_water.fetch_max(now, Ordering::AcqRel))
    }
    /// Fully construct before the critical section; equal versions require identical signed bytes.
    pub fn adopt(&self, bytes: &[u8], now: u64) -> Result<bool, &'static str> {
        let result = (|| {
            let now = self.effective_time(now);
            let candidate = Arc::new(self.verifier.verify(bytes, now, (self.monotonic_clock)())?);
            let mut current = self
                .current
                .write()
                .map_err(|_| "policy state unavailable")?;
            if let Some(old) = current.as_ref() {
                if candidate.payload.sequence < old.payload.sequence {
                    return Err("stale bundle");
                }
                if candidate.payload.sequence == old.payload.sequence {
                    if candidate.wire_digest == old.wire_digest {
                        return Ok(false);
                    }
                    return Err("equivocating bundle version");
                }
            }
            tracing::info!(service="gateway", event="bundle_adopted", sequence=candidate.payload.sequence,
                rollback=candidate.payload.rollback_of.is_some(), policy_sha256=%candidate.payload.policy_sha256);
            *current = Some(candidate);
            self.accepted.fetch_add(1, Ordering::Relaxed);
            Ok(true)
        })();
        if result.is_err() {
            self.rejected.fetch_add(1, Ordering::Relaxed);
        }
        result
    }
    pub fn decide(&self, input: &PolicyInput, now: u64) -> PolicyDecision {
        let snapshot = self.current.read().ok().and_then(|v| v.clone());
        let unavailable = || PolicyDecision {
            decision: Decision::Block,
            reason: DecisionReason::PolicyUnavailable,
            policy_version: snapshot
                .as_ref()
                .map_or_else(|| "1.0.0".into(), |s| s.payload.version.clone()),
            request_id: input.context.request_id.clone(),
        };
        let now = self.effective_time(now);
        let Some(snapshot) = snapshot.as_ref().filter(|s| {
            now < s.deadline
                && (self.monotonic_clock)() < s.monotonic_deadline
                && now >= s.payload.issued_at_unix_ms
        }) else {
            return unavailable();
        };
        let mut decision = Decision::Block;
        let mut reason = DecisionReason::NoMatch;
        if input.context.tenant_id == snapshot.payload.tenant_id {
            for r in &snapshot.policy.rules {
                if r.tool_id != input.tool_id
                    || r.action != input.action
                    || r.resource != input.resource
                    || (!r.user_ids.is_empty() && !r.user_ids.contains(&input.context.user_id))
                    || (!r.agent_ids.is_empty() && !r.agent_ids.contains(&input.context.agent_id))
                    || (!r.device_ids.is_empty()
                        && input
                            .context
                            .device_id
                            .as_ref()
                            .is_none_or(|d| !r.device_ids.contains(d)))
                {
                    continue;
                }
                reason = DecisionReason::Matched;
                if r.effect == BundleEffect::Block {
                    decision = Decision::Block;
                    break;
                }
                if (now >= snapshot.payload.expires_at_unix_ms
                    || (self.monotonic_clock)() >= snapshot.monotonic_expiry)
                    && !r.grace_allowed
                {
                    if decision != Decision::Allow {
                        reason = DecisionReason::PolicyUnavailable;
                    }
                    continue;
                }
                decision = Decision::Allow;
            }
        }
        PolicyDecision {
            decision,
            reason,
            policy_version: snapshot.payload.version.clone(),
            request_id: input.context.request_id.clone(),
        }
    }
}
impl PolicyEvaluator for VerifiedPolicy {
    fn evaluate<'a>(
        &'a self,
        input: &'a PolicyInput,
        now: u64,
    ) -> PortFuture<'a, Result<PolicyDecision, &'static str>> {
        Box::pin(async move { Ok(self.decide(input, now)) })
    }
    fn ready(&self, now: u64) -> bool {
        let now = self.effective_time(now);
        self.current.read().ok().is_some_and(|v| {
            v.as_ref().is_some_and(|s| {
                now < s.deadline
                    && (self.monotonic_clock)() < s.monotonic_deadline
                    && now >= s.payload.issued_at_unix_ms
            })
        })
    }
    fn decision_valid(&self, decision: &PolicyDecision, input: &PolicyInput, now: u64) -> bool {
        self.decide(input, now) == *decision
    }
    fn metrics(&self, now: u64) -> String {
        let current = self.current.read().ok().and_then(|v| v.clone());
        let now = self.effective_time(now);
        let state = current.as_ref().map_or(0, |s| {
            if now >= s.deadline || (self.monotonic_clock)() >= s.monotonic_deadline {
                3
            } else if now >= s.payload.expires_at_unix_ms
                || (self.monotonic_clock)() >= s.monotonic_expiry
            {
                2
            } else {
                1
            }
        });
        format!("toolgate_bundle_state {}\ntoolgate_bundle_sequence {}\ntoolgate_bundle_accepted_total {}\ntoolgate_bundle_rejected_total {}\ntoolgate_bundle_fetch_failed_total {}\n",
            state, current.as_ref().map_or(0, |s| s.payload.sequence), self.accepted.load(Ordering::Relaxed),
            self.rejected.load(Ordering::Relaxed), self.fetch_failed.load(Ordering::Relaxed))
    }
}

/// One bounded, non-overlapping poll loop per replica. No network work in evaluation.
pub async fn poll(
    policy: Arc<VerifiedPolicy>,
    mut shutdown: watch::Receiver<bool>,
) -> Result<(), &'static str> {
    let _ = rustls::crypto::ring::default_provider().install_default();
    let config = &policy.verifier.config;
    let client = reqwest::Client::builder()
        .no_proxy()
        .tls_sslkeylogfile(false)
        .redirect(reqwest::redirect::Policy::none())
        .timeout(Duration::from_millis(config.fetch_timeout_ms))
        .connect_timeout(Duration::from_millis(config.fetch_timeout_ms))
        .pool_max_idle_per_host(1)
        .build()
        .map_err(|_| "bundle HTTP client unavailable")?;
    let mut interval = tokio::time::interval(Duration::from_millis(config.poll_interval_ms));
    interval.set_missed_tick_behavior(tokio::time::MissedTickBehavior::Skip);
    loop {
        tokio::select! {
            biased;
            _ = shutdown.changed() => return Ok(()),
            _ = interval.tick() => {
                let fetched = tokio::time::timeout(Duration::from_millis(config.fetch_timeout_ms), fetch(&client, config)).await;
                match fetched {
                    Ok(Ok(bytes)) => {
                        if let Some(now) = crate::unix_ms() {
                            if let Err(reason) = policy.adopt(&bytes, now) { tracing::warn!(service="gateway", event="bundle_rejected", reason); }
                        }
                    },
                    _ => { policy.fetch_failed.fetch_add(1, Ordering::Relaxed); tracing::warn!(service="gateway", event="bundle_fetch_failed"); }
                }
            }
        }
    }
}
async fn fetch(
    client: &reqwest::Client,
    config: &BundleSourceConfig,
) -> Result<Vec<u8>, &'static str> {
    let file = tokio::fs::File::open(&config.token_path)
        .await
        .map_err(|_| "bundle credential unavailable")?;
    let mut token = Vec::new();
    file.take(16_385)
        .read_to_end(&mut token)
        .await
        .map_err(|_| "bundle credential unavailable")?;
    if token.len() > 16_384 {
        return Err("invalid bundle credential");
    }
    let token = std::str::from_utf8(&token)
        .map_err(|_| "invalid bundle credential")?
        .trim();
    if token.is_empty()
        || !token
            .bytes()
            .all(|b| b.is_ascii_alphanumeric() || b"._-".contains(&b))
    {
        return Err("invalid bundle credential");
    }
    let trace = digest(
        format!(
            "bundle-{}-{}",
            std::process::id(),
            crate::unix_ms().unwrap_or(0)
        )
        .as_bytes(),
    );
    let mut response = client
        .get(&config.url)
        .bearer_auth(token)
        .header("Accept", "application/json")
        .header(
            "traceparent",
            format!("00-{}-{}-00", &trace[..32], &trace[32..48]),
        )
        .send()
        .await
        .map_err(|_| "bundle fetch unavailable")?;
    if response.status() != reqwest::StatusCode::OK
        || response
            .content_length()
            .is_some_and(|v| v > MAX_WIRE as u64)
        || response
            .headers()
            .get("content-type")
            .and_then(|v| v.to_str().ok())
            .is_none_or(|v| v.split(';').next() != Some("application/json"))
    {
        return Err("bundle fetch rejected");
    }
    let mut bytes = Vec::new();
    while let Some(chunk) = response
        .chunk()
        .await
        .map_err(|_| "bundle fetch unavailable")?
    {
        if bytes.len() + chunk.len() > MAX_WIRE {
            return Err("bundle too large");
        }
        bytes.extend_from_slice(&chunk);
    }
    Ok(bytes)
}

#[cfg(test)]
mod fetch_tests {
    use super::*;
    use tokio::io::AsyncWriteExt;

    #[tokio::test]
    async fn redirects_media_status_and_streamed_size_are_rejected_without_credential_forwarding() {
        let file = std::env::temp_dir().join(format!("toolgate-fetch-test-{}", std::process::id()));
        tokio::fs::write(&file, b"test.only.token").await.unwrap();
        let listener = tokio::net::TcpListener::bind("127.0.0.1:0").await.unwrap();
        let url = format!(
            "http://{}/api/control/v1/bundles/current",
            listener.local_addr().unwrap()
        );
        let config = BundleSourceConfig {
            url,
            tenant_id: "test".into(),
            issuer: "control".into(),
            audience: "gateway".into(),
            keyring_path: "public.json".into(),
            token_path: file.clone(),
            minimum_sequence: 0,
            max_grace_ms: 0,
            poll_interval_ms: 100,
            fetch_timeout_ms: 1000,
            development_loopback_http: true,
        };
        let _ = rustls::crypto::ring::default_provider().install_default();
        let client = reqwest::Client::builder()
            .no_proxy()
            .redirect(reqwest::redirect::Policy::none())
            .build()
            .unwrap();
        let responses = [
            b"HTTP/1.1 302 Found\r\nLocation: http://127.0.0.1:1/evil\r\nContent-Length: 0\r\n\r\n".to_vec(),
            b"HTTP/1.1 200 OK\r\nContent-Type: text/plain\r\nContent-Length: 2\r\n\r\n{}".to_vec(),
            b"HTTP/1.1 401 Unauthorized\r\nContent-Type: application/json\r\nContent-Length: 2\r\n\r\n{}".to_vec(),
            b"HTTP/1.1 200 OK\r\nContent-Type: application/json\r\nContent-Length: 1500101\r\n\r\n".to_vec(),
            [b"HTTP/1.1 200 OK\r\nContent-Type: application/json\r\nConnection: close\r\n\r\n".as_slice(), &vec![b'x'; MAX_WIRE + 1]].concat(),
        ];
        for response in responses {
            let serve = async {
                let (mut stream, _) = listener.accept().await.unwrap();
                let mut headers = vec![0; 8192];
                let count = stream.read(&mut headers).await.unwrap();
                let headers = String::from_utf8_lossy(&headers[..count]);
                assert!(headers
                    .to_lowercase()
                    .contains("authorization: bearer test.only.token"));
                // The client may close early when enforcing declared/streamed bounds.
                let _ = stream.write_all(&response).await;
            };
            let (result, ()) = tokio::join!(fetch(&client, &config), serve);
            assert!(result.is_err());
        }
        tokio::fs::write(&file, b"token\r\ninjected: header")
            .await
            .unwrap();
        assert!(fetch(&client, &config).await.is_err());
        tokio::fs::remove_file(file).await.unwrap();
    }
}
