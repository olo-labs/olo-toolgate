# Canonical Package Model

Every package may contain:

```text
identity
version
package type
AI metadata
tool definitions
input/output schemas
runtime definitions
artifacts
permissions
resource mappings
credential requirements
compatibility
documentation
```

One model is reused by:

- Custom Tool Builder.
- API-to-MCP.
- Marketplace.
- private registry.
- import/export.
- Endpoint Client.

Published Marketplace versions are immutable.
