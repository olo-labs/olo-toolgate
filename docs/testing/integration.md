# Integration Tests

## Purpose

OIDC, PostgreSQL, vault adapters, MCP upstream, API-generated tool, Marketplace API/object storage and client runtime adapters.

Run [device registry](../control-plane/device-registry.md) transaction checks on
both PostgreSQL and Quickstart SQLite: audit/idempotency rollback, stable approval
CAS during check-ins, pending metadata, finite/unlimited grants and exact-key
certificate recovery. Composed execution tests must use trusted registered identities
and prove disablement and device filters block protected effects.
