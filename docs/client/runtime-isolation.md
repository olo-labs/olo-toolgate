# Runtime Isolation

## Purpose

Filesystem/network/process/memory/time restrictions are derived from package declaration and organization policy.

The [device registry gates](../control-plane/device-registry.md) run before
protected execution alongside Gateway filters and permits. Timed/unlimited
connection approval never relaxes filesystem, network or process confinement;
deployment and health reports cannot substitute for current execution permission.

Module 08 enforces an immutable-image, non-root, no-network, no-host-mount OCI
boundary with bounded tmpfs, resources, output and process creation. See
[actual limits and recovery](local-runtimes.md#limits-health-and-recovery).
