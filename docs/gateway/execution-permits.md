# Execution Permits

## Purpose

Permits bind user, agent, device, tool, operation, resource and request hash. They are short-lived, signed and optionally single-use with distributed replay protection.

The [device registry](../control-plane/device-registry.md) supplies trusted execution
identity. Current owner/device enablement and installed-client approval remain
effect-boundary gates; a previously issued permit cannot bypass suspension.
Finite approval caps tool leases at its deadline. Unlimited approval does not
extend permit lifetime, broaden resource filters or remove consumption/replay checks.

## Required Tests

- Success path.
- Failure path.
- Security edge cases.
- Metrics/audit emitted.
