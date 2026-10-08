# Upgrades

## Purpose

Rolling Gateway/Control upgrades, schema migrations, client staged rollout and compatibility-window enforcement.

The current [device registry](../control-plane/device-registry.md) requires matching
Control/UI/contracts and PostgreSQL migration V11. Keep the retained data volume;
redeployment preserves identities, owners, approvals and enablement and seeds only
missing Quickstart devices. Review policies explicitly scoped to `local-builtins`
before HotFolder starts authorizing as `local-hotfolder`. Current clients support
exact-key certificate recovery after an extended outage; approval remains unchanged.

Module 09 uses immutable signed descriptors and monotonic device generations.
See the [fleet usage, upgrade and debug guide](../client/package-deployment.md) for external trust/store
configuration, health-gated activation, rollback/uninstall and client compatibility.
