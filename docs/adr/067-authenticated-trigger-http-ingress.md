<!-- Copyright 2026 OLO Labs; SPDX-License-Identifier: Apache-2.0 -->
# ADR 067: Authenticated generic trigger HTTP ingress

**Status:** Proposed; local authentication implementation only. No ingress
endpoint, runnable webhook package or completed M7/M8 qualification is claimed.

## Context

The requested `webhook-receiver` is a server PUSH source. It must use the shared
trigger supervision, lease and admission path. Packages must not bind ambient
sockets, obtain raw signing credentials, add watch-specific Compose services or
change Gateway-to-client invocation routing. Later chat and Git webhook sources
must reuse the same generic ingress capability.

## Decision being implemented

### Callback and profile configuration (proposed)

External producers use HTTPS POST, not a filesystem socket:

```text
https://<trusted-public-origin>/api/triggers/ingress/<binding-id>
```

The path is a proposed generic ingress route; no current endpoint implements it.
The public origin must match the deployment's configured TLS/reverse-proxy
authority. A package cannot choose a new public listener or host. Control
creates a stable opaque binding ID for a reviewed tenant/source assignment and
returns the complete callback URL as read-only deployment status. The URL does
not authorize requests. Broker lookup supplies the owning tenant/binding;
request content cannot select another owner, workflow, credential or target.

The workflow-scoped source configuration will declare ingress authentication
scheme, secret references, body/freshness/rate/credit limits and event mapping.
The corresponding dispatch binding continues to select the configured workflow,
version, agent and message pointer. The exact config schema and canonical status
model must be implemented before this proposal is accepted as runnable config.
Secret values stay in the selected profile's private credential custody; profile
JSON contains references only. Provisioning exposes callback URL plus non-secret
producer parameters and a package-specific README/example producer.

Within the trusted runtime, broker registration/subscription is lease-bound.
Package events travel through the generic broker/trigger protocol and existing
durable event routing/admission. Any internal socket is platform IPC, not the
external callback address. Adding another ingress package must not require a
watch-name switch, separate listener container or Gateway-to-client protocol.
Ingress registration is revoked on disable, lease loss or credential revocation.

### Initial authentication primitive

The trusted ingress broker authenticates raw bounded requests before handing
untrusted data to a package. A package/profile declares its ingress and scoped
credential references; key bytes stay in broker custody. Binding ownership,
current lease/generation, source enablement and authority are checked separately
at admission. A valid signature is not a grant or an accepted trigger event.

The initial **OLO HMAC v1** mock/provider adapter signs this exact byte sequence:

```text
olo.trigger-ingress.v1\n
<tenant-id>\n
<immutable-binding-id>\n
<delivery-id>\n
<canonical-decimal-unix-seconds>\n
<raw-body-bytes>
```

The displayed `\n` is one LF byte, with no extra LF before/after the body.
The proposed HTTP adapter reads exactly one `Olo-Delivery-Id`, `Olo-Timestamp`
and `Olo-Signature` header and requires `Content-Type: application/json`.
Duplicate/ambiguous headers and framing are refused before authentication;
JSON/event-schema validation follows successful raw-body authentication.
These headers do not select tenant or binding: the callback lookup supplies
that context. Provisioning gives the producer their non-secret signing IDs.
IDs contain 1–128 ASCII letters/digits/dot/underscore/hyphen, with no whitespace.
The signature value is exactly `v1=` plus 64 lowercase hexadecimal HMAC-SHA256
characters. Timestamps have no sign, whitespace, leading zeros or fraction and
fit the Control safe-integer bound. Policy sets a maximum body size of 1–16384 bytes and
freshness tolerance to 1–300 seconds. A trusted current clock is supplied by
the broker; fail-closed clock/authority handling remains its responsibility.

Tenant and immutable binding are signed, so accidentally reused key material
does not authorize cross-tenant or cross-source delivery. Broker keys have
32–64 bytes. At most two active keys support explicit credential-rotation overlap;
all active keys are checked using the existing pinned `ring` verifier. Revoked
keys must be removed from current broker custody before admission. Neither
keys nor raw requests are rendered by diagnostic formatting.

This is one canonical authentication scheme, not a claim of compatibility with
GitHub, Grafana, Slack, PagerDuty or other provider signatures. Each provider
adapter needs its own primary protocol reference and deterministic mock tests.
Unsupported authentication schemes/wire versions are refused; no aliases,
migration, conversion or unauthenticated fallback is added.

## Admission and remaining implementation

Authentication returns a non-publicly-constructible authenticated request and
its raw-body digest, **not an acceptance receipt**. Durable replay protection
must atomically bind `(tenant, immutable binding, delivery id)` to payload digest
and the admitted trigger event. An identical duplicate returns its original
receipt; a conflicting body is rejected. Freshness alone is not replay protection.
No success HTTP response may be sent before durable admission. Backpressure,
revocation, failover and unknown outcomes must not silently rerun workflow effects.

Shared PUSH lifecycle/emitter integration (M7), server supervision/placement and
lease-bound credential/ingress brokerage (M8), public canonical contracts,
durable replay/admission, package descriptor/export, profile deployment and
mock HTTP → configured workflow/agent → confirmed result remain outstanding.
The original module weights and Done-when gates are preserved. Inbound HTTP is
requested catalog work outside earned baseline credit, not silently absorbed
into a completed module. No endpoint is exposed by this initial library.
