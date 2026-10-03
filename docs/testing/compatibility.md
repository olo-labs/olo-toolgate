# Compatibility Tests

## Purpose

N/N-1 product compatibility, package schema migrations, Marketplace API clients and client/server version windows.

Module 09 uses immutable signed descriptors and monotonic device generations.
See the [fleet usage, upgrade and debug guide](../client/package-deployment.md) for external trust/store
configuration, health-gated activation, rollback/uninstall and client compatibility.

Module 10 adds optional signed inline source and designated-client leases.
Author packages require 0.10 readers; old descriptors without source retain their
existing wire format. Public genuine lease vectors verify across Python/Rust,
and all generated languages round-trip the new builder models. See
[authoring compatibility and execution](../client/tool-builder.md).
