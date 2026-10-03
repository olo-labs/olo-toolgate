// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
//! Reproducible core baseline; no network or collector latency is hidden in claims.
use olo_toolgate_contracts::{AuthorizationRequest, RequestContext, RuntimeAuditEvent};
use olo_toolgate_gateway::{
    application::Gateway, audit::AuditSink, config::Config, extraction::Registry,
    policy::PortFuture, unix_ms, validation::Contracts,
};
use std::{hint::black_box, sync::Arc, time::Instant};

struct Acknowledged;
impl AuditSink for Acknowledged {
    fn record(&self, event: RuntimeAuditEvent) -> PortFuture<'_, Result<(), &'static str>> {
        Box::pin(async move {
            black_box(event);
            Ok(())
        })
    }
    fn ready(&self) -> bool {
        true
    }
}

fn main() {
    let config: Config =
        serde_json::from_str(include_str!("../../../docs/examples/gateway-static.json")).unwrap();
    let gateway = Gateway {
        contracts: Contracts::new().unwrap(),
        extractors: Registry::new(config.extractors).unwrap(),
        policy: Arc::new(config.policy.unwrap()),
        audit: Arc::new(Acknowledged),
        approval: None,
        permit_signer: None,
    };
    let request: AuthorizationRequest = serde_json::from_str(
        r#"{"toolId":"files.read","action":"read","arguments":{"path":"workspace/readme.txt"}}"#,
    )
    .unwrap();
    let context = RequestContext {
        request_id: "benchmark".into(),
        tenant_id: "tenant-demo".into(),
        user_id: "user-demo".into(),
        agent_id: "agent-demo".into(),
        device_id: None,
    };
    let runtime = tokio::runtime::Builder::new_current_thread()
        .enable_all()
        .build()
        .unwrap();
    runtime.block_on(async {
        let now = unix_ms().unwrap();
        for _ in 0..1000 { black_box(gateway.authorize(request.clone(), context.clone(), "1".repeat(32), now).await.unwrap()); }
        let mut samples = Vec::with_capacity(20000);
        for _ in 0..20000 {
            let started = Instant::now();
            black_box(gateway.authorize(request.clone(), context.clone(), "1".repeat(32), now).await.unwrap());
            samples.push(started.elapsed().as_nanos() as u64);
        }
        samples.sort_unstable();
        println!("{}", serde_json::json!({"benchmark":"authorization-core", "iterations":samples.len(), "warmup":1000, "p50Ns":samples[10000], "p95Ns":samples[19000], "p99Ns":samples[19800], "meanNs":samples.iter().sum::<u64>()/samples.len() as u64, "audit":"immediate acknowledgement; no collector IO", "profile":"release", "contracts":olo_toolgate_contracts::ContractSet::current().version}));
    });
    // Fixed signed public vectors keep crypto inputs/time reproducible. Verification
    // is outside the hot path; a 512-rule last-match deny exercises a full scan.
    let vectors: serde_json::Value = serde_json::from_str(include_str!(
        "../../../tests/fixtures/policy/signed-v1.json"
    ))
    .unwrap();
    let source: olo_toolgate_gateway::bundles::BundleSourceConfig = serde_json::from_value(serde_json::json!({
        "url":"http://127.0.0.1:8082/api/control/v1/bundles/current","tenantId":"example","issuer":"control","audience":"gateway",
        "keyringPath":"test-keyring","tokenPath":"test-token","minimumSequence":0,"maxGraceMs":0,"pollIntervalMs":5000,
        "fetchTimeoutMs":3000,"developmentLoopbackHttp":true})).unwrap();
    let verifier = olo_toolgate_gateway::bundles::BundleVerifier::new(
        source,
        serde_json::from_value(vectors["keyring"].clone()).unwrap(),
    )
    .unwrap();
    let fixed_instant = Instant::now();
    let policy = olo_toolgate_gateway::bundles::VerifiedPolicy::with_monotonic_clock(
        verifier,
        std::sync::Arc::new(move || fixed_instant),
    );
    let now = vectors["now"].as_u64().unwrap();
    let input: olo_toolgate_contracts::PolicyInput =
        serde_json::from_value(vectors["input"].clone()).unwrap();
    let start = Instant::now();
    policy
        .adopt(
            &serde_json::to_vec(&vectors["bundles"]["many"]).unwrap(),
            now,
        )
        .unwrap();
    let verification_ns = start.elapsed().as_nanos();
    for _ in 0..1000 {
        let decision = policy.decide(&input, now);
        assert_eq!(
            decision.reason,
            olo_toolgate_contracts::DecisionReason::Matched
        );
        assert_eq!(decision.policy_version, "1.0.1");
        black_box(decision);
    }
    let mut samples = Vec::with_capacity(20000);
    for _ in 0..20000 {
        let start = Instant::now();
        let decision = policy.decide(&input, now);
        assert_eq!(
            decision.reason,
            olo_toolgate_contracts::DecisionReason::Matched
        );
        assert_eq!(decision.policy_version, "1.0.1");
        black_box(decision);
        samples.push(start.elapsed().as_nanos() as u64);
    }
    samples.sort_unstable();
    println!(
        "{}",
        serde_json::json!({"benchmark":"signed-policy-evaluation","rules":512,"iterations":samples.len(),"warmup":1000,
        "p50Ns":samples[10000],"p95Ns":samples[19000],"p99Ns":samples[19800],"coldVerificationNs":verification_ns,
        "meanNs":samples.iter().sum::<u64>()/samples.len() as u64,"profile":"release","includes":"snapshot acquisition, exact-match full scan and decision allocation; excludes extraction, schema/audit IO"})
    );
    // ASK's local path remains a full scan; coordination/signing/audit are excluded.
    use base64::{engine::general_purpose::URL_SAFE_NO_PAD, Engine};
    let vectors: serde_json::Value = serde_json::from_str(include_str!(
        "../../../tests/fixtures/approval/signed-v2.json"
    ))
    .unwrap();
    let bundle = &vectors["bundles"]["many"];
    let payload: olo_toolgate_contracts::ApprovalBundlePayload = serde_json::from_slice(
        &URL_SAFE_NO_PAD
            .decode(bundle["jws"].as_str().unwrap().split('.').nth(1).unwrap())
            .unwrap(),
    )
    .unwrap();
    let compiled: olo_toolgate_contracts::ApprovalCompiledPolicy =
        serde_json::from_slice(&URL_SAFE_NO_PAD.decode(&payload.policy).unwrap()).unwrap();
    assert_eq!(compiled.rules.len(), 512);
    let last = compiled.rules.last().unwrap();
    assert_eq!(last.effect, olo_toolgate_contracts::Decision::Ask);
    let source: olo_toolgate_gateway::bundles::BundleSourceConfig = serde_json::from_value(serde_json::json!({
        "url":"http://127.0.0.1:8082/api/control/v1/bundles/current","tenantId":"http-tenant","issuer":"control","audience":"gateway",
        "keyringPath":"test-keyring","tokenPath":"test-token","minimumSequence":0,"maxGraceMs":0,"pollIntervalMs":5000,
        "fetchTimeoutMs":3000,"developmentLoopbackHttp":true})).unwrap();
    let verifier = olo_toolgate_gateway::bundles::BundleVerifier::new(
        source,
        serde_json::from_value(vectors["keyring"].clone()).unwrap(),
    )
    .unwrap();
    let fixed_instant = Instant::now();
    let policy = olo_toolgate_gateway::bundles::VerifiedPolicy::with_monotonic_clock(
        verifier,
        Arc::new(move || fixed_instant),
    );
    let input = olo_toolgate_contracts::PolicyInput {
        context: RequestContext {
            request_id: "benchmark".into(),
            tenant_id: "http-tenant".into(),
            user_id: last
                .user_ids
                .first()
                .cloned()
                .unwrap_or_else(|| "alice".into()),
            agent_id: last
                .agent_ids
                .first()
                .cloned()
                .unwrap_or_else(|| "agent-demo".into()),
            device_id: last.device_ids.first().cloned(),
        },
        tool_id: last.tool_id.clone(),
        action: last.action.clone(),
        resource: last.resource.clone(),
        arguments_digest: "a".repeat(64),
    };
    let now = vectors["clock"]["nowUnixMs"].as_u64().unwrap();
    let start = Instant::now();
    policy
        .adopt(&serde_json::to_vec(bundle).unwrap(), now)
        .unwrap();
    let verification_ns = start.elapsed().as_nanos();
    for _ in 0..1000 {
        let decision = policy.decide(&input, now);
        assert_eq!(decision.decision, olo_toolgate_contracts::Decision::Ask);
        assert_eq!(decision.policy_version, "2.0.1");
        black_box(decision);
    }
    let mut samples = Vec::with_capacity(20000);
    for _ in 0..20000 {
        let start = Instant::now();
        let decision = policy.decide(&input, now);
        assert_eq!(decision.decision, olo_toolgate_contracts::Decision::Ask);
        assert_eq!(decision.policy_version, "2.0.1");
        black_box(decision);
        samples.push(start.elapsed().as_nanos() as u64);
    }
    samples.sort_unstable();
    println!(
        "{}",
        serde_json::json!({"event":"approval_benchmark","benchmark":"signed-ask-policy-evaluation","rules":512,
        "iterations":samples.len(),"warmup":1000,"p50Ns":samples[10000],"p95Ns":samples[19000],"p99Ns":samples[19800],
        "meanNs":samples.iter().sum::<u64>()/samples.len() as u64,"coldVerificationNs":verification_ns,"profile":"release",
        "includes":"snapshot acquisition, exact-match full scan and decision allocation; excludes extraction, online approval, permit signing, schema/audit IO"})
    );
}
