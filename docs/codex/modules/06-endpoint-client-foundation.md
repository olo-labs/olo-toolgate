# Module 06 — Endpoint Client Foundation

For current maintenance, preserve the device registry flow and uniform execution gates. Treat pending enrollment, enablement, connection approval and tool filters as separate checks. Support explicit unlimited approval with short-lived certificates, reversible HTTP 423 suspension, stable approval revisions and exact-key recovery. Verify tenant/owner binding and real effect denial; do not infer access from installation, green status or a registry row.

See [Device registry and tool-call controls](../../control-plane/device-registry.md).

## Dependencies

Modules 00–02 and Gateway identity interfaces.

## Objective

Implement native client enrollment/device identity/check-in foundation.

## Mandatory Preparation

Read the master prompt and requirements traceability document.

Before coding produce the module requirement coverage table.

## Deliverables


- Rust client;
- Windows/Linux/macOS abstraction;
- device key generation/storage;
- server discovery;
- browser enrollment protocol;
- mTLS/device identity design implementation;
- heartbeat;
- reported state;
- local protected service architecture;
- safe IPC;
- health;
- structured logs;
- install/uninstall command framework;
- cross-platform CI builds.


## Mandatory Tests


- enrollment;
- revoked device;
- wrong server;
- key persistence;
- IPC authorization;
- reconnect;
- offline behavior;
- cross-platform compile.


## Deployment / Release


- GitHub Release packaging skeleton for OS/arch matrix;
- checksums/SBOM;
- signing hooks.


## Completion

Apply `docs/codex/08-DEFINITION-OF-DONE.md`.

Do not start another module.

## Suggested Commit

```text
feat(client): add endpoint client foundation
```
