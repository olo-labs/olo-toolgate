# Configuration Import/Export

## Purpose

All non-secret configuration round-trips through versioned JSON/YAML with validate, diff, dry-run and merge/replace. Plain secrets never export.

Module 02 implements formatVersion 1 for users/teams/agents/tools/policies/devices.
Use [API semantics](../api/control-plane-api.md) and the
[valid dry-run example](../examples/control-import.json). Exports have an optimistic
tenant revision; imports require it in both snapshot and If-Match. Unknown versions
are rejected. Credentials, identity-provider accounts, audit history and deployment
state are outside this configuration snapshot.
