# OLO ToolGate Documentation

**Repository:** `https://github.com/olo-labs/olo-toolgate`

OLO ToolGate is a self-hosted platform to **build, distribute, authorize, and govern AI tools across users, agents, devices, and resources**.

This documentation is designed for two goals:

1. **A new user can run ToolGate in about one minute.**
2. **A new contributor can understand where to work in about one minute.**

## Start Here

### I just want to run it

Read:

- [`getting-started/one-minute-quickstart.md`](getting-started/one-minute-quickstart.md)
- [`getting-started/first-login.md`](getting-started/first-login.md)
- [`getting-started/first-tool.md`](getting-started/first-tool.md)

### I want to contribute

Read:

- [`getting-started/contributor-60-seconds.md`](getting-started/contributor-60-seconds.md)
- [`development/local-development.md`](development/local-development.md)
- [`architecture/component-map.md`](architecture/component-map.md)

### I want to understand the architecture

Read:

- [`architecture/overview.md`](architecture/overview.md)
- [`architecture/trust-boundaries.md`](architecture/trust-boundaries.md)
- [`architecture/data-flows.md`](architecture/data-flows.md)

### I want to build one component

- Gateway: [`gateway/README.md`](gateway/README.md)
- Control Plane: [`control-plane/README.md`](control-plane/README.md)
- Endpoint Client: [`client/README.md`](client/README.md)
- Marketplace: [`marketplace/README.md`](marketplace/README.md)

### I want the full original specifications

- [`reference/architecture-master.md`](reference/architecture-master.md)
- [`reference/marketplace-api-master.md`](reference/marketplace-api-master.md)

## Product Artifacts

Customer/runtime releases:

```text
olo-toolgate-quickstart
olo-toolgate-gateway
olo-toolgate-control
olo-toolgate-client
```

Public Marketplace infrastructure:

```text
marketplace-drupal
marketplace-api
marketplace-worker
```

## Core Authorization Model

```text
User + Team + Agent + Device + Tool + Action + Arguments + Resource + Context
                                      |
                                      v
                              ALLOW / ASK / BLOCK
```

## Documentation Rule

Keep files short and focused. If a document grows beyond what a contributor can scan quickly, split it and link the parts from the section README.

## Shared Contracts

- [`architecture/shared-contracts.md`](architecture/shared-contracts.md)
- [`development/contracts-release.md`](development/contracts-release.md)
