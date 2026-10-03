# Module 07 requirement coverage plan

Prepared before implementation on 2026-10-03 after reading the master prompt,
traceability, definition of done and HotFolder architecture. Module 06 is a
committed foundation with open validation gates; this module will verify the
identity/service boundaries it uses and will not assume an unsafe future fix.

| Requirement IDs | Applicable | Implementation location | Test location | Documentation location | Release/deployment impact |
|---|---|---|---|---|---|
| ARC-001–007; CON-001–006 | yes | endpoint tools/authorization/filesystem ports; canonical built-in IPC/catalog/download contracts | contract round trips/drift; tool and service integration | ADR 008; client docs; completion report | Additive protocol 2; synchronized version |
| SEC-001–005, SEC-007–010; API-001–007; QLT-001–006 | yes | capability-relative no-follow paths; protected OS service; online Gateway checks; bounded parsers/provider; audited operations; verified public artifacts | traversal/symlink/hardlink/ADS/limits/races/auth/outage/secret negatives | security and HotFolder runbook | No delete or offline authorization; anonymous downloads never grant enrollment |
| TST-001–006, TST-008; DAT-001–005; OBS-001–007; PRF-001–004, PRF-006 | yes | bounded events/reports; service/counters/logs/shutdown; existing Control state | real HTTP/TLS Gateway/tool flow; concurrent writes; logged-out service smoke; download browser E2E | operations/limits/config documentation | Existing service manager owns lifecycle; no user session required |
| DKR-001–008; K8S-001–016 | yes for existing Control only | Control download adapter and image assets/external mount; existing chart | image smoke/scan; Helm render; existing HA boundary | download publication/image docs | Native clients are OS services; no new Helm workload |
| REL-001–011; DOC-001–007 | yes | three OS client assets, OS/arch CI, artifact manifest/checksums/SBOM/signing hooks; anonymous landing page | real executable headers/version, packaging integrity and availability; static/browser checks | client/install/download docs; changelog/compatibility | Windows/macOS/Linux downloads on anonymous Control home page |
| SEC-006; TST-007; PRF-005 | no | No untrusted execution or new Gateway evaluator; fixed safe built-ins only | Existing Gateway benchmarks retained | Completion scope reasons | General local runtime remains out of scope |

Every ID will receive COMPLETE, NOT_APPLICABLE with a reason, or BLOCKED with
evidence. A blocked gate prevents declaring done. User authorization includes
implementation and a commit after review and verification; no push is requested.
No module beyond 07 is started. System services continue after logout; browser
enrollment remains a human action, and anonymous downloads expose no credentials.
