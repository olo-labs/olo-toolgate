# Module 01 completion report

Status: DONE for Module 01. Prepared against the master prompt,
requirements traceability and `08-DEFINITION-OF-DONE.md`, 2026-10-02. The
[coverage plan](01-coverage.md) and [ADR 002](../../adr/002-gateway-static-foundation.md)
preceded implementation. Module 00 remains the dependency; its existing guard
fixes were preserved. No later module was started.

## Implementation and review

The Rust Gateway provides authenticated `/v1/authorize` decisions over generated
shared contracts. Identity comes exclusively from administrator-provisioned,
expiring, high-entropy runtime credentials stored as SHA-256 digests. Immutable
exact-match policy evaluates emergency BLOCK, matching BLOCK, ALLOW, then default
BLOCK. Expiry, unsupported ASK, extractor errors and policy/audit failures fail
closed. Credential and policy freshness are checked again before an ALLOW response.

Transport adapters are separate from application coordination and the policy,
resource extraction and audit ports. Canonical schemas compile once with offline
references. Strict JSON rejects duplicate keys recursively and bounds depth/body.
Trusted extractor bindings derive logical FILE/CUSTOM resources; traversal,
absolute/URI/escape/control/backslash aliases reject. No DB, service lookup,
network egress, code execution, package loading or signing path exists.

Authenticated stateless MCP 2026-07-28 ingress implements mirrored metadata/header
validation, ping and empty discovery. Calls traverse authorization but cannot
execute; no executable capability or permit is advertised. Unsupported legacy
sessions, streams, notifications and permits have explicit errors. Parse errors
retain JSON-RPC `id: null`; rejected notifications have no JSON-RPC reply.

A bounded acknowledged stdout audit adapter emits canonical events with principal
context and argument/resource digests. JSON tracing logs use stderr and fixed
fields; no bearer, raw arguments or raw locator is logged. Acknowledgement proves
local write/flush, not durable collector retention. Management probes/metrics use
a separate restricted listener. Admission bounds connections, concurrency, rate,
headers/body, request/connection/drain deadlines and audit queue. SIGTERM removes
readiness, stops admission and drains bounded work. Replicas share immutable
snapshots and need no session affinity; capacity limits are per replica.

Two additive closed contracts and fixtures advance product/contracts/chart to
0.2.0-dev with frozen v1 definitions intact. Rust schema assets are generated
inside the shared crate. Java artifacts remain locally proven in both dependency
modes. Version plumbing includes Gateway OpenAPI. The raw release bundle,
compatibility metadata, chart and upgrade assets include Gateway scope.

The pinned musl builder produces a static binary in a pinned distroless static
non-root runtime. Cargo/Rust/musl notices and the locked dependency graph are
retained in the image. The initial cc base had a high OpenSSL advisory; the unused
library was removed by using the static runtime, with no suppression. Source
license review added permissive MIT-0/Zlib, narrowly normalized legacy MIT/Apache
Cargo expressions and handled SPDX exception symbols without implicit approval.

Gateway Helm is opt-in and emits Deployment, runtime/management Services,
ConfigMap, ServiceAccount, deny-egress NetworkPolicy and PDB. Optional HPA,
TLS ingress and ServiceMonitor, resources/security contexts/probes/placement and
external credential references are validated. Disabled defaults emit no workload.
Kind checks two ready replicas and actual ALLOW -> BLOCK -> ALLOW across install,
emergency-policy upgrade and rollback. Production CNI enforcement remains an
operator validation responsibility; Kind's default CNI smoke is not that proof.

## Definition of done

| Area | Applied result |
|---|---|
| Implementation | Primary static authorization path complete; real adapters, no permissive mock/bypass or blocking TODO; shared contracts and ownership boundaries preserved. |
| Tests | Unit, negative, contract, security and real-boundary integrations pass; deterministic clock tests and critical-path benchmark recorded. Multi-component E2E is not applicable before an owning customer flow exists. |
| Quality | Headers, public/security API documentation, centralized justified dependencies, no duplicated wire model/secret telemetry; static gates pass. |
| Operations | Separate health/readiness/metrics, structured logs/audit, trace/request IDs and bounded graceful shutdown verified. |
| Deployment | Actual non-root/read-only image and restricted Helm workload; resources, probes, account, policies, HA and rollback documented/tested. |
| Release | Versions/publication impacts handled; protected Maven/Helm/GHCR/GitHub assets and SBOM/provenance pipelines configured. Local checks issue no remote release or attestation. |
| Documentation | Root/component README, ADR, canonical OpenAPI/API, configuration, upgrade/HA/troubleshooting and examples updated. |
| Proof | Executed commands/results and all 104 individual requirement classifications follow; suggested commit supplied. |

## Executed commands and results

Windows verification used Java 21.0.11, Gradle 9.7.1, Node 24.18.0, Python 3.12,
GNU Make 4.4.1 and Docker Rust 1.94.1/PHP 8.2/Helm 3.17.3 toolchains with
`TOOLGATE_DOCKER_TOOLS=1`. CI selects Node 22 and Ubuntu 24.04. Native verified
Kind 0.27.0/Helm 3.17.3 and Docker-provided kubectl exercised an isolated,
SHA-256-pinned Kubernetes 1.32.2 cluster. Commands below abbreviate the bundled
Python executable path; no required tool was skipped.

| Command | Result |
|---|---|
| `make check PYTHON=<Python executable>` | PASS: full workspace, mandatory local publication proof and chart gates. |
| `python -m unittest discover -s tests/contracts -v` | PASS: 21 schema/negative/compatibility/drift/version/license/CI/API/chart/docs/bundle tests. |
| `python tools/contracts/version.py --check`; `python tools/contracts/generate.py --check`; `python tools/quality.py` | PASS: synchronized versions, 65 generated outputs and source/header/license/secret gate. |
| `gradlew.bat projects javaCheck build`; local `publishToMavenLocal` and artifact-mode builds via check.py | PASS: Maven JAR/POM/sources/Javadoc inspected; all three consumers load the actual 0.2.0-dev JAR; empty repository rejects. |
| `cargo fmt --all --check`; `cargo test --workspace --locked`; `cargo clippy --workspace --all-targets --locked -- -D warnings` | PASS: 19 Gateway tests and 2 shared contract tests. Affected gates rerun after final MCP protocol fixes. |
| npm contract check/build/test; PHP round trips and every generated source lint | PASS: TypeScript transport/negative type checks; 24 PHP model round trips and negatives; Java/Rust additions pass. |
| `python tools/check.py --scans` | PASS: actionlint, npm/pip audit, 183-entry resolved license audit, redacted Gitleaks and Trivy source high/critical vulnerability/license gates. |
| `python tools/gateway/check.py` | PASS: disabled empty chart; five enabled variants in install/upgrade mode; strict core Kubernetes and verified upstream ServiceMonitor schemas; seven invalid-value cases reject. |
| `python tools/gateway/container.py`; `--no-build` verification | PASS: production static image, notices/lock inspected, non-root/read-only/no capabilities, ALLOW/BLOCK/401, MCP ping/parse errors, separate management, metrics, stdout/stderr redaction and SIGTERM exit 0. |
| Trivy image `--exit-code 1 --severity HIGH,CRITICAL`; CycloneDX image export | PASS: OS and embedded Cargo graph have no high/critical findings; SBOM retained. |
| `python tools/gateway/cluster.py` | PASS: isolated Kind installation with two ready replicas, authorization/auth failure, emergency BLOCK upgrade and ALLOW rollback. Own cluster removed afterward. |
| `python tools/gateway/benchmark.py` | PASS: release authorization-core sample with host/CPU/toolchain and exclusions recorded. |
| `python tools/release/bundle.py`; `helm package`; checksum verification | PASS: reproducible raw contracts/OpenAPI bundle, chart, compatibility, notices/SBOM evidence and SHA-256 assets prepared locally. |
| `git diff HEAD --check` | PASS. |

Operational startup/port-forward polling is bounded and checks actual endpoint or
rollout state; correctness tests do not synchronize with arbitrary sleeps. The
rate-boundary test was repaired to use a paused clock. Rollback readiness ignores
terminating pods and probes a current ready pod after each completed rollout.
The old PHP fixture count was updated from 22 to 24 after the full gate caught it.

Local ignored evidence is in `build/gateway/`: benchmark.json,
container-smoke.json, cluster-smoke.json, rendered variants, gateway-sbom.cdx.json
and release assets/checksums. Execution logs remain under `.dev/`. No remote
image/artifact push, tag, GitHub release, CI execution or provenance issuance is
claimed. Protected CI environments, registry permissions and Maven credentials
must be configured by administrators for the publication scaffold.

## Measured baseline and practical limits

The core benchmark warmed 1,000 iterations and measured 20,000 release calls on
Windows 11/Docker, Intel Family 6 Model 85 Stepping 4, Rust 1.94.1:

| Statistic | Nanoseconds | Microseconds |
|---|---:|---:|
| p50 | 8,909 | 8.909 |
| p95 | 28,418 | 28.418 |
| p99 | 37,900 | 37.900 |
| mean | 12,358 | 12.358 |

Validation, normalized data/extraction/digests, static evaluation and audit event
construction are included. Audit acknowledges immediately in this benchmark.
Network, TLS, body transfer, collector/durable storage I/O and the production
musl runtime are excluded. This is an observed baseline, not a throughput/SLO
claim. Production smoke runs with a 256 MiB memory limit and one CPU. Docker reports
5,057,554 image bytes for the tested image content ID
`sha256:9d112a594f9ea54d19efa27d32c56c2b8bbcbe49b05094241ae7c9e7efea16bf`; the same image passed the Kind smoke.

Configuration and credentials are static mounted inputs requiring rolling
restart for changes; old replicas retain old inputs until replaced. Urgent
revocation must remove ingress or replace all old replicas before reopening.
Rollback may restore older permissions. Management must stay private and TLS
must terminate at a trusted restricted ingress. ASK, OIDC, signed distribution,
permits, execution and credential brokering remain unsupported with no fallback;
future executors must independently enforce resource confinement. No later
module is implemented here.

## Individual requirement matrix

All 104 unique IDs are classified: 87 COMPLETE and 17 NOT_APPLICABLE with scope reasons.
No applicable requirement is BLOCKED. Three template example rows in the traceability
document are excluded from the count; the Module 00 matrix duplicate rows were
removed without changing its classifications.

| Requirement | Status | Evidence / scope reason |
|---|---|---|

| ARC-001 | COMPLETE | Gateway owns decisions only; application.rs and ADR 002. |
| ARC-002 | COMPLETE | Thin http/mcp adapters, application coordination and separate policy/auth/extraction/audit modules. |
| ARC-003 | COMPLETE | PolicyEvaluator, ResourceExtractor, AuditSink ports; injected failure tests. |
| ARC-004 | COMPLETE | Generated AuthorizationRequest, PolicyInput/Decision, RequestContext, ErrorEnvelope and RuntimeAuditEvent. |
| ARC-005 | COMPLETE | Shared Rust crate embeds canonical schemas; no service-source imports. |
| ARC-006 | NOT_APPLICABLE | No package/client/approval lifecycle workflow exists in a stateless decision service. |
| ARC-007 | COMPLETE | Immutable snapshots, no DB/session/PVC; two-replica Kind install and rollback. |
| CON-001 | COMPLETE | Canonical runtime.schema.json and OpenAPI shared references. |
| CON-002 | NOT_APPLICABLE | Maven publication configuration is inherited from Module 00; local 0.2.0-dev artifact proof was rerun. |
| CON-003 | NOT_APPLICABLE | Dependency modes are inherited; both were revalidated for additive contracts. |
| CON-004 | COMPLETE | Frozen v1 schema/corpus compatibility plus additive fixtures in four languages. |
| CON-005 | COMPLETE | 65 deterministic generated outputs, including embedded Rust schemas; drift gate passed. |
| CON-006 | COMPLETE | Synchronized 0.2.0-dev product/contracts/chart/OpenAPI; wire major remains v1. |
| SEC-001 | COMPLETE | Emergency BLOCK, explicit BLOCK, ALLOW, default BLOCK precedence; expiry/ASK/dependency failure tests. |
| SEC-002 | COMPLETE | Fixed log fields, digest-only credentials, hashed argument/resource audit; container redaction proof. |
| SEC-003 | NOT_APPLICABLE | No signed package/bundle/permit or executable is ingested. Static local administrator inputs are the explicit supported source. |
| SEC-004 | COMPLETE | Marketplace metadata/signatures are not runtime policy inputs. |
| SEC-005 | COMPLETE | Deployment state never grants access; credential-bound identity and exact policy required. |
| SEC-006 | NOT_APPLICABLE | No untrusted code executes or is loaded; MCP execution is explicitly unsupported. |
| SEC-007 | COMPLETE | No egress/shell/archive/file execution; extractor rejects traversal/absolute/URI/escape/control/backslash aliases. |
| SEC-008 | COMPLETE | Runtime credentials independent of Marketplace, organization, bundle and permit signing domains. |
| SEC-009 | NOT_APPLICABLE | No security mutation/admin API exists. Authorization evaluations are audited. |
| SEC-010 | NOT_APPLICABLE | No execution permit or mutation/replay-sensitive action; a decision cannot authorize later execution. |
| TST-001 | COMPLETE | 19 Gateway Rust tests, 2 shared Rust tests and Java/TypeScript/PHP package tests. |
| TST-002 | COMPLETE | Malformed/duplicate JSON, media/body/header/rate/concurrency/auth/expiry/extractor/dependency negatives. |
| TST-003 | COMPLETE | Real TCP HTTP/MCP, production image/audit/SIGTERM, Kind install/upgrade/rollback. |
| TST-004 | COMPLETE | 21 Python schema/compatibility/drift/asset tests and all four language round trips. |
| TST-005 | COMPLETE | Credential identity isolation, fail-closed ports/ASK, telemetry redaction and source/image scans. |
| TST-006 | NOT_APPLICABLE | No multi-component customer flow yet. TCP, image and cluster tests cover this module primary path. |
| TST-007 | COMPLETE | 20,000-sample release authorization-core benchmark with recorded environment. |
| TST-008 | COMPLETE | Paused clocks for exact boundaries/deadlines; bounded real readiness/rollout polling. |
| QLT-001 | COMPLETE | Source copyright/SPDX gate passed for Rust/Python/shell/YAML and generated bindings. |
| QLT-002 | COMPLETE | Public ports/security decisions/config/transport/limits documented in source and gateway docs. |
| QLT-003 | COMPLETE | Rust fmt/clippy warnings denied, Java/TS/PHP/Python checks and actionlint passed. |
| QLT-004 | COMPLETE | Workspace dependency versions/lock, schema network retrieval disabled, ADR dependency rationale, license inventory. |
| QLT-005 | COMPLETE | Small boundary modules, explicit per-process state, injected ports; no service locator or mutable global correctness state. |
| QLT-006 | COMPLETE | Shared ErrorCode/ErrorEnvelope and documented MCP RPC errors; sanitized responses. |
| API-001 | COMPLETE | POST /v1/authorize and versioned private management; /v1/permits explicitly unsupported. |
| API-002 | COMPLETE | Canonical gateway-v1.yaml refers to shared request/decision/error definitions. |
| API-003 | COMPLETE | Offline schemas, strict recursive duplicate-key JSON, bounded ingress, MCP metadata validation. |
| API-004 | COMPLETE | Shared ErrorCode and documented HTTP statuses; MCP parse/header/version codes tested. |
| API-005 | COMPLETE | Server-generated request ID; validated traceparent; caller identity/request IDs cannot overwrite auth. |
| API-006 | NOT_APPLICABLE | No mutation API; authorize is a stateless decision operation. |
| API-007 | COMPLETE | Fixed sanitized envelopes/RPC messages/startup reasons; no exception details. |
| DAT-001 | NOT_APPLICABLE | No database or persistent schema. |
| DAT-002 | NOT_APPLICABLE | No database install migration. |
| DAT-003 | NOT_APPLICABLE | No database upgrade migration. |
| DAT-004 | NOT_APPLICABLE | No database transactions; bounded admission concurrency is tested. |
| DAT-005 | COMPLETE | No DB or retained local app state; external immutable config/Secret, stdout audit with explicit durability limits. |
| OBS-001 | COMPLETE | Private /v1/health/live tested independently of runtime router. |
| OBS-002 | COMPLETE | Readiness covers credential/policy expiry, audit readiness and drain state. |
| OBS-003 | COMPLETE | JSON tracing on stderr; canonical structured audit events on stdout. |
| OBS-004 | COMPLETE | Private Prometheus fixed-cardinality counters/histogram and optional ServiceMonitor. |
| OBS-005 | COMPLETE | Validated W3C traceparent propagated into application/audit and completion spans. |
| OBS-006 | NOT_APPLICABLE | No durable job API; audit acknowledgement is explicitly local write/flush. |
| OBS-007 | COMPLETE | Raw arguments/locators/tokens excluded; redaction assertions and container log inspection. |
| DKR-001 | COMPLETE | Pinned musl builder and separate distroless static runtime Dockerfile. |
| DKR-002 | COMPLETE | Distroless static runtime without shell or unused OpenSSL; image scan passed. |
| DKR-003 | COMPLETE | USER 65532:65532 verified in container and Helm. |
| DKR-004 | COMPLETE | No credential COPY; excluded ignored dev/build inputs; digest-only mounted credentials. |
| DKR-005 | COMPLETE | OCI title/source/license/version/revision labels; build VERSION validated. |
| DKR-006 | COMPLETE | Actual image scan and CycloneDX pipeline; local source/image scans and SBOM export. |
| DKR-007 | COMPLETE | SIGTERM drains listeners/requests/audit with deadlines; TCP/container exit-zero proof. |
| DKR-008 | COMPLETE | Production smoke uses read-only root, dropped capabilities and no-new-privileges. |
| K8S-001 | COMPLETE | Opt-in Deployment/Services/ConfigMap; default disabled chart remains empty. |
| K8S-002 | COMPLETE | Configurable resource requests/limits with strict values validation. |
| K8S-003 | COMPLETE | Non-root/read-only/no escalation/capabilities ALL drop/seccomp RuntimeDefault. |
| K8S-004 | COMPLETE | Startup/live/ready probes on separate management listener. |
| K8S-005 | COMPLETE | Dedicated ServiceAccount, account/pod automount disabled. |
| K8S-006 | COMPLETE | Deny egress; explicit runtime/management peers. Kind does not prove production CNI enforcement. |
| K8S-007 | COMPLETE | Two replicas and configurable PDB. |
| K8S-008 | COMPLETE | Optional CPU HPA; min/max validation. |
| K8S-009 | COMPLETE | Node selectors/tolerations/affinity/topology spread values. |
| K8S-010 | COMPLETE | Existing read-only credential Secret mount; no secret values in chart. |
| K8S-011 | COMPLETE | Optional ingress requires TLS Secret, class and reviewed peers. |
| K8S-012 | COMPLETE | Optional monitor/private metrics Service; verified upstream CRD structural schema. |
| K8S-013 | COMPLETE | helm lint --strict and negative values passed. |
| K8S-014 | COMPLETE | Five enabled variants, install/upgrade renders, strict core/monitor schemas passed. |
| K8S-015 | COMPLETE | Kind 0.27.0/Kubernetes 1.32.2 install and emergency-deny upgrade, two ready replicas. |
| K8S-016 | COMPLETE | Actual ALLOW -> BLOCK -> ALLOW install/upgrade/rollback probes; rotation/rollback docs. |
| REL-001 | NOT_APPLICABLE | Inherited CI-only Maven pipeline retained; local 0.2.0-dev publication proof passed. |
| REL-002 | COMPLETE | Protected matching-tag GHCR scaffold pushes the exact scanned saved image; no local remote push. |
| REL-003 | COMPLETE | Existing protected Helm OCI pipeline packages the versioned deployable chart. |
| REL-004 | COMPLETE | Raw runtime schema/OpenAPI, upgrade notes, compatibility and contract/chart release assets; image/SBOM/benchmark CI artifacts. |
| REL-005 | COMPLETE | Reproducible bundle checksums; gateway workflow verifies image/SBOM checksums before push. |
| REL-006 | COMPLETE | 183-entry resolved license inventory, source/image CycloneDX pipeline and local image export. |
| REL-007 | COMPLETE | Pinned GitHub provenance attestations for image digest/SBOM; configured, not issued locally. |
| REL-008 | NOT_APPLICABLE | Signing is not configured; no signed bundle/permit or signature is claimed. Provenance pipeline is configured. |
| REL-009 | COMPLETE | CHANGELOG.md records Module 01 and additive 0.2.0-dev contracts. |
| REL-010 | COMPLETE | Gateway/chart/contracts/API/MCP compatibility docs and release compatibility.json. |
| REL-011 | COMPLETE | gateway/upgrades.md documents immutable inputs, restarts/rotation and rollback; no prior stable runtime window. |
| DOC-001 | COMPLETE | Root README/CONTRACTS and gateway developer/operations/performance docs updated. |
| DOC-002 | COMPLETE | apps/gateway/README.md and docs/gateway/README.md explain supported scope and local commands. |
| DOC-003 | COMPLETE | Config paths, trust inputs, TLS/origins, limits, precedence and failure behavior documented. |
| DOC-004 | COMPLETE | Canonical OpenAPI plus docs/api/gateway-api.md document decisions/restricted MCP. |
| DOC-005 | COMPLETE | ADR 002 documents ownership/ports/static trust/fail-closed audit/limits. |
| DOC-006 | COMPLETE | No real bearer values in example; one-hour random dev tokens only in ignored .dev. |
| DOC-007 | COMPLETE | HA/TLS/NetworkPolicy/collector limitations, readiness, upgrade/rollback troubleshooting. |
| PRF-001 | COMPLETE | In-memory schema/policy/extractor snapshots; no DB/Control/Marketplace client. |
| PRF-002 | COMPLETE | Bounded collections, connections, concurrency, rate, headers/body/depth/time and audit queue. |
| PRF-003 | COMPLETE | No session/affinity; two replicas and optional HPA; no fleet quota claimed. |
| PRF-004 | NOT_APPLICABLE | No fleet-scale reporting path in Gateway. |
| PRF-005 | COMPLETE | Warmup/samples/environment/scope/exclusions documented; no noisy threshold gate. |
| PRF-006 | COMPLETE | Bounded admission and acknowledged audit queue; saturation and deadline tests fail closed. |

## Suggested commit

```text
feat(gateway): implement stateless gateway core
```

No commit, remote publication or next-module work was performed.
