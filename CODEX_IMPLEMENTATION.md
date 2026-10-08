# Codex Implementation

For current maintenance, preserve the device registry flow and uniform execution gates. Treat pending enrollment, enablement, connection approval and tool filters as separate checks. Support explicit unlimited approval with short-lived certificates, reversible HTTP 423 suspension, stable approval revisions and exact-key recovery. Verify tenant/owner binding and real effect denial; do not infer access from installation, green status or a registry row.

See [Device registry and tool-call controls](docs/control-plane/device-registry.md).

Use the implementation playbook under:

```text
docs/codex/
```

Start with:

1. [`docs/codex/README.md`](docs/codex/README.md)
2. [`docs/codex/00-MASTER-IMPLEMENTATION-PROMPT.md`](docs/codex/00-MASTER-IMPLEMENTATION-PROMPT.md)
3. [`docs/codex/CODEX-RUNBOOK.md`](docs/codex/CODEX-RUNBOOK.md)
4. [`docs/codex/09-REQUIREMENTS-TRACEABILITY.md`](docs/codex/09-REQUIREMENTS-TRACEABILITY.md)

Implement modules in order and commit each independently.

The traceability matrix exists specifically to ensure that testing, security, Docker, Helm/Kubernetes, Maven contract publication, container publishing, GitHub releases, observability, upgrades, documentation and supply-chain requirements are not silently omitted.
