# Module 09 — Package Deployment and Desired/Reported State

## Dependencies

Modules 02,06,08.

## Objective

Implement safe fleet package lifecycle.

Preserve the [device registry controls](../../control-plane/device-registry.md).
Assignments, signed artifacts and READY reports grant no tool access. Check current
registered owner/device and installed-client approval at authenticated boundaries;
execution still needs Gateway filters and permits. Keep approval revision separate
from reported-state revision and preserve identity through temporary suspension.

## Mandatory Preparation

Read the master prompt and requirements traceability document.

Before coding produce the module requirement coverage table.

## Deliverables


- package assignment;
- desired generation/signature;
- client reconciliation;
- artifact grant/download;
- hash/signature verification;
- staging;
- atomic activation;
- health;
- READY/failure states;
- update;
- rollback;
- uninstall;
- canary/staged rollout;
- offline recovery;
- UI status.


## Mandatory Tests


- interrupted download/install;
- hash mismatch;
- wrong assignment;
- stale generation;
- offline client;
- rollback;
- incompatible OS/arch/runtime;
- rollout aggregation;
- E2E deployment.


## Deployment / Release


- artifact store configuration;
- Helm external artifact-store values;
- fleet metrics.


## Completion

Apply `docs/codex/08-DEFINITION-OF-DONE.md`.

Do not start another module.

## Suggested Commit

```text
feat(deployment): add managed package lifecycle
```
