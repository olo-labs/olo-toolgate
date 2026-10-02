# Compatibility

Every release should publish:

```text
Gateway version
Control Plane version
Endpoint Client versions
Quickstart version
Package schema version
Policy bundle version
Marketplace API version
MCP versions
```

Packages declare minimum compatible:

```text
Control Plane
Gateway when relevant
Endpoint Client
runtime
OS/architecture
schema reader
```

Unsupported packages may be visible but cannot be installed.

## Module 02 compatibility baseline

| Component/artifact | Version | Compatibility |
|---|---|---|
| Control Plane | 0.3.0-dev | Java 21, Quarkus 3.40.1, PostgreSQL 17.11 tested; Flyway V2 required |
| Shared contracts | 0.3.0-dev | Canonical v1; additive Control models; frozen foundation corpus compatible |
| Gateway | product 0.3.0-dev | Module 01 behavior and runtime v1 API retained; no Control policy publication yet |
| Helm chart | 0.3.0-dev | Services opt-in; empty default; external database/secrets |
| Config snapshot | formatVersion 1 | Same-tenant import with optimistic tenant revision; unknown versions rejected |
| Endpoint/UI/Marketplace | pending | No cross-component workflow delivered by this module |
