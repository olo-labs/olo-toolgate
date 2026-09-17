# Documentation Rules

## Every Feature

Update docs in same PR.

## Required Topics

Document:

```text
purpose
architecture
configuration
security behavior
failure behavior
API
metrics
runbook/troubleshooting
upgrade behavior
examples
```

where applicable.

## Examples

Examples must be:

```text
copyable
safe
fake credentials only
versioned
```

## Diagrams

Prefer simple text diagrams that render everywhere.

## Generated Docs

OpenAPI/contract generated docs should identify source and generation command.
