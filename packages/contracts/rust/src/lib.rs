//! Shared structural contracts. Validate canonical schemas at input boundaries.
// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
// GENERATED FILE — DO NOT EDIT DIRECTLY; tools/contracts/generate.py
use serde::{Deserialize, Serialize};
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
/// Endpoint identity foundation wire model.
#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase", deny_unknown_fields)]
pub struct EndpointCheckIn {
    pub sequence: u64,
    pub report: ClientReport,
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
    pub identity: Option<DeviceIdentity>,
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
impl ContractSet {
    /// Current canonical contract set.
    pub fn current() -> Self {
        Self {
            name: "olo-toolgate-contracts".into(),
            version: "0.6.0-dev".into(),
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
