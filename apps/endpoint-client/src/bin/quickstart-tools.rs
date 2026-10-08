// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
//! Private loopback adapter for the existing fixed built-ins. No author code executes here.
use axum::{
    extract::{DefaultBodyLimit, State},
    http::{HeaderMap, StatusCode},
    routing::post,
    Json, Router,
};
use olo_toolgate_client::{
    builtins::{AuthorizationPort, Executor, Settings},
    contracts::Contracts,
    transport::Call,
    Failure,
};
use olo_toolgate_contracts::{
    AuthorizationOutcome, AuthorizationRequest, BuiltinInvocation, Decision,
};
use serde_json::{json, Value};
use std::sync::{Arc, Mutex};

struct LocalGateway {
    client: reqwest::Client,
    token: String,
    hotfolder_token: String,
    outcome: Arc<Mutex<Option<Value>>>,
    contracts: Contracts,
}
impl AuthorizationPort for LocalGateway {
    fn authorize(&self, request: AuthorizationRequest) -> Call<'_, ()> {
        Box::pin(async move {
            // Read current directory enablement before every effect, including ALLOW policies.
            // This is the fixed Quickstart adapter; no agent can choose the service identity.
            let machine = olo_toolgate_client::tool_gateway::secret(std::path::Path::new(
                "/data/run/machine-token",
            ))?;
            let response = self
                .client
                .get("http://127.0.0.1:8082/api/control/v1/users/local-tools")
                .bearer_auth(&machine)
                .send()
                .await
                .map_err(|_| Failure::Unavailable)?;
            if response.status() != 200 {
                return Err(Failure::Unauthorized);
            }
            let owner: olo_toolgate_contracts::ControlUser = self.contracts.decode(
                "ControlUser",
                &olo_toolgate_client::tool_gateway::body(response).await?,
            )?;
            if owner.id != "local-tools" || !owner.enabled {
                return Err(Failure::Unauthorized);
            }
            let devices: &[&str] = if request.tool_id.starts_with("hotfolder.") {
                &["local-builtins", "local-hotfolder"]
            } else {
                &["local-builtins"]
            };
            for id in devices {
                let response = self
                    .client
                    .get(format!("http://127.0.0.1:8082/api/control/v1/devices/{id}"))
                    .bearer_auth(&machine)
                    .send()
                    .await
                    .map_err(|_| Failure::Unavailable)?;
                if response.status() != 200 {
                    return Err(Failure::Unauthorized);
                }
                let device: olo_toolgate_contracts::ControlDevice = self.contracts.decode(
                    "ControlDevice",
                    &olo_toolgate_client::tool_gateway::body(response).await?,
                )?;
                if device.id != *id || !device.enabled || device.owner_user_id != "local-tools" {
                    return Err(Failure::Unauthorized);
                }
            }
            let correlation = olo_toolgate_client::identity::nonce()?;
            let token = if request.tool_id.starts_with("hotfolder.") {
                &self.hotfolder_token
            } else {
                &self.token
            };
            let response = self
                .client
                .post("http://127.0.0.1:8081/v2/authorize")
                .bearer_auth(token)
                .header("x-request-id", &correlation)
                .header(
                    "traceparent",
                    format!("00-{correlation}-{}-01", &correlation[..16]),
                )
                .header("content-type", "application/json")
                .body(serde_json::to_vec(&request).map_err(|_| Failure::Validation)?)
                .send()
                .await
                .map_err(|_| Failure::Unavailable)?;
            if response.status() != 200 {
                return Err(Failure::Unauthorized);
            }
            let request_id = response
                .headers()
                .get("x-request-id")
                .and_then(|h| h.to_str().ok())
                .ok_or(Failure::Validation)?
                .to_owned();
            let outcome: AuthorizationOutcome = self.contracts.decode(
                "AuthorizationOutcome",
                &olo_toolgate_client::tool_gateway::body(response).await?,
            )?;
            if outcome.decision.request_id != request_id {
                return Err(Failure::Validation);
            }
            if outcome.decision.decision != Decision::Allow {
                *self.outcome.lock().map_err(|_| Failure::Unavailable)? = Some(
                    json!({"decision":outcome.decision.decision,"approvalId":outcome.approval_id}),
                );
                return Err(Failure::Unauthorized);
            }
            let permit = match (outcome.approval_id, outcome.permit) {
                (Some(_), Some(permit)) => Some(permit),
                (None, None) => None,
                _ => return Err(Failure::Unauthorized),
            };
            if let Some(permit) = permit {
                let response = self
                    .client
                    .post("http://127.0.0.1:8081/v1/permits/consume")
                    .bearer_auth(token)
                    .header("x-request-id", &correlation)
                    .header(
                        "traceparent",
                        format!("00-{correlation}-{}-01", &correlation[..16]),
                    )
                    .header("content-type", "application/json")
                    .body(
                        serde_json::to_vec(&json!({"permit":permit,"request":request}))
                            .map_err(|_| Failure::Validation)?,
                    )
                    .send()
                    .await
                    .map_err(|_| Failure::Unavailable)?;
                let response_id = response
                    .headers()
                    .get("x-request-id")
                    .and_then(|h| h.to_str().ok())
                    .ok_or(Failure::Validation)?
                    .to_owned();
                let value: olo_toolgate_contracts::PolicyDecision = self.contracts.decode(
                    "PolicyDecision",
                    &olo_toolgate_client::tool_gateway::body(response).await?,
                )?;
                if value.decision != Decision::Allow || value.request_id != response_id {
                    return Err(Failure::Unauthorized);
                }
            }
            Ok(())
        })
    }
}
struct Tools {
    executor: tokio::sync::Mutex<Executor>,
    token: String,
    outcome: Arc<Mutex<Option<Value>>>,
}
async fn invoke(
    State(tools): State<Arc<Tools>>,
    headers: HeaderMap,
    Json(invocation): Json<BuiltinInvocation>,
) -> (StatusCode, Json<Value>) {
    use subtle::ConstantTimeEq;
    let expected = format!("Bearer {}", tools.token);
    if !bool::from(
        headers
            .get("authorization")
            .map(|h| h.as_bytes())
            .unwrap_or_default()
            .ct_eq(expected.as_bytes()),
    ) {
        return (
            StatusCode::UNAUTHORIZED,
            Json(json!({"code":"UNAUTHORIZED"})),
        );
    }
    let mut executor = tools.executor.lock().await;
    if let Ok(mut outcome) = tools.outcome.lock() {
        *outcome = None;
    }
    match executor
        .execute(invocation, olo_toolgate_client::now() + 10000)
        .await
    {
        Ok(result) => (StatusCode::OK, Json(json!({"result":result}))),
        Err(failure) => {
            let outcome = tools.outcome.lock().ok().and_then(|v| v.clone());
            if let Some(value) = outcome {
                return (StatusCode::FORBIDDEN, Json(value));
            }
            let (status, code) = match failure {
                Failure::Validation => (StatusCode::BAD_REQUEST, "VALIDATION"),
                Failure::Unauthorized => (StatusCode::FORBIDDEN, "FORBIDDEN"),
                Failure::Unsupported => (StatusCode::NOT_IMPLEMENTED, "UNSUPPORTED"),
                _ => (StatusCode::SERVICE_UNAVAILABLE, "DEPENDENCY_UNAVAILABLE"),
            };
            (status, Json(json!({"code":code})))
        }
    }
}
#[tokio::main]
async fn main() -> Result<(), Box<dyn std::error::Error>> {
    let _ = rustls::crypto::ring::default_provider().install_default();
    let settings: Settings = serde_json::from_slice(&std::fs::read("/data/run/builtins.json")?)?;
    let token = olo_toolgate_client::tool_gateway::secret(&settings.gateway_token_path)
        .map_err(|_| "Invalid private token")?;
    let hotfolder_token = olo_toolgate_client::tool_gateway::secret(std::path::Path::new(
        "/data/run/hotfolder-token",
    ))
    .map_err(|_| "Invalid HotFolder token")?;
    let outcome = Arc::new(Mutex::new(None));
    let client = reqwest::Client::builder()
        .no_proxy()
        .redirect(reqwest::redirect::Policy::none())
        .retry(reqwest::retry::never())
        .referer(false)
        .tls_sslkeylogfile(false)
        .timeout(std::time::Duration::from_secs(5))
        .build()?;
    let authorization = Arc::new(LocalGateway {
        client,
        token: token.clone(),
        hotfolder_token,
        outcome: outcome.clone(),
        contracts: Contracts::new().map_err(|_| "Invalid canonical contracts")?,
    });
    let executor =
        Executor::new(&settings, authorization).map_err(|_| "Invalid built-in configuration")?;
    let state = Arc::new(Tools {
        executor: tokio::sync::Mutex::new(executor),
        token,
        outcome,
    });
    let router = Router::new()
        .route("/invoke", post(invoke))
        .layer(DefaultBodyLimit::max(65536))
        .with_state(state);
    let listener = tokio::net::TcpListener::bind("127.0.0.1:8083").await?;
    axum::serve(listener, router)
        .with_graceful_shutdown(async {
            #[cfg(unix)]
            {
                let mut terminate =
                    tokio::signal::unix::signal(tokio::signal::unix::SignalKind::terminate())
                        .expect("Signal handler");
                tokio::select! { _=terminate.recv()=>{}, _=tokio::signal::ctrl_c()=>{} }
            }
            #[cfg(not(unix))]
            {
                let _ = tokio::signal::ctrl_c().await;
            }
        })
        .await?;
    Ok(())
}
