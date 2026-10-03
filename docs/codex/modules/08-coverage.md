# Module 08 requirement coverage plan

Prepared before implementation on 2026-10-03 after reading the master prompt,
traceability, governing requirements, runtime references and definition of done.

| Requirement IDs | Applicable | Implementation location | Test location | Documentation location | Release/deployment impact |
|---|---|---|---|---|---|
| ARC-001–006; CON-001–006 | yes | canonical execution schemas; endpoint execution ports/adapters/config/IPC | schema/round-trip/compatibility/drift; runtime tests | ADR 009; runtime operations | Additive versioned contracts and native packages |
| SEC-001–010; API-001, API-003–007 | yes | protected organization-reviewed local tool registration; digest verification; fresh Gateway grant; OCI sandbox | injection, timeout, overflow, child restriction, environment/path/output/trust/outage tests | isolation and trust/configuration guide | No host interpreter fallback; isolated managed provisioning |
| TST-001–006, TST-008; QLT-001–006 | yes | bounded runtime manager, adapters and engine port | actual Docker/runtime boundaries, IPC/E2E and language gates | completion evidence | CI required negative gates |
| OBS-001–005, OBS-007; PRF-001–004, PRF-006; DAT-004–005 | yes | discovery/self-test/counters; serialized preparation/execution; bounded management/cleanup | runtime status, cleanup, concurrent preparation, redaction tests | execution/debug guide | Machine-local protected config; no database on ordinary authorization |
| DKR-003–004, DKR-007–008; K8S-010, K8S-013–014; REL-001–011 | yes, existing release integration | runtime container isolation; client packaging/CI; existing image/chart version plumbing | image/runtime policy/package/Helm checks | compatibility/release/runtime dependencies | External engine; digest-pinned runtimes install during preparation/first use; no secret embedding |
| DOC-001–007 | yes | client README, runtime guides, ADR, operations, changelog, report | link/presence/source checks | docs/client and docs/operations | Explicit install/first-use and offline behavior |
| ARC-007; API-002; DAT-001–003; OBS-006 | no | No new server, REST route, database migration or asynchronous fleet workflow | Existing server gates retained | Completion reasons | Module 09 distribution is not implemented |
| DKR-001–002, DKR-005–006; K8S-001–009, K8S-011–012, K8S-015–016 | no new service | Existing server pipelines remain; native client is not a Helm workload | Existing chart gates retained | Completion reasons | No new cluster deployment |
| TST-007; PRF-005 | no | Local tool execution is outside the Gateway hot path; bounded execution tests apply | Not a throughput benchmark module | Completion reasons | Benchmark module unchanged |

All 104 individual IDs receive COMPLETE, NOT_APPLICABLE with a reason, or BLOCKED
with evidence in the completion report. Unexecuted native platform gates remain
BLOCKED. Do not declare DONE while gates remain blocked; do not start Module 09.
