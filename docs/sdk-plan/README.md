# Tool SDK plans

## Purpose

Design and delivery plans for the ToolGate Tool SDK and the governed MCP tool platform. The normative SDK contract lives in [tool-sdk/spec](../../tool-sdk/spec/README.md); these documents explain why and in what order.

| Document | Content |
|---|---|
| [plan.md](plan.md) | Full design, revision 10: authoring contract, package format, Tool Host, permits, tasks, kill switch, gates and milestones |
| [publishing-plan.md](publishing-plan.md) | How the SDKs, examples and docs are published to public registries |
| [implementation-plan.md](implementation-plan.md) | What to build, in which order and in which files, reconciled with the code at commit `8ea5091` |

Delivery is one pull request per phase, each merged before the next starts: design gates D0, D1, D2 and D3, then milestones M1 and M0, M2, M3 and later.
