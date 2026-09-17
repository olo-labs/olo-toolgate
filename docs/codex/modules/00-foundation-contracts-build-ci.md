# Module 00 — Foundation, Shared Contracts, Build and CI

## Dependencies

None.

## Objective

Create the stable technical foundation all later modules depend on.

## Mandatory Preparation

Read the master prompt and requirements traceability document.

Before coding produce the module requirement coverage table.

## Deliverables


- finalize Gradle Kotlin DSL multi-project build;
- finalize Rust workspace structure;
- canonical contract directory and versioning;
- initial common/error/identifier/tool/resource/policy/package/client/deployment contract schemas;
- deterministic generated bindings for Java/Rust/TypeScript/PHP where practical;
- Java Maven publication configuration;
- local published-contract dependency proof;
- license/header checking;
- shared test fixtures;
- initial CI workflows;
- dependency/license/secret scan hooks;
- `make check`;
- developer documentation;
- release version plumbing;
- initial Helm chart skeleton with values schema and no fake workloads.


## Mandatory Tests


- schema validation tests;
- serialization round-trip tests;
- compatibility fixtures;
- generated-code drift test;
- Java artifact local publication test;
- service scaffold build against local artifact mode;
- CI smoke.


## Deployment / Release


- Maven publication is configured but remote credentials are CI-only.
- Establish GHCR/OCI naming.
- Establish Helm chart metadata/version strategy.


## Completion

Apply `docs/codex/08-DEFINITION-OF-DONE.md`.

Do not start another module.

## Suggested Commit

```text
feat: establish shared contracts, build system and CI foundation
```
