# Module 08 verification report

Status: **IMPLEMENTED IN PART, NOT DONE**, 2026-10-03. Batch/CMD remains
UNSUPPORTED until a secure Windows sandbox adapter exists. Windows/macOS system
engine lifetime across logout/boot and ARM interpreter execution are not certified
by this session. Native CI must execute; a cross-build does not satisfy that gate.
No Module 09 work, remote push, publication or host service installation was done.

Preparation preceded coding: [coverage plan](08-coverage.md), master prompt,
requirements traceability and definition of done. See [ADR 009](../../adr/009-managed-local-runtime-sandbox.md)
and [runtime usage/debug/configuration guide](../../client/local-runtimes.md).
Historical Module 06/07 reports are not retroactively marked DONE.

## Delivered behavior

The OS-authenticated client service accepts canonical JSON stdin/stdout invocation
through additive IPC revision 3. Fixed native, Python, Node, PowerShell, Shell,
Java JAR and .NET adapters run in reviewed digest-pinned Linux images. Batch/CMD
fails UNSUPPORTED; WASM is a future SandboxPort capability. Caller data never
selects an executable, working directory, interpreter, environment or CLI option.
Tool registrations and engine executable are protected administrator deployment
trust, independent from marketplace metadata or user IPC input.

Missing approved images download at installation-time preparation or first use
when allowFirstUsePull is enabled. Host Python is unnecessary. Offline cache mode
blocks missing images. An administrator must first supply the system-accessible
engine; user-session Docker Desktop alone does not guarantee logout operation.
No root installer script or arbitrary host interpreter fallback is supplied.

Every invocation needs fresh device readiness and online Gateway authorization.
ASK operations bind semantic input and immutable image, consume permits online,
and retain their expiry through execution startup/deadline. Outage/revocation
blocks. Preparation runs a confined version self-test before requesting execution
authorization; it grants no host capability. Exact Java four-component versions
retain the fourth component as SemVer build metadata.

Each container has no network/host mounts/capabilities, a read-only root, UID65532,
private bounded tmpfs, enforced memory/swap/PID/CPU/output/time limits and a fixed
seccomp profile that permits runtime threads but rejects fork/subprocess and
namespace/kernel escape operations. Host and image environment are cleared. Image
volumes are rejected. Malformed/duplicate/wrong-request/wrong-schema output and
stderr fail closed without reflecting tool diagnostics or secrets to callers.

Cancellation schedules owned cleanup; normal shutdown awaits recovery. A hard
service/engine crash may leave an isolated owned container until restart recovery;
this is documented, not a claim of an external independent watchdog. Health is a
bounded last-self-test snapshot and cleanup state, separate from device readiness.

## Verification evidence

Evidence logs and real binaries are ignored local artifacts. Commands below are
executed using Docker build tooling where host Rust/PHP/Helm are unavailable.

| Gate | Result / local evidence |
|---|---|
| `python tools/check.py` (`make check` equivalent) | PASS: 47 Python contract tests, 109-model language round trips, Gradle build/local Maven publication and all artifact-mode services, Rust workspace tests/clippy, 41 UI tests/five browser tests and complete strict Helm matrix. `.dev/module08-check-final.log`. |
| `python tools/check.py --scans` | PASS: workflow validation, npm/pip/Rust dependency/license policy, secret/source scans and filesystem scan. `.dev/module08-scans.log`. |
| Real seven-adapter `python tools/client/runtime_check.py --native-fixture ... --test-binary ...` | PASS: all seven real adapters, exact versions, literal injection, no children/network/host/environment, malformed output/overflow/timeout/memory, wrong version/missing image, expired consumed permit and online outage. Actual pinned Python first-use provisioning succeeded; `build/client/runtime-smoke.json`, `.dev/module08-runtime-smoke.log`. |
| `python tools/client/integration.py --runtime-image ... --runtime-version ... --docker-cli ...` | PASS: production Control TLS/device enrollment/mTLS, IPC invocation, HTTPS Gateway ALLOW, unauthorized peer, outage and revocation; `build/client/module08-integration.json`, `.dev/module08-integration.log`. |
| Linux native and Windows/macOS x64 release builds | PASS: Linux ELF, Windows PE and macOS Mach-O x64 0.8.0-dev release executables; final Linux unit/negative tests and clippy passed. `.dev/module08-verified-binaries.log`. |
| Three native archives, SBOM/checksums and deterministic rebuild | PASS: three real x64 native archives validated with original dependency notices, CycloneDX SBOMs, checksums, runtime profile/guides/example and anonymous-download manifest. Repeated final Linux packaging is checked below; `.dev/module08-packages-final.log`. |
| Windows/macOS/ARM native system service/runtime CI | BLOCKED: not executed here; Linux x64 real interpreter job configured. |
| Batch/CMD runtime | BLOCKED: secure adapter absent, explicit UNSUPPORTED; no insecure host fallback. |

The cross-component E2E uses existing production Module 07 Control/Gateway images
and the new 0.8 client, proving the additive contract boundary remains compatible.
Windows host execution tests passed all three runtime unit/negative tests; native
Linux client tests and clippy passed after the final resource-limit changes.

## Release, operations and definition of done

Product/contracts/chart version is 0.8.0-dev. No database migration or new
server/Helm workload is introduced. Existing approved client release publishing,
external signing/provenance hooks and anonymous download packaging remain. Managed
images require their own organization approval, license/SBOM/scan/provenance and
immutable distribution; they are not implicitly trusted or auto-registered.
Local packages are unsigned development artifacts, not production certified releases.
The full native OS/architecture CI and missing Batch adapter prevent DONE under
[the definition of done](../08-DEFINITION-OF-DONE.md), including complete primary
path and applicable native verification. No insecure temporary bypass is present.
Suggested commit: `feat(client): implement managed local tool runtimes`.

All 104 IDs are classified: 3 BLOCKED, 76 COMPLETE, 25 NOT_APPLICABLE.

## Individual requirement matrix

Statuses refer to this module's scope; NOT_APPLICABLE never erases prior gates.
Batch and native certification limitations above remain module-level blockers.

| ID | Status | Evidence / scope reason |
|---|---|---|
| ARC-001 | COMPLETE | Execution manager/adapter/engine ports; immutable local registration and canonical protocol; no Module 09 distribution. |
| ARC-002 | COMPLETE | Execution manager/adapter/engine ports; immutable local registration and canonical protocol; no Module 09 distribution. |
| ARC-003 | COMPLETE | Execution manager/adapter/engine ports; immutable local registration and canonical protocol; no Module 09 distribution. |
| ARC-004 | COMPLETE | Execution manager/adapter/engine ports; immutable local registration and canonical protocol; no Module 09 distribution. |
| ARC-005 | COMPLETE | Execution manager/adapter/engine ports; immutable local registration and canonical protocol; no Module 09 distribution. |
| ARC-006 | COMPLETE | Execution manager/adapter/engine ports; immutable local registration and canonical protocol; no Module 09 distribution. |
| ARC-007 | NOT_APPLICABLE | No new server/REST/database/cluster workload or hot-path throughput benchmark in Module 08; existing implementation/gates retained. |
| CON-001 | COMPLETE | 16 canonical schema groups, 109 models and deterministic Java/Rust/TypeScript/PHP bindings; additive IPC 3, frozen IPC 1/2, Maven artifact modes. |
| CON-002 | COMPLETE | 16 canonical schema groups, 109 models and deterministic Java/Rust/TypeScript/PHP bindings; additive IPC 3, frozen IPC 1/2, Maven artifact modes. |
| CON-003 | COMPLETE | 16 canonical schema groups, 109 models and deterministic Java/Rust/TypeScript/PHP bindings; additive IPC 3, frozen IPC 1/2, Maven artifact modes. |
| CON-004 | COMPLETE | 16 canonical schema groups, 109 models and deterministic Java/Rust/TypeScript/PHP bindings; additive IPC 3, frozen IPC 1/2, Maven artifact modes. |
| CON-005 | COMPLETE | 16 canonical schema groups, 109 models and deterministic Java/Rust/TypeScript/PHP bindings; additive IPC 3, frozen IPC 1/2, Maven artifact modes. |
| CON-006 | COMPLETE | 16 canonical schema groups, 109 models and deterministic Java/Rust/TypeScript/PHP bindings; additive IPC 3, frozen IPC 1/2, Maven artifact modes. |
| SEC-001 | COMPLETE | Protected custody, immutable image verification, fresh HTTPS Gateway/consumed ASK binding, isolated non-root/no network/no mounts sandbox, bounds and redaction. |
| SEC-002 | COMPLETE | Protected custody, immutable image verification, fresh HTTPS Gateway/consumed ASK binding, isolated non-root/no network/no mounts sandbox, bounds and redaction. |
| SEC-003 | COMPLETE | Protected custody, immutable image verification, fresh HTTPS Gateway/consumed ASK binding, isolated non-root/no network/no mounts sandbox, bounds and redaction. |
| SEC-004 | COMPLETE | Protected custody, immutable image verification, fresh HTTPS Gateway/consumed ASK binding, isolated non-root/no network/no mounts sandbox, bounds and redaction. |
| SEC-005 | COMPLETE | Protected custody, immutable image verification, fresh HTTPS Gateway/consumed ASK binding, isolated non-root/no network/no mounts sandbox, bounds and redaction. |
| SEC-006 | COMPLETE | Protected custody, immutable image verification, fresh HTTPS Gateway/consumed ASK binding, isolated non-root/no network/no mounts sandbox, bounds and redaction. |
| SEC-007 | COMPLETE | Protected custody, immutable image verification, fresh HTTPS Gateway/consumed ASK binding, isolated non-root/no network/no mounts sandbox, bounds and redaction. |
| SEC-008 | COMPLETE | Protected custody, immutable image verification, fresh HTTPS Gateway/consumed ASK binding, isolated non-root/no network/no mounts sandbox, bounds and redaction. |
| SEC-009 | COMPLETE | Protected custody, immutable image verification, fresh HTTPS Gateway/consumed ASK binding, isolated non-root/no network/no mounts sandbox, bounds and redaction. |
| SEC-010 | COMPLETE | Protected custody, immutable image verification, fresh HTTPS Gateway/consumed ASK binding, isolated non-root/no network/no mounts sandbox, bounds and redaction. |
| TST-001 | COMPLETE | Unit/negative contracts plus explicit real OCI adapter/security gate and enrolled-client HTTP/IPC integration; native platform exclusions below. |
| TST-002 | COMPLETE | Unit/negative contracts plus explicit real OCI adapter/security gate and enrolled-client HTTP/IPC integration; native platform exclusions below. |
| TST-003 | BLOCKED | Batch/CMD adapter missing; native OS/ARM service/runtime execution gates not run. Cross-builds do not establish certified release readiness. |
| TST-004 | COMPLETE | Unit/negative contracts plus explicit real OCI adapter/security gate and enrolled-client HTTP/IPC integration; native platform exclusions below. |
| TST-005 | COMPLETE | Unit/negative contracts plus explicit real OCI adapter/security gate and enrolled-client HTTP/IPC integration; native platform exclusions below. |
| TST-006 | BLOCKED | Batch/CMD adapter missing; native OS/ARM service/runtime execution gates not run. Cross-builds do not establish certified release readiness. |
| TST-007 | NOT_APPLICABLE | No new server/REST/database/cluster workload or hot-path throughput benchmark in Module 08; existing implementation/gates retained. |
| TST-008 | COMPLETE | Unit/negative contracts plus explicit real OCI adapter/security gate and enrolled-client HTTP/IPC integration; native platform exclusions below. |
| QLT-001 | COMPLETE | Fixed vectors, documented security APIs, formatting/clippy/source/header/drift checks; no extra external Rust dependency. |
| QLT-002 | COMPLETE | Fixed vectors, documented security APIs, formatting/clippy/source/header/drift checks; no extra external Rust dependency. |
| QLT-003 | COMPLETE | Fixed vectors, documented security APIs, formatting/clippy/source/header/drift checks; no extra external Rust dependency. |
| QLT-004 | COMPLETE | Fixed vectors, documented security APIs, formatting/clippy/source/header/drift checks; no extra external Rust dependency. |
| QLT-005 | COMPLETE | Fixed vectors, documented security APIs, formatting/clippy/source/header/drift checks; no extra external Rust dependency. |
| QLT-006 | COMPLETE | Fixed vectors, documented security APIs, formatting/clippy/source/header/drift checks; no extra external Rust dependency. |
| API-001 | COMPLETE | Shared JSON protocol/IPC errors, closed input/output schemas, deadline/size/authorization validation; no new REST route. |
| API-002 | NOT_APPLICABLE | No new server/REST/database/cluster workload or hot-path throughput benchmark in Module 08; existing implementation/gates retained. |
| API-003 | COMPLETE | Shared JSON protocol/IPC errors, closed input/output schemas, deadline/size/authorization validation; no new REST route. |
| API-004 | COMPLETE | Shared JSON protocol/IPC errors, closed input/output schemas, deadline/size/authorization validation; no new REST route. |
| API-005 | COMPLETE | Shared JSON protocol/IPC errors, closed input/output schemas, deadline/size/authorization validation; no new REST route. |
| API-006 | COMPLETE | Shared JSON protocol/IPC errors, closed input/output schemas, deadline/size/authorization validation; no new REST route. |
| API-007 | COMPLETE | Shared JSON protocol/IPC errors, closed input/output schemas, deadline/size/authorization validation; no new REST route. |
| DAT-001 | NOT_APPLICABLE | No new server/REST/database/cluster workload or hot-path throughput benchmark in Module 08; existing implementation/gates retained. |
| DAT-002 | NOT_APPLICABLE | No new server/REST/database/cluster workload or hot-path throughput benchmark in Module 08; existing implementation/gates retained. |
| DAT-003 | NOT_APPLICABLE | No new server/REST/database/cluster workload or hot-path throughput benchmark in Module 08; existing implementation/gates retained. |
| DAT-004 | COMPLETE | Private configuration/profile state and existing protected enrollment custody; tool data is ephemeral tmpfs, identity and host secrets never mounted. |
| DAT-005 | COMPLETE | Private configuration/profile state and existing protected enrollment custody; tool data is ephemeral tmpfs, identity and host secrets never mounted. |
| OBS-001 | COMPLETE | Structured runtime prepare/execution events, correlation, health/self-test and success/failure counters; orderly cleanup and owned orphan recovery. |
| OBS-002 | COMPLETE | Structured runtime prepare/execution events, correlation, health/self-test and success/failure counters; orderly cleanup and owned orphan recovery. |
| OBS-003 | COMPLETE | Structured runtime prepare/execution events, correlation, health/self-test and success/failure counters; orderly cleanup and owned orphan recovery. |
| OBS-004 | COMPLETE | Structured runtime prepare/execution events, correlation, health/self-test and success/failure counters; orderly cleanup and owned orphan recovery. |
| OBS-005 | COMPLETE | Structured runtime prepare/execution events, correlation, health/self-test and success/failure counters; orderly cleanup and owned orphan recovery. |
| OBS-006 | NOT_APPLICABLE | No new server/REST/database/cluster workload or hot-path throughput benchmark in Module 08; existing implementation/gates retained. |
| OBS-007 | COMPLETE | Structured runtime prepare/execution events, correlation, health/self-test and success/failure counters; orderly cleanup and owned orphan recovery. |
| DKR-001 | NOT_APPLICABLE | No new server/REST/database/cluster workload or hot-path throughput benchmark in Module 08; existing implementation/gates retained. |
| DKR-002 | NOT_APPLICABLE | No new server/REST/database/cluster workload or hot-path throughput benchmark in Module 08; existing implementation/gates retained. |
| DKR-003 | COMPLETE | Read-only root, UID 65532, no caps/network/host mounts, enforced memory/PID/CPU/seccomp; approved images contain interpreters/dependencies. |
| DKR-004 | COMPLETE | Read-only root, UID 65532, no caps/network/host mounts, enforced memory/PID/CPU/seccomp; approved images contain interpreters/dependencies. |
| DKR-005 | NOT_APPLICABLE | No new server/REST/database/cluster workload or hot-path throughput benchmark in Module 08; existing implementation/gates retained. |
| DKR-006 | NOT_APPLICABLE | No new server/REST/database/cluster workload or hot-path throughput benchmark in Module 08; existing implementation/gates retained. |
| DKR-007 | COMPLETE | Read-only root, UID 65532, no caps/network/host mounts, enforced memory/PID/CPU/seccomp; approved images contain interpreters/dependencies. |
| DKR-008 | COMPLETE | Read-only root, UID 65532, no caps/network/host mounts, enforced memory/PID/CPU/seccomp; approved images contain interpreters/dependencies. |
| K8S-001 | NOT_APPLICABLE | No new server/REST/database/cluster workload or hot-path throughput benchmark in Module 08; existing implementation/gates retained. |
| K8S-002 | NOT_APPLICABLE | No new server/REST/database/cluster workload or hot-path throughput benchmark in Module 08; existing implementation/gates retained. |
| K8S-003 | NOT_APPLICABLE | No new server/REST/database/cluster workload or hot-path throughput benchmark in Module 08; existing implementation/gates retained. |
| K8S-004 | NOT_APPLICABLE | No new server/REST/database/cluster workload or hot-path throughput benchmark in Module 08; existing implementation/gates retained. |
| K8S-005 | NOT_APPLICABLE | No new server/REST/database/cluster workload or hot-path throughput benchmark in Module 08; existing implementation/gates retained. |
| K8S-006 | NOT_APPLICABLE | No new server/REST/database/cluster workload or hot-path throughput benchmark in Module 08; existing implementation/gates retained. |
| K8S-007 | NOT_APPLICABLE | No new server/REST/database/cluster workload or hot-path throughput benchmark in Module 08; existing implementation/gates retained. |
| K8S-008 | NOT_APPLICABLE | No new server/REST/database/cluster workload or hot-path throughput benchmark in Module 08; existing implementation/gates retained. |
| K8S-009 | NOT_APPLICABLE | No new server/REST/database/cluster workload or hot-path throughput benchmark in Module 08; existing implementation/gates retained. |
| K8S-010 | COMPLETE | No new cluster workload; existing chart metadata/version and lint/render schema gates retained. |
| K8S-011 | NOT_APPLICABLE | No new server/REST/database/cluster workload or hot-path throughput benchmark in Module 08; existing implementation/gates retained. |
| K8S-012 | NOT_APPLICABLE | No new server/REST/database/cluster workload or hot-path throughput benchmark in Module 08; existing implementation/gates retained. |
| K8S-013 | COMPLETE | No new cluster workload; existing chart metadata/version and lint/render schema gates retained. |
| K8S-014 | COMPLETE | No new cluster workload; existing chart metadata/version and lint/render schema gates retained. |
| K8S-015 | NOT_APPLICABLE | No new server/REST/database/cluster workload or hot-path throughput benchmark in Module 08; existing implementation/gates retained. |
| K8S-016 | NOT_APPLICABLE | No new server/REST/database/cluster workload or hot-path throughput benchmark in Module 08; existing implementation/gates retained. |
| REL-001 | COMPLETE | 0.8.0-dev synchronized contracts/product/chart; native package guides, seccomp profile, checksums, SBOM/notices; existing CI-only publication and external signing hooks. |
| REL-002 | COMPLETE | 0.8.0-dev synchronized contracts/product/chart; native package guides, seccomp profile, checksums, SBOM/notices; existing CI-only publication and external signing hooks. |
| REL-003 | COMPLETE | 0.8.0-dev synchronized contracts/product/chart; native package guides, seccomp profile, checksums, SBOM/notices; existing CI-only publication and external signing hooks. |
| REL-004 | COMPLETE | 0.8.0-dev synchronized contracts/product/chart; native package guides, seccomp profile, checksums, SBOM/notices; existing CI-only publication and external signing hooks. |
| REL-005 | COMPLETE | 0.8.0-dev synchronized contracts/product/chart; native package guides, seccomp profile, checksums, SBOM/notices; existing CI-only publication and external signing hooks. |
| REL-006 | COMPLETE | 0.8.0-dev synchronized contracts/product/chart; native package guides, seccomp profile, checksums, SBOM/notices; existing CI-only publication and external signing hooks. |
| REL-007 | COMPLETE | 0.8.0-dev synchronized contracts/product/chart; native package guides, seccomp profile, checksums, SBOM/notices; existing CI-only publication and external signing hooks. |
| REL-008 | COMPLETE | 0.8.0-dev synchronized contracts/product/chart; native package guides, seccomp profile, checksums, SBOM/notices; existing CI-only publication and external signing hooks. |
| REL-009 | COMPLETE | 0.8.0-dev synchronized contracts/product/chart; native package guides, seccomp profile, checksums, SBOM/notices; existing CI-only publication and external signing hooks. |
| REL-010 | BLOCKED | Batch/CMD adapter missing; native OS/ARM service/runtime execution gates not run. Cross-builds do not establish certified release readiness. |
| REL-011 | COMPLETE | 0.8.0-dev synchronized contracts/product/chart; native package guides, seccomp profile, checksums, SBOM/notices; existing CI-only publication and external signing hooks. |
| DOC-001 | COMPLETE | Client/runtime/configuration/first-use/offline/debug guides, ADR 009, architecture/security/compatibility/changelog and this report. |
| DOC-002 | COMPLETE | Client/runtime/configuration/first-use/offline/debug guides, ADR 009, architecture/security/compatibility/changelog and this report. |
| DOC-003 | COMPLETE | Client/runtime/configuration/first-use/offline/debug guides, ADR 009, architecture/security/compatibility/changelog and this report. |
| DOC-004 | COMPLETE | Client/runtime/configuration/first-use/offline/debug guides, ADR 009, architecture/security/compatibility/changelog and this report. |
| DOC-005 | COMPLETE | Client/runtime/configuration/first-use/offline/debug guides, ADR 009, architecture/security/compatibility/changelog and this report. |
| DOC-006 | COMPLETE | Client/runtime/configuration/first-use/offline/debug guides, ADR 009, architecture/security/compatibility/changelog and this report. |
| DOC-007 | COMPLETE | Client/runtime/configuration/first-use/offline/debug guides, ADR 009, architecture/security/compatibility/changelog and this report. |
| PRF-001 | COMPLETE | Bounded process I/O, memory/PID/CPU/time/temp storage, serialized service access and backpressure; execution stays outside Gateway policy hot path. |
| PRF-002 | COMPLETE | Bounded process I/O, memory/PID/CPU/time/temp storage, serialized service access and backpressure; execution stays outside Gateway policy hot path. |
| PRF-003 | COMPLETE | Bounded process I/O, memory/PID/CPU/time/temp storage, serialized service access and backpressure; execution stays outside Gateway policy hot path. |
| PRF-004 | COMPLETE | Bounded process I/O, memory/PID/CPU/time/temp storage, serialized service access and backpressure; execution stays outside Gateway policy hot path. |
| PRF-005 | NOT_APPLICABLE | No new server/REST/database/cluster workload or hot-path throughput benchmark in Module 08; existing implementation/gates retained. |
| PRF-006 | COMPLETE | Bounded process I/O, memory/PID/CPU/time/temp storage, serialized service access and backpressure; execution stays outside Gateway policy hot path. |
