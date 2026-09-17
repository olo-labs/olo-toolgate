# OLO ToolGate Helm Chart

This directory is reserved for the canonical production Helm chart.

Implementation is governed by:

```text
docs/codex/05-KUBERNETES-AND-HELM.md
docs/codex/modules/16-production-helm-ha-observability.md
```

The Foundation module establishes chart metadata/skeleton.
Deployable service modules must add their own templates/values incrementally rather than postponing all Kubernetes work until the end.
