# Managed roles and permission scopes

Navigation groups are **Users** (Users, Teams, Roles, Agents), **Tools** (Tools,
Policies, Tool builder, Quickstart built-ins/vault), **Devices** (Clients, Enroll
device, Packages) and **Audit** (Audit log, Approvals). Overview stays separate.
Group headings expand/collapse submenus. Endpoint downloads remain on login and
Enroll device only.

A Super Admin creates, edits, enables, disables and deletes named roles under
**Users → Roles**, using the same revision and idempotency checks as Teams.
New roles start disabled. Remove user/team assignments before deleting referenced
roles. Disabling an assigned role contributes no privileges.

Each role has a portal classification and fixed privilege templates. Supported
identifiers are `TOOL_USER`, `IT_CLOUD_ADMIN` and `APPROVER`. These capabilities are
defined by the server and cannot be extended through JSON. Combine templates on
the role. Individual users receive role IDs directly or through team membership.
Identity-provider accounts, authentication and verified groups are still required.

The editor's permission JSON has `deviceScope`, `deviceGroupIds` and `toolIds`.
Selected templates are stored with those fields in the canonical `rules` object:

```json
{
  "templateIds": ["IT_CLOUD_ADMIN"],
  "deviceScope": "ALL",
  "deviceGroupIds": [],
  "toolIds": []
}
```

For selected cloud devices and tools:

```json
{
  "templateIds": ["IT_CLOUD_ADMIN", "APPROVER"],
  "deviceScope": "GROUPS",
  "deviceGroupIds": ["production-devices"],
  "toolIds": ["system-info", "log-reader"]
}
```

Omit `templateIds` from the UI JSON field and use its fixed checkboxes.
`GROUPS` requires at least one enabled Team with enabled `deviceIds`. `NONE` and
`ALL` require empty group lists. `NONE` grants no runtime tool access and suits an
approval-only role. An empty tool list uses tools allowed by policy; a nonempty
list narrows that scope to registered tools. Unknown fields, arbitrary expressions
and executable permission code are rejected.

Device/tool JSON narrows runtime tool calls. `APPROVER` is a fixed tenant-level
review capability; it still requires signed approver authority and the existing
approval checks. It is not a device-specific approval delegation template.

Assign role IDs in Users or Teams. Enabled members of enabled teams inherit
enabled roles. Multiple direct and team roles combine. Device scope is resolved
per user and intersected with policy device scope, preventing cross-user device
grants. Explicit BLOCK remains effective, and ASK still requires approval.
Roles do not bypass policies, install packages or execute remote tools themselves.

Only Super Admin can change role definitions, role assignments or membership/
enabled state of role-bearing teams. Administrators manage ordinary users and
teams without role assignments. Role-based portal access still needs matching
signed IdP groups. The original explicit Super Admin identity remains the recovery
anchor; the last enabled explicit Super Admin cannot be removed.

Publish a new signed policy bundle after role, team, device or policy changes.
Gateways keep the verified current bundle until replacement/expiry. Portal and
approval checks use current directory roles immediately. Existing direct profiles
are preserved on upgrade. Managed role assignments replace legacy runtime
template grants; legacy portal classifications remain a compatibility floor.

`/api/control/v1/roles` supports list/get/create/update/delete. Updates/deletes
require `If-Match` and `Idempotency-Key`. Exports include `roles`; older snapshots
without the optional array remain accepted. Replacement imports must retain all
referenced roles. Role-bearing imports require Super Admin and preserve references
and the recovery identity.

PostgreSQL V9 extends the directory kind constraint. SQLite V3 transactionally
rebuilds the directory table while retaining records and revisions. Older images
reject V3 state: back up `/data` before upgrading. Deploy matched Control, UI and
shared contracts because old closed-schema readers cannot read the new fields.
Gateway signed bundle wire formats remain unchanged.

Validation covers role CRUD/stale revisions, retained-state reopening, schema and
unknown-template rejection, inherited device scope, disabled roles, signed portal
authority, ordinary-admin escalation denial, UI scope editing, navigation and
browser role/team/audit flows.
