# Enterprise access control implementation status

## Purpose

Track the mandatory attached implementation specification against source changes and executable evidence. The group-only model replaces the prior individual ACL model. No legacy ALLOW path may run beside the new evaluator. Data conversion is an explicit reviewed operation, not a runtime authorization fallback.

## Inspection and architecture decisions

- Existing stack: generated JSON Schema/OpenAPI contracts, Quarkus/Java Control, Rust Gateway and managed client, React console, PostgreSQL/SQLite, signed distribution and existing Docker/Helm packaging.
- Identified superseded paths: UserAccess/direct role arrays; per-Agent allowedToolIds; per-Tool deviceGroupId; Teams used as device groups; owner-bound relay; unbound administrator bypass; policy arrays whose empty dimensions become wildcards.
- Existing enforcement boundaries to integrate: Gateway authorize/MCP, approval leases, client relay submission/dispatch/authorize/result, managed local invocation, built-in execution, package runtime, vault/artifact retrieval and administration.
- Target authority: one deterministic group-path evaluator; online current-revision checks before effects; group-derived snapshots are discovery hints, never permits.
- Defaults for all four membership types confer no grants. Membership and group deletion are transactional. Tools belong to exactly one Tool Group; Tool Groups support multiple explicit execution bindings.
- Group source scopes and actor/human scopes remain complete tuples. Delegation must match the same Team and same Agent Group throughout the witness.
- Durable invocation states include OUTCOME_UNKNOWN; permit consumption alone never claims exactly-once external effects.
- Production deployment and release publication are outside this implementation run.

## Current checkpoint — 2026-10-09

This is an implementation checkpoint, not an enterprise-ready release. Current source implements the group graph, live evaluator, disabled identity intake, typed complete grants and delegations, exact installed profiles, durable approvals and effect permits, reviewed configuration changes, scoped management, native device adoption, and group administration screens. The attached specification remains the acceptance target.

Passing verification for this checkpoint: 118 console tests and TypeScript check; 36 ordinary Java tests and 69 enterprise tests; the Rust Gateway/client suite; 81 contract tests; enterprise Helm installation/upgrade rendering and nine negative configuration checks. Generated bindings were verified and Rust source formatted. Identity metadata, inherited provenance and encrypted vault tests are included in the enterprise suite. Full fresh-container, HTTP/mTLS, browser, deployment, migration-shadow and performance gates are still required.

Remaining work:

- Validate the rewritten Quickstart composition in a new isolated data volume, including initial reviewed installation, independent configuration reviewers, actual device enrollment, direct human execution, ASK, immediate revocation, restart and restore. Existing debug data must not be silently converted or reset.
- Update the remaining deployment/debug fixtures, active compatibility contracts and release gates to use the new wire model; retire obsolete producers while retaining immutable historical migrations and migration evidence.
- Complete reviewed migration conversion and a recorded shadow/cutover report. Current cutover archives old access records and fails closed; it does not automatically translate legacy permissions into new grants.
- Finish scoped durable quota accounting, effect-outcome reconciliation, queued package/Builder creator-authority rechecks, public DNS destination pinning and the deployment custody checks.
- Complete the operator runbooks, use-case evidence matrix, accessible browser verification and load/failure testing. Candidate inherited grants and device acknowledgements are metadata, not effect authorization or exactly-once completion guarantees.

## Mandatory stages

| ID | Requirement | Implemented source | Status | Verification evidence |
|---|---|---|---|---|
| S1 | Canonical group-only graph and defaults | GroupGraph, DirectoryService, generated enterprise contracts, four defaults and atomic membership checks | Core implemented; integration pending | EnterpriseConformanceTest, EnterpriseAdministrationTest, migration tests |
| S2 | Shared deterministic evaluator and witness paths | EnterpriseEvaluator, RuntimeAccess, complete same-Team/Agent-Group witnesses and live Core authority | Core implemented; integration pending | Normative conformance and operation tests; Rust online authority suite |
| S3 | Verified identity intake, disabled activation and lifecycle | VerifiedActors, IdentityIntake, immutable identity facts, credential epochs/history and reviewed enablement | Core implemented; HTTP validation pending | Disabled/concurrent intake, lifecycle, recovery and administration tests |
| S4 | Resource sets and every protected effect | ResourceExtraction, InstalledProfiles, current MCP relay, native checkpoints and dedicated effect journal | In progress | Resource and operation conformance; client execution tests; full runtime integration pending |
| S5 | Bound approvals, signed permits, durable reservations and outcomes | EnterpriseOperations, ConfigurationChanges, ReviewedRecovery and purpose-specific Control signer | Core implemented; integration pending | Independent approval, revision/epoch, signature, nonce-race and configuration tests |
| S6 | Epochs, outbox, freshness, adoption and operations | Transactional epoch/outbox, explicit publication, authenticated device acknowledgement, online Gateway freshness | In progress | Storage monotonicity and Rust freshness tests; full adoption/operations gates pending |
| S7 | Complete group-only administration workflows | Separate Agents menu, group mapping, membership, typed scope editors, configuration reviews, inherited provenance and activation status | In progress | 118 console tests and TypeScript check; new workflow/browser validation pending |
| S8 | Reviewed data conversion and one recorded cutover | Immutable legacy archive, deny-by-default cutover and independently reviewed recovery utility | In progress | Migration storage tests; conversion review/shadow report pending |

## Use-case traceability

The per-case rows below remain acceptance gates until their end-to-end evidence is recorded. “Pending” does not imply that the associated source has not been implemented.

| ID | Requirement | Stage | Status | Evidence |
|---|---|---|---|---|
| UC01 | Give one User one read Tool: Put the User in a dedicated Team and the Tool in a scoped Tool Group; a complete group grant permits only the action/resource/bound group. | S2/S7 | Pending | No implementation claim |
| UC02 | Add a new Finance employee: Team membership inherits Finance grants only after identity/account activation. | S3/S7 | Pending | No implementation claim |
| UC03 | User joins Finance and IT Teams: Union valid complete rules; no cross-product of one Team's tools and the other's devices. | S2/S7 | Pending | No implementation claim |
| UC04 | Assign one Role to several Teams: The same bounded rule set applies to enabled members; team membership remains separate. | S2/S7 | Pending | No implementation claim |
| UC05 | User inherits Roles from several Teams: Resolve complete rules with provenance; applicable BLOCK still wins. | S2/S7 | Pending | No implementation claim |
| UC06 | Remove a User from a Team: Remove that Team's grants and delegations; independent grants from other Teams remain visible, and last-membership removal cannot orphan the User. | S3/S7 | Pending | No implementation claim |
| UC07 | Disable a Team or Role: Stop inherited grants without deleting memberships or audit history. | S3/S7 | Pending | No implementation claim |
| UC08 | Give a contractor limited access: Explicit expiry, allowed resources and Device Groups; expiry denies without manual cleanup. | S3/S7 | Pending | No implementation claim |
| UC09 | Auditor inspects configuration: Read-only management scope; no implied runtime execution or secret access. | S2/S7 | Pending | No implementation claim |
| UC10 | Administrator manages devices but cannot run payment Tools: Management permission and tool-use grants remain independent. | S2/S7 | Pending | No implementation claim |
| UC11 | Team grants read while Role grants write: Each action requires its own applicable tuple; read does not imply write. | S2/S7 | Pending | No implementation claim |
| UC12 | Conflicting grant and explicit denial: Applicable BLOCK denies across all sources; explanation identifies the denial safely. | S2/S7 | Pending | No implementation claim |
| UC13 | Grant a Tool Group containing many Tools: Include only enabled members within granted actions, resources and bound device scope. | S2/S7 | Pending | No implementation claim |
| UC14 | Newly published Tool enters a privileged Tool Group: Start in default-tools; show inherited-access impact before reviewed movement into the privileged group. | S1/S7 | Pending | No implementation claim |
| UC15 | Tool moves from reporting to payment group: Recompute affected discovery, grants and Agent capabilities; no stale group-derived rights. | S1/S7 | Pending | No implementation claim |
| UC16 | Isolate one dangerous Tool: Move it to a restricted Tool Group, or disable it pending review; do not attach a direct ACL to the Tool. | S2/S7 | Pending | No implementation claim |
| UC17 | Tools require different execution Device Groups: Place them in separate Tool Groups with explicit bindings; no per-Tool override. | S1/S7 | Pending | No implementation claim |
| UC18 | Tool requires multiple actions such as read and delete: Return allowed actions; only explicitly permitted actions execute. | S4 | Pending | No implementation claim |
| UC19 | Tool receives a resource outside the allowed folder/account/project: Server extraction and canonical resource matching deny it before the effect. | S4 | Pending | No implementation claim |
| UC20 | Installed client advertises an unregistered Tool: Installation inventory cannot create a Tool grant or trusted definition. | S4 | Pending | No implementation claim |
| UC21 | Approved Tool version changes: Review definition/extractor changes and runtime assignment; stale/incompatible permits fail. | S4 | Pending | No implementation claim |
| UC22 | Tool is disabled or its group is disabled: Remove discovery and deny queued or new executions using that binding. | S2/S7 | Pending | No implementation claim |
| UC23 | New Device enrolls: Join `default-devices`; still require identity review, enablement, approval and runtime grants. | S1/S7 | Pending | No implementation claim |
| UC24 | Administrator moves Device into Production: Atomic membership change, revision check, audit and impact preview; no membership-free intermediate state. | S1/S7 | Pending | No implementation claim |
| UC25 | Device belongs to two departmental groups: A Tool requires access to its bound group; the other membership cannot substitute. | S2/S7 | Pending | No implementation claim |
| UC26 | User accesses a shared department Device: Target trust and caller identity are checked separately; group grants do not impersonate the Device owner. | S2/S7 | Pending | No implementation claim |
| UC27 | User can access a Device Group but no Tool Group: No Tools become executable from device eligibility alone. | S2/S7 | Pending | No implementation claim |
| UC28 | User can access a Tool Group but not its Device Group: Bound Tools are hidden or denied in that target context. | S2/S7 | Pending | No implementation claim |
| UC29 | Tool executes on a shared server rather than caller's laptop: Evaluate the execution Device; apply separate caller-device posture if policy requires it. | S4 | Pending | No implementation claim |
| UC30 | Remove Device from the Tool's group while a job waits: Recheck binding before dispatch/effect; deny the queued request. | S4 | Pending | No implementation claim |
| UC31 | Device key is revoked, approval expires or owner account is disabled: Fail the live trust/registry gate regardless of cached policy or green availability. | S3/S7 | Pending | No implementation claim |
| UC32 | Last group membership is removed: Reject mutation; default membership may be replaced only with at least one other group. | S1/S7 | Pending | No implementation claim |
| UC33 | Disable a Device Group: All execution bindings requiring it stop granting; another enabled membership does not revive them. | S2/S7 | Pending | No implementation claim |
| UC34 | Target is offline or Tool runtime is absent: Report unavailable safely; do not silently reroute to an unapproved Device. | S4 | Pending | No implementation claim |
| UC35 | Agent can use Tool but User cannot: Deny delegated execution. | S2/S7 | Pending | No implementation claim |
| UC36 | User can use Tool but Agent cannot: Hide/deny in that Agent context; authorized direct use may still be permitted. | S2/S7 | Pending | No implementation claim |
| UC37 | Agent Groups supply no applicable capabilities: Discover no Tools and execute none; default membership does not supply a grant. | S2/S7 | Pending | No implementation claim |
| UC38 | Agent needs all safe reporting Tools but no payment Tool: Map only the reporting Tool Group; payment Tools belong to a separate restricted group. | S2/S7 | Pending | No implementation claim |
| UC39 | Team shares an assistant: Require allowed delegation from the requesting User/Team; evaluate that User's grants. | S2/S7 | Pending | No implementation claim |
| UC40 | Agent owner is a Super Admin: Ownership supplies no additional runtime rights to the Agent or its delegated Users. | S2/S7 | Pending | No implementation claim |
| UC41 | Scheduled automation has no human present: Use explicit service/workload identity and grants; retain actor attribution. | S2/S7 | Pending | No implementation claim |
| UC42 | Agent attempts to change userId or tenantId: Reject untrusted identity changes before policy evaluation. | S2/S7 | Pending | No implementation claim |
| UC43 | Agent delegates to another Agent: Verified, bounded chain; downstream scope cannot exceed any upstream ceiling. | S2/S7 | Pending | No implementation claim |
| UC44 | Agent credential expires or mapping is revoked mid-workflow: Recheck subsequent protected effects; already completed effects remain audited. | S5/S6 | Pending | No implementation claim |
| UC45 | Retry changes target or arguments under the same request key: Reject conflicting replay; an identical retry cannot duplicate the protected effect. | S5/S6 | Pending | No implementation claim |
| UC46 | Payment/deployment requires independent approval: ASK binds reviewer, subject, actor, target, action, resource, arguments and expiry; no self-approval. | S5/S6 | Pending | No implementation claim |
| UC47 | User or Agent loses required grant after approval: Approval cannot revive the revoked capability; deny before effect. | S5/S6 | Pending | No implementation claim |
| UC48 | Temporary incident elevation: Explicit role/grant with incident reference, expiry and bounded targets; record activation and expiry. | S5/S6 | Pending | No implementation claim |
| UC49 | Break-glass recovery: Dedicated emergency workflow, signed authority, narrow time window, strong review/audit; no general Super Admin bypass. | S5/S6 | Pending | No implementation claim |
| UC50 | Require corporate network, device posture or business hours: Trusted contextual attributes must satisfy policy; missing or stale evidence fails closed. | S4 | Pending | No implementation claim |
| UC51 | Region/data-residency restriction: Binding plus trusted location attributes keeps execution/data within allowed scopes. | S4 | Pending | No implementation claim |
| UC52 | High-value payment or unusual invocation rate: Trusted argument extraction and approved thresholds add ASK/BLOCK or bounded limits. | S4 | Pending | No implementation claim |
| UC53 | Maker/checker separation of duties: Requester cannot approve their own request; policy/role authoring and publication may require separate administrators. | S5/S6 | Pending | No implementation claim |
| UC54 | Bulk disable during an incident: Invalidate applicable live grants, queue dispatch and fresh permits; expose rollout/adoption state. | S5/S6 | Pending | No implementation claim |
| UC55 | Two administrators edit a mapping concurrently: Revision conflict prevents lost updates; reload and review before another mutation. | S1/S7 | Pending | No implementation claim |
| UC56 | Tenant A names Tenant B's Tool/Device/User: Reject without revealing the other tenant's inventory. | S2/S7 | Pending | No implementation claim |
| UC57 | Gateway/Control outage or stale signed policy: Follow bounded freshness rules; unavailable or expired authority never permits new protected effects. | S5/S6 | Pending | No implementation claim |
| UC58 | Offline client requests execution: Default deny for enterprise operations requiring live authorization; any future offline capability needs explicit narrow leases and a declared revocation bound. | S5/S6 | Pending | No implementation claim |
| UC59 | Permission removed during a long-running operation: Recheck at protected checkpoints; stop remaining effects where safe and audit partial completion. | S5/S6 | Pending | No implementation claim |
| UC60 | Restore backup or roll back policy: Never reset identity revisions or replay old permits; publish a new monotonic version and evaluate current membership/trust. | S5/S6 | Pending | No implementation claim |
| UC61 | Access review finds dormant grants: Show source and affected operations; revoke with audit and validate resulting effective access. | S2/S7 | Pending | No implementation claim |
| UC62 | Dry-run policy against a production request: Return decision/reason/provenance with no effect, no approval consumption and no secret disclosure. | S2/S7 | Pending | No implementation claim |
| UC63 | Nested groups or dynamic identity-provider Teams are introduced: Explicit supported rules, trusted synchronization and cycle/depth limits; no inferred or stale membership expansion. | S2/S7 | Pending | No implementation claim |
| UC64 | Orphaned owner, deleted group or retired identifier: Preserve history, reject dangling active references and prevent identifier reuse from reviving access. | S1/S7 | Pending | No implementation claim |
| UC65 | First access by a verified unknown human identity: Register exactly one disabled User, assign default Team, show it in Users and deny the attempted resource access. | S3/S7 | Pending | No implementation claim |
| UC66 | Administrator enables a pending User: Explicit audited enablement; runtime/portal access still depends on that User's Team grants and Roles. | S3/S7 | Pending | No implementation claim |
| UC67 | Previously disabled User tries again: Update bounded attempt metadata; never automatically enable or create a duplicate identity. | S3/S7 | Pending | No implementation claim |
| UC68 | Unauthenticated caller supplies a fake User identity: Reject authentication; log a safe failed-authentication event without manufacturing a registered User. | S3/S7 | Pending | No implementation claim |
| UC69 | Tool or User loses its last group during deletion/import: Reject or atomically transfer to the corresponding default; no ungrouped committed entity. | S1/S7 | Pending | No implementation claim |
| UC70 | Same verified identity reaches several Gateways concurrently: Stable issuer/subject/tenant binding and uniqueness produce one disabled User and one valid default membership. | S3/S7 | Pending | No implementation claim |
| UC71 | New Agent is registered: Assign default-agents atomically; no direct capabilities, no inherited runtime access from default membership. | S1/S7 | Pending | No implementation claim |
| UC72 | Agent joins an authorized Agent Group: Inherit only complete group capabilities and compatible Actor Roles; existing individual credentials remain distinct. | S1/S7 | Pending | No implementation claim |
| UC73 | Agent belongs to several Agent Groups: Union complete rules without mixing one group's Tool scope with another's Device Group or delegation scope. | S2/S7 | Pending | No implementation claim |
| UC74 | Team can delegate to Agent Group A, Agent has a capability only through B: Deny that path; the capability and delegation must agree on the same enabled Agent Group. | S2/S7 | Pending | No implementation claim |
| UC75 | Agent Group is disabled or Agent leaves it: Remove that group's capabilities and recheck queued/new effects; independent valid group paths may remain. | S2/S7 | Pending | No implementation claim |
| UC76 | Agent loses its last group or its custom group is deleted: Reject or atomically move to default-agents; never commit an orphan or retain the removed group's grants. | S1/S7 | Pending | No implementation claim |
| UC77 | One Agent needs exceptional restricted capabilities: Create a dedicated scoped Agent Group and assign membership; never create a direct Agent ACL. | S2/S7 | Pending | No implementation claim |
| UC78 | Group permits delegated operations but not unattended automation: Service-mode requests remain denied until an explicit service grant and verified workload binding exist. | S2/S7 | Pending | No implementation claim |
| UC79 | Several Agents share a group: Group permissions are shared, credentials are individual, and audit identifies the actual requesting Agent. | S2/S7 | Pending | No implementation claim |
| UC80 | Administrator opens the Agents menu: Agent inventory and group mappings are managed under their own menu; no direct permission editors appear on individual details. | S1/S7 | Pending | No implementation claim |

## Additional acceptance cases

| ID | Acceptance boundary | Stage | Status | Evidence |
|---|---|---|---|---|
| AC01 | Each authorization dimension independently denies | S1–S8 | Pending | No implementation claim |
| AC02 | No Device Group bridge or cross-product | S1–S8 | Pending | No implementation claim |
| AC03 | Same semantics for human, service and delegated modes | S1–S8 | Pending | No implementation claim |
| AC04 | Discovery eligibility and ASK separation | S1–S8 | Pending | No implementation claim |
| AC05 | BLOCK survives narrowing and approvals | S1–S8 | Pending | No implementation claim |
| AC06 | Original binding and live rechecks at all handoffs | S1–S8 | Pending | No implementation claim |
| AC07 | Atomic audited membership/grant concurrency | S1–S8 | Pending | No implementation claim |
| AC08 | Monotonic snapshots and freshness bounds | S1–S8 | Pending | No implementation claim |
| AC09 | Shared execution preserves caller identity | S1–S8 | Pending | No implementation claim |
| AC10 | Exact approval/permit/retry binding and replay protection | S1–S8 | Pending | No implementation claim |
| AC11 | Accessible complete UI and impact/activation states | S1–S8 | Pending | No implementation claim |
| AC12 | Redacted audit and provenance | S1–S8 | Pending | No implementation claim |
| AC13 | All creation/import/sync/delete paths prevent orphans | S1–S8 | Pending | No implementation claim |
| AC14 | Concurrent verified first access creates one disabled User | S1–S8 | Pending | No implementation claim |
| AC15 | No direct individual ACL or Role assignment | S1–S8 | Pending | No implementation claim |
| AC16 | Same Agent Group delegation/capability path | S1–S8 | Pending | No implementation claim |

## Validation log

- Baseline working changes: 123 UI tests passed; 79 contract tests and 26 CI tests passed. These are baseline evidence, not proof of the target enterprise implementation.
- Repository contains no applicable AGENTS.md. Complete implementation instruction attachment read before changes in this run.
