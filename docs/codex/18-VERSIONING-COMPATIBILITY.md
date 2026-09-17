# Versioning and Compatibility

## SemVer

Use for:

```text
product releases
contract set
Helm chart
published libraries
```

## Compatibility Matrix

Release should state:

```text
Gateway
Control
Client
Quickstart
Contract schema
Marketplace API
Helm
```

compatibility.

## N / N-1

Define and test supported compatibility window before stable release.

## Unknown Fields

Contracts should specify whether unknown fields are ignored/rejected.

Security-sensitive unknown semantics should fail safely.

## Upgrade

Support:

```text
rolling server upgrade
client staged upgrade
DB migration
Helm upgrade
rollback strategy
```
