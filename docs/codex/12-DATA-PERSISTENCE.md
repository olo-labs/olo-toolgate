# Data and Persistence

## Production

Use external PostgreSQL for authoritative mutable server state.

## Migrations

Use a migration tool appropriate to component stack.

For Java services, Flyway is the baseline unless ADR says otherwise.

## Rules

- no destructive auto-DDL;
- deterministic migrations;
- unique constraints enforce invariants;
- indexes documented by query need;
- transaction boundaries explicit;
- pagination on large collections;
- retention policies defined.

## Test

Every migration series must boot from empty DB.

Upgrade tests should exercise supported previous schema state.

## Async Work

For durable jobs:

```text
job ID
state
attempt
lease owner
lease expiry
next attempt
error code
timestamps
```

should be persisted.

## Outbox

Use transactional outbox when DB mutation and event publication must be atomic.
