# Community Marketplace Platform

The Marketplace is independent public infrastructure.

```text
Drupal Web
   |
Marketplace API (Java, stateless)
   |
PostgreSQL / Object Storage / Queue
   |
Marketplace Worker (Java, stateless)
   |
Isolated Sandbox
   |
Signing Service / KMS
```

## Key Rule

**Community publishes capability. Organization administrators grant authority. Gateway authorizes each protected runtime action.**
