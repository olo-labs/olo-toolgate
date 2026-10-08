# Module 05 — ASK and Approval Workflow

## Dependencies

Modules 01–04.

## Objective

Current connection approval is governed by the [device registry](../../control-plane/device-registry.md).
Keep it separate from ASK operation approval. Timed/unlimited connection approval
cannot authorize a tool effect; current device/owner gates, filters and exact-operation
permit consumption still apply. Earlier ASK approval cannot bypass device suspension.

Add human-in-the-loop ASK decisions without weakening runtime protection.

## Mandatory Preparation

Read the master prompt and requirements traceability document.

Before coding produce the module requirement coverage table.

## Deliverables


- approval domain/state machine;
- request creation;
- approve once;
- temporary approval;
- deny;
- expiration;
- approver authorization;
- UI;
- Gateway integration;
- outage => BLOCK;
- audit;
- optional notification port but no mandatory Slack/Teams dependency.


## Mandatory Tests


- state transition tests;
- race/double-decision;
- expiration;
- wrong approver;
- approval service unavailable;
- permit binding after approval;
- E2E ASK flow.


## Deployment / Release


- no new mandatory infra unless ADR;
- Helm config for approval behavior.


## Completion

Apply `docs/codex/08-DEFINITION-OF-DONE.md`.

Do not start another module.

## Suggested Commit

```text
feat(approval): implement ASK workflow
```
