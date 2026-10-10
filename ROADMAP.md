# Roadmap

## Purpose

This file summarizes what ToolGate already delivers and where it is going next. The
detailed gates and milestones for the Tool SDK work live in
[the Tool SDK plan, §22](docs/sdk-plan/plan.md).

This roadmap describes direction, not guaranteed dates.

## Delivered

The original phases are summarized here; the module completion records are in
[docs/codex/modules](docs/codex/modules/).

- **Foundation:** monorepo, canonical schemas, CI, `make dev`, threat model,
  architecture decisions, benchmark harness.
- **Quickstart and Gateway:** single-node Quickstart, stateless Gateway, signed policy
  bundles, ALLOW/BLOCK, built-in safe tools, audit and metrics.
- **Control Plane:** users, teams, policies, the administration console, JSON/YAML
  import and export, tool registry and policy simulation.
- **ASK and approvals:** approval requests, approve once, temporary grants and runtime
  correlation.
- **Endpoint client:** Windows, Linux and macOS clients, enrollment, HotFolder,
  desired and reported state, local tool runtime and signed deployment assignment.
- **Custom tool builder:** Python, JavaScript, PowerShell and existing executables,
  input and output schemas, test client and package creation.

## Tool SDK and governed tools

This is the current focus. Design and coding are tracked separately: a **design gate**
produces frozen design artifacts, and a **coding milestone** implements against them.
No coding milestone starts until D0, D1 and D2 have passed. See
[plan §22](docs/sdk-plan/plan.md) for the full tables, owners and exit criteria, and
[the implementation plan](docs/sdk-plan/implementation-plan.md) for the work items.

### Design gates

| Gate | Content | Status |
|---|---|---|
| **D0: Authoring contract frozen** | SDK authoring contract, runtime modes, standalone identity, official MCP SDK compatibility record, [ADR 019](docs/adr/019-tool-sdk-authoring-contract.md) | Done |
| **D1: Contracts frozen** | v2 schemas and fixtures, MCP version model, OpenAPI changes, client check-in variables, Configuration menu specification, ADR renumbering, this roadmap | In progress (this change) |
| **D2: Trust and persistence reviewed** | Threat model for Tool Host, broker, egress and transports; persistence transitions and transaction model; invocation table migrations | Not passed |
| **D3: Conformance authored** | Fixtures and cases for every conformance suite (written, not yet passing) | Not passed |

### Coding milestones

| Milestone | Priority | Content |
|---|---|---|
| **M0: SDK standalone preview** | P0 | All four SDKs in standalone mode, with starters, examples and docs. Runs in parallel with M1. |
| **M1: Contract implementation** | P0 | Generated v2 bindings, validators, migrations, the MCP version model in the Gateway, the Configuration menu and settings PATCH, client check-in Phase A. Runs in parallel with M0. |
| **M2: Foundation** | P0 | Packager, governed Java runtime, package lifecycle, Tool Host with egress isolation, permits and admission, tasks, kill events, modern MCP and legacy adapter. |
| **M3: Adoption** | P0 | MCP proxy and OpenAPI import. |
| **M4: Enterprise reliability** | P1 | Full task experience, worker recovery, break-glass, connections, fleet deltas, client Phase B. |
| **M5: Multi-language governed** | P1 | Governed runtimes for Python, TypeScript and .NET. |
| **M6: Distribution and extensions** | P1/P2 | Marketplace, cohort rollouts, container kind, workload identity, air-gapped bundles. |

## Later

These earlier roadmap themes remain direction and are not yet scheduled:

- **Marketplace platform:** community UI, Marketplace API and worker, package signing,
  import, publishing, moderation and advisories (part of M6).
- **Strong endpoint enforcement:** credential custody, protected HotFolder,
  OS-specific enforcement and privileged remote execution.
- **Enterprise scale:** high availability, external vaults, SCIM, private registries,
  advanced SIEM and audit, policy GitOps, advanced sandboxing and multi-region.
