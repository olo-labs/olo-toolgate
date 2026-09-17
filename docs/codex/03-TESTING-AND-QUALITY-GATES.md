# Testing and Quality Gates

## Unit

Cover pure behavior:

```text
domain rules
state transitions
resource extractors
policy precedence
permit checks
contract conversions
sanitization
permission diffs
```

## Component

Exercise a module with its own adapters but without whole-system E2E.

## Integration

Use real dependencies where practical:

```text
PostgreSQL
MinIO/S3-compatible
OIDC test provider
HTTP server
filesystem sandbox
```

## Contract

Every shared contract change tests:

```text
serialization
deserialization
compatibility
unknown field behavior
versioning
error model
```

## Security

Test:

```text
invalid signature
expired token
replay
wrong audience
wrong device
path traversal
symlink escape
shell injection
SSRF
archive bomb
secret leakage
authorization bypass
race/idempotency
```

## E2E

Maintain deterministic flows:

```text
Quickstart boot
first login
policy publish
ALLOW
ASK
endpoint enrollment
HotFolder call
package deploy
Marketplace import
custom tool publish
rollback
```

## Coverage Guidance

```text
security/domain       >= 90%
contracts/converters  >= 90%
backend business      >= 80%
UI pure logic         >= 80%
```

Coverage does not replace meaningful tests.

## Static Gates

Eventually CI runs:

```text
Java compile with warnings-as-errors
Rust fmt/clippy
TypeScript lint/typecheck
PHP/Drupal standards
dependency vulnerability scan
secret scan
license scan
container scan
Helm lint
Kubernetes schema validation
```

## Flakiness

Flaky tests are defects.

Do not hide races with retries.
