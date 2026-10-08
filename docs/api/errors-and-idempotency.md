# Errors and Idempotency

## Purpose

Mutations accept `Idempotency-Key`. Same key+same request returns same logical result; same key+different request returns conflict.

Device approval uses `expectedApprovalRevision`; enablement uses the independent
directory `expectedRevision`. Stale revisions return 409 and require loading the
current record. Administrative retries replay the committed result and audit only
once; check-ins do not advance the approval revision.

HTTP 423 is a retryable installed-client suspension caused by disablement,
deapproval or approval expiry. Preserve the protected key and bounded retry state.
Permanent revocation and unauthenticated identities remain rejected. A retryable
error never authorizes a tool effect. See [device registry and controls](../control-plane/device-registry.md).
