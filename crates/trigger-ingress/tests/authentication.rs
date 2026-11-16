// Copyright 2026 OLO Labs; SPDX-License-Identifier: Apache-2.0
use olo_toolgate_trigger_ingress::{Policy, Refusal, Request, Scope, Verifier};
use ring::hmac;

const NOW: u64 = 1_700_000_000;
const BODY: &[u8] = b"{\"message\":\"database down\"}\n";
// Independent Python hmac/hashlib fixture with bytes(range(32)), never a production credential.
const GOLDEN: &str = "v1=fa6fee79b0dc22c18763edf4e32b8e25f6e6b07687b4bc70a899dee3d99bbcaa";
fn key() -> Vec<u8> {
    (0..32).collect()
}
fn verifier() -> Verifier {
    Verifier::new(Policy::new(16_384, 300).unwrap(), &[&key()]).unwrap()
}
fn scope() -> Scope<'static> {
    Scope::new("tenant-a", "binding-a").unwrap()
}
fn request<'a>(signature: &'a str, timestamp: &'a str, body: &'a [u8]) -> Request<'a> {
    Request {
        delivery_id: "d-1",
        timestamp,
        signature,
        body,
    }
}
fn sign(key: &[u8], timestamp: &str, body: &[u8]) -> String {
    let mut message =
        format!("olo.trigger-ingress.v1\ntenant-a\nbinding-a\nd-1\n{timestamp}\n").into_bytes();
    message.extend_from_slice(body);
    let tag = hmac::sign(&hmac::Key::new(hmac::HMAC_SHA256, key), &message);
    format!(
        "v1={}",
        tag.as_ref()
            .iter()
            .map(|byte| format!("{byte:02x}"))
            .collect::<String>()
    )
}

#[test]
fn independent_golden_authenticates_exact_body_and_digest() {
    let result = verifier()
        .authenticate(scope(), request(GOLDEN, "1700000000", BODY), NOW)
        .unwrap();
    assert_eq!(result.tenant(), "tenant-a");
    assert_eq!(result.binding(), "binding-a");
    assert_eq!(result.delivery_id(), "d-1");
    assert_eq!(result.signed_at(), NOW);
    assert_eq!(result.body(), BODY);
    let digest: String = result
        .body_digest()
        .iter()
        .map(|byte| format!("{byte:02x}"))
        .collect();
    assert_eq!(
        digest,
        "a0fdf39f38f0956e518e65f01c7ba75dd34bb020aa8c32a50f2ada31539ebf86"
    );
}

#[test]
fn changed_body_whitespace_or_trailing_newline_invalidates_signature() {
    for body in [
        b"{\"message\": \"database down\"}\n".as_slice(),
        b"{\"message\":\"database up\"}\n".as_slice(),
        b"{\"message\":\"database down\"}".as_slice(),
    ] {
        assert_eq!(
            verifier()
                .authenticate(scope(), request(GOLDEN, "1700000000", body), NOW)
                .err(),
            Some(Refusal::Unauthenticated)
        );
    }
}

#[test]
fn reused_key_does_not_authorize_another_tenant_or_source_binding() {
    for scope in [
        Scope::new("tenant-b", "binding-a").unwrap(),
        Scope::new("tenant-a", "binding-b").unwrap(),
    ] {
        assert_eq!(
            verifier()
                .authenticate(scope, request(GOLDEN, "1700000000", BODY), NOW)
                .err(),
            Some(Refusal::Unauthenticated)
        );
    }
}

#[test]
fn changed_delivery_id_and_fresh_timestamp_are_signed() {
    let mut changed = request(GOLDEN, "1700000000", BODY);
    changed.delivery_id = "d-2";
    assert_eq!(
        verifier().authenticate(scope(), changed, NOW).err(),
        Some(Refusal::Unauthenticated)
    );
    assert_eq!(
        verifier()
            .authenticate(scope(), request(GOLDEN, "1700000001", BODY), NOW)
            .err(),
        Some(Refusal::Unauthenticated)
    );
}

#[test]
fn every_changed_tag_digit_and_a_wrong_key_are_rejected() {
    for index in 3..GOLDEN.len() {
        let mut bytes = GOLDEN.as_bytes().to_vec();
        bytes[index] = if bytes[index] == b'0' { b'1' } else { b'0' };
        let signature = String::from_utf8(bytes).unwrap();
        assert_eq!(
            verifier()
                .authenticate(scope(), request(&signature, "1700000000", BODY), NOW)
                .err(),
            Some(Refusal::Unauthenticated)
        );
    }
    let wrong = Verifier::new(Policy::new(16384, 300).unwrap(), &[&[42; 32]]).unwrap();
    assert_eq!(
        wrong
            .authenticate(scope(), request(GOLDEN, "1700000000", BODY), NOW)
            .err(),
        Some(Refusal::Unauthenticated)
    );
}

#[test]
fn rotation_overlap_accepts_both_keys_and_removal_revokes_old_key() {
    let new_key = [42; 32];
    let signature = sign(&new_key, "1700000000", BODY);
    let rotated = Verifier::new(Policy::new(16384, 300).unwrap(), &[&key(), &new_key]).unwrap();
    for value in [GOLDEN, &signature] {
        assert!(rotated
            .authenticate(scope(), request(value, "1700000000", BODY), NOW)
            .is_ok());
    }
    let revoked = Verifier::new(Policy::new(16384, 300).unwrap(), &[&new_key]).unwrap();
    assert_eq!(
        revoked
            .authenticate(scope(), request(GOLDEN, "1700000000", BODY), NOW)
            .err(),
        Some(Refusal::Unauthenticated)
    );
    assert!(revoked
        .authenticate(scope(), request(&signature, "1700000000", BODY), NOW)
        .is_ok());
}

#[test]
fn freshness_boundary_in_both_directions_is_enforced() {
    for offset in [-300_i64, 300] {
        let timestamp = (NOW as i64 + offset).to_string();
        let signature = sign(&key(), &timestamp, BODY);
        assert!(verifier()
            .authenticate(scope(), request(&signature, &timestamp, BODY), NOW)
            .is_ok());
    }
    for offset in [-301_i64, 301] {
        let timestamp = (NOW as i64 + offset).to_string();
        let signature = sign(&key(), &timestamp, BODY);
        assert_eq!(
            verifier()
                .authenticate(scope(), request(&signature, &timestamp, BODY), NOW)
                .err(),
            Some(Refusal::NotFresh)
        );
    }
}

#[test]
fn timestamp_aliases_and_overflows_are_refused_without_conversion() {
    for timestamp in [
        "",
        "01700000000",
        "+1700000000",
        "1700000000.0",
        "1700000000 ",
        "1700000000\n",
        "-1",
        "9007199254740992",
        "18446744073709551615",
        "not-a-clock",
    ] {
        assert_eq!(
            verifier()
                .authenticate(scope(), request(GOLDEN, timestamp, BODY), NOW)
                .err(),
            Some(Refusal::MalformedRequest)
        );
    }
    assert_eq!(
        verifier()
            .authenticate(scope(), request(GOLDEN, "1700000000", BODY), u64::MAX)
            .err(),
        Some(Refusal::MalformedRequest)
    );
}

#[test]
fn noncanonical_signature_encodings_are_not_accepted_as_aliases() {
    for signature in [
        GOLDEN.to_uppercase(),
        GOLDEN.replace("v1=", "v2="),
        GOLDEN.replace("v1=", "sha256="),
        GOLDEN[3..].to_owned(),
        format!("{GOLDEN} "),
        format!("{GOLDEN}\n"),
        GOLDEN.replace("fa6f", "za6f"),
        "v1=".to_owned(),
    ] {
        assert_eq!(
            verifier()
                .authenticate(scope(), request(&signature, "1700000000", BODY), NOW)
                .err(),
            Some(Refusal::MalformedRequest)
        );
    }
}

#[test]
fn prefix_fields_reject_delimiter_and_unicode_injection() {
    for id in [
        "",
        "tenant\na",
        "tenant\ra",
        "tenant/a",
        "ténant-a",
        &"a".repeat(129),
    ] {
        assert!(Scope::new(id, "binding-a").is_err());
        assert!(Scope::new("tenant-a", id).is_err());
        let mut changed = request(GOLDEN, "1700000000", BODY);
        changed.delivery_id = id;
        assert_eq!(
            verifier().authenticate(scope(), changed, NOW).err(),
            Some(Refusal::MalformedRequest)
        );
    }
}

#[test]
fn full_bound_is_authenticated_without_truncation_and_larger_body_refused() {
    let body = vec![b'a'; 16384];
    let signature = sign(&key(), "1700000000", &body);
    assert_eq!(
        verifier()
            .authenticate(scope(), request(&signature, "1700000000", &body), NOW)
            .unwrap()
            .body(),
        body
    );
    assert_eq!(
        verifier()
            .authenticate(
                scope(),
                request(GOLDEN, "1700000000", &vec![b'a'; 16385]),
                NOW
            )
            .err(),
        Some(Refusal::BodyTooLarge)
    );
}

#[test]
fn invalid_policy_and_key_shapes_are_never_defaulted() {
    for (bytes, seconds) in [(0, 300), (16385, 300), (1024, 0), (1024, 301)] {
        assert!(Policy::new(bytes, seconds).is_err());
    }
    for keys in [
        vec![],
        vec![vec![0; 31]],
        vec![vec![0; 65]],
        vec![vec![0; 32]; 3],
    ] {
        let borrowed: Vec<&[u8]> = keys.iter().map(Vec::as_slice).collect();
        assert!(Verifier::new(Policy::new(1024, 300).unwrap(), &borrowed).is_err());
    }
}

#[test]
fn authentication_alone_is_not_durable_replay_acceptance() {
    let verifier = verifier();
    for _ in 0..2 {
        assert!(verifier
            .authenticate(scope(), request(GOLDEN, "1700000000", BODY), NOW)
            .is_ok());
    }
    // Admission must separately bind the source/delivery id to a durable receipt and payload digest.
}

#[test]
fn diagnostics_do_not_render_source_body_or_credentials() {
    let error = verifier()
        .authenticate(
            scope(),
            request(GOLDEN, "1700000000", b"sensitive-body"),
            NOW,
        )
        .err()
        .unwrap();
    assert_eq!(
        error.to_string(),
        "trigger ingress request refused: Unauthenticated"
    );
}
