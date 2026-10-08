# Architecture

Managed tool execution uses a trusted registered device, current owner/device enablement, installed-client connection approval, and the existing agent/tool/action/resource filters before every protected effect. Registration, green availability or deployment never grants execution.

See [Device registry and tool-call controls](docs/control-plane/device-registry.md).

Module 08 separates privileged identity custody from untrusted execution through
a local OCI sandbox port. Protected image/tool registration and fresh Gateway
authorization are separate inputs; JSON stdin/stdout never becomes command text.
See [ADR 009](docs/adr/009-managed-local-runtime-sandbox.md).

Module 07's native system service exposes only the fixed canonical built-in
catalog through OS-authenticated IPC. Capability-relative HotFolder access
requires enrolled readiness and fresh Gateway authorization before each effect;
copy/move authorize both endpoints. It continues independently of interactive
login. Control's anonymous home page can serve an immutable verified three-OS
client bundle; administrative APIs retain their existing authentication.
See [ADR 008](docs/adr/008-hotfolder-builtins.md) and
[the operations guide](docs/client/hotfolder.md).

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

Module 03 embeds the React console at Control's `/console/` origin. It uses
canonical OpenAPI and the shared TypeScript package, leaving validation,
authorization and policy meaning in the backend. See
[ADR 004](docs/adr/004-embedded-admin-console.md).

Module 04 makes Control the compiler/publisher and Gateway the verifier/evaluator
of tenant-bound signed policy snapshots. Publication/history/audit/replay share
one database transaction; signature verification and bounded fetching are outside
the authorization path. Atomic snapshots, monotonic sequences and freshness
deadlines preserve fail-closed behavior during outages and rollback. See
[ADR 005](docs/adr/005-signed-policy-bundles.md) and
[protocol/operations](docs/control-plane/policy-bundles.md).

Module 05 adds durable human ASK decisions in Control while Gateway retains runtime
policy checks and separate permit signing. PostgreSQL serializes human decisions,
once grants and strict jti consumption across stateless replicas. Approval failure
blocks ASK, and approved requests cannot bypass current BLOCK or expiry. See
[ADR 006](docs/adr/006-ask-approvals.md) and [approval operations](docs/control-plane/approvals.md).
