# Shared Contracts

## Purpose

OLO ToolGate treats shared contracts as a **versioned, publishable product**.

The monorepo may consume contract packages directly from the workspace today, but application source code must use the same stable package/module names that will be published later.

That allows components to move into independent repositories without redesigning imports or protocol models.

## Source of Truth

Canonical contract sources live under:

```text
packages/contracts/
```

The source of truth is language-neutral:

```text
packages/contracts/
├── VERSION
├── contract-set.yaml
├── schemas/
├── openapi/
└── events/
```

Generated language bindings live beside the canonical source:

```text
packages/contracts/
├── rust/
├── java/
├── typescript/
└── php/
```

## Published Artifacts

One contract release publishes the same semantic version across all supported ecosystems:

```text
Raw contract bundle:
  olo-toolgate-contracts-<version>.tar.gz

Rust:
  olo-toolgate-contracts

Maven:
  io.ololabs.toolgate:toolgate-contracts

npm:
  @olo-labs/toolgate-contracts

Composer:
  olo-labs/toolgate-contracts
```

Additional SDK/API-client packages may be published separately later. The core contracts package should remain lightweight and business-logic free.

## Monorepo Dependency Rule

Inside this repository, components should use workspace/local dependencies.

Examples:

```text
Rust        local path/workspace dependency
Java        the local Gradle project with the same published Maven coordinates
TypeScript  npm/pnpm workspace package
Drupal/PHP  Composer path repository
```

Application code imports the published package identity even while using local workspace resolution.

When a component moves to another repository, only dependency resolution changes from:

```text
local workspace
```

to:

```text
published version
```

The source imports stay the same.

## Versioning

Use one contract-set SemVer:

```text
MAJOR  breaking wire/schema/semantic compatibility
MINOR  backwards-compatible additive contract change
PATCH  compatible correction/metadata/generator fix
```

Security-sensitive meaning changes must never be disguised as a patch.

## Generated Code

Generated bindings are checked in under each language package so:

- workspace consumers do not need a generator just to compile;
- a future standalone contracts repository is immediately publishable;
- CI can detect drift between canonical schemas and generated code.

Generation remains deterministic.

CI must fail when generated bindings are stale.

## Contract Libraries Must Contain

Appropriate shared definitions such as:

```text
identifiers
API envelopes
errors
policy inputs/decisions
resource models
tool/package models
deployment desired/reported state
execution permit claims
client enrollment models
Marketplace release/submission metadata
event envelopes
version/capability negotiation
```

They must not contain:

```text
database repositories
service implementations
authorization decisions
credential values
business workflows
network clients with hidden policy
component-specific runtime logic
```

## Independence Goal

The following should eventually be possible without changing application imports:

```text
olo-toolgate/
    apps/gateway
```

moves to:

```text
olo-toolgate-gateway/
```

and simply changes its dependency source from workspace path to:

```text
olo-toolgate-contracts = "<compatible version>"
```

The same rule applies to Java, TypeScript and PHP consumers.

## Implemented foundation

Module 00 supplies canonical v1 schemas, generated bindings, workspace and local
Maven artifact build modes, mandatory checks and protected release plumbing.
Run `make check`; see [foundation workflow](docs/development/foundation.md).
Module 01 adds closed runtime authorization/audit types and embeds canonical
schemas in the Rust package for offline boundary validation. Frozen v1 definitions
remain unchanged; current product/contracts versions are 0.2.0-dev.
