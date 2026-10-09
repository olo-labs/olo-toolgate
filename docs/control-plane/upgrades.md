# Enterprise cutover and upgrades

Deploy matching Control, Gateway, managed client, console and contract artifacts. The enterprise group contract intentionally removes individual ACL fields and old approval/policy models. Old clients and old configuration writers are rejected; this is a breaking development-version change, not an additive compatibility promise.

PostgreSQL migrations through V16 and SQLite through V9 retain audit/retired IDs, archive obsolete authority, establish group defaults and advance the cutover revision. They never translate missing legacy fields to wildcards. Stop old writers and capture their configuration and decisions before migration. Use explicit group mappings, read-only shadow evaluation and two-reviewer configuration import as described in the [operations runbook](../enterprise-access-control/operations.md).

Runtime credentials cannot change schema or update/delete audit, immutable release or credential-history records. Keep separate migration and runtime database roles. Apply migrations through the database owner; never use Flyway clean as recovery. All active writes, audit and revision/outbox changes commit atomically.

Helm rollback changes application manifests, not database history. Do not downgrade to an old evaluator against the new schema. Correct policy through a newer reviewed configuration. Restore only into an isolated empty destination using external review trust, live monotonic floors and post-backup retirement evidence; quarantine all prior runtime/management authority before reconnecting Core.
