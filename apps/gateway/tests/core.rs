// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
use axum::{
    body::{to_bytes, Body},
    http::{Request, StatusCode},
    Router,
};
use olo_toolgate_contracts::*;
use olo_toolgate_gateway::{
    application::Gateway,
    audit::{AuditSink, JsonAudit},
    auth::{Authenticator, Credential},
    config::Config,
    digest,
    extraction::{Registry, ResourceExtractor},
    http::{management_router, runtime_router, AppState},
    limits::RateLimit,
    policy::{Effect, PolicyEvaluator, PortFuture},
    server, unix_ms,
    validation::{strict_json, Contracts},
};
use serde_json::{json, Value};
use std::{
    sync::{atomic::Ordering, Arc, Mutex},
    time::Duration,
};
use tokio::{
    io::{AsyncReadExt, AsyncWriteExt},
    net::{TcpListener, TcpStream},
    sync::watch,
    time::Instant,
};
use tower::ServiceExt;

const TOKEN: &str = "module01-test-credential-00000000000000000000000000000000";
fn config() -> Config {
    serde_json::from_str(include_str!("../../../docs/examples/gateway-static.json")).unwrap()
}
fn request() -> AuthorizationRequest {
    serde_json::from_value(payload()).unwrap()
}
fn payload() -> Value {
    json!({"toolId":"files.read","action":"read","arguments":{"path":"workspace/readme.txt"}})
}
fn context() -> RequestContext {
    RequestContext {
        request_id: "test-request".into(),
        tenant_id: "tenant-demo".into(),
        user_id: "user-demo".into(),
        agent_id: "agent-demo".into(),
        device_id: None,
    }
}
fn credential() -> Credential {
    Credential {
        token_sha256: digest(TOKEN.as_bytes()),
        tenant_id: "tenant-demo".into(),
        user_id: "user-demo".into(),
        agent_id: "agent-demo".into(),
        device_id: None,
        expires_at_unix_ms: 4102444800000,
    }
}

#[derive(Default)]
struct RecordingAudit {
    events: Mutex<Vec<RuntimeAuditEvent>>,
    fail: bool,
}
impl AuditSink for RecordingAudit {
    fn record(&self, event: RuntimeAuditEvent) -> PortFuture<'_, Result<(), &'static str>> {
        Box::pin(async move {
            if self.fail {
                Err("sink failed")
            } else {
                self.events.lock().unwrap().push(event);
                Ok(())
            }
        })
    }
    fn ready(&self) -> bool {
        !self.fail
    }
}

struct BrokenPolicy {
    ask: bool,
    pending: bool,
}
impl PolicyEvaluator for BrokenPolicy {
    fn evaluate<'a>(
        &'a self,
        input: &'a PolicyInput,
        _: u64,
    ) -> PortFuture<'a, Result<PolicyDecision, &'static str>> {
        Box::pin(async move {
            if self.pending {
                std::future::pending::<()>().await;
            }
            if self.ask {
                Ok(PolicyDecision {
                    decision: Decision::Ask,
                    reason: DecisionReason::ApprovalRequired,
                    policy_version: "0.1.0-dev".into(),
                    request_id: input.context.request_id.clone(),
                })
            } else {
                Err("policy failure containing private details")
            }
        })
    }
    fn ready(&self, _: u64) -> bool {
        true
    }
}

fn state(
    policy: Arc<dyn PolicyEvaluator>,
    audit: Arc<dyn AuditSink>,
    mut cfg: Config,
) -> Arc<AppState> {
    cfg.limits.request_timeout_ms = 50;
    let contracts = Contracts::new().unwrap();
    let auth = Authenticator::new(vec![credential()], &contracts, unix_ms().unwrap()).unwrap();
    Arc::new(AppState::new(
        Gateway {
            contracts,
            extractors: Registry::new(cfg.extractors.clone()).unwrap(),
            policy,
            audit,
        },
        auth,
        cfg,
    ))
}

async fn call(
    router: Router,
    path: &str,
    body: impl Into<Body>,
    token: Option<&str>,
) -> (StatusCode, Value, axum::http::HeaderMap) {
    let mut builder = Request::builder()
        .method("POST")
        .uri(path)
        .header("content-type", "application/json");
    if let Some(token) = token {
        builder = builder.header("authorization", format!("Bearer {token}"));
    }
    let response = router
        .oneshot(builder.body(body.into()).unwrap())
        .await
        .unwrap();
    let status = response.status();
    let headers = response.headers().clone();
    let bytes = to_bytes(response.into_body(), 1_048_576).await.unwrap();
    (status, serde_json::from_slice(&bytes).unwrap(), headers)
}

#[test]
fn policy_block_precedence_order_independence_emergency_and_default() {
    let mut p = config().policy;
    let input = PolicyInput {
        context: context(),
        tool_id: request().tool_id,
        action: request().action,
        resource: ResourceDescriptor {
            kind: ResourceKind::File,
            locator: "workspace/readme.txt".into(),
        },
        arguments_digest: "a".repeat(64),
    };
    assert_eq!(Decision::Allow, p.decide(&input, 1).decision);
    let mut block = p.rules[0].clone();
    block.effect = Effect::Block;
    p.rules.push(block);
    assert_eq!(Decision::Block, p.decide(&input, 1).decision);
    p.rules.reverse();
    assert_eq!(Decision::Block, p.decide(&input, 1).decision);
    p.rules.retain(|r| r.effect == Effect::Allow);
    p.emergency_block = true;
    assert_eq!(Decision::Block, p.decide(&input, 1).decision);
    p.emergency_block = false;
    let mut other = input.clone();
    other.context.tenant_id = "other-tenant".into();
    assert_eq!(DecisionReason::NoMatch, p.decide(&other, 1).reason);
    other = input.clone();
    other.resource.locator = "workspace/secret.txt".into();
    assert_eq!(Decision::Block, p.decide(&other, 1).decision);
    assert_eq!(
        DecisionReason::PolicyUnavailable,
        p.decide(&input, p.expires_at_unix_ms).reason
    );
    assert!(!p.ready(p.expires_at_unix_ms));
}

#[test]
fn config_rejects_unknown_unbounded_expired_or_unsafe_settings() {
    let contracts = Contracts::new().unwrap();
    let cfg = config();
    cfg.validate(&contracts, 1).unwrap();
    let mut invalid = serde_json::to_value(&cfg).unwrap();
    invalid["authBypass"] = json!(true);
    assert!(serde_json::from_value::<Config>(invalid).is_err());
    for field in [
        "maxBodyBytes",
        "maxConcurrentRequests",
        "maxConnections",
        "requestsPerSecond",
        "requestTimeoutMs",
        "auditQueueCapacity",
    ] {
        let mut invalid = serde_json::to_value(&cfg).unwrap();
        invalid["limits"][field] = json!(0);
        assert!(
            serde_json::from_value::<Config>(invalid)
                .unwrap()
                .validate(&contracts, 1)
                .is_err(),
            "{field}"
        );
    }
    let mut invalid = cfg.clone();
    invalid.listen = "0.0.0.0:8081".parse().unwrap();
    assert!(invalid.validate(&contracts, 1).is_err());
    invalid = cfg.clone();
    invalid.allowed_origins = vec!["*".into()];
    assert!(invalid.validate(&contracts, 1).is_err());
    invalid = cfg.clone();
    invalid.extractors.push(invalid.extractors[0].clone());
    assert!(invalid.validate(&contracts, 1).is_err());
    invalid = cfg;
    invalid.policy.expires_at_unix_ms = 1;
    assert!(invalid.validate(&contracts, 1).is_err());
    let mut invalid = serde_json::to_value(config()).unwrap();
    invalid["policy"]["rules"][0]["effect"] = json!("ASK");
    assert!(serde_json::from_value::<Config>(invalid).is_err());
}

#[test]
fn auth_binds_principal_rejects_expiry_duplicates_and_wrong_token() {
    let contracts = Contracts::new().unwrap();
    let c = credential();
    let auth = Authenticator::new(vec![c.clone()], &contracts, 1).unwrap();
    let authenticated = auth.authenticate(TOKEN, "r".into(), 2).unwrap();
    assert_eq!("tenant-demo", authenticated.0.tenant_id);
    assert!(auth.authenticate("invalid", "r".into(), 2).is_none());
    assert!(auth
        .authenticate(TOKEN, "r".into(), c.expires_at_unix_ms)
        .is_none());
    assert!(Authenticator::new(vec![c.clone(), c.clone()], &contracts, 1).is_err());
    assert!(Authenticator::new(vec![], &contracts, 1).is_err());
    let mut bad = c;
    bad.tenant_id = "../tenant".into();
    assert!(Authenticator::new(vec![bad], &contracts, 1).is_err());
}

#[test]
fn extraction_rejects_aliases_traversal_missing_fields_and_unknown_tools() {
    let registry = Registry::new(config().extractors).unwrap();
    for path in [
        "../secret",
        "workspace/../secret",
        "workspace//readme.txt",
        "workspace/%2e%2e/secret",
        "https://metadata.invalid",
        "/absolute",
        " workspace/readme.txt",
        "workspace\\readme.txt",
        "workspace/readme.txt\n",
    ] {
        let mut req = request();
        req.arguments.insert("path".into(), json!(path));
        assert!(registry.extract(&req).is_err(), "{path}");
    }
    let mut req = request();
    req.arguments.clear();
    assert!(registry.extract(&req).is_err());
    req = request();
    req.tool_id = "unknown".into();
    assert!(registry.extract(&req).is_err());
    assert_eq!(
        "workspace/readme.txt",
        registry.extract(&request()).unwrap().locator
    );
}

#[test]
fn strict_json_rejects_duplicates_at_every_depth_and_normalizes_order() {
    assert!(strict_json(br#"{"toolId":"a","toolId":"b"}"#).is_err());
    assert!(strict_json(br#"{"arguments":{"a":1,"a":2}}"#).is_err());
    let a = strict_json(br#"{"b":{"y":1,"x":2},"a":3}"#).unwrap();
    let b = strict_json(br#"{"a":3,"b":{"x":2,"y":1}}"#).unwrap();
    assert_eq!(
        digest(&serde_json::to_vec(&a).unwrap()),
        digest(&serde_json::to_vec(&b).unwrap())
    );
}

#[tokio::test(start_paused = true)]
async fn rate_limit_exact_boundary_is_deterministic() {
    let limit = RateLimit::new(2);
    let now = Instant::now();
    assert!(limit.admit(now));
    assert!(limit.admit(now));
    assert!(!limit.admit(now));
    assert!(!limit.admit(now + Duration::from_millis(999)));
    assert!(limit.admit(now + Duration::from_secs(1)));
}

#[tokio::test]
async fn http_allows_only_matching_input_and_audits_without_secrets() {
    let audit = Arc::new(RecordingAudit::default());
    let s = state(Arc::new(config().policy), audit.clone(), config());
    let mut body = payload();
    body["arguments"]["private"] = json!("password-private-sentinel");
    let (status, decision, headers) = call(
        runtime_router(s.clone()),
        "/v1/authorize",
        body.to_string(),
        Some(TOKEN),
    )
    .await;
    assert_eq!(StatusCode::OK, status);
    assert_eq!("ALLOW", decision["decision"]);
    assert_eq!(
        headers["x-request-id"].to_str().unwrap(),
        decision["requestId"]
    );
    assert_eq!("no-store", headers["cache-control"]);
    assert!(headers.contains_key("traceparent"));
    let events = audit.events.lock().unwrap();
    assert_eq!(1, events.len());
    let serialized = serde_json::to_string(&events[0]).unwrap();
    for secret in [TOKEN, "password-private-sentinel", "workspace/readme.txt"] {
        assert!(!serialized.contains(secret));
    }
    assert!(!s.metrics.render().contains("files.read"));
    assert_eq!(1, s.metrics.allow.load(Ordering::Relaxed));
}

#[tokio::test]
async fn http_malformed_auth_and_limits_never_return_allow() {
    let s = state(
        Arc::new(config().policy),
        Arc::new(RecordingAudit::default()),
        config(),
    );
    let router = runtime_router(s.clone());
    for (body, expected) in [
        ("{", StatusCode::BAD_REQUEST),
        ("[]", StatusCode::BAD_REQUEST),
        (
            r#"{"toolId":"files.read","action":"read","arguments":{},"context":{"tenantId":"victim"}}"#,
            StatusCode::BAD_REQUEST,
        ),
    ] {
        let (status, v, _) = call(router.clone(), "/v1/authorize", body, Some(TOKEN)).await;
        assert_eq!(expected, status);
        assert_ne!("ALLOW", v["decision"]);
    }
    for token in [None, Some("invalid-credential")] {
        let (status, v, _) = call(
            router.clone(),
            "/v1/authorize",
            payload().to_string(),
            token,
        )
        .await;
        assert_eq!(StatusCode::UNAUTHORIZED, status);
        assert_eq!("UNAUTHORIZED", v["code"]);
    }
    let (status, _, _) = call(
        router.clone(),
        "/v1/authorize",
        "x".repeat(s.config.limits.max_body_bytes + 1),
        Some(TOKEN),
    )
    .await;
    assert_eq!(StatusCode::PAYLOAD_TOO_LARGE, status);
    let _held = s
        .admission
        .acquire_many(s.config.limits.max_concurrent_requests as u32)
        .await
        .unwrap();
    let (status, _, _) = call(router, "/v1/authorize", payload().to_string(), Some(TOKEN)).await;
    assert_eq!(StatusCode::SERVICE_UNAVAILABLE, status);
}

#[tokio::test(start_paused = true)]
async fn policy_timeout_is_bounded_without_sleep_synchronization() {
    let s = state(
        Arc::new(BrokenPolicy {
            ask: false,
            pending: true,
        }),
        Arc::new(RecordingAudit::default()),
        config(),
    );
    let (status, body, _) = call(
        runtime_router(s),
        "/v1/authorize",
        payload().to_string(),
        Some(TOKEN),
    )
    .await;
    assert_eq!(StatusCode::GATEWAY_TIMEOUT, status);
    assert_eq!("TIMEOUT", body["code"]);
}

#[tokio::test]
async fn policy_and_audit_failures_and_ask_fail_closed() {
    for policy in [
        Arc::new(BrokenPolicy {
            ask: false,
            pending: false,
        }) as Arc<dyn PolicyEvaluator>,
        Arc::new(config().policy),
    ] {
        let audit = Arc::new(RecordingAudit {
            fail: true,
            ..Default::default()
        });
        let s = state(policy, audit, config());
        let (status, body, _) = call(
            runtime_router(s),
            "/v1/authorize",
            payload().to_string(),
            Some(TOKEN),
        )
        .await;
        assert_eq!(StatusCode::SERVICE_UNAVAILABLE, status);
        assert_eq!("DEPENDENCY_UNAVAILABLE", body["code"]);
        assert!(!body.to_string().contains("private"));
    }
    let s = state(
        Arc::new(BrokenPolicy {
            ask: true,
            pending: false,
        }),
        Arc::new(RecordingAudit::default()),
        config(),
    );
    let (_, body, _) = call(
        runtime_router(s),
        "/v1/authorize",
        payload().to_string(),
        Some(TOKEN),
    )
    .await;
    assert_eq!("BLOCK", body["decision"]);
    assert_eq!("APPROVAL_REQUIRED", body["reason"]);
}

struct FailedExtractor;
impl ResourceExtractor for FailedExtractor {
    fn extract(&self, _: &Value) -> Result<ResourceDescriptor, &'static str> {
        Err("extractor private error")
    }
}

#[tokio::test]
async fn extractor_port_failure_rejects_and_expired_policy_blocks() {
    let cfg = config();
    let contracts = Contracts::new().unwrap();
    let mut registry = Registry::new(cfg.extractors).unwrap();
    registry.insert(
        "files.read".into(),
        "read".into(),
        Arc::new(FailedExtractor),
    );
    let gateway = Gateway {
        contracts,
        extractors: registry,
        policy: Arc::new(cfg.policy),
        audit: Arc::new(RecordingAudit::default()),
    };
    assert_eq!(
        ErrorCode::Validation,
        gateway
            .authorize(request(), context(), "1".repeat(32), 1)
            .await
            .unwrap_err()
    );
    let mut policy = config().policy;
    policy.expires_at_unix_ms = 1;
    let s = state(
        Arc::new(policy),
        Arc::new(RecordingAudit::default()),
        config(),
    );
    assert!(!s.ready());
    let (_, body, _) = call(
        runtime_router(s),
        "/v1/authorize",
        payload().to_string(),
        Some(TOKEN),
    )
    .await;
    assert_eq!("BLOCK", body["decision"]);
}

fn mcp_request(method: &str, name: Option<&str>) -> Request<Body> {
    let mut params = json!({"_meta":{"io.modelcontextprotocol/protocolVersion":"2026-07-28","io.modelcontextprotocol/clientInfo":{"name":"test","version":"1.0.0"},"io.modelcontextprotocol/clientCapabilities":{}}});
    if let Some(name) = name {
        params["name"] = json!(name);
        params["arguments"] = payload()["arguments"].clone();
    }
    let mut builder = Request::builder()
        .method("POST")
        .uri("/mcp")
        .header("content-type", "application/json")
        .header("accept", "application/json, text/event-stream")
        .header("authorization", format!("Bearer {TOKEN}"))
        .header("mcp-protocol-version", "2026-07-28")
        .header("mcp-method", method);
    if let Some(name) = name {
        builder = builder.header("mcp-name", name);
    }
    builder
        .body(Body::from(
            json!({"jsonrpc":"2.0","id":7,"method":method,"params":params}).to_string(),
        ))
        .unwrap()
}

#[tokio::test]
async fn mcp_stateless_ping_discovery_calls_and_header_mismatch() {
    let s = state(
        Arc::new(config().policy),
        Arc::new(RecordingAudit::default()),
        config(),
    );
    let router = runtime_router(s.clone());
    for method in ["ping", "server/discover", "tools/list", "tools/call"] {
        let response = router
            .clone()
            .oneshot(mcp_request(
                method,
                (method == "tools/call").then_some("files.read"),
            ))
            .await
            .unwrap();
        assert_eq!(StatusCode::OK, response.status());
        assert!(!response.headers().contains_key("mcp-session-id"));
        let body: Value =
            serde_json::from_slice(&to_bytes(response.into_body(), 65536).await.unwrap()).unwrap();
        assert_eq!(7, body["id"]);
        if method != "tools/call" {
            assert_eq!("complete", body["result"]["resultType"]);
        }
        if method == "tools/list" {
            assert_eq!(json!([]), body["result"]["tools"]);
        }
        if method == "tools/call" {
            assert_eq!("UNSUPPORTED", body["error"]["data"]["code"]);
        }
    }
    let mut request = mcp_request("tools/call", Some("files.read"));
    request
        .headers_mut()
        .insert("mcp-name", "different".parse().unwrap());
    let response = router.clone().oneshot(request).await.unwrap();
    assert_eq!(StatusCode::BAD_REQUEST, response.status());
    let body: Value =
        serde_json::from_slice(&to_bytes(response.into_body(), 65536).await.unwrap()).unwrap();
    assert_eq!(-32020, body["error"]["code"]);
    let mut request = mcp_request("ping", None);
    request
        .headers_mut()
        .insert("origin", "https://untrusted.invalid".parse().unwrap());
    assert_eq!(
        StatusCode::FORBIDDEN,
        router.clone().oneshot(request).await.unwrap().status()
    );
    let response = router
        .oneshot(
            Request::builder()
                .uri("/mcp")
                .header("authorization", format!("Bearer {TOKEN}"))
                .body(Body::empty())
                .unwrap(),
        )
        .await
        .unwrap();
    assert_eq!(StatusCode::METHOD_NOT_ALLOWED, response.status());
}

#[tokio::test]
async fn mcp_protocol_negative_cases_and_encoded_name() {
    let router = runtime_router(state(
        Arc::new(config().policy),
        Arc::new(RecordingAudit::default()),
        config(),
    ));
    for (case, expected) in [
        ("version", -32022),
        ("capabilities", -32602),
        ("json", -32700),
    ] {
        let (mut parts, body) = mcp_request("ping", None).into_parts();
        let mut value: Value =
            serde_json::from_slice(&to_bytes(body, 65536).await.unwrap()).unwrap();
        if case == "version" {
            parts
                .headers
                .insert("mcp-protocol-version", "2025-11-25".parse().unwrap());
            value["params"]["_meta"]["io.modelcontextprotocol/protocolVersion"] =
                json!("2025-11-25");
        } else if case == "capabilities" {
            value["params"]["_meta"]
                .as_object_mut()
                .unwrap()
                .remove("io.modelcontextprotocol/clientCapabilities");
        }
        let body = if case == "json" {
            "{".to_owned()
        } else {
            value.to_string()
        };
        let response = router
            .clone()
            .oneshot(Request::from_parts(parts, Body::from(body)))
            .await
            .unwrap();
        assert_eq!(StatusCode::BAD_REQUEST, response.status());
        let value: Value =
            serde_json::from_slice(&to_bytes(response.into_body(), 65536).await.unwrap()).unwrap();
        assert_eq!(expected, value["error"]["code"]);
        if case == "json" {
            assert_eq!(Some(&Value::Null), value.get("id"));
        }
    }
    let mut request = mcp_request("tools/call", Some("files.read"));
    request
        .headers_mut()
        .insert("mcp-name", "=?base64?ZmlsZXMucmVhZA==?=".parse().unwrap());
    let response = router.oneshot(request).await.unwrap();
    assert_eq!(StatusCode::OK, response.status());
    let value: Value =
        serde_json::from_slice(&to_bytes(response.into_body(), 65536).await.unwrap()).unwrap();
    assert_eq!("UNSUPPORTED", value["error"]["data"]["code"]);
    let (parts, body) = mcp_request("ping", None).into_parts();
    let mut value: Value = serde_json::from_slice(&to_bytes(body, 65536).await.unwrap()).unwrap();
    value.as_object_mut().unwrap().remove("id");
    let response = runtime_router(state(
        Arc::new(config().policy),
        Arc::new(RecordingAudit::default()),
        config(),
    ))
    .oneshot(Request::from_parts(parts, Body::from(value.to_string())))
    .await
    .unwrap();
    assert_eq!(StatusCode::NOT_IMPLEMENTED, response.status());
    let value: Value =
        serde_json::from_slice(&to_bytes(response.into_body(), 65536).await.unwrap()).unwrap();
    assert!(value.get("jsonrpc").is_none());
}

#[tokio::test]
async fn correlation_and_management_surfaces_do_not_trust_identity_headers() {
    let s = state(
        Arc::new(config().policy),
        Arc::new(RecordingAudit::default()),
        config(),
    );
    let trace = "00-1234567890abcdef1234567890abcdef-1234567890abcdef-01";
    let request = Request::builder()
        .method("POST")
        .uri("/v1/authorize")
        .header("content-type", "application/json")
        .header("authorization", format!("Bearer {TOKEN}"))
        .header("traceparent", trace)
        .header("x-tenant-id", "victim")
        .header("x-request-id", "caller-id")
        .body(Body::from(payload().to_string()))
        .unwrap();
    let response = runtime_router(s.clone()).oneshot(request).await.unwrap();
    assert_eq!(StatusCode::OK, response.status());
    assert!(response.headers()["traceparent"]
        .to_str()
        .unwrap()
        .contains("1234567890abcdef1234567890abcdef"));
    assert_ne!("caller-id", response.headers()["x-request-id"]);
    let management = management_router(s.clone());
    for path in ["/v1/health/live", "/v1/health/ready", "/v1/metrics"] {
        assert_eq!(
            StatusCode::OK,
            management
                .clone()
                .oneshot(Request::builder().uri(path).body(Body::empty()).unwrap())
                .await
                .unwrap()
                .status()
        );
    }
    s.draining.store(true, Ordering::Release);
    assert_eq!(
        StatusCode::SERVICE_UNAVAILABLE,
        management
            .oneshot(
                Request::builder()
                    .uri("/v1/health/ready")
                    .body(Body::empty())
                    .unwrap()
            )
            .await
            .unwrap()
            .status()
    );
    let (status, _, _) = call(
        runtime_router(s),
        "/v1/authorize",
        payload().to_string(),
        Some(TOKEN),
    )
    .await;
    assert_eq!(StatusCode::SERVICE_UNAVAILABLE, status);
}

#[tokio::test]
async fn real_tcp_http_and_graceful_shutdown() {
    let s = state(
        Arc::new(config().policy),
        Arc::new(RecordingAudit::default()),
        config(),
    );
    let listener = TcpListener::bind("127.0.0.1:0").await.unwrap();
    let address = listener.local_addr().unwrap();
    let (stop, signal) = watch::channel(false);
    let task = tokio::spawn(server::serve(
        listener,
        runtime_router(s.clone()),
        s.config.limits.clone(),
        signal,
    ));
    let body = payload().to_string();
    let mut connection = TcpStream::connect(address).await.unwrap();
    connection.write_all(format!("POST /v1/authorize HTTP/1.1\r\nHost: localhost\r\nAuthorization: Bearer {TOKEN}\r\nContent-Type: application/json\r\nContent-Length: {}\r\nConnection: close\r\n\r\n{body}",body.len()).as_bytes()).await.unwrap();
    let mut bytes = Vec::new();
    connection.read_to_end(&mut bytes).await.unwrap();
    let text = String::from_utf8(bytes).unwrap();
    assert!(text.starts_with("HTTP/1.1 200"));
    assert!(text.contains("\"decision\":\"ALLOW\""));
    let (parts, body) = mcp_request("ping", None).into_parts();
    let body = to_bytes(body, 65536).await.unwrap();
    let mut wire = String::from("POST /mcp HTTP/1.1\r\nHost: localhost\r\n");
    for (name, value) in &parts.headers {
        wire.push_str(&format!("{name}: {}\r\n", value.to_str().unwrap()));
    }
    wire.push_str(&format!(
        "Content-Length: {}\r\nConnection: close\r\n\r\n",
        body.len()
    ));
    let mut connection = TcpStream::connect(address).await.unwrap();
    connection.write_all(wire.as_bytes()).await.unwrap();
    connection.write_all(&body).await.unwrap();
    let mut bytes = Vec::new();
    connection.read_to_end(&mut bytes).await.unwrap();
    let text = String::from_utf8(bytes).unwrap();
    assert!(text.starts_with("HTTP/1.1 200"));
    assert!(text.contains("\"id\":7"));
    stop.send(true).unwrap();
    task.await.unwrap().unwrap();
    assert!(TcpStream::connect(address).await.is_err());
}

#[tokio::test]
async fn json_audit_writer_failure_is_acknowledged_and_removes_readiness() {
    struct BrokenWriter;
    impl std::io::Write for BrokenWriter {
        fn write(&mut self, _: &[u8]) -> std::io::Result<usize> {
            Err(std::io::Error::other("collector unavailable"))
        }
        fn flush(&mut self) -> std::io::Result<()> {
            Ok(())
        }
    }
    let (audit, worker) = JsonAudit::new(1, BrokenWriter);
    let event: RuntimeAuditEvent = serde_json::from_value(json!({"timestampUnixMs":1,"context":context(),"toolId":"files.read","action":"read","resourceDigest":"a".repeat(64),"argumentsDigest":"b".repeat(64),"decision":{"decision":"BLOCK","reason":"NO_MATCH","policyVersion":"0.1.0-dev","requestId":"test-request"},"traceId":"c".repeat(32)})).unwrap();
    assert!(audit.record(event).await.is_err());
    worker.await.unwrap();
    assert!(!audit.ready());
}

#[tokio::test]
async fn ingress_rate_header_duplicate_credentials_and_media_limits() {
    let mut cfg = config();
    cfg.limits.requests_per_second = 1;
    let s = state(
        Arc::new(cfg.policy.clone()),
        Arc::new(RecordingAudit::default()),
        cfg,
    );
    let router = runtime_router(s);
    assert_eq!(
        StatusCode::OK,
        call(
            router.clone(),
            "/v1/authorize",
            payload().to_string(),
            Some(TOKEN)
        )
        .await
        .0
    );
    assert_eq!(
        StatusCode::TOO_MANY_REQUESTS,
        call(router, "/v1/authorize", payload().to_string(), Some(TOKEN))
            .await
            .0
    );
    let s = state(
        Arc::new(config().policy),
        Arc::new(RecordingAudit::default()),
        config(),
    );
    let router = runtime_router(s);
    let mut huge = mcp_request("ping", None);
    huge.headers_mut()
        .insert("x-padding", "x".repeat(9000).parse().unwrap());
    assert_eq!(
        StatusCode::REQUEST_HEADER_FIELDS_TOO_LARGE,
        router.clone().oneshot(huge).await.unwrap().status()
    );
    let mut duplicate = mcp_request("ping", None);
    duplicate
        .headers_mut()
        .append("authorization", format!("Bearer {TOKEN}").parse().unwrap());
    assert_eq!(
        StatusCode::UNAUTHORIZED,
        router.clone().oneshot(duplicate).await.unwrap().status()
    );
    let mut media = mcp_request("ping", None);
    media
        .headers_mut()
        .insert("content-type", "text/plain".parse().unwrap());
    assert_eq!(
        StatusCode::UNSUPPORTED_MEDIA_TYPE,
        router.oneshot(media).await.unwrap().status()
    );
}

#[tokio::test(start_paused = true)]
async fn audit_dependency_wait_is_within_the_request_deadline() {
    struct PendingAudit;
    impl AuditSink for PendingAudit {
        fn record(&self, _: RuntimeAuditEvent) -> PortFuture<'_, Result<(), &'static str>> {
            Box::pin(std::future::pending())
        }
        fn ready(&self) -> bool {
            true
        }
    }
    let s = state(Arc::new(config().policy), Arc::new(PendingAudit), config());
    let (status, body, _) = call(
        runtime_router(s),
        "/v1/authorize",
        payload().to_string(),
        Some(TOKEN),
    )
    .await;
    assert_eq!(StatusCode::GATEWAY_TIMEOUT, status);
    assert_eq!("TIMEOUT", body["code"]);
}
