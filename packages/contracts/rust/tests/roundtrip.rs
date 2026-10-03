// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
use olo_toolgate_contracts::*;
use serde_json::Value;

#[test]
fn shared_fixture_round_trips() {
    let fixtures: Value = serde_json::from_str(include_str!(
        "../../../../tests/fixtures/contracts/v1/valid.json"
    ))
    .unwrap();
    macro_rules! check {
        ($($name:ident),+ $(,)?) => {
            $(
                let json = fixtures[stringify!($name)].clone();
                let model: $name = serde_json::from_value(json.clone()).unwrap();
                assert_eq!(json, serde_json::to_value(model).unwrap(), stringify!($name));
            )+
        };
    }
    check!(
        ContractSet,
        RequestContext,
        ErrorCode,
        ErrorEnvelope,
        ResourceKind,
        ResourceDescriptor,
        ToolAction,
        ToolDefinition,
        Decision,
        DecisionReason,
        PolicyInput,
        PolicyDecision,
        PackageState,
        ReportedPackage,
        PackageCompatibility,
        ArtifactDescriptor,
        MarketplaceRelease,
        DeploymentAssignment,
        DesiredState,
        ClientEnrollmentRequest,
        ClientReport,
        PackageManifest,
        AuthorizationRequest,
        RuntimeAuditEvent,
        ControlUser,
        ControlUserPage,
        ControlTeam,
        ControlTeamPage,
        ControlAgent,
        ControlAgentPage,
        ControlTool,
        ControlToolPage,
        ControlPolicy,
        ControlPolicyPage,
        ControlDevice,
        ControlDevicePage,
        ControlSnapshot,
        ControlImportMode,
        ControlImportRequest,
        ControlChangeKind,
        ControlEntityKind,
        ControlChange,
        ControlImportResult,
        ControlAudit,
        ControlAuditPage,
        BundleRule,
        BundleEffect,
        CompiledPolicy,
        BundleHeader,
        BundlePayload,
        SignedPolicyBundle,
        BundlePublishRequest,
        ApprovalState,
        ApprovalChoice,
        ApprovalSubmission,
        ApprovalDecisionRequest,
        ApprovalRecord,
        ApprovalPage,
        ApprovalResolution,
        ApprovalPermitUse,
        ExecutionPermitHeader,
        ExecutionPermitClaims,
        SignedExecutionPermit,
        ExecutionPermitUseRequest,
        AuthorizationOutcome,
        ApprovalBundleRule,
        ApprovalCompiledPolicy,
        ApprovalBundlePayload,
        ClientPlatform,
        EnrollmentState,
        EndpointState,
        EnrollmentChoice,
        ClientDiscovery,
        SignedClientDiscovery,
        EndpointEnrollmentStart,
        EndpointEnrollmentChallenge,
        EndpointEnrollmentPoll,
        EndpointEnrollmentReview,
        EndpointEnrollmentDecision,
        DeviceIdentity,
        EndpointEnrollmentResult,
        EndpointCheckIn,
        EndpointCheckInAck,
        EndpointDeviceRecord,
        EndpointRevokeRequest,
        ClientHealth,
        ClientIpcOperation,
        ClientIpcRequest,
        ClientIpcResponse,
        EndpointEnrollmentPrompt,
    );
    assert_eq!(ContractSet::current().version, env!("CARGO_PKG_VERSION"));
    assert_eq!(ContractSet::current().name, "olo-toolgate-contracts");
}

#[test]
fn unknown_or_missing_security_fields_are_rejected() {
    assert!(serde_json::from_str::<Decision>("\"UNKNOWN\"").is_err());
    let valid =
        r#"{"decision":"BLOCK","reason":"NO_MATCH","policyVersion":"0.1.0-dev","requestId":"r"}"#;
    let mut value: Value = serde_json::from_str(valid).unwrap();
    value["bypass"] = Value::Bool(true);
    assert!(serde_json::from_value::<PolicyDecision>(value.clone()).is_err());
    value.as_object_mut().unwrap().remove("bypass");
    value.as_object_mut().unwrap().remove("decision");
    assert!(serde_json::from_value::<PolicyDecision>(value).is_err());
    assert!(serde_json::from_str::<ErrorEnvelope>(
        r#"{"code":"INTERNAL","requestId":"r","retryable":null}"#
    )
    .is_err());
}
