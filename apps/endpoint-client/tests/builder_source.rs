// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
use olo_toolgate_client::{execution::source, Failure};
use olo_toolgate_contracts::*;
use serde_json::Value;

fn tool() -> LocalToolRegistration {
    let fixtures: Value = serde_json::from_str(include_str!(
        "../../../tests/fixtures/contracts/v1/valid.json"
    ))
    .unwrap();
    serde_json::from_value(fixtures["BuilderDefinition"]["tool"].clone()).unwrap()
}
#[test]
fn source_integrity_runtime_and_budget_fail_closed() {
    let mut registration = tool();
    source::validate(&LocalRuntimeKind::Python, &registration).unwrap();
    assert_eq!(
        source::validate(&LocalRuntimeKind::Native, &registration),
        Err(Failure::Unsupported)
    );
    registration.source.as_mut().unwrap().code.push(' ');
    assert_eq!(
        source::validate(&LocalRuntimeKind::Python, &registration),
        Err(Failure::Validation)
    );
    registration = tool();
    registration.limits.max_input_bytes = 8193;
    assert_eq!(
        source::validate(&LocalRuntimeKind::Python, &registration),
        Err(Failure::Validation)
    );
}
#[test]
fn author_code_is_stdin_data_and_cannot_change_host_arguments() {
    let registration = tool();
    let invocation =
        br#"{"protocolVersion":1,"requestId":"request","arguments":{"text":"; $(touch /escape)"}}"#;
    let (framed, limits) = source::input(&registration, invocation).unwrap();
    let lines: Vec<_> = framed.split(|byte| *byte == b'\n').collect();
    assert!(lines[0].iter().all(u8::is_ascii_hexdigit));
    assert_eq!(lines[1], invocation);
    assert_eq!(
        limits.max_input_bytes,
        registration.limits.max_input_bytes + 16386
    );
    for kind in [
        LocalRuntimeKind::Python,
        LocalRuntimeKind::Node,
        LocalRuntimeKind::Powershell,
        LocalRuntimeKind::Shell,
    ] {
        let command = source::command(&kind).unwrap();
        assert!(!command
            .iter()
            .any(|argument| argument.contains(&registration.source.as_ref().unwrap().code)));
        assert_eq!(&command[..2], &["/usr/bin/env", "-i"]);
    }
}

#[test]
fn maximum_source_and_input_fit_the_separate_transport_budget() {
    let mut registration = tool();
    registration.limits.max_input_bytes = 8192;
    let source = registration.source.as_mut().unwrap();
    source.code = "x".repeat(8192);
    source.sha256 = olo_toolgate_client::digest(source.code.as_bytes());
    source::validate(&LocalRuntimeKind::Python, &registration).unwrap();
    let input = vec![b' '; 8192];
    let (framed, limits) = source::input(&registration, &input).unwrap();
    assert!(framed.len() as u64 <= limits.max_input_bytes);
    registration.source = None;
    let (framed, limits) = source::input(&registration, &input).unwrap();
    assert!(framed.len() as u64 <= limits.max_input_bytes);
}

#[test]
fn signed_lease_binds_authority_device_server_expiry_and_permissions() {
    let fixture: Value = serde_json::from_str(include_str!(
        "../../../tests/fixtures/builder/v1/signed.json"
    ))
    .unwrap();
    let settings = olo_toolgate_client::deployment::Settings {
        organization_keys: serde_json::from_value(fixture["organizationKeys"].clone()).unwrap(),
        release_keys: serde_json::from_value(fixture["releaseKeys"].clone()).unwrap(),
    };
    let identity = DeviceIdentity {
        device_id: "device-1".into(),
        tenant_id: "tenant".into(),
        user_id: "owner".into(),
        server_id: "server".into(),
        certificate_pem: String::new(),
        issuer_certificate_pem: fixture["issuerCertificatePem"].as_str().unwrap().into(),
        expires_at_unix_ms: 1700001000000,
    };
    let signed: FleetSignedDocument = serde_json::from_value(fixture["valid"].clone()).unwrap();
    olo_toolgate_client::builder::verify(&settings, &signed, &identity, "server", 1700000001000)
        .unwrap();
    assert!(olo_toolgate_client::builder::verify(
        &settings,
        &signed,
        &identity,
        "wrong-server",
        1700000001000
    )
    .is_err());
    assert!(olo_toolgate_client::builder::verify(
        &settings,
        &signed,
        &identity,
        "server",
        1700000060000
    )
    .is_err());
    let mut wrong = identity.clone();
    wrong.device_id = "another-device".into();
    assert!(olo_toolgate_client::builder::verify(
        &settings,
        &signed,
        &wrong,
        "server",
        1700000001000
    )
    .is_err());
    for label in [
        "wrongAuthority",
        "permission",
        "credential",
        "queued",
        "resource",
    ] {
        let negative: FleetSignedDocument = serde_json::from_value(fixture[label].clone()).unwrap();
        assert!(
            olo_toolgate_client::builder::verify(
                &settings,
                &negative,
                &identity,
                "server",
                1700000001000
            )
            .is_err(),
            "accepted {label}"
        );
    }
    let mut tampered = signed;
    tampered.jws.push('x');
    assert!(olo_toolgate_client::builder::verify(
        &settings,
        &tampered,
        &identity,
        "server",
        1700000001000
    )
    .is_err());
}
