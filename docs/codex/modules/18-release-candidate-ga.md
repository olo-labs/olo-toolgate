# Module 18 — Release Candidate / GA

## Dependencies

All modules.

## Objective

Verify every architecture requirement and publish a coherent release.

## Mandatory Preparation

Read the master prompt and requirements traceability document.

Before coding produce the module requirement coverage table.

## Deliverables


- full requirements matrix;
- complete release workflow;
- Maven contract artifact publication;
- container publication;
- endpoint binaries/installers;
- Helm OCI publication;
- GitHub Release;
- checksums;
- SBOM;
- provenance/signing;
- changelog;
- compatibility matrix;
- upgrade guide;
- migration test;
- Quickstart one-minute validation;
- production Helm validation;
- contributor docs;
- security disclosure flow.


## Mandatory Tests


- complete CI;
- fresh install;
- N/N-1 upgrade if version history exists;
- rollback;
- Quickstart;
- production E2E;
- Marketplace E2E;
- client matrix;
- security suite;
- performance gate.


## Deployment / Release


- publish RC first;
- promote stable only after acceptance matrix is green.


## Completion

Apply `docs/codex/08-DEFINITION-OF-DONE.md`.

Do not start another module.

## Suggested Commit

```text
release: prepare OLO ToolGate release candidate
```
