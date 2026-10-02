# Module 03 requirement coverage plan

Prepared before implementation on 2026-10-02 after reading the master prompt,
traceability document, governing documents, Module 02 API and definition of done.

| Requirement IDs | Applicable | Implementation location | Test location | Documentation location | Release/deployment impact |
|---|---|---|---|---|---|
| ARC-001–005, ARC-007; CON-001–006 | yes | apps/admin-ui; generated OpenAPI operation bindings; Control static assets | UI contract drift/typecheck; published-contract proof | ADR 004; UI README | Stable contracts identity; embedded same-origin console |
| SEC-001–002, SEC-004–005, SEC-007, SEC-009–010; API-001–007; QLT-006 | yes, UI/API boundary | memory-only authentication; bounded API adapter; user revision/idempotency headers | API negatives; real signed-token browser E2E; stale edit and reader denial | admin-ui.md | Backend remains authorization authority; no persistent token or external API URL |
| TST-001–006, TST-008; QLT-001–005 | yes | accessible React components; shared fixtures; check tooling | component/error/accessibility tests; real browser E2E; make check | UI developer workflow | Required CI gates and production assets |
| OBS-001–005, OBS-007; DKR-001–008 | yes, existing Control host | static serving headers; Docker Node build stage; existing telemetry | static asset/header checks; container smoke/scan | packaging and operations | Console included in existing non-root Control image and SBOM |
| K8S-001–016 | yes, inherited Control deployment | existing Control service/ingress/security/probes | lint/render; embedded-path container/cluster smoke | chart README; UI deployment notes | No additional service; root ingress serves UI and versioned API |
| REL-001–007, REL-009–011; DOC-001–007; PRF-003, PRF-006 | yes, applicable foundation | version plumbing; UI artifact/checksum; existing protected Control publication | production build; license/dependency/secret scans; release checks | compatibility, upgrades, completion report | Shared release version; immutable embedded assets; protected CI publication |
| ARC-006; SEC-003, SEC-006, SEC-008; TST-007; DAT-001–005; OBS-006; REL-008; PRF-001–002, PRF-004–005 | no new implementation | No lifecycle, execution, signing, database migration, async fleet or critical runtime evaluation path | Existing backend/Gateway gates retained | Completion matrix gives individual reasons | No later module started |

The primary path is authenticated browsing of users, teams, tools, policies,
devices and agents, a bounded directory dashboard, and user create/update/delete.
Stored policies and device metadata are not runtime grants or enrollment status.
Enterprise identities remain externally provisioned; this shell accepts an
externally issued signed access token in memory. Interactive OIDC and Quickstart
bootstrap are not claimed. Every individual requirement receives COMPLETE,
NOT_APPLICABLE with a reason, or BLOCKED with evidence in the completion report.
A blocked mandatory gate prevents declaring the module complete.

Implementation refinement: SEC-010 applies to the UI's use of the existing
durable mutation replay boundary; it does not introduce a new replay protocol.
