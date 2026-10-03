# Module 07 verification report

Status: **IMPLEMENTED, NOT DONE**, 2026-10-03. The local verification results below
are recorded separately from native CI that has not executed in this session.
There are 99 COMPLETE, two BLOCKED and three NOT_APPLICABLE requirement IDs.
A blocked gate prevents claiming completion under the definition of done.
Implementation and a reviewed local commit are authorized; no push or remote
publication was performed. No module beyond 07 was started. The historical
Module 06 report is not retroactively marked complete.

Preparation preceded coding: [coverage plan](07-coverage.md), master prompt,
requirements traceability and definition of done were read. See
[ADR 008](../../adr/008-hotfolder-builtins.md) and
[the installation and operations guide](../../client/hotfolder.md).

## Delivered behavior

Eighteen fixed canonical tools run through OS-authenticated IPC in the protected
system service. Relative capability paths reject traversal, symlinks/reparse
points, hardlinks and unsafe aliases; extension, size, depth and inventory bounds
apply. Writes replace atomically and append is serialized. Copy/move authorize
both endpoints and never overwrite destinations. Delete/arbitrary execution are
absent. Pure utilities are bounded, and Brave search is disabled without a
protected provider credential. Events are a bounded transient mutation poll with
epoch/reset and lossless pagination, not general external filesystem watching.

Each execution needs matching enrolled device identity, current Control readiness
and fresh online Gateway ALLOW. ASK grants require exact-operation online permit
consumption. Outage/revocation/expiry blocks with no offline permission cache.
Windows installs an automatic LocalSystem service, macOS a system LaunchDaemon,
and Linux a root multi-user systemd service. They continue across user logout and
screen lock; a powered-off/asleep host cannot execute work. Initial browser
enrollment remains a human step. No service was installed on the user's host.

Control's anonymous home page offers real Windows/macOS/Linux native archives
when the image contains the verified immutable bundle. Administrative APIs stay
protected. Archives contain install/uninstall CLI, service definitions, checksum,
SBOM and original dependency notices. Public transfers are allowlisted, hash-
verified, bounded and limited to two active streams per replica. Default builds
without assets explicitly show unavailable downloads. No fake binary is shipped.

Prerequisite fixes used by this path include post-response enrollment timestamps,
SemVer minimum comparison, unambiguous endpoint routes, build-time TLS client-auth
REQUEST and Windows TrustedInstaller/inherit-only ACL handling. Windows child
custody inherits explicitly protected ACLs.
Windows installation also rejects service paths writable by the installer's
ordinary caller SID; privileged binary/configuration custody must remain OS/admin-only.
File-level Windows ACL checks reject untrusted writers even when an administrator
has changed an individual file's inherited custody. Existing protocol 1 stays frozen;
protocol 2 and download models are additive. Product/contracts/chart are 0.7.0-dev;
there is no new database migration beyond existing V5.

## Verification commands and evidence

Local logs are ignored build evidence, not committed secrets or generated binaries.

| Command / gate | Result and local evidence |
|---|---|
| `python tools/check.py` (`make check` equivalent, Docker Rust/PHP/Helm tooling) | PASS: 47 Python contract tests, Gradle workspace build, local Maven publication and all published-artifact service builds, Rust workspace tests/clippy, 97-model language round trips, 41 UI tests, five real browser tests and strict complete Helm matrix. `.dev/module07-check.log` |
| Final `cargo test -p olo-toolgate-client --locked`, clippy, native/cross release build | PASS: one Gateway grant unit test, eight built-in tests, eight foundation tests on Linux; genuine Linux ELF, Windows PE and macOS Mach-O x64 release executables. `.dev/module07-client-security-final.log` |
| Execute Windows cross-built test executables on the Windows host | PASS: ten built-in, seven foundation and one grant test; includes inherited ACL custody, hardlinks, DPAPI persistence, unauthorized IPC and event pagination. `.dev/module07-windows-security-final.log` |
| `gradlew :control-plane:test --tests io.ololabs.toolgate.control.ClientArtifactsTest` | PASS: four artifact allowlist, corruption, unavailable and bounded/failed-stream tests. `.dev/module07-artifact-tests.log` |
| `python tools/client/integration.py` | PASS: real PostgreSQL/production direct-TLS Control enrollment, human API confirmation, device mTLS check-in, persistent service restart, HTTPS Gateway grants, source/destination denial, OS peer rejection, Gateway outage and device revocation. `build/client/module07-integration.json`, `.dev/module07-integration-bounded.log` |
| `python tools/client/package.py` for three x64 targets; manifest and repeated package build | PASS: executable CPU/header checks, complete original dependency notices, CycloneDX SBOMs and SHA-256 sidecars; repeated Linux archive is byte-identical. `.dev/module07-packages-final-proof.log`, `deploy/client-assets/release/manifest.json` |
| `python tools/control/container.py --image olo-toolgate-control:module07-downloads --client-assets deploy/client-assets/release` | PASS: bundled production image, non-root/read-only execution, real PostgreSQL HTTP/auth/audit/metrics, redacted logs and graceful shutdown. `.dev/module07-container-final-proof.log`, `build/control/container-smoke.json` |
| `python tools/client/downloads.py` | PASS: all three real native archive bytes/hash downloads anonymously, private API remains 401, filename negatives, desktop/mobile browser accessibility. `.dev/module07-downloads-final-proof.log`, `build/client/public-downloads-proof.json` |
| `python tools/control/cluster.py --image olo-toolgate-control:module07-downloads --client-downloads` | PASS: owned Kind 1.32.2 cluster, two ready replicas, anonymous three-platform manifest on install/upgrade/rollback, persistent signed bundles/approval consumption. `.dev/module07-cluster-final.log`, `build/control/cluster-smoke.json` |
| `python tools/check.py --scans` and final actionlint/header/drift checks | PASS: actionlint, npm/pip vulnerability audit, 795-entry dependency license audit, Gitleaks/negative secret fixture proof and source vulnerability/license scan. `.dev/module07-scans-final.log` |
| Trivy 0.61.1 production image HIGH/CRITICAL scans and Control CycloneDX | PASS: zero HIGH/CRITICAL findings in Control and Gateway; no image secrets. `build/client/control-image-scan.json`, `gateway-image-scan.json`, `control-sbom.cdx.json` |

The Kind proof uses the verified Control image before the final client worker
limit adjustment; server/chart behavior is identical. Final native assets and
their new manifest are rechecked through the production download image. Native
CI service-manager smoke and remote tagged release/signing are not claimed as
executed local commands. Linux end-to-end runs detached with no interactive TTY;
the Windows/macOS/Linux service-manager installation runs remain external gates.

## Definition of done and limitations

Implementation, architecture boundaries, shared contracts, fixed tools, online
authorization, protected configuration and operational documentation are delivered.
Local unit/negative/contract/integration/security/browser checks are required, as
are static scans, publication proof, container and Helm/Kind checks. Native service
CI performs real install/health/system-manager/uninstall on all six OS/CPU runners,
but those remote runs have not occurred here. This report does not treat a
cross-compiled Mach-O header as a macOS installation test or claim an actual
interactive logout was performed. Detached Linux service execution proves no TTY
dependency; service definitions and CI target OS lifecycle independently.

Signing identities are external and currently unconfigured; development binaries
are unsigned, and macOS notarization is not implemented. Protected CI publication
requires the exact VERSION tag, all native/package/Control/compatibility gates,
and creates provenance for the native assets. No credentials or private keys are
embedded. Operators must apply enterprise signing/notarization requirements.

Suggested commit: `feat(client): add HotFolder and safe built-in tools`.

## Individual requirement coverage

| Requirement | Status | Evidence or scope reason |
|---|---|---|
| ARC-001 | COMPLETE | ADR 008; executor/filesystem/Gateway ports; protected system service; immutable Control assets. |
| ARC-002 | COMPLETE | ADR 008; executor/filesystem/Gateway ports; protected system service; immutable Control assets. |
| ARC-003 | COMPLETE | ADR 008; executor/filesystem/Gateway ports; protected system service; immutable Control assets. |
| ARC-004 | COMPLETE | ADR 008; executor/filesystem/Gateway ports; protected system service; immutable Control assets. |
| ARC-005 | COMPLETE | ADR 008; executor/filesystem/Gateway ports; protected system service; immutable Control assets. |
| ARC-006 | COMPLETE | Explicit existing enrollment state and readiness transitions; bounded epoch/cursor event lifecycle. No arbitrary job engine. |
| ARC-007 | COMPLETE | ADR 008; executor/filesystem/Gateway ports; protected system service; immutable Control assets. |
| CON-001 | COMPLETE | Canonical builtins.schema.json/catalog; 97-model round trips; drift/version checks; local published Java service dependency proof. |
| CON-002 | COMPLETE | Canonical builtins.schema.json/catalog; 97-model round trips; drift/version checks; local published Java service dependency proof. |
| CON-003 | COMPLETE | Canonical builtins.schema.json/catalog; 97-model round trips; drift/version checks; local published Java service dependency proof. |
| CON-004 | COMPLETE | Canonical builtins.schema.json/catalog; 97-model round trips; drift/version checks; local published Java service dependency proof. |
| CON-005 | COMPLETE | Canonical builtins.schema.json/catalog; 97-model round trips; drift/version checks; local published Java service dependency proof. |
| CON-006 | COMPLETE | Canonical builtins.schema.json/catalog; 97-model round trips; drift/version checks; local published Java service dependency proof. |
| SEC-001 | COMPLETE | Capability no-follow access; owner/ACL custody; traversal/link/limit tests; real mTLS/Gateway outage/revocation E2E; closed fixed tools. |
| SEC-002 | COMPLETE | Capability no-follow access; owner/ACL custody; traversal/link/limit tests; real mTLS/Gateway outage/revocation E2E; closed fixed tools. |
| SEC-003 | COMPLETE | Signed discovery/device validation and fresh HTTPS Gateway outcome/ASK consumption; SHA-256 immutable download verification and package header validation. |
| SEC-004 | COMPLETE | Public client download is distribution only; local enrollment approval remains separate. No marketplace trust shortcut. |
| SEC-005 | COMPLETE | Install/enrollment never grants tool permission; every execution requires fresh Gateway ALLOW. |
| SEC-006 | NOT_APPLICABLE | No untrusted executable code; fixed built-ins only. General local runtime belongs to a later module. |
| SEC-007 | COMPLETE | Capability no-follow access; owner/ACL custody; traversal/link/limit tests; real mTLS/Gateway outage/revocation E2E; closed fixed tools. |
| SEC-008 | COMPLETE | Device CA, IdP, Gateway runtime credential, policy/permit and optional provider credentials remain distinct external identities. |
| SEC-009 | COMPLETE | Gateway authorization audit; sanitized builtin request/outcome/digest logs; existing transactional Control enrollment/revocation audit. |
| SEC-010 | COMPLETE | Existing durable enrollment/check-in/idempotency and single-use ASK consumption; copy/move cannot overwrite. File mutations have documented no automatic retry after lost response. |
| TST-001 | COMPLETE | Client unit/negative tests, canonical corpus, real Control TLS/Gateway integration and anonymous browser download E2E. |
| TST-002 | COMPLETE | Client unit/negative tests, canonical corpus, real Control TLS/Gateway integration and anonymous browser download E2E. |
| TST-003 | BLOCKED | Real Linux cross-component and Windows filesystem/identity tests pass; native Windows SCM/macOS LaunchDaemon/Linux systemd installation/boot smoke is configured in six-target CI but has not run in this local session. |
| TST-004 | COMPLETE | Client unit/negative tests, canonical corpus, real Control TLS/Gateway integration and anonymous browser download E2E. |
| TST-005 | COMPLETE | Client unit/negative tests, canonical corpus, real Control TLS/Gateway integration and anonymous browser download E2E. |
| TST-006 | BLOCKED | Real anonymous download browser E2E and detached Linux service enrollment/tool E2E pass; six native service-manager CI jobs still need successful remote execution before claiming full platform completion. |
| TST-007 | NOT_APPLICABLE | No new performance-sensitive Gateway evaluator path; bounded local utilities/filesystem operations. Existing Gateway benchmarks unchanged. |
| TST-008 | COMPLETE | Client unit/negative tests, canonical corpus, real Control TLS/Gateway integration and anonymous browser download E2E. |
| QLT-001 | COMPLETE | Copyright/SPDX gate; clippy -D warnings; closed shared models; centralized cap/semver dependencies; documented ports. |
| QLT-002 | COMPLETE | Copyright/SPDX gate; clippy -D warnings; closed shared models; centralized cap/semver dependencies; documented ports. |
| QLT-003 | COMPLETE | Copyright/SPDX gate; clippy -D warnings; closed shared models; centralized cap/semver dependencies; documented ports. |
| QLT-004 | COMPLETE | Copyright/SPDX gate; clippy -D warnings; closed shared models; centralized cap/semver dependencies; documented ports. |
| QLT-005 | COMPLETE | Copyright/SPDX gate; clippy -D warnings; closed shared models; centralized cap/semver dependencies; documented ports. |
| QLT-006 | COMPLETE | Copyright/SPDX gate; clippy -D warnings; closed shared models; centralized cap/semver dependencies; documented ports. |
| API-001 | COMPLETE | Protocol 2 and public v1 OpenAPI; canonical schema/duplicate-key validation; stable redacted errors; traceparent and response-ID checks. |
| API-002 | COMPLETE | Protocol 2 and public v1 OpenAPI; canonical schema/duplicate-key validation; stable redacted errors; traceparent and response-ID checks. |
| API-003 | COMPLETE | Protocol 2 and public v1 OpenAPI; canonical schema/duplicate-key validation; stable redacted errors; traceparent and response-ID checks. |
| API-004 | COMPLETE | Protocol 2 and public v1 OpenAPI; canonical schema/duplicate-key validation; stable redacted errors; traceparent and response-ID checks. |
| API-005 | COMPLETE | Protocol 2 and public v1 OpenAPI; canonical schema/duplicate-key validation; stable redacted errors; traceparent and response-ID checks. |
| API-006 | COMPLETE | Existing Control Idempotency-Key/optimistic revocation revision preserved; local file calls explicitly document no replay-safe automatic mutation retry. |
| API-007 | COMPLETE | Protocol 2 and public v1 OpenAPI; canonical schema/duplicate-key validation; stable redacted errors; traceparent and response-ID checks. |
| DAT-001 | COMPLETE | Existing Flyway V5/transactional endpoint state; no new migration; external PostgreSQL and private OS state; serialized atomic writes. |
| DAT-002 | COMPLETE | Existing Flyway V5/transactional endpoint state; no new migration; external PostgreSQL and private OS state; serialized atomic writes. |
| DAT-003 | COMPLETE | Existing Flyway V5/transactional endpoint state; no new migration; external PostgreSQL and private OS state; serialized atomic writes. |
| DAT-004 | COMPLETE | Existing Flyway V5/transactional endpoint state; no new migration; external PostgreSQL and private OS state; serialized atomic writes. |
| DAT-005 | COMPLETE | Existing Flyway V5/transactional endpoint state; no new migration; external PostgreSQL and private OS state; serialized atomic writes. |
| OBS-001 | COMPLETE | Client health/readiness/check-in counters; sanitized spans/logs; Gateway audit and Control HTTP metrics; durable endpoint report journal. |
| OBS-002 | COMPLETE | Client health/readiness/check-in counters; sanitized spans/logs; Gateway audit and Control HTTP metrics; durable endpoint report journal. |
| OBS-003 | COMPLETE | Client health/readiness/check-in counters; sanitized spans/logs; Gateway audit and Control HTTP metrics; durable endpoint report journal. |
| OBS-004 | COMPLETE | Existing client check-in health counters, Gateway metrics, and Control HTTP/endpoint metrics; no public local metrics listener. |
| OBS-005 | COMPLETE | OS IPC request span -> Gateway traceparent; canonical decision ID must match server response header. |
| OBS-006 | COMPLETE | Existing durable pending check-in and explicit enrolled/revoked state; event ring is documented transient polling. No new async job queue. |
| OBS-007 | COMPLETE | Client health/readiness/check-in counters; sanitized spans/logs; Gateway audit and Control HTTP metrics; durable endpoint report journal. |
| DKR-001 | COMPLETE | Control/Gateway multi-stage image builds; non-root/read-only Control; immutable assets; external secrets; image scan/SBOM/shutdown gates. |
| DKR-002 | COMPLETE | Control/Gateway multi-stage image builds; non-root/read-only Control; immutable assets; external secrets; image scan/SBOM/shutdown gates. |
| DKR-003 | COMPLETE | Control/Gateway multi-stage image builds; non-root/read-only Control; immutable assets; external secrets; image scan/SBOM/shutdown gates. |
| DKR-004 | COMPLETE | Control/Gateway multi-stage image builds; non-root/read-only Control; immutable assets; external secrets; image scan/SBOM/shutdown gates. |
| DKR-005 | COMPLETE | Control/Gateway multi-stage image builds; non-root/read-only Control; immutable assets; external secrets; image scan/SBOM/shutdown gates. |
| DKR-006 | COMPLETE | Control/Gateway multi-stage image builds; non-root/read-only Control; immutable assets; external secrets; image scan/SBOM/shutdown gates. |
| DKR-007 | COMPLETE | Control/Gateway multi-stage image builds; non-root/read-only Control; immutable assets; external secrets; image scan/SBOM/shutdown gates. |
| DKR-008 | COMPLETE | Control/Gateway multi-stage image builds; non-root/read-only Control; immutable assets; external secrets; image scan/SBOM/shutdown gates. |
| K8S-001 | COMPLETE | Existing configurable Control service/security/probes/HA/NetworkPolicy; clientDownloads values; lint/strict render and owned Kind upgrade/rollback. |
| K8S-002 | COMPLETE | Existing configurable Control service/security/probes/HA/NetworkPolicy; clientDownloads values; lint/strict render and owned Kind upgrade/rollback. |
| K8S-003 | COMPLETE | Existing configurable Control service/security/probes/HA/NetworkPolicy; clientDownloads values; lint/strict render and owned Kind upgrade/rollback. |
| K8S-004 | COMPLETE | Existing configurable Control service/security/probes/HA/NetworkPolicy; clientDownloads values; lint/strict render and owned Kind upgrade/rollback. |
| K8S-005 | COMPLETE | Existing configurable Control service/security/probes/HA/NetworkPolicy; clientDownloads values; lint/strict render and owned Kind upgrade/rollback. |
| K8S-006 | COMPLETE | Existing configurable Control service/security/probes/HA/NetworkPolicy; clientDownloads values; lint/strict render and owned Kind upgrade/rollback. |
| K8S-007 | COMPLETE | Existing configurable Control service/security/probes/HA/NetworkPolicy; clientDownloads values; lint/strict render and owned Kind upgrade/rollback. |
| K8S-008 | COMPLETE | Existing configurable Control service/security/probes/HA/NetworkPolicy; clientDownloads values; lint/strict render and owned Kind upgrade/rollback. |
| K8S-009 | COMPLETE | Existing configurable Control service/security/probes/HA/NetworkPolicy; clientDownloads values; lint/strict render and owned Kind upgrade/rollback. |
| K8S-010 | COMPLETE | Existing configurable Control service/security/probes/HA/NetworkPolicy; clientDownloads values; lint/strict render and owned Kind upgrade/rollback. |
| K8S-011 | COMPLETE | Existing configurable Control service/security/probes/HA/NetworkPolicy; clientDownloads values; lint/strict render and owned Kind upgrade/rollback. |
| K8S-012 | COMPLETE | Existing configurable Control service/security/probes/HA/NetworkPolicy; clientDownloads values; lint/strict render and owned Kind upgrade/rollback. |
| K8S-013 | COMPLETE | Existing configurable Control service/security/probes/HA/NetworkPolicy; clientDownloads values; lint/strict render and owned Kind upgrade/rollback. |
| K8S-014 | COMPLETE | Existing configurable Control service/security/probes/HA/NetworkPolicy; clientDownloads values; lint/strict render and owned Kind upgrade/rollback. |
| K8S-015 | COMPLETE | Existing configurable Control service/security/probes/HA/NetworkPolicy; clientDownloads values; lint/strict render and owned Kind upgrade/rollback. |
| K8S-016 | COMPLETE | Existing configurable Control service/security/probes/HA/NetworkPolicy; clientDownloads values; lint/strict render and owned Kind upgrade/rollback. |
| REL-001 | COMPLETE | Synchronized 0.7.0-dev; native CI packages/checksums/SBOM/notices; protected tagged client publication/provenance and external signing hooks. |
| REL-002 | COMPLETE | Synchronized 0.7.0-dev; native CI packages/checksums/SBOM/notices; protected tagged client publication/provenance and external signing hooks. |
| REL-003 | COMPLETE | Synchronized 0.7.0-dev; native CI packages/checksums/SBOM/notices; protected tagged client publication/provenance and external signing hooks. |
| REL-004 | COMPLETE | Synchronized 0.7.0-dev; native CI packages/checksums/SBOM/notices; protected tagged client publication/provenance and external signing hooks. |
| REL-005 | COMPLETE | Synchronized 0.7.0-dev; native CI packages/checksums/SBOM/notices; protected tagged client publication/provenance and external signing hooks. |
| REL-006 | COMPLETE | Synchronized 0.7.0-dev; native CI packages/checksums/SBOM/notices; protected tagged client publication/provenance and external signing hooks. |
| REL-007 | COMPLETE | Synchronized 0.7.0-dev; native CI packages/checksums/SBOM/notices; protected tagged client publication/provenance and external signing hooks. |
| REL-008 | COMPLETE | Protected sign.py hooks use external certificate store/keychain/KMS refs. Signing identities are unconfigured; development artifacts are explicitly unsigned, notarization unavailable. |
| REL-009 | COMPLETE | Synchronized 0.7.0-dev; native CI packages/checksums/SBOM/notices; protected tagged client publication/provenance and external signing hooks. |
| REL-010 | COMPLETE | Synchronized 0.7.0-dev; native CI packages/checksums/SBOM/notices; protected tagged client publication/provenance and external signing hooks. |
| REL-011 | COMPLETE | Synchronized 0.7.0-dev; native CI packages/checksums/SBOM/notices; protected tagged client publication/provenance and external signing hooks. |
| DOC-001 | COMPLETE | Client README and hotfolder.md config/install/security/recovery/upgrade; ADR 008; architecture/security/OpenAPI/compatibility/changelog. |
| DOC-002 | COMPLETE | Client README and hotfolder.md config/install/security/recovery/upgrade; ADR 008; architecture/security/OpenAPI/compatibility/changelog. |
| DOC-003 | COMPLETE | Client README and hotfolder.md config/install/security/recovery/upgrade; ADR 008; architecture/security/OpenAPI/compatibility/changelog. |
| DOC-004 | COMPLETE | Client README and hotfolder.md config/install/security/recovery/upgrade; ADR 008; architecture/security/OpenAPI/compatibility/changelog. |
| DOC-005 | COMPLETE | Client README and hotfolder.md config/install/security/recovery/upgrade; ADR 008; architecture/security/OpenAPI/compatibility/changelog. |
| DOC-006 | COMPLETE | Client README and hotfolder.md config/install/security/recovery/upgrade; ADR 008; architecture/security/OpenAPI/compatibility/changelog. |
| DOC-007 | COMPLETE | Client README and hotfolder.md config/install/security/recovery/upgrade; ADR 008; architecture/security/OpenAPI/compatibility/changelog. |
| PRF-001 | COMPLETE | In-memory normal Gateway authorization; 64KiB files, bounded inventory/IPC/provider/events, serialized service and two download slots per replica. |
| PRF-002 | COMPLETE | In-memory normal Gateway authorization; 64KiB files, bounded inventory/IPC/provider/events, serialized service and two download slots per replica. |
| PRF-003 | COMPLETE | In-memory normal Gateway authorization; 64KiB files, bounded inventory/IPC/provider/events, serialized service and two download slots per replica. |
| PRF-004 | COMPLETE | Existing bounded ordered heartbeat reporting and reconnect journal; no per-call device database query in Gateway static evaluation. |
| PRF-005 | NOT_APPLICABLE | No new performance benchmark target; existing Gateway methodology retained. |
| PRF-006 | COMPLETE | In-memory normal Gateway authorization; 64KiB files, bounded inventory/IPC/provider/events, serialized service and two download slots per replica. |
