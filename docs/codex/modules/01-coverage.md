# Module 01 requirement coverage plan

Prepared before implementation on 2026-10-02 after reading the master prompt,
traceability, governing documents and definition of done. Module 00 is the
foundation dependency; its pending guard fixes are preserved.

| Requirement IDs | Applicable | Implementation | Tests | Documentation | Release/deployment impact |
|---|---|---|---|---|---|
| ARC-001–005, ARC-007; CON-001, CON-004–006 | yes | apps/gateway; shared runtime schema and bindings | Contract drift/compatibility; Rust tests | ADR 002; gateway API and README | Stable imports; versioned API |
| SEC-001–002, SEC-004–005, SEC-007–008 | yes | Credential-bound context, extraction, fail-closed policy/audit ports | Auth, tenant isolation, extractor/dependency failures | Gateway security review | Secret mounts; no unsigned distributed bundle ingestion |
| TST-001–005, TST-007–008; QLT-001–006 | yes | Workspace gates and benchmark | Unit, HTTP/MCP integration, limits, benchmark | Test methodology | Mandatory CI and scans |
| API-001–005, API-007; DAT-005; OBS-001–005, OBS-007 | yes | Versioned ingress, stateless context, probes, metrics/tracing | Malformed input, correlation, telemetry redaction | Canonical OpenAPI; config/operations | Stateless replicas; bounded telemetry |
| DKR-001–008; K8S-001–016 | yes | Dockerfile and gateway Helm templates | Container smoke/scan; render/schema and cluster smoke | Deployment/rollback | Non-root image, restricted workload, ingress/metrics options |
| REL-002–007, REL-009–011; DOC-001–007 | yes | Image CI and existing release integration | Workflow/version/artifact smoke | Changelog; compatibility/upgrades | Protected GHCR push, SBOM/provenance |
| PRF-001–003, PRF-005–006 | yes | In-memory policy; bounded admission/body/time/audit queue | Limits/backpressure and benchmark baseline | Performance/scaling | No DB in authorization path |
| ARC-006; CON-002–003; SEC-003, SEC-006, SEC-009–010; TST-006; API-006; DAT-001–004; OBS-006; REL-001, REL-008; PRF-004 | no | No lifecycle, signing, code execution, durable mutation, DB or fleet reporting | Existing foundation gates retained | Individual completion reasons | Owning modules deferred; existing contract publication retained |

Every individual ID will be COMPLETE, NOT_APPLICABLE with a reason, or BLOCKED
with evidence in the completion report. A blocked applicable gate prevents
declaring the module done. No later module is started.
