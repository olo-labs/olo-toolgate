# Module 10 requirement coverage plan

Prepared before implementation on 2026-10-03 after reading the master prompt,
traceability, governing architecture/security/build/release documents and DoD.
Inherited native OS/ARM and fleet-enabled cluster certification limitations remain
explicit. Module 10 does not grant host/network/credential capabilities to code.

| Requirement IDs | Applicable? | Implementation location | Test location | Documentation location | Release/deployment impact |
|---|---|---|---|---|---|
| ARC-001–007; CON-001–006 | yes | builder/shared inline-source contracts; Control authoring/job/store ports; client sandbox adapter; console | schema/drift/round trips/compatibility/Maven; layer and routing tests | ADR 011; builder guide | Additive source and job contracts; synchronized versions |
| SEC-001–010; API-001–007 | yes | admin-only drafts/sealing/test/deploy/export; bounded secret/permission checks; signed desired source; mTLS designated-client test leases; runtime Gateway checks | secrets/permissions/replay/wrong-client/hash/version/sandbox and API negatives | trust, permission and credential restrictions | Separate release/organization/runtime trust; no execution in Control |
| TST-001–006, TST-008; QLT-001–006 | yes | deterministic builder validation/state machine and bounded adapters | PostgreSQL, actual sandbox/client and browser E2E; source/type/static checks | command evidence/completion report | Protected compatibility/release gates; inherited certification blockers |
| DAT-001–005; OBS-001–007 | yes | Flyway V7 draft/immutable version/durable test jobs; audit/replay; client reports | clean/upgrade, optimistic revisions/idempotency, job ownership/expiry/recovery | state/operations/debug guide | External PostgreSQL retained; no additional infrastructure |
| DKR-001–008; K8S-013–014; REL-001–011 | yes, existing packaging | existing Control image/client/contract release pipelines; source runner in client | container/chart/build/scan/publication/package compatibility checks | compatibility/changelog/upgrade notes | Inline source is signed immutable descriptor data, never image build hooks |
| DOC-001–007; PRF-001–004, PRF-006 | yes | bounded editor/drafts/jobs/validation/source/input/output and cursor pages | validation/backpressure/batched polling/status tests | author/user/admin/debug/examples | No new work in Gateway hot path; finite state and transfer limits |
| TST-007; PRF-005 | no | No new Gateway hot path | Existing methodology retained | Completion reason | No throughput claim |
| K8S-001–012, K8S-015–016 | no new chart/config | Existing Control/client deployment reused | Existing smoke retained; new builder cross-component E2E outside cluster | No new Helm resource/config; inherited Module 09 limitations retained | No new service, signing secret or infrastructure |

All 104 IDs receive COMPLETE, NOT_APPLICABLE with a scope reason, or BLOCKED with
evidence in the completion report. A blocked applicable gate prevents DONE.
Do not start Module 11. Commit the checked implementation as requested.
