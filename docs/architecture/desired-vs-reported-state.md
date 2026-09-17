# Desired vs Reported State

Do not implement fleet software deployment as one-shot "push this file" RPCs.

## Desired State

Stored by Control Plane:

```text
Device A:
  community/pdf-tools -> 1.4.0
  old-tool -> absent
```

## Reported State

Returned by client:

```text
community/pdf-tools 1.3.0 READY
old-tool 2.0.0 READY
```

## Reconciliation

The client repeatedly converges reported state toward desired state.

This handles:

- offline devices.
- retries.
- interrupted installs.
- rollbacks.
- drift.
- staged deployments.
