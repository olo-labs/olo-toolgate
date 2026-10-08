# Threat Model

A pending registry row, enabled directory metadata or public CSR cannot grant execution. Reapproval preserves key/owner binding; exact-key certificate recovery rechecks enablement and approval. Browser details expose public review metadata, not private enrollment codes, credentials or tool payloads.

See [Device registry and tool-call controls](../control-plane/device-registry.md).

## Purpose

Threats include malicious internet users, compromised AI agents, malicious MCP servers, compromised endpoint processes, stolen devices, malicious community packages, admin compromise and supply-chain attacks.
