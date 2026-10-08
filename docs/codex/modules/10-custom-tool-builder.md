# Module 10 — Custom Local Tool Builder

## Dependencies

Modules 03,08,09.

## Objective

Allow administrators/tool authors to create, test, version, deploy and later publish tools.

Designated execution must use the current [registered device flow](../../control-plane/device-registry.md).
Authoring or test success cannot approve a device, extend its connection deadline,
reenable it or bypass tool/resource filters. Keep tenant/owner binding and current
registry checks before test and runtime effects, alongside existing signed leases,
permission narrowing and sandbox confinement.

## Mandatory Preparation

Read the master prompt and requirements traceability document.

Before coding produce the module requirement coverage table.

## Deliverables


- code editor;
- runtime selection;
- inputs/outputs;
- AI description/useWhen/doNotUseWhen/examples;
- permissions;
- resource mapping;
- credential requirements;
- versioning;
- secret scan;
- test on designated client/sandbox, never Control JVM;
- create organization package;
- deploy action;
- publication preparation action.


## Mandatory Tests


- schema validation;
- secret detection;
- invalid permission;
- test sandbox routing;
- version immutability after assignment;
- UI tests/E2E.


## Deployment / Release


- no direct Helm changes unless new config;
- docs/examples.


## Completion

Apply `docs/codex/08-DEFINITION-OF-DONE.md`.

Do not start another module.

## Suggested Commit

```text
feat(tools): add custom local tool builder
```
