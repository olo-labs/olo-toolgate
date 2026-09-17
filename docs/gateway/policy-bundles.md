# Signed Policy Bundles

## Purpose

Control Plane compiles immutable versioned bundles. Gateway verifies signature/hash/version/expiry and atomically swaps only valid bundles. Last-known-good remains active according to configured expiry/grace rules.

## Required Tests

- Success path.
- Failure path.
- Security edge cases.
- Metrics/audit emitted.
