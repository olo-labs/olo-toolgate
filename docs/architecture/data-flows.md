# Core Data Flows

Current execution flow: tool call → trusted device identity → owner/device enablement → installed-client approval/deadline → agent/device/tool/resource filters → fresh authorization/permit → selected executor → rechecked result and audit.

See [Device registry and tool-call controls](../control-plane/device-registry.md).

## Marketplace Import

```text
Admin
 -> Control Plane
 -> Marketplace API
 -> verify release/signature
 -> download artifacts
 -> organization scan
 -> artifact mirror
 -> local package UNDER_REVIEW
 -> configure
 -> approve
 -> deploy
```

## Client Deployment

```text
Control Plane desired state
 -> Endpoint Client
 -> download mirrored artifact
 -> verify
 -> stage
 -> prepare runtime
 -> self-test
 -> atomic activate
 -> report READY
```

## Tool Execution

```text
AI
 -> Endpoint Client or Gateway
 -> resource extraction
 -> policy
 -> ALLOW / ASK / BLOCK
 -> short-lived permit if needed
 -> execute
 -> audit
```

## Publish Custom Tool

```text
Tool Author
 -> Custom Tool Builder
 -> test on managed test client
 -> sanitize
 -> public package draft
 -> Marketplace API
 -> worker scan/build
 -> review
 -> signing
 -> immutable publication
```
