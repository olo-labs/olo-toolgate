# Module 09 verification report

Status: **IMPLEMENTED FOR THE LINUX PATH, NOT DONE**, 2026-10-03.
Native Windows/macOS service-manager and system OCI engine execution across
logout/boot, ARM runtime execution, and the new fleet-enabled Kubernetes path
remain uncertified. Unsupported Batch/CMD and WASM packages fail closed.
The applicable blocked gates below prevent declaring this module DONE.
No Module 10, remote publication, push or host service installation was performed.

Preparation preceded implementation: [coverage plan](09-coverage.md), master
prompt, traceability, governing documents and [definition of done](../08-DEFINITION-OF-DONE.md).
See [ADR 010](../../adr/010-signed-fleet-reconciliation.md) and the
[production/debug usage and configuration guide](../../client/package-deployment.md).
Historical Module 06â€“08 blockers remain explicit.

## Delivered behavior

Canonical additive fleet contracts and versioned OpenAPI define separately signed
release descriptors, organization desired snapshots, scoped artifact grants,
rollout records and client status. Immutable releases identify exact bounded JSON
bytes and digest-pinned OCI runtime/tool registrations. Control never executes
package code, extracts archives or accepts package-supplied download URLs.

PostgreSQL migration V6 externalizes release, desired-generation and rollout
records. Tenant transactions serialize assignments; audit, idempotency, optimistic
revision and desired generations commit together. Releases cannot mutate.
Stable device ordering supports staged canaries and revision-bound advancement.
Rollback assigns a retained release at a higher generation; uninstall removes
registration without deleting a shared image. READY requires current generation,
version and presence; stale, offline, waiting and superseded observations remain
distinct. Caps are 128 releases, 256 rollouts, 256 targets per rollout, 16 packages
per device and bounded cursor pages.

Direct mTLS grants and downloads recheck active enrollment and current assignment.
A fixed external HTTPS mirror has exact digest paths, independent CA/token
references, no redirects/inherited proxy, a total five-second transfer deadline,
two concurrent transfers and a 32 KiB descriptor bound. Release, organization,
IdP, policy and device-CA key domains remain separate.

The protected OS client persists and verifies signed intent before staging.
Downloads, compatibility, sandbox preparation and mandatory confined health probes
must succeed before an atomic activation journal and READY report. New intent
fences old package calls, including storage failures. Restart re-verifies journals
and repeats fresh Control reconciliation and health; offline startup never trusts
saved READY. Existing device freshness, Gateway authorization and ASK permit
binding apply to every managed invocation. Expired desired state blocks execution.
Partial downloads are discarded; failed staging retains the signed last-good
journal for diagnosis and a higher-generation rollback.

The Packages console publishes signed envelopes, assigns canaries, advances
rollouts and displays authoritative status with accessible error/loading/empty
states. It currently displays the first 32 releases/rollouts; API cursors expose
older records. Fleet deployment creates no new mandatory server or infrastructure.
Helm requires external signing/trust and artifact-store configuration and explicit
mirror egress. Product/contracts/chart versions are synchronized at 0.9.0-dev.
Existing release workflows now gate the actual signed fleet E2E.

## Executed commands and evidence

Windows uses PYTHONUTF8=1 and the bundled Python executable. TOOLGATE_DOCKER_TOOLS=1
provides isolated Cargo/PHP/Helm tooling; no missing tool is silently skipped.

| Command / gate | Result and evidence |
|---|---|
| python tools/check.py | PASS: 50 Python contract tests; schema validation/compatibility/drift; Java workspace build and 26 Control tests against PostgreSQL; HTTP/auth/telemetry; local Maven artifact/service dependency proof; Rust workspace tests/clippy; TS/UI build and 44 component tests; PHP 129-model round trips; complete Helm/render/schema and fleet negatives |
| python tools/check.py --scans | PASS: actionlint, npm/pip/Cargo/Gradle vulnerability scan, resolved 795-entry dependency license audit and gitleaks; no HIGH/CRITICAL vulnerability or secret finding |
| cargo test -p olo-toolgate-client --locked | PASS all 22 client tests on final source: genuine signatures/hash/trust separation; stale/wrong identity/expiry/offline; persistent intent/restart and journal-write failure fencing; HotFolder/runtime/IPC/identity regressions. One explicitly separate heavy runtime fixture gate is ignored by Cargo and was exercised in Module 08; real Python sandbox preparation/invocation is exercised by the fleet E2E |
| cargo clippy -p olo-toolgate-client --all-targets --locked -- -D warnings | PASS final client source |
| cargo build --release --target x86_64-unknown-linux-gnu; cargo zigbuild --release --target x86_64-pc-windows-gnu --target x86_64-apple-darwin | PASS three real x64 binaries; cross-builds do not certify native service/runtime behavior |
| python tools/client/package.py for three targets; python tools/client/manifest.py | PASS three 0.9.0-dev archives, checksums, CycloneDX dependency SBOMs/notices and verified public bundle; local generated artifacts are ignored |
| python tools/deployment/check.py --control-image olo-toolgate-control:module09 --binary target/client-release/x86_64-unknown-linux-gnu/release/olo-toolgate-client | PASS final binaries: actual release/desired signatures, mTLS grant/download, atomic READY, interrupted-transfer/service recovery, hash mismatch, incompatible OS/arch/runtime, health timeout, higher-generation rollback, uninstall, restart, aggregation, real browser assignment/accessibility, Control outage/recovery, Gateway outage and revocation; build/client/module09-integration.json |
| python tools/control/container.py --no-build --image olo-toolgate-control:module09 | PASS actual production image: HTTP/auth/tenant/idempotency/audit/telemetry, non-root/read-only runtime and graceful shutdown |
| trivy image --severity HIGH,CRITICAL --exit-code 1 olo-toolgate-control:module09 | PASS OS/JAR vulnerability and secret scan; generated image CycloneDX SBOM merged with embedded UI (374 components) |
| python tools/control/cluster.py --image olo-toolgate-control:module09 | PASS Kind 0.27/Kubernetes 1.32.2 two-replica Control install, authenticated API, PostgreSQL persistence, signed policy and spent approval persistence through upgrade/rollback; build/control/cluster-smoke.json. This generic gate keeps fleet disabled, so new fleet-enabled mTLS/artifact routing remains BLOCKED |
| python tools/control/container.py --image olo-toolgate-control:module09-downloads --client-assets deploy/client-assets/release; python tools/client/downloads.py --image olo-toolgate-control:module09-downloads | PASS image build/smoke and real anonymous home-page browser download of all three 0.9.0-dev archives, exact checksums, filename allowlist and protected private APIs; build/client/public-downloads-proof.json |

Actual PostgreSQL tests cover clean and incremental migrations, immutable release
storage, tenant isolation, duplicate/stale mutations, concurrent rollout creation,
and rollback on audit rejection. Actual HTTPS store tests cover redirect refusal,
wrong size, body overflow, interrupted response, total-body timeout and recovery.
Public genuine signature fixtures are verified by independent Java, Rust and
Python implementations; only their exact JWS fields are exempted from the JWT
secret rule, and private keys/other fields remain scanned.

The initial final fleet rerun timed out on sandbox health while Kubernetes image
loading and scans contended for Docker resources; the client reported FAILED.
The first cluster smoke failed exporting PostgreSQL due to D: capacity; its owned
cluster was removed. Generated ordinary Cargo artifacts were cleaned through
Cargo, retaining source and release binaries. These failures are recorded rather
than treated as successful certification.

## Definition of done assessment

Implementation, canonical contracts, architecture boundaries, closed security
configuration, audit/idempotency, headers, static checks, server operations,
non-root container, Helm resources/probes/security/egress, version/publication
plumbing and production/debug/upgrade documentation are implemented and checked
for the executed Linux path. No production mocks or authorization bypasses exist.

Unit/negative/contract/security/PostgreSQL/HTTP/client/browser tests cover the
executed path. Native Windows/macOS/ARM runtime and logout/boot E2E remain BLOCKED;
cross-builds and portable unit checks are not substitutes. The existing generic
Control cluster smoke cannot certify new fleet-enabled secret/mTLS/artifact
routing. No hot Gateway throughput path changed, so a new benchmark is out of
scope. Protected CI remains responsible for remote signing/publication/provenance.

## Individual requirement matrix

Statuses refer to this module's scope and do not erase historical module blockers.


| ID | Status | Evidence / scope reason |
|---|---|---|
| ARC-001 | COMPLETE | ADR 010; FleetService/FleetStore/FleetCrypto/ArtifactStore ports, immutable descriptor and client state/runtime boundary. |
| ARC-002 | COMPLETE | ADR 010; FleetService/FleetStore/FleetCrypto/ArtifactStore ports, immutable descriptor and client state/runtime boundary. |
| ARC-003 | COMPLETE | ADR 010; FleetService/FleetStore/FleetCrypto/ArtifactStore ports, immutable descriptor and client state/runtime boundary. |
| ARC-004 | COMPLETE | ADR 010; FleetService/FleetStore/FleetCrypto/ArtifactStore ports, immutable descriptor and client state/runtime boundary. |
| ARC-005 | COMPLETE | ADR 010; FleetService/FleetStore/FleetCrypto/ArtifactStore ports, immutable descriptor and client state/runtime boundary. |
| ARC-006 | COMPLETE | ADR 010; FleetService/FleetStore/FleetCrypto/ArtifactStore ports, immutable descriptor and client state/runtime boundary. |
| ARC-007 | COMPLETE | ADR 010; FleetService/FleetStore/FleetCrypto/ArtifactStore ports, immutable descriptor and client state/runtime boundary. |
| CON-001 | COMPLETE | 129 generated models; schema/drift/genuine compatibility/round trips and local Maven artifact/service proof. |
| CON-002 | COMPLETE | 129 generated models; schema/drift/genuine compatibility/round trips and local Maven artifact/service proof. |
| CON-003 | COMPLETE | 129 generated models; schema/drift/genuine compatibility/round trips and local Maven artifact/service proof. |
| CON-004 | COMPLETE | 129 generated models; schema/drift/genuine compatibility/round trips and local Maven artifact/service proof. |
| CON-005 | COMPLETE | 129 generated models; schema/drift/genuine compatibility/round trips and local Maven artifact/service proof. |
| CON-006 | COMPLETE | 129 generated models; schema/drift/genuine compatibility/round trips and local Maven artifact/service proof. |
| SEC-001 | COMPLETE | Exact-byte independent RS256/hash keys, direct scoped mTLS, current assignment/fresh Gateway, bounded HTTPS mirror, custody/audit/idempotency and negative tests. |
| SEC-002 | COMPLETE | Exact-byte independent RS256/hash keys, direct scoped mTLS, current assignment/fresh Gateway, bounded HTTPS mirror, custody/audit/idempotency and negative tests. |
| SEC-003 | COMPLETE | Exact-byte independent RS256/hash keys, direct scoped mTLS, current assignment/fresh Gateway, bounded HTTPS mirror, custody/audit/idempotency and negative tests. |
| SEC-004 | COMPLETE | Exact-byte independent RS256/hash keys, direct scoped mTLS, current assignment/fresh Gateway, bounded HTTPS mirror, custody/audit/idempotency and negative tests. |
| SEC-005 | COMPLETE | Exact-byte independent RS256/hash keys, direct scoped mTLS, current assignment/fresh Gateway, bounded HTTPS mirror, custody/audit/idempotency and negative tests. |
| SEC-006 | BLOCKED | Linux sandbox checked; native Windows/macOS system OCI engine across logout/boot and ARM execution are not certified. |
| SEC-007 | COMPLETE | Exact-byte independent RS256/hash keys, direct scoped mTLS, current assignment/fresh Gateway, bounded HTTPS mirror, custody/audit/idempotency and negative tests. |
| SEC-008 | COMPLETE | Exact-byte independent RS256/hash keys, direct scoped mTLS, current assignment/fresh Gateway, bounded HTTPS mirror, custody/audit/idempotency and negative tests. |
| SEC-009 | COMPLETE | Exact-byte independent RS256/hash keys, direct scoped mTLS, current assignment/fresh Gateway, bounded HTTPS mirror, custody/audit/idempotency and negative tests. |
| SEC-010 | COMPLETE | Exact-byte independent RS256/hash keys, direct scoped mTLS, current assignment/fresh Gateway, bounded HTTPS mirror, custody/audit/idempotency and negative tests. |
| TST-001 | COMPLETE | Contract/unit/PostgreSQL/HTTPS/security/client/browser gates and recovery/failure assertions in the evidence table. |
| TST-002 | COMPLETE | Contract/unit/PostgreSQL/HTTPS/security/client/browser gates and recovery/failure assertions in the evidence table. |
| TST-003 | BLOCKED | Actual Linux real-boundary gates checked; native Windows/macOS/ARM service/runtime integration remains unexecuted. |
| TST-004 | COMPLETE | Contract/unit/PostgreSQL/HTTPS/security/client/browser gates and recovery/failure assertions in the evidence table. |
| TST-005 | COMPLETE | Contract/unit/PostgreSQL/HTTPS/security/client/browser gates and recovery/failure assertions in the evidence table. |
| TST-006 | BLOCKED | Actual signed Linux fleet/browser E2E checked; native logout/boot/runtime E2E remains unexecuted. |
| TST-007 | NOT_APPLICABLE | Fleet reconciliation is outside the Gateway hot path; no new authorization throughput claim. |
| TST-008 | COMPLETE | Contract/unit/PostgreSQL/HTTPS/security/client/browser gates and recovery/failure assertions in the evidence table. |
| QLT-001 | COMPLETE | Header/source/license checks, clippy/type/static checks, existing centralized dependencies/typed errors, bounded ports. |
| QLT-002 | COMPLETE | Header/source/license checks, clippy/type/static checks, existing centralized dependencies/typed errors, bounded ports. |
| QLT-003 | COMPLETE | Header/source/license checks, clippy/type/static checks, existing centralized dependencies/typed errors, bounded ports. |
| QLT-004 | COMPLETE | Header/source/license checks, clippy/type/static checks, existing centralized dependencies/typed errors, bounded ports. |
| QLT-005 | COMPLETE | Header/source/license checks, clippy/type/static checks, existing centralized dependencies/typed errors, bounded ports. |
| QLT-006 | COMPLETE | Header/source/license checks, clippy/type/static checks, existing centralized dependencies/typed errors, bounded ports. |
| API-001 | COMPLETE | Canonical versioned fleet OpenAPI, closed bounded inputs, error/correlation envelope, revision/idempotency and peer certificate routes. |
| API-002 | COMPLETE | Canonical versioned fleet OpenAPI, closed bounded inputs, error/correlation envelope, revision/idempotency and peer certificate routes. |
| API-003 | COMPLETE | Canonical versioned fleet OpenAPI, closed bounded inputs, error/correlation envelope, revision/idempotency and peer certificate routes. |
| API-004 | COMPLETE | Canonical versioned fleet OpenAPI, closed bounded inputs, error/correlation envelope, revision/idempotency and peer certificate routes. |
| API-005 | COMPLETE | Canonical versioned fleet OpenAPI, closed bounded inputs, error/correlation envelope, revision/idempotency and peer certificate routes. |
| API-006 | COMPLETE | Canonical versioned fleet OpenAPI, closed bounded inputs, error/correlation envelope, revision/idempotency and peer certificate routes. |
| API-007 | COMPLETE | Canonical versioned fleet OpenAPI, closed bounded inputs, error/correlation envelope, revision/idempotency and peer certificate routes. |
| DAT-001 | COMPLETE | Flyway V6 clean/incremental install, immutable release trigger, tenant transaction/race/audit rollback tests and durable external state. |
| DAT-002 | COMPLETE | Flyway V6 clean/incremental install, immutable release trigger, tenant transaction/race/audit rollback tests and durable external state. |
| DAT-003 | COMPLETE | Flyway V6 clean/incremental install, immutable release trigger, tenant transaction/race/audit rollback tests and durable external state. |
| DAT-004 | COMPLETE | Flyway V6 clean/incremental install, immutable release trigger, tenant transaction/race/audit rollback tests and durable external state. |
| DAT-005 | COMPLETE | Flyway V6 clean/incremental install, immutable release trigger, tenant transaction/race/audit rollback tests and durable external state. |
| OBS-001 | COMPLETE | Existing health/readiness/logs/OTLP, bounded fleet metrics, durable authoritative statuses and redacted client reconciliation. |
| OBS-002 | COMPLETE | Existing health/readiness/logs/OTLP, bounded fleet metrics, durable authoritative statuses and redacted client reconciliation. |
| OBS-003 | COMPLETE | Existing health/readiness/logs/OTLP, bounded fleet metrics, durable authoritative statuses and redacted client reconciliation. |
| OBS-004 | COMPLETE | Existing health/readiness/logs/OTLP, bounded fleet metrics, durable authoritative statuses and redacted client reconciliation. |
| OBS-005 | COMPLETE | Existing health/readiness/logs/OTLP, bounded fleet metrics, durable authoritative statuses and redacted client reconciliation. |
| OBS-006 | COMPLETE | Existing health/readiness/logs/OTLP, bounded fleet metrics, durable authoritative statuses and redacted client reconciliation. |
| OBS-007 | COMPLETE | Existing health/readiness/logs/OTLP, bounded fleet metrics, durable authoritative statuses and redacted client reconciliation. |
| DKR-001 | COMPLETE | Actual production multi-stage non-root/read-only smoke, graceful shutdown, clean image scan and merged SBOM; existing protected publication. |
| DKR-002 | COMPLETE | Actual production multi-stage non-root/read-only smoke, graceful shutdown, clean image scan and merged SBOM; existing protected publication. |
| DKR-003 | COMPLETE | Actual production multi-stage non-root/read-only smoke, graceful shutdown, clean image scan and merged SBOM; existing protected publication. |
| DKR-004 | COMPLETE | Actual production multi-stage non-root/read-only smoke, graceful shutdown, clean image scan and merged SBOM; existing protected publication. |
| DKR-005 | COMPLETE | Actual production multi-stage non-root/read-only smoke, graceful shutdown, clean image scan and merged SBOM; existing protected publication. |
| DKR-006 | COMPLETE | Actual production multi-stage non-root/read-only smoke, graceful shutdown, clean image scan and merged SBOM; existing protected publication. |
| DKR-007 | COMPLETE | Actual production multi-stage non-root/read-only smoke, graceful shutdown, clean image scan and merged SBOM; existing protected publication. |
| DKR-008 | COMPLETE | Actual production multi-stage non-root/read-only smoke, graceful shutdown, clean image scan and merged SBOM; existing protected publication. |
| K8S-001 | COMPLETE | Control chart resources/contexts/SA/probes/PDB/HPA/topology/ingress/metrics retained; fleet external secrets/narrow mirror egress; lint/render/schema negatives. |
| K8S-002 | COMPLETE | Control chart resources/contexts/SA/probes/PDB/HPA/topology/ingress/metrics retained; fleet external secrets/narrow mirror egress; lint/render/schema negatives. |
| K8S-003 | COMPLETE | Control chart resources/contexts/SA/probes/PDB/HPA/topology/ingress/metrics retained; fleet external secrets/narrow mirror egress; lint/render/schema negatives. |
| K8S-004 | COMPLETE | Control chart resources/contexts/SA/probes/PDB/HPA/topology/ingress/metrics retained; fleet external secrets/narrow mirror egress; lint/render/schema negatives. |
| K8S-005 | COMPLETE | Control chart resources/contexts/SA/probes/PDB/HPA/topology/ingress/metrics retained; fleet external secrets/narrow mirror egress; lint/render/schema negatives. |
| K8S-006 | COMPLETE | Control chart resources/contexts/SA/probes/PDB/HPA/topology/ingress/metrics retained; fleet external secrets/narrow mirror egress; lint/render/schema negatives. |
| K8S-007 | COMPLETE | Control chart resources/contexts/SA/probes/PDB/HPA/topology/ingress/metrics retained; fleet external secrets/narrow mirror egress; lint/render/schema negatives. |
| K8S-008 | COMPLETE | Control chart resources/contexts/SA/probes/PDB/HPA/topology/ingress/metrics retained; fleet external secrets/narrow mirror egress; lint/render/schema negatives. |
| K8S-009 | COMPLETE | Control chart resources/contexts/SA/probes/PDB/HPA/topology/ingress/metrics retained; fleet external secrets/narrow mirror egress; lint/render/schema negatives. |
| K8S-010 | COMPLETE | Control chart resources/contexts/SA/probes/PDB/HPA/topology/ingress/metrics retained; fleet external secrets/narrow mirror egress; lint/render/schema negatives. |
| K8S-011 | COMPLETE | Control chart resources/contexts/SA/probes/PDB/HPA/topology/ingress/metrics retained; fleet external secrets/narrow mirror egress; lint/render/schema negatives. |
| K8S-012 | COMPLETE | Control chart resources/contexts/SA/probes/PDB/HPA/topology/ingress/metrics retained; fleet external secrets/narrow mirror egress; lint/render/schema negatives. |
| K8S-013 | COMPLETE | Control chart resources/contexts/SA/probes/PDB/HPA/topology/ingress/metrics retained; fleet external secrets/narrow mirror egress; lint/render/schema negatives. |
| K8S-014 | COMPLETE | Control chart resources/contexts/SA/probes/PDB/HPA/topology/ingress/metrics retained; fleet external secrets/narrow mirror egress; lint/render/schema negatives. |
| K8S-015 | BLOCKED | New fleet-enabled external key/mTLS/artifact routing has not been exercised in a local cluster; existing generic Control smoke is separate evidence. |
| K8S-016 | COMPLETE | Control chart resources/contexts/SA/probes/PDB/HPA/topology/ingress/metrics retained; fleet external secrets/narrow mirror egress; lint/render/schema negatives. |
| REL-001 | COMPLETE | 0.9.0-dev synchronized versions, Maven proof, three checked x64 archives/checksums/SBOM/notices, protected CI publication/signing/attestation and upgrade docs. |
| REL-002 | COMPLETE | 0.9.0-dev synchronized versions, Maven proof, three checked x64 archives/checksums/SBOM/notices, protected CI publication/signing/attestation and upgrade docs. |
| REL-003 | COMPLETE | 0.9.0-dev synchronized versions, Maven proof, three checked x64 archives/checksums/SBOM/notices, protected CI publication/signing/attestation and upgrade docs. |
| REL-004 | COMPLETE | 0.9.0-dev synchronized versions, Maven proof, three checked x64 archives/checksums/SBOM/notices, protected CI publication/signing/attestation and upgrade docs. |
| REL-005 | COMPLETE | 0.9.0-dev synchronized versions, Maven proof, three checked x64 archives/checksums/SBOM/notices, protected CI publication/signing/attestation and upgrade docs. |
| REL-006 | COMPLETE | 0.9.0-dev synchronized versions, Maven proof, three checked x64 archives/checksums/SBOM/notices, protected CI publication/signing/attestation and upgrade docs. |
| REL-007 | COMPLETE | 0.9.0-dev synchronized versions, Maven proof, three checked x64 archives/checksums/SBOM/notices, protected CI publication/signing/attestation and upgrade docs. |
| REL-008 | COMPLETE | 0.9.0-dev synchronized versions, Maven proof, three checked x64 archives/checksums/SBOM/notices, protected CI publication/signing/attestation and upgrade docs. |
| REL-009 | COMPLETE | 0.9.0-dev synchronized versions, Maven proof, three checked x64 archives/checksums/SBOM/notices, protected CI publication/signing/attestation and upgrade docs. |
| REL-010 | BLOCKED | Compatibility matrix records unsupported Batch/WASM and uncertified native Windows/macOS/ARM runtime/service execution; no release readiness claim. |
| REL-011 | COMPLETE | 0.9.0-dev synchronized versions, Maven proof, three checked x64 archives/checksums/SBOM/notices, protected CI publication/signing/attestation and upgrade docs. |
| DOC-001 | COMPLETE | READMEs, ADR 010, API, architecture/changelog and production/debug configuration/recovery/troubleshooting runbook. |
| DOC-002 | COMPLETE | READMEs, ADR 010, API, architecture/changelog and production/debug configuration/recovery/troubleshooting runbook. |
| DOC-003 | COMPLETE | READMEs, ADR 010, API, architecture/changelog and production/debug configuration/recovery/troubleshooting runbook. |
| DOC-004 | COMPLETE | READMEs, ADR 010, API, architecture/changelog and production/debug configuration/recovery/troubleshooting runbook. |
| DOC-005 | COMPLETE | READMEs, ADR 010, API, architecture/changelog and production/debug configuration/recovery/troubleshooting runbook. |
| DOC-006 | COMPLETE | READMEs, ADR 010, API, architecture/changelog and production/debug configuration/recovery/troubleshooting runbook. |
| DOC-007 | COMPLETE | READMEs, ADR 010, API, architecture/changelog and production/debug configuration/recovery/troubleshooting runbook. |
| PRF-001 | COMPLETE | No deployment DB/work in Gateway; capped tenant records/batches/desired bytes/packages/runtime/tools/pages, bounded mirror concurrency/transfer time. |
| PRF-002 | COMPLETE | No deployment DB/work in Gateway; capped tenant records/batches/desired bytes/packages/runtime/tools/pages, bounded mirror concurrency/transfer time. |
| PRF-003 | COMPLETE | No deployment DB/work in Gateway; capped tenant records/batches/desired bytes/packages/runtime/tools/pages, bounded mirror concurrency/transfer time. |
| PRF-004 | COMPLETE | No deployment DB/work in Gateway; capped tenant records/batches/desired bytes/packages/runtime/tools/pages, bounded mirror concurrency/transfer time. |
| PRF-005 | NOT_APPLICABLE | No new throughput claim or Gateway hot-path change; existing methodology retained. |
| PRF-006 | COMPLETE | No deployment DB/work in Gateway; capped tenant records/batches/desired bytes/packages/runtime/tools/pages, bounded mirror concurrency/transfer time. |

104 IDs: 97 COMPLETE, 5 BLOCKED and 2 NOT_APPLICABLE.

Suggested commit: `feat(deployment): add managed package lifecycle`.
