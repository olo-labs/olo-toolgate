# ASK approvals and execution permits

Module 05 adds human review of an exact operation. Control stores human decisions;
Gateway remains the runtime authorizer. Existing PostgreSQL supplies durable
concurrency and replay protection. No Slack, Teams, queue or cache is required.
See [ADR 006](../adr/006-ask-approvals.md) and
[signed policy bundles](policy-bundles.md).

## Request and decision flow

1. An administrator creates an enabled ASK policy and publishes it. Policies
   containing ASK use signed format 2; ALLOW/BLOCK-only publications retain
   format 1. Gateway accepts both. Older Gateway versions reject format 2 safely.
2. The authenticated requester calls Gateway `/v2/authorize` with the ordinary
   shared `AuthorizationRequest`. Gateway extracts the resource and hashes
   canonical arguments locally. BLOCK wins over ASK; ASK wins over ALLOW.
3. Gateway sends the normalized exact input to Control with its machine identity.
   Control independently checks a fresh current published ASK and enabled directory
   identities. It creates or resolves a durable approval and returns PENDING.
   The runtime response contains ASK and an approval reference, without a permit.
4. A human connects to `/console/`, opens Approvals and reviews the exact scope.
   Choices are approve once, approve temporarily or deny. No broad persistent
   permission is created. The UI displays identity, operation, resource, arguments
   digest, policy version and deadlines; raw arguments and credentials are absent.
5. The requester retries authorization after approval. Gateway resolves online,
   rechecks current policy, signs a short-lived permit and acknowledges audit.
6. Before executing, a consumer sends the permit and exact operation to
   `/v1/permits/consume`. Gateway verifies its signature and binding, then Control
   atomically consumes jti. Repeated use fails. Module 05 implements this boundary;
   endpoint tool execution belongs to later modules.

Existing `/v1/authorize` and MCP retain their earlier response contracts and block
ASK. Integrations needing approvals use runtime v2; a naked ALLOW decision from
an approval UI or Control record is never an executable permit.

## Authorization and scope

`toolgate-approver` is the initial fixed tenant approver group. It is a dedicated
IdP role, independent of `toolgate-admin` and `toolgate-reader`. Human requests
also require a signed `user_id` claim that maps to an enabled directory user.
The requester cannot approve their own request. Cross-tenant access fails.
The IdP owns these claims; directory records and browser input cannot mint roles.

Machine resolve/consume APIs require `toolgate-approval-gateway`, verified issuer,
audience, tenant and bounded JWT lifetime. The machine role cannot decide requests.
Tokens combining machine and human approver roles are rejected by both interfaces.
Bundle fetching additionally requires `toolgate-bundle-reader` (or an existing
explicit administrative reader role). Keep machine and human identities separate.

Every approval binds tenant, user, agent, device including absence, tool, action,
extracted resource, arguments SHA-256 and exact published policy version.
Temporary grants exclude changing ingress request IDs from their operation scope;
each resulting permit still binds its own request ID and fresh jti. Changed
arguments, identity, device, resource or policy require a different approval.
BLOCK or stale policy always prevents execution, including during consumption.

## Durable states and races

PENDING transitions to APPROVED_ONCE, APPROVED_TEMPORARY, DENIED or EXPIRED.
APPROVED_ONCE transitions to CONSUMED when a lease is issued, or EXPIRED.
APPROVED_TEMPORARY expires at its deadline. DENIED and CONSUMED are tombstones
through their validity window; a different request ID cannot escape them.
After expiry, a later operation may start a new human workflow.

Pending requests last at most five minutes. Temporary grants last at most thirty
minutes and never beyond current policy expiry. Permit lifetime is at most ten
seconds, capped by the human grant, signed policy and runtime credential expiry.
Approval decision, expiry, grant issuance, lease consumption, audit and decision
idempotency use database transactions. Expected revisions prevent two humans
deciding the same request. One-time grant issuance and jti consumption each have
one atomic winner across replicas. A lost response conservatively loses the
one-time grant; automatic retries do not manufacture additional execution rights.

Expiry is evaluated and persisted on reads/resolution/decisions, so the API and UI
expose durable status rather than relying on background notifications. Clock
regression fails closed. Old policy versions cannot revive grants after rollback;
signed rollback publishes a new monotonic version. Database restore requires the
bundle sequence-floor procedure in the bundle runbook.

Control persists the authenticated clock observation even when a rejected business
transaction rolls back. A later replica cannot regain an expired lease by observing
an earlier wall time. Gateway signing and verification share a monotonic elapsed
clock and a high-water mark. Operators must still provide trusted UTC across cold
starts; these checks do not replace host clock management.

## Configuration

Control defaults to approvals disabled. Enable bundle signing first and set:

```text
TOOLGATE_CONTROL_APPROVAL_ENABLED=true
TOOLGATE_CONTROL_APPROVAL_PENDING_TTL_MS=300000
TOOLGATE_CONTROL_APPROVAL_MAX_ACTIVE=1000
TOOLGATE_CONTROL_APPROVAL_MAX_ACTIVE_PERMITS=4096
```

Pending TTL bounds are 1000–300000 ms; capacity settings are 1–10000. Active
request/lease caps, bounded pages and existing HTTP/rate limits provide backpressure.
History is authoritative PostgreSQL state; runtime credentials cannot delete it.
Archive expired history using reviewed operator retention procedures, preserving
required audit evidence and ensuring unexpired tombstones/leases are retained.

Gateway's optional `approval` configuration is:

```json
{
  "url": "https://control.example.invalid",
  "tokenPath": "/etc/toolgate/approval-auth/access-token",
  "privateKeyPath": "/etc/toolgate/permit-signing/private.pem",
  "keyId": "permit-key-1",
  "issuer": "gateway",
  "audience": "endpoint",
  "requestTimeoutMs": 1000,
  "permitLifetimeMs": 10000,
  "developmentLoopbackHttp": false
}
```

The fixed origin has no userinfo, path, query or fragment. TLS verification,
redirect/proxy rejection, bounded JWT/body reads and total deadlines apply.
Only literal loopback HTTP with the explicit development flag is supported for
isolated tests. Production Helm always disables it and requires intentional
Control/DNS egress. Approval timeout must leave room within the ingress deadline.
No grant cache or unavailable-service fallback exists.

The Gateway key is external PKCS8 RSA, 2048/3072/4096 bits with exponent 65537.
RS256 JWS uses protected type `toolgate-execution-permit+jws`; policy JWS, IdP
JWTs, Marketplace and deployment keys are different trust domains. Never reuse
their private keys. Permits include issuer/audience, full identity, exact operation
and resource, argument digest, request ID, policy version, approval ID, jti and
issue/expiry. Structural schema acceptance alone does not verify cryptography.

Helm adds `control.approval` behavior values and `gateway.approval` external
signing/token Secret references, key revision, origin, timeout and lifetime.
Gateway receives its permit key; Control receives its policy key. Neither receives
the other's private key. Changing key revision triggers a rollout. Plan Gateway
key rotation so old short-lived permits drain before removing old verification
capability; a process restart may conservatively invalidate outstanding permits.

## API, outages and operations

Canonical [Control OpenAPI](../../packages/contracts/openapi/control-v1.yaml) defines
list/get/decision/resolve/consume routes. Human decisions require
`Idempotency-Key` and `expectedRevision`. Reusing a key with a changed body conflicts.
Consumption never replays success. Responses use no-store and correlation IDs;
errors use the shared sanitized envelope. List pages are bounded and tenant scoped.

Control, database, JWT, TLS, signing or audit failure blocks ASK. Existing verified
ordinary ALLOW/BLOCK remains local; approval outages never grant extra rights.
Gateway liveness stays process based. Observe private health/readiness, bounded
operation/state metrics and durable Control audit (request creation, decision,
expiry, issuance and consumption). No raw arguments, JWTs, permits or private keys
belong in metrics, ordinary logs, notifications or configuration export.

On missing requests, check current published ASK, identities and version. On 403,
check tenant, dedicated role, signed user ID and self-approval. On 409, reload the
current record; a stale decision or spent permit cannot be retried as fresh.
On BLOCK after approval, check current policy, deadline, token refresh, Control
availability and signing/audit health. Notifications are an optional port, with
UI polling authoritative; notification absence never changes authorization.

## Verification and upgrade

Run `make check`, `make approval-e2e`, `make policy-e2e`, `make security` and
`make benchmark`. The browser gate uses real PostgreSQL/Control and four genuine
ASK requests. Production E2E exercises races, wrong/self approvers, one-time and
temporary grants, exact signed permit binding/consume, denial, expiry, outage and
policy revocation. Helm verifies install/upgrade/rotation and negative configs.

Flyway V4 is additive. Apply migrations with the existing dedicated migration
identity; keep runtime privileges limited. Rolling upgrade must place format-2
readers everywhere before publishing ASK. Software/chart rollback leaves schema,
approval history and spent leases intact. Earlier readers cannot operate ASK;
publish a reviewed ALLOW/BLOCK-only format-1 policy if intentionally reverting.
Product, contracts and chart are synchronized at `0.5.0-dev`.
