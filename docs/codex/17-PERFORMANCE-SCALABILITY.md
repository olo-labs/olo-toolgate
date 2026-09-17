# Performance and Scalability

## Gateway

Critical path.

Goals include:

```text
in-memory policy evaluation
no DB query in normal authorization path
bounded allocation
fast startup
horizontal scale
```

Benchmark before publishing claims.

## Control Plane

Optimize correctness/operability before micro-latency.

Use pagination, indexes and async jobs.

## Client Fleet

Batch heartbeats/reported state.

Avoid per-device permanent server memory dependence.

## Marketplace

Large uploads go direct to object storage.

Scans/builds are asynchronous.

Worker/API scale independently.

## Backpressure

All queue/upload/job systems need:

```text
size limits
concurrency limits
timeouts
retry limits
dead-letter/failure state
```
