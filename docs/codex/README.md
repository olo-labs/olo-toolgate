# OLO ToolGate — Codex Implementation Playbook

For current maintenance, preserve the device registry flow and uniform execution gates. Treat pending enrollment, enablement, connection approval and tool filters as separate checks. Support explicit unlimited approval with short-lived certificates, reversible HTTP 423 suspension, stable approval revisions and exact-key recovery. Verify tenant/owner binding and real effect denial; do not infer access from installation, green status or a registry row.

See [Device registry and tool-call controls](../control-plane/device-registry.md).

## Goal

This folder is the **implementation contract** for building OLO ToolGate with Codex or another coding agent.

The intent is:

> **Nothing important is optional, implicit, or postponed accidentally.**

Each module is implemented independently, but every module inherits the same mandatory requirements for:

```text
architecture
security
tests
documentation
observability
configuration
Docker
Kubernetes / Helm
CI/CD
release engineering
shared contracts
upgrade behavior
maintainability
licensing
copyright
```

## How to Use

For each coding session give Codex:

```text
docs/codex/00-MASTER-IMPLEMENTATION-PROMPT.md
docs/codex/09-REQUIREMENTS-TRACEABILITY.md
docs/codex/08-DEFINITION-OF-DONE.md
docs/codex/modules/<selected-module>.md
```

Then say:

```text
Implement ONLY this module.

Before coding:
1. read all mandatory repository docs referenced by the master prompt;
2. produce a requirements coverage plan;
3. map each applicable requirement ID to files/tests/artifacts you will create.

During coding:
- do not skip requirements;
- do not weaken architecture/security;
- do not start the next module.

Before finishing:
- execute all required checks;
- update the traceability table;
- prove each applicable requirement is COMPLETE or explicitly NOT APPLICABLE with justification.
```

## Recommended Implementation Order

```text
00 Foundation / Contracts / Build / CI
01 Gateway Core
02 Control Plane Backend
03 Admin UI
04 Signed Policy Bundles
05 ASK / Approval
06 Endpoint Client Foundation
07 HotFolder + Built-in Tools
08 Local Tool Runtime
09 Package Deployment / Desired State
10 Custom Local Tool Builder
11 Quickstart
12 Marketplace API
13 Marketplace Worker
14 Drupal Marketplace
15 Marketplace ↔ Control Integration
16 Production / Helm / HA / Observability
17 Security / Performance / Chaos Hardening
18 Release Candidate / GA
```

## Important

Do not give Codex only the master architecture document and ask it to implement the entire project in one enormous change.

Use the module prompts so:

- dependencies stay small;
- code stays reviewable;
- Git history stays understandable;
- architecture boundaries remain enforceable;
- failures can be isolated;
- tests remain meaningful.

The **whole project requirement set is still enforced in one go** because every module inherits the same requirement IDs and final GA verifies the complete matrix.
