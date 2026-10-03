# Endpoint Client Foundation

Module 06 adds a native protected service and unprivileged CLI. It does not
execute tools, install packages, or issue Gateway runtime credentials.

Build with `cargo build -p olo-toolgate-client --locked`; test with
`cargo test -p olo-toolgate-client --locked`. The service uses the shared
canonical contracts. Read [the enrollment decision](../../docs/adr/007-endpoint-enrollment.md).

An administrator runs `olo-toolgate-client install --server https://control.example.com`.
The installer registers systemd on Linux, launchd on macOS, or a LocalSystem
Windows service. The ordinary user runs `olo-toolgate-client enroll` and confirms
the public code and key fingerprint in the browser. The enroller's signed IdP
token needs the `toolgate-enroller` role, `tenant_id`, and an enabled `user_id`.
The browser never receives the device code or private key.

The protected service discovers its configured HTTPS origin, verifies its signed
discovery document, generates a P-256 key locally and submits a signed PKCS#10
request. Control verifies possession, binds the key to the approved user/device,
and signs a client-only certificate for at most 24 hours. Heartbeats renew it
when less than 12 hours remain. The active device and owner are checked on every
check-in. Administrative revocation is durable and prevents retries and renewal.

`health` and `check-in` are bounded IPC commands. Unix sockets use kernel peer
UIDs; Windows pipes use OS impersonation SIDs and reject remote clients. The CLI
verifies that its IPC server is root or LocalSystem. Allowed peers are explicitly
listed in the administrator-owned configuration. There is no credential export,
arbitrary path, shell, execution or configuration mutation operation.

Unix state is owner-only and rejects writable ancestors and symlinks. Windows
keys use account-scoped DPAPI and protected ACLs; privileged installed files are
administrator-owned. The exclusive service lease and durable pending report
prevent duplicate processes from advancing the sequence independently. A lost
response retries the exact stored report. Offline, expired or revoked clients
are unready. Offline state grants no runtime permission. Readiness becomes stale
after two minutes without a successful check-in.

Configuration fields are `serverUrl`, absolute `stateDirectory`, `ipcEndpoint`,
`authorizedPeers`, optional absolute `caCertificatePath`, and
`requestTimeoutSeconds` (1–15). Linux uses `/etc/olo-toolgate/client.json` and
`/var/lib/olo-toolgate`; macOS uses `/Library/Application Support/OLO/ToolGate`;
Windows uses `C:\ProgramData\OLO\ToolGate`. Enterprise HTTPS roots may be added
through the protected CA file setting. Redirects, proxies and insecure TLS are
disabled. Discovery CA changes require administrator-led recovery; no automatic
trust replacement is implemented.

Control enrollment is opt-in (`toolgate.control.endpoint.enabled=true`). Set
`tenant-id`, `server-id`, `organization`, `control-url`, `gateway-url`,
`private-key-path` (PKCS#8 RSA, at least 3072 bits), and `ca-certificate-path`.
The device CA must differ from IdP and policy keys. Direct HTTPS must disable
insecure requests, request client certificates and configure a PKCS#12 device
CA trust store. Forwarded certificate headers are never identity evidence.
The existing PostgreSQL migration V5 holds bounded enrollment rows, durable
identity tombstones, and ordered check-in reports. Database rollback requires
backup restore; older Control versions do not support the new identity tables.

The Helm `control.endpoint` settings reference existing external device CA,
server TLS and device trust-store Secrets. No key is generated or stored in
chart values. Browser TLS and client mTLS must reach Control directly; ingress
TLS termination cannot authenticate device certificates. Apply ingress quotas
in addition to the server's 32 active enrollment records per tenant and five
second polling limit. Existing Control readiness/metrics and HA remain in use.

Uninstall stops/removes the OS service and retains identity by default.
`uninstall --purge` explicitly deletes only the fixed protected state files.
Revoke the identity in Control before retiring a machine. A failed partial
installation requires administrator recovery of the fixed service/config paths;
automatic repair and expired-certificate re-enrollment are not implemented.

CI builds six native OS/architecture combinations. `tools/client/package.py`
produces deterministic tar/zip archives, SHA-256 checksums and a dependency SBOM.
This is a release skeleton: protected signing and GitHub Release publication,
full native installation tests, and real TLS enrollment/reconnect browser E2E
still require completion before Module 06 can be declared done.
