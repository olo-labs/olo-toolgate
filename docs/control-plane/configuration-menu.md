# Console Configuration menu

## Purpose

This document specifies the **Configuration** menu of the administration console: a
new sidebar group with Server, Device and Backup/Restore pages, and the field-level
settings save behind them.

> **Status: frozen specification, not yet available.** This is a design gate D1
> artifact. It is implemented in milestone M1; the current console does not have this
> menu yet. Changes after D1 passes need an ADR amendment. Source:
> [Tool SDK plan §18](../sdk-plan/plan.md).

## Current state

- Sidebar groups are defined in `navigationGroups` at `apps/admin-ui/src/App.tsx:30`.
  The `configuration` route ("Configuration reviews") sits in the **Audit** group.
- That page (`apps/admin-ui/src/ConfigurationRequests.tsx`) renders, above the review
  queue, the `ServerSettings` component (`apps/admin-ui/src/ServerSettings.tsx`) and the
  `ConfigurationTransfer` component (`apps/admin-ui/src/ConfigurationTransfer.tsx`).
- `ServerSettings` has a **Gateway** fieldset, a **Device approval** fieldset and an
  "Import and export settings" area. It saves the whole record with
  `PUT /api/control/v1/settings`.

## Layout

The console must add a sidebar group **Configuration** with three pages. Their contents
move unchanged from Audit > Configuration reviews.

| Page | Route | Contents | Moved from |
|---|---|---|---|
| Server | `#config-server` | **Gateway**: Gateway name (default from `TOOLGATE_GATEWAY_NAME`) | `ServerSettings` Gateway fieldset |
| Device | `#config-device` | **Device approval**: auto-approve, duration, owner. **Check-in**: effective values, read-only | `ServerSettings` Device approval fieldset; check-in is new |
| Backup/Restore | `#config-backup` | **Import and export configuration**, plus the server settings file import/export | `ConfigurationTransfer`, and the settings import/export from `ServerSettings` |

```text
Sidebar                      Configuration > Server (#config-server)
-------------------------    -------------------------------------------
Overview                     [Gateway]
> Audit                        Gateway name  [______________]
    Audit log                  (default from TOOLGATE_GATEWAY_NAME)
    Approvals                                               [Save]
    Configuration reviews
    Effect outcomes          Configuration > Device (#config-device)
    Access simulation        -------------------------------------------
> Configuration              [Device approval]
    Server                     [x] Auto approve devices
    Device                     Auto approve duration (days) [___]
    Backup/Restore             Owner user ID for auto-approved devices [___]
> Tools                                                     [Save]
> Devices                    [Check-in]  (read-only, set by environment)
  ...                          Check-in interval       2000 ms
                               Jitter                  20 %
                               Offline after           120000 ms

                             Configuration > Backup/Restore (#config-backup)
                             -------------------------------------------
                             [Import and export configuration]
                               [Export complete configuration]
                               Import mode  [Replace | Merge]
                               Files [...]  [Preview import]
                             [Server settings file]
                               [Export server settings]
                               Server settings JSON file [...]
```

## Rules

- Audit > Configuration reviews must keep only the review queue.
- Old `#configuration` links must redirect to `#config-server`. Links that name a
  review (`#configuration?id=<id>`, built in `apps/admin-ui/src/Failure.tsx:7`) must
  still open that review in the queue.
- The Check-in fieldset is read-only. Its values are environment-owned and come from
  the variables in [Client check-in](configuration.md#client-check-in-specification-design-gate-d1).
  The page shows the effective values and says they are set by the deployment.
- Server and Device each edit part of one `ControlServerSettings` record. Each page
  must send only its own fields.

## Saving

Saving uses field-level PATCH:

```text
PATCH /api/control/v1/settings
Content-Type: application/merge-patch+json
If-Match: "<revision>"
Idempotency-Key: <key>

{"gatewayName": "Production gateway"}
```

- `If-Match` carries the quoted revision the page loaded.
- A stale revision returns `409` with the current record, as the existing PUT does.
  The UI must show "changed elsewhere, review and retry" and load the current values
  instead of overwriting them.
- The new fields raise `ControlServerSettings.formatVersion` to 2.
- Environment-owned values (such as the check-in values) are excluded from the startup
  configuration import.
- The existing full-record `PUT /api/control/v1/settings` stays for import, with
  strict compare-and-swap on the revision.

## Failure Behavior

| Case | Result |
|---|---|
| Revision changed since the page loaded | `409` with the current record; UI shows "changed elsewhere, review and retry" |
| Retried save with the same `Idempotency-Key` | Same result as the first attempt; no second change |

## Later items

Later additions to this menu, not part of M1:

- trusted issuers and introspection;
- Tool Host pools;
- egress approvals;
- kill switch;
- allowed runtime tiers;
- compatibility profiles.

## Related Docs

- [Admin UI](admin-ui.md)
- [Control Plane configuration](configuration.md)
- [Import and export](import-export.md)
- [Tool SDK plan](../sdk-plan/plan.md)
