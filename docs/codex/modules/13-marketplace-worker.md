# Module 13 — Marketplace Worker

## Dependencies

Module 12.

## Objective

Implement asynchronous hostile-package processing workflow.

## Mandatory Preparation

Read the master prompt and requirements traceability document.

Before coding produce the module requirement coverage table.

## Deliverables


- durable job lease;
- validate;
- archive safety;
- secret scan adapter;
- malware scan adapter;
- SBOM adapter;
- static scan adapter;
- build sandbox adapter;
- permission diff;
- canonical release;
- signing-service adapter;
- immutable object promotion;
- retry/backoff/failure state;
- metrics;
- Dockerfile;
- Helm worker deployment.


## Mandatory Tests


- duplicate delivery;
- lease expiry;
- crash recovery;
- malicious archive;
- scan failure;
- sandbox failure;
- signing failure;
- object-store failure;
- no publish without signature.


## Deployment / Release


- Worker image;
- Helm deployment/SA/NetworkPolicy/resources/HPA;
- no public service unless metrics need it.


## Completion

Apply `docs/codex/08-DEFINITION-OF-DONE.md`.

Do not start another module.

## Suggested Commit

```text
feat(marketplace): add package processing worker
```
