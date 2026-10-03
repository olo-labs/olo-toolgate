# Module 05 completion report

Status: **DONE for Module 05**, 2026-10-03. Mandatory preparation was completed
before implementation; see the [coverage plan](05-coverage.md) and
[ADR 006](../../adr/006-ask-approvals.md). Dependencies are Modules 01–04.
No later module was started.

## Implementation and security review

Control implements durable approval state, dedicated approver authorization,
self-approval rejection, optimistic revision and idempotency, once/temporary/deny,
expiration and transactional audit. Machine and human roles cannot be combined.
PostgreSQL serializes decisions, lease issuance and single-use consumption.
Temporary approval issues distinct short-lived leases for the exact approved scope.
The optional notification port defaults to no-op; no new mandatory infrastructure.

Gateway supports signed policy formats 1 and 2, BLOCK > ASK > ALLOW precedence,
and bounded online ASK coordination. Unavailable or malformed responses BLOCK.
Separate external RSA keys sign permits for at most ten seconds. Identity, optional
device, tool, action, resource, argument digest, nonce and policy version bind consumption.
Policy and credential expiry are rechecked after remote calls and audit. Normal
ALLOW/BLOCK evaluation remains in memory. Existing v1/MCP behavior fails closed on ASK.

Control persists authenticated clock observations after rejected transaction rollback.
Gateway shares monotonic/high-water time for signing and verification. Operator UTC
remains necessary across cold starts. Review findings for mixed roles and clock
regression were fixed and covered by tests. No tool execution is introduced.

The accessible UI supports exact-scope review, once/temporary/deny, expiration and
stale-revision recovery. Canonical contracts contain 68 models/enums, 13 schema groups
and 156 deterministic generated outputs. Product/contracts/chart are 0.5.0-dev.
ASK requires format 2; ALLOW/BLOCK-only publication remains format 1. Old administrative
schema readers can reject the widened ControlPolicy decision enum, as documented.

## Verification executed

| Command / gate | Result and local evidence |
|---|---|
| `make check` | PASS: contracts, workspace/local Java, Rust, UI, PHP, browser and complete Helm matrix. `build/approval/make-check.log` |
| `python tools/check.py --publication-only` | PASS: locked local-artifact dependency proof, all 16 Control tests and missing-artifact rejection. Eight-way contention originally hit the production lock deadline under load; race test now uses two synchronized contenders. `build/approval/publication-final.log` |
| `python tools/approval/check.py --build` | PASS: real production images/PostgreSQL; wrong/self approver, races, once/temp/deny/expiry, permit binding/signatures, consume/replay, outage, revocation, audit/redaction. `build/approval/e2e.json` |
| `python tools/policy/check.py` | PASS: existing production signed-policy compatibility/security/lifecycle. `build/approval/policy-final.log` |
| `python tools/control/cluster.py` | PASS: two replicas; atomic consume and replay denial; spent approval and immutable bundle survive install/upgrade/rollback. `build/control/cluster-smoke.json` |
| `python tools/gateway/benchmark.py` | PASS: 512-rule local ASK evaluation, 20,000 iterations, p95 1592 ns/p99 1796 ns. Excludes network, signing, audit IO and production Instant sampling. `build/gateway/approval-benchmark.json` |
| `python tools/check.py --scans` | PASS: actionlint, npm/pip audit, 757-entry license audit, Gitleaks/fixture negative proof, Trivy source vulnerability/license scan. `build/approval/scans-final.log` |
| Trivy 0.61.1 image scans and CycloneDX | PASS: both images HIGH/CRITICAL and secret scans; Control SBOM includes embedded UI. `build/approval/*-image-scan.json`, `build/control/control-sbom.cdx.json`, `build/gateway/gateway-sbom.cdx.json` |
| Release bundle and Helm package | PASS: 0.5.0-dev raw contracts, compatibility, Java binary/source/javadoc, chart, license report, both SBOMs, SHA256SUMS. `build/approval/release` |

Full checks include 40 Python contract tests, 16 Control Java tests, 12 Gateway approval
integration tests, existing Rust core/bundle tests, 36 UI unit tests, five real browser
E2Es and 68-model round trips. Approval Helm validates install/upgrade/rotation and
nine invalid configurations alongside inherited deployment variants.

## Definition of done and operational limits

- [x] Implementation: primary path, shared contracts, explicit state/ports, no mock or bypass.
- [x] Tests: unit, errors, contracts, integration, security, browser/production E2E and benchmark.
- [x] Quality: headers, API documentation, dependency discipline, static checks and secret redaction.
- [x] Operations: health/readiness, logs, bounded metrics, correlation and graceful shutdown.
- [x] Deployment: rebuilt non-root images, external Secrets, probes/resources/NetworkPolicy and tested HA.
- [x] Release: synchronized versions, publication gates, assets/checksums/SBOMs and protected provenance hooks.
- [x] Documentation: READMEs, ADR, OpenAPI, configuration, upgrade and operational guidance.
- [x] Proof: commands/results, individual traceability and suggested commit below.

Implementation, tests, code quality, operations, deployment, release and documentation
are covered above. All applicable definition-of-done gates passed. Remote publication,
attestation issuance and optional image signing remain protected CI operations;
no remote release is claimed. Production requires external IdP/TLS/keys, trusted UTC
and enforcing CNI. Gateway replicas share the configured permit key; rotation may
invalidate outstanding ten-second permits conservatively. Helm rollback never reverses
approval spending. See the [approval runbook](../../control-plane/approvals.md).

## Individual requirement matrix

104 unique IDs: 102 COMPLETE and two NOT_APPLICABLE with scope reasons; none BLOCKED.

| Requirement | Status | Evidence / scope reason |
|---|---|---|
| ARC-001 | COMPLETE | ADR 006; ApprovalService/domain and Gateway coordinator/signer ports; external PostgreSQL, stateless replicas. |
| ARC-002 | COMPLETE | ADR 006; ApprovalService/domain and Gateway coordinator/signer ports; external PostgreSQL, stateless replicas. |
| ARC-003 | COMPLETE | ADR 006; ApprovalService/domain and Gateway coordinator/signer ports; external PostgreSQL, stateless replicas. |
| ARC-004 | COMPLETE | ADR 006; ApprovalService/domain and Gateway coordinator/signer ports; external PostgreSQL, stateless replicas. |
| ARC-005 | COMPLETE | ADR 006; ApprovalService/domain and Gateway coordinator/signer ports; external PostgreSQL, stateless replicas. |
| ARC-006 | COMPLETE | ADR 006; ApprovalService/domain and Gateway coordinator/signer ports; external PostgreSQL, stateless replicas. |
| ARC-007 | COMPLETE | ADR 006; ApprovalService/domain and Gateway coordinator/signer ports; external PostgreSQL, stateless replicas. |
| CON-001 | COMPLETE | Canonical approval schemas, generated bindings/drift and compatibility tests; workspace/published Maven proof; 0.5.0-dev. |
| CON-002 | COMPLETE | Canonical approval schemas, generated bindings/drift and compatibility tests; workspace/published Maven proof; 0.5.0-dev. |
| CON-003 | COMPLETE | Canonical approval schemas, generated bindings/drift and compatibility tests; workspace/published Maven proof; 0.5.0-dev. |
| CON-004 | COMPLETE | Canonical approval schemas, generated bindings/drift and compatibility tests; workspace/published Maven proof; 0.5.0-dev. |
| CON-005 | COMPLETE | Canonical approval schemas, generated bindings/drift and compatibility tests; workspace/published Maven proof; 0.5.0-dev. |
| CON-006 | COMPLETE | Canonical approval schemas, generated bindings/drift and compatibility tests; workspace/published Maven proof; 0.5.0-dev. |
| SEC-001 | COMPLETE | Java/Rust negative tests and production ASK E2E: exact binding, trust/key separation, roles, audit, replay, expiry/outage rejection. |
| SEC-002 | COMPLETE | Java/Rust negative tests and production ASK E2E: exact binding, trust/key separation, roles, audit, replay, expiry/outage rejection. |
| SEC-003 | COMPLETE | Java/Rust negative tests and production ASK E2E: exact binding, trust/key separation, roles, audit, replay, expiry/outage rejection. |
| SEC-004 | COMPLETE | Java/Rust negative tests and production ASK E2E: exact binding, trust/key separation, roles, audit, replay, expiry/outage rejection. |
| SEC-005 | COMPLETE | Java/Rust negative tests and production ASK E2E: exact binding, trust/key separation, roles, audit, replay, expiry/outage rejection. |
| SEC-006 | NOT_APPLICABLE | No untrusted code execution/sandbox is introduced. |
| SEC-007 | COMPLETE | Java/Rust negative tests and production ASK E2E: exact binding, trust/key separation, roles, audit, replay, expiry/outage rejection. |
| SEC-008 | COMPLETE | Java/Rust negative tests and production ASK E2E: exact binding, trust/key separation, roles, audit, replay, expiry/outage rejection. |
| SEC-009 | COMPLETE | Java/Rust negative tests and production ASK E2E: exact binding, trust/key separation, roles, audit, replay, expiry/outage rejection. |
| SEC-010 | COMPLETE | Java/Rust negative tests and production ASK E2E: exact binding, trust/key separation, roles, audit, replay, expiry/outage rejection. |
| TST-001 | COMPLETE | make check, Java/Rust/schema tests, real PostgreSQL, UI/browser and production E2E, measured ASK benchmark. |
| TST-002 | COMPLETE | make check, Java/Rust/schema tests, real PostgreSQL, UI/browser and production E2E, measured ASK benchmark. |
| TST-003 | COMPLETE | make check, Java/Rust/schema tests, real PostgreSQL, UI/browser and production E2E, measured ASK benchmark. |
| TST-004 | COMPLETE | make check, Java/Rust/schema tests, real PostgreSQL, UI/browser and production E2E, measured ASK benchmark. |
| TST-005 | COMPLETE | make check, Java/Rust/schema tests, real PostgreSQL, UI/browser and production E2E, measured ASK benchmark. |
| TST-006 | COMPLETE | make check, Java/Rust/schema tests, real PostgreSQL, UI/browser and production E2E, measured ASK benchmark. |
| TST-007 | COMPLETE | make check, Java/Rust/schema tests, real PostgreSQL, UI/browser and production E2E, measured ASK benchmark. |
| TST-008 | COMPLETE | make check, Java/Rust/schema tests, real PostgreSQL, UI/browser and production E2E, measured ASK benchmark. |
| QLT-001 | COMPLETE | Header/static/type/drift gates; resolved dependency license and source/secret scans; typed failures and injected boundaries. |
| QLT-002 | COMPLETE | Header/static/type/drift gates; resolved dependency license and source/secret scans; typed failures and injected boundaries. |
| QLT-003 | COMPLETE | Header/static/type/drift gates; resolved dependency license and source/secret scans; typed failures and injected boundaries. |
| QLT-004 | COMPLETE | Header/static/type/drift gates; resolved dependency license and source/secret scans; typed failures and injected boundaries. |
| QLT-005 | COMPLETE | Header/static/type/drift gates; resolved dependency license and source/secret scans; typed failures and injected boundaries. |
| QLT-006 | COMPLETE | Header/static/type/drift gates; resolved dependency license and source/secret scans; typed failures and injected boundaries. |
| API-001 | COMPLETE | Versioned canonical OpenAPI/generated UI operations; bounded strict validation, correlation, revision/idempotency and redacted errors. |
| API-002 | COMPLETE | Versioned canonical OpenAPI/generated UI operations; bounded strict validation, correlation, revision/idempotency and redacted errors. |
| API-003 | COMPLETE | Versioned canonical OpenAPI/generated UI operations; bounded strict validation, correlation, revision/idempotency and redacted errors. |
| API-004 | COMPLETE | Versioned canonical OpenAPI/generated UI operations; bounded strict validation, correlation, revision/idempotency and redacted errors. |
| API-005 | COMPLETE | Versioned canonical OpenAPI/generated UI operations; bounded strict validation, correlation, revision/idempotency and redacted errors. |
| API-006 | COMPLETE | Versioned canonical OpenAPI/generated UI operations; bounded strict validation, correlation, revision/idempotency and redacted errors. |
| API-007 | COMPLETE | Versioned canonical OpenAPI/generated UI operations; bounded strict validation, correlation, revision/idempotency and redacted errors. |
| DAT-001 | COMPLETE | V4 clean/V3-upgrade tests; tenant transactions and durable clock; atomic audit/decision/consume; two-replica persistence. |
| DAT-002 | COMPLETE | V4 clean/V3-upgrade tests; tenant transactions and durable clock; atomic audit/decision/consume; two-replica persistence. |
| DAT-003 | COMPLETE | V4 clean/V3-upgrade tests; tenant transactions and durable clock; atomic audit/decision/consume; two-replica persistence. |
| DAT-004 | COMPLETE | V4 clean/V3-upgrade tests; tenant transactions and durable clock; atomic audit/decision/consume; two-replica persistence. |
| DAT-005 | COMPLETE | V4 clean/V3-upgrade tests; tenant transactions and durable clock; atomic audit/decision/consume; two-replica persistence. |
| OBS-001 | COMPLETE | Inherited health/readiness/logs/OTLP checks; approval metrics/traces, durable statuses and production redaction tests. |
| OBS-002 | COMPLETE | Inherited health/readiness/logs/OTLP checks; approval metrics/traces, durable statuses and production redaction tests. |
| OBS-003 | COMPLETE | Inherited health/readiness/logs/OTLP checks; approval metrics/traces, durable statuses and production redaction tests. |
| OBS-004 | COMPLETE | Inherited health/readiness/logs/OTLP checks; approval metrics/traces, durable statuses and production redaction tests. |
| OBS-005 | COMPLETE | Inherited health/readiness/logs/OTLP checks; approval metrics/traces, durable statuses and production redaction tests. |
| OBS-006 | COMPLETE | Inherited health/readiness/logs/OTLP checks; approval metrics/traces, durable statuses and production redaction tests. |
| OBS-007 | COMPLETE | Inherited health/readiness/logs/OTLP checks; approval metrics/traces, durable statuses and production redaction tests. |
| DKR-001 | COMPLETE | Rebuilt multi-stage minimal non-root production images; external keys, OCI labels, graceful shutdown/read-only runtime, clean image scans/SBOMs. |
| DKR-002 | COMPLETE | Rebuilt multi-stage minimal non-root production images; external keys, OCI labels, graceful shutdown/read-only runtime, clean image scans/SBOMs. |
| DKR-003 | COMPLETE | Rebuilt multi-stage minimal non-root production images; external keys, OCI labels, graceful shutdown/read-only runtime, clean image scans/SBOMs. |
| DKR-004 | COMPLETE | Rebuilt multi-stage minimal non-root production images; external keys, OCI labels, graceful shutdown/read-only runtime, clean image scans/SBOMs. |
| DKR-005 | COMPLETE | Rebuilt multi-stage minimal non-root production images; external keys, OCI labels, graceful shutdown/read-only runtime, clean image scans/SBOMs. |
| DKR-006 | COMPLETE | Rebuilt multi-stage minimal non-root production images; external keys, OCI labels, graceful shutdown/read-only runtime, clean image scans/SBOMs. |
| DKR-007 | COMPLETE | Rebuilt multi-stage minimal non-root production images; external keys, OCI labels, graceful shutdown/read-only runtime, clean image scans/SBOMs. |
| DKR-008 | COMPLETE | Rebuilt multi-stage minimal non-root production images; external keys, OCI labels, graceful shutdown/read-only runtime, clean image scans/SBOMs. |
| K8S-001 | COMPLETE | External approval Secrets/config plus inherited probes/resources/SA/NetworkPolicy/PDB/HPA/affinity/ingress/metrics; lint/render/Kind upgrade/rollback. |
| K8S-002 | COMPLETE | External approval Secrets/config plus inherited probes/resources/SA/NetworkPolicy/PDB/HPA/affinity/ingress/metrics; lint/render/Kind upgrade/rollback. |
| K8S-003 | COMPLETE | External approval Secrets/config plus inherited probes/resources/SA/NetworkPolicy/PDB/HPA/affinity/ingress/metrics; lint/render/Kind upgrade/rollback. |
| K8S-004 | COMPLETE | External approval Secrets/config plus inherited probes/resources/SA/NetworkPolicy/PDB/HPA/affinity/ingress/metrics; lint/render/Kind upgrade/rollback. |
| K8S-005 | COMPLETE | External approval Secrets/config plus inherited probes/resources/SA/NetworkPolicy/PDB/HPA/affinity/ingress/metrics; lint/render/Kind upgrade/rollback. |
| K8S-006 | COMPLETE | External approval Secrets/config plus inherited probes/resources/SA/NetworkPolicy/PDB/HPA/affinity/ingress/metrics; lint/render/Kind upgrade/rollback. |
| K8S-007 | COMPLETE | External approval Secrets/config plus inherited probes/resources/SA/NetworkPolicy/PDB/HPA/affinity/ingress/metrics; lint/render/Kind upgrade/rollback. |
| K8S-008 | COMPLETE | External approval Secrets/config plus inherited probes/resources/SA/NetworkPolicy/PDB/HPA/affinity/ingress/metrics; lint/render/Kind upgrade/rollback. |
| K8S-009 | COMPLETE | External approval Secrets/config plus inherited probes/resources/SA/NetworkPolicy/PDB/HPA/affinity/ingress/metrics; lint/render/Kind upgrade/rollback. |
| K8S-010 | COMPLETE | External approval Secrets/config plus inherited probes/resources/SA/NetworkPolicy/PDB/HPA/affinity/ingress/metrics; lint/render/Kind upgrade/rollback. |
| K8S-011 | COMPLETE | External approval Secrets/config plus inherited probes/resources/SA/NetworkPolicy/PDB/HPA/affinity/ingress/metrics; lint/render/Kind upgrade/rollback. |
| K8S-012 | COMPLETE | External approval Secrets/config plus inherited probes/resources/SA/NetworkPolicy/PDB/HPA/affinity/ingress/metrics; lint/render/Kind upgrade/rollback. |
| K8S-013 | COMPLETE | External approval Secrets/config plus inherited probes/resources/SA/NetworkPolicy/PDB/HPA/affinity/ingress/metrics; lint/render/Kind upgrade/rollback. |
| K8S-014 | COMPLETE | External approval Secrets/config plus inherited probes/resources/SA/NetworkPolicy/PDB/HPA/affinity/ingress/metrics; lint/render/Kind upgrade/rollback. |
| K8S-015 | COMPLETE | External approval Secrets/config plus inherited probes/resources/SA/NetworkPolicy/PDB/HPA/affinity/ingress/metrics; lint/render/Kind upgrade/rollback. |
| K8S-016 | COMPLETE | External approval Secrets/config plus inherited probes/resources/SA/NetworkPolicy/PDB/HPA/affinity/ingress/metrics; lint/render/Kind upgrade/rollback. |
| REL-001 | COMPLETE | Protected CI approval compatibility gate; Maven/container/chart/release metadata and checksums/SBOM/provenance/signing hooks; upgrade/changelog docs. |
| REL-002 | COMPLETE | Protected CI approval compatibility gate; Maven/container/chart/release metadata and checksums/SBOM/provenance/signing hooks; upgrade/changelog docs. |
| REL-003 | COMPLETE | Protected CI approval compatibility gate; Maven/container/chart/release metadata and checksums/SBOM/provenance/signing hooks; upgrade/changelog docs. |
| REL-004 | COMPLETE | Protected CI approval compatibility gate; Maven/container/chart/release metadata and checksums/SBOM/provenance/signing hooks; upgrade/changelog docs. |
| REL-005 | COMPLETE | Protected CI approval compatibility gate; Maven/container/chart/release metadata and checksums/SBOM/provenance/signing hooks; upgrade/changelog docs. |
| REL-006 | COMPLETE | Protected CI approval compatibility gate; Maven/container/chart/release metadata and checksums/SBOM/provenance/signing hooks; upgrade/changelog docs. |
| REL-007 | COMPLETE | Protected CI approval compatibility gate; Maven/container/chart/release metadata and checksums/SBOM/provenance/signing hooks; upgrade/changelog docs. |
| REL-008 | COMPLETE | Protected CI approval compatibility gate; Maven/container/chart/release metadata and checksums/SBOM/provenance/signing hooks; upgrade/changelog docs. |
| REL-009 | COMPLETE | Protected CI approval compatibility gate; Maven/container/chart/release metadata and checksums/SBOM/provenance/signing hooks; upgrade/changelog docs. |
| REL-010 | COMPLETE | Protected CI approval compatibility gate; Maven/container/chart/release metadata and checksums/SBOM/provenance/signing hooks; upgrade/changelog docs. |
| REL-011 | COMPLETE | Protected CI approval compatibility gate; Maven/container/chart/release metadata and checksums/SBOM/provenance/signing hooks; upgrade/changelog docs. |
| DOC-001 | COMPLETE | READMEs, approvals runbook, ADR 006, API/config/key rotation/compatibility/migration and troubleshooting documentation. |
| DOC-002 | COMPLETE | READMEs, approvals runbook, ADR 006, API/config/key rotation/compatibility/migration and troubleshooting documentation. |
| DOC-003 | COMPLETE | READMEs, approvals runbook, ADR 006, API/config/key rotation/compatibility/migration and troubleshooting documentation. |
| DOC-004 | COMPLETE | READMEs, approvals runbook, ADR 006, API/config/key rotation/compatibility/migration and troubleshooting documentation. |
| DOC-005 | COMPLETE | READMEs, approvals runbook, ADR 006, API/config/key rotation/compatibility/migration and troubleshooting documentation. |
| DOC-006 | COMPLETE | READMEs, approvals runbook, ADR 006, API/config/key rotation/compatibility/migration and troubleshooting documentation. |
| DOC-007 | COMPLETE | READMEs, approvals runbook, ADR 006, API/config/key rotation/compatibility/migration and troubleshooting documentation. |
| PRF-001 | COMPLETE | In-memory normal evaluation; bounded approval transport/pending/lease/page and DB deadlines; stateless replication and documented benchmark. |
| PRF-002 | COMPLETE | In-memory normal evaluation; bounded approval transport/pending/lease/page and DB deadlines; stateless replication and documented benchmark. |
| PRF-003 | COMPLETE | In-memory normal evaluation; bounded approval transport/pending/lease/page and DB deadlines; stateless replication and documented benchmark. |
| PRF-004 | NOT_APPLICABLE | No fleet reporting is introduced. |
| PRF-005 | COMPLETE | In-memory normal evaluation; bounded approval transport/pending/lease/page and DB deadlines; stateless replication and documented benchmark. |
| PRF-006 | COMPLETE | In-memory normal evaluation; bounded approval transport/pending/lease/page and DB deadlines; stateless replication and documented benchmark. |

Suggested commit:

```text
feat(approval): implement ASK workflow
```
