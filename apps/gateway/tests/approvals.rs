// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
//! Security boundaries use genuine ephemeral RSA keys and durable-port race doubles.
use axum::{
    body::{to_bytes, Body},
    http::{Request, StatusCode},
};
use base64::{engine::general_purpose::URL_SAFE_NO_PAD, Engine};
use olo_toolgate_contracts::*;
use olo_toolgate_gateway::{
    application::Gateway,
    approvals::{ApprovalConfig, ApprovalCoordinator, HttpApprovalCoordinator, PermitSigner},
    audit::AuditSink,
    auth::{Authenticator, Credential},
    bundles::{BundleKeyring, BundleSourceConfig, BundleVerifier, VerifiedPolicy},
    config::Config,
    digest,
    extraction::Registry,
    http::{runtime_router, AppState},
    policy::{PolicyEvaluator, PortFuture},
    unix_ms,
    validation::Contracts,
};
use serde_json::{json, Value};
use std::{
    collections::BTreeMap,
    io::Write,
    process::{Command, Stdio},
    sync::{
        atomic::{AtomicU64, AtomicUsize, Ordering},
        Arc, Mutex, OnceLock,
    },
};
use tokio::io::{AsyncReadExt, AsyncWriteExt};
use tower::ServiceExt;

const TOKEN: &str = "module05-test-credential-00000000000000000000000000000000";

fn generate_key(bits: usize, exponent: usize) -> Vec<u8> {
    // OpenSSL exists in the required pinned Rust contributor image. Private
    // material stays in anonymous subprocess pipes and memory, never fixtures.
    let generated = Command::new("openssl")
        .args([
            "genpkey",
            "-algorithm",
            "RSA",
            "-pkeyopt",
            &format!("rsa_keygen_bits:{bits}"),
            "-pkeyopt",
            &format!("rsa_keygen_pubexp:{exponent}"),
        ])
        .stderr(Stdio::null())
        .output()
        .expect("OpenSSL is required by RSA tests");
    assert!(generated.status.success());
    let mut converted = Command::new("openssl")
        .args(["pkcs8", "-topk8", "-nocrypt", "-outform", "DER"])
        .stdin(Stdio::piped())
        .stdout(Stdio::piped())
        .stderr(Stdio::null())
        .spawn()
        .unwrap();
    converted
        .stdin
        .take()
        .unwrap()
        .write_all(&generated.stdout)
        .unwrap();
    let converted = converted.wait_with_output().unwrap();
    assert!(converted.status.success());
    converted.stdout
}
fn private_key() -> &'static [u8] {
    static KEY: OnceLock<Vec<u8>> = OnceLock::new();
    KEY.get_or_init(|| generate_key(2048, 65537))
}
fn config() -> ApprovalConfig {
    ApprovalConfig {
        url: "http://127.0.0.1:8082".into(),
        token_path: "approval-token".into(),
        private_key_path: "permit-private.pem".into(),
        key_id: "permit-test".into(),
        issuer: "gateway".into(),
        audience: "endpoint".into(),
        request_timeout_ms: 100,
        permit_lifetime_ms: 10_000,
        development_loopback_http: true,
    }
}
fn signer() -> PermitSigner {
    PermitSigner::from_pkcs8(config(), private_key(), &BundleKeyring { keys: vec![] }).unwrap()
}
fn signed_permit(header: &[u8], claims: &[u8]) -> SignedExecutionPermit {
    let key = ring::signature::RsaKeyPair::from_pkcs8(private_key()).unwrap();
    let message = format!(
        "{}.{}",
        URL_SAFE_NO_PAD.encode(header),
        URL_SAFE_NO_PAD.encode(claims)
    );
    let mut signature = vec![0; key.public().modulus_len()];
    key.sign(
        &ring::signature::RSA_PKCS1_SHA256,
        &ring::rand::SystemRandom::new(),
        message.as_bytes(),
        &mut signature,
    )
    .unwrap();
    SignedExecutionPermit {
        jws: format!("{message}.{}", URL_SAFE_NO_PAD.encode(signature)),
    }
}
fn request() -> AuthorizationRequest {
    serde_json::from_value(json!({"toolId":"files.read", "action":"read", "arguments":{"path":"workspace/readme.txt"}})).unwrap()
}
fn context(id: &str) -> RequestContext {
    RequestContext {
        request_id: id.into(),
        tenant_id: "tenant-demo".into(),
        user_id: "user-demo".into(),
        agent_id: "agent-demo".into(),
        device_id: None,
    }
}
fn input(id: &str) -> PolicyInput {
    PolicyInput {
        context: context(id),
        tool_id: "files.read".into(),
        action: "read".into(),
        resource: ResourceDescriptor {
            kind: ResourceKind::File,
            locator: "workspace/readme.txt".into(),
        },
        arguments_digest: digest(&serde_json::to_vec(&request().arguments).unwrap()),
    }
}
fn resolution(input: PolicyInput, expiry: u64) -> ApprovalResolution {
    ApprovalResolution {
        approval_id: "approval-test".into(),
        state: ApprovalState::Consumed,
        input,
        policy_version: "2.0.1".into(),
        permit_id: Some("lease-test".into()),
        permit_expires_at_unix_ms: Some(expiry),
    }
}

struct MutablePolicy {
    mode: AtomicUsize,
    deadline: AtomicU64,
}
impl MutablePolicy {
    fn new() -> Self {
        Self {
            mode: AtomicUsize::new(0),
            deadline: AtomicU64::new(unix_ms().unwrap() + 60_000),
        }
    }
    fn decide(&self, input: &PolicyInput, now: u64) -> PolicyDecision {
        let mode = self.mode.load(Ordering::Acquire);
        let decision = if now >= self.deadline.load(Ordering::Acquire) {
            Decision::Block
        } else {
            match mode {
                1 => Decision::Allow,
                2 => Decision::Block,
                _ => Decision::Ask,
            }
        };
        PolicyDecision {
            reason: if decision == Decision::Ask {
                DecisionReason::ApprovalRequired
            } else {
                DecisionReason::Matched
            },
            decision,
            policy_version: if mode == 3 { "2.0.2" } else { "2.0.1" }.into(),
            request_id: input.context.request_id.clone(),
        }
    }
}
impl PolicyEvaluator for MutablePolicy {
    fn evaluate<'a>(
        &'a self,
        input: &'a PolicyInput,
        now: u64,
    ) -> PortFuture<'a, Result<PolicyDecision, &'static str>> {
        Box::pin(async move { Ok(self.decide(input, now)) })
    }
    fn ready(&self, now: u64) -> bool {
        now < self.deadline.load(Ordering::Acquire)
    }
    fn decision_valid(&self, decision: &PolicyDecision, input: &PolicyInput, now: u64) -> bool {
        self.decide(input, now) == *decision
    }
    fn approval_deadline(&self, input: &PolicyInput, now: u64) -> Option<u64> {
        (self.decide(input, now).decision == Decision::Ask)
            .then(|| self.deadline.load(Ordering::Acquire))
    }
}
struct RecordingAudit {
    events: Mutex<Vec<RuntimeAuditEvent>>,
    fail: bool,
    revoke: Option<Arc<MutablePolicy>>,
}
impl Default for RecordingAudit {
    fn default() -> Self {
        Self {
            events: Mutex::new(vec![]),
            fail: false,
            revoke: None,
        }
    }
}
impl AuditSink for RecordingAudit {
    fn record(&self, event: RuntimeAuditEvent) -> PortFuture<'_, Result<(), &'static str>> {
        Box::pin(async move {
            if self.fail {
                return Err("audit unavailable");
            }
            self.events.lock().unwrap().push(event);
            if let Some(policy) = &self.revoke {
                policy.mode.store(2, Ordering::Release);
            }
            Ok(())
        })
    }
    fn ready(&self) -> bool {
        !self.fail
    }
}

struct Coordinator {
    mode: AtomicUsize,
    calls: AtomicUsize,
    consumed: AtomicUsize,
    leases: Mutex<BTreeMap<String, ApprovalResolution>>,
    revoke: Option<Arc<MutablePolicy>>,
}
impl Default for Coordinator {
    fn default() -> Self {
        Self {
            mode: AtomicUsize::new(0),
            calls: AtomicUsize::new(0),
            consumed: AtomicUsize::new(0),
            leases: Mutex::new(BTreeMap::new()),
            revoke: None,
        }
    }
}
impl ApprovalCoordinator for Coordinator {
    fn resolve<'a>(
        &'a self,
        request: &'a ApprovalSubmission,
        _: &'a str,
    ) -> PortFuture<'a, Result<ApprovalResolution, &'static str>> {
        Box::pin(async move {
            let count = self.calls.fetch_add(1, Ordering::AcqRel);
            let mode = self.mode.load(Ordering::Acquire);
            if mode == 3 {
                return Err("unavailable");
            }
            let mut result = resolution(request.input.clone(), unix_ms().unwrap() + 10_000);
            result.policy_version = request.policy_version.clone();
            result.permit_id = Some(format!("lease-{count}"));
            if mode == 0 || (mode == 1 && count > 0) {
                result.state = if mode == 0 {
                    ApprovalState::Pending
                } else {
                    ApprovalState::Consumed
                };
                result.permit_id = None;
                result.permit_expires_at_unix_ms = None;
            }
            if mode == 2 {
                result.state = ApprovalState::ApprovedTemporary;
            }
            if mode == 4 {
                result.input.context.user_id = "wrong-user".into();
            }
            if mode == 5 {
                result.policy_version = "2.0.2".into();
            }
            if mode == 6 {
                result.permit_expires_at_unix_ms = None;
            }
            if mode == 7 {
                result.state = ApprovalState::Pending;
            }
            if mode == 8 {
                result.permit_expires_at_unix_ms = Some(unix_ms().unwrap() - 1);
            }
            if mode == 9 {
                result.state = ApprovalState::Denied;
                result.permit_id = None;
                result.permit_expires_at_unix_ms = None;
            }
            if mode == 10 {
                result.state = ApprovalState::Expired;
                result.permit_id = None;
                result.permit_expires_at_unix_ms = None;
            }
            if let Some(policy) = &self.revoke {
                policy.mode.store(2, Ordering::Release);
            }
            if let Some(id) = &result.permit_id {
                self.leases
                    .lock()
                    .unwrap()
                    .insert(id.clone(), result.clone());
            }
            Ok(result)
        })
    }
    fn consume<'a>(
        &'a self,
        request: &'a ApprovalPermitUse,
        _: &'a str,
    ) -> PortFuture<'a, Result<ApprovalResolution, &'static str>> {
        Box::pin(async move {
            self.consumed.fetch_add(1, Ordering::AcqRel);
            if self.mode.load(Ordering::Acquire) == 3 {
                return Err("unavailable");
            }
            let mut leases = self.leases.lock().unwrap();
            let stored = leases.get(&request.permit_id).ok_or("spent permit")?;
            if stored.input != request.input
                || stored.approval_id != request.approval_id
                || stored.policy_version != request.policy_version
            {
                return Err("mismatched lease");
            }
            let mut stored = leases.remove(&request.permit_id).unwrap();
            stored.state = ApprovalState::Consumed;
            if self.mode.load(Ordering::Acquire) == 11 {
                stored.approval_id = "wrong-approval".into();
            }
            if let Some(policy) = &self.revoke {
                policy.mode.store(2, Ordering::Release);
            }
            Ok(stored)
        })
    }
}
fn gateway(
    policy: Arc<MutablePolicy>,
    coordinator: Arc<Coordinator>,
    audit: Arc<RecordingAudit>,
) -> Gateway {
    let config: Config =
        serde_json::from_str(include_str!("../../../docs/examples/gateway-static.json")).unwrap();
    Gateway {
        contracts: Contracts::new().unwrap(),
        extractors: Registry::new(config.extractors).unwrap(),
        policy,
        audit,
        approval: Some(coordinator),
        permit_signer: Some(Arc::new(signer())),
    }
}
async fn authorize(gateway: &Gateway, id: &str) -> AuthorizationOutcome {
    gateway
        .authorize_v2(
            request(),
            context(id),
            "1".repeat(32),
            unix_ms().unwrap(),
            unix_ms().unwrap() + 60_000,
        )
        .await
        .unwrap()
}

#[test]
fn permit_binding_expiry_and_trust_domain_security() {
    let signer = signer();
    let now = unix_ms().unwrap();
    let input = input("issuance");
    let result = resolution(input.clone(), now + 20_000);
    let permit = signer.issue(&input, &result, now, now + 1000).unwrap();
    let claims = signer.verify(&permit, &input, now).unwrap();
    assert_eq!(claims.expires_at_unix_ms, now + 1000);
    assert_eq!(claims.jti, "lease-test");
    // Slightly older concurrent timestamps cannot lower the effective clock.
    assert!(signer.verify(&permit, &input, now - 1).is_ok());
    let mut changed = input.clone();
    changed.context.request_id = "consume-ingress".into();
    assert_eq!(
        signer.verify(&permit, &changed, now).unwrap().request_id,
        "issuance"
    );
    for field in 0..8 {
        let mut changed = input.clone();
        match field {
            0 => changed.context.tenant_id = "other".into(),
            1 => changed.context.user_id = "other".into(),
            2 => changed.context.agent_id = "other".into(),
            3 => changed.context.device_id = Some("other".into()),
            4 => changed.tool_id = "other".into(),
            5 => changed.action = "other".into(),
            6 => changed.resource.locator = "other".into(),
            _ => changed.arguments_digest = "f".repeat(64),
        }
        assert!(
            signer.verify(&permit, &changed, now).is_err(),
            "binding field {field}"
        );
    }
    let mut corrupted = permit.clone();
    let last = corrupted.jws.len() - 8;
    let changed = if corrupted.jws.as_bytes()[last] == b'_' {
        "A"
    } else {
        "_"
    };
    corrupted.jws.replace_range(last..last + 1, changed);
    assert!(signer.verify(&corrupted, &input, now).is_err());
    let public = signer.public_key();
    let reused: BundleKeyring = serde_json::from_value(json!({"keys":[public]})).unwrap();
    assert!(PermitSigner::from_pkcs8(config(), private_key(), &reused).is_err());
    let mut reused = signer.public_key();
    reused["keyId"] = "policy-other".into();
    assert!(PermitSigner::from_pkcs8(
        config(),
        private_key(),
        &serde_json::from_value(json!({"keys":[reused]})).unwrap()
    )
    .is_err());
    let mut wrong_domain = config();
    wrong_domain.audience = "other".into();
    let wrong_domain =
        PermitSigner::from_pkcs8(wrong_domain, private_key(), &BundleKeyring { keys: vec![] })
            .unwrap();
    assert!(wrong_domain.verify(&permit, &input, now).is_err());
    assert!(signer.verify(&permit, &input, now + 1000).is_err());
}

#[test]
fn permit_keys_exact_sizes_and_exponent_and_configuration_bounds() {
    for (bits, exponent) in [(2050, 65537), (2048, 3)] {
        assert!(PermitSigner::from_pkcs8(
            config(),
            &generate_key(bits, exponent),
            &BundleKeyring { keys: vec![] }
        )
        .is_err());
    }
    for url in [
        "http://example.com",
        "https://user:pass@example.com",
        "https://example.com/evil",
        "https://example.com?next=evil",
        "https://169.254.169.254",
        "https://metadata.google.internal",
        "https://[fe80::1]",
        "https://0.0.0.0",
    ] {
        let mut cfg = config();
        cfg.url = url.into();
        assert!(cfg.validate().is_err(), "origin {url}");
    }
    let mut cfg = config();
    cfg.permit_lifetime_ms = 10_001;
    assert!(cfg.validate().is_err());
    cfg = config();
    cfg.private_key_path = cfg.token_path.clone();
    assert!(cfg.validate().is_err());
    cfg = config();
    cfg.development_loopback_http = false;
    assert!(cfg.validate().is_err());
    cfg.url = "https://control.internal:8443".into();
    assert!(cfg.validate().is_ok());
}

#[tokio::test]
async fn pending_deny_expired_outage_malformed_resolution_and_legacy_fail_closed() {
    let policy = Arc::new(MutablePolicy::new());
    let coordinator = Arc::new(Coordinator::default());
    let audit = Arc::new(RecordingAudit::default());
    let gateway = gateway(policy.clone(), coordinator.clone(), audit.clone());
    let pending = authorize(&gateway, "pending").await;
    assert_eq!(pending.decision.decision, Decision::Ask);
    assert!(pending.permit.is_none());
    assert_eq!(pending.approval_id.unwrap(), "approval-test");
    assert_eq!(
        gateway
            .authorize(
                request(),
                context("legacy"),
                "1".repeat(32),
                unix_ms().unwrap()
            )
            .await
            .unwrap()
            .decision,
        Decision::Block
    );
    assert_eq!(coordinator.calls.load(Ordering::Acquire), 1);
    for mode in [3, 4, 5, 6, 7, 8, 9, 10] {
        coordinator.mode.store(mode, Ordering::Release);
        let outcome = authorize(&gateway, "blocked").await;
        assert_eq!(outcome.decision.decision, Decision::Block, "mode {mode}");
        assert!(outcome.permit.is_none());
    }
    policy.mode.store(1, Ordering::Release);
    let prior = coordinator.calls.load(Ordering::Acquire);
    assert_eq!(
        authorize(&gateway, "ordinary-allow")
            .await
            .decision
            .decision,
        Decision::Allow
    );
    policy.mode.store(2, Ordering::Release);
    assert_eq!(
        authorize(&gateway, "ordinary-block")
            .await
            .decision
            .decision,
        Decision::Block
    );
    assert_eq!(coordinator.calls.load(Ordering::Acquire), prior);
    assert!(!serde_json::to_string(&*audit.events.lock().unwrap())
        .unwrap()
        .contains("workspace/readme.txt"));
}

#[tokio::test]
async fn once_single_issuance_atomic_permit_consumption_and_replay() {
    let coordinator = Arc::new(Coordinator::default());
    coordinator.mode.store(1, Ordering::Release);
    let gateway = gateway(
        Arc::new(MutablePolicy::new()),
        coordinator.clone(),
        Arc::new(RecordingAudit::default()),
    );
    let approved = authorize(&gateway, "original-attempt").await;
    assert_eq!(approved.decision.decision, Decision::Allow);
    assert_eq!(
        authorize(&gateway, "second-attempt")
            .await
            .decision
            .decision,
        Decision::Block
    );
    let use_request = ExecutionPermitUseRequest {
        permit: approved.permit.unwrap(),
        request: request(),
    };
    let now = unix_ms().unwrap();
    let (first, second) = tokio::join!(
        gateway.consume_permit(
            use_request.clone(),
            context("consume-a"),
            "1".repeat(32),
            now,
            now + 60_000
        ),
        gateway.consume_permit(
            use_request.clone(),
            context("consume-b"),
            "2".repeat(32),
            now,
            now + 60_000
        )
    );
    let first = first.unwrap();
    let second = second.unwrap();
    assert_eq!(
        [first.decision.clone(), second.decision.clone()]
            .into_iter()
            .filter(|d| *d == Decision::Allow)
            .count(),
        1
    );
    assert_eq!(first.request_id, "consume-a");
    assert_eq!(second.request_id, "consume-b");
    assert_eq!(
        gateway
            .consume_permit(
                use_request,
                context("replay"),
                "1".repeat(32),
                unix_ms().unwrap(),
                now + 60_000
            )
            .await
            .unwrap()
            .decision,
        Decision::Block
    );
}

#[tokio::test]
async fn temporary_every_attempt_coordinates_and_credential_policy_deadlines_cap_permits() {
    let policy = Arc::new(MutablePolicy::new());
    let coordinator = Arc::new(Coordinator::default());
    coordinator.mode.store(2, Ordering::Release);
    let gateway = gateway(
        policy.clone(),
        coordinator.clone(),
        Arc::new(RecordingAudit::default()),
    );
    let first = authorize(&gateway, "temporary-a").await;
    let second = authorize(&gateway, "temporary-b").await;
    assert_eq!(first.decision.decision, Decision::Allow);
    assert_eq!(second.decision.decision, Decision::Allow);
    assert_ne!(first.permit, second.permit);
    assert_eq!(coordinator.calls.load(Ordering::Acquire), 2);
    let now = unix_ms().unwrap();
    policy.deadline.store(now + 3000, Ordering::Release);
    let third = gateway
        .authorize_v2(
            request(),
            context("deadline"),
            "1".repeat(32),
            now,
            now + 1000,
        )
        .await
        .unwrap();
    let claims = gateway
        .permit_signer
        .as_ref()
        .unwrap()
        .verify(
            &third.permit.unwrap(),
            &input("deadline"),
            unix_ms().unwrap(),
        )
        .unwrap();
    assert_eq!(claims.expires_at_unix_ms, now + 1000);
    coordinator.mode.store(3, Ordering::Release);
    assert_eq!(
        authorize(&gateway, "outage").await.decision.decision,
        Decision::Block
    );
}

#[tokio::test]
async fn network_and_audit_policy_races_and_audit_failure_cannot_allow() {
    let policy = Arc::new(MutablePolicy::new());
    let coordinator = Arc::new(Coordinator {
        revoke: Some(policy.clone()),
        ..Coordinator::default()
    });
    coordinator.mode.store(2, Ordering::Release);
    let gw = gateway(
        policy.clone(),
        coordinator,
        Arc::new(RecordingAudit::default()),
    );
    assert_eq!(
        authorize(&gw, "revoked-on-network").await.decision.decision,
        Decision::Block
    );
    policy.mode.store(0, Ordering::Release);
    let coordinator = Arc::new(Coordinator::default());
    coordinator.mode.store(2, Ordering::Release);
    let gw = gateway(
        policy.clone(),
        coordinator.clone(),
        Arc::new(RecordingAudit {
            revoke: Some(policy.clone()),
            ..RecordingAudit::default()
        }),
    );
    let result = authorize(&gw, "revoked-on-audit").await;
    assert_eq!(result.decision.decision, Decision::Block);
    assert!(result.permit.is_none());
    policy.mode.store(0, Ordering::Release);
    let gw = gateway(
        policy,
        coordinator,
        Arc::new(RecordingAudit {
            fail: true,
            ..RecordingAudit::default()
        }),
    );
    assert_eq!(
        gw.authorize_v2(
            request(),
            context("audit-down"),
            "1".repeat(32),
            unix_ms().unwrap(),
            unix_ms().unwrap() + 60_000
        )
        .await
        .unwrap_err(),
        ErrorCode::DependencyUnavailable
    );
}

#[tokio::test]
async fn permit_wrong_identity_version_operation_outage_and_mismatched_consume_block_before_grant()
{
    let policy = Arc::new(MutablePolicy::new());
    let coordinator = Arc::new(Coordinator::default());
    coordinator.mode.store(2, Ordering::Release);
    let gateway = gateway(
        policy.clone(),
        coordinator.clone(),
        Arc::new(RecordingAudit::default()),
    );
    let permit = authorize(&gateway, "issuance").await.permit.unwrap();
    let mut ctx = context("consume");
    ctx.user_id = "other".into();
    let use_request = ExecutionPermitUseRequest {
        request: request(),
        permit: permit.clone(),
    };
    let now = unix_ms().unwrap();
    assert_eq!(
        gateway
            .consume_permit(use_request.clone(), ctx, "1".repeat(32), now, now + 60_000)
            .await
            .unwrap()
            .decision,
        Decision::Block
    );
    let mut altered = use_request.clone();
    altered
        .request
        .arguments
        .insert("extra".into(), true.into());
    assert_eq!(
        gateway
            .consume_permit(
                altered,
                context("consume"),
                "1".repeat(32),
                now,
                now + 60_000
            )
            .await
            .unwrap()
            .decision,
        Decision::Block
    );
    policy.mode.store(3, Ordering::Release);
    assert_eq!(
        gateway
            .consume_permit(
                use_request.clone(),
                context("consume"),
                "1".repeat(32),
                now,
                now + 60_000
            )
            .await
            .unwrap()
            .decision,
        Decision::Block
    );
    assert_eq!(coordinator.consumed.load(Ordering::Acquire), 0);
    policy.mode.store(0, Ordering::Release);
    coordinator.mode.store(3, Ordering::Release);
    assert_eq!(
        gateway
            .consume_permit(
                use_request.clone(),
                context("consume"),
                "1".repeat(32),
                now,
                now + 60_000
            )
            .await
            .unwrap()
            .decision,
        Decision::Block
    );
    coordinator.mode.store(11, Ordering::Release);
    assert_eq!(
        gateway
            .consume_permit(
                use_request,
                context("consume"),
                "1".repeat(32),
                now,
                now + 60_000
            )
            .await
            .unwrap()
            .decision,
        Decision::Block
    );
}

fn signed_bundle(
    policy: Value,
    key: &ring::signature::RsaKeyPair,
    expiry: u64,
    sequence: u64,
) -> Vec<u8> {
    let bytes = serde_json::to_vec(&policy).unwrap();
    let payload = json!({"formatVersion":2, "issuer":"control", "audience":"gateway", "tenantId":"tenant-demo", "sequence":sequence,
        "version":format!("2.0.{sequence}"), "directoryRevision":0, "issuedAtUnixMs":1000, "expiresAtUnixMs":expiry,
        "graceMs":1000, "policySha256":digest(&bytes), "policy":URL_SAFE_NO_PAD.encode(bytes)});
    let header = json!({"alg":"RS256", "typ":"toolgate-policy-bundle+jws", "kid":"policy-test"});
    let message = format!(
        "{}.{}",
        URL_SAFE_NO_PAD.encode(serde_json::to_vec(&header).unwrap()),
        URL_SAFE_NO_PAD.encode(serde_json::to_vec(&payload).unwrap())
    );
    let mut signature = vec![0; key.public().modulus_len()];
    key.sign(
        &ring::signature::RSA_PKCS1_SHA256,
        &ring::rand::SystemRandom::new(),
        message.as_bytes(),
        &mut signature,
    )
    .unwrap();
    serde_json::to_vec(&json!({"jws":format!("{message}.{}", URL_SAFE_NO_PAD.encode(signature))}))
        .unwrap()
}

#[test]
fn signed_format_two_block_ask_allow_precedence_expiry_and_grace_cannot_bypass_ask() {
    let permit_signer = signer();
    let mut public = permit_signer.public_key();
    public["keyId"] = "policy-test".into();
    let config = BundleSourceConfig {
        url: "https://control/api/control/v1/bundles/current".into(),
        tenant_id: "tenant-demo".into(),
        issuer: "control".into(),
        audience: "gateway".into(),
        keyring_path: "keys".into(),
        token_path: "token".into(),
        minimum_sequence: 0,
        max_grace_ms: 1000,
        poll_interval_ms: 100,
        fetch_timeout_ms: 1000,
        development_loopback_http: false,
    };
    let instant = std::time::Instant::now();
    let policy = VerifiedPolicy::with_monotonic_clock(
        BundleVerifier::new(
            config,
            serde_json::from_value(json!({"keys":[public]})).unwrap(),
        )
        .unwrap(),
        Arc::new(move || instant),
    );
    let key = ring::signature::RsaKeyPair::from_pkcs8(private_key()).unwrap();
    let rule = |id: &str, effect: &str, grace: bool| {
        json!({"policyId":id,"userIds":[],"agentIds":[],"deviceIds":[],"toolId":"files.read",
        "action":"read", "resource":{"kind":"FILE","locator":"workspace/readme.txt"},"graceAllowed":grace,"effect":effect})
    };
    let rules = vec![rule("allow", "ALLOW", true), rule("ask", "ASK", false)];
    policy
        .adopt(
            &signed_bundle(json!({"formatVersion":2,"rules":rules}), &key, 2000, 1),
            1000,
        )
        .unwrap();
    assert_eq!(policy.decide(&input("req"), 1000).decision, Decision::Ask);
    assert_eq!(policy.approval_deadline(&input("req"), 1000), Some(2000));
    assert_eq!(policy.decide(&input("req"), 2000).decision, Decision::Block);
    assert!(policy.approval_deadline(&input("req"), 2000).is_none());
    let rules = vec![
        rule("ask", "ASK", false),
        rule("allow", "ALLOW", true),
        rule("block", "BLOCK", false),
    ];
    policy
        .adopt(
            &signed_bundle(json!({"formatVersion":2,"rules":rules}), &key, 4000, 2),
            2000,
        )
        .unwrap();
    assert_eq!(policy.decide(&input("req"), 2000).decision, Decision::Block);
    let rules = vec![rule("ask", "ASK", false), rule("allow", "ALLOW", true)];
    policy
        .adopt(
            &signed_bundle(json!({"formatVersion":2,"rules":rules}), &key, 4000, 3),
            2000,
        )
        .unwrap();
    let decision = policy.decide(&input("req"), 2000);
    assert_eq!(decision.decision, Decision::Ask);
    assert_eq!(decision.reason, DecisionReason::ApprovalRequired);
    let rules = vec![rule("ask", "ASK", true)];
    assert!(policy
        .adopt(
            &signed_bundle(json!({"formatVersion":2,"rules":rules}), &key, 4000, 4),
            2000
        )
        .is_err());
}

#[tokio::test]
async fn authenticated_http_v2_permit_consume_and_legacy_routes() {
    let policy = Arc::new(MutablePolicy::new());
    let coordinator = Arc::new(Coordinator::default());
    let gateway = gateway(
        policy,
        coordinator.clone(),
        Arc::new(RecordingAudit::default()),
    );
    let config: Config =
        serde_json::from_str(include_str!("../../../docs/examples/gateway-static.json")).unwrap();
    let auth = Authenticator::new(
        vec![Credential {
            token_sha256: digest(TOKEN.as_bytes()),
            tenant_id: "tenant-demo".into(),
            user_id: "user-demo".into(),
            agent_id: "agent-demo".into(),
            device_id: None,
            expires_at_unix_ms: unix_ms().unwrap() + 60_000,
        }],
        &gateway.contracts,
        unix_ms().unwrap(),
    )
    .unwrap();
    let router = runtime_router(Arc::new(AppState::new(gateway, auth, config)));
    let call = |path: &'static str, body: Value, token: Option<&str>| {
        let router = router.clone();
        let token = token.map(str::to_owned);
        async move {
            let mut request = Request::builder()
                .method("POST")
                .uri(path)
                .header("content-type", "application/json");
            if let Some(token) = token {
                request = request.header("authorization", format!("Bearer {token}"));
            }
            let response = router
                .oneshot(
                    request
                        .body(Body::from(serde_json::to_vec(&body).unwrap()))
                        .unwrap(),
                )
                .await
                .unwrap();
            let status = response.status();
            let headers = response.headers().clone();
            let bytes = to_bytes(response.into_body(), 32768).await.unwrap();
            (
                status,
                serde_json::from_slice::<Value>(&bytes).unwrap(),
                headers,
            )
        }
    };
    let body = serde_json::to_value(request()).unwrap();
    assert_eq!(
        call("/v2/authorize", body.clone(), None).await.0,
        StatusCode::UNAUTHORIZED
    );
    let (status, pending, headers) = call("/v2/authorize", body.clone(), Some(TOKEN)).await;
    assert_eq!(status, StatusCode::OK);
    assert_eq!(pending["decision"]["decision"], "ASK");
    assert_eq!(headers["cache-control"], "no-store");
    assert_eq!(
        call("/v1/authorize", body.clone(), Some(TOKEN)).await.1["decision"],
        "BLOCK"
    );
    coordinator.mode.store(2, Ordering::Release);
    let approved = call("/v2/authorize", body.clone(), Some(TOKEN)).await.1;
    assert_eq!(approved["decision"]["decision"], "ALLOW");
    let use_body = json!({"permit":approved["permit"],"request":body});
    assert_eq!(
        call("/v1/permits/consume", use_body.clone(), Some(TOKEN))
            .await
            .1["decision"],
        "ALLOW"
    );
    assert_eq!(
        call("/v1/permits/consume", use_body, Some(TOKEN)).await.1["decision"],
        "BLOCK"
    );
    assert_eq!(
        call(
            "/v1/permits/consume",
            json!({"permit":{"jws":"bad"},"request":{}}),
            Some(TOKEN)
        )
        .await
        .0,
        StatusCode::BAD_REQUEST
    );
}

#[tokio::test]
async fn approval_http_redirect_media_duplicate_json_size_and_external_token_refresh_reject_safely()
{
    let path = std::env::temp_dir().join(format!("toolgate-approval-token-{}", std::process::id()));
    tokio::fs::write(&path, b"test.approval.jwt").await.unwrap();
    let listener = tokio::net::TcpListener::bind("127.0.0.1:0").await.unwrap();
    let mut cfg = config();
    cfg.url = format!("http://{}", listener.local_addr().unwrap());
    cfg.token_path = path.clone();
    cfg.request_timeout_ms = 1000;
    let coordinator = HttpApprovalCoordinator::new(cfg).unwrap();
    let request = ApprovalSubmission {
        input: input("http"),
        policy_version: "2.0.1".into(),
    };
    let good = serde_json::to_vec(&resolution(
        request.input.clone(),
        unix_ms().unwrap() + 1000,
    ))
    .unwrap();
    let responses = vec![
        b"HTTP/1.1 302 Found\r\nLocation: http://127.0.0.1:1/evil\r\nContent-Length: 0\r\n\r\n".to_vec(),
        b"HTTP/1.1 200 OK\r\nContent-Type: text/plain\r\nContent-Length: 2\r\n\r\n{}".to_vec(),
        b"HTTP/1.1 401 Unauthorized\r\nContent-Type: application/json\r\nContent-Length: 2\r\n\r\n{}".to_vec(),
        b"HTTP/1.1 200 OK\r\nContent-Type: application/json\r\nContent-Length: 32769\r\n\r\n".to_vec(),
        [b"HTTP/1.1 200 OK\r\nContent-Type: application/json\r\nConnection: close\r\n\r\n".as_slice(), &vec![b'x';32769]].concat(),
        b"HTTP/1.1 200 OK\r\nContent-Type: application/json\r\nContent-Length: 13\r\n\r\n{\"a\":1,\"a\":2}".to_vec(),
        [format!("HTTP/1.1 200 OK\r\nContent-Type: application/json\r\nContent-Length: {}\r\n\r\n",good.len()).as_bytes(),&good].concat(),
    ];
    for (n, response) in responses.into_iter().enumerate() {
        if n == 6 {
            tokio::fs::write(&path, b"refreshed.approval.jwt")
                .await
                .unwrap();
        }
        let serve = async {
            let (mut stream, _) = listener.accept().await.unwrap();
            let mut headers = vec![0; 8192];
            let count = stream.read(&mut headers).await.unwrap();
            let headers = String::from_utf8_lossy(&headers[..count]);
            assert!(headers.starts_with("POST /api/control/v1/approvals/resolve "));
            assert!(headers.to_lowercase().contains(if n == 6 {
                "authorization: bearer refreshed.approval.jwt"
            } else {
                "authorization: bearer test.approval.jwt"
            }));
            let _ = stream.write_all(&response).await;
        };
        let trace = "1".repeat(32);
        let (result, ()) = tokio::join!(coordinator.resolve(&request, &trace), serve);
        assert_eq!(result.is_ok(), n == 6, "response {n}");
    }
    tokio::fs::write(&path, b"token\r\ninjected: header")
        .await
        .unwrap();
    assert!(coordinator
        .resolve(&request, &"1".repeat(32))
        .await
        .is_err());
    tokio::fs::remove_file(path).await.unwrap();
}

#[tokio::test]
async fn permit_monotonic_expiry_wall_regression_and_consume_never_revive() {
    let elapsed = Arc::new(AtomicU64::new(0));
    let clock_elapsed = elapsed.clone();
    let instant = std::time::Instant::now();
    let now = unix_ms().unwrap();
    let signer = Arc::new(
        PermitSigner::with_clock(
            config(),
            private_key(),
            &BundleKeyring { keys: vec![] },
            now,
            Arc::new(move || {
                instant + std::time::Duration::from_millis(clock_elapsed.load(Ordering::Acquire))
            }),
        )
        .unwrap(),
    );
    let policy = Arc::new(MutablePolicy::new());
    let coordinator = Arc::new(Coordinator::default());
    coordinator.mode.store(2, Ordering::Release);
    let mut gateway = gateway(
        policy,
        coordinator.clone(),
        Arc::new(RecordingAudit::default()),
    );
    gateway.permit_signer = Some(signer.clone());
    let outcome = authorize(&gateway, "clock-bound").await;
    let permit = outcome.permit.unwrap();
    let claims = signer.verify(&permit, &input("clock-bound"), now).unwrap();
    elapsed.store(claims.expires_at_unix_ms - now, Ordering::Release);
    // Wall clock has not observed expiry; monotonic projection independently has.
    assert!(signer.verify(&permit, &input("clock-bound"), now).is_err());
    // Even a regressed wall and injected monotonic clock cannot lower high-water.
    elapsed.store(1, Ordering::Release);
    assert!(signer
        .verify(&permit, &input("clock-bound"), now + 1)
        .is_err());
    let consumed = gateway
        .consume_permit(
            ExecutionPermitUseRequest {
                permit,
                request: request(),
            },
            context("regressed-consume"),
            "1".repeat(32),
            now + 1,
            now + 60_000,
        )
        .await
        .unwrap();
    assert_eq!(consumed.decision, Decision::Block);
    assert_eq!(coordinator.consumed.load(Ordering::Acquire), 0);
    let resolution = resolution(input("reissue"), now + 5000);
    assert!(signer
        .issue(&resolution.input, &resolution, now + 1, now + 5000)
        .is_err());
}

#[test]
fn signed_permit_algorithm_type_unknown_fields_duplicates_and_future_claims_reject() {
    let signer = signer();
    let now = unix_ms().unwrap();
    let input = input("signed-negative");
    let permit = signer
        .issue(
            &input,
            &resolution(input.clone(), now + 10_000),
            now,
            now + 10_000,
        )
        .unwrap();
    let parts: Vec<_> = permit.jws.split('.').collect();
    let header: Value = serde_json::from_slice(&URL_SAFE_NO_PAD.decode(parts[0]).unwrap()).unwrap();
    let claims: Value = serde_json::from_slice(&URL_SAFE_NO_PAD.decode(parts[1]).unwrap()).unwrap();
    for field in ["alg", "typ", "kid", "jku"] {
        let mut bad_header = header.clone();
        bad_header[field] = json!("unexpected");
        let invalid = signed_permit(
            &serde_json::to_vec(&bad_header).unwrap(),
            &serde_json::to_vec(&claims).unwrap(),
        );
        assert!(
            signer.verify(&invalid, &input, now).is_err(),
            "header {field}"
        );
    }
    for field in ["issuer", "audience", "permitVersion", "unknown"] {
        let mut bad_claims = claims.clone();
        bad_claims[field] = json!("unexpected");
        let invalid = signed_permit(
            &serde_json::to_vec(&header).unwrap(),
            &serde_json::to_vec(&bad_claims).unwrap(),
        );
        assert!(
            signer.verify(&invalid, &input, now).is_err(),
            "claims {field}"
        );
    }
    let mut future = claims.clone();
    future["issuedAtUnixMs"] = json!(now + 60_000);
    future["expiresAtUnixMs"] = json!(now + 61_000);
    assert!(signer
        .verify(
            &signed_permit(
                &serde_json::to_vec(&header).unwrap(),
                &serde_json::to_vec(&future).unwrap()
            ),
            &input,
            now
        )
        .is_err());
    let mut long = claims.clone();
    long["expiresAtUnixMs"] = json!(long["issuedAtUnixMs"].as_u64().unwrap() + 10_001);
    assert!(signer
        .verify(
            &signed_permit(
                &serde_json::to_vec(&header).unwrap(),
                &serde_json::to_vec(&long).unwrap()
            ),
            &input,
            now
        )
        .is_err());
    let duplicate_header = br#"{"alg":"RS256","alg":"RS256","kid":"permit-test","typ":"toolgate-execution-permit+jws"}"#;
    assert!(signer
        .verify(
            &signed_permit(duplicate_header, &serde_json::to_vec(&claims).unwrap()),
            &input,
            now
        )
        .is_err());
    let text = serde_json::to_string(&claims).unwrap();
    let duplicate = text.replacen('{', "{\"permitVersion\":1,", 1);
    assert!(signer
        .verify(
            &signed_permit(&serde_json::to_vec(&header).unwrap(), duplicate.as_bytes()),
            &input,
            now
        )
        .is_err());
}
