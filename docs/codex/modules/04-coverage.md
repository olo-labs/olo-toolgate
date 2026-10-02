# Module 04 requirement coverage plan

Prepared before implementation on 2026-10-02 after reading the master prompt,
traceability document, governing documents and definition of done.

| Requirement IDs | Applicable | Implementation location | Test location | Documentation location | Release/deployment impact |
|---|---|---|---|---|---|
| ARC-001–007; CON-001–006 | yes | canonical bundle schema; Control application/compiler/signing/store adapters; Gateway verified snapshot and fetch adapter | contract corpus; Java/Rust unit tests; real Control/Gateway E2E | ADR 005; bundle protocol and operations docs | Additive v1 contract; stable published identities; stateless replicas |
| SEC-001–005, SEC-007–010 | yes | dedicated bundle key domain; strict JWS/hash validation; authenticated publish/rollback; monotonic sequence; bounded fixed-destination fetch | forged/stale/wrong-domain/malformed/expiry/rotation/replay/concurrency tests; redaction checks | security review and key rotation runbook | External signing Secret and verification keyring; no unsigned fallback |
| SEC-006 | no | No tool or uploaded code execution | Existing prohibition retained | Completion scope | No sandbox/runtime added |
| TST-001–008; QLT-001–006 | yes | bounded compiler and atomic evaluator; shared fixtures; checks/CI | deterministic clocks; concurrent replacement; outage E2E; evaluation benchmark; drift/lint/scans | test methodology and completion evidence | Required gates extended |
| API-001–007 | yes | Control publish/current/version/rollback APIs and canonical OpenAPI | authentication/roles/tenant/validation/idempotency/error tests | Control API and bundle protocol | Versioned API; shared models |
| DAT-001–005 | yes | Flyway immutable bundle history; tenant transaction lock; atomic publication/audit/replay | real PostgreSQL fresh/upgrade/concurrent transactions | upgrade and database permissions notes | Additive migration and least-privilege grants |
| OBS-001–005, OBS-007 | yes | inherited health/tracing; bundle freshness readiness; bounded update/status metrics and sanitized lifecycle audit | readiness/metrics/outage/audit/redaction tests | monitoring and failure runbook | Expiry/grace status visible without payload/secrets |
| OBS-006 | no | Publication is synchronous and transactional; polling has no durable job | Transaction result and current version are explicit | Completion scope | No queue/job subsystem |
| DKR-001–008; K8S-001–016 | yes | existing Gateway/Control images and chart; external key/token references; intentional Control egress | image build/scan/non-root smoke; Helm render/schema negatives; local cluster install/upgrade/rollback | component deployment docs and chart README | Keys never built into images or Helm values; workloads remain opt-in |
| REL-001–007, REL-009–011 | yes | existing contract/image/chart workflows; bundle compatibility fixtures and evidence | local Maven artifact proof; release checks; scans/SBOM/checksums | release process, compatibility matrix and changelog | Product signing keys separate from release provenance keys |
| REL-008 | yes, runtime bundles | Control JWS signer and Gateway verifier | cross-language RSA verification and key rotation | protocol/ADR/security review | Runtime bundles always signed; artifact signing remains protected CI when configured |
| DOC-001–007 | yes | README/API/config/operations/ADR/completion report | documentation presence/link and example checks | same locations | Upgrade, outage, compromise and rollback guidance |
| PRF-001–003, PRF-005–006 | yes | in-memory evaluator; bounded compile/fetch/rules/timeouts; per-replica polling | benchmark; limits and concurrency tests | benchmark methodology and scaling guidance | No database/network on authorization path |
| PRF-004 | no | No endpoint fleet reporting in this module | Not applicable | Completion scope | Later client modules |

Every individual ID will receive COMPLETE, NOT_APPLICABLE with a reason, or
BLOCKED with evidence in the completion report. A blocked gate prevents declaring
the module done. No later module is started.
