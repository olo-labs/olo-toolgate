// Copyright 2026 OLO Labs; SPDX-License-Identifier: Apache-2.0
use http::{HeaderMap, HeaderName, HeaderValue, Method};
use olo_toolgate_trigger_ingress::{Policy, Refusal, Scope, Verifier};

const BODY: &[u8] = b"{\"message\":\"database down\"}\n";
const NOW: u64 = 1_700_000_000;
const SIGNATURE: &str = "v1=fa6fee79b0dc22c18763edf4e32b8e25f6e6b07687b4bc70a899dee3d99bbcaa";

fn verifier() -> Verifier {
    let key: Vec<u8> = (0..32).collect();
    Verifier::new(Policy::new(16384, 300).unwrap(), &[&key]).unwrap()
}
fn headers() -> HeaderMap {
    let mut headers = HeaderMap::new();
    for (name, value) in [
        ("host", "olo.example.test"),
        ("content-type", "application/json"),
        ("olo-delivery-id", "d-1"),
        ("olo-timestamp", "1700000000"),
        ("olo-signature", SIGNATURE),
    ] {
        headers.insert(
            HeaderName::from_bytes(name.as_bytes()).unwrap(),
            HeaderValue::from_str(value).unwrap(),
        );
    }
    headers
}
fn refusal(method: &Method, headers: &HeaderMap, body: &[u8]) -> Option<Refusal> {
    verifier()
        .authenticate_http(
            Scope::new("tenant-a", "binding-a").unwrap(),
            method,
            headers,
            body,
            NOW,
        )
        .err()
}

#[test]
fn valid_http_envelope_keeps_verified_owner_identity_and_exact_body() {
    let headers = headers();
    let result = verifier()
        .authenticate_http(
            Scope::new("tenant-a", "binding-a").unwrap(),
            &Method::POST,
            &headers,
            BODY,
            NOW,
        )
        .unwrap();
    assert_eq!(result.tenant(), "tenant-a");
    assert_eq!(result.binding(), "binding-a");
    assert_eq!(result.delivery_id(), "d-1");
    assert_eq!(result.body(), BODY);
}

#[test]
fn only_post_can_authenticate_even_with_an_otherwise_valid_signature() {
    for method in [
        Method::GET,
        Method::PUT,
        Method::DELETE,
        Method::from_bytes(b"post").unwrap(),
    ] {
        assert_eq!(
            refusal(&method, &headers(), BODY),
            Some(Refusal::MethodNotAllowed)
        );
    }
}

#[test]
fn missing_authentication_fields_are_not_replaced_by_defaults() {
    for name in ["olo-delivery-id", "olo-timestamp", "olo-signature"] {
        let mut headers = headers();
        headers.remove(name);
        assert_eq!(
            refusal(&Method::POST, &headers, BODY),
            Some(Refusal::MalformedRequest)
        );
    }
}

#[test]
fn duplicate_authentication_fields_are_refused_case_insensitively() {
    for name in ["Olo-Delivery-Id", "Olo-Timestamp", "Olo-Signature"] {
        let mut headers = headers();
        headers.append(
            HeaderName::from_bytes(name.as_bytes()).unwrap(),
            HeaderValue::from_static("another-value"),
        );
        assert_eq!(
            refusal(&Method::POST, &headers, BODY),
            Some(Refusal::MalformedRequest)
        );
    }
}

#[test]
fn missing_or_non_json_media_types_and_non_utf8_parameters_are_refused() {
    let mut absent = headers();
    absent.remove("content-type");
    assert_eq!(
        refusal(&Method::POST, &absent, BODY),
        Some(Refusal::UnsupportedMediaType)
    );
    for value in [
        "text/plain",
        "application/json; charset=utf-16",
        "application/json; charset=utf-8; charset=utf-8",
        "application/json; other=value",
        "application/json;",
    ] {
        let mut headers = headers();
        headers.insert("content-type", HeaderValue::from_str(value).unwrap());
        assert_eq!(
            refusal(&Method::POST, &headers, BODY),
            Some(Refusal::UnsupportedMediaType)
        );
    }
}

#[test]
fn json_media_type_and_utf8_parameter_follow_http_case_rules() {
    for value in [
        "Application/JSON",
        "application/json; charset=UTF-8",
        "application/json; charset=\"UTF-8\"",
    ] {
        let mut headers = headers();
        headers.insert("content-type", HeaderValue::from_str(value).unwrap());
        assert_eq!(refusal(&Method::POST, &headers, BODY), None);
    }
}

#[test]
fn duplicated_host_content_type_and_framing_fields_are_refused() {
    for name in [
        "host",
        "content-type",
        "content-length",
        "transfer-encoding",
        "content-encoding",
    ] {
        let mut headers = headers();
        if !headers.contains_key(name) {
            headers.insert(
                HeaderName::from_bytes(name.as_bytes()).unwrap(),
                HeaderValue::from_static("1"),
            );
        }
        headers.append(
            HeaderName::from_bytes(name.as_bytes()).unwrap(),
            HeaderValue::from_static("1"),
        );
        assert_eq!(
            refusal(&Method::POST, &headers, BODY),
            Some(Refusal::MalformedRequest)
        );
    }
}

#[test]
fn content_length_matches_decoded_body_and_cannot_conflict_with_transfer_encoding() {
    let mut valid = headers();
    valid.insert(
        "content-length",
        HeaderValue::from_str(&BODY.len().to_string()).unwrap(),
    );
    assert_eq!(refusal(&Method::POST, &valid, BODY), None);
    valid.insert("transfer-encoding", HeaderValue::from_static("chunked"));
    assert_eq!(
        refusal(&Method::POST, &valid, BODY),
        Some(Refusal::MalformedRequest)
    );
    for length in ["0", "1", "99999", "00027", "+27", "27,27"] {
        let mut headers = headers();
        headers.insert("content-length", HeaderValue::from_str(length).unwrap());
        assert_eq!(
            refusal(&Method::POST, &headers, BODY),
            Some(Refusal::MalformedRequest)
        );
    }
}

#[test]
fn transfer_decoding_is_external_and_unsupported_encodings_are_refused() {
    let mut chunked = headers();
    chunked.insert("transfer-encoding", HeaderValue::from_static("chunked"));
    assert_eq!(refusal(&Method::POST, &chunked, BODY), None);
    chunked.insert(
        "transfer-encoding",
        HeaderValue::from_static("gzip, chunked"),
    );
    assert_eq!(
        refusal(&Method::POST, &chunked, BODY),
        Some(Refusal::MalformedRequest)
    );
    let mut encoded = headers();
    encoded.insert("content-encoding", HeaderValue::from_static("gzip"));
    assert_eq!(
        refusal(&Method::POST, &encoded, BODY),
        Some(Refusal::UnsupportedMediaType)
    );
    encoded.insert("content-encoding", HeaderValue::from_static("identity"));
    assert_eq!(refusal(&Method::POST, &encoded, BODY), None);
}

#[test]
fn header_count_per_value_and_total_metadata_are_bounded() {
    let mut many = headers();
    for index in 0..33 {
        many.insert(
            HeaderName::from_bytes(format!("x-{index}").as_bytes()).unwrap(),
            HeaderValue::from_static("a"),
        );
    }
    assert_eq!(
        refusal(&Method::POST, &many, BODY),
        Some(Refusal::MalformedRequest)
    );
    let mut oversized = headers();
    oversized.insert("x-extra", HeaderValue::from_str(&"a".repeat(1025)).unwrap());
    assert_eq!(
        refusal(&Method::POST, &oversized, BODY),
        Some(Refusal::MalformedRequest)
    );
    let mut total = headers();
    for index in 0..9 {
        total.insert(
            HeaderName::from_bytes(format!("x-{index}").as_bytes()).unwrap(),
            HeaderValue::from_str(&"a".repeat(1024)).unwrap(),
        );
    }
    assert_eq!(
        refusal(&Method::POST, &total, BODY),
        Some(Refusal::MalformedRequest)
    );
}

#[test]
fn non_text_authentication_and_oversized_body_fail_before_handoff() {
    let mut headers = headers();
    headers.insert("olo-signature", HeaderValue::from_bytes(&[255]).unwrap());
    assert_eq!(
        refusal(&Method::POST, &headers, BODY),
        Some(Refusal::MalformedRequest)
    );
    assert_eq!(
        refusal(&Method::POST, &headers, &vec![0; 16385]),
        Some(Refusal::BodyTooLarge)
    );
}

#[test]
fn changed_body_still_requires_valid_cryptographic_authentication() {
    assert_eq!(
        refusal(&Method::POST, &headers(), b"{\"message\":\"tampered\"}"),
        Some(Refusal::Unauthenticated)
    );
}
