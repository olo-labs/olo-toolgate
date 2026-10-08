# Security Implementation Rules

For current maintenance, preserve the device registry flow and uniform execution gates. Treat pending enrollment, enablement, connection approval and tool filters as separate checks. Support explicit unlimited approval with short-lived certificates, reversible HTTP 423 suspension, stable approval revisions and exact-key recovery. Verify tenant/owner binding and real effect denial; do not infer access from installation, green status or a registry row.

See [Device registry and tool-call controls](../control-plane/device-registry.md).

## Authorization

LLMs do not make security decisions.

Authorization is deterministic.

## Policy Failure

Any security-processing failure defaults to deny/block.

## Permit

Permit validation must include where applicable:

```text
issuer
audience
tenant
user
agent
device
tool
operation
resource
request hash
issuedAt
expiresAt
jti
policy version
signature
```

## Credentials

Secret values:

- do not enter prompts;
- do not enter Marketplace packages;
- do not enter ordinary audit logs;
- do not enter exported config;
- do not enter metrics labels.

## Endpoint

PATH wrappers are not strong enforcement.

Use credential custody / privileged service / OS enforcement for protected operations.

## Supply Chain

Verify:

```text
Marketplace package signature
artifact hash
organization assignment/signature
runtime permit
```

at their appropriate boundaries.

## Untrusted Code

Never execute directly in:

```text
Drupal
Marketplace API
Control Plane
Marketplace Worker host namespace
```

## SSRF

Protect:

```text
metadata endpoints
link-local
loopback
private ranges by policy
redirects
DNS rebinding
Host override
```

## Audit

Security-relevant mutation gets actor, target, version, result, timestamp, trace/request ID.

## Cryptography

Do not invent algorithms or formats.

Use mature libraries and explicit ADRs.

## Security Review Requirement

Changes to:

```text
auth
authorization
signing
keys
vault
package verification
sandbox
client privilege
network egress
```

require dedicated security tests and review notes.
