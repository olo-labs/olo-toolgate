# Module 09 requirement coverage plan

Prepared before implementation on 2026-10-03 after reading the master prompt,
traceability, governing documents, package/desired-state architecture and DoD.
Module 08 limitations are explicit prerequisites: unsupported runtime/OS/arch
packages cannot activate; existing runtime authorization is never bypassed.

| Requirement IDs | Applicable? | Implementation location | Test location | Documentation location | Release/deployment impact |
|---|---|---|---|---|---|
| ARC-001–007; CON-001–006 | yes | canonical fleet schemas/OpenAPI; Control deployment domain/application/store ports; client reconciliation/runtime port | drift/round trips/frozen fixtures/local Maven; lifecycle domain/client tests | ADR 010; fleet protocol/architecture | Additive fleet protocol; synchronized release and immutable descriptors |
| SEC-001–010; API-001–007 | yes | separate release/deployment signature keys; tenant/device/generation/expiry; mTLS scoped artifact grants; bounded mirror; custody/activation; existing Gateway adapter | invalid signature/hash, replay/stale/wrong assignment, revocation, SSRF/path/archive/shell negatives, admin and permit boundaries | deployment security and key rotation guide | No package or assignment grants runtime permission; external secrets |
| TST-001–006, TST-008; QLT-001–006 | yes | explicit durable lifecycle and rollout states; ports and pure compatibility checks | PostgreSQL migration/transactions, actual client persistence/runtime/HTTP and UI/browser E2E | completion report | Required deployment CI/release compatibility gate |
| DAT-001–005; OBS-001–007 | yes | Flyway V6; immutable release/rollout/device generations and reported snapshots; client durable journal; telemetry | clean/upgrade migration, concurrency/idempotency, interrupted install/recovery, offline and aggregation | fleet operations/debug/runbook | External DB/artifact store; bounded metrics labels |
| DKR-001–008; K8S-001–016 | yes, existing Control/client integration | existing Control image/chart plus external artifact-store/key references and intentional egress | container/Helm/schema and owned local-cluster upgrade/rollback gates | chart/control deployment guide | No new server; artifact store is external, no embedded credentials |
| REL-001–011; DOC-001–007 | yes | existing release workflows/client archives/shared contracts; deployment guides and UI status | publication/package/scan/compatibility/source/link gates | README, ADR, API, user/debug guides, changelog, completion | Schema/DB/client compatibility, checksums/SBOM/signing hooks |
| PRF-001–004, PRF-006 | yes | bounded rollout batches/desired assignments/artifact transfer and report pagination; no deployment work in Gateway | batching/backpressure/rollout aggregation and bounded inputs | fleet scale/retention guidance | Stateless replicas; bounded server/client state |
| TST-007; PRF-005 | no | Deployment reconciliation is outside Gateway hot path | Existing benchmarks retained; lifecycle limits tested | Completion reason | No new throughput claim |

Every one of the 104 individual IDs receives COMPLETE, NOT_APPLICABLE with a
reason, or BLOCKED with evidence in the final matrix. Unexecuted native or cluster
gates remain BLOCKED. Do not claim DONE or begin Module 10 with blocked gates.
