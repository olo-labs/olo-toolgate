# ADR 003: tenant-scoped Control Plane and transactional administration

Status: accepted, 2026-10-02. Scope: Module 02.

The Control Plane uses Java 21 and Quarkus. Pure Java domain types validate identifiers
and directory relationships. Application use cases depend on a transactional store
port; JDBC and HTTP are adapters. PostgreSQL stores validated versioned records as
JSONB with indexed tenant/kind/id keys. This preserves shared wire contracts without
introducing an ORM model that duplicates them. Flyway owns schema changes.

Every write takes a PostgreSQL transaction advisory lock for its tenant. Validation,
optimistic revision checks, data writes, audit mutation records and idempotency
responses commit together. This makes reference validation and configuration imports
safe across stateless replicas. A bounded directory makes full graph validation
practical; cursor pagination bounds responses. Read transactions use a consistent
snapshot. Per-tenant serialization is a deliberate administrative throughput tradeoff.

RS256 access tokens must come from an explicitly configured issuer and audience,
with a mounted public verification key. Signed tenant and group claims supply
administrative identity and roles. Directory user records do not provision identity
provider accounts. Administrative credentials never authorize Gateway runtime calls.
Stored policies are configuration records; Module 04 will implement publication.

Imports support JSON and safe YAML data, dry-run, merge and replace. Unknown formats,
fields, invalid references, executable content and revision conflicts are rejected.
Tool schemas remain data and are never executed or resolved through network references.
No user-provided code, script, plugin or template can execute in this service.

PostgreSQL is external to the production chart. Migrations use separate credentials;
the runtime role cannot modify schema or existing audit entries. Startup validation
and private dependency readiness prevent traffic before initialization succeeds.
