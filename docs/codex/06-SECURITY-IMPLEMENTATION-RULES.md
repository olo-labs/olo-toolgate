# Security Implementation Rules

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
