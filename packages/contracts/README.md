# OLO ToolGate Shared Contracts

This directory is a publishable contract set used by every ToolGate component.

## Canonical Sources

```text
schemas/   JSON Schema and other language-neutral data contracts
openapi/   HTTP API contracts
events/    event/webhook envelopes
```

## Generated / Publishable Bindings

```text
rust/        crate: olo-toolgate-contracts
java/        Maven: io.ololabs.toolgate:toolgate-contracts
typescript/  npm: @olo-labs/toolgate-contracts
php/         Composer: olo-labs/toolgate-contracts
```

## Development Rule

Applications in the monorepo should resolve these packages from the workspace.

Application source must import the same public package identity it would use after repository separation.

## Release Rule

All language artifacts for one contract set share the same SemVer.

Read [`../../CONTRACTS.md`](../../CONTRACTS.md).
