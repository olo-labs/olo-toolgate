# OLO ToolGate foundation chart

This metadata-only application chart renders no workloads. It has no running
service, probe, network policy or persistence to configure in Module 00.

`global.imageRegistry` defaults to `ghcr.io/olo-labs`. `global.imagePullSecrets`
accepts a list of objects containing only existing Secret names. Secret values
and unknown workload settings are rejected by `values.schema.json`.

```sh
helm lint --strict deploy/helm/olo-toolgate
helm template foundation deploy/helm/olo-toolgate
helm template foundation deploy/helm/olo-toolgate --is-upgrade
helm package deploy/helm/olo-toolgate --destination build/release
```

Default and upgrade renders must be empty. Kubernetes object validation and
cluster install/rollback tests become applicable when actual resources exist.
Chart version and appVersion follow the product VERSION. The OCI package path
is `oci://ghcr.io/olo-labs/charts/olo-toolgate`; protected CI publishes the chart.
See [foundation workflow](../../../docs/development/foundation.md).
