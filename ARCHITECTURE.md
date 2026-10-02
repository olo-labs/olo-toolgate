# Architecture

## One Sentence

OLO ToolGate separates **administration**, **runtime authorization**, **local execution**, and **community package distribution** into explicit trust boundaries.

## Customer Runtime

```text
                    ToolGate Control
                   policy / packages
                         / fleet
                           |
                   signed desired state
                           |
                           v
AI / MCP ---> ToolGate Gateway ---> Remote Tool
                  |
           runtime permit
                  |
                  v
            Endpoint Client
                  |
             Local Tool
```

## Marketplace

```text
Drupal
   |
Marketplace API
   |
Metadata / Object Storage / Queue
   |
Marketplace Worker
   |
Sandbox
   |
Signing Service
```

## Key Boundaries

### Control Plane

Decides:

```text
what configuration should exist
who belongs to teams
which package is assigned
which policy is active
```

### Gateway

Decides:

```text
is this exact action allowed now?
```

### Endpoint Client

Enforces:

```text
is this package assigned and valid?
can this exact local action execute?
```

### Marketplace

Answers:

```text
what community package/version exists?
who published it?
what does it declare?
is this exact release signed?
```

## Runtime Decision

```text
User
Team
Agent
Device
Tool
Action
Arguments
Resource
Environment
Context
   |
   v
ALLOW / ASK / BLOCK
```

## Trust Chain

```text
Marketplace Release Trust
        +
Organization Deployment Trust
        +
Gateway Runtime Authorization
```

## Read More

- [`docs/architecture/overview.md`](docs/architecture/overview.md)
- [`docs/architecture/trust-boundaries.md`](docs/architecture/trust-boundaries.md)
- [`docs/reference/architecture-master.md`](docs/reference/architecture-master.md)

## Shared Contracts

Cross-component contracts are published as versioned Rust, Java, TypeScript and PHP libraries from one language-neutral contract set. Monorepo components resolve them locally; separated repositories resolve the same package identities from registries.

See [`CONTRACTS.md`](CONTRACTS.md).

## Module 02 boundary

The Control Plane implements tenant-scoped administrative records and versioned
APIs with pure domain types, application ports/use cases and HTTP/JDBC adapters.
PostgreSQL externalizes directory, revision, audit and replay state; Flyway owns
migrations. An external IdP supplies signed administrative identities and roles.
Directory metadata neither provisions IdP accounts nor grants Gateway runtime
access. Policies remain administrative data until bundle publication is implemented.
See [ADR 003](docs/adr/003-control-plane-transactions.md) and
[Control operations](docs/control-plane/configuration.md).
