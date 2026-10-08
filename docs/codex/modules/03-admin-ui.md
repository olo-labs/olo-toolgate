# Module 03 — Admin UI Foundation

## Dependencies

Module 02.

## Objective

Implement the simple-first management UI.

Maintain the current [device registry UI](../../control-plane/device-registry.md).
Show pending and registered devices together with device-name and registered-user
columns, accessible details, connection state, approval and expiry. Provide
independent enable/disable and approve/deapprove controls, with explicit timed or
unlimited approval. Server-managed devices show real readiness and have no client
approval form. Keep permission and revision checks on the server and test API failures.

## Mandatory Preparation

Read the master prompt and requirements traceability document.

Before coding produce the module requirement coverage table.

## Deliverables


- React/TypeScript/Vite application;
- typed API client generated/wrapped from OpenAPI;
- authentication shell;
- dashboard;
- users/teams/tools/policies/client navigation;
- accessible components;
- loading/error/empty states;
- no business/security logic duplicated from backend;
- Docker/static serving strategy;
- integration into control/quickstart packaging plan.


## Mandatory Tests


- component/unit tests;
- accessibility tests;
- API error-state tests;
- browser smoke/E2E.


## Deployment / Release


- build artifact production;
- container or embedding strategy documented;
- Helm routing/ingress path if separate service.


## Completion

Apply `docs/codex/08-DEFINITION-OF-DONE.md`.

Do not start another module.

## Suggested Commit

```text
feat(ui): add administration console foundation
```
