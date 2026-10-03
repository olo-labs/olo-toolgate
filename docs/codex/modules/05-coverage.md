# Module 05 requirement coverage plan

Prepared before implementation on 2026-10-03 after reading the master prompt,
requirements traceability, governing documents and definition of done.

| Requirement IDs | Applicable | Implementation location | Test location | Documentation location | Release/deployment impact |
|---|---|---|---|---|---|
| ARC-001–007; CON-001–006 | yes | canonical approval contracts; Control application/domain/adapter; Gateway approval port; UI | schema/round-trip/compatibility/drift; Java/Rust tests | ADR 006; approval runbook | Additive contracts; signed policy format 2; synchronized 0.5.0-dev |
| SEC-001–005, SEC-007–010; API-001–007; QLT-006 | yes | exact approval binding; dedicated roles/key domain; transactional decision/consume; bounded HTTPS | wrong approver/self/tenant/binding; races; expiry; unavailable; signature/replay/revocation | authorization/config/API/security docs | External Gateway permit key; no cache fallback |
| TST-001–008; QLT-001–005 | yes | source/tooling/CI; real PostgreSQL and production services | state machine, negative/security, real integration/E2E, benchmark and scans | module completion evidence | Required release compatibility gate |
| DAT-001–005; OBS-001–007 | yes | Flyway approval/permit state; audited transitions; bounded status/metrics/traces | clean/upgrade/race/audit/expiration/health tests | operations and retention | Existing PostgreSQL; no new mandatory infrastructure |
| DKR-001–008; K8S-001–016 | yes | existing images/chart; opt-in approval behavior and external Secret references | production image/scan; Helm lint/render/negative; local cluster upgrade/rollback | chart README; component operations | Stateless replicas; durable once/permit consumption in PostgreSQL |
| REL-001–011; DOC-001–007 | yes | version/release/workflows; component and developer docs | local artifact proof; compatibility/assets/checksums/SBOM/provenance configuration | changelog; compatibility; upgrades; completion report | Protected CI publishing retained; policy/permit signing trust stays separate |
| PRF-001–003, PRF-005–006 | yes | ordinary ALLOW/BLOCK remain in memory; only ASK uses bounded remote coordination | benchmark; request/pending/page/time/body limits | methodology and scale guidance | Approval outages block ASK; normal policy behavior retained |
| SEC-006; PRF-004 | no | No tool execution/sandbox or fleet reporting in Module 05 | Not applicable | Scope recorded in completion report | Endpoint/fleet work remains later modules |

Every individual ID receives COMPLETE, NOT_APPLICABLE with a reason, or BLOCKED
with evidence in the completion report. A blocked gate prevents declaring done.
No later module is started.
