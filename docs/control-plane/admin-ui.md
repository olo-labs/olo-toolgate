# Admin UI

## Purpose

Module 03 delivers the directory foundation at `/console/`. Root `/` redirects
there. React/TypeScript/Vite assets ship in the existing Control image: UI and
versioned API share one administrative origin.

## Authentication

Use HTTPS outside loopback development; token entry is disabled on insecure
non-loopback origins. Supply a short-lived token from the external organization
IdP with the claims required by [Control configuration](configuration.md).
The shell verifies connectivity through the backend and does not decode claims
or decide permissions. Refresh, disconnect or backend 401 clears the in-memory
session and aborts outstanding requests. The masked input clears on connection.
Tokens never enter storage, cookies, URLs, exported configuration or app logs.

Readers browse; the server rejects their mutation attempts with a visible error.
Directory users do not provision IdP accounts or roles. This shell accepts an
externally issued token; interactive OIDC redirects and Quickstart bootstrap
are not implemented or claimed. Do not deploy developer test keys or the isolated
test harness as a production identity provider. There is no default password,
authentication bypass or production mock.

## Management and failures

Dashboard values are first-page counts capped at 50, with `+` for further pages.
They are directory metadata. Six sections support cursor paging, record
inspection and loading/error/empty states. Clients display device records without
enrollment or online status. Policies read WHO / CAN USE / WHERE / stored
ALLOW-BLOCK. They have not been distributed to Gateway. Tool schemas render as
text. Advanced protocol fields remain behind an optional disclosure.

User create/edit/delete uses server validation and optimistic revisions. Conflict
preserves inputs and offers Reload current record before resubmission. No policy
or reference rules duplicate the backend. An exact retry retains its idempotency
key; changed values get a new key. Mutations are never retried automatically.
Deletion requires confirmation; retired IDs cannot be reused. Navigation or
disconnect drops unsaved form state.

Requests stop after 15 seconds; response memory is bounded at 2 MiB. Redirects
cannot carry tokens to another host. Errors show fixed safe wording and validated
request references. Raw exception bodies are never displayed. There is no browser
telemetry exporter.

## Accessibility

Semantic landmarks, skip link, labeled inputs, table headers, status/alerts,
visible keyboard focus, native disclosure controls and editor focus support
keyboard/screen-reader use. Playwright/axe checks WCAG A/AA tags on pages,
errors and forms. Desktop and 390px layouts, screenshots and keyboard focus are
verified. Automated checks are not a comprehensive accessibility certification.

## Deploy and operate

Enable the existing Control Helm workload and TLS root-path ingress.
`/console/` and `/api/control/v1/` must reach the same origin. Keep private 9092
management probes/metrics separate. Existing replicas, resources, NetworkPolicy,
external PostgreSQL/key Secrets, readiness and shutdown apply. No separate UI
service, container, CORS setting, secret or Helm value is required.

Static responses use strict CSP (self-hosted scripts/styles/API; no inline
execution or external frames), nosniff, no-referrer and disabled device APIs.
HTML/release metadata use no-store; hashed assets cache immutably. Preserve these
headers through proxies. Source maps and Vite's internal manifest are excluded.
Runtime MIT copyright/license notices are included. Rebuild UI and backend
together from the lockfile. Product version is injected into the console; shared
wire contracts remain v1 at the current 0.5.0-dev development baseline. The normal
version tool synchronizes the console for the next release. No DB migration is
added by this module.

During rolling upgrades, old HTML may reference assets absent on a new replica.
Refresh the console after rollout. Retain previous asset sets at the proxy if
uninterrupted old-document reloads are required. There is no service worker or
offline token cache. Existing Helm rollback restores matching UI/API images.

For blank pages check JS/CSS responses, CSP/proxy rewriting and artifact version.
For 401 obtain a fresh correctly issued token; for 403 check IdP roles; for 409
reload current data. Correlate safe backend logs with the displayed request ID.
Database outages produce safe retryable errors and backend readiness failure.

## Packaging plan

Protected Control CI builds/tests/scans the embedded assets, runs real browser
E2E, and collects asset checksums and a production npm CycloneDX SBOM. Existing
GHCR Control and OCI chart naming apply. Future Quickstart packaging reuses these
assets and origin, then supplies local identity bootstrap/routing in Module 11.
No Module 11 work is started here. See [developer commands](../../apps/admin-ui/README.md)
and [ADR 004](../adr/004-embedded-admin-console.md).
