# Endpoint Enforcement

## Purpose

Strong enforcement requires credential custody and/or OS controls. PATH wrappers alone are convenience, not a security boundary.

Enrollment lists require the same `toolgate-enroller` role and enabled, same-tenant
user as enrollment review and approval. Lists include at most 32 unexpired pending
requests and expose only review metadata, never private device codes, CSRs or
certificates. Decisions still bind the human approval to the reviewed fingerprint
and commit identity, deadline, audit and idempotency atomically.

New approvals have an absolute `connectionExpiresAtUnixMs` deadline (24 hours
when omitted by an older API caller). Check-ins, socket authentication and tool
access reject expired approvals. Certificates and renewal are capped at the
deadline, rounded down to X.509 second precision. Tool leases are also capped at
the deadline, and open job sockets recheck authorization every five seconds.
