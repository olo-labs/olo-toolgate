//! Shared structural contracts. Validate canonical schemas at input boundaries.
// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
// GENERATED FILE — DO NOT EDIT DIRECTLY; tools/contracts/generate.py
use serde::{Deserialize, Serialize};
/// Immutable artifact identity; digest must be verified by consumers.
#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase", deny_unknown_fields)]
pub struct ArtifactDescriptor {
    pub uri: String,
    pub sha256: String,
    pub size_bytes: u64,
}
/// Device identity and capabilities. Enrollment credentials travel separately.
#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase", deny_unknown_fields)]
pub struct ClientEnrollmentRequest {
    pub device_id: String,
    pub client_version: String,
    pub capabilities: Vec<String>,
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
/// Identifies the shared contract set, independently of product versions.
#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase", deny_unknown_fields)]
pub struct ContractSet {
    pub name: String,
    pub version: String,
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
            version: "0.1.0-dev".into(),
        }
    }
}
