//! Shared structural contracts. Validate canonical schemas at input boundaries.
// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
// GENERATED FILE — DO NOT EDIT DIRECTLY; tools/contracts/generate.py
use serde::{Deserialize, Serialize};
/// Server-verified administrator portal session.
#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase", deny_unknown_fields)]
pub struct AdminSession {
    pub role: UserRole,
}
/// Signed format 2 envelope; compilation adds ASK; old readers reject safely.
#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase", deny_unknown_fields)]
pub struct ApprovalBundlePayload {
    pub format_version: u64,
    pub issuer: String,
    pub audience: String,
    pub tenant_id: String,
    pub sequence: u64,
    pub version: String,
    pub directory_revision: u64,
    pub issued_at_unix_ms: u64,
    pub expires_at_unix_ms: u64,
    pub grace_ms: u64,
    pub policy_sha256: String,
    pub policy: String,
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub rollback_of: Option<u64>,
}
/// Format 2 exact rule; BLOCK > ASK > ALLOW. ASK never uses grace.
#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase", deny_unknown_fields)]
pub struct ApprovalBundleRule {
    pub policy_id: String,
    pub user_ids: Vec<String>,
    pub agent_ids: Vec<String>,
    pub device_ids: Vec<String>,
    pub tool_id: String,
    pub action: String,
    pub resource: ResourceDescriptor,
    pub grace_allowed: bool,
    pub effect: Decision,
}
/// Canonical ApprovalChoice wire values.
#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
pub enum ApprovalChoice {
    #[serde(rename = "APPROVE_ONCE")]
    ApproveOnce,
    #[serde(rename = "APPROVE_TEMPORARY")]
    ApproveTemporary,
    #[serde(rename = "DENY")]
    Deny,
}
/// Format 2 adds human approval without weakening default deny.
#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase", deny_unknown_fields)]
pub struct ApprovalCompiledPolicy {
    pub format_version: u64,
    pub rules: Vec<ApprovalBundleRule>,
}
/// Optimistic human decision; duration is required only for temporary approval.
#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase", deny_unknown_fields)]
pub struct ApprovalDecisionRequest {
    pub decision: ApprovalChoice,
    pub expected_revision: u64,
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub duration_ms: Option<u64>,
}
/// Bounded tenant-scoped approval page.
#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase", deny_unknown_fields)]
pub struct ApprovalPage {
    pub items: Vec<ApprovalRecord>,
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub next_cursor: Option<String>,
}
/// Gateway-only atomic lease consumption; repeat use never grants again.
#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase", deny_unknown_fields)]
pub struct ApprovalPermitUse {
    pub approval_id: String,
    pub permit_id: String,
    pub input: PolicyInput,
    pub policy_version: String,
}
/// Durable tenant approval status; identity/resource and digest only, never raw arguments.
#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase", deny_unknown_fields)]
pub struct ApprovalRecord {
    pub id: String,
    pub revision: u64,
    pub state: ApprovalState,
    pub input: PolicyInput,
    pub policy_version: String,
    pub created_at_unix_ms: u64,
    pub expires_at_unix_ms: u64,
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub decided_by: Option<String>,
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub decided_at_unix_ms: Option<u64>,
}
/// Authenticated Control result for this exact Gateway attempt; lease fields occur only on a successful grant.
#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase", deny_unknown_fields)]
pub struct ApprovalResolution {
    pub approval_id: String,
    pub state: ApprovalState,
    pub input: PolicyInput,
    pub policy_version: String,
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub permit_id: Option<String>,
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub permit_expires_at_unix_ms: Option<u64>,
}
/// Canonical ApprovalState wire values.
#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
pub enum ApprovalState {
    #[serde(rename = "PENDING")]
    Pending,
    #[serde(rename = "APPROVED_ONCE")]
    ApprovedOnce,
    #[serde(rename = "APPROVED_TEMPORARY")]
    ApprovedTemporary,
    #[serde(rename = "DENIED")]
    Denied,
    #[serde(rename = "EXPIRED")]
    Expired,
    #[serde(rename = "CONSUMED")]
    Consumed,
}
/// Gateway-only normalized ASK request. No raw arguments or credentials.
#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase", deny_unknown_fields)]
pub struct ApprovalSubmission {
    pub input: PolicyInput,
    pub policy_version: String,
}
/// Immutable artifact identity; digest must be verified by consumers.
#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase", deny_unknown_fields)]
pub struct ArtifactDescriptor {
    pub uri: String,
    pub sha256: String,
    pub size_bytes: u64,
}
/// V2 ASK outcome; pending ASK grants no execution; ALLOW for approved ASK requires a signed permit.
#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase", deny_unknown_fields)]
pub struct AuthorizationOutcome {
    pub decision: PolicyDecision,
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub approval_id: Option<String>,
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub permit: Option<SignedExecutionPermit>,
}
/// Runtime request; principal context and resource identity are derived by the gateway, never asserted by the caller.
#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase", deny_unknown_fields)]
pub struct AuthorizationRequest {
    pub tool_id: String,
    pub action: String,
    pub arguments: std::collections::BTreeMap<String, serde_json::Value>,
}
/// Canonical bounded authoring protocol; declarations never grant execution privileges.
#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase", deny_unknown_fields)]
pub struct BuilderDefinition {
    pub package_id: String,
    pub version: String,
    pub name: String,
    pub description: String,
    pub use_when: String,
    pub do_not_use_when: String,
    pub runtime: ManagedRuntime,
    pub tool: LocalToolRegistration,
    pub platforms: Vec<ClientPlatform>,
    pub architectures: Vec<FleetArchitecture>,
    pub examples: Vec<FleetSelfTest>,
    pub permissions: Vec<BuilderPermission>,
    pub resource: ResourceDescriptor,
    pub credential_requirements: Vec<String>,
}
/// Canonical bounded authoring protocol; declarations never grant execution privileges.
#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase", deny_unknown_fields)]
pub struct BuilderDraft {
    pub id: String,
    pub revision: u64,
    pub definition: BuilderDefinition,
    pub definition_digest: String,
    pub sealed: bool,
    pub package_document: FleetPackageDocument,
}
/// Canonical bounded authoring protocol; declarations never grant execution privileges.
#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase", deny_unknown_fields)]
pub struct BuilderDraftPage {
    pub items: Vec<BuilderDraft>,
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub next_cursor: Option<String>,
}
/// Canonical bounded authoring protocol; declarations never grant execution privileges.
#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase", deny_unknown_fields)]
pub struct BuilderDraftRequest {
    pub id: String,
    pub expected_revision: u64,
    pub definition: BuilderDefinition,
}
/// Canonical BuilderPermission wire values.
#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
pub enum BuilderPermission {
    #[serde(rename = "COMPUTE")]
    Compute,
    #[serde(rename = "FILE_READ")]
    FileRead,
    #[serde(rename = "FILE_WRITE")]
    FileWrite,
    #[serde(rename = "NETWORK")]
    Network,
    #[serde(rename = "CREDENTIALS")]
    Credentials,
}
/// Canonical bounded authoring protocol; declarations never grant execution privileges.
#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase", deny_unknown_fields)]
pub struct BuilderRevisionRequest {
    pub expected_revision: u64,
}
/// Canonical bounded authoring protocol; declarations never grant execution privileges.
#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase", deny_unknown_fields)]
pub struct BuilderTestPage {
    pub items: Vec<BuilderTestRecord>,
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub next_cursor: Option<String>,
}
/// Canonical bounded authoring protocol; declarations never grant execution privileges.
#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase", deny_unknown_fields)]
pub struct BuilderTestPoll {
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub task: Option<FleetSignedDocument>,
}
/// Canonical bounded authoring protocol; declarations never grant execution privileges.
#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase", deny_unknown_fields)]
pub struct BuilderTestRecord {
    pub id: String,
    pub draft_id: String,
    pub definition_digest: String,
    pub device_id: String,
    pub state: BuilderTestState,
    pub revision: u64,
    pub created_at_unix_ms: u64,
    pub expires_at_unix_ms: u64,
    pub attempt: u64,
    pub lease_id: String,
    pub example_index: u64,
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub error: Option<ErrorCode>,
}
/// Canonical bounded authoring protocol; declarations never grant execution privileges.
#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase", deny_unknown_fields)]
pub struct BuilderTestRequest {
    pub id: String,
    pub draft_id: String,
    pub expected_revision: u64,
    pub device_id: String,
    pub example_index: u64,
}
/// Canonical bounded authoring protocol; declarations never grant execution privileges.
#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase", deny_unknown_fields)]
pub struct BuilderTestResult {
    pub job_id: String,
    pub lease_id: String,
    pub definition_digest: String,
    pub success: bool,
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub error: Option<ErrorCode>,
}
/// Canonical BuilderTestState wire values.
#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
pub enum BuilderTestState {
    #[serde(rename = "QUEUED")]
    Queued,
    #[serde(rename = "RUNNING")]
    Running,
    #[serde(rename = "PASSED")]
    Passed,
    #[serde(rename = "FAILED")]
    Failed,
    #[serde(rename = "EXPIRED")]
    Expired,
}
/// Canonical bounded authoring protocol; declarations never grant execution privileges.
#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase", deny_unknown_fields)]
pub struct BuilderTestTask {
    pub format_version: u64,
    pub tenant_id: String,
    pub server_id: String,
    pub device_id: String,
    pub job: BuilderTestRecord,
    pub definition: BuilderDefinition,
    pub expires_at_unix_ms: u64,
}
/// Fixed service tool boundary; validate schema before use.
#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase", deny_unknown_fields)]
pub struct BuiltinInvocation {
    pub tool_id: String,
    pub arguments: std::collections::BTreeMap<String, serde_json::Value>,
}
/// Fixed service tool boundary; validate schema before use.
#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase", deny_unknown_fields)]
pub struct BuiltinIpcRequest {
    pub protocol_version: u64,
    pub request_id: String,
    pub operation: BuiltinOperation,
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub invocation: Option<BuiltinInvocation>,
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub agent_id: Option<String>,
}
/// Fixed service tool boundary; validate schema before use.
#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase", deny_unknown_fields)]
pub struct BuiltinIpcResponse {
    pub request_id: String,
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub tools: Option<Vec<BuiltinToolInfo>>,
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub output: Option<std::collections::BTreeMap<String, serde_json::Value>>,
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub error: Option<ErrorCode>,
}
/// Canonical BuiltinOperation wire values.
#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
pub enum BuiltinOperation {
    #[serde(rename = "CATALOG")]
    Catalog,
    #[serde(rename = "CALL")]
    Call,
}
/// Fixed service tool boundary; validate schema before use.
#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase", deny_unknown_fields)]
pub struct BuiltinToolInfo {
    pub tool_id: String,
    pub action: String,
    pub description: String,
    pub enabled: bool,
    pub input_schema: std::collections::BTreeMap<String, serde_json::Value>,
}
/// Canonical BundleEffect wire values.
#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
pub enum BundleEffect {
    #[serde(rename = "ALLOW")]
    Allow,
    #[serde(rename = "BLOCK")]
    Block,
}
/// Strict JWS protected header; no remote or embedded keys and no algorithm negotiation.
#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase", deny_unknown_fields)]
pub struct BundleHeader {
    pub alg: String,
    pub typ: String,
    pub kid: String,
}
/// Signed version, trust domain, immutable policy bytes and bounded freshness claims.
#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase", deny_unknown_fields)]
pub struct BundlePayload {
    pub format_version: u64,
    pub issuer: String,
    pub audience: String,
    pub tenant_id: String,
    pub sequence: u64,
    pub version: String,
    pub directory_revision: u64,
    pub issued_at_unix_ms: u64,
    pub expires_at_unix_ms: u64,
    pub grace_ms: u64,
    pub policy_sha256: String,
    pub policy: String,
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub rollback_of: Option<u64>,
}
/// Publish a consistent directory snapshot or roll back into a new sequence. Requires Idempotency-Key.
#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase", deny_unknown_fields)]
pub struct BundlePublishRequest {
    pub directory_revision: u64,
    pub expected_sequence: u64,
    pub lifetime_ms: u64,
    pub grace_ms: u64,
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub grace_policy_ids: Option<Vec<String>>,
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub rollback_of: Option<u64>,
}
/// Exact tenant-scoped compiled policy; empty identity dimensions are unrestricted. BLOCK has precedence.
#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase", deny_unknown_fields)]
pub struct BundleRule {
    pub policy_id: String,
    pub user_ids: Vec<String>,
    pub agent_ids: Vec<String>,
    pub device_ids: Vec<String>,
    pub tool_id: String,
    pub action: String,
    pub resource: ResourceDescriptor,
    pub grace_allowed: bool,
    pub effect: BundleEffect,
}
/// Endpoint identity foundation wire model.
#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase", deny_unknown_fields)]
pub struct ClientDiscovery {
    pub protocol_version: u64,
    pub server_id: String,
    pub organization: String,
    pub tenant_id: String,
    pub control_url: String,
    pub gateway_url: String,
    pub verification_uri: String,
    pub minimum_client_version: String,
    pub issued_at_unix_ms: u64,
    pub expires_at_unix_ms: u64,
    pub issuer_certificate_pem: String,
}
/// Fixed service tool boundary; validate schema before use.
#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase", deny_unknown_fields)]
pub struct ClientDownloadArtifact {
    pub platform: ClientPlatform,
    pub target: String,
    pub filename: String,
    pub sha256: String,
    pub bytes: u64,
}
/// Fixed service tool boundary; validate schema before use.
#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase", deny_unknown_fields)]
pub struct ClientDownloadManifest {
    pub version: String,
    pub artifacts: Vec<ClientDownloadArtifact>,
}
/// Device identity and capabilities. Enrollment credentials travel separately.
#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase", deny_unknown_fields)]
pub struct ClientEnrollmentRequest {
    pub device_id: String,
    pub client_version: String,
    pub capabilities: Vec<String>,
}
/// Endpoint identity foundation wire model.
#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase", deny_unknown_fields)]
pub struct ClientHealth {
    pub state: EndpointState,
    pub ready: bool,
    pub uptime_seconds: u64,
    pub successful_check_ins: u64,
    pub failed_check_ins: u64,
    pub report_sequence: u64,
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub last_success_unix_ms: Option<u64>,
}
/// Unsigned interactive system-service installer; signing is a separate release gate.
#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase", deny_unknown_fields)]
pub struct ClientInstallerArtifact {
    pub platform: ClientPlatform,
    pub target: String,
    pub filename: String,
    pub sha256: String,
    pub bytes: u64,
}
/// Fixed service tool boundary; validate schema before use.
#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase", deny_unknown_fields)]
pub struct ClientInstallerManifest {
    pub version: String,
    pub artifacts: Vec<ClientInstallerArtifact>,
}
/// Canonical ClientIpcOperation wire values.
#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
pub enum ClientIpcOperation {
    #[serde(rename = "HEALTH")]
    Health,
    #[serde(rename = "ENROLL")]
    Enroll,
    #[serde(rename = "CHECK_IN")]
    CheckIn,
}
/// Endpoint identity foundation wire model.
#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase", deny_unknown_fields)]
pub struct ClientIpcRequest {
    pub protocol_version: u64,
    pub request_id: String,
    pub operation: ClientIpcOperation,
}
/// Endpoint identity foundation wire model.
#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase", deny_unknown_fields)]
pub struct ClientIpcResponse {
    pub request_id: String,
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub health: Option<ClientHealth>,
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub challenge: Option<EndpointEnrollmentPrompt>,
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub error: Option<ErrorCode>,
}
/// Canonical ClientPlatform wire values.
#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
pub enum ClientPlatform {
    #[serde(rename = "WINDOWS")]
    Windows,
    #[serde(rename = "LINUX")]
    Linux,
    #[serde(rename = "MACOS")]
    Macos,
}
/// Batched report of device inventory and applied desired revision.
#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase", deny_unknown_fields)]
pub struct ClientReport {
    pub device_id: String,
    pub client_version: String,
    pub applied_revision: u64,
    pub packages: Vec<ReportedPackage>,
}
/// Canonical ClientSocketOperation wire values.
#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
pub enum ClientSocketOperation {
    #[serde(rename = "CHECK_IN")]
    CheckIn,
    #[serde(rename = "AUTHORIZE")]
    Authorize,
    #[serde(rename = "RESULT")]
    Result,
    #[serde(rename = "BUILDER_POLL")]
    BuilderPoll,
    #[serde(rename = "BUILDER_RESULT")]
    BuilderResult,
}
/// Correlated status and response; body is validated again as the expected response contract.
#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase", deny_unknown_fields)]
pub struct ClientSocketReply {
    pub request_id: String,
    pub status: u64,
    pub body: std::collections::BTreeMap<String, serde_json::Value>,
}
/// Correlated bounded request; body is validated again as the selected operation contract.
#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase", deny_unknown_fields)]
pub struct ClientSocketRequest {
    pub request_id: String,
    pub operation: ClientSocketOperation,
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub body: Option<std::collections::BTreeMap<String, serde_json::Value>>,
}
/// Version 1 deterministic exact-match rules, with unconditional default deny.
#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase", deny_unknown_fields)]
pub struct CompiledPolicy {
    pub format_version: u64,
    pub rules: Vec<BundleRule>,
}
/// Identifies the shared contract set, independently of product versions.
#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase", deny_unknown_fields)]
pub struct ContractSet {
    pub name: String,
    pub version: String,
}
/// Tenant-scoped agents configuration record. Not a runtime credential or policy grant.
#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase", deny_unknown_fields)]
pub struct ControlAgent {
    pub id: String,
    pub name: String,
    pub enabled: bool,
    pub revision: u64,
    pub owner_user_id: String,
}
/// Cursor page; nextCursor is absent after the last item.
#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase", deny_unknown_fields)]
pub struct ControlAgentPage {
    pub items: Vec<ControlAgent>,
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub next_cursor: Option<String>,
}
/// Append-only mutation metadata; payloads and credentials are excluded.
#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase", deny_unknown_fields)]
pub struct ControlAudit {
    pub sequence: u64,
    pub tenant_id: String,
    pub actor_id: String,
    pub operation: String,
    pub target: String,
    pub revision: u64,
    pub request_id: String,
    pub occurred_at: String,
    pub request_digest: String,
}
/// Cursor page of audit mutation metadata.
#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase", deny_unknown_fields)]
pub struct ControlAuditPage {
    pub items: Vec<ControlAudit>,
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub next_cursor: Option<String>,
}
/// Deterministic import diff without payload or credential material.
#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase", deny_unknown_fields)]
pub struct ControlChange {
    pub kind: ControlEntityKind,
    pub id: String,
    pub operation: ControlChangeKind,
}
/// Canonical ControlChangeKind wire values.
#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
pub enum ControlChangeKind {
    #[serde(rename = "CREATE")]
    Create,
    #[serde(rename = "UPDATE")]
    Update,
    #[serde(rename = "DELETE")]
    Delete,
}
/// Tenant-scoped devices configuration record. Not a runtime credential or policy grant.
#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase", deny_unknown_fields)]
pub struct ControlDevice {
    pub id: String,
    pub name: String,
    pub enabled: bool,
    pub revision: u64,
    pub owner_user_id: String,
}
/// Cursor page; nextCursor is absent after the last item.
#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase", deny_unknown_fields)]
pub struct ControlDevicePage {
    pub items: Vec<ControlDevice>,
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub next_cursor: Option<String>,
}
/// Canonical ControlEntityKind wire values.
#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
pub enum ControlEntityKind {
    #[serde(rename = "USER")]
    User,
    #[serde(rename = "TEAM")]
    Team,
    #[serde(rename = "AGENT")]
    Agent,
    #[serde(rename = "TOOL")]
    Tool,
    #[serde(rename = "POLICY")]
    Policy,
    #[serde(rename = "DEVICE")]
    Device,
    #[serde(rename = "ROLE")]
    Role,
}
/// Canonical ControlImportMode wire values.
#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
pub enum ControlImportMode {
    #[serde(rename = "MERGE")]
    Merge,
    #[serde(rename = "REPLACE")]
    Replace,
}
/// Validate and diff before applying a bounded configuration transaction.
#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase", deny_unknown_fields)]
pub struct ControlImportRequest {
    pub snapshot: ControlSnapshot,
    pub mode: ControlImportMode,
    pub dry_run: bool,
}
/// Import validation/diff result with the current tenant revision.
#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase", deny_unknown_fields)]
pub struct ControlImportResult {
    pub applied: bool,
    pub revision: u64,
    pub changes: Vec<ControlChange>,
}
/// Tenant-scoped policies configuration record. Not a runtime credential or policy grant.
#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase", deny_unknown_fields)]
pub struct ControlPolicy {
    pub id: String,
    pub name: String,
    pub enabled: bool,
    pub revision: u64,
    pub tool_id: String,
    pub action: String,
    pub resource: ResourceDescriptor,
    pub decision: Decision,
    pub user_ids: Vec<String>,
    pub team_ids: Vec<String>,
    pub agent_ids: Vec<String>,
    pub device_ids: Vec<String>,
}
/// Cursor page; nextCursor is absent after the last item.
#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase", deny_unknown_fields)]
pub struct ControlPolicyPage {
    pub items: Vec<ControlPolicy>,
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub next_cursor: Option<String>,
}
/// Named tenant role with fixed templates and bounded permission rules.
#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase", deny_unknown_fields)]
pub struct ControlRole {
    pub id: String,
    pub name: String,
    pub enabled: bool,
    pub revision: u64,
    pub portal_role: UserRole,
    pub rules: RoleRules,
}
/// Cursor page; nextCursor is absent after the last item.
#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase", deny_unknown_fields)]
pub struct ControlRolePage {
    pub items: Vec<ControlRole>,
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub next_cursor: Option<String>,
}
/// Versioned configuration data. Contains no credentials or executable code.
#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase", deny_unknown_fields)]
pub struct ControlSnapshot {
    pub format_version: u64,
    pub tenant_id: String,
    pub revision: u64,
    pub users: Vec<ControlUser>,
    pub teams: Vec<ControlTeam>,
    pub agents: Vec<ControlAgent>,
    pub tools: Vec<ControlTool>,
    pub policies: Vec<ControlPolicy>,
    pub devices: Vec<ControlDevice>,
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub roles: Option<Vec<ControlRole>>,
}
/// Tenant-scoped teams configuration record. Not a runtime credential or policy grant.
#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase", deny_unknown_fields)]
pub struct ControlTeam {
    pub id: String,
    pub name: String,
    pub enabled: bool,
    pub revision: u64,
    pub user_ids: Vec<String>,
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub device_ids: Option<Vec<String>>,
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub role_ids: Option<Vec<String>>,
}
/// Cursor page; nextCursor is absent after the last item.
#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase", deny_unknown_fields)]
pub struct ControlTeamPage {
    pub items: Vec<ControlTeam>,
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub next_cursor: Option<String>,
}
/// Tenant-scoped tools configuration record. Not a runtime credential or policy grant.
#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase", deny_unknown_fields)]
pub struct ControlTool {
    pub id: String,
    pub name: String,
    pub enabled: bool,
    pub revision: u64,
    pub definition: ToolDefinition,
}
/// Cursor page; nextCursor is absent after the last item.
#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase", deny_unknown_fields)]
pub struct ControlToolPage {
    pub items: Vec<ControlTool>,
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub next_cursor: Option<String>,
}
/// Tenant-scoped users configuration record. Not a runtime credential or policy grant.
#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase", deny_unknown_fields)]
pub struct ControlUser {
    pub id: String,
    pub name: String,
    pub enabled: bool,
    pub revision: u64,
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub access: Option<UserAccess>,
}
/// Cursor page; nextCursor is absent after the last item.
#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase", deny_unknown_fields)]
pub struct ControlUserPage {
    pub items: Vec<ControlUser>,
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub next_cursor: Option<String>,
}
/// Canonical Decision wire values.
#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
pub enum Decision {
    #[serde(rename = "ALLOW")]
    Allow,
    #[serde(rename = "ASK")]
    Ask,
    #[serde(rename = "BLOCK")]
    Block,
}
/// Canonical DecisionReason wire values.
#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
pub enum DecisionReason {
    #[serde(rename = "MATCHED")]
    Matched,
    #[serde(rename = "NO_MATCH")]
    NoMatch,
    #[serde(rename = "POLICY_UNAVAILABLE")]
    PolicyUnavailable,
    #[serde(rename = "INVALID_INPUT")]
    InvalidInput,
    #[serde(rename = "APPROVAL_REQUIRED")]
    ApprovalRequired,
}
/// Organization assignment with independent Marketplace evidence; grants no runtime permission.
#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase", deny_unknown_fields)]
pub struct DeploymentAssignment {
    pub assignment_id: String,
    pub device_id: String,
    pub release: MarketplaceRelease,
    pub organization_key_id: String,
    pub organization_signature: String,
    pub desired_presence: bool,
}
/// Versioned desired package assignments reconciled by an endpoint.
#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase", deny_unknown_fields)]
pub struct DesiredState {
    pub device_id: String,
    pub revision: u64,
    pub assignments: Vec<DeploymentAssignment>,
}
/// Endpoint identity foundation wire model.
#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase", deny_unknown_fields)]
pub struct DeviceIdentity {
    pub device_id: String,
    pub tenant_id: String,
    pub user_id: String,
    pub server_id: String,
    pub certificate_pem: String,
    pub issuer_certificate_pem: String,
    pub expires_at_unix_ms: u64,
}
/// Reversible approval decision for an already enrolled key, preserving its owner and directory status.
#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase", deny_unknown_fields)]
pub struct EndpointApprovalRequest {
    pub expected_approval_revision: u64,
    pub approved: bool,
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub connection_expires_at_unix_ms: Option<u64>,
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub unlimited_connection: Option<bool>,
}
/// Endpoint identity foundation wire model.
#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase", deny_unknown_fields)]
pub struct EndpointCheckIn {
    pub sequence: u64,
    pub report: ClientReport,
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub configuration_digest: Option<String>,
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub local_tools: Option<Vec<BuiltinToolInfo>>,
}
/// Endpoint identity foundation wire model.
#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase", deny_unknown_fields)]
pub struct EndpointCheckInAck {
    pub device_id: String,
    pub sequence: u64,
    pub server_time_unix_ms: u64,
    pub next_interval_seconds: u64,
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub next_interval_ms: Option<u64>,
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub identity: Option<DeviceIdentity>,
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub configuration: Option<EndpointPermissionConfiguration>,
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub task: Option<RemoteToolTask>,
}
/// Endpoint identity foundation wire model.
#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase", deny_unknown_fields)]
pub struct EndpointDeviceRecord {
    pub device_id: String,
    pub tenant_id: String,
    pub user_id: String,
    pub key_fingerprint: String,
    pub state: EndpointState,
    pub revision: u64,
    pub last_seen_unix_ms: u64,
    pub report_sequence: u64,
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub report: Option<ClientReport>,
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub connection_expires_at_unix_ms: Option<u64>,
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub connection_approved: Option<bool>,
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub approval_revision: Option<u64>,
}
/// Toggle device directory activation, including a pending device. Zero expects no directory record yet.
#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase", deny_unknown_fields)]
pub struct EndpointEnabledRequest {
    pub expected_revision: u64,
    pub enabled: bool,
}
/// Endpoint identity foundation wire model.
#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase", deny_unknown_fields)]
pub struct EndpointEnrollmentChallenge {
    pub enrollment_id: String,
    pub device_code: String,
    pub user_code: String,
    pub verification_uri: String,
    pub expires_at_unix_ms: u64,
    pub poll_interval_seconds: u64,
}
/// Endpoint identity foundation wire model.
#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase", deny_unknown_fields)]
pub struct EndpointEnrollmentDecision {
    pub user_code: String,
    pub key_fingerprint: String,
    pub choice: EnrollmentChoice,
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub connection_expires_at_unix_ms: Option<u64>,
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub unlimited_connection: Option<bool>,
}
/// All unexpired pending requests in the authenticated tenant; enrollment quota is 32.
#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase", deny_unknown_fields)]
pub struct EndpointEnrollmentPage {
    pub items: Vec<EndpointEnrollmentReview>,
}
/// Endpoint identity foundation wire model.
#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase", deny_unknown_fields)]
pub struct EndpointEnrollmentPoll {
    pub enrollment_id: String,
    pub device_code: String,
}
/// Endpoint identity foundation wire model.
#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase", deny_unknown_fields)]
pub struct EndpointEnrollmentPrompt {
    pub enrollment_id: String,
    pub user_code: String,
    pub verification_uri: String,
    pub expires_at_unix_ms: u64,
    pub poll_interval_seconds: u64,
    pub key_fingerprint: String,
}
/// Endpoint identity foundation wire model.
#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase", deny_unknown_fields)]
pub struct EndpointEnrollmentResult {
    pub state: EnrollmentState,
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub identity: Option<DeviceIdentity>,
}
/// Endpoint identity foundation wire model.
#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase", deny_unknown_fields)]
pub struct EndpointEnrollmentReview {
    pub enrollment_id: String,
    pub user_code: String,
    pub device_id: String,
    pub platform: ClientPlatform,
    pub key_fingerprint: String,
    pub state: EnrollmentState,
    pub expires_at_unix_ms: u64,
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub connection_expires_at_unix_ms: Option<u64>,
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub unlimited_connection: Option<bool>,
}
/// Endpoint identity foundation wire model.
#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase", deny_unknown_fields)]
pub struct EndpointEnrollmentStart {
    pub device_id: String,
    pub client_version: String,
    pub platform: ClientPlatform,
    pub csr_pem: String,
    pub capabilities: Vec<String>,
}
/// Directory identity, enrolled approval and pending request combined for administrative management.
#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase", deny_unknown_fields)]
pub struct EndpointManagedDevice {
    pub device_id: String,
    pub system_executor: bool,
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub directory_device: Option<ControlDevice>,
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub endpoint_device: Option<EndpointDeviceRecord>,
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub enrollment: Option<EndpointEnrollmentReview>,
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub registered_user: Option<ControlUser>,
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub system_executor_kind: Option<SystemExecutorKind>,
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub system_available: Option<bool>,
}
/// All directory devices plus pending requests within the existing directory and enrollment quotas.
#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase", deny_unknown_fields)]
pub struct EndpointManagedDevicePage {
    pub items: Vec<EndpointManagedDevice>,
}
/// Server-cached complete replacement of device permissions, acknowledged by digest on the next authenticated poll.
#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase", deny_unknown_fields)]
pub struct EndpointPermissionConfiguration {
    pub server_id: String,
    pub device_id: String,
    pub user_id: String,
    pub revision: u64,
    pub digest: String,
    pub permissions: Vec<EndpointPermissionRule>,
}
/// Device-owner permission scope used only for local discovery; protected calls still require online authorization.
#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase", deny_unknown_fields)]
pub struct EndpointPermissionRule {
    pub tool_id: String,
    pub action: String,
    pub agent_ids: Vec<String>,
    pub resource: ResourceDescriptor,
    pub decision: Decision,
}
/// Endpoint identity foundation wire model.
#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase", deny_unknown_fields)]
pub struct EndpointRevokeRequest {
    pub expected_revision: u64,
}
/// Canonical EndpointState wire values.
#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
pub enum EndpointState {
    #[serde(rename = "UNENROLLED")]
    Unenrolled,
    #[serde(rename = "PENDING")]
    Pending,
    #[serde(rename = "ACTIVE")]
    Active,
    #[serde(rename = "OFFLINE")]
    Offline,
    #[serde(rename = "REVOKED")]
    Revoked,
}
/// Canonical EnrollmentChoice wire values.
#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
pub enum EnrollmentChoice {
    #[serde(rename = "APPROVE")]
    Approve,
    #[serde(rename = "DENY")]
    Deny,
}
/// Canonical EnrollmentState wire values.
#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
pub enum EnrollmentState {
    #[serde(rename = "PENDING")]
    Pending,
    #[serde(rename = "APPROVED")]
    Approved,
    #[serde(rename = "DENIED")]
    Denied,
    #[serde(rename = "EXPIRED")]
    Expired,
    #[serde(rename = "CONSUMED")]
    Consumed,
}
/// Canonical ErrorCode wire values.
#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
pub enum ErrorCode {
    #[serde(rename = "VALIDATION")]
    Validation,
    #[serde(rename = "UNAUTHORIZED")]
    Unauthorized,
    #[serde(rename = "FORBIDDEN")]
    Forbidden,
    #[serde(rename = "NOT_FOUND")]
    NotFound,
    #[serde(rename = "CONFLICT")]
    Conflict,
    #[serde(rename = "DEPENDENCY_UNAVAILABLE")]
    DependencyUnavailable,
    #[serde(rename = "TIMEOUT")]
    Timeout,
    #[serde(rename = "INTERNAL")]
    Internal,
    #[serde(rename = "UNSUPPORTED")]
    Unsupported,
}
/// Machine-readable error without exception text or caller-controlled detail.
#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase", deny_unknown_fields)]
pub struct ErrorEnvelope {
    pub code: ErrorCode,
    pub request_id: String,
    pub retryable: bool,
}
/// Exact-operation capability; maximum ten seconds; authoritative consume boundary enforces replay.
#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase", deny_unknown_fields)]
pub struct ExecutionPermitClaims {
    pub permit_version: u64,
    pub issuer: String,
    pub audience: String,
    pub tenant_id: String,
    pub user_id: String,
    pub agent_id: String,
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub device_id: Option<String>,
    pub tool_id: String,
    pub action: String,
    pub resource: ResourceDescriptor,
    pub arguments_digest: String,
    pub request_id: String,
    pub policy_version: String,
    pub approval_id: String,
    pub jti: String,
    pub issued_at_unix_ms: u64,
    pub expires_at_unix_ms: u64,
}
/// Dedicated runtime permit trust domain; no key negotiation or remote keys.
#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase", deny_unknown_fields)]
pub struct ExecutionPermitHeader {
    pub alg: String,
    pub typ: String,
    pub kid: String,
}
/// Authenticated runtime request to verify and atomically consume one exact-bound permit.
#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase", deny_unknown_fields)]
pub struct ExecutionPermitUseRequest {
    pub permit: SignedExecutionPermit,
    pub request: AuthorizationRequest,
}
/// Canonical FleetArchitecture wire values.
#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
pub enum FleetArchitecture {
    #[serde(rename = "x86_64")]
    X8664,
    #[serde(rename = "aarch64")]
    Aarch64,
}
/// Signed fleet lifecycle contract; organization deployment trust never grants runtime permission.
#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase", deny_unknown_fields)]
pub struct FleetArtifactGrantClaims {
    pub tenant_id: String,
    pub server_id: String,
    pub device_id: String,
    pub generation: u64,
    pub manifest_digest: String,
    pub size_bytes: u64,
    pub grant_id: String,
    pub issued_at_unix_ms: u64,
    pub expires_at_unix_ms: u64,
}
/// Signed fleet lifecycle contract; organization deployment trust never grants runtime permission.
#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase", deny_unknown_fields)]
pub struct FleetArtifactGrantRequest {
    pub generation: u64,
    pub manifest_digest: String,
}
/// Signed fleet lifecycle contract; organization deployment trust never grants runtime permission.
#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase", deny_unknown_fields)]
pub struct FleetAssignment {
    pub release: FleetPackageRelease,
    pub desired_presence: bool,
}
/// Signed fleet lifecycle contract; organization deployment trust never grants runtime permission.
#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase", deny_unknown_fields)]
pub struct FleetClientStatus {
    pub generation: u64,
    pub ready: bool,
    pub packages: Vec<ReportedPackage>,
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub error: Option<ErrorCode>,
}
/// Signed fleet lifecycle contract; organization deployment trust never grants runtime permission.
#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase", deny_unknown_fields)]
pub struct FleetDesiredDocument {
    pub format_version: u64,
    pub tenant_id: String,
    pub server_id: String,
    pub device_id: String,
    pub generation: u64,
    pub issued_at_unix_ms: u64,
    pub expires_at_unix_ms: u64,
    pub assignments: Vec<FleetAssignment>,
}
/// Signed fleet lifecycle contract; organization deployment trust never grants runtime permission.
#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase", deny_unknown_fields)]
pub struct FleetDesiredSnapshot {
    pub device_id: String,
    pub generation: u64,
    pub assignments: Vec<FleetAssignment>,
}
/// Signed fleet lifecycle contract; organization deployment trust never grants runtime permission.
#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase", deny_unknown_fields)]
pub struct FleetPackageDocument {
    pub format_version: u64,
    pub package_id: String,
    pub version: String,
    pub platforms: Vec<ClientPlatform>,
    pub architectures: Vec<FleetArchitecture>,
    pub minimum_client_version: String,
    pub runtimes: Vec<ManagedRuntime>,
    pub tools: Vec<LocalToolRegistration>,
    pub self_tests: Vec<FleetSelfTest>,
}
/// Signed fleet lifecycle contract; organization deployment trust never grants runtime permission.
#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase", deny_unknown_fields)]
pub struct FleetPackageRelease {
    pub package_id: String,
    pub version: String,
    pub manifest_digest: String,
    pub size_bytes: u64,
    pub release: FleetSignedDocument,
}
/// Signed fleet lifecycle contract; organization deployment trust never grants runtime permission.
#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase", deny_unknown_fields)]
pub struct FleetReleasePage {
    pub items: Vec<FleetPackageRelease>,
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub next_cursor: Option<String>,
}
/// Signed fleet lifecycle contract; organization deployment trust never grants runtime permission.
#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase", deny_unknown_fields)]
pub struct FleetRolloutAdvance {
    pub expected_revision: u64,
    pub percentage: u64,
}
/// Signed fleet lifecycle contract; organization deployment trust never grants runtime permission.
#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase", deny_unknown_fields)]
pub struct FleetRolloutMember {
    pub device_id: String,
    pub generation: u64,
}
/// Signed fleet lifecycle contract; organization deployment trust never grants runtime permission.
#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase", deny_unknown_fields)]
pub struct FleetRolloutPage {
    pub items: Vec<FleetRolloutStatus>,
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub next_cursor: Option<String>,
}
/// Signed fleet lifecycle contract; organization deployment trust never grants runtime permission.
#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase", deny_unknown_fields)]
pub struct FleetRolloutRecord {
    pub id: String,
    pub package_id: String,
    pub version: String,
    pub desired_presence: bool,
    pub percentage: u64,
    pub revision: u64,
    pub created_at_unix_ms: u64,
    pub members: Vec<FleetRolloutMember>,
}
/// Signed fleet lifecycle contract; organization deployment trust never grants runtime permission.
#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase", deny_unknown_fields)]
pub struct FleetRolloutRequest {
    pub id: String,
    pub package_id: String,
    pub version: String,
    pub device_ids: Vec<String>,
    pub desired_presence: bool,
    pub percentage: u64,
}
/// Signed fleet lifecycle contract; organization deployment trust never grants runtime permission.
#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase", deny_unknown_fields)]
pub struct FleetRolloutStatus {
    pub rollout: FleetRolloutRecord,
    pub ready: u64,
    pub failed: u64,
    pub offline: u64,
    pub waiting: u64,
    pub pending: u64,
    pub superseded: u64,
}
/// Signed fleet lifecycle contract; organization deployment trust never grants runtime permission.
#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase", deny_unknown_fields)]
pub struct FleetSelfTest {
    pub tool_id: String,
    pub arguments: std::collections::BTreeMap<String, serde_json::Value>,
    pub expected_output: std::collections::BTreeMap<String, serde_json::Value>,
}
/// Signed fleet lifecycle contract; organization deployment trust never grants runtime permission.
#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase", deny_unknown_fields)]
pub struct FleetSignatureHeader {
    pub alg: String,
    pub typ: String,
    pub kid: String,
}
/// Signed fleet lifecycle contract; organization deployment trust never grants runtime permission.
#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase", deny_unknown_fields)]
pub struct FleetSignedDocument {
    pub jws: String,
}
/// Explicit RSA public trust key; no private material or implicit domain sharing.
#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase", deny_unknown_fields)]
pub struct FleetTrustKey {
    pub kid: String,
    pub n: String,
    pub e: String,
}
/// Bounded per-service execution counters and sandbox state.
#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase", deny_unknown_fields)]
pub struct LocalRuntimeHealth {
    pub ready: bool,
    pub successful_executions: u64,
    pub failed_executions: u64,
    pub runtimes: Vec<LocalRuntimeStatus>,
}
/// OS-authorized additive IPC revision; callers cannot select runtime images, paths or launch arguments.
#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase", deny_unknown_fields)]
pub struct LocalRuntimeIpcRequest {
    pub protocol_version: u64,
    pub request_id: String,
    pub operation: LocalRuntimeOperation,
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub invocation: Option<LocalToolInput>,
}
/// Canonical redacted execution/status response for IPC revision 3.
#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase", deny_unknown_fields)]
pub struct LocalRuntimeIpcResponse {
    pub request_id: String,
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub health: Option<LocalRuntimeHealth>,
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub result: Option<LocalToolOutput>,
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub error: Option<ErrorCode>,
}
/// Canonical LocalRuntimeKind wire values.
#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
pub enum LocalRuntimeKind {
    #[serde(rename = "NATIVE")]
    Native,
    #[serde(rename = "PYTHON")]
    Python,
    #[serde(rename = "NODE")]
    Node,
    #[serde(rename = "POWERSHELL")]
    Powershell,
    #[serde(rename = "BATCH")]
    Batch,
    #[serde(rename = "SHELL")]
    Shell,
    #[serde(rename = "JAVA_JAR")]
    JavaJar,
    #[serde(rename = "DOTNET")]
    Dotnet,
    #[serde(rename = "WASM")]
    Wasm,
}
/// Bounded local sandbox budget; limits never grant host access.
#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase", deny_unknown_fields)]
pub struct LocalRuntimeLimits {
    pub timeout_ms: u64,
    pub memory_mi_b: u64,
    pub max_input_bytes: u64,
    pub max_output_bytes: u64,
}
/// Canonical LocalRuntimeOperation wire values.
#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
pub enum LocalRuntimeOperation {
    #[serde(rename = "STATUS")]
    Status,
    #[serde(rename = "PREPARE")]
    Prepare,
    #[serde(rename = "INVOKE")]
    Invoke,
}
/// Canonical LocalRuntimeState wire values.
#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
pub enum LocalRuntimeState {
    #[serde(rename = "MISSING")]
    Missing,
    #[serde(rename = "READY")]
    Ready,
    #[serde(rename = "FAILED")]
    Failed,
    #[serde(rename = "UNSUPPORTED")]
    Unsupported,
}
/// Redacted runtime readiness and capability, never engine output or credential contents.
#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase", deny_unknown_fields)]
pub struct LocalRuntimeStatus {
    pub runtime_id: String,
    pub kind: LocalRuntimeKind,
    pub state: LocalRuntimeState,
    pub version: String,
}
/// Only enabled local definitions applicable to this verified agent and device.
#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase", deny_unknown_fields)]
pub struct LocalToolCatalog {
    pub tools: Vec<BuiltinToolInfo>,
}
/// One JSON stdin document; arguments never become process command strings.
#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase", deny_unknown_fields)]
pub struct LocalToolInput {
    pub protocol_version: u64,
    pub request_id: String,
    pub tool_id: String,
    pub arguments: std::collections::BTreeMap<String, serde_json::Value>,
}
/// One bounded JSON stdout document; request binding and output schema are verified.
#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase", deny_unknown_fields)]
pub struct LocalToolOutput {
    pub protocol_version: u64,
    pub request_id: String,
    pub output: std::collections::BTreeMap<String, serde_json::Value>,
}
/// Protected local organization registration, separate from marketplace trust and online Gateway authorization.
#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase", deny_unknown_fields)]
pub struct LocalToolRegistration {
    pub tool_id: String,
    pub action: String,
    pub runtime_id: String,
    pub entry_point: String,
    pub input_schema: std::collections::BTreeMap<String, serde_json::Value>,
    pub output_schema: std::collections::BTreeMap<String, serde_json::Value>,
    pub limits: LocalRuntimeLimits,
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub source: Option<LocalToolSource>,
}
/// Canonical bounded authoring protocol; declarations never grant execution privileges.
#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase", deny_unknown_fields)]
pub struct LocalToolSource {
    pub code: String,
    pub sha256: String,
}
/// Administrator-selected immutable tool/runtime image; runtime provisioning is not execution authorization.
#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase", deny_unknown_fields)]
pub struct ManagedRuntime {
    pub id: String,
    pub kind: LocalRuntimeKind,
    pub image: String,
    pub version: String,
}
/// Marketplace trust only; never organization or runtime authorization.
#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase", deny_unknown_fields)]
pub struct MarketplaceRelease {
    pub package_id: String,
    pub version: String,
    pub manifest_digest: String,
    pub marketplace_key_id: String,
    pub marketplace_signature: String,
}
/// Minimum supported contract and client versions.
#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase", deny_unknown_fields)]
pub struct PackageCompatibility {
    pub contracts_version: String,
    pub minimum_client_version: String,
}
/// Portable package metadata. Credential values are forbidden.
#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase", deny_unknown_fields)]
pub struct PackageManifest {
    pub schema_version: u64,
    pub id: String,
    pub version: String,
    pub tools: Vec<ToolDefinition>,
    pub artifacts: Vec<ArtifactDescriptor>,
    pub credential_references: Vec<String>,
    pub compatibility: PackageCompatibility,
}
/// Canonical PackageState wire values.
#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
pub enum PackageState {
    #[serde(rename = "ABSENT")]
    Absent,
    #[serde(rename = "STAGING")]
    Staging,
    #[serde(rename = "READY")]
    Ready,
    #[serde(rename = "FAILED")]
    Failed,
    #[serde(rename = "REVOKED")]
    Revoked,
}
/// Explicit wire decision, with no implicit ALLOW default. ASK requires approval.
#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase", deny_unknown_fields)]
pub struct PolicyDecision {
    pub decision: Decision,
    pub reason: DecisionReason,
    pub policy_version: String,
    pub request_id: String,
}
/// Exact request identity and argument digest supplied to authorization.
#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase", deny_unknown_fields)]
pub struct PolicyInput {
    pub context: RequestContext,
    pub tool_id: String,
    pub action: String,
    pub resource: ResourceDescriptor,
    pub arguments_digest: String,
}
/// A leased client rechecks the exact pending operation online before execution.
#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase", deny_unknown_fields)]
pub struct RemoteToolAuthorization {
    pub request_id: String,
    pub lease_id: String,
    pub request: AuthorizationRequest,
}
/// Fresh remote operation deadline, never a reusable grant.
#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase", deny_unknown_fields)]
pub struct RemoteToolAuthorizationAck {
    pub expires_at_unix_ms: u64,
}
/// Bounded recent local-tool request progress.
#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase", deny_unknown_fields)]
pub struct RemoteToolPage {
    pub items: Vec<RemoteToolRecord>,
}
/// Request progress visible to an administrator without arguments or results.
#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase", deny_unknown_fields)]
pub struct RemoteToolRecord {
    pub request_id: String,
    pub device_id: String,
    pub agent_id: String,
    pub tool_id: String,
    pub state: RemoteToolState,
    pub received_at_unix_ms: u64,
    pub expires_at_unix_ms: u64,
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub submitted_at_unix_ms: Option<u64>,
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub response_at_unix_ms: Option<u64>,
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub completed_at_unix_ms: Option<u64>,
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub error: Option<ErrorCode>,
}
/// Gateway-private queued request/result response.
#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase", deny_unknown_fields)]
pub struct RemoteToolResponse {
    pub record: RemoteToolRecord,
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub result: Option<RemoteToolResult>,
}
/// Exact leased execution response. Failed execution carries a sanitized error code.
#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase", deny_unknown_fields)]
pub struct RemoteToolResult {
    pub request_id: String,
    pub lease_id: String,
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub output: Option<std::collections::BTreeMap<String, serde_json::Value>>,
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub error: Option<ErrorCode>,
}
/// Canonical RemoteToolState wire values.
#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
pub enum RemoteToolState {
    #[serde(rename = "RECEIVED")]
    Received,
    #[serde(rename = "WAITING_FOR_POLL")]
    WaitingForPoll,
    #[serde(rename = "SUBMITTED")]
    Submitted,
    #[serde(rename = "RESPONSE_RECEIVED")]
    ResponseReceived,
    #[serde(rename = "DONE")]
    Done,
    #[serde(rename = "FAILED")]
    Failed,
    #[serde(rename = "EXPIRED")]
    Expired,
}
/// Dedicated Gateway-authenticated request for one device-local tool.
#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase", deny_unknown_fields)]
pub struct RemoteToolSubmission {
    pub input: PolicyInput,
    pub request: AuthorizationRequest,
    pub expires_at_unix_ms: u64,
}
/// Bounded lease delivered only over device-authenticated polling; executable definitions are never accepted from an agent.
#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase", deny_unknown_fields)]
pub struct RemoteToolTask {
    pub request_id: String,
    pub lease_id: String,
    pub input: PolicyInput,
    pub request: AuthorizationRequest,
    pub expires_at_unix_ms: u64,
}
/// Observed package state, distinct from assigned desired state.
#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase", deny_unknown_fields)]
pub struct ReportedPackage {
    pub package_id: String,
    pub version: String,
    pub state: PackageState,
}
/// Safe correlation and principal identifiers; contains no credentials.
#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase", deny_unknown_fields)]
pub struct RequestContext {
    pub request_id: String,
    pub tenant_id: String,
    pub user_id: String,
    pub agent_id: String,
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub device_id: Option<String>,
}
/// Declared resource identity; does not grant access or validate a path.
#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase", deny_unknown_fields)]
pub struct ResourceDescriptor {
    pub kind: ResourceKind,
    pub locator: String,
}
/// Canonical ResourceKind wire values.
#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
pub enum ResourceKind {
    #[serde(rename = "FILE")]
    File,
    #[serde(rename = "URL")]
    Url,
    #[serde(rename = "DATABASE")]
    Database,
    #[serde(rename = "DEVICE")]
    Device,
    #[serde(rename = "CUSTOM")]
    Custom,
}
/// Canonical RoleDeviceScope wire values.
#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
pub enum RoleDeviceScope {
    #[serde(rename = "NONE")]
    None,
    #[serde(rename = "ALL")]
    All,
    #[serde(rename = "GROUPS")]
    Groups,
}
/// Fixed capability templates narrowed by device scope and optional tool allowlist. GROUPS requires nonempty group IDs; NONE and ALL require empty groups, validated by Control. Policies authorize each call.
#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase", deny_unknown_fields)]
pub struct RoleRules {
    pub template_ids: Vec<UserPrivilegeTemplate>,
    pub device_scope: RoleDeviceScope,
    pub device_group_ids: Vec<String>,
    pub tool_ids: Vec<String>,
}
/// Sanitized authorization evaluation event; hashes replace raw arguments and resource locators. A decision is not execution success.
#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase", deny_unknown_fields)]
pub struct RuntimeAuditEvent {
    pub timestamp_unix_ms: u64,
    pub context: RequestContext,
    pub tool_id: String,
    pub action: String,
    pub resource_digest: String,
    pub arguments_digest: String,
    pub decision: PolicyDecision,
    pub trace_id: String,
}
/// Endpoint identity foundation wire model.
#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase", deny_unknown_fields)]
pub struct SignedClientDiscovery {
    pub payload: String,
    pub signature: String,
}
/// RS256 compact JWS; structural validation alone does not establish trust.
#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase", deny_unknown_fields)]
pub struct SignedExecutionPermit {
    pub jws: String,
}
/// RFC 7515 compact JWS; payload and hash must both verify before adoption.
#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase", deny_unknown_fields)]
pub struct SignedPolicyBundle {
    pub jws: String,
}
/// Canonical SystemExecutorKind wire values.
#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
pub enum SystemExecutorKind {
    #[serde(rename = "BUILTINS")]
    Builtins,
    #[serde(rename = "HOTFOLDER")]
    Hotfolder,
    #[serde(rename = "REST_FORWARDING")]
    RestForwarding,
}
/// Named operation and declared resource kinds.
#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase", deny_unknown_fields)]
pub struct ToolAction {
    pub name: String,
    pub resource_kinds: Vec<ResourceKind>,
}
/// Capability definition. JSON schemas are data, never executable code.
#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase", deny_unknown_fields)]
pub struct ToolDefinition {
    pub id: String,
    pub name: String,
    pub description: String,
    pub actions: Vec<ToolAction>,
    pub input_schema: std::collections::BTreeMap<String, serde_json::Value>,
    pub output_schema: std::collections::BTreeMap<String, serde_json::Value>,
}
/// Tenant-scoped role and privilege assignment. Device groups are directory teams containing device IDs.
#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase", deny_unknown_fields)]
pub struct UserAccess {
    pub role: UserRole,
    pub template_ids: Vec<UserPrivilegeTemplate>,
    pub device_group_ids: Vec<String>,
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub role_ids: Option<Vec<String>>,
}
/// Canonical UserPrivilegeTemplate wire values.
#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
pub enum UserPrivilegeTemplate {
    #[serde(rename = "TOOL_USER")]
    ToolUser,
    #[serde(rename = "IT_CLOUD_ADMIN")]
    ItCloudAdmin,
    #[serde(rename = "APPROVER")]
    Approver,
}
/// Canonical UserRole wire values.
#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
pub enum UserRole {
    #[serde(rename = "BASIC")]
    Basic,
    #[serde(rename = "ADMINISTRATOR")]
    Administrator,
    #[serde(rename = "SUPER_ADMIN")]
    SuperAdmin,
}
impl ContractSet {
    /// Current canonical contract set.
    pub fn current() -> Self {
        Self {
            name: "olo-toolgate-contracts".into(),
            version: "0.10.0-dev".into(),
        }
    }
}
/// Embedded canonical schemas for offline boundary validation.
pub const CANONICAL_SCHEMAS: &[(&str, &str)] = &[
    (
        "https://schemas.ololabs.io/toolgate/v1/approval.schema.json",
        include_str!("../schemas/v1/approval.schema.json"),
    ),
    (
        "https://schemas.ololabs.io/toolgate/v1/builder.schema.json",
        include_str!("../schemas/v1/builder.schema.json"),
    ),
    (
        "https://schemas.ololabs.io/toolgate/v1/builtins.schema.json",
        include_str!("../schemas/v1/builtins.schema.json"),
    ),
    (
        "https://schemas.ololabs.io/toolgate/v1/bundle.schema.json",
        include_str!("../schemas/v1/bundle.schema.json"),
    ),
    (
        "https://schemas.ololabs.io/toolgate/v1/client.schema.json",
        include_str!("../schemas/v1/client.schema.json"),
    ),
    (
        "https://schemas.ololabs.io/toolgate/v1/common.schema.json",
        include_str!("../schemas/v1/common.schema.json"),
    ),
    (
        "https://schemas.ololabs.io/toolgate/v1/control.schema.json",
        include_str!("../schemas/v1/control.schema.json"),
    ),
    (
        "https://schemas.ololabs.io/toolgate/v1/deployment.schema.json",
        include_str!("../schemas/v1/deployment.schema.json"),
    ),
    (
        "https://schemas.ololabs.io/toolgate/v1/endpoint.schema.json",
        include_str!("../schemas/v1/endpoint.schema.json"),
    ),
    (
        "https://schemas.ololabs.io/toolgate/v1/error.schema.json",
        include_str!("../schemas/v1/error.schema.json"),
    ),
    (
        "https://schemas.ololabs.io/toolgate/v1/execution.schema.json",
        include_str!("../schemas/v1/execution.schema.json"),
    ),
    (
        "https://schemas.ololabs.io/toolgate/v1/fleet.schema.json",
        include_str!("../schemas/v1/fleet.schema.json"),
    ),
    (
        "https://schemas.ololabs.io/toolgate/v1/identifiers.schema.json",
        include_str!("../schemas/v1/identifiers.schema.json"),
    ),
    (
        "https://schemas.ololabs.io/toolgate/v1/package.schema.json",
        include_str!("../schemas/v1/package.schema.json"),
    ),
    (
        "https://schemas.ololabs.io/toolgate/v1/policy.schema.json",
        include_str!("../schemas/v1/policy.schema.json"),
    ),
    (
        "https://schemas.ololabs.io/toolgate/v1/resource.schema.json",
        include_str!("../schemas/v1/resource.schema.json"),
    ),
    (
        "https://schemas.ololabs.io/toolgate/v1/runtime.schema.json",
        include_str!("../schemas/v1/runtime.schema.json"),
    ),
    (
        "https://schemas.ololabs.io/toolgate/v1/tool.schema.json",
        include_str!("../schemas/v1/tool.schema.json"),
    ),
];
