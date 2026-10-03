// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
// GENERATED FILE — DO NOT EDIT DIRECTLY; tools/contracts/generate.py
/** Signed format 2 envelope; compilation adds ASK; old readers reject safely. */
export interface ApprovalBundlePayload {
  readonly formatVersion: number;
  readonly issuer: string;
  readonly audience: string;
  readonly tenantId: string;
  readonly sequence: number;
  readonly version: string;
  readonly directoryRevision: number;
  readonly issuedAtUnixMs: number;
  readonly expiresAtUnixMs: number;
  readonly graceMs: number;
  readonly policySha256: string;
  readonly policy: string;
  readonly rollbackOf?: number;
}
/** Format 2 exact rule; BLOCK > ASK > ALLOW. ASK never uses grace. */
export interface ApprovalBundleRule {
  readonly policyId: string;
  readonly userIds: ReadonlyArray<string>;
  readonly agentIds: ReadonlyArray<string>;
  readonly deviceIds: ReadonlyArray<string>;
  readonly toolId: string;
  readonly action: string;
  readonly resource: ResourceDescriptor;
  readonly graceAllowed: boolean;
  readonly effect: Decision;
}
export type ApprovalChoice = "APPROVE_ONCE" | "APPROVE_TEMPORARY" | "DENY";
/** Format 2 adds human approval without weakening default deny. */
export interface ApprovalCompiledPolicy {
  readonly formatVersion: number;
  readonly rules: ReadonlyArray<ApprovalBundleRule>;
}
/** Optimistic human decision; duration is required only for temporary approval. */
export interface ApprovalDecisionRequest {
  readonly decision: ApprovalChoice;
  readonly expectedRevision: number;
  readonly durationMs?: number;
}
/** Bounded tenant-scoped approval page. */
export interface ApprovalPage {
  readonly items: ReadonlyArray<ApprovalRecord>;
  readonly nextCursor?: string;
}
/** Gateway-only atomic lease consumption; repeat use never grants again. */
export interface ApprovalPermitUse {
  readonly approvalId: string;
  readonly permitId: string;
  readonly input: PolicyInput;
  readonly policyVersion: string;
}
/** Durable tenant approval status; identity/resource and digest only, never raw arguments. */
export interface ApprovalRecord {
  readonly id: string;
  readonly revision: number;
  readonly state: ApprovalState;
  readonly input: PolicyInput;
  readonly policyVersion: string;
  readonly createdAtUnixMs: number;
  readonly expiresAtUnixMs: number;
  readonly decidedBy?: string;
  readonly decidedAtUnixMs?: number;
}
/** Authenticated Control result for this exact Gateway attempt; lease fields occur only on a successful grant. */
export interface ApprovalResolution {
  readonly approvalId: string;
  readonly state: ApprovalState;
  readonly input: PolicyInput;
  readonly policyVersion: string;
  readonly permitId?: string;
  readonly permitExpiresAtUnixMs?: number;
}
export type ApprovalState = "PENDING" | "APPROVED_ONCE" | "APPROVED_TEMPORARY" | "DENIED" | "EXPIRED" | "CONSUMED";
/** Gateway-only normalized ASK request. No raw arguments or credentials. */
export interface ApprovalSubmission {
  readonly input: PolicyInput;
  readonly policyVersion: string;
}
/** Immutable artifact identity; digest must be verified by consumers. */
export interface ArtifactDescriptor {
  readonly uri: string;
  readonly sha256: string;
  readonly sizeBytes: number;
}
/** V2 ASK outcome; pending ASK grants no execution; ALLOW for approved ASK requires a signed permit. */
export interface AuthorizationOutcome {
  readonly decision: PolicyDecision;
  readonly approvalId?: string;
  readonly permit?: SignedExecutionPermit;
}
/** Runtime request; principal context and resource identity are derived by the gateway, never asserted by the caller. */
export interface AuthorizationRequest {
  readonly toolId: string;
  readonly action: string;
  readonly arguments: Record<string, unknown>;
}
/** Fixed service tool boundary; validate schema before use. */
export interface BuiltinInvocation {
  readonly toolId: string;
  readonly arguments: Record<string, unknown>;
}
/** Fixed service tool boundary; validate schema before use. */
export interface BuiltinIpcRequest {
  readonly protocolVersion: number;
  readonly requestId: string;
  readonly operation: BuiltinOperation;
  readonly invocation?: BuiltinInvocation;
}
/** Fixed service tool boundary; validate schema before use. */
export interface BuiltinIpcResponse {
  readonly requestId: string;
  readonly tools?: ReadonlyArray<BuiltinToolInfo>;
  readonly output?: Record<string, unknown>;
  readonly error?: ErrorCode;
}
export type BuiltinOperation = "CATALOG" | "CALL";
/** Fixed service tool boundary; validate schema before use. */
export interface BuiltinToolInfo {
  readonly toolId: string;
  readonly action: string;
  readonly description: string;
  readonly enabled: boolean;
  readonly inputSchema: Record<string, unknown>;
}
export type BundleEffect = "ALLOW" | "BLOCK";
/** Strict JWS protected header; no remote or embedded keys and no algorithm negotiation. */
export interface BundleHeader {
  readonly alg: string;
  readonly typ: string;
  readonly kid: string;
}
/** Signed version, trust domain, immutable policy bytes and bounded freshness claims. */
export interface BundlePayload {
  readonly formatVersion: number;
  readonly issuer: string;
  readonly audience: string;
  readonly tenantId: string;
  readonly sequence: number;
  readonly version: string;
  readonly directoryRevision: number;
  readonly issuedAtUnixMs: number;
  readonly expiresAtUnixMs: number;
  readonly graceMs: number;
  readonly policySha256: string;
  readonly policy: string;
  readonly rollbackOf?: number;
}
/** Publish a consistent directory snapshot or roll back into a new sequence. Requires Idempotency-Key. */
export interface BundlePublishRequest {
  readonly directoryRevision: number;
  readonly expectedSequence: number;
  readonly lifetimeMs: number;
  readonly graceMs: number;
  readonly gracePolicyIds?: ReadonlyArray<string>;
  readonly rollbackOf?: number;
}
/** Exact tenant-scoped compiled policy; empty identity dimensions are unrestricted. BLOCK has precedence. */
export interface BundleRule {
  readonly policyId: string;
  readonly userIds: ReadonlyArray<string>;
  readonly agentIds: ReadonlyArray<string>;
  readonly deviceIds: ReadonlyArray<string>;
  readonly toolId: string;
  readonly action: string;
  readonly resource: ResourceDescriptor;
  readonly graceAllowed: boolean;
  readonly effect: BundleEffect;
}
/** Endpoint identity foundation wire model. */
export interface ClientDiscovery {
  readonly protocolVersion: number;
  readonly serverId: string;
  readonly organization: string;
  readonly tenantId: string;
  readonly controlUrl: string;
  readonly gatewayUrl: string;
  readonly verificationUri: string;
  readonly minimumClientVersion: string;
  readonly issuedAtUnixMs: number;
  readonly expiresAtUnixMs: number;
  readonly issuerCertificatePem: string;
}
/** Fixed service tool boundary; validate schema before use. */
export interface ClientDownloadArtifact {
  readonly platform: ClientPlatform;
  readonly target: string;
  readonly filename: string;
  readonly sha256: string;
  readonly bytes: number;
}
/** Fixed service tool boundary; validate schema before use. */
export interface ClientDownloadManifest {
  readonly version: string;
  readonly artifacts: ReadonlyArray<ClientDownloadArtifact>;
}
/** Device identity and capabilities. Enrollment credentials travel separately. */
export interface ClientEnrollmentRequest {
  readonly deviceId: string;
  readonly clientVersion: string;
  readonly capabilities: ReadonlyArray<string>;
}
/** Endpoint identity foundation wire model. */
export interface ClientHealth {
  readonly state: EndpointState;
  readonly ready: boolean;
  readonly uptimeSeconds: number;
  readonly successfulCheckIns: number;
  readonly failedCheckIns: number;
  readonly reportSequence: number;
  readonly lastSuccessUnixMs?: number;
}
export type ClientIpcOperation = "HEALTH" | "ENROLL" | "CHECK_IN";
/** Endpoint identity foundation wire model. */
export interface ClientIpcRequest {
  readonly protocolVersion: number;
  readonly requestId: string;
  readonly operation: ClientIpcOperation;
}
/** Endpoint identity foundation wire model. */
export interface ClientIpcResponse {
  readonly requestId: string;
  readonly health?: ClientHealth;
  readonly challenge?: EndpointEnrollmentPrompt;
  readonly error?: ErrorCode;
}
export type ClientPlatform = "WINDOWS" | "LINUX" | "MACOS";
/** Batched report of device inventory and applied desired revision. */
export interface ClientReport {
  readonly deviceId: string;
  readonly clientVersion: string;
  readonly appliedRevision: number;
  readonly packages: ReadonlyArray<ReportedPackage>;
}
/** Version 1 deterministic exact-match rules, with unconditional default deny. */
export interface CompiledPolicy {
  readonly formatVersion: number;
  readonly rules: ReadonlyArray<BundleRule>;
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
/** Endpoint identity foundation wire model. */
export interface DeviceIdentity {
  readonly deviceId: string;
  readonly tenantId: string;
  readonly userId: string;
  readonly serverId: string;
  readonly certificatePem: string;
  readonly issuerCertificatePem: string;
  readonly expiresAtUnixMs: number;
}
/** Endpoint identity foundation wire model. */
export interface EndpointCheckIn {
  readonly sequence: number;
  readonly report: ClientReport;
}
/** Endpoint identity foundation wire model. */
export interface EndpointCheckInAck {
  readonly deviceId: string;
  readonly sequence: number;
  readonly serverTimeUnixMs: number;
  readonly nextIntervalSeconds: number;
  readonly identity?: DeviceIdentity;
}
/** Endpoint identity foundation wire model. */
export interface EndpointDeviceRecord {
  readonly deviceId: string;
  readonly tenantId: string;
  readonly userId: string;
  readonly keyFingerprint: string;
  readonly state: EndpointState;
  readonly revision: number;
  readonly lastSeenUnixMs: number;
  readonly reportSequence: number;
  readonly report?: ClientReport;
}
/** Endpoint identity foundation wire model. */
export interface EndpointEnrollmentChallenge {
  readonly enrollmentId: string;
  readonly deviceCode: string;
  readonly userCode: string;
  readonly verificationUri: string;
  readonly expiresAtUnixMs: number;
  readonly pollIntervalSeconds: number;
}
/** Endpoint identity foundation wire model. */
export interface EndpointEnrollmentDecision {
  readonly userCode: string;
  readonly keyFingerprint: string;
  readonly choice: EnrollmentChoice;
}
/** Endpoint identity foundation wire model. */
export interface EndpointEnrollmentPoll {
  readonly enrollmentId: string;
  readonly deviceCode: string;
}
/** Endpoint identity foundation wire model. */
export interface EndpointEnrollmentPrompt {
  readonly enrollmentId: string;
  readonly userCode: string;
  readonly verificationUri: string;
  readonly expiresAtUnixMs: number;
  readonly pollIntervalSeconds: number;
  readonly keyFingerprint: string;
}
/** Endpoint identity foundation wire model. */
export interface EndpointEnrollmentResult {
  readonly state: EnrollmentState;
  readonly identity?: DeviceIdentity;
}
/** Endpoint identity foundation wire model. */
export interface EndpointEnrollmentReview {
  readonly enrollmentId: string;
  readonly userCode: string;
  readonly deviceId: string;
  readonly platform: ClientPlatform;
  readonly keyFingerprint: string;
  readonly state: EnrollmentState;
  readonly expiresAtUnixMs: number;
}
/** Endpoint identity foundation wire model. */
export interface EndpointEnrollmentStart {
  readonly deviceId: string;
  readonly clientVersion: string;
  readonly platform: ClientPlatform;
  readonly csrPem: string;
  readonly capabilities: ReadonlyArray<string>;
}
/** Endpoint identity foundation wire model. */
export interface EndpointRevokeRequest {
  readonly expectedRevision: number;
}
export type EndpointState = "UNENROLLED" | "PENDING" | "ACTIVE" | "OFFLINE" | "REVOKED";
export type EnrollmentChoice = "APPROVE" | "DENY";
export type EnrollmentState = "PENDING" | "APPROVED" | "DENIED" | "EXPIRED" | "CONSUMED";
export type ErrorCode = "VALIDATION" | "UNAUTHORIZED" | "FORBIDDEN" | "NOT_FOUND" | "CONFLICT" | "DEPENDENCY_UNAVAILABLE" | "TIMEOUT" | "INTERNAL" | "UNSUPPORTED";
/** Machine-readable error without exception text or caller-controlled detail. */
export interface ErrorEnvelope {
  readonly code: ErrorCode;
  readonly requestId: string;
  readonly retryable: boolean;
}
/** Exact-operation capability; maximum ten seconds; authoritative consume boundary enforces replay. */
export interface ExecutionPermitClaims {
  readonly permitVersion: number;
  readonly issuer: string;
  readonly audience: string;
  readonly tenantId: string;
  readonly userId: string;
  readonly agentId: string;
  readonly deviceId?: string;
  readonly toolId: string;
  readonly action: string;
  readonly resource: ResourceDescriptor;
  readonly argumentsDigest: string;
  readonly requestId: string;
  readonly policyVersion: string;
  readonly approvalId: string;
  readonly jti: string;
  readonly issuedAtUnixMs: number;
  readonly expiresAtUnixMs: number;
}
/** Dedicated runtime permit trust domain; no key negotiation or remote keys. */
export interface ExecutionPermitHeader {
  readonly alg: string;
  readonly typ: string;
  readonly kid: string;
}
/** Authenticated runtime request to verify and atomically consume one exact-bound permit. */
export interface ExecutionPermitUseRequest {
  readonly permit: SignedExecutionPermit;
  readonly request: AuthorizationRequest;
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
/** Endpoint identity foundation wire model. */
export interface SignedClientDiscovery {
  readonly payload: string;
  readonly signature: string;
}
/** RS256 compact JWS; structural validation alone does not establish trust. */
export interface SignedExecutionPermit {
  readonly jws: string;
}
/** RFC 7515 compact JWS; payload and hash must both verify before adoption. */
export interface SignedPolicyBundle {
  readonly jws: string;
}
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
export const CONTRACT_SET_VERSION = "0.7.0-dev" as const;
export const CONTRACT_SET_NAME = "olo-toolgate-contracts" as const;
