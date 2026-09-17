# Definition of Done

A module is DONE only if all applicable items are complete.

## Implementation

- [ ] Primary path fully implemented.
- [ ] No production mock.
- [ ] No insecure temporary bypass.
- [ ] No blocking TODO in primary path.
- [ ] Cross-component contracts use shared contract package.
- [ ] Architecture boundaries preserved.

## Tests

- [ ] Unit tests.
- [ ] Negative/error tests.
- [ ] Contract tests where applicable.
- [ ] Integration tests where applicable.
- [ ] Security tests where applicable.
- [ ] E2E updated where behavior is cross-component.
- [ ] Performance benchmark updated where critical.
- [ ] Tests deterministic.

## Code Quality

- [ ] Copyright/SPDX headers.
- [ ] Public/security-sensitive APIs documented.
- [ ] No unnecessary dependency.
- [ ] No duplicated contract model.
- [ ] No secret logging.
- [ ] Static checks pass.

## Operations

- [ ] Health/readiness.
- [ ] Structured logs.
- [ ] Metrics.
- [ ] Trace/correlation propagation where applicable.
- [ ] Graceful shutdown.

## Deployment

- [ ] Dockerfile updated/added if deployable.
- [ ] Non-root runtime.
- [ ] Helm values/templates updated if deployable.
- [ ] NetworkPolicy considered.
- [ ] Resource requests/limits configurable.
- [ ] Probes configured.
- [ ] HA behavior documented.

## Release

- [ ] Version behavior documented.
- [ ] Shared contract publication impact handled.
- [ ] Container publishing impact handled.
- [ ] Helm publishing impact handled.
- [ ] GitHub release impact handled.
- [ ] SBOM/provenance pipeline updated if needed.

## Docs

- [ ] README/component doc.
- [ ] Architecture doc if behavior changed.
- [ ] API/OpenAPI updated.
- [ ] Config documented.
- [ ] Upgrade/migration documented.

## Proof

- [ ] Commands executed are listed.
- [ ] Results are listed.
- [ ] Requirement traceability updated.
- [ ] Suggested commit message provided.
