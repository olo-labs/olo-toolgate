# Module 17 — Security, Performance and Chaos Hardening

## Dependencies

Primary feature set complete.

## Objective

Prove the design under attack, load and partial failure.

## Mandatory Preparation

Read the master prompt and requirements traceability document.

Before coding produce the module requirement coverage table.

## Deliverables


- threat-model test mapping;
- fuzzing where useful;
- authorization mutation tests;
- permit replay load;
- SSRF;
- path/shell/archive attacks;
- dependency/container scans;
- Gateway benchmarks;
- fleet scale tests;
- Marketplace queue load;
- chaos tests for DB/Vault/object store/queue/control;
- resource-limit tests;
- audit leakage tests.


## Mandatory Tests


- all dedicated security/performance/chaos suites;
- documented baseline;
- no critical/high unresolved issue for release candidate.


## Deployment / Release


- CI scheduled/nightly jobs;
- benchmark artifact retention.


## Completion

Apply `docs/codex/08-DEFINITION-OF-DONE.md`.

Do not start another module.

## Suggested Commit

```text
test: harden ToolGate security, scale and failure handling
```
