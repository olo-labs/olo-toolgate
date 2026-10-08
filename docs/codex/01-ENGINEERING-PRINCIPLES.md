# Engineering Principles

For current maintenance, preserve the device registry flow and uniform execution gates. Treat pending enrollment, enablement, connection approval and tool filters as separate checks. Support explicit unlimited approval with short-lived certificates, reversible HTTP 423 suspension, stable approval revisions and exact-key recovery. Verify tenant/owner binding and real effect denial; do not infer access from installation, green status or a registry row.

See [Device registry and tool-call controls](../control-plane/device-registry.md).

## Optimize for Long-Term Change

ToolGate is expected to evolve from a monorepo toward potentially separated repositories.

Dependency direction must make that cheap.

## Backend Layering

Recommended:

```text
domain
application
ports
adapters
bootstrap/configuration
```

### Domain

No framework dependency.

### Application

Coordinates use cases.

### Ports

Real external boundaries.

### Adapters

HTTP, DB, queue, Vault, object store, OS/runtime.

## Dependency Inversion

High-level policy/use-case code depends on interfaces owned by the high-level layer.

Infrastructure implements those interfaces.

## Explicit State Machines

Required for:

```text
approval
deployment
package installation
Marketplace submission
Marketplace publication
revocation workflow
```

## Make Invalid States Difficult

Prefer:

```text
typed identifiers
enums
sealed hierarchies
validated constructors
immutable records
```

over unstructured strings/maps.

## Concurrency

Correctness must survive multiple replicas.

Use:

```text
transaction
optimistic locking
lease
idempotency key
distributed replay store
outbox
```

instead of in-memory correctness assumptions.

## Simplicity

Do not over-engineer.

An abstraction must correspond to:

- a real boundary;
- a real variation point;
- a real testing seam.

## Failure Is Explicit

Errors should distinguish:

```text
validation
authorization
conflict
not found
dependency unavailable
timeout
internal
```

and map to stable API codes.

## Repository Split Readiness

Moving a service out of the monorepo should mostly change:

```text
dependency resolution
CI pipeline
release ownership
```

not imports or domain logic.
