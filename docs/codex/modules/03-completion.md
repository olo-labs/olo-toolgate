# Module 03 completion report

Status: DONE for Module 03, 2026-10-02. The master prompt, requirements
traceability, governing documents, Module 02 API and definition of done were read
before coding. The [coverage plan](03-coverage.md) and
[ADR 004](../../adr/004-embedded-admin-console.md) preceded implementation.
Module 02 is the dependency. No later module was started.

## Implementation and review

The [React/TypeScript/Vite console](../../../apps/admin-ui/README.md) is embedded
in Control at `/console/`. It implements a signed-token authentication shell,
bounded directory dashboard, cursor-paginated users/teams/tools/policies/clients
(device records)/agents, readable details and user create/edit/delete. Loading,
error and empty states, conflict recovery, deletion confirmation, semantic
landmarks, keyboard focus and responsive layouts are implemented.

The transport wraps deterministically generated canonical OpenAPI operations.
Records **and pages** are imported from `@olo-labs/toolgate-contracts`; no wire
model, JWT verifier, policy evaluator or backend reference/permission rule is
duplicated. Backend 401 ends the session, while 403 and 409 are safe visible
errors. Tokens remain in memory, never storage/cookies/URLs/telemetry. HTTPS is
required outside loopback. Requests stop at 15 seconds/2 MiB and cannot follow
redirects carrying credentials. Exact mutation retries retain an idempotency key;
changed payloads get a new key. Backend revisions and authorization remain final.

Directory users do not provision IdP users or roles. Policy records are stored
configuration, not distributed runtime grants. Device records are not enrollment,
rollout or online state. Tools and policy schemas render as text. Other than user
management, navigation/inspection is the intended foundation scope. There is no
interactive OIDC redirect/login, Quickstart bootstrap, ASK workflow or execution.
The shell uses externally issued signed tokens without a temporary auth bypass.

Gradle builds the console explicitly, checks product/contracts versions and embeds
production assets. The controlled prebuilt mode rejects missing/mismatched assets.
Docker adds a digest-pinned Node build stage; the runtime remains a non-root JRE
with read-only root, dropped capabilities and external secrets. Static serving
adds strict CSP, nosniff, no-referrer and disabled browser device APIs. HTML uses
no-store; hashed assets cache immutably. Source maps/internal Vite manifest are
excluded; Apache and upstream React/React DOM/Scheduler MIT notices are included.

UI dependencies are locked. Runtime adds React/React DOM/Scheduler; Vite,
TypeScript, Vitest/jsdom, Playwright and axe/testing-library are build/test tools.
No router, widget framework or client security engine was added. Permissive
MIT-0/CC0/BlueOak license expressions are checked; MPL-2.0 axe is tooling only.
The resolved audit now distinguishes npm production and development scope and
includes nested installations. Unknown/unreviewed runtime licenses still reject.

The existing Helm Control ingress serves UI and API on one origin, inheriting
replicas, external state, probes, resources, secrets and NetworkPolicy. No new UI
service or workload is invented. Quickstart packaging is documented to reuse
these assets and supply its own identity integration in Module 11.

## Verification executed

| Command/gate | Result |
|---|---|
| `make check PYTHON=<installed Python>` with `TOOLGATE_DOCKER_TOOLS=1` | PASS: full workspace, real PostgreSQL/HTTP/OTLP, local Maven publication, language checks, UI browser/coverage, release packaging and Helm gates. |
| `python -m unittest discover -s tests/contracts -v` | PASS: 30 schema/compatibility/drift/version/license/header/docs/CI checks, including generated operations and SBOM merge relationships/idempotence. |
| `npm run ui:build`; normal/prebuilt Gradle Control builds | PASS: strict TypeScript and production assets; Gradle configuration cache stored/reused; Linux Docker and Windows host builds produce the same asset hashes. |
| `npm --workspace @olo-labs/toolgate-admin-ui test` | PASS: 20 component/API tests; transport coverage 98.63% statements, 98.57% branches, 100% functions/lines (80% enforced threshold). |
| `python tools/ui/check.py --no-build` | PASS: three real Chromium E2E flows against production embedded UI, real PostgreSQL and generated signed identities; CRUD, revision conflict/reload, reader denial, wrong signing key, expiry/401 logout, empty states and 51-record cursor paging. |
| Playwright/axe and manual screenshots | PASS: WCAG A/AA automated tags on login, navigation, data/forms/errors; skip-link/editor focus; desktop and 390px mobile, no page overflow. Automated checks are not full accessibility certification. |
| Existing Control/Gateway regression gates through make | PASS: eight Control JUnit tests and real HTTP/OTLP/DB negatives; 19 Gateway tests, two Rust contract tests, 45-model corpus across all languages; workspace and published Java artifact modes plus missing-artifact rejection. |
| `python tools/check.py --scans` | PASS: actionlint; no npm/pip vulnerabilities; 677-entry resolved license inventory; no Gitleaks findings; no HIGH/CRITICAL filesystem dependency findings. Explicit Python/license audit covers Trivy's site-packages warning. |
| `python tools/control/container.py` | PASS: pinned production image; actual embedded assets/headers; full JWT/tenant/replay/import/audit/metrics boundary; non-root/read-only runtime; safe logs; SIGTERM exit 143 with completed shutdown. |
| Trivy image HIGH/CRITICAL JSON scan | PASS: zero findings; image `sha256:feb47c99ad7521992fa8205cd93276681f82b57de9ac658015a45b418191d35a`, local tag `olo-toolgate-control:module03`. |
| `python tools/control/cluster.py` | PASS: isolated Kind 0.27.0/Kubernetes 1.32.2, two replicas, real PostgreSQL, embedded static/API smoke, persistent upgrade and rollback; owned cluster removed. |
| Helm lint/render/Kubeconform through make | PASS: empty defaults, Gateway variants and six Control install/upgrade variants; ten negative Control configurations reject. |
| `python tools/ui/package.py`; `python tools/ui/merge_sbom.py` | PASS: reproducible versioned UI tar, asset hashes, checksums, complete production npm graph (five components); merged image SBOM has 371 components and preserves existing dependency edges. |
| `python tools/release/bundle.py`; drift/header/actionlint/diff checks | PASS: release compatibility metadata includes embedded UI, canonical outputs remain synchronized, updated workflows parse/lint and `git diff HEAD --check` passes. |

Verification caught and fixed native fetch receiver binding, trailing-slash
redirect recursion and caption contrast. npm's workspace/omit SBOM selector
omitted hoisted runtime nodes; packaging now uses npm's full component metadata
and the actual `npm ls --omit=dev` production graph, rejecting any missing node.
The verified npm inventory is merged into the image SBOM because minified assets
do not expose dependency metadata to the image scanner.

## Release, operations and limits

Product/contracts/chart remain on the existing **0.3.0-dev** development baseline;
there is no shared wire/schema or database migration change. Version plumbing now
includes the UI package and its contracts dependency. Existing protected CI Maven,
GHCR Control image and OCI Helm publication continue. CI installs the pinned
browser, runs UI gates, uploads evidence/asset bundle/checksums, merges the UI
inventory into the image SBOM and attests the existing image/SBOM. No remote CI
job, push, tag, registry publication or signing was performed locally.

UI/API release atomically. During mixed-image rolling upgrades, an older document
can reference an asset absent on a new replica; refresh after rollout or retain
previous asset sets at the proxy when uninterrupted old-page reloads are required.
There is no service worker or offline token cache. Existing Helm rollback restores
matching UI/API. Backend health/readiness/metrics/logs/traces remain authoritative;
the console shows safe request references rather than adding a browser exporter.

The isolated `--serve` developer harness uses real PostgreSQL and generated
five-minute identities, requires no company secrets and owns only its temporary
resources. It is not a production identity provider or Quickstart implementation.
Browser traces/video are disabled; credentials are redacted from failure reports.

Evidence lives in ignored `build/ui/`, `build/control/` and `build/release/`:
browser results/screenshots/smoke, coverage, UI tar/SBOM/checksums, container/cluster
smoke and image scan/merged SBOM. Tests clean up only their owned processes,
containers and cluster. No required gate remains blocked.

## Individual requirement matrix

All 104 unique IDs: **88 COMPLETE, 16 NOT_APPLICABLE, 0 BLOCKED**. Inherited host
and foundation requirements are verified through the unchanged backend/Gateway
boundary; NOT_APPLICABLE describes Module 03 implementation scope.

| Requirement | Status | Evidence / scope reason |
|---|---|---|
| ARC-001 | COMPLETE | Component map and ADR 004 keep UI presentation separate from backend authority. |
| ARC-002 | COMPLETE | Shell, directory views, transport and build/release tooling have cohesive responsibilities. |
| ARC-003 | COMPLETE | Injected fetch boundary and canonical operation adapter provide the test seam. |
| ARC-004 | COMPLETE | Shared record/page types; canonical generated operations; no copied schemas. |
| ARC-005 | COMPLETE | Published contracts identity; independent API/asset boundary documented in ADR 004. |
| ARC-006 | NOT_APPLICABLE | No approval/deployment/publication lifecycle implementation in this UI foundation. |
| ARC-007 | COMPLETE | Embedded assets add no authoritative server session state; two-replica Kind proof. |
| CON-001 | COMPLETE | Canonical Control OpenAPI drives tools/ui/generate.py. |
| CON-002 | COMPLETE | Existing local Maven JAR publication proof passes. |
| CON-003 | COMPLETE | Shared TS identity and Java workspace/published dependency modes pass. |
| CON-004 | COMPLETE | Unchanged v1 wire and frozen compatibility corpus pass. |
| CON-005 | COMPLETE | Canonical bindings and UI operation drift/negative tests pass. |
| CON-006 | COMPLETE | Version tool includes UI/product and contracts dependency; versioned metadata verified. |
| SEC-001 | COMPLETE | Backend verifies each call; real reader/expiry/wrong-key denials; no client auth decision. |
| SEC-002 | COMPLETE | Memory-only token, no storage/URL/cookie/export/log; browser and safe-error tests. |
| SEC-003 | NOT_APPLICABLE | UI does not accept or verify signed executable packages; existing trust path unchanged. |
| SEC-004 | COMPLETE | Policies/tools are labeled inert directory data; no Marketplace trust grant. |
| SEC-005 | COMPLETE | Stored configuration and device records do not claim runtime/deployment permission. |
| SEC-006 | NOT_APPLICABLE | No tool execution or sandbox implementation. |
| SEC-007 | COMPLETE | React text escaping, strict CSP, same-origin transport, encoded IDs and no redirect forwarding. |
| SEC-008 | NOT_APPLICABLE | No new signing/encryption key domain; test keys are isolated, production IdP remains external. |
| SEC-009 | COMPLETE | User writes cross existing atomic backend audit boundary, covered by real API regression. |
| SEC-010 | COMPLETE | Exact retry keys and revisions; inherited durable replay/tenant/actor proof passes. |
| TST-001 | COMPLETE | 20 component/transport tests and existing unit suites. |
| TST-002 | COMPLETE | API/auth/network/size/timeout/conflict/error and HTML-injection negatives. |
| TST-003 | COMPLETE | Real Control/PostgreSQL/Chromium integration; no production mock. |
| TST-004 | COMPLETE | Canonical operation drift and all-language compatibility corpus. |
| TST-005 | COMPLETE | Wrong signing key, expiry, reader denial, token custody, CSP and safe-error tests. |
| TST-006 | COMPLETE | Browser user CRUD and cross-component revision/authorization flows. |
| TST-007 | NOT_APPLICABLE | No new performance-critical runtime authorization path or throughput claim. |
| TST-008 | COMPLETE | Owned isolated infrastructure, semantic waits, zero test retries and complete fixtures. |
| QLT-001 | COMPLETE | TS/TSX/CSS/HTML/Java/Python source header gate passes. |
| QLT-002 | COMPLETE | Security-sensitive transport/static serving and component developer docs included. |
| QLT-003 | COMPLETE | Strict TypeScript/no-unused checks, header gate, actionlint and existing static gates. |
| QLT-004 | COMPLETE | Locked dependencies; 677 resolved licenses; vulnerability/secret scans. |
| QLT-005 | COMPLETE | Transport/controller/component ownership; no service locator or global credential state. |
| QLT-006 | COMPLETE | Typed ApiError, shared ErrorEnvelope and fixed human-safe wording. |
| API-001 | COMPLETE | Generated routes retain /api/control/v1. |
| API-002 | COMPLETE | Canonical OpenAPI wrapped with generated typed operation metadata. |
| API-003 | COMPLETE | Backend validates inputs; UI does not invent reference/security semantics. |
| API-004 | COMPLETE | Shared error model, 401/403/409/413/503 and malformed/network response tests. |
| API-005 | COMPLETE | Safe validated server request reference displayed for failed operations. |
| API-006 | COMPLETE | Idempotency-Key and If-Match on mutations; exact retry and real conflict recovery. |
| API-007 | COMPLETE | Raw exception/body/credential values excluded from error presentation. |
| DAT-001 | NOT_APPLICABLE | No UI database/schema migration; existing Flyway gates retained. |
| DAT-002 | NOT_APPLICABLE | No new UI persistence clean-install path; real backend database install remains tested. |
| DAT-003 | NOT_APPLICABLE | No database upgrade change; existing V1/V2 and cluster upgrade proof retained. |
| DAT-004 | NOT_APPLICABLE | UI adds no transaction implementation; backend retains atomic writes/audit/replay. |
| DAT-005 | NOT_APPLICABLE | UI adds no authoritative persistence; directory state remains in external PostgreSQL. |
| OBS-001 | COMPLETE | Existing Control private liveness exercised in host/container/cluster modes. |
| OBS-002 | COMPLETE | Existing readiness and real database outage/recovery gate passes. |
| OBS-003 | COMPLETE | Existing structured backend logs remain redacted; no UI token/body logger. |
| OBS-004 | COMPLETE | Existing private Prometheus metrics exercised; no fabricated dashboard runtime metrics. |
| OBS-005 | COMPLETE | Backend correlation/OTLP regression plus safe UI request references. |
| OBS-006 | NOT_APPLICABLE | No asynchronous job/fleet workflow in this UI foundation. |
| OBS-007 | COMPLETE | Token storage tests, sanitized errors/reports and backend log/trace redaction proof. |
| DKR-001 | COMPLETE | Separate pinned Node and JDK build stages; JRE runtime. |
| DKR-002 | COMPLETE | Runtime contains compiled Java/static assets without Node/dev tooling. |
| DKR-003 | COMPLETE | UID/GID 65532 production container smoke. |
| DKR-004 | COMPLETE | External secrets; no credential/key in image; scan and logs proof. |
| DKR-005 | COMPLETE | Existing OCI version/revision/source/license labels preserved. |
| DKR-006 | COMPLETE | Zero HIGH/CRITICAL image findings; complete 371-component merged SBOM. |
| DKR-007 | COMPLETE | SIGTERM completed shutdown; container exits 143. |
| DKR-008 | COMPLETE | Actual embedded console serves under read-only/cap-drop/no-new-privileges runtime. |
| K8S-001 | COMPLETE | Console embedded in existing canonical Control Deployment/Service; no extra workload. |
| K8S-002 | COMPLETE | Existing resources apply; render and two-replica smoke pass. |
| K8S-003 | COMPLETE | Existing restricted pod/container security contexts retained and validated. |
| K8S-004 | COMPLETE | Existing management startup/readiness/liveness probes; Kind ready proof. |
| K8S-005 | COMPLETE | Existing dedicated SA with token automount disabled. |
| K8S-006 | COMPLETE | Existing Control NetworkPolicy applies to same-origin UI/API; selector variants pass. |
| K8S-007 | COMPLETE | Existing Control PDB validated with replicas. |
| K8S-008 | COMPLETE | Existing optional HPA and invalid scaling settings render/reject correctly. |
| K8S-009 | COMPLETE | Existing placement/HA values retained and render-tested. |
| K8S-010 | COMPLETE | Existing external database/key/CA Secrets; no UI secret added. |
| K8S-011 | COMPLETE | Root-path TLS Control ingress routes console and versioned API on same origin. |
| K8S-012 | COMPLETE | Existing optional private ServiceMonitor strict schema variants. |
| K8S-013 | COMPLETE | Helm lint passes; disabled default stays empty. |
| K8S-014 | COMPLETE | Install/upgrade render variants and strict Kubeconform pass. |
| K8S-015 | COMPLETE | Real Kind two-replica embedded-asset/API smoke passes. |
| K8S-016 | COMPLETE | Persisted configuration upgrade and Helm rollback proof passes. |
| REL-001 | COMPLETE | Existing local Java publication and protected remote Maven configuration retained. |
| REL-002 | COMPLETE | Embedded console ships in scanned Control image; protected exact-image publication. |
| REL-003 | COMPLETE | Existing versioned OCI chart naming/metadata retained; no separate UI chart. |
| REL-004 | COMPLETE | Versioned production UI tar/evidence and canonical release compatibility metadata. |
| REL-005 | COMPLETE | Asset SHA-256 metadata, reproducible tar and release checksum files. |
| REL-006 | COMPLETE | Complete production npm graph and merged Control image SBOM. |
| REL-007 | COMPLETE | Existing protected image/SBOM provenance pipeline now includes UI inventory. |
| REL-008 | NOT_APPLICABLE | Registry signing keys are not configured; no signed-release claim. |
| REL-009 | COMPLETE | CHANGELOG Unreleased records Module 03 behavior and gates. |
| REL-010 | COMPLETE | Compatibility matrix and release metadata include embedded UI and unchanged Control v1. |
| REL-011 | COMPLETE | UI/API atomic upgrade, old-hash rolling caveat and rollback documented/tested. |
| DOC-001 | COMPLETE | Development/component commands cover UI build, tests and isolated live workspace. |
| DOC-002 | COMPLETE | UI README and administration behavior document. |
| DOC-003 | COMPLETE | HTTPS/token/host/proxy/build/version prerequisites documented. |
| DOC-004 | COMPLETE | Canonical API docs link the typed console consumer; no UI-specific wire endpoint. |
| DOC-005 | COMPLETE | ADR 004 precedes embedding/auth-shell implementation. |
| DOC-006 | COMPLETE | No default password or real token examples; generated local-only identities documented. |
| DOC-007 | COMPLETE | Blank-page/401/403/409/DB failure, request correlation, upgrade and rollback guidance. |
| PRF-001 | NOT_APPLICABLE | Gateway no-DB authorization path unchanged; UI does not implement runtime evaluation. |
| PRF-002 | NOT_APPLICABLE | No new Gateway critical-path allocation/cache implementation. |
| PRF-003 | COMPLETE | Static assets and external API state support existing two-replica Control host. |
| PRF-004 | NOT_APPLICABLE | No asynchronous fleet reporting or deployment workflow. |
| PRF-005 | NOT_APPLICABLE | No new latency/throughput claim or critical-path benchmark scope. |
| PRF-006 | COMPLETE | 50-record pages, bounded dashboard concurrency, 15-second deadline, 2 MiB response cap. |

## Changed files and suggested commit

Implementation: `apps/admin-ui/` application/configuration/tests; Control
`build.gradle.kts`, `Dockerfile`, `NOTICE.md`, `ConsoleAssets.java`; `tools/ui/`
generation/build/browser/package/SBOM tooling; Control static/container/cluster
verification; foundation/check/version/license/header/release tools.
Integration: root npm workspace/lock, Docker ignore, Makefile, two CI workflows.
Documentation: root architecture/development/readme/changelog; UI/component/API,
compatibility/Quickstart/release/Helm notes; ADR/index; coverage/completion/matrix.
See the repository diff for the full filename list. No commit or push was made.

```text
feat(ui): add administration console foundation
```
