# Module 02 requirement coverage plan

Prepared before implementation on 2026-10-02 after reading the master prompt,
requirements traceability, architecture/security documents and definition of done.

| Requirement IDs | Applicable | Implementation | Tests | Documentation | Release/deployment impact |
|---|---|---|---|---|---|
| ARC-001–005, ARC-007; CON-001–006 | yes | Domain/application/adapter layers; canonical control schemas | Domain, contracts, drift, local artifact | ADR 003; component README | Additive contracts and stable Maven identity |
| SEC-001–005, SEC-007–010; API-001–007 | yes | Signed JWTs, tenant/role checks, validation, durable idempotency | Signature/claim/role/tenant/error/replay tests | API and security configuration | Separate administrative trust domain; external secret references |
| TST-001–005, TST-008; QLT-001–006 | yes | Java checks and scan gates | Unit, PostgreSQL, HTTP and contract tests | Developer workflow; completion report | Required CI gates |
| DAT-001–005 | yes | PostgreSQL, Flyway, atomic audit/idempotency | Clean install, upgrade, rollback, concurrency | Persistence and upgrade guide | External database; additive migrations |
| OBS-001–005, OBS-007 | yes | Private probes/metrics; structured logs and correlation | Health, dependency and telemetry tests | Operations guide | Private management port |
| DKR-001–008; K8S-001–016 | yes | Production image and opt-in Control Helm resources | Container smoke/scan; Helm and cluster smoke | Deployment, HA and rollback | External PostgreSQL and Secrets |
| REL-001–007, REL-009–011; DOC-001–007 | yes | Version, publication, CI, OpenAPI and docs | Version/artifact/workflow checks | Changelog, compatibility and upgrade | Protected CI publication, SBOM and provenance |
| PRF-002–003, PRF-006 | yes | Cursor pages; bounded requests, imports, pool and timeouts | Limit and concurrent mutation tests | Limits and scaling | Stateless replicas |
| ARC-006; SEC-006; TST-006–007; OBS-006; REL-008; PRF-001, PRF-004–005 | no | No approvals/deployment lifecycle, custom execution, asynchronous jobs or critical authorization latency path | Not applicable | Individual reasons in completion report | Image signing remains unconfigured; Gateway and fleet behavior unchanged |

Every unique requirement ID receives COMPLETE, NOT_APPLICABLE with a reason, or
BLOCKED with evidence in the completion report. An applicable blocked gate prevents
declaring the module done. No later module is started.
