# Runtime Isolation

## Purpose

Filesystem/network/process/memory/time restrictions are derived from package declaration and organization policy.

Module 08 enforces an immutable-image, non-root, no-network, no-host-mount OCI
boundary with bounded tmpfs, resources, output and process creation. See
[actual limits and recovery](local-runtimes.md#limits-health-and-recovery).
