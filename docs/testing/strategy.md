# Testing Strategy

## Purpose

Layer unit → integration → E2E → security → performance → chaos. Keep deterministic fixtures and mock external services.

For the current [device registry](../control-plane/device-registry.md), cover
finite/unlimited approval, independent enablement, stable approval revisions,
owner/tenant binding, exact-key recovery and irreversible revocation. Prove denial
at actual server/client effect boundaries and retain resource/sandbox checks.
Exercise PostgreSQL and SQLite transactions and the combined name/user/status table.
