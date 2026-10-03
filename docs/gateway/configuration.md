# Gateway configuration

`TOOLGATE_GATEWAY_CONFIG` and `TOOLGATE_GATEWAY_CREDENTIALS` select required JSON
files. File fields override compiled capacity defaults; there are no environment
policy/identity overrides or automatic reload. Unknown fields, duplicate extractor
bindings, expired inputs and invalid limits prevent startup. Each file is limited
to 1 MiB. Diagnostics disclose only fixed reasons, never contents or JSON exceptions.

The [development example](../examples/gateway-static.json) has one exact ALLOW rule;
Helm defaults grant nothing. Production supplies reviewed immutable policy and a
separate credential Secret. Credentials are an array with `tokenSha256`, `tenantId`,
`userId`, `agentId`, optional `deviceId` and `expiresAtUnixMs`. Use at least 256
random bits per token: digest storage assumes high entropy, not human passwords.
Each credential binds one tenant/principal/device. Generate local inputs with
`python tools/gateway/local_credentials.py`; never commit them.

Runtime defaults bind loopback. Non-loopback runtime binding requires
`trustedTlsProxy=true`, an explicit deployment assumption, not TLS validation or
trust in forwarded identity headers. Use a trusted TLS terminator and restricted
network peers. Management has separate restricted, unauthenticated probes/metrics.
Present Origin headers must match `allowedOrigins` exactly (HTTPS origins); absent
Origin is accepted for service clients. Wildcard CORS is unsupported.

Rules match tenant/tool/action/resource exactly, with optional exact user/agent/
device restrictions. Emergency BLOCK wins, then any matching BLOCK, then ALLOW,
then default BLOCK. Rule order cannot override a BLOCK. ASK is rejected in static
config; future evaluator ASK becomes BLOCK until approval exists. Policy version
is independent of software version. Policy and credential expiry during processing
cannot produce an ALLOW response; expiry also removes readiness.

Extractors bind tool/action to a JSON pointer and FILE/CUSTOM kind. Exact relative
locators are accepted. Missing values, dot segments, absolute paths, aliases,
backslashes, percent escapes, control characters and URI-like inputs reject. No
file/DNS/symlink/SQL resolution or network egress occurs. Future executors still
need their own confinement and SSRF enforcement.

| Limit | Default | Scope |
|---|---|---|
| maxBodyBytes | 65536 | Uncompressed streamed request body |
| maxHeaderBytes | 8192 | Runtime headers; transport also bounds count/buffer |
| maxConcurrentRequests | 128 | Admitted requests per replica |
| maxConnections | 256 | Per listener, including incomplete requests |
| requestsPerSecond | 1000 | Fixed one-second window per replica |
| requestTimeoutMs | 2000 | Body, policy and audit acknowledgement |
| connectionTimeoutMs | 10000 | Single-request HTTP/1 connection lifetime |
| shutdownTimeoutMs | 15000 | Listener and audit drain |
| auditQueueCapacity | 256 | Acknowledged stdout audit queue |

Rate protection uses no identity-indexed map; it is not a fleet-wide quota.
Connection/drain deadlines must cover request deadlines. Tune limits together with
resources and collector throughput. JSON parser depth is bounded and duplicate
keys reject recursively. Sorted normalized JSON determines argument digests; it
is not a general-purpose JCS/signature format.

Module 04 adds mutually exclusive `bundleSource` mode in place of static `policy`.
It requires a dedicated public keyring and externally refreshed Control access
token, verifies signed current bundles, and swaps whole snapshots atomically.
Readiness reflects verified freshness; grace allows only explicitly classified
low-risk reads, and expiry blocks every action. See the complete
[bundle configuration and failure runbook](../control-plane/policy-bundles.md).

The optional `approval` object requires signed bundle mode and dedicated external
Gateway permit signing material. [Approval configuration](../control-plane/approvals.md)
documents fixed origin, machine JWT refresh, strict trust, deadlines and atomic use.
