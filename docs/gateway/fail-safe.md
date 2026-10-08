# Gateway Fail-Safe

Disabled, deapproved, expired-approval and unavailable devices cannot execute tools. Temporary client suspension uses HTTP 423 with retryable identity retention; irreversible revocation continues rejecting the retired key. Missing readiness is never shown green.

See [Device registry and tool-call controls](../control-plane/device-registry.md).

## Purpose

Control Plane down: continue with valid bundle. Approval unavailable: ASK→BLOCK. Vault unavailable: secret-dependent call fails. Expired privileged bundle: fail closed.

## Required Tests

- Success path.
- Failure path.
- Security edge cases.
- Metrics/audit emitted.
