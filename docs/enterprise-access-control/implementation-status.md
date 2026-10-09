# Enterprise access control implementation status

## Standard presets and configuration transfer - 2026-10-10

The release-selected `config/initial` JSON bundle defines protected defaults and
ReadOnly, ReadAndWrite and Admin Teams, Agent Groups, Device Groups and Tool Groups.
Presets include typed roles, complete grants, execution bindings and independent
review for sensitive Admin actions. Fresh installation applies the bundle once;
existing installations use reviewed import. Package pins and enabled identities
remain explicit prerequisites. Assignment and activation enforce administrative
ceilings, including protection against Super Admin elevation through membership.

The console supports device group selection during approval, preparation of missing
standard presets, complete 18-collection export, import preview and independent
review before applying edited snapshots. Imports preserve newer verified login
activity while rejecting credential/session rollback. Quickstart test identities
start disabled with unique private credentials.

The debug scripts use actual admin/reviewer API login and reviewed Agent allocation.
The MCP mimic uses the installed write/log tools, and a stdio bridge provides the
same authorized discovery/execution path for compatible AI clients.

Extension validation: 94 enterprise conformance tests passed, including the
27-combination preset action matrix, default/unpinned/offline denial, sensitive
operation review, membership elevation rejection and import/activity round trips.
The complete `debug/start.bat` deployment pipeline passed: 51 CI tests, 82 contract
tests, 124 console tests, Windows/Linux browser downloads, fresh native runtime
execution/revocation/approval/import/restart checks and the real administration
browser. The persistent debug container is healthy.

Installed Windows verification passed on device
`device-d5402222a1cf3d801e0529ddcde7e586`: the batch script repeated
`hotfolder.write_text` and returned `client.read_log_entry`; the MCP stdio bridge
completed initialization, discovery and both native calls. The file contents and
RUNNING/SUCCEEDED activity events were verified. ReadOnly discovery exposed only
the log reader; ReadAndWrite/Admin discovery exposed both diagnostic tools. The
installed executable remained unchanged (SHA-256
`c231ef7537c3f3b3d934f23e99623ad27ce7bb2afd86aeb5449c470863c47279`).

## Current implementation and acceptance - 2026-10-09

The canonical group-only implementation is present across contracts, Control,
Gateway, managed clients, administration and deployment. Individual ACLs and the
active legacy evaluator are retired. Historical migrations and archived inputs
remain solely for reviewed conversion and immutable audit evidence.

Completion is measured in two separate ways: all 96 acceptance scenarios must have
named passing automated evidence, and the integrated build/runtime/deployment
pipeline must pass. A mapped or unit-tested case alone does not certify production
readiness. `tools/enterprise/acceptance.py --write` regenerates the case statuses
below from actual JUnit results and required native/browser reports. Missing,
failed and skipped evidence stays open. `tools/enterprise/finish.py` runs the
completion gates sequentially and records full versus partial runs explicitly.

Implemented boundaries:

- Four protected defaults; atomic nonempty membership; one primary Tool Group;
  typed Human, Actor/Service and Management Roles; group-only complete grants,
  Agent capabilities, same-Team/Agent-Group delegations and explicit bindings.
- Deterministic current-authority evaluation for HUMAN, SERVICE and DELEGATED
  modes, bounded actor chains, independent resource checks, BLOCK precedence,
  accumulated ASK obligations and deny-by-default missing or expired evidence.
- Stable verified identity intake creates one disabled User; reviewed activation,
  session/credential epochs, retirement history and scoped administrative ceilings.
- Installed version/digest/extractor checks; live queue/effect/result/secret checks;
  signed single-use permits; durable approvals, quota reservations, exact retries,
  unknown-outcome fences and independently reviewed reconciliation.
- Complete group administration, separate Agents navigation, guided capability
  mapping, inherited provenance, membership impact, reviewed configuration and
  distinct saved/published/adopted state. Expanded persisted browser workflows are
  included in the final acceptance gates.
- Reviewed explicit migration/import, read-only shadow, monotonic cutover,
  independently signed recovery and quarantined SQLite/PostgreSQL restoration.

Final validation completed: **96/96 acceptance scenarios have mapped passing
automated evidence (100%)**, and **14/14 sequential completion gates passed**.
The frozen implementation source fingerprint is recorded in
`build/enterprise/finish/report.json`. A final CI-only image alias adjustment was
checked with Actionlint, matching production image IDs and another actual reviewed
PostgreSQL restore; `build/enterprise/ci-alias-validation.json` proves that all
runtime implementation inputs stayed identical to the validated source.

Passing gates include 130 Java/shared cases; the Rust workspace and Clippy;
TypeScript/JavaScript/PHP SDK checks (245 PHP model/security cases); 122 console
tests; non-root Linux custody without skips; real reviewed browser workflows;
native HUMAN/SERVICE/DELEGATED/ASK effects; immediate revocation and exact retries;
production Gateway outage/recovery; fresh SQLite/PostgreSQL runtime; two production
Control replicas; actual reviewed PostgreSQL restore; Helm installation/upgrade
and closed negative configurations; local Maven publication/artifact consumption;
source/dependency/license/secret scans; and all three exact runtime image scans.
The Quickstart, Gateway and Control images have zero HIGH/CRITICAL findings in
these scans, with generated CycloneDX SBOMs. No external release was published.

Measured discovery workload: 19 Tools, one catalog grant, 100 measured requests
after eight warmups, loopback HTTP through the identity adapter, 2 CPUs and
1536 MiB. SQLite p50/p95/p99: 234.838/284.771/327.320 ms; PostgreSQL:
233.092/314.513/355.053 ms. These are small-fixture observations, not enterprise
capacity claims.

Evidence is generated under `build/enterprise`, `build/quickstart`, `build/control`,
`build/gateway` and `build/ui`. Validation creates isolated owned data volumes and
preserves the existing debug installation. No production deployment or external
release publication is part of these gates.

## Architecture and supported scope

Control owns all current authorization and durable state. Gateway caches supply
bounded metadata, never effect authority. Default memberships carry no grants.
Ownership and connection approval are identity/trust facts, never permissions.
Complete grant witnesses retain their Team/Agent Group/Tool Group/Device Group
relationships; overlapping memberships cannot bridge unrelated rights.

Execution is online: permits expire within ten seconds with zero grace. Durable
single-use consumption is not an exactly-once external effect guarantee. Crashes
without reliable completion become OUTCOME_UNKNOWN and prohibit blind retry.

Nested/dynamic groups and offline effects are intentionally unsupported optional
scope. Trusted synchronization uses stable identity facts and reviewed canonical
import, with no email-based merging or automatic enablement. Arbitrary host shell,
raw SQL and unconstrained egress are denied until a structured, reviewed provider
and its confinement tests exist. Operator identity-provider provisioning, actual
customer migration plans and enterprise-scale capacity qualification remain
installation responsibilities, not manufactured fixture evidence.

## Mandatory stages

| ID | Requirement | Implemented source | Status | Verification evidence |
|---|---|---|---|---|
| S1 | Canonical group-only graph and defaults | GroupGraph, DirectoryService, generated enterprise contracts, four defaults and atomic membership checks | Verified automated | EnterpriseConformanceTest, EnterpriseAdministrationTest, migration tests |
| S2 | Shared deterministic evaluator and witness paths | EnterpriseEvaluator, RuntimeAccess, complete same-Team/Agent-Group witnesses and live Core authority | Verified automated | Normative conformance and operation tests; Rust online authority suite |
| S3 | Verified identity intake, disabled activation and lifecycle | VerifiedActors, IdentityIntake, immutable identity facts, credential epochs/history and reviewed enablement | Verified automated | Disabled/concurrent intake, lifecycle, recovery and administration tests |
| S4 | Resource sets and every protected effect | ResourceExtraction, InstalledProfiles, current MCP relay, native checkpoints and dedicated effect journal | Verified automated | Resource and operation conformance; client execution tests; fresh native runtime and production Gateway gates |
| S5 | Bound approvals, signed permits, durable reservations and outcomes | EnterpriseOperations, ConfigurationChanges, ReviewedRecovery and purpose-specific Control signer | Verified automated | Independent approval, revision/epoch, signature, nonce-race and configuration tests |
| S6 | Epochs, outbox, freshness, adoption and operations | Transactional epoch/outbox, explicit publication, authenticated device acknowledgement, online Gateway freshness | Verified automated | Storage monotonicity and Rust freshness tests; snapshot/adoption, restart and current-authority outage gates |
| S7 | Complete group-only administration workflows | Separate Agents menu, group mapping, membership, typed scope editors, configuration reviews, inherited provenance and activation status | Verified automated | 122 console tests and TypeScript check; real independently reviewed browser workflows and accessibility |
| S8 | Reviewed data conversion and one recorded cutover | Immutable legacy archive, deny-by-default cutover and independently reviewed recovery utility | Verified automated | Migration storage tests; actual reviewed conversion, import and recorded shadow reports |

## Use-case traceability

The rows below identify representative automated boundary tests and required native/browser evidence. Passing case evidence is distinct from the integrated gate report, production operator provisioning and capacity qualification.

| ID | Requirement | Stage | Status | Evidence |
|---|---|---|---|---|
| UC01 | Give one User one read Tool: Put the User in a dedicated Team and the Tool in a scoped Tool Group; a complete group grant permits only the action/resource/bound group. | S2/S7 | Verified automated | `humanExecutionRequiresOnlyHumanGrantAndBinding`; `actionRulesDoNotPromoteReadToWrite` |
| UC02 | Add a new Finance employee: Team membership inherits Finance grants only after identity/account activation. | S3/S7 | Verified automated | `disabledIdentityCannotBeApproved`; `roleInheritanceRetainsIndependentTeamWitnessesAndRevokesLive` |
| UC03 | User joins Finance and IT Teams: Union valid complete rules; no cross-product of one Team's tools and the other's devices. | S2/S7 | Verified automated | `unrelatedTeamsCannotShareGrantAndDelegation`; `unrelatedToolDeviceGrantsAreNotMultiplied` |
| UC04 | Assign one Role to several Teams: The same bounded rule set applies to enabled members; team membership remains separate. | S2/S7 | Verified automated | `roleInheritanceRetainsIndependentTeamWitnessesAndRevokesLive` |
| UC05 | User inherits Roles from several Teams: Resolve complete rules with provenance; applicable BLOCK still wins. | S2/S7 | Verified automated | `roleInheritanceRetainsIndependentTeamWitnessesAndRevokesLive`; `blockOverridesEveryGrant` |
| UC06 | Remove a User from a Team: Remove that Team's grants and delegations; independent grants from other Teams remain visible, and last-membership removal cannot orphan the User. | S3/S7 | Verified automated | `lastMembershipCannotBeRemovedByDirectGroupEditing`; `roleInheritanceRetainsIndependentTeamWitnessesAndRevokesLive` |
| UC07 | Disable a Team or Role: Stop inherited grants without deleting memberships or audit history. | S3/S7 | Verified automated | `roleInheritanceRetainsIndependentTeamWitnessesAndRevokesLive` |
| UC08 | Give a contractor limited access: Explicit expiry, allowed resources and Device Groups; expiry denies without manual cleanup. | S3/S7 | Verified automated | `jitGrantValidityHasExactExpiryAndNoImplicitExtension`; `resourcePrefixRespectsSegmentBoundary` |
| UC09 | Auditor inspects configuration: Read-only management scope; no implied runtime execution or secret access. | S2/S7 | Verified automated | `managementRoleCannotBeRuntimeGrantSource`; `portalAdministratorNeedsExplicitSecretPurposeManagement` |
| UC10 | Administrator manages devices but cannot run payment Tools: Management permission and tool-use grants remain independent. | S2/S7 | Verified automated | `managementRoleCannotBeRuntimeGrantSource`; `externalRoleClaimWithoutBoundManagementMembershipDoesNotAuthorize` |
| UC11 | Team grants read while Role grants write: Each action requires its own applicable tuple; read does not imply write. | S2/S7 | Verified automated | `actionRulesDoNotPromoteReadToWrite` |
| UC12 | Conflicting grant and explicit denial: Applicable BLOCK denies across all sources; explanation identifies the denial safely. | S2/S7 | Verified automated | `blockOverridesEveryGrant`; `approvedOperationNeverManufacturesMissingGrant` |
| UC13 | Grant a Tool Group containing many Tools: Include only enabled members within granted actions, resources and bound device scope. | S2/S7 | Verified automated | `eachRequiredAuthorityDimensionIndependentlyDenies`; `humanExecutionRequiresOnlyHumanGrantAndBinding` |
| UC14 | Newly published Tool enters a privileged Tool Group: Start in default-tools; show inherited-access impact before reviewed movement into the privileged group. | S1/S7 | Verified automated | `everyIndividualCreationAndGroupDeletionKeepsMandatoryMembership`; `makerCheckerPreviewAndAtomicApply` |
| UC15 | Tool moves from reporting to payment group: Recompute affected discovery, grants and Agent capabilities; no stale group-derived rights. | S1/S7 | Verified automated | `toolMoveAndDeviceRemovalCannotRetainOriginalBinding` |
| UC16 | Isolate one dangerous Tool: Move it to a restricted Tool Group, or disable it pending review; do not attach a direct ACL to the Tool. | S2/S7 | Verified automated | `eachRequiredAuthorityDimensionIndependentlyDenies`; `legacyIndividualAssignmentsAreRejected` |
| UC17 | Tools require different execution Device Groups: Place them in separate Tool Groups with explicit bindings; no per-Tool override. | S1/S7 | Verified automated | `multipleExplicitBindingsRemainIndependent` |
| UC18 | Tool requires multiple actions such as read and delete: Return allowed actions; only explicitly permitted actions execute. | S4 | Verified automated | `actionRulesDoNotPromoteReadToWrite`; `everyAffectedResourceNeedsCompletePermission` |
| UC19 | Tool receives a resource outside the allowed folder/account/project: Server extraction and canonical resource matching deny it before the effect. | S4 | Verified automated | `sourceAndDestinationAndAllBatchMembersAreExtracted`; `filesystemAndUrlEscapesRejectBeforePermissionEvaluation` |
| UC20 | Installed client advertises an unregistered Tool: Installation inventory cannot create a Tool grant or trusted definition. | S4 | Verified automated | `schemasSecretsPermissionsResourcesAndCredentialBoundary` |
| UC21 | Approved Tool version changes: Review definition/extractor changes and runtime assignment; stale/incompatible permits fail. | S4 | Verified automated | `signedPermitBindsTargetVersionsArgumentsAndFullResourceSet`; `schemasSecretsPermissionsResourcesAndCredentialBoundary` |
| UC22 | Tool is disabled or its group is disabled: Remove discovery and deny queued or new executions using that binding. | S2/S7 | Verified automated | `eachRequiredAuthorityDimensionIndependentlyDenies`; `directoryRevocationBetweenReservationAndEffectDenies` |
| UC23 | New Device enrolls: Join `default-devices`; still require identity review, enablement, approval and runtime grants. | S1/S7 | Verified automated | `enrollmentBindsOwnerKeyAndDurableSequenceAndRevocation`; `everyIndividualCreationAndGroupDeletionKeepsMandatoryMembership` |
| UC24 | Administrator moves Device into Production: Atomic membership change, revision check, audit and impact preview; no membership-free intermediate state. | S1/S7 | Verified automated | `concurrentMembershipChangesHaveOneWinnerAndNoOrphan`; `makerCheckerPreviewAndAtomicApply` |
| UC25 | Device belongs to two departmental groups: A Tool requires access to its bound group; the other membership cannot substitute. | S2/S7 | Verified automated | `overlappingDeviceGroupsDoNotBridgeBinding` |
| UC26 | User accesses a shared department Device: Target trust and caller identity are checked separately; group grants do not impersonate the Device owner. | S2/S7 | Verified automated | `completeDelegatedPathAllowsWithoutOwnerEquality`; `wrongCertificateDeviceAndTenantCannotConsume` |
| UC27 | User can access a Device Group but no Tool Group: No Tools become executable from device eligibility alone. | S2/S7 | Verified automated | `eachRequiredAuthorityDimensionIndependentlyDenies` |
| UC28 | User can access a Tool Group but not its Device Group: Bound Tools are hidden or denied in that target context. | S2/S7 | Verified automated | `unrelatedToolDeviceGrantsAreNotMultiplied` |
| UC29 | Tool executes on a shared server rather than caller's laptop: Evaluate the execution Device; apply separate caller-device posture if policy requires it. | S4 | Verified automated | `completeDelegatedPathAllowsWithoutOwnerEquality`; `trustedNetworkPostureRegionAndHoursAreConjunctive` |
| UC30 | Remove Device from the Tool's group while a job waits: Recheck binding before dispatch/effect; deny the queued request. | S4 | Verified automated | `toolMoveAndDeviceRemovalCannotRetainOriginalBinding`; `queuedBuildAndDeploymentCannotOutliveTheirCreatorsGroupAuthority` |
| UC31 | Device key is revoked, approval expires or owner account is disabled: Fail the live trust/registry gate regardless of cached policy or green availability. | S3/S7 | Verified automated | `deniedExpiredWrongCodeInvalidCsrAndDisabledOwnersFailClosed`; `enrollmentBindsOwnerKeyAndDurableSequenceAndRevocation` |
| UC32 | Last group membership is removed: Reject mutation; default membership may be replaced only with at least one other group. | S1/S7 | Verified automated | `lastMembershipCannotBeRemovedByDirectGroupEditing`; `membershipReplacementRetainsExactlyOnePrimaryToolGroup` |
| UC33 | Disable a Device Group: All execution bindings requiring it stop granting; another enabled membership does not revive them. | S2/S7 | Verified automated | `eachRequiredAuthorityDimensionIndependentlyDenies`; `overlappingDeviceGroupsDoNotBridgeBinding` |
| UC34 | Target is offline or Tool runtime is absent: Report unavailable safely; do not silently reroute to an unapproved Device. | S4 | Verified automated | `signedPermitBindsTargetVersionsArgumentsAndFullResourceSet`; `schemasSecretsPermissionsResourcesAndCredentialBoundary` |
| UC35 | Agent can use Tool but User cannot: Deny delegated execution. | S2/S7 | Verified automated | `eachRequiredAuthorityDimensionIndependentlyDenies` |
| UC36 | User can use Tool but Agent cannot: Hide/deny in that Agent context; authorized direct use may still be permitted. | S2/S7 | Verified automated | `eachRequiredAuthorityDimensionIndependentlyDenies`; `humanExecutionRequiresOnlyHumanGrantAndBinding` |
| UC37 | Agent Groups supply no applicable capabilities: Discover no Tools and execute none; default membership does not supply a grant. | S2/S7 | Verified automated | `defaultMembershipCreatesNoGrant`; `emptySelectorsDoNotMeanWildcard` |
| UC38 | Agent needs all safe reporting Tools but no payment Tool: Map only the reporting Tool Group; payment Tools belong to a separate restricted group. | S2/S7 | Verified automated | `unrelatedToolDeviceGrantsAreNotMultiplied`; `actionRulesDoNotPromoteReadToWrite` |
| UC39 | Team shares an assistant: Require allowed delegation from the requesting User/Team; evaluate that User's grants. | S2/S7 | Verified automated | `unrelatedTeamsCannotShareGrantAndDelegation` |
| UC40 | Agent owner is a Super Admin: Ownership supplies no additional runtime rights to the Agent or its delegated Users. | S2/S7 | Verified automated | `administrativeOwnerSuppliesNoRightsAndDependencyIsExplicit` |
| UC41 | Scheduled automation has no human present: Use explicit service/workload identity and grants; retain actor attribution. | S2/S7 | Verified automated | `serviceModeNeedsExplicitServiceGrantAndCapability` |
| UC42 | Agent attempts to change userId or tenantId: Reject untrusted identity changes before policy evaluation. | S2/S7 | Verified automated | `serviceIdentityCannotReuseDelegatedCredential`; `crossTenantCannotUseSameIdentifiers`; `delegatedCredentialCannotAttachToNewHumanSession` |
| UC43 | Agent delegates to another Agent: Verified, bounded chain; downstream scope cannot exceed any upstream ceiling. | S2/S7 | Verified automated | `downstreamAgentCannotExceedAnyUpstreamCeilingOrReuseIdentity` |
| UC44 | Agent credential expires or mapping is revoked mid-workflow: Recheck subsequent protected effects; already completed effects remain audited. | S5/S6 | Verified automated | `directoryRevocationBetweenReservationAndEffectDenies`; `olderTokenWithoutEpochCannotSurviveUserReenablement` |
| UC45 | Retry changes target or arguments under the same request key: Reject conflicting replay; an identical retry cannot duplicate the protected effect. | S5/S6 | Verified automated | `exactInvocationIsDurableAndIdempotentWithoutStoringArguments`; `permitCannotBeConsumedTwiceOrReservedAgainAfterConsumption` |
| UC46 | Payment/deployment requires independent approval: ASK binds reviewer, subject, actor, target, action, resource, arguments and expiry; no self-approval. | S5/S6 | Verified automated | `everyAskObligationNeedsIndependentReview`; `requesterCannotReviewOwnOperationEvenWithManagementRole` |
| UC47 | User or Agent loses required grant after approval: Approval cannot revive the revoked capability; deny before effect. | S5/S6 | Verified automated | `approvalRevocationAfterReservationStopsConsumption`; `approvedOperationNeverManufacturesMissingGrant` |
| UC48 | Temporary incident elevation: Explicit role/grant with incident reference, expiry and bounded targets; record activation and expiry. | S5/S6 | Verified automated | `jitGrantValidityHasExactExpiryAndNoImplicitExtension`; `managementCeilingRejectsAccessExpansionAndDirectRuntimeRoles` |
| UC49 | Break-glass recovery: Dedicated emergency workflow, signed authority, narrow time window, strong review/audit; no general Super Admin bypass. | S5/S6 | Verified automated | `independentlySignedReviewAppliesOnceAndNeverResurrectsExpiredAuthority`; `recoveryCannotAddRuntimePermissionsEvenWithGenuineReviewSignatures` |
| UC50 | Require corporate network, device posture or business hours: Trusted contextual attributes must satisfy policy; missing or stale evidence fails closed. | S4 | Verified automated | `trustedNetworkPostureRegionAndHoursAreConjunctive`; `missingRequiredTrustEvidenceDeniesButUnconditionalGrantNeedsNoUnusedAttestation` |
| UC51 | Region/data-residency restriction: Binding plus trusted location attributes keeps execution/data within allowed scopes. | S4 | Verified automated | `trustedNetworkPostureRegionAndHoursAreConjunctive`; `multipleExplicitBindingsRemainIndependent` |
| UC52 | High-value payment or unusual invocation rate: Trusted argument extraction and approved thresholds add ASK/BLOCK or bounded limits. | S4 | Verified automated | `highRiskRequiresOnlineAndAuthoritativeAmountAndQuota`; `durableGroupBudgetHasOneWinnerAcrossConcurrentSubmissions` |
| UC53 | Maker/checker separation of duties: Requester cannot approve their own request; policy/role authoring and publication may require separate administrators. | S5/S6 | Verified automated | `makerCheckerPreviewAndAtomicApply`; `requesterCannotReviewOwnOperationEvenWithManagementRole` |
| UC54 | Bulk disable during an incident: Invalidate applicable live grants, queue dispatch and fresh permits; expose rollout/adoption state. | S5/S6 | Verified automated | `directoryRevocationBetweenReservationAndEffectDenies`; `compilerIsDeterministicAndGroupSnapshotDoesNotFlattenIndividuals` |
| UC55 | Two administrators edit a mapping concurrently: Revision conflict prevents lost updates; reload and review before another mutation. | S1/S7 | Verified automated | `concurrentGraphChangeInvalidatesReview`; `concurrentMembershipChangesHaveOneWinnerAndNoOrphan` |
| UC56 | Tenant A names Tenant B's Tool/Device/User: Reject without revealing the other tenant's inventory. | S2/S7 | Verified automated | `crossTenantCannotUseSameIdentifiers`; `wrongCertificateDeviceAndTenantCannotConsume` |
| UC57 | Gateway/Control outage or stale signed policy: Follow bounded freshness rules; unavailable or expired authority never permits new protected effects. | S5/S6 | Verified automated | `highRiskRequiresOnlineAndAuthoritativeAmountAndQuota` |
| UC58 | Offline client requests execution: Default deny for enterprise operations requiring live authorization; any future offline capability needs explicit narrow leases and a declared revocation bound. | S5/S6 | Verified automated | `highRiskRequiresOnlineAndAuthoritativeAmountAndQuota` |
| UC59 | Permission removed during a long-running operation: Recheck at protected checkpoints; stop remaining effects where safe and audit partial completion. | S5/S6 | Verified automated | `watchdogCommitsUnknownEvenWhenSubsequentAuthorityIsRevoked`; `queuedBuildAndDeploymentCannotOutliveTheirCreatorsGroupAuthority` |
| UC60 | Restore backup or roll back policy: Never reset identity revisions or replay old permits; publish a new monotonic version and evaluate current membership/trust. | S5/S6 | Verified automated | `independentlySignedReviewAppliesOnceAndNeverResurrectsExpiredAuthority`; `identifiersRemainRetiredAfterDeletion` |
| UC61 | Access review finds dormant grants: Show source and affected operations; revoke with audit and validate resulting effective access. | S2/S7 | Verified automated | `individualProvenanceReportsCompleteTuplesWithoutCreatingDirectPermissions` |
| UC62 | Dry-run policy against a production request: Return decision/reason/provenance with no effect, no approval consumption and no secret disclosure. | S2/S7 | Verified automated | `shadowComparesCapturedLegacyDecisionWithoutApplyingGrantsOrConsumingAuthority` |
| UC63 | Nested groups or dynamic identity-provider Teams are introduced: Explicit supported rules, trusted synchronization and cycle/depth limits; no inferred or stale membership expansion. | S2/S7 | Verified automated | `unknownMissingDuplicateAndMalformedFieldsReject`; `mandatoryMembershipAndPrimaryToolGroupAreValidated` |
| UC64 | Orphaned owner, deleted group or retired identifier: Preserve history, reject dangling active references and prevent identifier reuse from reviving access. | S1/S7 | Verified automated | `identifiersRemainRetiredAfterDeletion`; `everyIndividualCreationAndGroupDeletionKeepsMandatoryMembership` |
| UC65 | First access by a verified unknown human identity: Register exactly one disabled User, assign default Team, show it in Users and deny the attempted resource access. | S3/S7 | Verified automated | `concurrentVerifiedIntakeCreatesOneDisabledIdentityWithMembership` |
| UC66 | Administrator enables a pending User: Explicit audited enablement; runtime/portal access still depends on that User's Team grants and Roles. | S3/S7 | Verified automated | `olderTokenWithoutEpochCannotSurviveUserReenablement`; `defaultMembershipCreatesNoGrant` |
| UC67 | Previously disabled User tries again: Update bounded attempt metadata; never automatically enable or create a duplicate identity. | S3/S7 | Verified automated | `repeatedVerifiedLoginMetadataDoesNotRevokeUnrelatedApprovals`; `concurrentVerifiedIntakeCreatesOneDisabledIdentityWithMembership` |
| UC68 | Unauthenticated caller supplies a fake User identity: Reject authentication; log a safe failed-authentication event without manufacturing a registered User. | S3/S7 | Verified automated | `invalidAuthenticationDoesNotCreateUsers` |
| UC69 | Tool or User loses its last group during deletion/import: Reject or atomically transfer to the corresponding default; no ungrouped committed entity. | S1/S7 | Verified automated | `everyIndividualCreationAndGroupDeletionKeepsMandatoryMembership`; `completeExportCanEnterReviewedImportWithoutRotatingUnchangedCredentials` |
| UC70 | Same verified identity reaches several Gateways concurrently: Stable issuer/subject/tenant binding and uniqueness produce one disabled User and one valid default membership. | S3/S7 | Verified automated | `concurrentVerifiedIntakeCreatesOneDisabledIdentityWithMembership` |
| UC71 | New Agent is registered: Assign default-agents atomically; no direct capabilities, no inherited runtime access from default membership. | S1/S7 | Verified automated | `everyIndividualCreationAndGroupDeletionKeepsMandatoryMembership`; `defaultMembershipCreatesNoGrant` |
| UC72 | Agent joins an authorized Agent Group: Inherit only complete group capabilities and compatible Actor Roles; existing individual credentials remain distinct. | S1/S7 | Verified automated | `groupRoleTypeCannotBeMisassigned`; `completeDelegatedPathAllowsWithoutOwnerEquality` |
| UC73 | Agent belongs to several Agent Groups: Union complete rules without mixing one group's Tool scope with another's Device Group or delegation scope. | S2/S7 | Verified automated | `unrelatedAgentGroupsCannotShareCapabilityAndDelegation`; `disabledAgentGroupRemovesItsPathWhileIndependentPathsSurvive` |
| UC74 | Team can delegate to Agent Group A, Agent has a capability only through B: Deny that path; the capability and delegation must agree on the same enabled Agent Group. | S2/S7 | Verified automated | `unrelatedAgentGroupsCannotShareCapabilityAndDelegation` |
| UC75 | Agent Group is disabled or Agent leaves it: Remove that group's capabilities and recheck queued/new effects; independent valid group paths may remain. | S2/S7 | Verified automated | `disabledAgentGroupRemovesItsPathWhileIndependentPathsSurvive` |
| UC76 | Agent loses its last group or its custom group is deleted: Reject or atomically move to default-agents; never commit an orphan or retain the removed group's grants. | S1/S7 | Verified automated | `everyIndividualCreationAndGroupDeletionKeepsMandatoryMembership`; `lastMembershipCannotBeRemovedByDirectGroupEditing` |
| UC77 | One Agent needs exceptional restricted capabilities: Create a dedicated scoped Agent Group and assign membership; never create a direct Agent ACL. | S2/S7 | Verified automated | `legacyIndividualAssignmentsAreRejected`; `completeDelegatedPathAllowsWithoutOwnerEquality` |
| UC78 | Group permits delegated operations but not unattended automation: Service-mode requests remain denied until an explicit service grant and verified workload binding exist. | S2/S7 | Verified automated | `serviceModeNeedsExplicitServiceGrantAndCapability`; `serviceIdentityCannotReuseDelegatedCredential` |
| UC79 | Several Agents share a group: Group permissions are shared, credentials are individual, and audit identifies the actual requesting Agent. | S2/S7 | Verified automated | `serviceIdentityCannotReuseDelegatedCredential`; `downstreamAgentCannotExceedAnyUpstreamCeilingOrReuseIdentity` |
| UC80 | Administrator opens the Agents menu: Agent inventory and group mappings are managed under their own menu; no direct permission editors appear on individual details. | S1/S7 | Verified automated | `legacyIndividualAssignmentsAreRejected` |

## Additional acceptance cases

| ID | Acceptance boundary | Stage | Status | Evidence |
|---|---|---|---|---|
| AC01 | Each authorization dimension independently denies | S1–S8 | Verified automated | `eachRequiredAuthorityDimensionIndependentlyDenies` |
| AC02 | No Device Group bridge or cross-product | S1–S8 | Verified automated | `overlappingDeviceGroupsDoNotBridgeBinding`; `unrelatedToolDeviceGrantsAreNotMultiplied` |
| AC03 | Same semantics for human, service and delegated modes | S1–S8 | Verified automated | `humanExecutionRequiresOnlyHumanGrantAndBinding`; `serviceModeNeedsExplicitServiceGrantAndCapability`; `completeDelegatedPathAllowsWithoutOwnerEquality` |
| AC04 | Discovery eligibility and ASK separation | S1–S8 | Verified automated | `askObligationsAccumulateAcrossAllowPaths`; `approvedOperationNeverManufacturesMissingGrant` |
| AC05 | BLOCK survives narrowing and approvals | S1–S8 | Verified automated | `blockOverridesEveryGrant`; `everyAffectedResourceNeedsCompletePermission` |
| AC06 | Original binding and live rechecks at all handoffs | S1–S8 | Verified automated | `directoryRevocationBetweenReservationAndEffectDenies`; `queuedBuildAndDeploymentCannotOutliveTheirCreatorsGroupAuthority` |
| AC07 | Atomic audited membership/grant concurrency | S1–S8 | Verified automated | `concurrentMembershipChangesHaveOneWinnerAndNoOrphan`; `realTransactionsAuditReplayRestartAndImmutableTriggers` |
| AC08 | Monotonic snapshots and freshness bounds | S1–S8 | Verified automated | `publicationRollbackConcurrencyAuditAndSigningFailureAreAtomic` |
| AC09 | Shared execution preserves caller identity | S1–S8 | Verified automated | `completeDelegatedPathAllowsWithoutOwnerEquality`; `wrongCertificateDeviceAndTenantCannotConsume` |
| AC10 | Exact approval/permit/retry binding and replay protection | S1–S8 | Verified automated | `signedPermitBindsTargetVersionsArgumentsAndFullResourceSet`; `parallelReplicasOnlyConsumeOneNonce`; `unknownExternalOutcomeCannotTriggerBlindRetry` |
| AC11 | Accessible complete UI and impact/activation states | S1–S8 | Verified automated | `individualProvenanceReportsCompleteTuplesWithoutCreatingDirectPermissions`; `makerCheckerPreviewAndAtomicApply` |
| AC12 | Redacted audit and provenance | S1–S8 | Verified automated | `exportedSpansExcludeRawUrlsEventsAndExceptionText`; `exactInvocationIsDurableAndIdempotentWithoutStoringArguments` |
| AC13 | All creation/import/sync/delete paths prevent orphans | S1–S8 | Verified automated | `everyIndividualCreationAndGroupDeletionKeepsMandatoryMembership`; `completeExportCanEnterReviewedImportWithoutRotatingUnchangedCredentials`; `concurrentMembershipChangesHaveOneWinnerAndNoOrphan` |
| AC14 | Concurrent verified first access creates one disabled User | S1–S8 | Verified automated | `concurrentVerifiedIntakeCreatesOneDisabledIdentityWithMembership` |
| AC15 | No direct individual ACL or Role assignment | S1–S8 | Verified automated | `legacyIndividualAssignmentsAreRejected`; `managementRoleCannotBeRuntimeGrantSource`; `groupRoleTypeCannotBeMisassigned` |
| AC16 | Same Agent Group delegation/capability path | S1–S8 | Verified automated | `unrelatedAgentGroupsCannotShareCapabilityAndDelegation`; `downstreamAgentCannotExceedAnyUpstreamCeilingOrReuseIdentity` |

## Validation evidence

- `build/enterprise/acceptance.json`: all 96 named cases and the exact passing JUnit
  report paths, with required browser and production Gateway proof.
- `build/enterprise/finish/report.json`: all 14 sequential gates, individual logs,
  timings, source fingerprint and complete-run status.
- `build/enterprise/ci-alias-validation.json`: final CI-only alias validation;
  Actionlint, unchanged runtime source, matching image IDs and real restore.
- `build/enterprise/conformance.json` and `linux-custody.json`: Java/shared cases
  and actual non-root Linux custody with no skips in the custody gate.
- `build/quickstart/browser-smoke.json`, `smoke-postgresql.json`,
  `build/gateway/container-smoke.json` and `build/control/container-smoke.json`:
  accessible persisted administration, native effects, trust, revocation,
  outage/recovery, production replicas and bounded measured workload.
- `build/enterprise/postgresql-restore.json` and migration reports: independently
  reviewed restore quarantine, retirement/floors and no-effect conversion/shadow.
- `build/enterprise/images/report.json` and image JSON/CycloneDX files: exact image
  IDs, zero HIGH/CRITICAL findings and generated SBOMs.

Private fixture credentials remain in ignored temporary custody and owned fixture
containers/volumes are removed. The existing debug installation and unrelated
containers/data were preserved. Nested groups, offline effects and an
identity-provider-specific SCIM connector remain explicitly unsupported scope;
production identity provisioning and customer migration require operator input.
