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
}
/// TLS authenticates Gateway; its response header and canonical decision must agree.
/// A pending ASK or malformed lease never grants permission.
fn grant(
    outcome: AuthorizationOutcome,
    response_id: &str,
) -> Result<Option<SignedExecutionPermit>> {
    if outcome.decision.request_id != response_id || outcome.decision.decision != Decision::Allow {
        return Err(Failure::Unauthorized);
    }
    match (outcome.approval_id, outcome.permit) {
        (Some(_), Some(permit)) => Ok(Some(permit)),
        (None, None) => Ok(None),
        _ => Err(Failure::Unauthorized),
    }
}
impl HttpsGateway {
    pub fn new(settings: &Settings, contracts: Arc<Contracts>) -> Result<Self> {
        settings.validate()?;
        Ok(Self {
            client: client(settings.gateway_ca_path.as_deref())?,
            origin: crate::config::origin(&settings.gateway_url)?,
            token: secret(&settings.gateway_token_path)?,
            contracts,
        })
    }
    async fn post<T: serde::de::DeserializeOwned>(
        &self,
        path: &str,
        bytes: Vec<u8>,
        correlation: &str,
        model: &str,
    ) -> Result<(T, String)> {
        let response = self
            .client
            .post(format!("{}{path}", self.origin))
            .bearer_auth(&self.token)
            .header("Content-Type", "application/json")
            .header("Accept", "application/json")
            .header("X-Request-ID", correlation)
            .header(
                "traceparent",
                format!("00-{correlation}-{}-01", &correlation[..16]),
            )
            .body(bytes)
            .send()
            .await
            .map_err(|_| Failure::Unavailable)?;
        let request_id = response
            .headers()
            .get("x-request-id")
            .and_then(|v| v.to_str().ok())
            .filter(|v| !v.is_empty() && v.len() <= 128)
            .ok_or(Failure::Unauthorized)?
            .to_owned();
        Ok((
            self.contracts.decode(model, &body(response).await?)?,
            request_id,
        ))
    }
}
impl AuthorizationPort for HttpsGateway {
    fn authorize(&self, request: AuthorizationRequest) -> Call<'_, ()> {
        Box::pin(async move { self.authorize_bound(request).await.map(|_| ()) })
    }
    fn authorize_bound(&self, request: AuthorizationRequest) -> Call<'_, Option<u64>> {
        Box::pin(async move {
            let correlation = crate::identity::nonce()?;
            let (outcome, response_id): (AuthorizationOutcome, String) = self
                .post(
                    "/v2/authorize",
                    self.contracts.encode("AuthorizationRequest", &request)?,
                    &correlation,
                    "AuthorizationOutcome",
                )
                .await?;
            tracing::info!(event="gateway_authorization",request_id=%correlation,tool_id=%request.tool_id,decision=?outcome.decision.decision);
            let mut deadline = None;
            if let Some(permit) = grant(outcome, &response_id)? {
                use base64::Engine;
                let parts: Vec<_> = permit.jws.split('.').collect();
                if parts.len() != 3 {
                    return Err(Failure::Unauthorized);
                }
                let payload = base64::engine::general_purpose::URL_SAFE_NO_PAD
                    .decode(parts[1])
                    .map_err(|_| Failure::Unauthorized)?;
                let claims: ExecutionPermitClaims =
                    self.contracts.decode("ExecutionPermitClaims", &payload)?;
                let use_request = ExecutionPermitUseRequest { permit, request };
                let (decision, response_id): (PolicyDecision, String) = self
                    .post(
                        "/v1/permits/consume",
                        self.contracts
                            .encode("ExecutionPermitUseRequest", &use_request)?,
                        &correlation,
                        "PolicyDecision",
                    )
                    .await?;
                if decision.request_id != response_id || decision.decision != Decision::Allow {
                    return Err(Failure::Unauthorized);
                }
                // The authenticated Gateway has just cryptographically verified and
                // consumed these exact claims. Parsing alone never grants permission.
                if claims.expires_at_unix_ms <= crate::now() {
                    return Err(Failure::Expired);
                }
                deadline = Some(claims.expires_at_unix_ms);
            }
            Ok(deadline)
        })
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    #[test]
    fn pending_ask_and_malformed_grants_fail_closed() {
        let fixtures: serde_json::Value = serde_json::from_str(include_str!(
            "../../../tests/fixtures/contracts/v1/valid.json"
        ))
        .unwrap();
        let mut outcome = fixtures["AuthorizationOutcome"].clone();
        let decode =
            |v: serde_json::Value| serde_json::from_value::<AuthorizationOutcome>(v).unwrap();
        assert!(grant(decode(outcome.clone()), "request-1").is_err());
        outcome["decision"]["decision"] = serde_json::json!("ASK");
        assert!(grant(decode(outcome.clone()), "request-1").is_err());
        outcome["decision"]["decision"] = serde_json::json!("ALLOW");
        assert!(grant(decode(outcome.clone()), "other-request").is_err());
        assert!(grant(decode(outcome.clone()), "request-1")
            .unwrap()
            .is_none());
        outcome["approvalId"] = serde_json::json!("approval-1");
        assert!(grant(decode(outcome.clone()), "request-1").is_err());
        outcome["permit"] = fixtures["SignedExecutionPermit"].clone();
        assert!(grant(decode(outcome.clone()), "request-1")
            .unwrap()
            .is_some());
        outcome.as_object_mut().unwrap().remove("approvalId");
        assert!(grant(decode(outcome), "request-1").is_err());
    }
}
