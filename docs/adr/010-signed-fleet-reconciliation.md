# ADR 010: Signed fleet generations and immutable OCI package descriptors

Status: accepted for Module 09, 2026-10-03.

## Context

The existing client runs approved tools from immutable OCI images. Fleet packages
must not extract scripts into the privileged identity service or turn assignment
into runtime permission. Marketplace publishing is a later module; valid externally
signed release inputs are used here, with no fake signature or built-in trust key.
Control replicas share authoritative PostgreSQL state and external artifact storage.

## Decision

Introduce an additive fleet protocol. A bounded portable JSON package descriptor
contains package identity/version, OS/architecture/client compatibility, immutable
OCI runtimes, registered tools and declared confined self-tests. An independent
release signature binds its exact bytes. Organization desired signatures bind
explicit assignments to tenant, server, device, monotonic generation and expiry.
Deployment/release/IdP/device/policy/permit keys must remain distinct. Local trusted
keyrings select exact key IDs and reject unknown domains/algorithms.

Artifacts are immutable content-addressed descriptors in an organization mirror.
Control issues short-lived device/generation/digest-bound download grants after
current-assignment checks. The mTLS Control artifact route rechecks assignment and
revocation on download, proxies only a fixed configured HTTPS mirror path, never
caller URLs, redirects or archive extraction, and enforces byte/time/concurrency
limits. OCI image content stays in its approved registry and is verified/prepared
by the existing runtime boundary; private images must be administrator preloaded.
No additional mandatory server or queue is introduced.

Control stores immutable release records, durable rollout membership and per-device
monotonic desired generations. Canary/staged advancement is explicit, optimistic,
idempotent and audited; failed/offline clients cannot silently count as READY.
Batches and pages are bounded. Rollback is a new generation assigning an earlier
approved immutable release, never decrementing a generation. Uninstall is an
explicit absence assignment. Omitted assignments do not grant retained tools.

The client persists verified desired intent before downloading, verifies release
signature and exact hash/size, stages private descriptor bytes, checks compatibility,
prepares images and executes declared health probes solely in the sandbox, then
atomically replaces the active snapshot and publishes READY. No host installers or
package scripts run. Partial failure preserves prior staged bytes for recovery but
never permits a release that is no longer desired. Cached packages help recovery;
offline clients retain state and report failures, while protected execution still
requires current identity and Gateway authorization. Expired desired trust blocks
managed-package execution. Startup revalidates persisted signed metadata.

Local image-cache cleanup is conservative: uninstall removes registration/active
package state, not shared images or unrelated engine resources. The existing
fixed built-ins and explicit local registrations remain separate; collisions reject.
The UI displays authoritative rollout/device reports and actions; it computes no
security decision. Reported READY is device evidence, not server-side proof of code
safety or permission.

## Consequences

Package distribution handles JSON/OCI descriptors, not arbitrary archive installers.
Windows/macOS clients need the existing system-accessible Linux engine; unsupported
Batch/CMD/WASM or architecture capabilities fail closed. Module 08 native certification
limitations remain explicit. State/transfer quotas and retention are documented;
object-store outage never triggers a different origin or unverified cache activation.
