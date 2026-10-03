# Module 06 requirement coverage plan

Prepared before implementation on 2026-10-03 after reading the master prompt,
traceability, governing documents, endpoint architecture and definition of done.
Dependencies 00–02 and the Gateway identity boundary are present.

| Requirement IDs | Applicable | Implementation location | Test location | Documentation location | Release/deployment impact |
|---|---|---|---|---|---|
| ARC-001–007; CON-001–006 | yes | apps/endpoint-client; Control enrollment domain/application/adapters; canonical endpoint schemas/OpenAPI | client unit/integration; Control PostgreSQL tests; schema/round-trip/drift/local Maven proof | ADR 007; client README and runbook; compatibility | Additive contracts, synchronized minor version; no runtime execution |
| SEC-001–005, SEC-007–010; API-001–007; QLT-001–006 | yes | fixed HTTPS discovery; dedicated external device CA; key custody; browser authorization; certificate registry/revocation; OS peer-authenticated IPC | wrong server/certificate/key/identity; replay, expiry, races, path/IPC/secret negatives; scans | security/enrollment/IPC/configuration docs | Device CA distinct from policy/permit/IdP keys; client artifacts and external Secrets |
| TST-001–006, TST-008; DAT-001–005; OBS-001–007 | yes | Flyway enrollment/device/check-in migration; protected local journal; health/state/logs/metrics/correlation; CI | clean/upgrade PostgreSQL; real TLS enrollment/check-in; key restart/reconnect/offline; browser enrollment | client API/fleet/runbook and completion report | No permanent fleet sessions or mandatory new infrastructure |
| PRF-001–004, PRF-006 | yes | bounded batched reports, bounded enrollment state/polling/backoff/IPC and fixed Gateway identity port | report/queue/body/time limits; reconnect; revoked device | scale/limits documentation | Existing Gateway normal path stays in memory; native service resource bounds |
| DKR-001–008; K8S-001–016 | yes, existing Control server only | existing Control image/chart with opt-in HTTPS/device CA Secret references | container build/scan; Helm lint/render/negative; local cluster | chart and Control/client operations | Endpoint runs as an OS service, not a Kubernetes workload; existing server HA preserved |
| REL-001–011; DOC-001–007 | yes | client OS/arch CI matrix, binary packaging/checksums/SBOM/signing hooks; existing publication gates | native/cross-target compilation; package/checksum/metadata validation; compatibility/release smoke | READMEs; ADR; release/upgrade/changelog; completion matrix | Windows/Linux/macOS x64/arm64 release skeleton; protected CI signing/publication |
| SEC-006; TST-007; PRF-005 | no | No untrusted execution/sandbox or new performance-critical runtime evaluator in Module 06 | Existing Gateway benchmarks retained; client correctness/limits tested | Scope reasons in completion report | Execution and its benchmarks remain later modules |

Every individual ID receives COMPLETE, NOT_APPLICABLE with a reason, or BLOCKED
with evidence in the completion report. A blocked gate prevents declaring done.
Implement Module 06 only. Completion requires all gates to pass. The user's
requested commit may record current work with an explicit unfinished status in
the verification report; it does not declare this module done.
