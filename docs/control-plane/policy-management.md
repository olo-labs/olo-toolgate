# Policy Management

## Purpose

Draft, simulate, review diff, publish immutable version, rollback. Explicit BLOCK outranks ALLOW. Policy impact should highlight newly reachable sensitive resources.

Device filters match the trusted [registered execution identity](device-registry.md).
Installed-client approval and enablement are independent prerequisites, not policy
ALLOW grants. Quickstart HotFolder uses `local-hotfolder`; existing policies scoped
only to `local-builtins` do not automatically expand to it. Review the intended
device scope and publish the updated bundle. REST forwarding retains target-client
filters in addition to its own registered path's enablement.
