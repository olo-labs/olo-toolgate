// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
//! Private tool ingress uses the same group authority and enrolled device identity as the client.
use axum::{
    extract::{DefaultBodyLimit, State},
    http::{HeaderMap, StatusCode},
    routing::post,
    Json, Router,
};
use olo_toolgate_client::{
    config::Config, contracts::Contracts, identity::DeviceKey, service::ClientService,
    storage::ProtectedStore, transport::HttpsControl, Failure,
};
use olo_toolgate_contracts::BuiltinInvocation;
use serde_json::{json, Value};
use std::sync::Arc;
struct Tools {
    service: tokio::sync::Mutex<ClientService>,
    token: String,
}
async fn invoke(
    State(tools): State<Arc<Tools>>,
    headers: HeaderMap,
    Json(invocation): Json<BuiltinInvocation>,
) -> (StatusCode, Json<Value>) {
    use subtle::ConstantTimeEq;
    let expected = format!("Bearer {}", tools.token);
    let mut authorization = headers.get_all("authorization").iter();
    let credential = authorization
        .next()
        .map(|h| h.as_bytes())
        .unwrap_or_default();
    if authorization.next().is_some() || !bool::from(credential.ct_eq(expected.as_bytes())) {
        return (
            StatusCode::UNAUTHORIZED,
            Json(json!({"code":"UNAUTHORIZED"})),
        );
    }
    let mut service = tools.service.lock().await;
    match service.execute_tool(invocation).await {
        Ok(result) => (StatusCode::OK, Json(json!({"result":result}))),
        Err(failure) => {
            let (status, code) = match failure {
                Failure::Validation => (StatusCode::BAD_REQUEST, "VALIDATION"),
                Failure::Unauthorized | Failure::Revoked => (StatusCode::FORBIDDEN, "FORBIDDEN"),
                Failure::Conflict => (StatusCode::CONFLICT, "CONFLICT"),
                Failure::Unsupported => (StatusCode::NOT_IMPLEMENTED, "UNSUPPORTED"),
                _ => (StatusCode::SERVICE_UNAVAILABLE, "DEPENDENCY_UNAVAILABLE"),
            };
            (status, Json(json!({"code":code})))
        }
    }
}
#[tokio::main]
async fn main() -> Result<(), Box<dyn std::error::Error>> {
    if std::env::args().nth(1).as_deref() == Some("--authorization-profiles") {
        println!(
            "{}",
            serde_json::to_string(
                &olo_toolgate_client::authorization_profile::builtin_profiles()
                    .map_err(|_| "Installed profile unavailable")?
            )?
        );
        return Ok(());
    }
    let path = std::env::var("TOOLGATE_CLIENT_CONFIG")
        .map_err(|_| "Enrolled client configuration required")?;
    let bytes =
        olo_toolgate_client::storage::read_owned(std::path::Path::new(&path), 1048576, true)
            .map_err(|_| "Protected client configuration required")?;
    let config: Config = serde_json::from_slice(&bytes)?;
    config
        .validate()
        .map_err(|_| "Invalid client configuration")?;
    let settings = config
        .tools
        .as_ref()
        .ok_or("Reviewed installed tool profiles required")?;
    let token = olo_toolgate_client::tool_gateway::secret(&settings.gateway_token_path)
        .map_err(|_| "Private ingress credential required")?;
    let contracts = Arc::new(Contracts::new().map_err(|_| "Canonical contracts unavailable")?);
    let store = ProtectedStore::open(config.state_directory.clone())
        .map_err(|_| "Protected state unavailable")?;
    let key = Arc::new(
        DeviceKey::load_or_create(&store).map_err(|_| "Protected device key unavailable")?,
    );
    let control = Arc::new(
        HttpsControl::new(config.clone(), key.clone(), contracts)
            .map_err(|_| "Device transport invalid")?,
    );
    let service = ClientService::open(config, store, key, control)
        .map_err(|_| "Device service unavailable")?;
    let tools = Arc::new(Tools {
        service: tokio::sync::Mutex::new(service),
        token,
    });
    let heartbeat = tools.clone();
    tokio::spawn(async move {
        loop {
            let _ = heartbeat.service.lock().await.tick().await;
            tokio::time::sleep(std::time::Duration::from_millis(500)).await;
        }
    });
    let router = Router::new()
        .route("/invoke", post(invoke))
        .layer(DefaultBodyLimit::max(65536))
        .with_state(tools);
    axum::serve(
        tokio::net::TcpListener::bind("127.0.0.1:8083").await?,
        router,
    )
    .with_graceful_shutdown(async {
        let _ = tokio::signal::ctrl_c().await;
    })
    .await?;
    Ok(())
}
