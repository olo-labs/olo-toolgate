# User roles and fixed privilege templates

Use [managed roles](../development/managed-roles.md) for new assignments. Roles
combine fixed server-defined templates with validated device/tool scope. Users
receive roles directly or through enabled teams. Templates are attached to roles.

| Classification/template | Meaning |
|---|---|
| Super Admin | Portal and role management; signed admin/super-admin authority required. |
| Administrator | Portal and ordinary management; cannot manage roles or role-bearing teams. |
| Basic | No portal; explicitly assigned runtime/approval capabilities. |
| TOOL_USER | Policy-authorized tool calls within role device/tool scope. |
| IT_CLOUD_ADMIN | Explicit ALL or GROUPS device scope, bounded by policy. |
| APPROVER | Review approvals with signed approver authority; no self-approval. |

Role records never create credentials. Signed user_id binds an enabled directory
user. Portal/approval checks resolve current roles; verified groups alone cannot
override an explicit Basic profile. Existing unbound service principals retain
verified IdP authority for bootstrap/compatibility. Audit excludes credentials and
mutation payloads.

Disabled users, roles, teams and devices grant no new runtime access. Device scope
combines per user, preserving BLOCK precedence. Empty groups never become wildcard
scope. Role changes require a new signed bundle for Gateway enforcement; current
bundles retain normal version/expiry behavior.

Quickstart preserves its explicit Super Admin bootstrap identity. Its last enabled
record cannot be removed through CRUD or replacement import. Legacy portal
classifications remain a compatibility floor; managed role assignments replace
legacy runtime template grants. Deploy matching Control/UI/contracts versions.
Gateway bundle wire versions remain unchanged.

Tests cover direct/team escalation, signed-role disagreement, stale revisions,
references, disabled roles, invalid scopes/templates, per-user device separation
and last-root protection.
