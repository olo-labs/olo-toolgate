# Gateway

The Gateway is the smallest and most security/performance-sensitive customer runtime component.

## Responsibilities

- MCP ingress/routing.
- identity validation.
- resource extraction.
- ALLOW/ASK/BLOCK.
- permit issuance.
- runtime credential brokering.
- runtime audit.
- rate limiting.

## Non-Responsibilities

- Admin UI.
- package installation.
- arbitrary custom-code execution.
- policy authoring.

Read next:

- `request-lifecycle.md`
- `policy-bundles.md`
- `execution-permits.md`
