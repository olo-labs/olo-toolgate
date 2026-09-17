# Module 16 — Production, Helm, HA and Observability

## Dependencies

All primary runtime services.

## Objective

Make the system operable in a real Kubernetes environment.

## Mandatory Preparation

Read the master prompt and requirements traceability document.

Before coding produce the module requirement coverage table.

## Deliverables


- complete Helm chart;
- values schema;
- HA defaults/profile;
- ingress;
- TLS references;
- external PostgreSQL;
- external Vault;
- artifact store;
- optional Redis/queue;
- NetworkPolicies;
- HPA;
- PDB;
- topology spread;
- security contexts;
- resource profiles;
- ServiceMonitor;
- dashboards/alert suggestions;
- backup/restore docs;
- upgrade/rollback docs;
- kind/k3d CI installation.


## Mandatory Tests


- helm lint;
- render validation;
- install;
- readiness;
- horizontal scaling;
- pod disruption;
- rolling upgrade;
- rollback;
- external dependency outage behavior.


## Deployment / Release


- package/publish chart to OCI;
- attach compatibility metadata.


## Completion

Apply `docs/codex/08-DEFINITION-OF-DONE.md`.

Do not start another module.

## Suggested Commit

```text
feat(deploy): add production Helm and HA deployment
```
