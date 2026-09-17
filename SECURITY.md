# Security Policy

Security issues should **not** be reported through a public GitHub issue.

## Reporting a Vulnerability

Preferred:

1. Use GitHub's **Private Vulnerability Reporting** for `olo-labs/olo-toolgate` when enabled.
2. If private reporting is temporarily unavailable, open a minimal non-sensitive maintainer contact request without publishing exploit details.

Do not include secrets, customer information, or working exploit details in public Discussions/issues.

## Scope

Security-sensitive areas include:

- Gateway authorization bypass
- execution-permit forgery/replay
- endpoint-client privilege bypass
- package signature bypass
- Marketplace supply-chain compromise
- sandbox escape
- credential leakage
- Vault integration
- OAuth/OIDC
- SSRF
- path traversal
- command injection
- package installation/update
- key management

## Security Principles

- default deny
- deterministic authorization
- no LLM security decision
- fail closed
- separate trust-domain signing keys
- credentials never enter ordinary prompts/packages/logs
- untrusted code never executes inside the Control Plane or Drupal web process
- published package versions are immutable

## Responsible Disclosure

Please allow maintainers time to investigate and coordinate a fix before public disclosure.
