// Copyright 2026 OLO Labs; SPDX-License-Identifier: Apache-2.0
use axum::{
    extract::State,
    http::{HeaderMap, StatusCode},
    response::IntoResponse,
    routing::post,
    Json, Router,
};
use olo_toolgate_contracts::ServiceAudience;
use serde_json::{json, Value};
use std::sync::{Arc, Mutex};
use tool_host::{
    config::ControlConfig,
    control::{Control, ControlError},
};

#[derive(Default)]
struct Seen {
    requests: Vec<Value>,
    statuses: Vec<u16>,
}

async fn fixture(statuses: Vec<u16>) -> (Control, Arc<Mutex<Seen>>, tokio::task::JoinHandle<()>) {
    let seen = Arc::new(Mutex::new(Seen {
        statuses,
        ..Seen::default()
    }));
    async fn issue(
        State(seen): State<Arc<Mutex<Seen>>>,
        headers: HeaderMap,
        Json(body): Json<Value>,
    ) -> axum::response::Response {
        assert_eq!(headers["toolgate-tenant-id"], "tenant-a");
        let mut seen = seen.lock().unwrap();
        let index = seen.requests.len();
        seen.requests.push(body);
        let status = seen.statuses[index.min(seen.statuses.len() - 1)];
        if status == 200 {
            Json(json!({"jws":"signed-permit"})).into_response()
        } else {
            StatusCode::from_u16(status).unwrap().into_response()
        }
    }
    let app = Router::new()
        .route("/api/control/v1/access/service-permits/issue", post(issue))
        .with_state(seen.clone());
    let listener = tokio::net::TcpListener::bind("127.0.0.1:0").await.unwrap();
    let address = listener.local_addr().unwrap();
    let task = tokio::spawn(async move { axum::serve(listener, app).await.unwrap() });
    let config: ControlConfig = serde_json::from_value(json!({"tenantId":"tenant-a", "url":format!("http://{address}"), "developmentLoopbackHttp":true})).unwrap();
    (Control::new(&config).unwrap(), seen, task)
}

#[tokio::test]
async fn admission_backoff_reuses_the_original_invocation_and_fence() {
    let (control, seen, server) = fixture(vec![429, 429, 200]).await;
    let result = control
        .service_permit(
            "inv-original",
            "fence-original",
            ServiceAudience::OloMemory,
            tool_host::now_ms() + 5000,
        )
        .await
        .unwrap();
    assert_eq!(result.jws, "signed-permit");
    let requests = &seen.lock().unwrap().requests;
    assert_eq!(requests.len(), 3);
    assert!(requests.iter().all(|body| *body == json!({"invocationId":"inv-original","fencingToken":"fence-original","audience":"olo-memory"})));
    server.abort();
}

#[tokio::test]
async fn actual_denials_and_server_errors_are_not_retried() {
    for status in [401, 403, 409, 503] {
        let (control, seen, server) = fixture(vec![status, 200]).await;
        assert!(control
            .service_permit(
                "inv",
                "fence",
                ServiceAudience::OloMemory,
                tool_host::now_ms() + 5000
            )
            .await
            .is_err());
        assert_eq!(seen.lock().unwrap().requests.len(), 1);
        server.abort();
    }
}

#[tokio::test]
async fn persistent_throttling_has_a_finite_attempt_limit() {
    let (control, seen, server) = fixture(vec![429]).await;
    assert_eq!(
        control
            .service_permit(
                "inv",
                "fence",
                ServiceAudience::OloRag,
                tool_host::now_ms() + 5000
            )
            .await
            .unwrap_err(),
        ControlError::Refused(429)
    );
    assert_eq!(seen.lock().unwrap().requests.len(), 4);
    server.abort();
}

#[tokio::test]
async fn expiry_prevents_any_permit_request() {
    let (control, seen, server) = fixture(vec![200]).await;
    assert!(control
        .service_permit(
            "inv",
            "fence",
            ServiceAudience::OloMemory,
            tool_host::now_ms()
        )
        .await
        .is_err());
    assert!(seen.lock().unwrap().requests.is_empty());
    server.abort();
}

#[tokio::test]
async fn backoff_cannot_outlive_the_invocation() {
    let (control, seen, server) = fixture(vec![429, 200]).await;
    assert!(control
        .service_permit(
            "inv",
            "fence",
            ServiceAudience::OloMemory,
            tool_host::now_ms() + 50
        )
        .await
        .is_err());
    // A busy scheduler may exhaust the budget before the first socket is opened;
    // it must never send the retry after that budget has expired.
    assert!(seen.lock().unwrap().requests.len() <= 1);
    server.abort();
}
