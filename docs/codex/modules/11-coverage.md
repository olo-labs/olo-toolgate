# Module 11 requirement coverage plan

Prepared before implementation on 2026-10-04 after reading the master prompt,
traceability, governing documents and definition of done.

| Requirement IDs | Applicable | Implementation location | Test location | Documentation location | Release/deployment impact |
|---|---|---|---|---|---|
| ARC-001–006; CON-001–006 | yes | Control store ports/SQLite adapter; apps/quickstart; canonical APIs | SQLite/domain/contract and local publication gates | ADR 012; quickstart guide | Existing stable packages; additive quickstart profile |
| SEC-001–010; API-001–007 | yes | local identity/bootstrap/vault; existing signed Gateway/ASK/enrollment | password, vault, auth, ASK, enrollment and fail-closed tests | security and configuration guide | Fresh separate keys; no embedded secrets; loopback HTTP UI only, direct TLS enrollment |
| TST-001–006, TST-008; QLT-001–006 | yes | tests; bounded configuration; scan hooks | unit, negative, production Docker/browser E2E | completion evidence | Required CI smoke/scan gates |
| DAT-001–005 | yes, explicit non-HA exception | SQLite migrations; /data layout; transactional store | fresh/restart/upgrade/concurrency/backup restore | ADR 012; operations guide | One writer, durable external volume; production PostgreSQL unchanged |
| OBS-001–007; DKR-001–008 | yes | supervisor/health; Gateway/Control; image | real image readiness/shutdown/read-only and scan | operations guide | Multi-stage non-root image; aggregate health; SBOM |
| REL-001–011; DOC-001–007 | yes | protected release workflow; package/docs | release checks, downloads, upgrade and backup smoke | README; compatibility; quickstart guide | GHCR olo-toolgate-quickstart; protected CI publication/provenance |
| PRF-001–002, PRF-006 | yes | in-memory Gateway; bounded local state/login/tool operations | existing benchmark and limits tests | performance limits | No DB in normal Gateway authorization |
| ARC-007; PRF-003–005; TST-007 | no, quickstart scope | Explicit stateful single-node/non-HA package; production remains separate | No new performance-sensitive evaluator | ADR 012 and non-HA UI | No horizontal scale claim or new hot-path benchmark |
| K8S-001–012, K8S-015–016 | no | Docker evaluation package, not production Kubernetes service | Existing chart gates retained | Docker-only quickstart documented | No HA/PDB/HPA/Helm quickstart workload |
| K8S-013–014 | yes, regression | Existing production chart | lint/render/schema gates | completion evidence | Production chart retained |

All 104 individual IDs will receive COMPLETE, NOT_APPLICABLE with a reason, or
BLOCKED with evidence in the completion report. A blocked gate prevents DONE.
Native certification blockers inherited from Modules 06–10 remain visible.
No Module 12 is started. Remote publication requires protected CI credentials;
local implementation does not invent registry credentials.
