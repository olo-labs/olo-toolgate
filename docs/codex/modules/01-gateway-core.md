# Module 01 — Gateway Core

## Dependencies

Module 00.

## Objective

Implement the high-performance stateless runtime authorization gateway foundation.

## Mandatory Preparation

Read the master prompt and requirements traceability document.

Before coding produce the module requirement coverage table.

## Deliverables


- Rust service;
- config validation;
- health/readiness/metrics;
- structured logging/tracing;
- MCP/runtime ingress skeleton required by architecture;
- normalized request context;
- policy evaluator interface;
- deterministic ALLOW/BLOCK static/test policy source initially;
- resource extractor registry;
- audit event interface;
- rate/size/time limits;
- graceful shutdown;
- production Dockerfile;
- Helm Gateway Deployment/Service/SA/NetworkPolicy/PDB/HPA/probes/resources.


## Mandatory Tests


- policy precedence;
- malformed request;
- limits;
- auth failures;
- extractor failures;
- fail-closed behavior;
- integration HTTP/MCP tests;
- benchmark baseline.


## Deployment / Release


- Build/push image workflow scaffold.
- Helm lint/render tests.
- Kubernetes local smoke path when CI supports cluster.


## Completion

Apply `docs/codex/08-DEFINITION-OF-DONE.md`.

Do not start another module.

## Suggested Commit

```text
feat(gateway): implement stateless gateway core
```
