// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
//! Administrator-configured HTTPS Gateway. ASK grants are consumed online before execution.
use crate::{
    builtins::{AuthorizationPort, Settings},
    contracts::Contracts,
    transport::Call,
    Failure, Result,
};
use olo_toolgate_contracts::*;
use std::{path::Path, sync::Arc, time::Duration};
pub fn secret(path: &Path) -> Result<String> {
    let bytes = crate::storage::read_owned(path, 4096, true)?;
    let value = String::from_utf8(bytes).map_err(|_| Failure::Validation)?;
    let value = value.trim();
    if value.is_empty() || value.len() > 2048 || !value.bytes().all(|b| b.is_ascii_graphic()) {
        return Err(Failure::Validation);
    }
    Ok(value.to_owned())
}
pub fn client(ca: Option<&Path>) -> Result<reqwest::Client> {
    let _ = rustls::crypto::ring::default_provider().install_default();
    let mut builder = reqwest::Client::builder()
        .https_only(true)
        .redirect(reqwest::redirect::Policy::none())
        .no_proxy()
        .tls_sslkeylogfile(false)
        .referer(false)
        .retry(reqwest::retry::never())
        .timeout(Duration::from_secs(5))
        .connect_timeout(Duration::from_secs(3))
        .pool_max_idle_per_host(1);
    if let Some(path) = ca {
        builder = builder.tls_certs_only([reqwest::Certificate::from_pem(
            &crate::storage::read_owned(path, 16384, false)?,
        )
        .map_err(|_| Failure::Validation)?]);
    }
    builder.build().map_err(|_| Failure::Unavailable)
}
pub(crate) fn public_address(address: std::net::IpAddr) -> bool {
    match address {
        std::net::IpAddr::V4(ip) => {
            let [a, b, c, _] = ip.octets();
            !(!(1..224).contains(&a)
                || ip.is_private()
                || ip.is_loopback()
                || ip.is_link_local()
                || a == 100 && (64..128).contains(&b)
                || a == 192 && (b == 0 || b == 88 && c == 99)
                || a == 198 && (b == 18 || b == 19 || b == 51 && c == 100)
                || a == 203 && b == 0 && c == 113)
        }
        std::net::IpAddr::V6(ip) => {
            let words = ip.segments();
            words[0] & 0xe000 == 0x2000
                && !(words[0] == 0x2001 && (words[1] < 0x200 || words[1] == 0xdb8))
                && words[0] != 0x2002
        }
    }
}
/// Resolve once for this request, reject every non-public answer, then pin sockets while retaining TLS hostname verification.
pub(crate) async fn public_client(host: &str) -> Result<reqwest::Client> {
    let addresses: Vec<_> =
        tokio::time::timeout(Duration::from_secs(3), tokio::net::lookup_host((host, 443)))
            .await
            .map_err(|_| Failure::Unavailable)?
            .map_err(|_| Failure::Unavailable)?
            .collect();
    if addresses.is_empty()
        || addresses.len() > 32
        || addresses.iter().any(|a| !public_address(a.ip()))
    {
        return Err(Failure::Unauthorized);
    }
    reqwest::Client::builder()
        .https_only(true)
        .no_proxy()
        .redirect(reqwest::redirect::Policy::none())
        .referer(false)
        .tls_sslkeylogfile(false)
        .retry(reqwest::retry::never())
        .timeout(Duration::from_secs(5))
        .connect_timeout(Duration::from_secs(3))
        .resolve_to_addrs(host, &addresses)
        .build()
        .map_err(|_| Failure::Unavailable)
}
pub async fn body(mut response: reqwest::Response) -> Result<Vec<u8>> {
    if response.status() != 200 {
        return Err(if matches!(response.status().as_u16(), 401 | 403) {
            Failure::Unauthorized
        } else {
            Failure::Unavailable
        });
    }
    if response.content_length().is_some_and(|n| n > 131072)
        || response
            .headers()
            .get("content-type")
            .and_then(|v| v.to_str().ok())
            .is_none_or(|v| v.split(';').next() != Some("application/json"))
    {
        return Err(Failure::Validation);
    }
    let mut bytes = Vec::new();
    while let Some(chunk) = response.chunk().await.map_err(|_| Failure::Unavailable)? {
        if bytes.len() + chunk.len() > 131072 {
            return Err(Failure::Validation);
        }
        bytes.extend_from_slice(&chunk);
    }
    Ok(bytes)
}
pub struct HttpsGateway {
    client: reqwest::Client,
    origin: String,
    token: String,
    contracts: Arc<Contracts>,
    config: crate::config::Config,
    settings: Settings,
    executing: tokio::sync::Mutex<Option<EnterpriseInvocation>>,
}
impl HttpsGateway {
    pub async fn catalog(&self, agent: Option<&str>) -> Result<LocalToolCatalog> {
        self.device()?;
        let query = agent
            .map(|id| serde_json::json!({"agentId":id}))
            .unwrap_or_else(|| serde_json::json!({}));
        let response = self
            .client
            .post(format!("{}/access/catalog", self.origin))
            .bearer_auth(&self.token)
            .header("Accept", "application/json")
            .header("Content-Type", "application/json")
            .body(serde_json::to_vec(&query).map_err(|_| Failure::Validation)?)
            .send()
            .await
            .map_err(|_| Failure::Unavailable)?;
        self.contracts
            .decode("LocalToolCatalog", &body(response).await?)
    }
    pub fn new(
        settings: &Settings,
        config: &crate::config::Config,
        contracts: Arc<Contracts>,
    ) -> Result<Self> {
        settings.validate()?;
        Ok(Self {
            client: client(settings.gateway_ca_path.as_deref())?,
            origin: crate::config::origin(&settings.gateway_url)?,
            token: secret(&settings.gateway_token_path)?,
            contracts,
            config: config.clone(),
            settings: settings.clone(),
            executing: tokio::sync::Mutex::new(None),
        })
    }
    fn device(
        &self,
    ) -> Result<(
        DeviceIdentity,
        crate::transport::HttpsControl,
        crate::storage::ProtectedStore,
    )> {
        let store = crate::storage::ProtectedStore::open(self.config.state_directory.clone())?;
        if store.read("device-key")?.is_none() {
            return Err(Failure::Unauthorized);
        }
        let key = Arc::new(crate::identity::DeviceKey::load_or_create(&store)?);
        let bytes = store.read("journal.json")?.ok_or(Failure::Unauthorized)?;
        let journal: serde_json::Value =
            serde_json::from_slice(&bytes).map_err(|_| Failure::Validation)?;
        if journal["revoked"] != false {
            return Err(Failure::Unauthorized);
        }
        let identity: DeviceIdentity = serde_json::from_value(journal["identity"].clone())
            .map_err(|_| Failure::Unauthorized)?;
        let manifest: ClientDiscovery = serde_json::from_value(journal["manifest"].clone())
            .map_err(|_| Failure::Unauthorized)?;
        if crate::config::origin(&manifest.control_url)?
            != crate::config::origin(&self.config.server_url)?
            || identity.device_id != self.settings.device_id
        {
            return Err(Failure::Unauthorized);
        }
        crate::identity::verify_identity(
            &identity,
            &manifest,
            &key,
            &identity.device_id,
            crate::now(),
        )?;
        Ok((
            identity,
            crate::transport::HttpsControl::new(self.config.clone(), key, self.contracts.clone())?,
            store,
        ))
    }
    fn installed(&self, request: &AuthorizationRequest) -> Result<BuiltinToolInfo> {
        if let Some(execution) = &self.config.execution {
            if let Some(tool) = execution
                .tools
                .iter()
                .find(|t| t.tool_id == request.tool_id && t.action == request.action)
            {
                let runtime = execution
                    .runtimes
                    .iter()
                    .find(|r| r.id == tool.runtime_id)
                    .ok_or(Failure::Validation)?;
                return crate::authorization_profile::managed_info(tool, runtime);
            }
        }
        let catalog: serde_json::Value = serde_json::from_str(include_str!(
            "../../../packages/contracts/tools/builtins.json"
        ))
        .map_err(|_| Failure::Validation)?;
        let item = catalog["tools"]
            .as_array()
            .and_then(|v| {
                v.iter()
                    .find(|t| t["toolId"] == request.tool_id && t["action"] == request.action)
            })
            .ok_or(Failure::Unauthorized)?;
        let profile = self
            .settings
            .authorization_profiles
            .iter()
            .find(|p| p.tool.id == request.tool_id)
            .ok_or(Failure::Unauthorized)?;
        crate::authorization_profile::builtin_info(
            item,
            profile,
            &crate::authorization_profile::builtin_package_digest()?,
        )
    }
}
impl AuthorizationPort for HttpsGateway {
    fn secret(&self, name: String) -> Call<'_, String> {
        Box::pin(async move {
            use crate::transport::ControlPort;
            let executing = self.executing.lock().await;
            let invocation = executing.as_ref().ok_or(Failure::Unauthorized)?;
            let (identity, control, _store) = self.device()?;
            Ok(control
                .deliver_secret(
                    identity,
                    EnterpriseSecretDeliveryRequest {
                        invocation_id: invocation.id.clone(),
                        name,
                    },
                )
                .await?
                .value)
        })
    }
    fn authorize(&self, request: AuthorizationRequest) -> Call<'_, ()> {
        Box::pin(async move { self.authorize_bound(request).await.map(|_| ()) })
    }
    fn authorize_bound(&self, request: AuthorizationRequest) -> Call<'_, Option<u64>> {
        Box::pin(async move {
            use crate::transport::ControlPort;
            let mut executing = self.executing.lock().await;
            if executing.is_some() {
                return Err(Failure::Conflict);
            }
            let (identity, control, store) = self.device()?;
            if let Some(bytes) = store.read("effect-journal.json")? {
                let previous: EnterpriseInvocation =
                    self.contracts.decode("EnterpriseInvocation", &bytes)?;
                if matches!(
                    previous.state,
                    EnterpriseInvocationState::Reserved
                        | EnterpriseInvocationState::Executing
                        | EnterpriseInvocationState::OutcomeUnknown
                ) {
                    let recovered = control
                        .effect_outcome(identity.clone(), previous.id.clone())
                        .await?;
                    if !recoverable_outcome(&previous, &recovered) {
                        return Err(Failure::Conflict);
                    }
                    store.write(
                        "effect-journal.json",
                        &self.contracts.encode("EnterpriseInvocation", &recovered)?,
                    )?;
                }
            }
            let installed = self.installed(&request)?;
            let correlation = crate::identity::nonce()?;
            let response = self
                .client
                .post(format!("{}/access/invocations", self.origin))
                .bearer_auth(&self.token)
                .header("Accept", "application/json")
                .header("Content-Type", "application/json")
                .header("Idempotency-Key", &correlation)
                .body(self.contracts.encode("AuthorizationRequest", &request)?)
                .send()
                .await
                .map_err(|_| Failure::Unavailable)?;
            let outcome: EnterpriseAuthorizationOutcome = self
                .contracts
                .decode("EnterpriseAuthorizationOutcome", &body(response).await?)?;
            let reservation = outcome.reservation.ok_or(Failure::Unauthorized)?;
            let i = reservation.invocation;
            let e = &i.evaluation;
            if i != outcome.invocation
                || i.state != EnterpriseInvocationState::Reserved
                || e.context.device_id != identity.device_id
                || e.context.tenant_id != identity.tenant_id
                || e.tool_id != request.tool_id
                || e.action != request.action
                || e.arguments_digest
                    != crate::digest(
                        &serde_json::to_vec(&request.arguments).map_err(|_| Failure::Validation)?,
                    )
                || e.tool_digest != installed.tool_digest
                || e.package_digest != installed.package_digest
            {
                return Err(Failure::Unauthorized);
            }
            // Persist BEFORE consuming. An ambiguous transport failure cannot cause an effect retry.
            store.write(
                "effect-journal.json",
                &self.contracts.encode("EnterpriseInvocation", &i)?,
            )?;
            let next = control
                .consume_effect(
                    identity,
                    EnterprisePermitConsumption {
                        invocation_id: i.id.clone(),
                        permit: reservation.permit,
                        arguments_digest: e.arguments_digest.clone(),
                        resources: e.resources.clone(),
                        tool_digest: e.tool_digest.clone(),
                        package_digest: e.package_digest.clone(),
                    },
                )
                .await?;
            if next.id != i.id
                || next.evaluation != i.evaluation
                || next.state != EnterpriseInvocationState::Executing
                || next.revision != i.revision + 1
            {
                return Err(Failure::Unauthorized);
            }
            store.write(
                "effect-journal.json",
                &self.contracts.encode("EnterpriseInvocation", &next)?,
            )?;
            let until = next.expires_at_unix_ms;
            *executing = Some(next);
            Ok(Some(until))
        })
    }
    fn complete(
        &self,
        request: AuthorizationRequest,
        output: std::collections::BTreeMap<String, serde_json::Value>,
    ) -> Call<'_, ()> {
        Box::pin(async move {
            use crate::transport::ControlPort;
            let mut executing = self.executing.lock().await;
            let i = executing.as_ref().ok_or(Failure::Unauthorized)?;
            if i.evaluation.arguments_digest
                != crate::digest(
                    &serde_json::to_vec(&request.arguments).map_err(|_| Failure::Validation)?,
                )
                || i.evaluation.tool_id != request.tool_id
                || i.evaluation.action != request.action
            {
                return Err(Failure::Unauthorized);
            }
            let (identity, control, store) = self.device()?;
            let next = control
                .report_effect(
                    identity,
                    EnterpriseEffectReport {
                        invocation_id: i.id.clone(),
                        expected_revision: i.revision,
                        state: EnterpriseInvocationState::Succeeded,
                        completed_resources: i.evaluation.resources.clone(),
                        result_digest: Some(crate::digest(
                            &serde_json::to_vec(&output).map_err(|_| Failure::Validation)?,
                        )),
                    },
                )
                .await?;
            if next.id != i.id || next.state != EnterpriseInvocationState::Succeeded {
                return Err(Failure::Unauthorized);
            }
            store.write(
                "effect-journal.json",
                &self.contracts.encode("EnterpriseInvocation", &next)?,
            )?;
            *executing = None;
            Ok(())
        })
    }
}

fn recoverable_outcome(previous: &EnterpriseInvocation, current: &EnterpriseInvocation) -> bool {
    current.id == previous.id
        && current.evaluation == previous.evaluation
        && current.request_digest == previous.request_digest
        && current.authorization_epoch == previous.authorization_epoch
        && current.revision >= previous.revision
        && matches!(
            current.state,
            EnterpriseInvocationState::Succeeded
                | EnterpriseInvocationState::Failed
                | EnterpriseInvocationState::Partial
                | EnterpriseInvocationState::Expired
                | EnterpriseInvocationState::Cancelled
        )
}

#[cfg(test)]
mod address_tests {
    use super::public_address;
    #[test]
    fn destinations_exclude_local_shared_reserved_and_transition_ranges() {
        for blocked in [
            "0.0.0.0",
            "10.1.2.3",
            "127.0.0.1",
            "169.254.169.254",
            "100.64.0.1",
            "100.127.255.254",
            "192.0.2.1",
            "198.18.0.1",
            "198.51.100.1",
            "203.0.113.1",
            "224.0.0.1",
            "255.255.255.255",
            "::1",
            "::ffff:8.8.8.8",
            "fe80::1",
            "fc00::1",
            "2001:db8::1",
            "2002:0808:0808::1",
        ] {
            assert!(!public_address(blocked.parse().unwrap()), "{blocked}");
        }
        for allowed in ["8.8.8.8", "1.1.1.1", "2606:4700:4700::1111"] {
            assert!(public_address(allowed.parse().unwrap()), "{allowed}");
        }
    }
    #[test]
    fn own_exact_terminal_outcome_can_release_a_fence_but_unknown_or_substituted_outcomes_cannot() {
        let mut previous: olo_toolgate_contracts::EnterpriseInvocation = serde_json::from_value(
            serde_json::from_str::<serde_json::Value>(include_str!(
                "../../../tests/fixtures/contracts/v1/valid.json"
            ))
            .unwrap()["EnterpriseInvocation"]
                .clone(),
        )
        .unwrap();
        previous.state = olo_toolgate_contracts::EnterpriseInvocationState::Executing;
        let mut current = previous.clone();
        current.revision += 1;
        current.state = olo_toolgate_contracts::EnterpriseInvocationState::OutcomeUnknown;
        assert!(!super::recoverable_outcome(&previous, &current));
        current.state = olo_toolgate_contracts::EnterpriseInvocationState::Succeeded;
        assert!(super::recoverable_outcome(&previous, &current));
        current.evaluation.context.device_id = "other-device".into();
        assert!(!super::recoverable_outcome(&previous, &current));
    }
}
