# Gateway deployment and operations

`make containers` or `python tools/gateway/container.py` builds and smokes the actual
digest-pinned multi-stage image. The distroless runtime is non-root, read-only-root
compatible, has OCI version/license labels and no embedded config or credentials.
The pinned Alpine Rust builder produces a musl static binary for the pinned
distroless static runtime, which carries no unused OpenSSL or shell.
Cargo dependency notices and Rust standard-library/musl license notices are
preserved under `/usr/share/licenses/olo-toolgate/third-party` in the image.
Registry identity is `ghcr.io/olo-labs/olo-toolgate-gateway:<VERSION>`; Helm accepts
an immutable image digest. No mutable latest tag is issued.

Enable the [chart](../../deploy/helm/olo-toolgate/README.md) after provisioning
credential JSON in an existing Secret:

```sh
kubectl create secret generic gateway-runtime --from-file=credentials.json=/secure/credentials.json
helm upgrade --install toolgate deploy/helm/olo-toolgate \
  --set gateway.enabled=true --set gateway.credentialsSecret=gateway-runtime \
  -f /secure/gateway-values.yaml --wait
```

Default policy blocks everything. Supply reviewed static inputs under gateway.config,
trusted TLS termination and permitted NetworkPolicy peers. Default pod ingress and
egress are denied. Runtime Service exposes 8081; management Service exposes 9091.
Do not publicly expose management. Node-origin probe handling depends on the CNI.
Optional Ingress requires class/TLS Secret and should enforce TLS redirect. Optional
ServiceMonitor needs the Prometheus Operator CRD and allowed management peers.

Two replicas, rolling maxUnavailable=0, PDB, configurable HPA/resources, restricted
security contexts, topology spreading, affinity/tolerations/nodeSelector are supplied.
No session affinity or persistent volume is required. Every replica must receive
the same immutable snapshots. HPA needs metrics-server; rates remain per replica.
Config checksum changes roll pods. Secret content changes require a rolling restart.
Termination grace must exceed application drain deadline.

`python tools/gateway/check.py` validates disabled, base, HA, ingress, monitoring,
network-selector and upgrade renders with Helm and Kubernetes schemas. ServiceMonitor
uses the SHA-256-verified Prometheus Operator v0.80.1 structural CRD schema.
`python tools/gateway/cluster.py` creates its own unique
Kind cluster, loads the image and tests install, authorization, deny upgrade and
rollback. Use Kind 0.27.0, kubectl and native Helm 3.17.3 on PATH or the
TOOLGATE_KIND_PATH/TOOLGATE_KUBECTL_PATH/TOOLGATE_HELM_PATH overrides. Only its own
cluster is deleted. CI runs this path on Ubuntu/Docker. Kind's default CNI smoke
does not prove production NetworkPolicy enforcement; validate it with your CNI.

Readiness 503 means expired policy/credentials, audit failure or drain. Liveness
does not depend on external Control/Vault availability. 401 indicates invalid
credentials; 400 indicates invalid input/extraction. 429/503 may indicate rate,
concurrency or audit backpressure. 504 means the deadline elapsed with no ALLOW
response. Inspect bounded metrics and collector health. Stdout audit acknowledgement
means local write/flush, not durable retention; configure a protected collector.
Stderr logs carry request/trace IDs and status, never bodies/tokens/resource locators.
