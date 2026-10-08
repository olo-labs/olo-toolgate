# Module 11 — One-Minute Quickstart

For current maintenance, preserve the device registry flow and uniform execution gates. Treat pending enrollment, enablement, connection approval and tool filters as separate checks. Support explicit unlimited approval with short-lived certificates, reversible HTTP 423 suspension, stable approval revisions and exact-key recovery. Verify tenant/owner binding and real effect denial; do not infer access from installation, green status or a registry row.

See [Device registry and tool-call controls](../../control-plane/device-registry.md).

## Dependencies

Core runtime modules through 10.

## Objective

Deliver the README promise: one container, almost immediate useful experience.

## Mandatory Preparation

Read the master prompt and requirements traceability document.

Before coding produce the module requirement coverage table.

## Deliverables


- one Quickstart image;
- Gateway + Control + Admin UI integration;
- embedded SQLite/local state;
- built-in encrypted vault;
- bootstrap password rules;
- default workspace/team/agent/policy;
- built-in tools;
- client downloads/enrollment endpoint;
- persistent `/data`;
- upgrade-safe local layout;
- prominent non-HA indicator.


## Mandatory Tests


- clean first boot;
- restart/persistence;
- bootstrap password;
- built-in tool call;
- ASK;
- client enrollment;
- version upgrade;
- backup/restore smoke.


## Deployment / Release


- publish `olo-toolgate-quickstart`;
- Docker smoke in CI;
- SBOM/scan/provenance.


## Completion

Apply `docs/codex/08-DEFINITION-OF-DONE.md`.

Do not start another module.

## Suggested Commit

```text
feat(quickstart): deliver one-container ToolGate experience
```
