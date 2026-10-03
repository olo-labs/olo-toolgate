// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
use olo_toolgate_client::{deployment::Settings, Failure};
#[cfg(unix)]
use olo_toolgate_client::{deployment::State, storage::ProtectedStore};
use olo_toolgate_contracts::*;
fn fixture() -> serde_json::Value {
    serde_json::from_str(include_str!("../../../tests/fixtures/fleet/v1/signed.json")).unwrap()
}
fn settings() -> Settings {
    let f = fixture();
    Settings {
        release_keys: serde_json::from_value(f["releaseKeys"].clone()).unwrap(),
        organization_keys: serde_json::from_value(f["organizationKeys"].clone()).unwrap(),
    }
}
#[cfg(unix)]
fn identity() -> DeviceIdentity {
    DeviceIdentity {
        device_id: "device".into(),
        tenant_id: "fleet".into(),
        user_id: "owner".into(),
        server_id: "server".into(),
        certificate_pem: String::new(),
        issuer_certificate_pem: String::new(),
        expires_at_unix_ms: 1700001000000,
    }
}
#[test]
fn signatures_hashes_wrong_trust_and_rotation_fail_closed() {
    let settings = settings();
    settings.validate().unwrap();
    let f = fixture();
    let mut release: FleetPackageRelease = serde_json::from_value(f["release"].clone()).unwrap();
    assert_eq!(settings.release(&release).unwrap().package_id, "fleet-demo");
    release.manifest_digest = "0".repeat(64);
    assert_eq!(
        settings.release(&release).unwrap_err(),
        Failure::Unauthorized
    );
    let signed: FleetSignedDocument = serde_json::from_value(f["signedDesired"].clone()).unwrap();
    assert!(settings
        .verify::<FleetPackageDocument>(
            &signed,
            "toolgate-package-release+jws",
            "FleetPackageDocument"
        )
        .is_err());
    let mut changed = signed.clone();
    changed.jws.push('a');
    assert!(settings
        .verify::<FleetDesiredDocument>(
            &changed,
            "toolgate-fleet-desired+jws",
            "FleetDesiredDocument"
        )
        .is_err());
    let mut rotated = settings.clone();
    rotated.organization_keys[0].kid = "unknown".into();
    assert!(rotated
        .verify::<FleetDesiredDocument>(
            &signed,
            "toolgate-fleet-desired+jws",
            "FleetDesiredDocument"
        )
        .is_err());
    let mut shared = settings.clone();
    shared.organization_keys = shared.release_keys.clone();
    assert_eq!(shared.validate(), Err(Failure::Validation));
}
#[cfg(unix)]
#[test]
fn intent_survives_interruption_stale_wrong_assignment_expiry_and_offline() {
    #[cfg(target_os = "macos")]
    let base = std::path::PathBuf::from("/private/tmp");
    #[cfg(not(target_os = "macos"))]
    let base = std::env::temp_dir();
    let path = base.join(format!(
        "fleet-test-{}",
        olo_toolgate_client::identity::nonce().unwrap()
    ));
    let store = ProtectedStore::open(path.clone()).unwrap();
    let settings = settings();
    let mut state = State::open(&store, &settings).unwrap();
    let signed: FleetSignedDocument =
        serde_json::from_value(fixture()["signedDesired"].clone()).unwrap();
    let mut wrong = identity();
    wrong.device_id = "other".into();
    assert!(state
        .accept(&store, &settings, &signed, &wrong, "server", 1700000001000)
        .is_err());
    assert!(state
        .accept(
            &store,
            &settings,
            &signed,
            &identity(),
            "wrong-server",
            1700000001000
        )
        .is_err());
    assert!(state
        .accept(
            &store,
            &settings,
            &signed,
            &identity(),
            "server",
            1700000400000
        )
        .is_err());
    assert!(state
        .accept(
            &store,
            &settings,
            &signed,
            &identity(),
            "server",
            1700000001000
        )
        .unwrap());
    assert_eq!(state.status.packages[0].state, PackageState::Staging);
    assert!(store.read("fleet-active.json").unwrap().is_none());
    let mut recovered = State::open(&store, &settings).unwrap();
    assert_eq!(recovered.status.generation, 3);
    assert!(!recovered.activated);
    assert_eq!(recovered.deadline(), Err(Failure::Unavailable));
    recovered.desired.as_mut().unwrap().generation = 4;
    assert_eq!(
        recovered.accept(
            &store,
            &settings,
            &signed,
            &identity(),
            "server",
            1700000001000
        ),
        Err(Failure::Conflict)
    );
    state.failed(Failure::Unavailable);
    assert_eq!(state.status.packages[0].state, PackageState::Failed);
    assert!(!state.status.ready);
    assert_eq!(state.activate(&store), Err(Failure::Expired));
    assert!(store.read("fleet-active.json").unwrap().is_none());
    // A persistence error cannot retain execution authority from the old assignment.
    state.desired.as_mut().unwrap().generation = 2;
    state.activated = true;
    let intent = path.join("fleet-intent.json");
    std::fs::remove_file(&intent).unwrap();
    std::os::unix::fs::symlink(path.join("missing"), &intent).unwrap();
    assert!(state
        .accept(
            &store,
            &settings,
            &signed,
            &identity(),
            "server",
            1700000001000
        )
        .is_err());
    assert!(!state.activated);
    assert_eq!(path.parent(), Some(base.as_path()));
    std::fs::remove_dir_all(path).unwrap();
}
