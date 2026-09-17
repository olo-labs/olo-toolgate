# Gateway Fail-Safe

## Purpose

Control Plane down: continue with valid bundle. Approval unavailable: ASK→BLOCK. Vault unavailable: secret-dependent call fails. Expired privileged bundle: fail closed.

## Required Tests

- Success path.
- Failure path.
- Security edge cases.
- Metrics/audit emitted.
