# Observability and Operations

## Structured Logs

JSON or structured output in production.

Include:

```text
timestamp
level
service
version
request/trace ID
safe entity identifiers
event/action
result
```

Exclude secrets.

## Metrics

Use bounded-cardinality labels.

Never put:

```text
raw path
token
email
arbitrary tool argument
```

into metric labels.

## Traces

Propagate trace context across:

```text
Control
Gateway
Client
Marketplace API
Marketplace Worker
```

where protocol supports it.

## Health

Health means process/core subsystem functioning.

Readiness means safe to receive traffic.

## Async Jobs

Expose durable state:

```text
queued
running
stage
failed
completed
```

not just logs.

## Runbooks

Operational docs should cover:

```text
DB down
Vault down
Control down
Gateway down
Marketplace down
object store down
queue backlog
bad package
compromised key
client rollout failure
```
