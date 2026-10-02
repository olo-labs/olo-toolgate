// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
// GENERATED FILE — DO NOT EDIT DIRECTLY; tools/contracts/generate.py
/** Immutable artifact identity; digest must be verified by consumers. */
export interface ArtifactDescriptor {
  readonly uri: string;
  readonly sha256: string;
  readonly sizeBytes: number;
}
/** Runtime request; principal context and resource identity are derived by the gateway, never asserted by the caller. */
export interface AuthorizationRequest {
  readonly toolId: string;
  readonly action: string;
  readonly arguments: Record<string, unknown>;
}
/** Device identity and capabilities. Enrollment credentials travel separately. */
export interface ClientEnrollmentRequest {
  readonly deviceId: string;
  readonly clientVersion: string;
  readonly capabilities: ReadonlyArray<string>;
}
/** Batched report of device inventory and applied desired revision. */
export interface ClientReport {
  readonly deviceId: string;
  readonly clientVersion: string;
  readonly appliedRevision: number;
  readonly packages: ReadonlyArray<ReportedPackage>;
}
/** Identifies the shared contract set, independently of product versions. */
export interface ContractSet {
  readonly name: string;
  readonly version: string;
}
/** Tenant-scoped agents configuration record. Not a runtime credential or policy grant. */
export interface ControlAgent {
  readonly id: string;
  readonly name: string;
  readonly enabled: boolean;
  readonly revision: number;
  readonly ownerUserId: string;
}
/** Cursor page; nextCursor is absent after the last item. */
export interface ControlAgentPage {
  readonly items: ReadonlyArray<ControlAgent>;
  readonly nextCursor?: string;
}
/** Append-only mutation metadata; payloads and credentials are excluded. */
export interface ControlAudit {
  readonly sequence: number;
  readonly tenantId: string;
  readonly actorId: string;
  readonly operation: string;
  readonly target: string;
  readonly revision: number;
  readonly requestId: string;
  readonly occurredAt: string;
  readonly requestDigest: string;
}
/** Cursor page of audit mutation metadata. */
export interface ControlAuditPage {
  readonly items: ReadonlyArray<ControlAudit>;
  readonly nextCursor?: string;
}
/** Deterministic import diff without payload or credential material. */
export interface ControlChange {
  readonly kind: ControlEntityKind;
  readonly id: string;
  readonly operation: ControlChangeKind;
}
export type ControlChangeKind = "CREATE" | "UPDATE" | "DELETE";
/** Tenant-scoped devices configuration record. Not a runtime credential or policy grant. */
export interface ControlDevice {
  readonly id: string;
  readonly name: string;
  readonly enabled: boolean;
  readonly revision: number;
  readonly ownerUserId: string;
}
/** Cursor page; nextCursor is absent after the last item. */
export interface ControlDevicePage {
  readonly items: ReadonlyArray<ControlDevice>;
  readonly nextCursor?: string;
}
export type ControlEntityKind = "USER" | "TEAM" | "AGENT" | "TOOL" | "POLICY" | "DEVICE";
export type ControlImportMode = "MERGE" | "REPLACE";
/** Validate and diff before applying a bounded configuration transaction. */
export interface ControlImportRequest {
  readonly snapshot: ControlSnapshot;
  readonly mode: ControlImportMode;
  readonly dryRun: boolean;
}
/** Import validation/diff result with the current tenant revision. */
export interface ControlImportResult {
  readonly applied: boolean;
  readonly revision: number;
  readonly changes: ReadonlyArray<ControlChange>;
}
/** Tenant-scoped policies configuration record. Not a runtime credential or policy grant. */
export interface ControlPolicy {
  readonly id: string;
  readonly name: string;
  readonly enabled: boolean;
  readonly revision: number;
  readonly toolId: string;
  readonly action: string;
  readonly resource: ResourceDescriptor;
  readonly decision: Decision;
  readonly userIds: ReadonlyArray<string>;
  readonly teamIds: ReadonlyArray<string>;
  readonly agentIds: ReadonlyArray<string>;
  readonly deviceIds: ReadonlyArray<string>;
}
/** Cursor page; nextCursor is absent after the last item. */
export interface ControlPolicyPage {
  readonly items: ReadonlyArray<ControlPolicy>;
  readonly nextCursor?: string;
}
/** Versioned configuration data. Contains no credentials or executable code. */
export interface ControlSnapshot {
  readonly formatVersion: number;
  readonly tenantId: string;
  readonly revision: number;
  readonly users: ReadonlyArray<ControlUser>;
  readonly teams: ReadonlyArray<ControlTeam>;
  readonly agents: ReadonlyArray<ControlAgent>;
  readonly tools: ReadonlyArray<ControlTool>;
  readonly policies: ReadonlyArray<ControlPolicy>;
  readonly devices: ReadonlyArray<ControlDevice>;
}
/** Tenant-scoped teams configuration record. Not a runtime credential or policy grant. */
export interface ControlTeam {
  readonly id: string;
  readonly name: string;
  readonly enabled: boolean;
  readonly revision: number;
  readonly userIds: ReadonlyArray<string>;
}
/** Cursor page; nextCursor is absent after the last item. */
export interface ControlTeamPage {
  readonly items: ReadonlyArray<ControlTeam>;
  readonly nextCursor?: string;
}
/** Tenant-scoped tools configuration record. Not a runtime credential or policy grant. */
export interface ControlTool {
  readonly id: string;
  readonly name: string;
  readonly enabled: boolean;
  readonly revision: number;
  readonly definition: ToolDefinition;
}
/** Cursor page; nextCursor is absent after the last item. */
export interface ControlToolPage {
  readonly items: ReadonlyArray<ControlTool>;
  readonly nextCursor?: string;
}
/** Tenant-scoped users configuration record. Not a runtime credential or policy grant. */
export interface ControlUser {
  readonly id: string;
  readonly name: string;
  readonly enabled: boolean;
  readonly revision: number;
}
/** Cursor page; nextCursor is absent after the last item. */
export interface ControlUserPage {
  readonly items: ReadonlyArray<ControlUser>;
  readonly nextCursor?: string;
}
export type Decision = "ALLOW" | "ASK" | "BLOCK";
export type DecisionReason = "MATCHED" | "NO_MATCH" | "POLICY_UNAVAILABLE" | "INVALID_INPUT" | "APPROVAL_REQUIRED";
/** Organization assignment with independent Marketplace evidence; grants no runtime permission. */
export interface DeploymentAssignment {
  readonly assignmentId: string;
  readonly deviceId: string;
  readonly release: MarketplaceRelease;
  readonly organizationKeyId: string;
  readonly organizationSignature: string;
  readonly desiredPresence: boolean;
}
/** Versioned desired package assignments reconciled by an endpoint. */
export interface DesiredState {
  readonly deviceId: string;
  readonly revision: number;
  readonly assignments: ReadonlyArray<DeploymentAssignment>;
}
export type ErrorCode = "VALIDATION" | "UNAUTHORIZED" | "FORBIDDEN" | "NOT_FOUND" | "CONFLICT" | "DEPENDENCY_UNAVAILABLE" | "TIMEOUT" | "INTERNAL" | "UNSUPPORTED";
/** Machine-readable error without exception text or caller-controlled detail. */
export interface ErrorEnvelope {
  readonly code: ErrorCode;
  readonly requestId: string;
  readonly retryable: boolean;
}
export type Identifier = string;
/** Marketplace trust only; never organization or runtime authorization. */
export interface MarketplaceRelease {
  readonly packageId: string;
  readonly version: string;
  readonly manifestDigest: string;
  readonly marketplaceKeyId: string;
  readonly marketplaceSignature: string;
}
/** Minimum supported contract and client versions. */
export interface PackageCompatibility {
  readonly contractsVersion: string;
  readonly minimumClientVersion: string;
}
/** Portable package metadata. Credential values are forbidden. */
export interface PackageManifest {
  readonly schemaVersion: number;
  readonly id: string;
  readonly version: string;
  readonly tools: ReadonlyArray<ToolDefinition>;
  readonly artifacts: ReadonlyArray<ArtifactDescriptor>;
  readonly credentialReferences: ReadonlyArray<string>;
  readonly compatibility: PackageCompatibility;
}
export type PackageState = "ABSENT" | "STAGING" | "READY" | "FAILED" | "REVOKED";
/** Explicit wire decision, with no implicit ALLOW default. ASK requires approval. */
export interface PolicyDecision {
  readonly decision: Decision;
  readonly reason: DecisionReason;
  readonly policyVersion: string;
  readonly requestId: string;
}
/** Exact request identity and argument digest supplied to authorization. */
export interface PolicyInput {
  readonly context: RequestContext;
  readonly toolId: string;
  readonly action: string;
  readonly resource: ResourceDescriptor;
  readonly argumentsDigest: string;
}
/** Observed package state, distinct from assigned desired state. */
export interface ReportedPackage {
  readonly packageId: string;
  readonly version: string;
  readonly state: PackageState;
}
/** Safe correlation and principal identifiers; contains no credentials. */
export interface RequestContext {
  readonly requestId: string;
  readonly tenantId: string;
  readonly userId: string;
  readonly agentId: string;
  readonly deviceId?: string;
}
/** Declared resource identity; does not grant access or validate a path. */
export interface ResourceDescriptor {
  readonly kind: ResourceKind;
  readonly locator: string;
}
export type ResourceKind = "FILE" | "URL" | "DATABASE" | "DEVICE" | "CUSTOM";
/** Sanitized authorization evaluation event; hashes replace raw arguments and resource locators. A decision is not execution success. */
export interface RuntimeAuditEvent {
  readonly timestampUnixMs: number;
  readonly context: RequestContext;
  readonly toolId: string;
  readonly action: string;
  readonly resourceDigest: string;
  readonly argumentsDigest: string;
  readonly decision: PolicyDecision;
  readonly traceId: string;
}
export type SecretReference = string;
export type SemanticVersion = string;
export type Sha256 = string;
/** Named operation and declared resource kinds. */
export interface ToolAction {
  readonly name: string;
  readonly resourceKinds: ReadonlyArray<ResourceKind>;
}
/** Capability definition. JSON schemas are data, never executable code. */
export interface ToolDefinition {
  readonly id: string;
  readonly name: string;
  readonly description: string;
  readonly actions: ReadonlyArray<ToolAction>;
  readonly inputSchema: Record<string, unknown>;
  readonly outputSchema: Record<string, unknown>;
}
export const CONTRACT_SET_VERSION = "0.3.0-dev" as const;
export const CONTRACT_SET_NAME = "olo-toolgate-contracts" as const;
