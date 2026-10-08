# Backup and Restore

## Purpose

Back up PostgreSQL, object storage, encrypted vault metadata and signing/CA material using secure key-specific procedures.

Preserve [device registry](../control-plane/device-registry.md) identities, owners,
approval/deadline and enablement together with their audit/idempotency state. Keep
the Quickstart data volume during redeployment and use its offline backup procedure.
Restore does not grant execution: expiry, current registry gates and tool filters
still apply. Never reset identity or broaden policy scope to make a restored client connect.
