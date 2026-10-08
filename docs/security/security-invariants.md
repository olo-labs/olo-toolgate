# Security Invariants

Managed tool execution uses a trusted registered device, current owner/device enablement, installed-client connection approval, and the existing agent/tool/action/resource filters before every protected effect. Registration, green availability or deployment never grants execution. Unlimited connection approval never grants permanent certificates or wider tool/resource permissions.

See [Device registry and tool-call controls](../control-plane/device-registry.md).

## Purpose

Default deny; policy error never ALLOW; ASK never bypasses approval; secrets never appear in ordinary exports/logs; invalid signatures fail; client never executes unassigned package.
