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
