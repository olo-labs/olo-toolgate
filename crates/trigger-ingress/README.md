<!-- Copyright 2026 OLO Labs; SPDX-License-Identifier: Apache-2.0 -->
# Trusted trigger ingress authentication

This internal Rust library implements the first authentication primitive for
the shared server ingress broker in [ADR 067](../../docs/adr/067-authenticated-trigger-http-ingress.md).
It verifies bounded OLO HMAC v1 requests scoped to a tenant and immutable binding,
using broker-owned keys. It currently exposes no HTTP endpoint or workflow
admission API and is not a published trigger package.

The future package/profile declares ingress configuration and credential
references; the trusted broker alone loads key bytes. `Policy::new` sets a
maximum body size (1–16384 bytes) and freshness (1–300 seconds).
`Verifier::new` takes that policy and one or two active keys of 32–64 bytes.
`Scope::new` takes the broker-resolved tenant and immutable binding.
`Verifier::authenticate` checks a raw request against a trusted current clock.
Exact headers/signing input and the proposed callback configuration are in the ADR.
No IDs, raw body or keys are included in diagnostic formatting.

The result contains delivery ID, signed timestamp, unchanged body and body
digest. It is **not an acceptance receipt**. Integration must still enforce
duplicate-header refusal, JSON/event schemas, lease/generation, enablement,
authorization, credits and atomic replay/event admission. Repeated authentication
can succeed within the freshness window; durable admission must return the
original receipt for an identical delivery and reject conflicting payloads.
Never return HTTP success based on this library alone.

Tests use synthetic keys/clocks and an independent Python HMAC/SHA-256 vector:

```sh
cargo test -p olo-toolgate-trigger-ingress
```

They cover tampering, tenant/binding confusion, encodings, bounds, freshness,
key rotation/removal and redaction. No provider account or credential is needed.
Provider adapters, runnable webhook PUSH export, HTTP/broker/lifecycle integration,
durable replay/admission, package matrix registration and confirmed workflow
result acceptance remain outstanding. No M7/M8 completion is claimed here.
