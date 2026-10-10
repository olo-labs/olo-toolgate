# Configuration Import/Export

## Purpose

All non-secret configuration round-trips through versioned JSON/YAML with validate, diff, dry-run and merge/replace. Plain secrets never export.

Module 02 implements formatVersion 1 for users/teams/agents/tools/policies/devices.
Use [API semantics](../api/control-plane-api.md) and the
[valid dry-run example](../examples/control-import.json). Exports have an optimistic
tenant revision; imports require it in both snapshot and If-Match. Unknown versions
are rejected. Credentials, identity-provider accounts, audit history and deployment
state are outside this configuration snapshot.

## Server settings

Server settings are a separate, small document (`ControlServerSettings`, formatVersion 1)
that the administrative Configuration page reads and saves through
`GET`/`PUT /api/control/v1/settings`. Saving requires `toolgate-admin`, an
`Idempotency-Key` and the current settings revision in `If-Match`. **Export server
settings** downloads the current document; importing a file loads its values into the
form for review, and nothing changes until an administrator saves.

| Setting | Default | Meaning |
|---|---|---|
| `autoApproveDevices` | `false` | New enrollments are approved at once, without an administrator decision |
| `autoApproveDurationDays` | `30` | Connection approval length for auto-approved devices, 1–3650 days |
| `autoApproveOwnerUserId` | none | Enabled directory user that owns auto-approved devices; required while auto-approval is on |

> **Security:** with auto-approval on, any device that can reach the enrollment
> endpoint receives an identity for the configured duration. Keys already registered
> are never changed. If the owner user is later disabled or removed, new enrollments
> fall back to manual approval. Every auto-approval writes an
> `ENROLLMENT_AUTO_APPROVE` audit entry next to `ENROLLMENT_CREATE`.

### Import during bring-up

At startup the control plane can seed server settings from a mounted folder and from
environment variables. Environment values override the file.

| Environment variable | Meaning |
|---|---|
| `TOOLGATE_CONFIG_IMPORT_DIRECTORY` | Folder holding an exported `server-settings.json` (its revision is ignored) |
| `TOOLGATE_SETTINGS_AUTO_APPROVE_DEVICES` | `true` or `false` |
| `TOOLGATE_SETTINGS_AUTO_APPROVE_DURATION_DAYS` | Days, such as `30` or `30d` |
| `TOOLGATE_SETTINGS_AUTO_APPROVE_OWNER` | Owner user ID for auto-approved devices |
| `TOOLGATE_CONFIG_IMPORT_OVERWRITE` | Default `false`: import only when no settings were saved yet. `true` replaces saved settings on every start |

The import uses the tenant in `TOOLGATE_CONTROL_ENDPOINT_TENANT_ID` and writes a
`SERVER_SETTINGS_IMPORT` audit entry. An invalid file or value stops startup. The
Quickstart Compose file mounts `deploy/compose/QuickStart/config` as the import folder.
