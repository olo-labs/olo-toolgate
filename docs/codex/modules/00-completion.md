# Module 00 completion report

Module 00 establishes the shared technical foundation only. No later module was
started. Prepared against the master prompt and definition of done, 2026-10-02.

## Result and implementation

Nine canonical v1 schema groups cover 26 definitions (22 generated model/enum
types plus four scalar schema aliases). Fifty-one deterministic generated output
files include Java records, Rust Serde models, TypeScript readonly types, PHP
readonly DTOs/enums, license copies and a generation manifest. Java artifacts
embed canonical schemas and Apache license text.

Gradle Kotlin DSL aggregates build/check tasks under Java 21, with centralized
versions, separate workspace/artifact dependency locks, stable Maven coordinates
and CI-only remote credentials. Cargo provides a stable pinned workspace with a
committed lock. The Windows Gradle bootstrap was repaired while retaining SHA-256
verification; Unix launchers/scripts have executable Git modes.

The local Maven proof inspects real published classifiers/POM and schema model
classes, forces the three service scaffolds to resolve the artifact exclusively,
verifies their loaded contract code came from a JAR and rejects an empty registry.
Imports do not change between dependency modes.

CI consolidates foundation gates and read-only scans. Release configuration builds
contract/chart assets, checksums, dependency inventory and CycloneDX SBOM and uses
GitHub provenance attestations and protected environment publication. Helm is
metadata-only, validates values and emits no fake workloads. GHCR names and
product/contract/chart version sources are documented.

## Design and security

ADR 001 adopts canonical schemas and deterministic structural bindings. Generated
models contain no service logic, hidden network client, repository or authorization
engine. Validation remains mandatory at untrusted boundaries; TypeScript types
provide no runtime validation. Security object schemas reject unknown/missing
fields and unknown enum semantics. ALLOW is never a default. Marketplace,
organization and runtime models are distinct. Errors expose no exception detail,
and packages carry secret references instead of values. No signature or digest
verification is faked; the fixture signatures are plainly non-signatures.

Jackson was upgraded to 2.21.7 after the vulnerability gate found advisories in
the initially selected version. No advisory or secret finding is suppressed.
License inventory uses actual Cargo/npm/Python/Maven metadata; license expressions
respect AND/OR and reject unknown/restricted choices. MPL/EPL/Python licenses are
permitted only for tooling; Unicode-3.0 is accepted for Rust identifier data.
JDK-generated Javadoc assets retain their classpath-exception notices separately
from runtime code. Source scans exclude dependency caches and build outputs.

## Test and command evidence

Executed with Java 21.0.11, Gradle 9.7.1, Node 24.18.0 (CI selects Node 22), Python 3.12,
Rust 1.94.1, PHP 8.2, Helm 3.17.3 and GNU Make 4.4.1. Rust/PHP/Helm used isolated
Docker toolchains on Windows with TOOLGATE_DOCKER_TOOLS=1. Equivalent commands
below omit the long local bundled Python executable path.

| Command | Result |
|---|---|
| make check PYTHON=<Python 3.12 executable> | PASS: mandatory gate completed, no skipped tools |
| python -m unittest discover -s tests/contracts -v | PASS: 17 tests, including schema/negative/trust/compatibility/drift/license/version/CI/docs/bundle checks |
| python tools/contracts/generate.py --check | PASS: 51 generated outputs current |
| python tools/contracts/version.py --check | PASS: synchronized metadata |
| gradlew.bat projects javaCheck build --no-daemon | PASS: four Java projects, two contract tests and one test per service |
| gradlew.bat javaCheck --write-locks --no-daemon | PASS: reviewed dependency lock updates |
| python tools/check.py --publication-only | PASS: local Maven publication/classifiers and all three artifact consumers; absent-repository failure verified |
| cargo fmt --all --check; cargo test --workspace --locked; cargo clippy --workspace --all-targets --locked -- -D warnings | PASS: two Rust tests and static gates |
| npm ci --ignore-scripts; npm run contracts:check; npm run contracts:build; npm --workspace @olo-labs/toolgate-contracts test | PASS: compile-negative checks and JSON corpus transport |
| php packages/contracts/php/tests/roundtrip.php; php -l <each generated source> | PASS: 22 model round trips and security negatives; all sources lint |
| helm lint --strict; helm template; helm template --is-upgrade | PASS: valid empty chart; invalid gateway.enabled value rejected |
| python tools/check.py --scans | PASS: npm audit, pip-audit, actionlint, 61-entry dependency license audit, redacted Gitleaks and Trivy high/critical gates |
| python tools/release/bundle.py; helm package; Trivy CycloneDX export; python tools/release/bundle.py --checksums-only | PASS: raw bundle/chart/Java classifiers/SBOM/license inventory; every checksum verified |
| git diff --check | PASS |

Release assets were prepared in ignored build/release. No remote artifact was
published, tag pushed, GitHub release created or workload deployed. GitHub-hosted
workflow execution and provenance issuance require pushing this change into the
configured protected CI environment. Local actionlint and CI smoke tests passed.

## Revalidation on 2026-10-02

Reviewed the committed foundation against the governing documents and existing
coverage table before making further edits. Tightened release version validation
to reject leading zeroes in numeric prerelease identifiers before writing version
sources. Tightened compatibility checks to reject enum additions, removals and
replacement of an unrestricted string with an enum, because existing generated
readers use closed enums. Frozen v1 schemas and fixtures remain unchanged.

Regression tests cover both guards and verify invalid version input leaves both
version sources unchanged. The 17-test Python suite, full `make check`, local
Maven publication and artifact consumer proof, empty default/upgrade Helm renders,
and all scan hooks passed again. Generated-code drift, version synchronization,
source quality and whitespace checks also passed. Changes are confined to
`tools/contracts/version.py`, `tests/contracts/test_foundation.py`,
`docs/development/foundation-upgrades.md` and this report. The completion matrix
still classifies 49 requirements as COMPLETE and 58 as NOT_APPLICABLE, with none
BLOCKED. No later module was started and no remote publication was performed.

## Definition of done application

- [x] Primary foundation path implemented, without production mocks or security bypass.
- [x] Shared models, architectural ownership and future repository split preserved.
- [x] Applicable unit/negative/contract/security/build-boundary integration tests pass.
- [x] Generated drift, static/type/lint, copyright/license and secret checks pass.
- [x] Local Java publication and published-mode service proof pass.
- [x] Versioning, release config, compatibility, upgrade and troubleshooting documented.
- [x] Empty Helm skeleton, schema, lint/default/upgrade/negative render tests pass.
- [x] SBOM/checksum assets produced; protected publication/provenance configured.
- [x] Every requirement is classified below; none is BLOCKED.
- Runtime E2E/performance, operations telemetry, database migrations and product
  Docker/Kubernetes runtime items are NOT_APPLICABLE with individual reasons.

## Limitations

Bindings are structural; full canonical validation is still a consumer duty.
There is no runtime API, authorization, enrollment service, package execution,
database, service image or Kubernetes workload. Existing later-module dev/E2E/
benchmark commands remain explicitly unsupported. No stable N/N-1 runtime window
is claimed; the initial fixture baseline is v1 and there is no prior stable release.
Remote Maven credentials and foundation-release environment protection must be
provided by CI administrators; these were neither requested nor stored locally.
Rust/npm/Composer registry publication remains opt-in, with identities and packages
established. Artifact signing is not configured. No runtime observability was added.

## Requirement completion matrix

| Requirement | Status | Evidence or reason |
|---|---|---|
| ARC-001 | COMPLETE | Canonical contracts only in packages/contracts; generated models are data-only; shared imports exercised in three service scaffolds. ADR 001 records boundaries and split readiness. |
| ARC-002 | COMPLETE | Canonical contracts only in packages/contracts; generated models are data-only; shared imports exercised in three service scaffolds. ADR 001 records boundaries and split readiness. |
| ARC-003 | COMPLETE | Canonical contracts only in packages/contracts; generated models are data-only; shared imports exercised in three service scaffolds. ADR 001 records boundaries and split readiness. |
| ARC-004 | COMPLETE | Canonical contracts only in packages/contracts; generated models are data-only; shared imports exercised in three service scaffolds. ADR 001 records boundaries and split readiness. |
| ARC-005 | COMPLETE | Canonical contracts only in packages/contracts; generated models are data-only; shared imports exercised in three service scaffolds. ADR 001 records boundaries and split readiness. |
| ARC-006 | NOT_APPLICABLE | No lifecycle workflow or production app container exists in Module 00; only shared value models and build-consuming scaffolds. |
| ARC-007 | NOT_APPLICABLE | No lifecycle workflow or production app container exists in Module 00; only shared value models and build-consuming scaffolds. |
| CON-001 | COMPLETE | Nine v1 schema groups; VERSION/version.py; deterministic generator and manifest; frozen compatibility corpus; local Maven JAR/POM/sources/Javadoc and three artifact-mode builds tested. |
| CON-002 | COMPLETE | Nine v1 schema groups; VERSION/version.py; deterministic generator and manifest; frozen compatibility corpus; local Maven JAR/POM/sources/Javadoc and three artifact-mode builds tested. |
| CON-003 | COMPLETE | Nine v1 schema groups; VERSION/version.py; deterministic generator and manifest; frozen compatibility corpus; local Maven JAR/POM/sources/Javadoc and three artifact-mode builds tested. |
| CON-004 | COMPLETE | Nine v1 schema groups; VERSION/version.py; deterministic generator and manifest; frozen compatibility corpus; local Maven JAR/POM/sources/Javadoc and three artifact-mode builds tested. |
| CON-005 | COMPLETE | Nine v1 schema groups; VERSION/version.py; deterministic generator and manifest; frozen compatibility corpus; local Maven JAR/POM/sources/Javadoc and three artifact-mode builds tested. |
| CON-006 | COMPLETE | Nine v1 schema groups; VERSION/version.py; deterministic generator and manifest; frozen compatibility corpus; local Maven JAR/POM/sources/Javadoc and three artifact-mode builds tested. |
| SEC-001 | COMPLETE | Explicit non-defaulted decision enum, closed security schemas, no secret fields in errors/enrollment, credential references only; trust-domain mismatch and unknown/missing field negatives pass. This is contract-boundary coverage, not a runtime verifier. |
| SEC-002 | COMPLETE | Explicit non-defaulted decision enum, closed security schemas, no secret fields in errors/enrollment, credential references only; trust-domain mismatch and unknown/missing field negatives pass. This is contract-boundary coverage, not a runtime verifier. |
| SEC-003 | NOT_APPLICABLE | No authorization/signature verifier, execution sandbox, external request, replay store, key management or audited mutation is implemented. Signature fixtures are explicitly non-signatures; ownership remains in later modules. |
| SEC-004 | COMPLETE | Explicit non-defaulted decision enum, closed security schemas, no secret fields in errors/enrollment, credential references only; trust-domain mismatch and unknown/missing field negatives pass. This is contract-boundary coverage, not a runtime verifier. |
| SEC-005 | COMPLETE | Explicit non-defaulted decision enum, closed security schemas, no secret fields in errors/enrollment, credential references only; trust-domain mismatch and unknown/missing field negatives pass. This is contract-boundary coverage, not a runtime verifier. |
| SEC-006 | NOT_APPLICABLE | No authorization/signature verifier, execution sandbox, external request, replay store, key management or audited mutation is implemented. Signature fixtures are explicitly non-signatures; ownership remains in later modules. |
| SEC-007 | NOT_APPLICABLE | No authorization/signature verifier, execution sandbox, external request, replay store, key management or audited mutation is implemented. Signature fixtures are explicitly non-signatures; ownership remains in later modules. |
| SEC-008 | NOT_APPLICABLE | No authorization/signature verifier, execution sandbox, external request, replay store, key management or audited mutation is implemented. Signature fixtures are explicitly non-signatures; ownership remains in later modules. |
| SEC-009 | NOT_APPLICABLE | No authorization/signature verifier, execution sandbox, external request, replay store, key management or audited mutation is implemented. Signature fixtures are explicitly non-signatures; ownership remains in later modules. |
| SEC-010 | NOT_APPLICABLE | No authorization/signature verifier, execution sandbox, external request, replay store, key management or audited mutation is implemented. Signature fixtures are explicitly non-signatures; ownership remains in later modules. |
| TST-001 | COMPLETE | 17 offline Python schema/boundary tests; Java round trips/security tests plus three service dependency tests; two Rust tests; TS compile-negative/transport tests; 22 PHP model round trips; real Maven proof with empty-repository negative. No timing-based correctness. |
| TST-002 | COMPLETE | 17 offline Python schema/boundary tests; Java round trips/security tests plus three service dependency tests; two Rust tests; TS compile-negative/transport tests; 22 PHP model round trips; real Maven proof with empty-repository negative. No timing-based correctness. |
| TST-003 | COMPLETE | 17 offline Python schema/boundary tests; Java round trips/security tests plus three service dependency tests; two Rust tests; TS compile-negative/transport tests; 22 PHP model round trips; real Maven proof with empty-repository negative. No timing-based correctness. |
| TST-004 | COMPLETE | 17 offline Python schema/boundary tests; Java round trips/security tests plus three service dependency tests; two Rust tests; TS compile-negative/transport tests; 22 PHP model round trips; real Maven proof with empty-repository negative. No timing-based correctness. |
| TST-005 | COMPLETE | 17 offline Python schema/boundary tests; Java round trips/security tests plus three service dependency tests; two Rust tests; TS compile-negative/transport tests; 22 PHP model round trips; real Maven proof with empty-repository negative. No timing-based correctness. |
| TST-006 | NOT_APPLICABLE | No running product cross-component flow or performance-critical runtime exists. Build-boundary integration is covered by the real Maven artifact proof. |
| TST-007 | NOT_APPLICABLE | No running product cross-component flow or performance-critical runtime exists. Build-boundary integration is covered by the real Maven artifact proof. |
| TST-008 | COMPLETE | 17 offline Python schema/boundary tests; Java round trips/security tests plus three service dependency tests; two Rust tests; TS compile-negative/transport tests; 22 PHP model round trips; real Maven proof with empty-repository negative. No timing-based correctness. |
| QLT-001 | COMPLETE | quality.py checks headers/npm licenses; dependency_licenses.py audits 61 resolved packages; central catalogs and lockfiles; Java -Xlint:all -Werror, Rust fmt/clippy, TS type checks, PHP lint. Generated value models contain no business workflows. |
| QLT-002 | COMPLETE | quality.py checks headers/npm licenses; dependency_licenses.py audits 61 resolved packages; central catalogs and lockfiles; Java -Xlint:all -Werror, Rust fmt/clippy, TS type checks, PHP lint. Generated value models contain no business workflows. |
| QLT-003 | COMPLETE | quality.py checks headers/npm licenses; dependency_licenses.py audits 61 resolved packages; central catalogs and lockfiles; Java -Xlint:all -Werror, Rust fmt/clippy, TS type checks, PHP lint. Generated value models contain no business workflows. |
| QLT-004 | COMPLETE | quality.py checks headers/npm licenses; dependency_licenses.py audits 61 resolved packages; central catalogs and lockfiles; Java -Xlint:all -Werror, Rust fmt/clippy, TS type checks, PHP lint. Generated value models contain no business workflows. |
| QLT-005 | COMPLETE | quality.py checks headers/npm licenses; dependency_licenses.py audits 61 resolved packages; central catalogs and lockfiles; Java -Xlint:all -Werror, Rust fmt/clippy, TS type checks, PHP lint. Generated value models contain no business workflows. |
| QLT-006 | COMPLETE | quality.py checks headers/npm licenses; dependency_licenses.py audits 61 resolved packages; central catalogs and lockfiles; Java -Xlint:all -Werror, Rust fmt/clippy, TS type checks, PHP lint. Generated value models contain no business workflows. |
| API-001 | NOT_APPLICABLE | No HTTP endpoint or mutation API exists; OpenAPI directory is canonical and reserved without inventing routes. |
| API-002 | NOT_APPLICABLE | No HTTP endpoint or mutation API exists; OpenAPI directory is canonical and reserved without inventing routes. |
| API-003 | COMPLETE | Canonical ErrorCode/ErrorEnvelope/RequestContext and required request IDs; offline validation and negative fixtures. No HTTP endpoint is introduced. |
| API-004 | COMPLETE | Canonical ErrorCode/ErrorEnvelope/RequestContext and required request IDs; offline validation and negative fixtures. No HTTP endpoint is introduced. |
| API-005 | COMPLETE | Canonical ErrorCode/ErrorEnvelope/RequestContext and required request IDs; offline validation and negative fixtures. No HTTP endpoint is introduced. |
| API-006 | NOT_APPLICABLE | No HTTP endpoint or mutation API exists; OpenAPI directory is canonical and reserved without inventing routes. |
| API-007 | COMPLETE | Canonical ErrorCode/ErrorEnvelope/RequestContext and required request IDs; offline validation and negative fixtures. No HTTP endpoint is introduced. |
| DAT-001 | NOT_APPLICABLE | No database, persistent application state or migration is introduced. |
| DAT-002 | NOT_APPLICABLE | No database, persistent application state or migration is introduced. |
| DAT-003 | NOT_APPLICABLE | No database, persistent application state or migration is introduced. |
| DAT-004 | NOT_APPLICABLE | No database, persistent application state or migration is introduced. |
| DAT-005 | NOT_APPLICABLE | No database, persistent application state or migration is introduced. |
| OBS-001 | NOT_APPLICABLE | No long-running process or async job is delivered; runtime telemetry belongs to the later owning service modules. |
| OBS-002 | NOT_APPLICABLE | No long-running process or async job is delivered; runtime telemetry belongs to the later owning service modules. |
| OBS-003 | NOT_APPLICABLE | No long-running process or async job is delivered; runtime telemetry belongs to the later owning service modules. |
| OBS-004 | NOT_APPLICABLE | No long-running process or async job is delivered; runtime telemetry belongs to the later owning service modules. |
| OBS-005 | NOT_APPLICABLE | No long-running process or async job is delivered; runtime telemetry belongs to the later owning service modules. |
| OBS-006 | NOT_APPLICABLE | No long-running process or async job is delivered; runtime telemetry belongs to the later owning service modules. |
| OBS-007 | NOT_APPLICABLE | No long-running process or async job is delivered; runtime telemetry belongs to the later owning service modules. |
| DKR-001 | NOT_APPLICABLE | No deployable application or runtime image exists. Containers used during validation are toolchains/scanners, not product workloads; GHCR names are documented. |
| DKR-002 | NOT_APPLICABLE | No deployable application or runtime image exists. Containers used during validation are toolchains/scanners, not product workloads; GHCR names are documented. |
| DKR-003 | NOT_APPLICABLE | No deployable application or runtime image exists. Containers used during validation are toolchains/scanners, not product workloads; GHCR names are documented. |
| DKR-004 | NOT_APPLICABLE | No deployable application or runtime image exists. Containers used during validation are toolchains/scanners, not product workloads; GHCR names are documented. |
| DKR-005 | NOT_APPLICABLE | No deployable application or runtime image exists. Containers used during validation are toolchains/scanners, not product workloads; GHCR names are documented. |
| DKR-006 | NOT_APPLICABLE | No deployable application or runtime image exists. Containers used during validation are toolchains/scanners, not product workloads; GHCR names are documented. |
| DKR-007 | NOT_APPLICABLE | No deployable application or runtime image exists. Containers used during validation are toolchains/scanners, not product workloads; GHCR names are documented. |
| DKR-008 | NOT_APPLICABLE | No deployable application or runtime image exists. Containers used during validation are toolchains/scanners, not product workloads; GHCR names are documented. |
| K8S-001 | NOT_APPLICABLE | Foundation chart intentionally renders no workloads or resources. Probes, network policies, secrets integration, HA, cluster install/upgrade/rollback apply when real services are delivered. |
| K8S-002 | NOT_APPLICABLE | Foundation chart intentionally renders no workloads or resources. Probes, network policies, secrets integration, HA, cluster install/upgrade/rollback apply when real services are delivered. |
| K8S-003 | NOT_APPLICABLE | Foundation chart intentionally renders no workloads or resources. Probes, network policies, secrets integration, HA, cluster install/upgrade/rollback apply when real services are delivered. |
| K8S-004 | NOT_APPLICABLE | Foundation chart intentionally renders no workloads or resources. Probes, network policies, secrets integration, HA, cluster install/upgrade/rollback apply when real services are delivered. |
| K8S-005 | NOT_APPLICABLE | Foundation chart intentionally renders no workloads or resources. Probes, network policies, secrets integration, HA, cluster install/upgrade/rollback apply when real services are delivered. |
| K8S-006 | NOT_APPLICABLE | Foundation chart intentionally renders no workloads or resources. Probes, network policies, secrets integration, HA, cluster install/upgrade/rollback apply when real services are delivered. |
| K8S-007 | NOT_APPLICABLE | Foundation chart intentionally renders no workloads or resources. Probes, network policies, secrets integration, HA, cluster install/upgrade/rollback apply when real services are delivered. |
| K8S-008 | NOT_APPLICABLE | Foundation chart intentionally renders no workloads or resources. Probes, network policies, secrets integration, HA, cluster install/upgrade/rollback apply when real services are delivered. |
| K8S-009 | NOT_APPLICABLE | Foundation chart intentionally renders no workloads or resources. Probes, network policies, secrets integration, HA, cluster install/upgrade/rollback apply when real services are delivered. |
| K8S-010 | NOT_APPLICABLE | Foundation chart intentionally renders no workloads or resources. Probes, network policies, secrets integration, HA, cluster install/upgrade/rollback apply when real services are delivered. |
| K8S-011 | NOT_APPLICABLE | Foundation chart intentionally renders no workloads or resources. Probes, network policies, secrets integration, HA, cluster install/upgrade/rollback apply when real services are delivered. |
| K8S-012 | NOT_APPLICABLE | Foundation chart intentionally renders no workloads or resources. Probes, network policies, secrets integration, HA, cluster install/upgrade/rollback apply when real services are delivered. |
| K8S-013 | COMPLETE | helm lint --strict passes; default and --is-upgrade renders are empty; unknown workload values are rejected. Zero Kubernetes resources exist to schema-validate. |
| K8S-014 | COMPLETE | helm lint --strict passes; default and --is-upgrade renders are empty; unknown workload values are rejected. Zero Kubernetes resources exist to schema-validate. |
| K8S-015 | NOT_APPLICABLE | Foundation chart intentionally renders no workloads or resources. Probes, network policies, secrets integration, HA, cluster install/upgrade/rollback apply when real services are delivered. |
| K8S-016 | NOT_APPLICABLE | Foundation chart intentionally renders no workloads or resources. Probes, network policies, secrets integration, HA, cluster install/upgrade/rollback apply when real services are delivered. |
| REL-001 | COMPLETE | release-foundation.yml configures protected CI Maven/Helm/GitHub publishing and GitHub attestations; raw bundle, Java classifiers and Helm package built locally; SHA256SUMS verified; CycloneDX SBOM and 61-entry license inventory produced; compatibility/upgrade docs and CHANGELOG updated. Remote publication/attestation are intentionally CI-only and were not executed locally. |
| REL-002 | NOT_APPLICABLE | No deployable container exists (REL-002); artifact signing keys are not configured (REL-008). GitHub provenance is configured independently. |
| REL-003 | COMPLETE | release-foundation.yml configures protected CI Maven/Helm/GitHub publishing and GitHub attestations; raw bundle, Java classifiers and Helm package built locally; SHA256SUMS verified; CycloneDX SBOM and 61-entry license inventory produced; compatibility/upgrade docs and CHANGELOG updated. Remote publication/attestation are intentionally CI-only and were not executed locally. |
| REL-004 | COMPLETE | release-foundation.yml configures protected CI Maven/Helm/GitHub publishing and GitHub attestations; raw bundle, Java classifiers and Helm package built locally; SHA256SUMS verified; CycloneDX SBOM and 61-entry license inventory produced; compatibility/upgrade docs and CHANGELOG updated. Remote publication/attestation are intentionally CI-only and were not executed locally. |
| REL-005 | COMPLETE | release-foundation.yml configures protected CI Maven/Helm/GitHub publishing and GitHub attestations; raw bundle, Java classifiers and Helm package built locally; SHA256SUMS verified; CycloneDX SBOM and 61-entry license inventory produced; compatibility/upgrade docs and CHANGELOG updated. Remote publication/attestation are intentionally CI-only and were not executed locally. |
| REL-006 | COMPLETE | release-foundation.yml configures protected CI Maven/Helm/GitHub publishing and GitHub attestations; raw bundle, Java classifiers and Helm package built locally; SHA256SUMS verified; CycloneDX SBOM and 61-entry license inventory produced; compatibility/upgrade docs and CHANGELOG updated. Remote publication/attestation are intentionally CI-only and were not executed locally. |
| REL-007 | COMPLETE | release-foundation.yml configures protected CI Maven/Helm/GitHub publishing and GitHub attestations; raw bundle, Java classifiers and Helm package built locally; SHA256SUMS verified; CycloneDX SBOM and 61-entry license inventory produced; compatibility/upgrade docs and CHANGELOG updated. Remote publication/attestation are intentionally CI-only and were not executed locally. |
| REL-008 | NOT_APPLICABLE | No deployable container exists (REL-002); artifact signing keys are not configured (REL-008). GitHub provenance is configured independently. |
| REL-009 | COMPLETE | release-foundation.yml configures protected CI Maven/Helm/GitHub publishing and GitHub attestations; raw bundle, Java classifiers and Helm package built locally; SHA256SUMS verified; CycloneDX SBOM and 61-entry license inventory produced; compatibility/upgrade docs and CHANGELOG updated. Remote publication/attestation are intentionally CI-only and were not executed locally. |
| REL-010 | COMPLETE | release-foundation.yml configures protected CI Maven/Helm/GitHub publishing and GitHub attestations; raw bundle, Java classifiers and Helm package built locally; SHA256SUMS verified; CycloneDX SBOM and 61-entry license inventory produced; compatibility/upgrade docs and CHANGELOG updated. Remote publication/attestation are intentionally CI-only and were not executed locally. |
| REL-011 | COMPLETE | release-foundation.yml configures protected CI Maven/Helm/GitHub publishing and GitHub attestations; raw bundle, Java classifiers and Helm package built locally; SHA256SUMS verified; CycloneDX SBOM and 61-entry license inventory produced; compatibility/upgrade docs and CHANGELOG updated. Remote publication/attestation are intentionally CI-only and were not executed locally. |
| DOC-001 | COMPLETE | README/DEVELOPMENT/CONTRACTS, five package READMEs, chart README, ADR 001, foundation workflow/upgrade docs, coverage plan and this report; local documentation link tests pass. |
| DOC-002 | COMPLETE | README/DEVELOPMENT/CONTRACTS, five package READMEs, chart README, ADR 001, foundation workflow/upgrade docs, coverage plan and this report; local documentation link tests pass. |
| DOC-003 | COMPLETE | README/DEVELOPMENT/CONTRACTS, five package READMEs, chart README, ADR 001, foundation workflow/upgrade docs, coverage plan and this report; local documentation link tests pass. |
| DOC-004 | COMPLETE | README/DEVELOPMENT/CONTRACTS, five package READMEs, chart README, ADR 001, foundation workflow/upgrade docs, coverage plan and this report; local documentation link tests pass. |
| DOC-005 | COMPLETE | README/DEVELOPMENT/CONTRACTS, five package READMEs, chart README, ADR 001, foundation workflow/upgrade docs, coverage plan and this report; local documentation link tests pass. |
| DOC-006 | COMPLETE | README/DEVELOPMENT/CONTRACTS, five package READMEs, chart README, ADR 001, foundation workflow/upgrade docs, coverage plan and this report; local documentation link tests pass. |
| DOC-007 | COMPLETE | README/DEVELOPMENT/CONTRACTS, five package READMEs, chart README, ADR 001, foundation workflow/upgrade docs, coverage plan and this report; local documentation link tests pass. |
| PRF-001 | NOT_APPLICABLE | No Gateway authorization path, database path, fleet workflow, upload/queue or performance-sensitive runtime exists. Schema collection/string bounds are input constraints, not performance claims. |
| PRF-002 | NOT_APPLICABLE | No Gateway authorization path, database path, fleet workflow, upload/queue or performance-sensitive runtime exists. Schema collection/string bounds are input constraints, not performance claims. |
| PRF-003 | NOT_APPLICABLE | No Gateway authorization path, database path, fleet workflow, upload/queue or performance-sensitive runtime exists. Schema collection/string bounds are input constraints, not performance claims. |
| PRF-004 | NOT_APPLICABLE | No Gateway authorization path, database path, fleet workflow, upload/queue or performance-sensitive runtime exists. Schema collection/string bounds are input constraints, not performance claims. |
| PRF-005 | NOT_APPLICABLE | No Gateway authorization path, database path, fleet workflow, upload/queue or performance-sensitive runtime exists. Schema collection/string bounds are input constraints, not performance claims. |
| PRF-006 | NOT_APPLICABLE | No Gateway authorization path, database path, fleet workflow, upload/queue or performance-sensitive runtime exists. Schema collection/string bounds are input constraints, not performance claims. |
| ARC-001 | COMPLETE | Canonical contracts only in packages/contracts; generated models are data-only; shared imports exercised in three service scaffolds. ADR 001 records boundaries and split readiness. |
| SEC-001 | COMPLETE | Explicit non-defaulted decision enum, closed security schemas, no secret fields in errors/enrollment, credential references only; trust-domain mismatch and unknown/missing field negatives pass. This is contract-boundary coverage, not a runtime verifier. |
| K8S-001 | NOT_APPLICABLE | Foundation chart intentionally renders no workloads or resources. Probes, network policies, secrets integration, HA, cluster install/upgrade/rollback apply when real services are delivered. |

## Files changed

Full source inventory below; generated outputs are also listed in
`packages/contracts/generated-manifest.json`. Changes remain in the workspace.

- `.gitattributes`
- `.github/DISCUSSION_TEMPLATE/ideas.yml`
- `.github/ISSUE_TEMPLATE/bug.yml`
- `.github/ISSUE_TEMPLATE/config.yml`
- `.github/ISSUE_TEMPLATE/feature.yml`
- `.github/workflows/contracts.yml`
- `.github/workflows/docs.yml`
- `.github/workflows/foundation.yml`
- `.github/workflows/gradle.yml`
- `.github/workflows/release-foundation.yml`
- `.github/workflows/scaffold-check.yml`
- `.gitignore`
- `.gitleaks.toml`
- `.gitleaksignore`
- `.trivyignore`
- `CHANGELOG.md`
- `CONTRACTS.md`
- `Cargo.lock`
- `Cargo.toml`
- `DEVELOPMENT.md`
- `Makefile`
- `README.md`
- `VERSION`
- `apps/control-plane/build.gradle.kts`
- `apps/control-plane/gradle-artifact.lockfile`
- `apps/control-plane/gradle.lockfile`
- `apps/control-plane/src/main/java/io/ololabs/toolgate/control/ControlPlaneModule.java`
- `apps/control-plane/src/test/java/io/ololabs/toolgate/control/ContractDependencyTest.java`
- `apps/marketplace-api/build.gradle.kts`
- `apps/marketplace-api/gradle-artifact.lockfile`
- `apps/marketplace-api/gradle.lockfile`
- `apps/marketplace-api/src/main/java/io/ololabs/toolgate/marketplace/api/MarketplaceApiModule.java`
- `apps/marketplace-api/src/test/java/io/ololabs/toolgate/marketplace/api/ContractDependencyTest.java`
- `apps/marketplace-worker/build.gradle.kts`
- `apps/marketplace-worker/gradle-artifact.lockfile`
- `apps/marketplace-worker/gradle.lockfile`
- `apps/marketplace-worker/src/main/java/io/ololabs/toolgate/marketplace/worker/MarketplaceWorkerModule.java`
- `apps/marketplace-worker/src/test/java/io/ololabs/toolgate/marketplace/worker/ContractDependencyTest.java`
- `build.gradle.kts`
- `deploy/helm/olo-toolgate/Chart.yaml`
- `deploy/helm/olo-toolgate/README.md`
- `deploy/helm/olo-toolgate/templates/NOTES.txt`
- `deploy/helm/olo-toolgate/templates/_helpers.tpl`
- `deploy/helm/olo-toolgate/values.schema.json`
- `deploy/helm/olo-toolgate/values.yaml`
- `docs/adr/001-foundation-contract-generation.md`
- `docs/codex/09-REQUIREMENTS-TRACEABILITY.md`
- `docs/codex/modules/00-completion.md`
- `docs/codex/modules/00-coverage.md`
- `docs/development/foundation-upgrades.md`
- `docs/development/foundation.md`
- `docs/examples/package-python.yaml`
- `docs/examples/policy.yaml`
- `gradle.properties`
- `gradle/bootstrap.ps1`
- `gradle/libs.versions.toml`
- `gradlew`
- `gradlew.bat`
- `package-lock.json`
- `package.json`
- `packages/contracts/contract-set.yaml`
- `packages/contracts/generated-manifest.json`
- `packages/contracts/java/LICENSE`
- `packages/contracts/java/README.md`
- `packages/contracts/java/build.gradle.kts`
- `packages/contracts/java/gradle.lockfile`
- `packages/contracts/java/src/main/java/io/ololabs/toolgate/contracts/ArtifactDescriptor.java`
- `packages/contracts/java/src/main/java/io/ololabs/toolgate/contracts/ClientEnrollmentRequest.java`
- `packages/contracts/java/src/main/java/io/ololabs/toolgate/contracts/ClientReport.java`
- `packages/contracts/java/src/main/java/io/ololabs/toolgate/contracts/ContractSet.java`
- `packages/contracts/java/src/main/java/io/ololabs/toolgate/contracts/Decision.java`
- `packages/contracts/java/src/main/java/io/ololabs/toolgate/contracts/DecisionReason.java`
- `packages/contracts/java/src/main/java/io/ololabs/toolgate/contracts/DeploymentAssignment.java`
- `packages/contracts/java/src/main/java/io/ololabs/toolgate/contracts/DesiredState.java`
- `packages/contracts/java/src/main/java/io/ololabs/toolgate/contracts/ErrorCode.java`
- `packages/contracts/java/src/main/java/io/ololabs/toolgate/contracts/ErrorEnvelope.java`
- `packages/contracts/java/src/main/java/io/ololabs/toolgate/contracts/MarketplaceRelease.java`
- `packages/contracts/java/src/main/java/io/ololabs/toolgate/contracts/PackageCompatibility.java`
- `packages/contracts/java/src/main/java/io/ololabs/toolgate/contracts/PackageManifest.java`
- `packages/contracts/java/src/main/java/io/ololabs/toolgate/contracts/PackageState.java`
- `packages/contracts/java/src/main/java/io/ololabs/toolgate/contracts/PolicyDecision.java`
- `packages/contracts/java/src/main/java/io/ololabs/toolgate/contracts/PolicyInput.java`
- `packages/contracts/java/src/main/java/io/ololabs/toolgate/contracts/ReportedPackage.java`
- `packages/contracts/java/src/main/java/io/ololabs/toolgate/contracts/RequestContext.java`
- `packages/contracts/java/src/main/java/io/ololabs/toolgate/contracts/ResourceDescriptor.java`
- `packages/contracts/java/src/main/java/io/ololabs/toolgate/contracts/ResourceKind.java`
- `packages/contracts/java/src/main/java/io/ololabs/toolgate/contracts/ToolAction.java`
- `packages/contracts/java/src/main/java/io/ololabs/toolgate/contracts/ToolDefinition.java`
- `packages/contracts/java/src/test/java/io/ololabs/toolgate/contracts/ContractRoundTripTest.java`
- `packages/contracts/php/LICENSE`
- `packages/contracts/php/README.md`
- `packages/contracts/php/src/ArtifactDescriptor.php`
- `packages/contracts/php/src/ClientEnrollmentRequest.php`
- `packages/contracts/php/src/ClientReport.php`
- `packages/contracts/php/src/ContractSet.php`
- `packages/contracts/php/src/Decision.php`
- `packages/contracts/php/src/DecisionReason.php`
- `packages/contracts/php/src/DeploymentAssignment.php`
- `packages/contracts/php/src/DesiredState.php`
- `packages/contracts/php/src/ErrorCode.php`
- `packages/contracts/php/src/ErrorEnvelope.php`
- `packages/contracts/php/src/MarketplaceRelease.php`
- `packages/contracts/php/src/PackageCompatibility.php`
- `packages/contracts/php/src/PackageManifest.php`
- `packages/contracts/php/src/PackageState.php`
- `packages/contracts/php/src/PolicyDecision.php`
- `packages/contracts/php/src/PolicyInput.php`
- `packages/contracts/php/src/ReportedPackage.php`
- `packages/contracts/php/src/RequestContext.php`
- `packages/contracts/php/src/ResourceDescriptor.php`
- `packages/contracts/php/src/ResourceKind.php`
- `packages/contracts/php/src/ToolAction.php`
- `packages/contracts/php/src/ToolDefinition.php`
- `packages/contracts/php/tests/roundtrip.php`
- `packages/contracts/rust/Cargo.toml`
- `packages/contracts/rust/LICENSE`
- `packages/contracts/rust/README.md`
- `packages/contracts/rust/src/lib.rs`
- `packages/contracts/rust/tests/roundtrip.rs`
- `packages/contracts/schemas/README.md`
- `packages/contracts/schemas/v1/client.schema.json`
- `packages/contracts/schemas/v1/common.schema.json`
- `packages/contracts/schemas/v1/deployment.schema.json`
- `packages/contracts/schemas/v1/error.schema.json`
- `packages/contracts/schemas/v1/identifiers.schema.json`
- `packages/contracts/schemas/v1/package.schema.json`
- `packages/contracts/schemas/v1/policy.schema.json`
- `packages/contracts/schemas/v1/resource.schema.json`
- `packages/contracts/schemas/v1/tool.schema.json`
- `packages/contracts/typescript/LICENSE`
- `packages/contracts/typescript/README.md`
- `packages/contracts/typescript/package.json`
- `packages/contracts/typescript/src/index.ts`
- `packages/contracts/typescript/test/roundtrip.mjs`
- `packages/contracts/typescript/test/types.ts`
- `packages/contracts/typescript/tsconfig.test.json`
- `rust-toolchain.toml`
- `settings-gradle.lockfile`
- `settings.gradle.kts`
- `tests/contracts/test_foundation.py`
- `tests/fixtures/contracts/v1/compatibility.json`
- `tests/fixtures/contracts/v1/schemas/client.schema.json`
- `tests/fixtures/contracts/v1/schemas/common.schema.json`
- `tests/fixtures/contracts/v1/schemas/deployment.schema.json`
- `tests/fixtures/contracts/v1/schemas/error.schema.json`
- `tests/fixtures/contracts/v1/schemas/identifiers.schema.json`
- `tests/fixtures/contracts/v1/schemas/package.schema.json`
- `tests/fixtures/contracts/v1/schemas/policy.schema.json`
- `tests/fixtures/contracts/v1/schemas/resource.schema.json`
- `tests/fixtures/contracts/v1/schemas/tool.schema.json`
- `tests/fixtures/contracts/v1/valid.json`
- `tools/check.py`
- `tools/contracts/check.sh`
- `tools/contracts/generate.py`
- `tools/contracts/release-dry-run.sh`
- `tools/contracts/version.py`
- `tools/dependency_licenses.py`
- `tools/dev/check.sh`
- `tools/dev/dev.sh`
- `tools/dev/docs-check.sh`
- `tools/dev/not-implemented.sh`
- `tools/dev/stop.sh`
- `tools/quality.py`
- `tools/release/bundle.py`
- `tools/requirements.txt`

## Suggested commit

```text
feat: establish shared contracts, build system and CI foundation
```
