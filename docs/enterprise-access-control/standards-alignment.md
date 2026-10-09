# Enterprise access control and established practice

## Purpose

Record the distinction between established access control principles and ToolGate's
specific enterprise requirements. This is an architectural comparison, not a claim
of standards certification.

Role-based access control assigns permissions to roles. Attribute-based checks
evaluate trusted subject, resource, action and environmental facts. ToolGate combines
these approaches with group membership and explicit execution bindings.

References: [NIST RBAC](https://csrc.nist.gov/projects/role-based-access-control),
[NIST ABAC](https://csrc.nist.gov/projects/attribute-based-access-control).

## Established patterns

Tenant isolation, least privilege, default deny, explicit service identities,
separation of duties, bounded credentials, revocation, transactional audit and
authorization immediately before protected effects are the implementation baseline.
Authentication proves identity; account enablement, ownership and installation do
not themselves grant permission. Resource attributes and execution targets remain
part of each complete authorization decision.

## ToolGate-specific choices

| Choice | Relationship to established practice |
| --- | --- |
| All assignments pass through groups | A stricter administrative convention. RBAC permits user-to-role assignments; ToolGate intentionally forbids them. |
| Four protected default groups | An inventory and orphan-prevention convention. Membership creates no implicit permission. |
| New verified humans register disabled | A review-gated onboarding policy. Automatic provisioning and enablement are also used in enterprises; ToolGate requires explicit enablement. |
| Same-Team grant and delegation witness | A product constraint preventing unrelated membership combinations from widening delegation. |
| Same-Agent-Group capability and delegation witness | A product constraint preserving the authority of the delegated group. |
| Typed Human, Actor/Service and Management Roles | A product representation of separation between human use, workload execution and administration. |
| Mandatory Tool Group execution bindings | A routing and authorization constraint. Each invocation chooses one binding; overlapping device memberships supply no substitute. |
| Online authority for high-risk effects | A fail-closed deployment choice. Offline execution is outside the mandatory implementation. |

These choices implement the supplied specification. Exceptions should use narrowly
scoped, reviewed, expiring group grants, with provenance and revocation. They must
not introduce direct individual ACLs or make default membership a hidden grant.

## Implementation decisions requiring explicit disclosure

The Gateway delegates current authorization to Control rather than reproducing a
second effect-authorizing evaluator in Rust. Signed graph snapshots support
distribution and adoption; they never authorize protected effects during an outage.
This centralizes semantics and makes authority availability an execution dependency.

Single-use permit reservation guarantees one durable reservation, not exactly-once
external effects. Unknown outcomes require downstream reconciliation or explicit
operator review before a destructive retry.
