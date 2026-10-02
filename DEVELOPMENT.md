# Development

## Goal

A fresh contributor should need one command:

```bash
make dev
```

## Expected Developer Stack

`make dev` should start:

```text
Admin UI
Control Plane
Gateway
Mock MCP server
Mock REST/OpenAPI server
Development endpoint client
Development PostgreSQL
Development object store if required
Seed users / teams / policies / tools
```

and print the URLs.

## Stable Top-Level Commands

```bash
make dev
make stop
make check
make test
make integration
make e2e
make security
make benchmark
make containers
make client
make docs
make clean
```

## Local Data

All local developer state should live under a predictable ignored directory such as:

```text
.dev/
```

`make clean` may remove it.

## No Production Secrets

Local development uses generated/test credentials only.

Never require contributors to paste real cloud or company credentials to run the basic stack.

## Component Languages

```text
Gateway             Rust
Control Plane       Java 21+
Admin UI            React / TypeScript
Endpoint Client     Rust
Marketplace API     Java 21+
Marketplace Worker  Java 21+
Marketplace Web     Drupal / PHP
```

## Shared Contracts

Canonical contract set:

```text
packages/contracts/
  schemas/
  openapi/
  events/
  rust/
  java/
  typescript/
  php/
```

Use workspace dependencies now and the same published package identities after repository separation. See [`CONTRACTS.md`](CONTRACTS.md).

## Debugging

Every component should support:

- structured development logs
- trace IDs
- health endpoint
- useful startup error
- local debug configuration

## More

See [`docs/development/`](docs/development/).


## Java Build

Java services and libraries use **Gradle Kotlin DSL**.

```bash
./gradlew projects
./gradlew javaCheck
```

Shared Java contracts are a local Gradle project now and a published Maven-compatible artifact later.

See [`docs/development/gradle.md`](docs/development/gradle.md).

## Implemented foundation

Module 00 supplies canonical v1 schemas, generated bindings, workspace and local
Maven artifact build modes, mandatory checks and protected release plumbing.
Run `make check`; see [foundation workflow](docs/development/foundation.md).
The service modules remain build scaffolds and the Helm chart has no workloads.
