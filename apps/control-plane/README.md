# Control Plane

**Status:** Module 02 runtime

**Primary stack:** Java 21 / Quarkus 3.40.1

## Responsibility

Tenant-scoped organization records, policy configuration, audit and import/export.
Approvals, fleet/package deployment and Marketplace integration are later modules.

## Architecture references

Read:

- `../../ARCHITECTURE.md`
- `../../docs/architecture/component-map.md`
- the matching section under `../../docs/`
- `../../docs/security/security-invariants.md`

Do not move responsibility across component boundaries without an ADR.
## Verification and deployment

Java 21/Quarkus organization administration, PostgreSQL/Flyway, typed directory
identities, CRUD, audit and safe config import/export. No arbitrary tool code executes.

The domain has no framework/database dependencies; application use cases depend on
`Store`/`Codec` ports. HTTP, JWT and JDBC live in adapters. Shared wire models and
schemas come from the contracts project or published Maven JAR in artifact mode.

Run `python tools/control/check.py` for unit, real PostgreSQL and HTTP verification.
Use [configuration](../../docs/control-plane/configuration.md),
[API](../../docs/api/control-plane-api.md),
[deployment](../../docs/control-plane/upgrades.md) and
[ADR 003](../../docs/adr/003-control-plane-transactions.md).

Module 04 adds the framework-independent policy compiler and `BundleSigner` port,
immutable PostgreSQL publication history and signed publish/rollback APIs. The
JCA signing adapter requires an external dedicated key. Run `make policy-e2e`
for production-image integration and read
[bundle protocol and operations](../../docs/control-plane/policy-bundles.md).

Module 05 adds dedicated approver/Gateway approval APIs, PostgreSQL-backed lifecycle
and consumption, and Flyway V4. ASK policies publish signed format 2. No new queue
or cache is required. [Approval configuration and runbook](../../docs/control-plane/approvals.md).

Module 09 uses immutable signed descriptors and monotonic device generations.
See the [fleet usage, upgrade and debug guide](../../docs/client/package-deployment.md) for external trust/store
configuration, health-gated activation, rollback/uninstall and client compatibility.
