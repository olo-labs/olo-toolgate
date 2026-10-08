# Client API

## Purpose

Enrollment discovery, desired-state, reported-state, artifact grants and authenticated local IPC.

The current [device registry flow](../control-plane/device-registry.md) binds a
signed CSR to a reviewed human owner. Installed clients authenticate with their
actual TLS peer certificate; caller-supplied device IDs cannot choose an executor.
Check-in, relay jobs and protected effects require current owner/device enablement
and connection approval, followed by the applicable tool/resource filters.

Approval may be timed or explicitly unlimited. HTTP 423 signals reversible
disablement, deapproval or approval expiry: retain protected identity and retry.
Permanent key revocation remains rejected. Current clients can recover a short-lived
certificate for the same still-approved key after an extended outage; recovery
cannot change the owner or extend the approval deadline.
