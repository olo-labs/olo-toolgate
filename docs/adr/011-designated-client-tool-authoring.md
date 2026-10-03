# ADR 011: Signed source authoring and designated-client sandbox tests

Status: accepted for Module 10, 2026-10-03.

Control persists and validates bounded drafts; it never interprets, compiles,
builds images from, or runs author code. Additive inline script source is part of
the signed immutable tool registration. Python/Node/PowerShell/Shell runners
receive code through stdin data and evaluate it only inside the existing
no-network/no-host-mount/non-root/child-process-restricted OCI sandbox. Fixed
runner code is application-owned; author input never becomes host command text.
Native/Java/.NET continue using reviewed image-contained artifacts, with explicit
unsupported inline-source capability. Batch/WASM remain unsupported.

Tests are durable leased jobs targeted to one active enrolled device. The device
polls over direct mTLS and verifies an organization-signed, short-lived job binding
tenant/server/device/source digest/lease before confined execution. Results contain
only match/failure codes, never arbitrary stdout/stderr. Test execution has no
host/network/credential capability and cannot register a runtime tool or mint a
Gateway permit. Ordinary deployed calls retain fresh Gateway/ASK authorization.

COMPUTE is supported. Host files, network and ambient credential injection are
rejected; credential requirements are references in draft metadata, and an
unbound requirement prevents testing/sealing/deployment. This module provides no
credential bypass and does not depend on future Vault work for its compute path.

Sealing requires a matching successful test, locks the draft and reserves the
package/version immutably. It creates exact descriptor bytes for an independent
external release authority; the organization assignment signer is never reused
as a release signer. The supplied signing CLI executes trusted packaging code
outside Control, never author code. Existing release verification/mirror/desired
reconciliation performs deployment. Public preparation excludes device/job/tenant
state and organization credential references and repeats secret scanning.

No new server, broker, container engine in Control or mandatory infrastructure is
introduced. PostgreSQL persists drafts, versions and leases with audit/idempotency
in one transaction. Existing fleet signing/mTLS configuration enables the builder;
no new Helm setting or secret is needed. Native certification limitations inherited
from Modules 08/09 remain explicit and cannot be replaced by cross-compilation.
