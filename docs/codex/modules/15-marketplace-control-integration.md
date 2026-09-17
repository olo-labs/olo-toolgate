# Module 15 — Marketplace ↔ Control Plane Integration

## Dependencies

Modules 02,03,09,10,12,13,14.

## Objective

Complete community import and publication loop.

## Mandatory Preparation

Read the master prompt and requirements traceability document.

Before coding produce the module requirement coverage table.

## Deliverables


- Marketplace account linking;
- registry adapter;
- search;
- signed version import;
- organization scan;
- artifact mirror;
- UNDER_REVIEW;
- configure credentials/policy/devices;
- deploy;
- custom tool sanitization;
- publication transform;
- submission/upload/status;
- advisories/revocation sync;
- webhooks with polling fallback;
- UI.


## Mandatory Tests


- forged signature;
- hash mismatch;
- revoked package;
- Marketplace outage;
- publish sanitization;
- webhook replay;
- import/deploy E2E;
- publish then second-org import E2E.


## Deployment / Release


- registry/Vault/artifact mirror Helm config;
- outbound network policy;
- E2E.


## Completion

Apply `docs/codex/08-DEFINITION-OF-DONE.md`.

Do not start another module.

## Suggested Commit

```text
feat(marketplace): integrate organization import and publishing
```
