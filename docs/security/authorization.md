# Authorization

Managed tool execution uses a trusted registered device, current owner/device enablement, installed-client connection approval, and the existing agent/tool/action/resource filters before every protected effect. Registration, green availability or deployment never grants execution.

See [Device registry and tool-call controls](../control-plane/device-registry.md).

## Purpose

Decision is deterministic over user/team/agent/device/tool/action/resource/arguments/context. LLMs never decide security.
