# OLO ToolGate chart

Module 03 embeds the administration console in the existing Control image at
`/console/`. The Control root-path TLS ingress also routes `/api/control/v1/` on
the same origin. Private management ports remain separate. No UI workload,
additional service, secret or chart values are needed. See
[console deployment and upgrade guidance](../../../docs/control-plane/admin-ui.md).

Gateway is disabled by default, so default/upgrade renders remain empty. Module 01
adds the real Gateway Deployment, separate runtime/management Services, SA,
ConfigMap, NetworkPolicy, PDB, configurable HPA, Ingress and ServiceMonitor.

`global.imageRegistry` defaults to `ghcr.io/olo-labs`. `global.imagePullSecrets`
accepts a list of objects containing only existing Secret names. Secret values
and unknown settings are rejected by `values.schema.json`. Enabling Gateway
requires an existing credential Secret; default policy grants no authorization.

```sh
helm lint --strict deploy/helm/olo-toolgate
helm template foundation deploy/helm/olo-toolgate
helm template foundation deploy/helm/olo-toolgate --is-upgrade
helm package deploy/helm/olo-toolgate --destination build/release
```

Disabled renders must be empty. Enabled install/upgrade variants undergo
Kubernetes schema validation, and Kind smoke tests install, auth, upgrade/rollback.
Chart version and appVersion follow the product VERSION. The OCI package path
is `oci://ghcr.io/olo-labs/charts/olo-toolgate`; protected CI publishes the chart.
See [foundation workflow](../../../docs/development/foundation.md).
See [gateway deployment](../../../docs/gateway/deployment.md) for TLS, allowed
network peers, credentials, probes, resources, security, metrics and scaling.

## Module 02 Control Plane

`control.enabled` adds a real Java administration workload with external PostgreSQL,
JWT verification key and credential Secret references. Defaults render no service.
See [configuration](../../../docs/control-plane/configuration.md) and
[deployment/HA/upgrades](../../../docs/control-plane/upgrades.md). Control is independent
of Gateway enabling and does not publish Gateway policies in Module 02.

## Module 04 signed bundles

Enable `control.bundle.enabled` with an external dedicated signing Secret.
Enable `gateway.bundle.enabled` with separate public-keyring and refreshed
fetch-token Secrets, an HTTPS Control endpoint, explicit `controlTo`/`dnsTo`
egress peers and TLS `controlPort`. Signed mode removes static policy from
the rendered Gateway config. `keyRevision`/`trustRevision` trigger rollouts
after external key changes. Secret material is never accepted in values or
rendered resources. Full fields, rotation and expiry behavior are documented in
[the bundle runbook](../../../docs/control-plane/policy-bundles.md).
