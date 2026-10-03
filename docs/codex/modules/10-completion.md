# Module 10 verification report

Status: **IMPLEMENTED FOR THE LINUX PATH, NOT DONE**, 2026-10-04.
Native Windows/macOS system OCI engine
execution across logout/boot and ARM execution remain uncertified. Historical
fleet-enabled cluster and native release limitations remain in Module 09's report.
No Module 11 or remote publication is started.

Preparation preceded implementation: [coverage](10-coverage.md), master prompt,
traceability, governing documents and [definition of done](../08-DEFINITION-OF-DONE.md).
See [ADR 011](../../adr/011-designated-client-tool-authoring.md), the
[production/debug guide](../../client/tool-builder.md) and the
[example](../../../examples/tool-builder-python/README.md).

## Delivered behavior

The embedded console authors code/runtime/input-output schemas/AI guidance/
examples/permissions/resource mappings/credential references. Control validates
bounded closed schemas and detects known secret patterns without executing
source. PostgreSQL V7 persists revisions, leases, results and immutable sealed
versions in tenant transactions with audit/idempotency. Every current example
must pass on a designated enrolled client before sealing; tests never assign
ordinary executable capability. Credential binding and non-compute permissions
are unsupported and fail closed.

Organization-signed mTLS leases bind tenant/server/device/draft digest/expiry.
The protected client confines Python/Node/PowerShell/Shell source in its existing
OCI engine using fixed stdin runners, source integrity, cleared environment,
no network/host mounts/children and resource limits. Native/Java/.NET use reviewed
image artifacts. Timeout cancellation shuts down sandbox state. Results export
only typed statuses/errors. Ordinary source invocation binds Gateway/ASK scope
to a registration digest as well as image/resource/input; test success grants
no runtime authorization. Windows/macOS/ARM runtime certification remains open.

Sealing reserves an immutable package version. An independent external release
authority reviews/signs exact descriptor bytes outside Control. Publish requires
equality with the sealed candidate and existing independent release trust.
Deployment uses existing signed fleet reconciliation/canaries. Publication
preparation exports author metadata without tenant/device/job internals, and
does not publish to a marketplace. No new infrastructure/Helm configuration.
Contracts/product/chart versions advance to 0.10.0-dev; 143 generated models in
18 groups retain old optional-source-free fixture compatibility.

## Definition of done assessment

Implemented Linux primary path uses shared contracts and preserves Control/client/
Gateway boundaries without production mocks, host execution fallback or an
authorization bypass. Existing non-root image, probes, resources, network policy,
HA database transactions, health/logs/traces and bounded builder metrics apply.
Production/debug/upgrade documentation, offline signer and example accompany
native packaging; protected CI includes real builder E2E and inline runtime tests.

An applicable blocked gate prevents declaring DONE. Native system-engine/service
logout/boot and ARM tests require their real platforms; cross-builds and Linux
containers do not certify them. Existing generic chart smoke cannot establish
fleet-enabled cluster mTLS/artifact routing. Remote signing/publication/attestation
remain protected CI actions and were not performed locally.

## Executed gates

| Command / gate | Result |
|---|---|
| `tools/check.py` (`make check` runner; `TOOLGATE_DOCKER_TOOLS=1`) | PASS: 53 contract tests, drift/version/header gates; all language round trips; 28 PostgreSQL Control tests; local Maven artifact-mode builds and missing-artifact rejection; workspace tests/clippy; 49 UI tests with required coverage; five real general browser tests; chart lint/render/schema/security negatives. Feature-specific browser specs run in separate harnesses. |
| `tools/control/check.py` | PASS: clean/incremental PostgreSQL migrations, author revision race and sealed/version database guards; signed JWT/roles/isolation/limits/audit/redacted telemetry HTTP checks. |
| `tools/deployment/check.py --builder --control-image olo-toolgate-control:module10 --binary target/client-release/x86_64-unknown-linux-gnu/release/olo-toolgate-client` | PASS: actual TLS/mTLS authoring; invalid secret/schema/permission/role/client rejection; signed designated execution; tests grant no capability; seal/release/deploy; immutable assigned version; Gateway invocation; real browser authoring/accessibility; Control outage/recovery, Gateway outage and revocation. |
| `tools/client/runtime_check.py --test-binary <compiled Windows execution test> --native-fixture <real Linux runtime_fixture>` | PASS: seven real OCI adapters and four inline runners; command/JSON framing/injection/isolation/environment/children/network/time/output/memory and registration-digest binding. Windows host driving Linux OCI does not certify native logout/boot. |
| Linux and Windows `builder_source` tests | PASS: source integrity/budgets/framing and genuine lease rejection for wrong authority/device/server/expiry/permission/credential/state/resource/tamper. |
| Final client `cargo test` and `cargo clippy --all-targets -- -D warnings` | PASS after correcting terminating-newline accounting; maximum source and caller-input transport budgets have an explicit regression test. |
| `cargo build --release --target x86_64-unknown-linux-gnu`; `cargo zigbuild --release --target x86_64-pc-windows-gnu --target x86_64-apple-darwin` | PASS: three real x64 binaries; cross-builds do not certify native service/runtime behavior. |
| `tools/check.py --scans` | PASS: actionlint, npm/pip audits, resolved dependency licenses, Gitleaks/public-JWS negative self-tests and working-source vulnerability/license scan. |
| Production Docker build; `tools/control/container.py --no-build --image olo-toolgate-control:module10`; Trivy HIGH/CRITICAL image scan; image SBOM plus `tools/ui/merge_sbom.py` | PASS: actual PostgreSQL, non-root/read-only image, authenticated APIs and graceful shutdown; clean image scan and embedded-UI SBOM. |
| `tools/release/bundle.py`; `git diff --check` | PASS: canonical raw contract bundle/checksums and clean patch. |
| `tools/client/package.py` for three x64 targets; `tools/client/manifest.py`; `tools/client/downloads.py --image olo-toolgate-control:module10-downloads` | PASS: actual ELF/PE/Mach-O archives, checksums/SBOM/notices/docs and anonymous browser downloads; protected APIs remain authenticated. Native platform certification remains separate. |

Local generated evidence: `build/client/module10-integration.json`,
`build/client/module09-integration.json`, `build/client/runtime-smoke.json`,
`build/control/http-smoke.json`, `build/control/container-smoke.json`,
`build/control/control-sbom.cdx.json`, language test results and Helm renders.
Private harness credentials/logs and generated artifacts are ignored, not committed.
No remote CI execution/publication, host service installation or native macOS/ARM
execution is claimed.

## Individual requirement matrix

Statuses describe Module 10 scope and retain inherited limitations.

| ID | Status | Evidence / scope reason |
|---|---|---|
| ARC-001 | COMPLETE | ADR 011; BuilderService/BuilderStore and existing fleet/endpoint/runtime ports; source remains data in Control, versions/jobs are durable. |
| ARC-002 | COMPLETE | ADR 011; BuilderService/BuilderStore and existing fleet/endpoint/runtime ports; source remains data in Control, versions/jobs are durable. |
| ARC-003 | COMPLETE | ADR 011; BuilderService/BuilderStore and existing fleet/endpoint/runtime ports; source remains data in Control, versions/jobs are durable. |
| ARC-004 | COMPLETE | ADR 011; BuilderService/BuilderStore and existing fleet/endpoint/runtime ports; source remains data in Control, versions/jobs are durable. |
| ARC-005 | COMPLETE | ADR 011; BuilderService/BuilderStore and existing fleet/endpoint/runtime ports; source remains data in Control, versions/jobs are durable. |
| ARC-006 | COMPLETE | ADR 011; BuilderService/BuilderStore and existing fleet/endpoint/runtime ports; source remains data in Control, versions/jobs are durable. |
| ARC-007 | COMPLETE | ADR 011; BuilderService/BuilderStore and existing fleet/endpoint/runtime ports; source remains data in Control, versions/jobs are durable. |
| CON-001 | COMPLETE | 18 canonical groups/143 generated models; drift/round trips/genuine signed compatibility and local Maven artifact/service proof. |
| CON-002 | COMPLETE | 18 canonical groups/143 generated models; drift/round trips/genuine signed compatibility and local Maven artifact/service proof. |
| CON-003 | COMPLETE | 18 canonical groups/143 generated models; drift/round trips/genuine signed compatibility and local Maven artifact/service proof. |
| CON-004 | COMPLETE | 18 canonical groups/143 generated models; drift/round trips/genuine signed compatibility and local Maven artifact/service proof. |
| CON-005 | COMPLETE | 18 canonical groups/143 generated models; drift/round trips/genuine signed compatibility and local Maven artifact/service proof. |
| CON-006 | COMPLETE | 18 canonical groups/143 generated models; drift/round trips/genuine signed compatibility and local Maven artifact/service proof. |
| SEC-001 | COMPLETE | Bounded schemas/source integrity/secret scan; direct mTLS signed leases; independent release, fresh registration-bound Gateway/ASK, audit/replay and real negatives. |
| SEC-002 | COMPLETE | Bounded schemas/source integrity/secret scan; direct mTLS signed leases; independent release, fresh registration-bound Gateway/ASK, audit/replay and real negatives. |
| SEC-003 | COMPLETE | Bounded schemas/source integrity/secret scan; direct mTLS signed leases; independent release, fresh registration-bound Gateway/ASK, audit/replay and real negatives. |
| SEC-004 | COMPLETE | Bounded schemas/source integrity/secret scan; direct mTLS signed leases; independent release, fresh registration-bound Gateway/ASK, audit/replay and real negatives. |
| SEC-005 | COMPLETE | Bounded schemas/source integrity/secret scan; direct mTLS signed leases; independent release, fresh registration-bound Gateway/ASK, audit/replay and real negatives. |
| SEC-006 | BLOCKED | Linux OCI isolation passes; native macOS/Windows system engine across logout/boot and ARM execution remain uncertified. |
| SEC-007 | COMPLETE | Bounded schemas/source integrity/secret scan; direct mTLS signed leases; independent release, fresh registration-bound Gateway/ASK, audit/replay and real negatives. |
| SEC-008 | COMPLETE | Bounded schemas/source integrity/secret scan; direct mTLS signed leases; independent release, fresh registration-bound Gateway/ASK, audit/replay and real negatives. |
| SEC-009 | COMPLETE | Bounded schemas/source integrity/secret scan; direct mTLS signed leases; independent release, fresh registration-bound Gateway/ASK, audit/replay and real negatives. |
| SEC-010 | COMPLETE | Bounded schemas/source integrity/secret scan; direct mTLS signed leases; independent release, fresh registration-bound Gateway/ASK, audit/replay and real negatives. |
| TST-001 | COMPLETE | Executed contract/unit/PostgreSQL/security/real OCI/TLS/client/browser and outage/revocation gates above. |
| TST-002 | COMPLETE | Executed contract/unit/PostgreSQL/security/real OCI/TLS/client/browser and outage/revocation gates above. |
| TST-003 | BLOCKED | Real Linux TLS/OCI integration passes; native system-service/runtime and ARM boundary integration remain unexecuted. |
| TST-004 | COMPLETE | Executed contract/unit/PostgreSQL/security/real OCI/TLS/client/browser and outage/revocation gates above. |
| TST-005 | COMPLETE | Executed contract/unit/PostgreSQL/security/real OCI/TLS/client/browser and outage/revocation gates above. |
| TST-006 | BLOCKED | Actual author/client/deploy/browser/outage E2E passes; native logout/boot/ARM execution E2E remains unexecuted. |
| TST-007 | NOT_APPLICABLE | Authoring/testing is outside the Gateway hot path; no new throughput claim or performance-sensitive path. |
| TST-008 | COMPLETE | Executed contract/unit/PostgreSQL/security/real OCI/TLS/client/browser and outage/revocation gates above. |
| QLT-001 | COMPLETE | Header/license/source scans, documented ports, typed errors, shared models and clippy/type checks; no new dependency. |
| QLT-002 | COMPLETE | Header/license/source scans, documented ports, typed errors, shared models and clippy/type checks; no new dependency. |
| QLT-003 | COMPLETE | Header/license/source scans, documented ports, typed errors, shared models and clippy/type checks; no new dependency. |
| QLT-004 | COMPLETE | Header/license/source scans, documented ports, typed errors, shared models and clippy/type checks; no new dependency. |
| QLT-005 | COMPLETE | Header/license/source scans, documented ports, typed errors, shared models and clippy/type checks; no new dependency. |
| QLT-006 | COMPLETE | Header/license/source scans, documented ports, typed errors, shared models and clippy/type checks; no new dependency. |
| API-001 | COMPLETE | Ten versioned canonical builder routes; bounded validation, TLS/role or certificate auth, stable errors/correlation, revision/idempotency and redacted results. |
| API-002 | COMPLETE | Ten versioned canonical builder routes; bounded validation, TLS/role or certificate auth, stable errors/correlation, revision/idempotency and redacted results. |
| API-003 | COMPLETE | Ten versioned canonical builder routes; bounded validation, TLS/role or certificate auth, stable errors/correlation, revision/idempotency and redacted results. |
| API-004 | COMPLETE | Ten versioned canonical builder routes; bounded validation, TLS/role or certificate auth, stable errors/correlation, revision/idempotency and redacted results. |
| API-005 | COMPLETE | Ten versioned canonical builder routes; bounded validation, TLS/role or certificate auth, stable errors/correlation, revision/idempotency and redacted results. |
| API-006 | COMPLETE | Ten versioned canonical builder routes; bounded validation, TLS/role or certificate auth, stable errors/correlation, revision/idempotency and redacted results. |
| API-007 | COMPLETE | Ten versioned canonical builder routes; bounded validation, TLS/role or certificate auth, stable errors/correlation, revision/idempotency and redacted results. |
| DAT-001 | COMPLETE | Flyway V7 clean/upgrade; tenant transaction/revision race, audit/replay, sealed/version triggers and external durable PostgreSQL jobs. |
| DAT-002 | COMPLETE | Flyway V7 clean/upgrade; tenant transaction/revision race, audit/replay, sealed/version triggers and external durable PostgreSQL jobs. |
| DAT-003 | COMPLETE | Flyway V7 clean/upgrade; tenant transaction/revision race, audit/replay, sealed/version triggers and external durable PostgreSQL jobs. |
| DAT-004 | COMPLETE | Flyway V7 clean/upgrade; tenant transaction/revision race, audit/replay, sealed/version triggers and external durable PostgreSQL jobs. |
| DAT-005 | COMPLETE | Flyway V7 clean/upgrade; tenant transaction/revision race, audit/replay, sealed/version triggers and external durable PostgreSQL jobs. |
| OBS-001 | COMPLETE | Existing readiness/health/redacted structured logs/OTLP; bounded builder metrics and durable QUEUED/RUNNING/PASSED/FAILED/EXPIRED states. |
| OBS-002 | COMPLETE | Existing readiness/health/redacted structured logs/OTLP; bounded builder metrics and durable QUEUED/RUNNING/PASSED/FAILED/EXPIRED states. |
| OBS-003 | COMPLETE | Existing readiness/health/redacted structured logs/OTLP; bounded builder metrics and durable QUEUED/RUNNING/PASSED/FAILED/EXPIRED states. |
| OBS-004 | COMPLETE | Existing readiness/health/redacted structured logs/OTLP; bounded builder metrics and durable QUEUED/RUNNING/PASSED/FAILED/EXPIRED states. |
| OBS-005 | COMPLETE | Existing readiness/health/redacted structured logs/OTLP; bounded builder metrics and durable QUEUED/RUNNING/PASSED/FAILED/EXPIRED states. |
| OBS-006 | COMPLETE | Existing readiness/health/redacted structured logs/OTLP; bounded builder metrics and durable QUEUED/RUNNING/PASSED/FAILED/EXPIRED states. |
| OBS-007 | COMPLETE | Existing readiness/health/redacted structured logs/OTLP; bounded builder metrics and durable QUEUED/RUNNING/PASSED/FAILED/EXPIRED states. |
| DKR-001 | COMPLETE | Production multi-stage non-root/read-only image rebuilt/smoked; shutdown, clean scan, combined SBOM and protected publication retained. |
| DKR-002 | COMPLETE | Production multi-stage non-root/read-only image rebuilt/smoked; shutdown, clean scan, combined SBOM and protected publication retained. |
| DKR-003 | COMPLETE | Production multi-stage non-root/read-only image rebuilt/smoked; shutdown, clean scan, combined SBOM and protected publication retained. |
| DKR-004 | COMPLETE | Production multi-stage non-root/read-only image rebuilt/smoked; shutdown, clean scan, combined SBOM and protected publication retained. |
| DKR-005 | COMPLETE | Production multi-stage non-root/read-only image rebuilt/smoked; shutdown, clean scan, combined SBOM and protected publication retained. |
| DKR-006 | COMPLETE | Production multi-stage non-root/read-only image rebuilt/smoked; shutdown, clean scan, combined SBOM and protected publication retained. |
| DKR-007 | COMPLETE | Production multi-stage non-root/read-only image rebuilt/smoked; shutdown, clean scan, combined SBOM and protected publication retained. |
| DKR-008 | COMPLETE | Production multi-stage non-root/read-only image rebuilt/smoked; shutdown, clean scan, combined SBOM and protected publication retained. |
| K8S-001 | NOT_APPLICABLE | No new chart/resource/configuration; existing deployment reused. Historical Module 09 fleet-enabled cluster certification blocker remains, not erased. |
| K8S-002 | NOT_APPLICABLE | No new chart/resource/configuration; existing deployment reused. Historical Module 09 fleet-enabled cluster certification blocker remains, not erased. |
| K8S-003 | NOT_APPLICABLE | No new chart/resource/configuration; existing deployment reused. Historical Module 09 fleet-enabled cluster certification blocker remains, not erased. |
| K8S-004 | NOT_APPLICABLE | No new chart/resource/configuration; existing deployment reused. Historical Module 09 fleet-enabled cluster certification blocker remains, not erased. |
| K8S-005 | NOT_APPLICABLE | No new chart/resource/configuration; existing deployment reused. Historical Module 09 fleet-enabled cluster certification blocker remains, not erased. |
| K8S-006 | NOT_APPLICABLE | No new chart/resource/configuration; existing deployment reused. Historical Module 09 fleet-enabled cluster certification blocker remains, not erased. |
| K8S-007 | NOT_APPLICABLE | No new chart/resource/configuration; existing deployment reused. Historical Module 09 fleet-enabled cluster certification blocker remains, not erased. |
| K8S-008 | NOT_APPLICABLE | No new chart/resource/configuration; existing deployment reused. Historical Module 09 fleet-enabled cluster certification blocker remains, not erased. |
| K8S-009 | NOT_APPLICABLE | No new chart/resource/configuration; existing deployment reused. Historical Module 09 fleet-enabled cluster certification blocker remains, not erased. |
| K8S-010 | NOT_APPLICABLE | No new chart/resource/configuration; existing deployment reused. Historical Module 09 fleet-enabled cluster certification blocker remains, not erased. |
| K8S-011 | NOT_APPLICABLE | No new chart/resource/configuration; existing deployment reused. Historical Module 09 fleet-enabled cluster certification blocker remains, not erased. |
| K8S-012 | NOT_APPLICABLE | No new chart/resource/configuration; existing deployment reused. Historical Module 09 fleet-enabled cluster certification blocker remains, not erased. |
| K8S-013 | COMPLETE | Existing chart lint/render/schema/security-negative checks pass; no new authoring workloads, values or Secrets. |
| K8S-014 | COMPLETE | Existing chart lint/render/schema/security-negative checks pass; no new authoring workloads, values or Secrets. |
| K8S-015 | NOT_APPLICABLE | No new chart/resource/configuration; existing deployment reused. Historical Module 09 fleet-enabled cluster certification blocker remains, not erased. |
| K8S-016 | NOT_APPLICABLE | No new chart/resource/configuration; existing deployment reused. Historical Module 09 fleet-enabled cluster certification blocker remains, not erased. |
| REL-001 | COMPLETE | 0.10.0-dev synchronization; Maven proof, signed compatibility, three x64 builds, release bundle/SBOM and protected signing/publication/attestation retained. |
| REL-002 | COMPLETE | 0.10.0-dev synchronization; Maven proof, signed compatibility, three x64 builds, release bundle/SBOM and protected signing/publication/attestation retained. |
| REL-003 | COMPLETE | 0.10.0-dev synchronization; Maven proof, signed compatibility, three x64 builds, release bundle/SBOM and protected signing/publication/attestation retained. |
| REL-004 | COMPLETE | 0.10.0-dev synchronization; Maven proof, signed compatibility, three x64 builds, release bundle/SBOM and protected signing/publication/attestation retained. |
| REL-005 | COMPLETE | 0.10.0-dev synchronization; Maven proof, signed compatibility, three x64 builds, release bundle/SBOM and protected signing/publication/attestation retained. |
| REL-006 | COMPLETE | 0.10.0-dev synchronization; Maven proof, signed compatibility, three x64 builds, release bundle/SBOM and protected signing/publication/attestation retained. |
| REL-007 | COMPLETE | 0.10.0-dev synchronization; Maven proof, signed compatibility, three x64 builds, release bundle/SBOM and protected signing/publication/attestation retained. |
| REL-008 | COMPLETE | 0.10.0-dev synchronization; Maven proof, signed compatibility, three x64 builds, release bundle/SBOM and protected signing/publication/attestation retained. |
| REL-009 | COMPLETE | 0.10.0-dev synchronization; Maven proof, signed compatibility, three x64 builds, release bundle/SBOM and protected signing/publication/attestation retained. |
| REL-010 | BLOCKED | Compatibility limits documented; native system-engine/service and ARM certification needed before an unrestricted platform readiness claim. |
| REL-011 | COMPLETE | 0.10.0-dev synchronization; Maven proof, signed compatibility, three x64 builds, release bundle/SBOM and protected signing/publication/attestation retained. |
| DOC-001 | COMPLETE | READMEs, ADR 011, canonical OpenAPI, compatibility/changelog/upgrade notes, example and production/debug configuration/execution/troubleshooting guide. |
| DOC-002 | COMPLETE | READMEs, ADR 011, canonical OpenAPI, compatibility/changelog/upgrade notes, example and production/debug configuration/execution/troubleshooting guide. |
| DOC-003 | COMPLETE | READMEs, ADR 011, canonical OpenAPI, compatibility/changelog/upgrade notes, example and production/debug configuration/execution/troubleshooting guide. |
| DOC-004 | COMPLETE | READMEs, ADR 011, canonical OpenAPI, compatibility/changelog/upgrade notes, example and production/debug configuration/execution/troubleshooting guide. |
| DOC-005 | COMPLETE | READMEs, ADR 011, canonical OpenAPI, compatibility/changelog/upgrade notes, example and production/debug configuration/execution/troubleshooting guide. |
| DOC-006 | COMPLETE | READMEs, ADR 011, canonical OpenAPI, compatibility/changelog/upgrade notes, example and production/debug configuration/execution/troubleshooting guide. |
| DOC-007 | COMPLETE | READMEs, ADR 011, canonical OpenAPI, compatibility/changelog/upgrade notes, example and production/debug configuration/execution/troubleshooting guide. |
| PRF-001 | COMPLETE | No builder work in Gateway; drafts/jobs/source/schema/body/examples/pages/leases are capped and execution stays in confined endpoints. |
| PRF-002 | COMPLETE | No builder work in Gateway; drafts/jobs/source/schema/body/examples/pages/leases are capped and execution stays in confined endpoints. |
| PRF-003 | COMPLETE | No builder work in Gateway; drafts/jobs/source/schema/body/examples/pages/leases are capped and execution stays in confined endpoints. |
| PRF-004 | COMPLETE | No builder work in Gateway; drafts/jobs/source/schema/body/examples/pages/leases are capped and execution stays in confined endpoints. |
| PRF-005 | NOT_APPLICABLE | Authoring/testing is outside the Gateway hot path; no new throughput claim or performance-sensitive path. |
| PRF-006 | COMPLETE | No builder work in Gateway; drafts/jobs/source/schema/body/examples/pages/leases are capped and execution stays in confined endpoints. |

104 IDs: 84 COMPLETE, 4 BLOCKED, 16 NOT_APPLICABLE.

Suggested commit: `feat(tools): add custom local tool builder`.
