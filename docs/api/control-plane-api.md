# Control Plane API

## Purpose

Users, teams, tools, policies, approvals, clients, deployments, vault bindings, import/export and audit.

Module 02 exposes `/api/control/v1/{users,teams,agents,tools,policies,devices}`:
GET collection/item, POST create, PUT update and DELETE. POST requires revision 1;
PUT uses the current body revision and quoted `If-Match`, returning revision +1.
DELETE requires quoted `If-Match`. Deleted IDs are permanently retired per tenant
and kind. References must exist; enabled records cannot depend on disabled records.
Policies require at least one explicit subject and a declared tool action/resource
kind. Only ALLOW/BLOCK records are accepted; no publication or runtime grant occurs.

All mutations require `Idempotency-Key`, scoped to verified tenant and actor, with
seven-day retention. Identical request replay returns the original status/body/ETag
without another mutation or audit. Reusing a key for different content fails 409.
Authorization precedes replay. List `limit` is 1–100; `nextCursor` is absent at the
end. Directory cursors are bound to tenant/kind; no tenant can read another tenant's
rows by supplying a cursor, ID or snapshot.

`GET /config/export` returns a `ControlSnapshot` with a tenant revision ETag; Accept
may be `application/json` or `application/yaml`. `POST /config/import` accepts
`ControlImportRequest` in either format, requiring `If-Match` to equal both current
tenant revision and snapshot revision. `formatVersion: 1` only is supported; unknown
versions are rejected for an explicit future migration. MERGE overlays input records;
REPLACE uses only input records. Imported record revisions are normalized against
existing records. Dry-run validates the entire resulting graph and returns a stable
CREATE/UPDATE/DELETE diff without writes/audit/replay storage. Applying changes commits
atomically; tenant revision advances once, changed record revisions advance once.
An applied no-op import still receives an audit record and replay protection.

YAML uses safe construction, no custom object tags, duplicate keys or collection
aliases. Unknown fields, secret/code fields, invalid IDs, invalid references, ASK,
remote tool schema references, stale revisions and quota violations are rejected.
Tool schema objects are inert data; no network resolution or code execution occurs.
Credentials and vault secret values have no representation in directory contracts.
Do not embed credentials in descriptions, names or schema data.

`GET /audit` is admin-only, with a numeric sequence cursor and bounded limit.
Audit entries carry opaque SHA-256 actor IDs, operation/target, tenant revision,
request ID, timestamp and request digest, without payloads or credentials.
Each security mutation and applied import is recorded in its own transaction.

`GET /openapi` requires read access and serves a self-contained spec assembled from
the [canonical OpenAPI](../../packages/contracts/openapi/control-v1.yaml) and shared
schemas; it introduces no copied wire definitions. Errors use `ErrorEnvelope` and
`X-Request-ID`, without exception details. Configuration, JWT claims, roles and
management endpoints are documented in [configuration](../control-plane/configuration.md).
Early HTTP transport body/header limits can reject before the REST error envelope
and correlation filter; the canonical specification documents that boundary.
