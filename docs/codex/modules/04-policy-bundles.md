# Module 04 — Signed Policy Bundles

## Dependencies

Modules 01 and 02.

## Objective

Implement versioned signed bundle compilation and fail-safe Gateway distribution.

## Mandatory Preparation

Read the master prompt and requirements traceability document.

Before coding produce the module requirement coverage table.

## Deliverables


- canonical bundle contract;
- compiler;
- immutable bundle version;
- hash/signature;
- key ID;
- publish API;
- Gateway verification;
- atomic hot swap;
- last-known-good;
- expiry/grace behavior;
- rollback;
- audit;
- key rotation compatibility.


## Mandatory Tests


- invalid signature;
- stale version;
- expired bundle;
- atomic replacement;
- rollback;
- Control unavailable;
- malformed bundle;
- compatibility.


## Deployment / Release


- signing secret/key referenced externally;
- Helm values/Secret references;
- release compatibility tests.


## Completion

Apply `docs/codex/08-DEFINITION-OF-DONE.md`.

Do not start another module.

## Suggested Commit

```text
feat(policy): add signed policy bundle distribution
```
