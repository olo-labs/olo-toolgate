# Kubernetes and Helm Requirements

## Canonical Chart

Use:

```text
deploy/helm/olo-toolgate/
```

unless an ADR explicitly splits charts.

Expected structure:

```text
Chart.yaml
values.yaml
values.schema.json
README.md

templates/
  gateway-deployment.yaml
  gateway-service.yaml
  control-deployment.yaml
  control-service.yaml
  serviceaccounts.yaml
  configmaps.yaml
  ingress.yaml
  networkpolicies.yaml
  pdb.yaml
  hpa.yaml
  servicemonitor.yaml
  NOTES.txt
  _helpers.tpl

tests/
```

## Never Put Secret Values in Helm Defaults

Use:

```text
existingSecret
secretKeyRef
External Secrets
Vault integration
cloud secret store
```

## Workload Security

Default:

```text
runAsNonRoot: true
allowPrivilegeEscalation: false
readOnlyRootFilesystem: true where possible
capabilities.drop: [ALL]
seccompProfile: RuntimeDefault
```

Only relax per component with documented reason.

## Resources

Every workload gets configurable:

```text
requests.cpu
requests.memory
limits.cpu
limits.memory
```

with sensible defaults.

## Probes

Use component-specific:

```text
startupProbe
readinessProbe
livenessProbe
```

Do not use liveness to detect external dependency outages if it would create restart loops.

## Availability

Where applicable:

```text
replicaCount >= 2 production recommendation
PodDisruptionBudget
topologySpreadConstraints
anti-affinity
HPA
graceful termination
```

## Networking

Provide NetworkPolicy templates.

Separate:

```text
runtime ingress
admin ingress
Marketplace ingress
metrics ingress if needed
```

Egress should be intentional.

## Persistence

Gateway and Control Plane pods are stateless.

Persistent state belongs in external services.

## Observability

Optional ServiceMonitor/PodMonitor support.

Expose metrics ports through values.

## Tests

Required:

```bash
helm lint deploy/helm/olo-toolgate
helm template ...
```

Also validate rendered manifests using a Kubernetes schema validator.

Test at least:

```text
default values
HA values
ingress enabled
external secrets
metrics enabled
restricted NetworkPolicy
upgrade rendering
```

## Kind/K3d E2E

CI should eventually create a local cluster and install the chart.

Smoke test:

```text
pods Ready
health endpoints
Gateway request
Control API request
upgrade chart
rollback
```
