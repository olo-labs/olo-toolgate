# API Contracts

## Canonical OpenAPI

HTTP contracts live under:

```text
packages/contracts/openapi/
```

## URL Version

Example:

```text
/api/v1/...
/api/marketplace/v1/...
```

## Error Envelope

Use stable machine-readable codes.

Do not make callers parse human messages.

## Pagination

Cursor pagination for large mutable collections.

## Idempotency

Documented mutation endpoints accept:

```text
Idempotency-Key
```

Persist request hash + result reference.

Same key/different payload => conflict.

## Compatibility

Additive fields must not break old clients.

Security-sensitive semantic changes require explicit version/compat review.
