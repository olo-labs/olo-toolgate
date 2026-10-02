# Module 02 completion report

Status: DONE for Module 02, 2026-10-02. The master prompt, traceability,
governing architecture/security documents and
[definition of done](../08-DEFINITION-OF-DONE.md) were read before coding.
The [coverage plan](02-coverage.md) and
[ADR 003](../../adr/003-control-plane-transactions.md) preceded implementation.
Module 00 remains the dependency; Module 01 behavior is preserved. No later module
was started.

## Implementation and review

The [Java 21/Quarkus application](../../../apps/control-plane/README.md) provides
tenant-scoped users, teams, agents, tools, policies and device records through
`/api/control/v1`. Pure typed-ID/domain graph rules, application Store/Codec ports
and HTTP/JDBC/security adapters preserve the documented layering. Shared generated
DTOs and offline canonical schemas supply all wire boundaries. The protected
[canonical OpenAPI](../../../packages/contracts/openapi/control-v1.yaml) is served
with referenced shared schemas embedded.

RS256 signatures, exact HTTPS issuer/administrative audience, mounted public key,
signed tenant/groups/sub and bounded issue/expiry claims are required. Readers
cannot mutate or access audit. Tenant identity comes only from verified claims;
directory users do not provision IdP accounts or assign JWT roles. Stored
ALLOW/BLOCK policies are inert configuration and do not publish Gateway grants.
There is no custom-code execution, remote schema fetch, approval or deployment
lifecycle path.

External PostgreSQL/Flyway V1/V2 stores indexed tenant/kind/id JSONB records.
Per-tenant transaction locks, consistent snapshot reads, optimistic revisions,
reference checks, append-only audit and durable replay responses preserve
correctness across replicas. Failed audit/replay/data writes roll back together.
Deleted identities remain retired to prevent stale-revision reuse. Safe JSON/YAML
imports provide deterministic dry-run/diff/MERGE/REPLACE with one atomic commit.
Applied no-op imports are audited. Replay is actor/tenant scoped, retains seven
days, and has a 10000-live-key quota.

Private management probes/Prometheus and structured metadata logs are separate
from the administrative API. W3C trace context propagates; the compiled CDI OTLP
exporter has an explicit disabled-by-default runtime gate. Before export, the SDK
customizer removes raw URLs/paths/query strings, events, links, exception
descriptions and trace-state, preserving trace IDs, timing, fixed span name,
method/status and server request ID. Actual OTLP protobuf receiver tests verify
the production exporter and redaction, alongside SDK unit tests.

Requests, nesting, tenant graph, pages, connection pool, SQL/lock/socket deadlines
and shutdown are bounded. Invalid configuration fails startup before readiness.
The initial foundation deliberately supports at most 512 active records/1 MiB
serialized records per tenant; fleet reporting is outside this module.

Product/contracts/chart advance additively to 0.3.0-dev, retaining v1 wire paths.
Twenty-one new models/enums bring the shared round-trip corpus to 45 across eleven
schema groups. All 108 generated outputs are deterministic, and frozen foundation
compatibility fixtures remain unchanged. Project and actual published-Maven-JAR
dependency modes pass; an empty artifact repository fails without project fallback.

The pinned multi-stage image uses a JRE-only non-root runtime, read-only root,
dropped capabilities, bounded tmpfs and external credentials/key mounts.
Third-party JAR/Temurin notices, dependency versions and exact source/license
review URLs are retained. Nine unmodified Jakarta/Parsson dependencies select
their EPL-2.0 option through exact-version/scope reviews; the classfile backport
Classpath-exception review applies only to build tooling. It is absent from the
366-component image SBOM. Unknown/unreviewed runtime license expressions still
fail the scan.

The opt-in [Control chart](../../../deploy/helm/olo-toolgate/README.md) includes
Deployment, Services, ServiceAccount, ConfigMap, NetworkPolicy, PDB, optional HPA,
TLS Ingress and ServiceMonitor, configurable placement/resources/probes and
external Secret/DB/collector references. Disabled defaults remain empty.
Kind verifies two ready replicas, the signed-token API suite, configuration upgrade,
persisted records and rollback. Default Kind CNI is not a proof of production
NetworkPolicy enforcement.

The protected [Control CI workflow](../../../.github/workflows/control.yml)
builds/tests/scans the actual image, retains its SBOM/checksums, exercises Kind and
publishes the exact saved image with provenance. Existing foundation release
plumbing includes the new raw contracts/OpenAPI, compatibility metadata and
distinct Gateway/Control upgrade assets. Remote Maven/GHCR/Helm/GitHub operations
remain protected CI-only and were not invoked locally.

## Definition of done

| Area | Applied result |
|---|---|
| Implementation | Real primary CRUD/config/audit path, no production mock/bypass or blocking TODO; shared contracts and ownership/layering preserved. |
| Tests | Domain, error, schema/compatibility/drift, real PostgreSQL migration/concurrency/atomicity, signed-token HTTP, real OTLP, container and Kind checks pass. No UI/Endpoint product E2E or new latency-critical benchmark applies. |
| Code quality | Copyright/SPDX, documented sensitive APIs, centralized/locked dependencies, no wire-model duplication or secret telemetry; Java/Rust/TS/PHP/workflow/quality gates pass. |
| Operations | Private liveness/readiness/metrics, JSON logs, verified W3C/OTLP propagation/redaction, dependency failure/recovery, bounded graceful shutdown. |
| Deployment | Pinned production image, actual non-root/read-only smoke, configurable secure Helm resources/probes/HA and explicit network/secret references. |
| Release | Synchronized SemVer, local Maven proof, OCI pipeline/metadata, raw release bundle/chart, SHA-256, dependency/image SBOM and protected provenance pipeline updated. Signing remains unconfigured. |
| Documentation | Component/root README, architecture/ADR, canonical/API docs, config, imports, troubleshooting, compatibility, HA and migration/rollback guides updated. |
| Proof | Executed commands/results below, every unique requirement classified, and suggested commit provided. |

## Executed verification

Executed on Windows/Docker Linux using Java 21, Gradle 9.7.1, Rust 1.94.1,
TypeScript 5.9.3, PHP Docker tooling, Python, Helm 3.17.3, Kind 0.27.0 and matching
kubectl/Kubernetes 1.32.2. `TOOLGATE_DOCKER_TOOLS=1` and native tool-path overrides
select installed tools on this host; no required gate was skipped.

| Command | Result |
|---|---|
| `make check PYTHON=<installed Python>` | PASS on final implementation: all workspace/language, real PostgreSQL/HTTP/OTLP, local publication, contract and Helm gates. |
| `python -m unittest discover -s tests/contracts -v` | PASS: 26 schema, compatibility, drift, header/license, version, release, docs, CI and deployment asset checks. |
| `python tools/contracts/version.py --check`; `python tools/contracts/generate.py --check`; `python tools/quality.py` | PASS: synchronized versions, 108 generated outputs, headers/npm licenses/obvious-secret checks. |
| Gradle `projects javaCheck build`, via make | PASS: all Java builds; eight Control JUnit tests, including real PostgreSQL and actual SDK export. |
| `python tools/check.py --publication-only`, also mandatory in make | PASS: local Maven JAR/POM/sources/Javadoc inspection; all three service artifact-mode builds; empty repository rejects. |
| `python tools/control/check.py` | PASS: domain/real DB tests, clean install/V1 upgrade, runtime privileges, concurrent replay, audit failure rollback, retirement, HTTP CRUD/roles/JWT/tenant/revisions/limits/import/audit, private telemetry, DB outage/recovery, real redacted OTLP and invalid startup limits. |
| Cargo fmt/test/clippy workspace commands via make | PASS: 19 existing Gateway tests and two contract tests; no warning or format drift. |
| npm contract check/build/test; PHP round trips and generated source lint via make | PASS: TypeScript wire/type checks; PHP 45-model round trips/security negatives; Java/Rust corpus also passes. |
| `python tools/check.py --scans` | PASS: actionlint, zero npm/pip vulnerabilities, 542-entry resolved license audit, redacted Gitleaks and no HIGH/CRITICAL source dependency findings. Python tool licenses are covered by the explicit resolved audit despite Trivy's container site-packages warning. |
| `python tools/control/helm.py`, also via final make | PASS: six install/upgrade variants including collector egress, strict Kubernetes/ServiceMonitor schemas, ten invalid settings reject; integer limits render correctly. Gateway and empty-default gates also pass. |
| `python tools/control/container.py` | PASS: final production image, non-root/read-only/cap-drop, JWT/tenant/roles/CRUD replay, safe imports/audit, private metrics, redacted logs, requests work after transport 413, SIGTERM exit 143 with completed shutdown, notices/lock inspected. |
| Trivy image `--scanners vuln --exit-code 1 --severity HIGH,CRITICAL`; CycloneDX export | PASS: final image OS/Java graph has zero HIGH/CRITICAL findings; 366-component SBOM, build-only classfile dependency absent. |
| `python tools/control/cluster.py` | PASS: isolated Kind/two-replica real PostgreSQL install, signed-token HTTP suite, persisted configuration upgrade and rollback; owned cluster removed. |
| `python tools/release/bundle.py`; `helm package`; Docker image save; SHA-256 verification | PASS: raw Control contract/OpenAPI/compatibility bundle, 0.3.0-dev chart, saved tested image and SBOM/checksums prepared locally. |
| `git diff HEAD --check` and final documentation/matrix checks | PASS. |

Operational polling observes readiness/rollout state with deadlines; correctness
does not depend on arbitrary sleeps. Verification caught and fixed the Flyway
clean-install ordering, framework auth envelopes, trusted OpenAPI alias parsing,
fixture counts, PostgreSQL init-server readiness race, Docker partial multi-platform
archive import, build-time exporter selection and Helm scientific integer rendering.
Invalid limits are now checked eagerly at startup. The oversized-body case runs
last for kubectl tunnels because early backend rejection can close that tunnel;
direct host/container tests prove the application remains healthy afterward.

Final tested/scanned image ID:
`sha256:6f40aeff80654996335a984564a60512b85a15a9b725c66b086116edb619b1e7`.
Ignored local proof is in `build/control/`: HTTP/container/cluster reports,
rendered variants, image-scan.json, control-sbom.cdx.json, image.tar and SHA256SUMS;
raw/chart/dependency-license assets are in `build/release/`.
The image was built from the uncommitted module work; it is local verification
evidence, not a published release. No remote job, push, tag, release or provenance
issuance is claimed.

## Practical boundaries

Production requires an external IdP, PostgreSQL, separate schema-owner/runtime
credentials, TLS/Secret provisioning and a CNI that enforces NetworkPolicy.
Directory mutations do not revoke IdP accounts or distribute Gateway policy.
Audit/retired identities persist and require external capacity/retention planning.
Helm rollback restores application/configuration, not the database schema; recovery
uses a coordinated restore or forward migration. No prior Module-02 runtime image
exists for a schema downgrade. Collector availability is separate from durable
mutation auditing.

See [configuration](../../control-plane/configuration.md),
[API](../../api/control-plane-api.md) and
[deployment/upgrades](../../control-plane/upgrades.md).

## Requirement completion matrix

All 104 unique IDs are classified: **95 COMPLETE, 9 NOT_APPLICABLE, 0 BLOCKED**.
The three examples in the traceability template are not additional requirements.

| Requirement | Status | Evidence or scope reason |
|---|---|---|
| ARC-001 | COMPLETE | Control owns organization administration; no execution, bundle publication or fleet workflow. ADR 003 and component docs. |
| ARC-002 | COMPLETE | Pure domain, application ports and HTTP/JDBC adapters; no framework in domain. |
| ARC-003 | COMPLETE | Store/Codec ports isolate PostgreSQL, serialization and transport. |
| ARC-004 | COMPLETE | Generated shared Control DTOs and canonical schemas; no private wire models. |
| ARC-005 | COMPLETE | Versioned Maven identity and successful project/published modes. |
| ARC-006 | NOT_APPLICABLE | No approval, enrollment, deployment or execution lifecycle state machine in this CRUD/config foundation. |
| ARC-007 | COMPLETE | Two stateless replicas; all audit, revision and replay state in PostgreSQL; Kind proof. |
| CON-001 | COMPLETE | control.schema.json and canonical control-v1.yaml under packages/contracts. |
| CON-002 | COMPLETE | Local JAR/POM/sources/Javadoc proof for io.ololabs.toolgate:toolgate-contracts:0.3.0-dev. |
| CON-003 | COMPLETE | Full project and published-JAR service builds; empty repository rejects. |
| CON-004 | COMPLETE | Frozen v1 corpus/schema compatibility and 45-model language round trips. |
| CON-005 | COMPLETE | 108 generated outputs verified without mutation; deterministic drift negatives. |
| CON-006 | COMPLETE | Synchronized 0.3.0-dev product/contracts/chart and v1 compatibility metadata. |
| SEC-001 | COMPLETE | Verified identity and role checks; tenant isolation; no ASK grants or default accounts. |
| SEC-002 | COMPLETE | Closed credential-free config; log, trace and image checks exclude secrets. |
| SEC-003 | COMPLETE | Real RS256 verification; wrong key, issuer, audience, expiry and claim negatives. |
| SEC-004 | COMPLETE | No Marketplace trust import or deployment approval inferred from directory metadata. |
| SEC-005 | COMPLETE | Stored policies do not become runtime grants; administrative audience is distinct. |
| SEC-006 | NOT_APPLICABLE | No untrusted custom-code execution; schemas/configuration are inert data and are never run. |
| SEC-007 | COMPLETE | Offline schema references; safe YAML; no shell, archive, network schema fetch or tool execution. |
| SEC-008 | COMPLETE | Only mounted public administrative verification key; no runtime/signing key reused. |
| SEC-009 | COMPLETE | Atomic append-only mutation audit with hashed actor and request digest; DB privileges tested. |
| SEC-010 | COMPLETE | Durable tenant/actor/key replay, conflict on changed request, revisions and retired IDs. |
| TST-001 | COMPLETE | DomainTest (five cases) and TraceRedactionTest; eight Control JUnit tests in total. |
| TST-002 | COMPLETE | HTTP/schema/roles/limits/unsafe formats/stale revisions/reference/DB failure negatives. |
| TST-003 | COMPLETE | Real PostgreSQL 17.11, actual Quarkus HTTP/OTLP, production container and Kind. |
| TST-004 | COMPLETE | Shared schema fixtures, all-language round trips, frozen compatibility and drift. |
| TST-005 | COMPLETE | JWT/role/tenant/replay/DB privilege/atomic failure and telemetry redaction tests. |
| TST-006 | NOT_APPLICABLE | No Admin UI, Gateway policy distribution or Endpoint cross-component product workflow exists in Module 02; real HTTP/container/cluster integration is covered. |
| TST-007 | NOT_APPLICABLE | Administrative CRUD is not the performance-sensitive authorization hot path; bounded limits/concurrency are tested and existing Gateway tests remain intact. |
| TST-008 | COMPLETE | Concurrency uses futures and database locks; startup/rollout polls actual state with deadlines. |
| QLT-001 | COMPLETE | Headers checked, including SQL and properties; generated notices retain SPDX. |
| QLT-002 | COMPLETE | Security-sensitive adapter/domain APIs, ADR 003, canonical API and operations docs. |
| QLT-003 | COMPLETE | Java -Xlint:all/-Werror; Rust fmt/clippy; TS/PHP checks; actionlint and quality gates. |
| QLT-004 | COMPLETE | Central BOM/catalog and locked scopes; exact-version dependency license review and scans. |
| QLT-005 | COMPLETE | Small domain/use-case/adapter types; no service locator or mutable global correctness state. |
| QLT-006 | COMPLETE | Shared ErrorEnvelope/ErrorCode; API responses exclude exception text. |
| API-001 | COMPLETE | /api/control/v1 paths for six record kinds, audit, import/export and OpenAPI. |
| API-002 | COMPLETE | Canonical shared-model OpenAPI; protected served document embeds all referenced schemas. |
| API-003 | COMPLETE | Closed canonical validation, strict JSON, safe YAML, bounded graph and typed IDs. |
| API-004 | COMPLETE | Stable ErrorCode mapping and explicit early transport 413 limitation. |
| API-005 | COMPLETE | Server request ID in responses/audit/logs; trusted W3C trace context propagated. |
| API-006 | COMPLETE | Create/update/delete/applied imports require durable Idempotency-Key; replay tested. |
| API-007 | COMPLETE | Generic mapped errors; safe call-site telemetry; no stack trace, payload or credentials. |
| DAT-001 | COMPLETE | Flyway V1 and V2; no ORM auto-DDL/clean/baseline/repair. |
| DAT-002 | COMPLETE | Fresh unique PostgreSQL databases; clean install and actual runtime grants. |
| DAT-003 | COMPLETE | Seeded V1 row survives V2 migration; validation/checksum gate. |
| DAT-004 | COMPLETE | Tenant advisory transaction lock, snapshot reads, optimistic revisions, atomic audit/replay; concurrent and rollback tests. |
| DAT-005 | COMPLETE | External PostgreSQL; no database/credentials generated by production chart. |
| OBS-001 | COMPLETE | Private management liveness; live endpoint HTTP test. |
| OBS-002 | COMPLETE | PostgreSQL readiness; paused dependency yields 503 and recovers. |
| OBS-003 | COMPLETE | Structured JSON fixed metadata and safe failure origins; log redaction proof. |
| OBS-004 | COMPLETE | Private Prometheus framework/custom metrics; fixed kind/operation tags. |
| OBS-005 | COMPLETE | W3C context in logs and real OTLP protobuf with original trace ID. |
| OBS-006 | NOT_APPLICABLE | No asynchronous jobs are introduced. |
| OBS-007 | COMPLETE | SDK exporter removes URL/path/query/events/links/descriptions/trace-state; real receiver and SDK tests. |
| DKR-001 | COMPLETE | Pinned JDK build and pinned JRE runtime stages. |
| DKR-002 | COMPLETE | JRE-only image, without Gradle/build tools or build-only classfile dependency. |
| DKR-003 | COMPLETE | UID/GID 65532 with dropped capabilities; real smoke. |
| DKR-004 | COMPLETE | External credentials/public key; ephemeral tests; no image secret layer. |
| DKR-005 | COMPLETE | Version/revision/source/license OCI labels; synchronized VERSION. |
| DKR-006 | COMPLETE | CI image scan and 366-component CycloneDX SBOM; final local scan clean. |
| DKR-007 | COMPLETE | SIGTERM shutdown completed; container exit 143, not forced kill. |
| DKR-008 | COMPLETE | Read-only root, bounded writable tmpfs, actual successful HTTP smoke. |
| K8S-001 | COMPLETE | Real Control Deployment, Services, ConfigMap, ServiceAccount and NetworkPolicy. |
| K8S-002 | COMPLETE | Configurable resource requests/limits; schema/render validation. |
| K8S-003 | COMPLETE | Non-root/read-only/cap-drop/seccomp and no SA token; schema/render and container proof. |
| K8S-004 | COMPLETE | Private startup/readiness/liveness probes; actual two-ready-replica smoke. |
| K8S-005 | COMPLETE | Dedicated ServiceAccount, token automount disabled. |
| K8S-006 | COMPLETE | Default-deny ingress/egress; explicit DB/DNS/collector peers; Kind does not prove CNI enforcement. |
| K8S-007 | COMPLETE | Configurable PDB; default two replicas/minAvailable 1. |
| K8S-008 | COMPLETE | Optional autoscaling/v2 CPU HPA with validated bounds. |
| K8S-009 | COMPLETE | Node selectors, tolerations, affinity and topology spread options. |
| K8S-010 | COMPLETE | Public-key, DB login/migrator and DB CA references; no generated Secrets. |
| K8S-011 | COMPLETE | Optional class/TLS ingress; strict render and negative tests. |
| K8S-012 | COMPLETE | Optional ServiceMonitor validated with upstream CRD schema and private metrics. |
| K8S-013 | COMPLETE | Strict helm lint passes. |
| K8S-014 | COMPLETE | Six install/upgrade variants, kubeconform and ServiceMonitor schemas; ten negative cases. |
| K8S-015 | COMPLETE | Kind 1.32.2 installation and signed-token HTTP suite with two replicas. |
| K8S-016 | COMPLETE | Limit upgrade/rollback retains records and restores 512; DB rollback limits documented. |
| REL-001 | COMPLETE | Existing protected foundation Maven pipeline updated shared contracts; local proof passes. |
| REL-002 | COMPLETE | Protected control-release exact saved/scanned GHCR image pipeline; no local push. |
| REL-003 | COMPLETE | Versioned opt-in Helm OCI packaging in protected foundation pipeline. |
| REL-004 | COMPLETE | Raw Control schemas/OpenAPI, compatibility and distinct component upgrade notes in release assets. |
| REL-005 | COMPLETE | SHA-256 asset generation and local verification; tested exact-image save/load pipeline. |
| REL-006 | COMPLETE | Dependency inventory and final image CycloneDX SBOM; CI publishes evidence. |
| REL-007 | COMPLETE | Pinned GitHub provenance actions for image digest/SBOM and foundation assets; issuance CI-only. |
| REL-008 | NOT_APPLICABLE | Image/artifact signing is not configured; provenance pipeline is distinct and no signature claim is made. |
| REL-009 | COMPLETE | 0.3.0-dev CHANGELOG documents directory/security/deployment scope. |
| REL-010 | COMPLETE | Compatibility matrix/raw release metadata includes Control, PostgreSQL and V2 schema. |
| REL-011 | COMPLETE | HA, backup/restore responsibility, additive migration and Helm rollback guide. |
| DOC-001 | COMPLETE | README, contributor checks and foundation/release workflow updated. |
| DOC-002 | COMPLETE | Control application and docs/control-plane README updated. |
| DOC-003 | COMPLETE | JWT, DB roles/TLS, limits, ports and tracing settings documented. |
| DOC-004 | COMPLETE | CRUD/revision/replay/import/export/audit and canonical OpenAPI documented. |
| DOC-005 | COMPLETE | ADR 003, root architecture and compatibility baseline updated. |
| DOC-006 | COMPLETE | Validated dry-run example has no real secrets or executable code. |
| DOC-007 | COMPLETE | Private probes, troubleshooting, limits, HA/migrations/rollback and external dependencies documented. |
| PRF-001 | NOT_APPLICABLE | No change to the normal Gateway authorization path; Control legitimately uses its external database. |
| PRF-002 | COMPLETE | 512 active records/1 MiB tenant graph, 2 MiB request, depth 32, 100-row pages, bounded pool/timeouts. |
| PRF-003 | COMPLETE | External DB correctness state and transaction locks; two replicas survive configuration rollouts. |
| PRF-004 | NOT_APPLICABLE | Fleet-scale asynchronous reporting is outside Module 02; bounded directory pages are provided. |
| PRF-005 | NOT_APPLICABLE | No new latency-critical benchmark path; no fabricated benchmark or fleet performance claim. |
| PRF-006 | COMPLETE | Connection pool/SQL/lock/socket deadlines, upload limits, live replay quota and bounded imports. |

## Suggested commit

```text
feat(control): implement control plane backend foundation
```

No commit, tag or remote publication was performed by this module task.
