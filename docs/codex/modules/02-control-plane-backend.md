# Module 02 — Control Plane Backend

## Dependencies

Module 00.

## Objective

Implement the organization management backend with clean domain/application/adapter layering.

Preserve the current [device registry](../../control-plane/device-registry.md):
joined pending/registered views, same-tenant owner binding, finite/unlimited
connection approval, independent enablement and stable approval revisions.
Mutations must commit audit and idempotency atomically on PostgreSQL and SQLite.
Keep reversible HTTP 423 suspension distinct from irreversible key revocation;
exact-key certificate recovery must retain the existing owner and grant.

## Mandatory Preparation

Read the master prompt and requirements traceability document.

Before coding produce the module requirement coverage table.

## Deliverables


- Java/Quarkus application;
- PostgreSQL + Flyway;
- users/teams/agents/tools/policies/device records;
- typed IDs;
- CRUD/use-case APIs;
- OpenAPI;
- audit mutation records;
- config import/export foundation;
- health/readiness/metrics/tracing;
- Dockerfile;
- Helm Control Deployment/Service/SA/NetworkPolicy/PDB/HPA/probes/resources;
- no arbitrary custom-code execution.


## Mandatory Tests


- domain unit tests;
- repository integration tests using PostgreSQL;
- migration clean install;
- API validation/error tests;
- authorization/admin role tests;
- idempotency where applicable.


## Deployment / Release


- Container CI build/scan.
- Helm tests.


## Completion

Apply `docs/codex/08-DEFINITION-OF-DONE.md`.

Do not start another module.

## Suggested Commit

```text
feat(control): implement control plane backend foundation
```
