# Gateway Request Lifecycle

Managed tool execution uses a trusted registered device, current owner/device enablement, installed-client connection approval, and the existing agent/tool/action/resource filters before every protected effect. Registration, green availability or deployment never grants execution. Quickstart’s REST-forwarding device gates the relay in addition to the target client’s registry and filters.

See [Device registry and tool-call controls](../control-plane/device-registry.md).

## Purpose

TLS → identity → agent/device → normalize tool/arguments → resource extraction → policy → approval/allow/block → credential resolution → upstream/local permit → audit → response.

## Required Tests

- Success path.
- Failure path.
- Security edge cases.
- Metrics/audit emitted.
