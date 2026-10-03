# 007: Protected endpoint identity and browser enrollment

Status: accepted, 2026-10-03. Scope: Module 06.

Control owns pending enrollment, human confirmation, certificate issuance,
revocation and bounded batched reported state in existing PostgreSQL. Enrollment
uses a short-lived device code held by the protected service and a separate human
code shown in the browser. Signed IdP user/tenant claims bind the confirmed device
to an enabled directory user. The device proves possession through a verified
PKCS#10 request. A dedicated external device CA issues client-auth certificates;
neither browser nor server receives the device private key.

Discovery is HTTPS-only and signed with the dedicated device identity key.
The initial configured server origin and system/custom TLS roots establish trust;
discovery cannot silently change the Control origin. Signed Gateway identity and
device CA information are pinned in the protected local enrollment journal.
Control checks the actual TLS peer certificate and active registry on every
check-in. Forwarded certificate headers never establish identity. Device identity
is separate from organization deployment and Gateway runtime authorization.

The native Rust service owns identity and network operations. Linux/macOS use
owner-only protected storage and kernel-authenticated Unix sockets. Windows uses
service-account DPAPI and a restricted local named pipe with OS peer identity.
Only bounded health/enrollment/check-in operations cross IPC; there is no shell,
arbitrary file access, credential export or tool execution capability. The CLI and
browser stay unprivileged. Service installation uses fixed OS service-manager
commands and protected configuration; uninstall retains identity unless explicit
purge is requested. Root/Administrator remains outside the ordinary-user threat
model. No claim of resistance to machine administrators is made.

The loop reports one bounded inventory batch per interval, persists report sequence,
uses bounded reconnect backoff and marks offline/revoked clients unready. Offline
state cannot create runtime authorization. Existing Control replicas retain no
per-device session state. Package reconciliation, HotFolder and execution are out
of scope. Bouncy Castle handles PKCS#10/X.509; rcgen/ring/rustls handle native key
generation and TLS. No bespoke certificate encoder or mandatory CA service is added.
