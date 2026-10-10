# ToolGate Tool SDK

## Purpose

`tool-sdk/` holds the ToolGate Tool SDK: one authoring contract for Java, Python, TypeScript and .NET tools that run as plain MCP servers (standalone mode) or under ToolGate governance (governed mode).

**Status:** design phase. This directory contains only the normative specification produced by design gate D0. No SDK code exists yet, and none may be written until gates D0, D1 and D2 have passed ([implementation plan](../docs/sdk-plan/implementation-plan.md)).

## Layout

| Path | Contents | Gate |
|---|---|---|
| [`spec/`](spec/README.md) | Normative authoring contract, descriptor rules, runtime modes, composition, protocol layer, official SDK compatibility record | D0 |
| `conformance/` | Conformance cases (added at D3) | D3 |
| `cli/`, `java-sdk/`, `python-sdk/`, `typescript-sdk/`, `dotnet-sdk/`, `examples/`, `docs/` | Implementation (added at M0) | M0 |

## Related Docs

- [Design plan](../docs/sdk-plan/plan.md)
- [Publishing plan](../docs/sdk-plan/publishing-plan.md)
- [Implementation plan](../docs/sdk-plan/implementation-plan.md)
- [ADR 019](../docs/adr/019-tool-sdk-authoring-contract.md)
