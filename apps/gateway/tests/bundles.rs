// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
use olo_toolgate_contracts::{Decision, DecisionReason, PolicyInput};
use olo_toolgate_gateway::{
    bundles::{BundleKeyring, BundleSourceConfig, BundleVerifier, VerifiedPolicy},
    policy::PolicyEvaluator,
};
use serde_json::Value;
use std::sync::{Arc, Barrier};

fn vectors() -> Value {
    serde_json::from_str(include_str!(
        "../../../tests/fixtures/policy/signed-v1.json"
    ))
    .unwrap()
}
fn config() -> BundleSourceConfig {
    BundleSourceConfig {
        url: "http://127.0.0.1:8082/api/control/v1/bundles/current".into(),
        tenant_id: "example".into(),
        issuer: "control".into(),
        audience: "gateway".into(),
        keyring_path: "keyring.json".into(),
        token_path: "token".into(),
        minimum_sequence: 0,
        max_grace_ms: 100,
        poll_interval_ms: 100,
        fetch_timeout_ms: 1000,
        development_loopback_http: true,
    }
}
fn policy() -> VerifiedPolicy {
    let instant = std::time::Instant::now();
    VerifiedPolicy::with_monotonic_clock(
        BundleVerifier::new(
            config(),
            serde_json::from_value::<BundleKeyring>(vectors()["keyring"].clone()).unwrap(),
        )
        .unwrap(),
        Arc::new(move || instant),
    )
}

#[test]
fn monotonic_deadline_expires_even_when_wall_clock_stops_or_regresses() {
    use std::sync::atomic::{AtomicU64, Ordering};
    let elapsed = Arc::new(AtomicU64::new(0));
    let clock_elapsed = elapsed.clone();
    let base = std::time::Instant::now();
    let p = VerifiedPolicy::with_monotonic_clock(
        BundleVerifier::new(
            config(),
            serde_json::from_value(vectors()["keyring"].clone()).unwrap(),
        )
        .unwrap(),
        Arc::new(move || {
            base + std::time::Duration::from_millis(clock_elapsed.load(Ordering::Acquire))
        }),
    );
    p.adopt(&bundle("strict"), now()).unwrap();
    elapsed.store(1000, Ordering::Release);
    assert_eq!(p.decide(&input(), now() - 100).decision, Decision::Block);
    elapsed.store(1100, Ordering::Release);
    assert!(!p.ready(now() - 100));
}
fn bundle(name: &str) -> Vec<u8> {
    serde_json::to_vec(&vectors()["bundles"][name]).unwrap()
}
fn now() -> u64 {
    vectors()["now"].as_u64().unwrap()
}
fn input() -> PolicyInput {
    serde_json::from_value(vectors()["input"].clone()).unwrap()
}

#[test]
fn empty_expiry_grace_clock_regression_and_no_revival() {
    let p = policy();
    assert!(!p.ready(now()));
    assert_eq!(
        p.decide(&input(), now()).reason,
        DecisionReason::PolicyUnavailable
    );
    assert!(p.adopt(&bundle("allow"), now()).unwrap());
    assert_eq!(p.decide(&input(), now()).decision, Decision::Allow);
    assert!(p.ready(now() + 20));
    assert_eq!(p.decide(&input(), now() + 10).decision, Decision::Allow);
    assert!(p.ready(now() + 999));
    assert!(p.metrics(now() + 1000).contains("toolgate_bundle_state 2"));
    assert_eq!(p.decide(&input(), now() + 1099).decision, Decision::Allow);
    assert!(!p.ready(now() + 1100));
    assert_eq!(p.decide(&input(), now() + 1100).decision, Decision::Block);
    assert!(p.adopt(&bundle("allow"), now() + 1100).is_err());
    assert!(!p.ready(now() + 100));
    let mut cfg = config();
    cfg.max_grace_ms = 0;
    let p = VerifiedPolicy::new(
        BundleVerifier::new(
            cfg,
            serde_json::from_value(vectors()["keyring"].clone()).unwrap(),
        )
        .unwrap(),
    );
    p.adopt(&bundle("allow"), now()).unwrap();
    assert!(!p.ready(now() + 1000));
}
#[test]
fn grace_requires_explicit_low_risk_classification_and_post_audit_recheck() {
    let p = policy();
    p.adopt(&bundle("strict"), now()).unwrap();
    let fresh = p.decide(&input(), now());
    assert_eq!(fresh.decision, Decision::Allow);
    assert_eq!(p.decide(&input(), now() + 1000).decision, Decision::Block);
    assert!(!p.decision_valid(&fresh, &input(), now() + 1000));
    let p = policy();
    p.adopt(&bundle("allow"), now()).unwrap();
    let grace = p.decide(&input(), now() + 1000);
    assert_eq!(grace.decision, Decision::Allow);
    assert!(p.decision_valid(&grace, &input(), now() + 1099));
    assert!(!p.decision_valid(&grace, &input(), now() + 1100));
}
#[test]
fn invalid_signature_hash_claims_structure_and_semantics_preserve_last_known_good() {
    for (name, b) in vectors()["bundles"].as_object().unwrap() {
        if matches!(
            name.as_str(),
            "allow"
                | "strict"
                | "many"
                | "block"
                | "rollback"
                | "rotated"
                | "precedence"
                | "equivocation"
        ) {
            continue;
        }
        let p = policy();
        p.adopt(&bundle("allow"), now()).unwrap();
        assert!(
            p.adopt(&serde_json::to_vec(b).unwrap(), now()).is_err(),
            "{name}"
        );
        assert_eq!(
            p.decide(&input(), now()).decision,
            Decision::Allow,
            "{name}"
        );
    }
    let p = policy();
    p.adopt(&bundle("allow"), now()).unwrap();
    let mut value = vectors()["bundles"]["allow"].clone();
    let jws = value["jws"].as_str().unwrap();
    let mut altered = jws.as_bytes().to_vec();
    let index = jws.rfind('.').unwrap() + 1;
    altered[index] = if altered[index] == b'A' { b'B' } else { b'A' };
    value["jws"] = String::from_utf8(altered).unwrap().into();
    assert!(p
        .adopt(&serde_json::to_vec(&value).unwrap(), now())
        .is_err());
    for bad in [
        b"{".to_vec(),
        b"null".to_vec(),
        vec![b'x'; 1_500_101],
        b"{\"jws\":\"a.b.c\",\"jws\":\"a.b.d\"}".to_vec(),
    ] {
        assert!(p.adopt(&bad, now()).is_err());
    }
    assert!(p.ready(now()));
}
#[test]
fn monotonic_versions_forward_rollback_and_key_rotation() {
    let p = policy();
    p.adopt(&bundle("allow"), now()).unwrap();
    assert!(!p.adopt(&bundle("allow"), now()).unwrap());
    assert!(p.adopt(&bundle("equivocation"), now()).is_err());
    p.adopt(&bundle("block"), now()).unwrap();
    assert_eq!(p.decide(&input(), now()).decision, Decision::Block);
    assert!(p.adopt(&bundle("allow"), now()).is_err());
    p.adopt(&bundle("rollback"), now()).unwrap();
    assert_eq!(p.decide(&input(), now()).policy_version, "1.0.3");
    p.adopt(&bundle("rotated"), now()).unwrap();
    assert_eq!(p.decide(&input(), now()).decision, Decision::Allow);
    let mut ring: BundleKeyring = serde_json::from_value(vectors()["keyring"].clone()).unwrap();
    ring.keys.remove(1);
    let p = VerifiedPolicy::new(BundleVerifier::new(config(), ring).unwrap());
    assert!(p.adopt(&bundle("rotated"), now()).is_err());
    let mut cfg = config();
    cfg.minimum_sequence = 2;
    let p = VerifiedPolicy::new(
        BundleVerifier::new(
            cfg,
            serde_json::from_value(vectors()["keyring"].clone()).unwrap(),
        )
        .unwrap(),
    );
    assert!(p.adopt(&bundle("allow"), now()).is_err());
    assert!(p.adopt(&bundle("block"), now()).is_ok());
}
#[test]
fn block_precedence_and_exact_identity_resource_tenant() {
    let p = policy();
    p.adopt(&bundle("precedence"), now()).unwrap();
    assert_eq!(p.decide(&input(), now()).decision, Decision::Block);
    let p = policy();
    p.adopt(&bundle("allow"), now()).unwrap();
    let mut i = input();
    i.context.tenant_id = "other".into();
    assert_eq!(p.decide(&i, now()).decision, Decision::Block);
    let mut i = input();
    i.context.user_id = "other".into();
    assert_eq!(p.decide(&i, now()).decision, Decision::Block);
    let mut i = input();
    i.resource.locator.push('x');
    assert_eq!(p.decide(&i, now()).decision, Decision::Block);
}
#[test]
fn replacement_is_atomic_and_inflight_old_allow_cannot_escape_revocation() {
    let p = Arc::new(policy());
    p.adopt(&bundle("allow"), now()).unwrap();
    let old = p.decide(&input(), now());
    let barrier = Arc::new(Barrier::new(3));
    std::thread::scope(|scope| {
        for _ in 0..2 {
            let p = p.clone();
            let barrier = barrier.clone();
            scope.spawn(move || {
                barrier.wait();
                for _ in 0..2000 {
                    let d = p.decide(&input(), now());
                    assert!(matches!(
                        (d.policy_version.as_str(), d.decision),
                        ("1.0.1", Decision::Allow) | ("1.0.2", Decision::Block)
                    ));
                }
            });
        }
        barrier.wait();
        p.adopt(&bundle("block"), now()).unwrap();
    });
    assert!(!p.decision_valid(&old, &input(), now()));
    assert_eq!(p.decide(&input(), now()).decision, Decision::Block);
}
#[test]
fn unsafe_sources_and_invalid_keyrings_reject() {
    for url in [
        "http://control:8082/api/control/v1/bundles/current",
        "https://user:pass@control/api/control/v1/bundles/current",
        "https://169.254.169.254/api/control/v1/bundles/current",
        "https://169.254.1.2/api/control/v1/bundles/current",
        "https://[fe80::1]/api/control/v1/bundles/current",
        "https://0.0.0.0/api/control/v1/bundles/current",
        "https://control/api/control/v1/bundles/current?redirect=evil",
        "https://control/wrong",
    ] {
        let mut c = config();
        c.url = url.into();
        assert!(c.validate().is_err());
    }
    let mut c = config();
    c.development_loopback_http = false;
    assert!(c.validate().is_err());
    let mut ring: BundleKeyring = serde_json::from_value(vectors()["keyring"].clone()).unwrap();
    ring.keys[0].exponent = "Aw".into();
    assert!(BundleVerifier::new(config(), ring).is_err());
    let mut ring: BundleKeyring = serde_json::from_value(vectors()["keyring"].clone()).unwrap();
    use base64::Engine;
    let mut unsupported = vec![128; 257];
    unsupported[256] = 129;
    ring.keys[0].modulus = base64::engine::general_purpose::URL_SAFE_NO_PAD.encode(unsupported);
    assert!(BundleVerifier::new(config(), ring).is_err());
}
