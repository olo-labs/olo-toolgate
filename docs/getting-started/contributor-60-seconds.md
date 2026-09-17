# Contributor Setup in 60 Seconds

## Purpose

Help a contributor understand the repo and start the development stack immediately.

## Clone

```bash
git clone https://github.com/olo-labs/olo-toolgate.git
cd olo-toolgate
```

## Start Developer Stack

The repository should expose one command:

```bash
make dev
```

`make dev` must:

1. Validate required developer tools.
2. Start local development dependencies.
3. Build/start Gateway.
4. Build/start Control Plane.
5. Start Admin UI with hot reload.
6. Start a mock MCP/API upstream.
7. Start a development endpoint client in non-privileged mode.
8. Seed a local admin, team, policies and example tools.
9. Print URLs.

Expected output:

```text
OLO ToolGate development environment

Admin UI:       http://localhost:3000
Control API:    http://localhost:8080
Gateway:        http://localhost:8081
Mock MCP:       http://localhost:8090

Login:
  admin / dev-only-password

Try:
  make test
  make e2e
  make docs
```

## Find Your Component

```text
apps/gateway             Rust
apps/control-plane       Java
apps/admin-ui            React/TypeScript
apps/endpoint-client     Rust
apps/marketplace-api     Java
apps/marketplace-worker  Java
apps/marketplace-drupal  Drupal/PHP
```

## Before Opening a PR

```bash
make check
```

`make check` should run formatting, linting, unit tests, schema generation checks, and fast security checks.

## Architecture

Read [`../architecture/component-map.md`](../architecture/component-map.md) before modifying cross-component behavior.
