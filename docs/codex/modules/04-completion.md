# Module 04 completion report

Status: **DONE for Module 04**, 2026-10-03. The master prompt, requirements traceability, governing
documents, dependencies and [definition of done](../08-DEFINITION-OF-DONE.md) were
read before implementation. The [coverage plan](04-coverage.md) and
[ADR 005](../../adr/005-signed-policy-bundles.md) preceded implementation.
Modules 01 and 02 are the dependencies. No later module was started.

## Implementation and review

The canonical [bundle contract](../../../packages/contracts/schemas/v1/bundle.schema.json)
defines a compact JWS envelope, protected header, tenant-bound payload, compiled
policy and publication request. Product/contracts/chart are `0.4.0-dev`; policy
format remains v1 and bundle versions are `1.0.<sequence>`. Existing v1 definitions
remain compatible. The contract set now contains 12 schema groups and 52 models
and enums, with 123 deterministic generated Java/Rust/TypeScript/PHP outputs.

Control's application layer owns deterministic compilation and synchronous
publication through `Codec`, `BundleSigner` and `Store` ports. Enabled policies
are sorted; team members and identity selections are normalized deterministically;
exact tool/action/resource matching and BLOCK precedence are preserved. Selected
empty teams cannot become unrestricted grants. Unsupported references or semantics
reject compilation. Grace is an explicit authenticated administrator assertion
through `gracePolicyIds`, signed as `graceAllowed`. All other ALLOW rules block at
expiry. ASK remains unsupported in this module.

The JCA adapter signs the exact UTF-8 payload using RS256 with an external,
dedicated RSA key. Header type, algorithm, key ID, issuer, audience, tenant,
sequence, source revision, issue/expiry, policy SHA-256 and rollback relationship
are verified by Gateway before adoption. Unknown fields, duplicate JSON keys,
noncanonical base64url, unsupported formats and remote key references reject.
The compiler and verifier import canonical generated models rather than copying
wire types. The operational public keyring is local configuration, not a fetched
trust source.

Flyway V3 adds immutable tenant-scoped publication history. Tenant locking
serializes directory writes and publishers. Publication, audit and durable replay
commit together; signing or audit/database failure leaves no partial publication.
The runtime role has SELECT/INSERT access to history and cannot UPDATE/DELETE it.
Rollback copies historical compiled bytes and source revision into a newly signed,
higher sequence, preserving history rather than rewinding it. Authentication and
authorization precede replay; stale revisions/sequences and changed replay bodies
return stable errors.

Gateway signed and static policy modes are mutually exclusive. Each replica polls
one configured TLS destination, refreshes its bounded external JWT file, checks
status/media type/streamed size and enforces deadlines. Redirects and ambient
proxies are disabled; metadata and unsafe literal addresses reject. Only explicitly
configured literal loopback HTTP is available to isolated development. Production
TLS termination, DNS and enforcing egress are operator inputs.

Verification constructs a complete immutable snapshot before a short swap lock.
Evaluation uses memory only, with no database, network, signature verification or
compilation on the authorization path. Post-audit reevaluation closes the ALLOW
race with replacement or expiry. EMPTY/FRESH/GRACE/EXPIRED states control readiness
and bounded metrics. Invalid updates preserve last-known-good only through its
existing deadline; wall-clock high-water and monotonic deadlines prevent clock
rollback extending validity. Grace permits only explicitly classified low-risk
reads. Restart begins EMPTY and requires a fresh verified current bundle.

Helm references three independent external inputs: Control signing Secret,
Gateway public-keyring Secret and Gateway fetch-token Secret. Signing material is
never mounted into Gateway. Signed mode removes static policy, requires intentional
Control/DNS egress and retains restricted security contexts, probes, resources,
SA/PDB/HPA and placement controls. Secret revision values trigger rollouts.
Workloads remain opt-in and default rendering remains empty.

Review checked trust-domain separation, replay/equivocation rejection, rollback
semantics, transactional failure, compilation bounds, atomic replacement and
in-flight authorization. Dedicated policy keys must remain separate from IdP,
Marketplace, deployment, permit, device and release keys. Control rejects reuse
of its configured IdP modulus; operators remain responsible for separation from
other systems. No unsigned fallback, production mock, code execution or temporary
authorization bypass is introduced.

## Verification executed

| Command/gate | Result |
|---|---|
| `make check PYTHON=<installed Python>` with `TOOLGATE_DOCKER_TOOLS=1` | PASS: Gradle workspace/builds, real PostgreSQL/HTTP/OTLP, local Maven publication and all artifact-mode services, missing-artifact rejection, Rust checks, four-language round trips, UI, headers/docs and all Helm gates. Log: `build/policy/make-check.log`. |
| `python -m unittest discover -s tests/contracts -v` | PASS: 35 schema/compatibility/drift/version/license/header/docs/CI tests. Final rerun after OpenAPI formatting passed; `build/policy/final-contracts.log`. |
| `python tools/control/check.py` | PASS: 11 JUnit tests (three new bundle tests/eight inherited), real PostgreSQL clean/V2→V3 upgrade, six concurrent publishers, audit/signing failure rollback, immutable grants and signed-token HTTP/OTLP regression. Final canonical OpenAPI/resource rebuild passed in `build/policy/control-final.log`. |
| Rust workspace tests, fmt and clippy through make | PASS: 28 Gateway tests (eight bundle, one HTTP-fetch, 19 inherited) and two Rust contract tests; `cargo fmt --all --check` and `cargo clippy --workspace --all-targets --locked -- -D warnings`. |
| Four-language generated contracts/publication | PASS: 12 schema groups, 52 models/enums and 123 deterministic outputs; Java/Rust/TypeScript/PHP corpus and original frozen v1 compatibility. Java 0.4.0-dev JAR/POM/sources/Javadoc locally published and consumed by all three Java service builds. |
| `python tools/policy/check.py --build` | PASS: production Control/Gateway images plus real PostgreSQL; genuine JCA→Python/ring signature/hash verification, roles/tenant/idempotency, ALLOW→BLOCK atomic update, forward rollback, key overlap/rotation, credential refresh, Control outage, classified read grace, expiry BLOCK, restart EMPTY and fresh recovery; durable audit/redacted logs. `build/policy/e2e.json`. |
| Helm lint/render/Kubeconform through make | PASS: empty defaults, inherited workload variants, and three signed install/upgrade/rotation variants with 14 valid objects each. Eight signed-mode negative configurations reject. |
| `python tools/control/cluster.py` | PASS: isolated Kind 0.27.0/Kubernetes 1.32.2, two replicas, real PostgreSQL, external signing Secret, independently verified publication and unchanged history after Helm upgrade/rollback. Final Control image tested; owned cluster removed. `build/control/cluster-smoke.json`. |
| `python tools/gateway/benchmark.py` | PASS: genuine signed 512-rule full scan, 1,000 warmups/20,000 samples, fixed UTC/monotonic validity and matched-version assertions. p50 9,703 ns, p95 39,251 ns, p99 60,599 ns, mean 15,864 ns; cold adoption 9,349,243 ns. `build/gateway/bundle-benchmark.json`. |
| `python tools/check.py --scans` | PASS: actionlint, no npm/pip known vulnerabilities, 757-entry resolved license inventory, no Gitleaks findings, public-JWS allowance positive/negative proof, and no HIGH/CRITICAL source vulnerability/license findings. Final combined log: `build/policy/scans-final.log`. |
| Gateway/Control container smoke with `--no-build` | PASS: final image IDs match E2E; non-root/read-only roots, auth/probes/metrics, no secret logging, upstream notices and graceful SIGTERM (Gateway 0; Control 143 with shutdown completed). |
| Trivy 0.61.1 final image scans/CycloneDX SBOM | PASS: zero HIGH/CRITICAL vulnerabilities in both images. Gateway inventory 222 components; Control 371 after merging the five-component embedded UI production graph. Image scan JSON and SBOMs under respective `build/` component folders. |
| `python tools/release/bundle.py --output build/policy/release`; Helm package; checksums | PASS: versioned raw contracts, compatibility metadata (bundle v1/RS256/Flyway V3), chart, Java main/sources/Javadoc, resolved license inventory, both SBOMs and nine SHA-256 asset entries. Generated drift/header/whitespace checks pass. |

Final tested image IDs are `sha256:c0126d1bf07736ee7cd42902c5ebe2cdd2bccf3204eb0630f5cb9b749237c19e`
(Gateway) and `sha256:03b9db3884a8b5a52637de22012aa3f2f61578c65503448e9141ae6c38e59c75`
(Control), local tags `olo-toolgate-gateway:module04` and `olo-toolgate-control:module04`.
The completion date follows the client timezone; preparation and initial tests were
on 2026-10-02. Remote publication/GitHub jobs were configured and locally validated,
not executed.

The signed microbenchmark measures snapshot acquisition, exact-match scanning and
decision allocation on Rust 1.94.1 release in Docker `rust:1.94-bookworm`, Windows
11, Intel Family 6 Model 85 Stepping 4. It excludes production clock sampling,
extraction, schemas, network and audit I/O; cold adoption is separate. These are
local observations, not production throughput/latency promises. Correctness has
no noisy latency threshold. Unit clocks are injected; real E2E observes bounded
state transitions.

Review caught and fixed key-size disagreement (both services now accept RSA
2048/3072/4096 with exponent 65537), concurrent ingress clock handling, monotonic
expiry, and benchmark expiry shortcuts. No unresolved material review findings
remain. The CI compatibility pair is built from the same revision in its own job;
the image publishers separately preserve the exact scanned/smoked image artifacts.

License review retained the published MIT/Unlicense declarations for locked
same-file/walkdir and the exact CDLA-Permissive-2.0 certificate dataset license.
Public JWS fixture bytes are not credentials; Gitleaks's allowance is limited to
its JWT rule, exact fixture paths and `jws` lines. A credential field and private
key in those same paths still fail. Windows mount traversal hit a scanner timeout;
the gate now scans every tracked/modified and untracked non-ignored source file
in an immutable Linux snapshot, with no source/license finding exclusions. Actual
runtime artifacts retain separate image scans. The final combined scan passed.

## Definition of done applied

- [x] Implementation: complete signed primary path; shared contracts and component ownership; no production mock, unsigned fallback, insecure bypass or blocking TODO.
- [x] Tests: units, negative/security/contracts, real DB/HTTP/crypto boundaries, cross-component E2E, compatibility, deterministic clock/concurrency tests and benchmark.
- [x] Quality: copyright/SPDX, documented security APIs, centralized justified dependencies, typed errors, no secret telemetry, drift/static/type/lint checks.
- [x] Operations: health/readiness, lifecycle logs/metrics, correlation, shutdown, outage/expiry/recovery and HA behavior.
- [x] Deployment: production images, non-root/read-only runtime, external secrets/state, Helm security/resources/probes/SA/egress/PDB/HPA/placement and install/upgrade/rollback evidence.
- [x] Release: synchronized versions, local published artifacts, protected Maven/GHCR/Helm/assets pipelines, mandatory compatibility gate, SBOM/checksums/provenance and upgrade guidance.
- [x] Documentation/proof: README/component/API/config/protocol/ADR/security/runbooks, all command results, individual traceability and suggested commit. No later module started.

## Release, operations and limits

The reusable `.github/workflows/policy.yml` gate builds production images and
checks cross-component compatibility, Helm configuration and the signed evaluator
benchmark. Protected Gateway/Control image publishers and foundation Maven/chart/
asset release depend on it. Existing GHCR/OCI identities, checksums, image scans,
SBOM and provenance remain in place. Maven remote credentials remain CI-only.
Runtime JWS signing is implemented; release image signing remains unconfigured.
No local push, tag, remote publication or GitHub Actions run is claimed.

[Bundle operations](../../control-plane/policy-bundles.md) documents the protocol,
API/configuration, expiry/grace, polling, rotation, revocation, compromise handling,
backup restore and rollback. Publication is explicit: editing directory policies
does not itself revoke an already signed snapshot. Choose TTL and grace according
to revocation needs. Replicas can temporarily differ while polling, so observe
every replica before declaring a rollout complete. Fetch JWT rotation is external;
keyring changes require a restart.

Process-local anti-replay state is not persisted. Database restoration requires an
explicit minimum sequence and reviewed sequence advancement; restarting a
stateless Gateway alone does not retain the previous floor. Application/Helm
rollback does not undo Flyway or publication history. Frozen earlier contracts
remain compatible, but older runtime software has no signed-bundle reader: no
N/N-1 signed-runtime compatibility is claimed. Kind verifies Secret mounts and
publication/history behavior; production TLS/egress requires an operator-provided
endpoint and enforcing CNI.

Evidence is retained in ignored `build/policy/`, `build/gateway/`, `build/control/`
and the final release assets in `build/policy/release/`. Ephemeral test private keys never enter source/release
fixtures. Frozen public signature vectors are explicitly regenerated only for
reviewed changes. Tests clean up only their owned containers/processes/cluster.

## Individual requirement matrix

All **104 unique IDs** are listed: **101 COMPLETE, 3 NOT_APPLICABLE**,
with scope reasons. No BLOCKED requirement remains.

| Requirement | Status | Evidence / scope reason |
|---|---|---|
| ARC-001 | COMPLETE | `PolicyCompiler`/`BundleService` own Control compilation/publication; `apps/gateway/src/bundles.rs` owns verification/evaluation; ADR 005. |
| ARC-002 | COMPLETE | Application signing/store/codec ports and bounded adapter responsibilities; compiler/signer/store tests in `PolicyBundleTest.java`. |
| ARC-003 | COMPLETE | `BundleSigner`, `Codec`, `Store`, `PolicyEvaluator` and injected clocks isolate external boundaries; deterministic Java/Rust tests. |
| ARC-004 | COMPLETE | Canonical `bundle.schema.json` and generated language models are imported by both services; OpenAPI reuse/drift tests. |
| ARC-005 | COMPLETE | Stable published Maven/npm/Cargo contract identities; local artifact service proof; ADR 005 describes independent service boundary. |
| ARC-006 | COMPLETE | Explicit transactional publication/rollback and EMPTY/FRESH/GRACE/EXPIRED lifecycle; Java atomic-failure and Rust state-transition tests. |
| ARC-007 | COMPLETE | PostgreSQL owns durable history; Gateway replicas hold ephemeral verified snapshots; production E2E restart and two-replica Kind evidence. |
| CON-001 | COMPLETE | `packages/contracts/schemas/v1/bundle.schema.json` is language-neutral canonical source. |
| CON-002 | COMPLETE | `packages/contracts/java/build.gradle.kts` retains `io.ololabs.toolgate:toolgate-contracts`; local Maven publication gate. |
| CON-003 | COMPLETE | `tools/check.py` workspace/published dependency modes build service scaffolds and reject a missing artifact. |
| CON-004 | COMPLETE | Frozen v1 corpus plus genuine `tests/fixtures/policy/signed-v1.json`; `test_policy_bundles.py` compatibility checks. |
| CON-005 | COMPLETE | `tools/contracts/generate.py --check`; generated manifest and four-language 52-model round trips. |
| CON-006 | COMPLETE | `tools/contracts/version.py --check` synchronizes `0.4.0-dev`; signed format v1 and monotonic bundle SemVer documented. |
| SEC-001 | COMPLETE | EMPTY/expired/invalid bundles BLOCK; BLOCK precedence and signed/static exclusivity; Rust bundle and E2E outage/restart tests. |
| SEC-002 | COMPLETE | External private keys/JWT files, fixed rejection reasons and redacted E2E logs; `tools/policy/check.py` secret-output assertions. |
| SEC-003 | COMPLETE | Exact JWS RS256 verification before payload parsing plus policy SHA-256; forged/hash/algorithm/header tests in Rust/Python/Java. |
| SEC-004 | COMPLETE | Protected policy header type and local dedicated keyring bind bundle trust; `test_trust_domains_and_algorithms_are_not_interchangeable`. |
| SEC-005 | COMPLETE | Bundles authorize policy configuration only; no deployment assignment or execution permit; ADR 005 and bundle operations. |
| SEC-006 | NOT_APPLICABLE | No untrusted tool/uploaded code execution or sandbox path is implemented in Module 04. |
| SEC-007 | COMPLETE | Fixed HTTPS destination, unsafe literal/metadata rejection, no redirects/proxy, strict JSON and size/time limits; Rust source/fetch negatives. |
| SEC-008 | COMPLETE | Dedicated external policy keys; `BundleBootstrap` rejects IdP modulus reuse; rotation/unknown-key/header-type tests and key-management docs. |
| SEC-009 | COMPLETE | `BUNDLE_PUBLISH`/`BUNDLE_ROLLBACK` audit commits with history/replay; Java audit-failure rollback and E2E durable audit assertions. |
| SEC-010 | COMPLETE | Expected revision/sequence, actor-scoped durable idempotency, monotonic/equivocation rejection; concurrent Java and Rust/E2E replay tests. |
| TST-001 | COMPLETE | Three new `PolicyBundleTest` tests, eight Rust bundle tests and inherited component/unit suites. |
| TST-002 | COMPLETE | Signature/hash/header/schema/key/trust/time/rollback/ref/role failures and HTTP status/media/body negatives. |
| TST-003 | COMPLETE | Real PostgreSQL transaction/migration tests and real production Control/Gateway images in `tools/policy/check.py`. |
| TST-004 | COMPLETE | Canonical schema/OpenAPI tests, all-language round trips, generated drift and public signed compatibility corpus. |
| TST-005 | COMPLETE | Wrong signature/key/type/tenant, expiry/replay, role denial, immutable DB grants and log-redaction tests. |
| TST-006 | COMPLETE | Genuine publication→Gateway update→rollback→rotation→outage/grace/expiry→restart/recovery E2E. |
| TST-007 | COMPLETE | `apps/gateway/benches/authorization.rs` and `tools/gateway/benchmark.py` record signed 512-rule hot-path/cold-verification baseline. |
| TST-008 | COMPLETE | Injected wall/monotonic clocks, barrier-based atomic replacement and bounded state-driven E2E waits; final repeatability gate. |
| QLT-001 | COMPLETE | SPDX/copyright headers on new Java/Rust/Python/workflow files; `tools/check.py` source header gate. |
| QLT-002 | COMPLETE | Javadoc/Rust public trust-boundary comments; ADR 005 and protocol/API/config/operations documentation. |
| QLT-003 | COMPLETE | Final rustfmt/clippy, Java/type/lint, actionlint, schema/generated/header/docs and whitespace gates. |
| QLT-004 | COMPLETE | Central Cargo ring/reqwest/rustls entries, locked dependency graph, scan/license gates; signing uses existing JCA. |
| QLT-005 | COMPLETE | Compiler, transaction service, signing/store adapters and per-replica verified policy state; no service locator/global policy state. |
| QLT-006 | COMPLETE | Existing shared `ErrorEnvelope`/typed `Failure` codes; publication conflict/validation/not-found/unavailable HTTP tests. |
| API-001 | COMPLETE | `/api/control/v1/bundles/current`, `/versions/{sequence}`, `/publish`, `/rollback` in `ControlResource`. |
| API-002 | COMPLETE | Canonical `packages/contracts/openapi/control-v1.yaml` references shared bundle schemas; Python OpenAPI test. |
| API-003 | COMPLETE | Closed schema, duplicate-key rejection, revision/sequence/lifetime/grace and risk classification validation; Java/Python/Rust negatives. |
| API-004 | COMPLETE | Stable 400/401/403/404/409/503 behavior through existing typed error path; real API/E2E assertions. |
| API-005 | COMPLETE | Inherited validated request IDs, audit request reference and HTTP trace propagation; API regression and bundle fetch trace header. |
| API-006 | COMPLETE | Required `Idempotency-Key`; same actor/body returns original signed bytes, changed replay conflicts; Java concurrency and real API tests. |
| API-007 | COMPLETE | Safe typed failures and fixed fetch/verifier logs omit exception bodies, JWTs and key material; redaction/secret gates. |
| DAT-001 | COMPLETE | `V3__immutable_policy_bundles.sql` adds history and permitted audit operations with explicit grants. |
| DAT-002 | COMPLETE | `PostgresTest` and `PolicyBundleTest` apply fresh V1→V3 against real PostgreSQL; container/E2E clean setup. |
| DAT-003 | COMPLETE | Real V2→V3 migration, existing-record preservation and Kind persisted upgrade evidence. |
| DAT-004 | COMPLETE | Tenant advisory lock plus single history/audit/replay transaction; concurrent publication, audit failure and signing failure tests. |
| DAT-005 | COMPLETE | Immutable history/replay/audit persist in external PostgreSQL; production pods have no authoritative local disk state. |
| OBS-001 | COMPLETE | Gateway liveness remains healthy through Control outage/expired policy; real E2E management assertions. |
| OBS-002 | COMPLETE | EMPTY/EXPIRED readiness 503, verified fresh/read grace readiness 200; fake-clock and outage/recovery tests. |
| OBS-003 | COMPLETE | Structured adoption/fetch/rejection events with bounded fields; inherited Control structured logging and safe-log E2E. |
| OBS-004 | COMPLETE | `toolgate_bundle_state`, sequence, accepted/rejected/fetch-failed counters on private management; E2E state transitions. |
| OBS-005 | COMPLETE | W3C `traceparent` on fetch and inherited Control correlation/OTLP regression; request ID in mutation audit. |
| OBS-006 | NOT_APPLICABLE | Publication is synchronous/transactional; replica polling is not a durable asynchronous job or fleet workflow. |
| OBS-007 | COMPLETE | No identity metric labels/private keys/tokens/payloads in telemetry; `tools/policy/check.py` and inherited redaction tests. |
| DKR-001 | COMPLETE | Existing digest-pinned Gateway/Control multi-stage Dockerfiles build the new contracts/service code. |
| DKR-002 | COMPLETE | Rust/JRE runtime stages retain no compiler/Node/ephemeral test keys; final production image inspection/scan. |
| DKR-003 | COMPLETE | Both images run UID/GID 65532; production bundle E2E and generic container smoke. |
| DKR-004 | COMPLETE | Test/private credentials mounted externally; no signing key copied into images; image/source scans and redaction checks. |
| DKR-005 | COMPLETE | VERSION/revision/source/license OCI labels remain explicit; final image metadata inspection. |
| DKR-006 | COMPLETE | Existing image workflows enforce HIGH/CRITICAL scans and CycloneDX SBOM; both final images have zero HIGH/CRITICAL findings and complete component inventories. |
| DKR-007 | COMPLETE | Poll shutdown watch plus existing bounded server drain; production container SIGTERM smoke. |
| DKR-008 | COMPLETE | Real E2E uses read-only roots, cap drop and no-new-privileges; external mounts remain read-only. |
| K8S-001 | COMPLETE | Existing Gateway/Control Deployment/Service templates render signed-mode config and external-key mounts. |
| K8S-002 | COMPLETE | Requests/limits retained as values; signed-mode strict render validation. |
| K8S-003 | COMPLETE | Non-root/seccomp/cap-drop/read-only security contexts and fsGroup-readable signing mount; Helm/Kind tests. |
| K8S-004 | COMPLETE | Existing startup/liveness/readiness probes retained; verified policy readiness and Kind-ready deployment evidence. |
| K8S-005 | COMPLETE | Dedicated existing service accounts with token automount disabled; Helm render checks. |
| K8S-006 | COMPLETE | Signed mode requires explicit Control TCP/DNS peers; absent egress rejects; unsafe fetch tests and chart validation. |
| K8S-007 | COMPLETE | Existing replicated Gateway/Control PDB values/templates retained and render-tested. |
| K8S-008 | COMPLETE | Existing optional HPA and replicas/scaling schema checks retained; signed-mode render compatibility. |
| K8S-009 | COMPLETE | Existing topology/affinity/placement values remain compatible; inherited chart variants and HA documentation. |
| K8S-010 | COMPLETE | Independent external signing/keyring/JWT Secret references, read-only mounts and rollout revisions; raw/missing-key negatives. |
| K8S-011 | COMPLETE | Existing optional TLS ingress retained; production bundle URL requires HTTPS endpoint; chart/API docs. |
| K8S-012 | COMPLETE | Existing optional management ServiceMonitor retained; bundle metrics use private management endpoint. |
| K8S-013 | COMPLETE | Final Helm lint for defaults/service/signed modes; default chart remains workload-free. |
| K8S-014 | COMPLETE | `tools/policy/helm.py` validates install/upgrade/rotation with strict Kubeconform and eight negatives; inherited chart suite. |
| K8S-015 | COMPLETE | `tools/control/cluster.py` isolated Kind two-replica install verifies external signing Secret and genuine publication. |
| K8S-016 | COMPLETE | Kind upgrade/Helm rollback preserves exact signed history; forward policy rollback tested separately in real image E2E. |
| REL-001 | COMPLETE | Existing protected Maven publisher plus required policy compatibility gate; local 0.4.0-dev artifact proof. |
| REL-002 | COMPLETE | Gateway/Control protected exact-image GHCR publish jobs now depend on reusable signed-policy gate. |
| REL-003 | COMPLETE | Existing versioned OCI chart naming/metadata; protected foundation release depends on policy gate. |
| REL-004 | COMPLETE | Raw versioned contract bundle includes canonical bundle schema; release compatibility/assets workflow retained. |
| REL-005 | COMPLETE | `tools/release/bundle.py` deterministic tar and `SHA256SUMS`; final reproducibility/checksum tests. |
| REL-006 | COMPLETE | CycloneDX image/dependency inventory covers new Rust dependencies and merged Control UI production graph. |
| REL-007 | COMPLETE | Existing protected image/SBOM attestation pipeline retained; final workflow/actionlint and release metadata review. |
| REL-008 | COMPLETE | Runtime policy JWS RS256 signing/verifying and key rotation implemented; release image signing explicitly remains unconfigured. |
| REL-009 | COMPLETE | `CHANGELOG.md` records Module 04 protocol, publication/distribution and migration behavior. |
| REL-010 | COMPLETE | `docs/architecture/compatibility.md`, frozen fixtures and release metadata distinguish v1 bundles from older unsupported readers. |
| REL-011 | COMPLETE | Control/Gateway upgrade docs and bundle runbook cover V3, format support, key rollout, process floors and rollback/restore. |
| DOC-001 | COMPLETE | Root/component/development docs include check, policy E2E, benchmark and release commands. |
| DOC-002 | COMPLETE | Gateway/Control READMEs and bundle operations explain component ownership and delivered scope. |
| DOC-003 | COMPLETE | Environment/JSON/Helm inputs, bounds, trust files, TLS, JWT refresh and Secret rollout revisions documented. |
| DOC-004 | COMPLETE | Control API/OpenAPI document publish/current/history/rollback, roles, preconditions, replay and safe error behavior. |
| DOC-005 | COMPLETE | ADR 005, architecture ownership/trust and compatibility docs describe transactional signer/verified snapshot design. |
| DOC-006 | COMPLETE | Placeholder public-key/config examples and ephemeral generated test identities only; source secret scan. |
| DOC-007 | COMPLETE | Bundle operations cover EMPTY/expired alerts, JWT/TLS/key diagnosis, outage/grace, compromise, restore and rollback. |
| PRF-001 | COMPLETE | `VerifiedPolicy::decide` reads one in-memory snapshot; compilation/verification/polling stay outside normal authorization path. |
| PRF-002 | COMPLETE | Bounded directory/policy/rules/keyring/JWT/wire sizes, fixed intervals/timeouts and per-replica one-fetch loop. |
| PRF-003 | COMPLETE | Stateless Gateway replicas plus tenant-serialized external PostgreSQL publication; two-replica Kind and HA runbook. |
| PRF-004 | NOT_APPLICABLE | No endpoint fleet reporting, deployment queue or asynchronous fleet workflow is delivered in Module 04. |
| PRF-005 | COMPLETE | `tools/gateway/benchmark.py`, benchmark JSON and Gateway performance docs record host/toolchain/samples/inclusions/exclusions. |
| PRF-006 | COMPLETE | No overlapping polls, total fetch deadline/streamed size caps, compile limits and inherited ingress/rate/concurrency bounds. |

## Changed files and suggested commit

Implementation: canonical bundle schema and four-language generated contracts;
Control compiler/service/signing/store/bootstrap/API and V3 migration; Gateway
verification/polling/snapshot lifecycle, configuration, management metrics and
post-audit check; shared public signature fixtures and Java/Rust/Python tests.
Integration: version/lock files, Helm external trust/egress templates and schema,
policy E2E/render tools, benchmark, container/cluster gates, reusable CI workflow
and protected release dependencies. Documentation: protocol/config/API/operations,
security/architecture/compatibility/release/changelog, ADR and module evidence.
See the repository diff for the exact filename list. No commit or push is claimed.

```text
feat(policy): add signed policy bundle distribution
```
