# Module 11 verification and completion report

Status: **DONE for the documented Linux/amd64 Docker Quickstart scope**, 2026-10-04.
Earlier native Windows/macOS/ARM service and managed-runtime certification
blockers in Modules 06–10 remain open. This is a single-node/non-HA evaluation
package. Remote publication is protected CI-only and was not run locally.
No Module 12 is started.

Preparation preceded implementation: [coverage](11-coverage.md), master prompt,
traceability, governing documents and [definition of done](../08-DEFINITION-OF-DONE.md).
See [ADR 012a](../../adr/012a-single-node-quickstart.md), the
[walkthrough](../../getting-started/one-minute-quickstart.md) and
[production/debug configuration and recovery guide](../../deployment/quickstart.md).

## Delivered behavior

One non-root image combines the real Rust Gateway, existing Control JVM/domain
services, embedded console, fixed Rust built-in executor and bounded local
identity/vault/composition adapters. Explicit SQLite profile preserves the
production PostgreSQL default. No mock, permissive policy, author-code host
execution or Docker socket is introduced.

Private /data retains directory/audit/approvals/devices, verifier/generation,
separate trust keys, encrypted vault and HotFolder. First login changes the
password; signed administrator sessions stay in memory. Defaults seed the
workspace/team/agent and fixed policies. Exact welcome.txt effects require ASK
and online single-use permits; other paths/deletion are denied. Web search stays
disabled without provider configuration. Vault lists names only; custom credential
binding remains unavailable. Password changes record durable intent before file
replacement and completion afterward; those are separate commit boundaries.

Actual Windows/macOS/Linux archives download anonymously with verified hashes.
Direct TLS enrollment reviews the fingerprint and returns bound mTLS identity;
real check-in is tested. Native execution separately needs configured Gateway
credentials/policy and a system engine; historical certification limits remain.

Read-only root/noexec /tmp/cap-drop, aggregate readiness, private metrics,
redacted logs and child-failure shutdown pass. Offline consistent backup closes
SQLite connections before hashing WAL state. Restore requires an empty volume
and checks every file. Ordered migration retains identity/defaults; unknown
future state is rejected. Rollback uses a pre-upgrade backup in a new volume.

## Definition of done and executed evidence

Implementation, canonical contracts, real unit/negative/integration/security/E2E,
static/dependency/license/secret checks, health/metrics/correlation/shutdown and
Docker gates pass. Configurable launch resources and loopback/non-HA constraints
are documented. No Quickstart Helm workload is appropriate; existing production
chart regression gates pass. Release/docs include protected exact-tag publication,
immutable Release evidence, SBOM/checksums/provenance, compatibility and upgrades.
Product/contracts retain unreleased 0.10.0-dev; no earlier released Quickstart
image compatibility is invented.

Commands ran from the repository. Windows used the pinned Codex Python and
TOOLGATE_DOCKER_TOOLS=1 for isolated Rust/PHP/Helm.

| Executed command/gate | Result |
|---|---|
| `python tools/check.py` with `TOOLGATE_UPDATE_LOCKS=1` | PASS: 53 contract tests; JVM/workspace, local Maven and empty-repository negative proof; PostgreSQL HTTP/auth/audit/telemetry; Rust workspace tests/clippy/fmt; 53 UI units and five production browser checks; PHP 143 round trips/lint; all Helm schema/render/negative gates |
| `python tools/check.py --scans` | PASS: actionlint, npm/pip audits, 796 resolved license entries, gitleaks, source vulnerability/license scan |
| Control Docker build with `QUARKUS_PROFILE=quickstart`, then Quickstart build | PASS: actual production composition |
| `gradlew :control-plane:test --tests io.ololabs.toolgate.control.SqliteTest` | PASS: three real SQLite tests, including immutable migration checksum rejection |
| Linux image `python -m unittest discover -s tests/quickstart -v` | PASS: five real identity/custody tests, including WAL backup, stale session, audit failure, weak bootstrap and future layout |
| `python tools/quickstart/check.py` | PASS: all eight mandatory scenarios plus native download hashes, Host/Origin/default-deny/traversal, encrypted vault, single-use ASK, real browser accessibility, metrics and Gateway child outage |
| `cargo fmt --all --check`; quickstart binary clippy with `-D warnings` | PASS after final Rust boundary changes |
| Trivy 0.61.1 actual image scan HIGH/CRITICAL plus secret scan | PASS: no findings at the configured gate |
| Image CycloneDX plus `tools/ui/merge_sbom.py --image-sbom build/quickstart/quickstart-sbom.cdx.json` | PASS: Java/SQLite, Rust, Python/OS and embedded React inventory |
| Version/generation drift, source headers, `git diff --check` | PASS |

The broad Control JVM run passed 30 tests; the final SQLite-focused run passed
three tests, including checksum tampering. Java contracts
include two round-trip tests. Feature-specific fixtures are correctly skipped in
the ordinary repository browser run; Quickstart runs its own real browser fixture.
Existing managed-runtime Docker tests remain fixture-gated; no author runtime is
added here and their native certification is not claimed.

Ignored local evidence: build/quickstart/smoke.json, image-scan.json and
quickstart-sbom.cdx.json; protected CI uploads equivalent evidence. The final
cached-image first boot measured **19.91 seconds** on this workstation;
pull/network/source-build time is additional. This is not a universal one-minute
SLA. Failed development runs were fixed and production-image gates rerun.

## Individual requirement disposition

| ID | Status | Evidence/reason |
|---|---|---|
| ARC-001 | COMPLETE | ADR 012; composition owns lifecycle and local adapters; existing domain/application/store/authorization ports and canonical models retained. |
| ARC-002 | COMPLETE | ADR 012; composition owns lifecycle and local adapters; existing domain/application/store/authorization ports and canonical models retained. |
| ARC-003 | COMPLETE | ADR 012; composition owns lifecycle and local adapters; existing domain/application/store/authorization ports and canonical models retained. |
| ARC-004 | COMPLETE | ADR 012; composition owns lifecycle and local adapters; existing domain/application/store/authorization ports and canonical models retained. |
| ARC-005 | COMPLETE | ADR 012; composition owns lifecycle and local adapters; existing domain/application/store/authorization ports and canonical models retained. |
| ARC-006 | COMPLETE | ADR 012; composition owns lifecycle and local adapters; existing domain/application/store/authorization ports and canonical models retained. |
| ARC-007 | NOT_APPLICABLE | Stateful/non-HA Docker evaluation exception in ADR 012; production services retain external state. |
| CON-001 | COMPLETE | Canonical versioned OpenAPI and 311-file generated drift/frozen fixture gates; workspace and local Maven artifact-mode builds pass. |
| CON-002 | COMPLETE | Configured Maven identity io.ololabs.toolgate:toolgate-contracts; local JAR/POM/sources/Javadoc proof passed. |
| CON-003 | COMPLETE | Workspace and all three locally published-contract service builds passed; empty artifact repository correctly rejects compilation. |
| CON-004 | COMPLETE | Canonical versioned OpenAPI and 311-file generated drift/frozen fixture gates; workspace and local Maven artifact-mode builds pass. |
| CON-005 | COMPLETE | Canonical versioned OpenAPI and 311-file generated drift/frozen fixture gates; workspace and local Maven artifact-mode builds pass. |
| CON-006 | COMPLETE | Canonical versioned OpenAPI and 311-file generated drift/frozen fixture gates; workspace and local Maven artifact-mode builds pass. |
| SEC-001 | COMPLETE | Private separate generated keys, closed/bounded input, exact online Gateway/ASK/mTLS, durable audit and encrypted name-only vault; security/negative/image gates pass. |
| SEC-002 | COMPLETE | Private separate generated keys, closed/bounded input, exact online Gateway/ASK/mTLS, durable audit and encrypted name-only vault; security/negative/image gates pass. |
| SEC-003 | COMPLETE | Private separate generated keys, closed/bounded input, exact online Gateway/ASK/mTLS, durable audit and encrypted name-only vault; security/negative/image gates pass. |
| SEC-004 | COMPLETE | Marketplace/deployment/runtime trust remain separate; frozen trust negatives pass; Quickstart does not enable marketplace or fleet release authority. |
| SEC-005 | COMPLETE | Enrollment and stored credentials grant no execution permission; every fixed operation uses Gateway and consumes ASK permits online. |
| SEC-006 | COMPLETE | No author interpreter, JVM author-code execution or Docker socket in the Quickstart path; existing designated-client sandbox boundary retained. |
| SEC-007 | COMPLETE | Private separate generated keys, closed/bounded input, exact online Gateway/ASK/mTLS, durable audit and encrypted name-only vault; security/negative/image gates pass. |
| SEC-008 | COMPLETE | Independent RSA-3072 identity/policy/permit/device/TLS keys and AES-256 vault key, generated privately per volume. |
| SEC-009 | COMPLETE | Append-only Control mutation/approval/device/bundle audit; vault plus audit commit atomically; password intent precedes custody replacement and completion follows. |
| SEC-010 | COMPLETE | Private separate generated keys, closed/bounded input, exact online Gateway/ASK/mTLS, durable audit and encrypted name-only vault; security/negative/image gates pass. |
| TST-001 | COMPLETE | Real SQLite/JVM, five Linux identity/custody tests, contracts, PostgreSQL regression, Docker and browser E2E pass; bounded polling with state assertions. |
| TST-002 | COMPLETE | Real SQLite/JVM, five Linux identity/custody tests, contracts, PostgreSQL regression, Docker and browser E2E pass; bounded polling with state assertions. |
| TST-003 | COMPLETE | Real SQLite/JVM, five Linux identity/custody tests, contracts, PostgreSQL regression, Docker and browser E2E pass; bounded polling with state assertions. |
| TST-004 | COMPLETE | Real SQLite/JVM, five Linux identity/custody tests, contracts, PostgreSQL regression, Docker and browser E2E pass; bounded polling with state assertions. |
| TST-005 | COMPLETE | Real SQLite/JVM, five Linux identity/custody tests, contracts, PostgreSQL regression, Docker and browser E2E pass; bounded polling with state assertions. |
| TST-006 | COMPLETE | Real SQLite/JVM, five Linux identity/custody tests, contracts, PostgreSQL regression, Docker and browser E2E pass; bounded polling with state assertions. |
| TST-007 | NOT_APPLICABLE | No new evaluator hot path; existing Gateway benchmark retained. First boot is measured without a flaky latency assertion. |
| TST-008 | COMPLETE | Real SQLite/JVM, five Linux identity/custody tests, contracts, PostgreSQL regression, Docker and browser E2E pass; bounded polling with state assertions. |
| QLT-001 | COMPLETE | Headers, Java/Rust/TypeScript static checks and scans pass; server-owned identity/login state, main-owned lifecycle events, durable verifier/generation, canonical typed errors and pinned justified dependencies. |
| QLT-002 | COMPLETE | Headers, Java/Rust/TypeScript static checks and scans pass; server-owned identity/login state, main-owned lifecycle events, durable verifier/generation, canonical typed errors and pinned justified dependencies. |
| QLT-003 | COMPLETE | Headers, Java/Rust/TypeScript static checks and scans pass; server-owned identity/login state, main-owned lifecycle events, durable verifier/generation, canonical typed errors and pinned justified dependencies. |
| QLT-004 | COMPLETE | Headers, Java/Rust/TypeScript static checks and scans pass; server-owned identity/login state, main-owned lifecycle events, durable verifier/generation, canonical typed errors and pinned justified dependencies. |
| QLT-005 | COMPLETE | Server owns password writer/login budget/locks; main owns lifecycle events. Verifier/generation/audit are durable; domain logic remains in existing services. |
| QLT-006 | COMPLETE | Headers, Java/Rust/TypeScript static checks and scans pass; server-owned identity/login state, main-owned lifecycle events, durable verifier/generation, canonical typed errors and pinned justified dependencies. |
| API-001 | COMPLETE | Versioned quickstart-v1 OpenAPI, canonical tool/error boundaries, input/Host/Origin limits and memory-only signed sessions; existing Control mutation idempotency retained. |
| API-002 | COMPLETE | Versioned quickstart-v1 OpenAPI, canonical tool/error boundaries, input/Host/Origin limits and memory-only signed sessions; existing Control mutation idempotency retained. |
| API-003 | COMPLETE | Versioned quickstart-v1 OpenAPI, canonical tool/error boundaries, input/Host/Origin limits and memory-only signed sessions; existing Control mutation idempotency retained. |
| API-004 | COMPLETE | Versioned quickstart-v1 OpenAPI, canonical tool/error boundaries, input/Host/Origin limits and memory-only signed sessions; existing Control mutation idempotency retained. |
| API-005 | COMPLETE | Canonical error/header IDs agree; fixed proxy retains request/trace headers. Gateway creates its own request IDs and retains caller trace context. |
| API-006 | COMPLETE | Directory/approval/enrollment idempotency retained; once permit cannot execute twice. Vault is bounded name replacement, documented without replay-key semantics. |
| API-007 | COMPLETE | Versioned quickstart-v1 OpenAPI, canonical tool/error boundaries, input/Host/Origin limits and memory-only signed sessions; existing Control mutation idempotency retained. |
| DAT-001 | COMPLETE | Checksummed SQLite V1–V2, WAL/FULL/IMMEDIATE, real clean/upgrade/restart/audit/immutable tests; /data lock and consistent offline backup/restore. |
| DAT-002 | COMPLETE | Checksummed SQLite V1–V2, WAL/FULL/IMMEDIATE, real clean/upgrade/restart/audit/immutable tests; /data lock and consistent offline backup/restore. |
| DAT-003 | COMPLETE | Checksummed SQLite V1–V2, WAL/FULL/IMMEDIATE, real clean/upgrade/restart/audit/immutable tests; /data lock and consistent offline backup/restore. |
| DAT-004 | COMPLETE | Checksummed SQLite V1–V2, WAL/FULL/IMMEDIATE, real clean/upgrade/restart/audit/immutable tests; /data lock and consistent offline backup/restore. |
| DAT-005 | COMPLETE | Checksummed SQLite V1–V2, WAL/FULL/IMMEDIATE, real clean/upgrade/restart/audit/immutable tests; /data lock and consistent offline backup/restore. |
| OBS-001 | COMPLETE | Aggregate readiness, child supervision, structured redacted Gateway/Control/supervisor logs, correlation, private metrics and durable ASK/enrollment status. |
| OBS-002 | COMPLETE | Aggregate readiness, child supervision, structured redacted Gateway/Control/supervisor logs, correlation, private metrics and durable ASK/enrollment status. |
| OBS-003 | COMPLETE | Aggregate readiness, child supervision, structured redacted Gateway/Control/supervisor logs, correlation, private metrics and durable ASK/enrollment status. |
| OBS-004 | COMPLETE | Aggregate readiness, child supervision, structured redacted Gateway/Control/supervisor logs, correlation, private metrics and durable ASK/enrollment status. |
| OBS-005 | COMPLETE | Aggregate readiness, child supervision, structured redacted Gateway/Control/supervisor logs, correlation, private metrics and durable ASK/enrollment status. |
| OBS-006 | COMPLETE | Durable approval and device enrollment state survives restart/backup; no new background fleet job introduced. |
| OBS-007 | COMPLETE | Aggregate readiness, child supervision, structured redacted Gateway/Control/supervisor logs, correlation, private metrics and durable ASK/enrollment status. |
| DKR-001 | COMPLETE | Multi-stage pinned base, JRE/Rust/Python runtime without author engines, UID 65532, fresh runtime keys, OCI labels, read-only/noexec/cap-drop smoke, scans/SBOM and graceful shutdown. |
| DKR-002 | COMPLETE | Multi-stage pinned base, JRE/Rust/Python runtime without author engines, UID 65532, fresh runtime keys, OCI labels, read-only/noexec/cap-drop smoke, scans/SBOM and graceful shutdown. |
| DKR-003 | COMPLETE | Multi-stage pinned base, JRE/Rust/Python runtime without author engines, UID 65532, fresh runtime keys, OCI labels, read-only/noexec/cap-drop smoke, scans/SBOM and graceful shutdown. |
| DKR-004 | COMPLETE | Multi-stage pinned base, JRE/Rust/Python runtime without author engines, UID 65532, fresh runtime keys, OCI labels, read-only/noexec/cap-drop smoke, scans/SBOM and graceful shutdown. |
| DKR-005 | COMPLETE | Multi-stage pinned base, JRE/Rust/Python runtime without author engines, UID 65532, fresh runtime keys, OCI labels, read-only/noexec/cap-drop smoke, scans/SBOM and graceful shutdown. |
| DKR-006 | COMPLETE | Multi-stage pinned base, JRE/Rust/Python runtime without author engines, UID 65532, fresh runtime keys, OCI labels, read-only/noexec/cap-drop smoke, scans/SBOM and graceful shutdown. |
| DKR-007 | COMPLETE | Multi-stage pinned base, JRE/Rust/Python runtime without author engines, UID 65532, fresh runtime keys, OCI labels, read-only/noexec/cap-drop smoke, scans/SBOM and graceful shutdown. |
| DKR-008 | COMPLETE | Multi-stage pinned base, JRE/Rust/Python runtime without author engines, UID 65532, fresh runtime keys, OCI labels, read-only/noexec/cap-drop smoke, scans/SBOM and graceful shutdown. |
| K8S-001 | NOT_APPLICABLE | Docker-only local/non-HA composition; no Quickstart Kubernetes workload, HA or cluster claim. Existing production chart gates pass. |
| K8S-002 | NOT_APPLICABLE | Docker-only local/non-HA composition; no Quickstart Kubernetes workload, HA or cluster claim. Existing production chart gates pass. |
| K8S-003 | NOT_APPLICABLE | Docker-only local/non-HA composition; no Quickstart Kubernetes workload, HA or cluster claim. Existing production chart gates pass. |
| K8S-004 | NOT_APPLICABLE | Docker-only local/non-HA composition; no Quickstart Kubernetes workload, HA or cluster claim. Existing production chart gates pass. |
| K8S-005 | NOT_APPLICABLE | Docker-only local/non-HA composition; no Quickstart Kubernetes workload, HA or cluster claim. Existing production chart gates pass. |
| K8S-006 | NOT_APPLICABLE | Docker-only local/non-HA composition; no Quickstart Kubernetes workload, HA or cluster claim. Existing production chart gates pass. |
| K8S-007 | NOT_APPLICABLE | Docker-only local/non-HA composition; no Quickstart Kubernetes workload, HA or cluster claim. Existing production chart gates pass. |
| K8S-008 | NOT_APPLICABLE | Docker-only local/non-HA composition; no Quickstart Kubernetes workload, HA or cluster claim. Existing production chart gates pass. |
| K8S-009 | NOT_APPLICABLE | Docker-only local/non-HA composition; no Quickstart Kubernetes workload, HA or cluster claim. Existing production chart gates pass. |
| K8S-010 | NOT_APPLICABLE | Docker-only local/non-HA composition; no Quickstart Kubernetes workload, HA or cluster claim. Existing production chart gates pass. |
| K8S-011 | NOT_APPLICABLE | Docker-only local/non-HA composition; no Quickstart Kubernetes workload, HA or cluster claim. Existing production chart gates pass. |
| K8S-012 | NOT_APPLICABLE | Docker-only local/non-HA composition; no Quickstart Kubernetes workload, HA or cluster claim. Existing production chart gates pass. |
| K8S-013 | COMPLETE | Existing helm lint/install-upgrade renders, kubeconform and negative external-secret/TLS tests passed; no new Quickstart workload. |
| K8S-014 | COMPLETE | Existing helm lint/install-upgrade renders, kubeconform and negative external-secret/TLS tests passed; no new Quickstart workload. |
| K8S-015 | NOT_APPLICABLE | Docker-only local/non-HA composition; no Quickstart Kubernetes workload, HA or cluster claim. Existing production chart gates pass. |
| K8S-016 | NOT_APPLICABLE | Docker-only local/non-HA composition; no Quickstart Kubernetes workload, HA or cluster claim. Existing production chart gates pass. |
| REL-001 | COMPLETE | Existing CI-only Maven publication retained; local publication and all artifact-mode service builds passed. |
| REL-002 | COMPLETE | Protected exact-tag GHCR publication, immutable Release evidence/checksums, React-inclusive CycloneDX and OIDC provenance configured; local build/scan verified, remote publication not run. |
| REL-003 | COMPLETE | Existing versioned Helm OCI pipeline retained and chart regression gates passed; Quickstart has no chart. |
| REL-004 | COMPLETE | Protected workflow publishes uniquely named immutable SBOM, operations guide, OCI digest and checksums; native archive release pipeline retained. |
| REL-005 | COMPLETE | Actual anonymous three-OS archives checked against SHA-256; backup manifest verified; CI hashes exact image archive/SBOM and Release evidence. |
| REL-006 | COMPLETE | Protected exact-tag GHCR publication, immutable Release evidence/checksums, React-inclusive CycloneDX and OIDC provenance configured; local build/scan verified, remote publication not run. |
| REL-007 | COMPLETE | Local BuildKit provenance present; protected CI attests exact pushed digest/SBOM with pinned OIDC action. No remote attestation claimed. |
| REL-008 | NOT_APPLICABLE | No container signing key configured. Protected OIDC provenance is configured; policy/permit/device signatures remain mandatory. |
| REL-009 | COMPLETE | Protected exact-tag GHCR publication, immutable Release evidence/checksums, React-inclusive CycloneDX and OIDC provenance configured; local build/scan verified, remote publication not run. |
| REL-010 | COMPLETE | First Quickstart image, unreleased 0.10.0-dev, Gateway v2/policy format 2, layout 1/SQLite 1–2 and inherited native limits documented. |
| REL-011 | COMPLETE | Real schema-1 volume upgrades through the production image; future version/checksum rejection and offline restore tested. No invented older released-image upgrade claim. |
| DOC-001 | COMPLETE | README/component README, canonical API, ADR 012, user and production/debug guides, changelog and compatibility/upgrade/backup guidance. |
| DOC-002 | COMPLETE | README/component README, canonical API, ADR 012, user and production/debug guides, changelog and compatibility/upgrade/backup guidance. |
| DOC-003 | COMPLETE | README/component README, canonical API, ADR 012, user and production/debug guides, changelog and compatibility/upgrade/backup guidance. |
| DOC-004 | COMPLETE | README/component README, canonical API, ADR 012, user and production/debug guides, changelog and compatibility/upgrade/backup guidance. |
| DOC-005 | COMPLETE | README/component README, canonical API, ADR 012, user and production/debug guides, changelog and compatibility/upgrade/backup guidance. |
| DOC-006 | COMPLETE | README/component README, canonical API, ADR 012, user and production/debug guides, changelog and compatibility/upgrade/backup guidance. |
| DOC-007 | COMPLETE | README/component README, canonical API, ADR 012, user and production/debug guides, changelog and compatibility/upgrade/backup guidance. |
| PRF-001 | COMPLETE | In-memory Gateway unchanged; bounded request/output/login/thread/file limits and configurable memory/CPU; actual private Gateway metrics checked. |
| PRF-002 | COMPLETE | In-memory Gateway unchanged; bounded request/output/login/thread/file limits and configurable memory/CPU; actual private Gateway metrics checked. |
| PRF-003 | NOT_APPLICABLE | No Quickstart HA/fleet-scale claim; production Gateway/client interfaces and bounded batch behavior retained. |
| PRF-004 | NOT_APPLICABLE | No Quickstart HA/fleet-scale claim; production Gateway/client interfaces and bounded batch behavior retained. |
| PRF-005 | NOT_APPLICABLE | No Quickstart HA/fleet-scale claim; production Gateway/client interfaces and bounded batch behavior retained. |
| PRF-006 | COMPLETE | In-memory Gateway unchanged; bounded request/output/login/thread/file limits and configurable memory/CPU; actual private Gateway metrics checked. |

All 104 IDs accounted for: **84 COMPLETE, 20 NOT_APPLICABLE, 0 BLOCKED** in this declared Docker scope.
Earlier module certification blockers are not relabeled.

Suggested commit: `feat(quickstart): deliver one-container ToolGate experience`.
