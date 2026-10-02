# Control Plane configuration and operations

The required external inputs are PostgreSQL and an identity provider issuing RS256
access tokens. There is no built-in password database, default account or generated
production credential. Directory users describe the organization; they do not create
identity-provider users or change their token roles. Disable/revoke IdP accounts in
the IdP. Directory changes do not publish Gateway policy bundles in this module.

| Environment variable | Meaning |
|---|---|
| `QUARKUS_DATASOURCE_JDBC_URL` | PostgreSQL JDBC URL; production requires `sslmode=verify-full` and a trusted CA |
| `QUARKUS_DATASOURCE_USERNAME`, `QUARKUS_DATASOURCE_PASSWORD` | Dedicated runtime login, member of `toolgate_control_runtime` |
| `QUARKUS_FLYWAY_USERNAME`, `QUARKUS_FLYWAY_PASSWORD` | Separate schema owner/migration login |
| `MP_JWT_VERIFY_PUBLICKEY_LOCATION` | Local mounted PEM RSA public key, at least 2048 bits; no signing key |
| `MP_JWT_VERIFY_ISSUER` | Exact HTTPS identity-provider issuer |
| `MP_JWT_VERIFY_AUDIENCES` | Dedicated administrative access-token audience |
| `TOOLGATE_CONTROL_MAX_RECORDS` | Active records across all kinds per tenant, 1–512; default 512 |
| `TOOLGATE_CONTROL_MAX_CONFIG_BYTES` | Serialized record bytes per tenant, 1024–1048576; default 1 MiB |
| `TOOLGATE_CONTROL_DEVELOPMENT_MODE` | Default false; explicit isolated local tests may use PostgreSQL without TLS |
| `QUARKUS_HTTP_PORT`, `QUARKUS_MANAGEMENT_PORT` | Defaults 8082 and 9092 |
| `TOOLGATE_CONTROL_TRACE_EXPORT_ENABLED` | Default false; enable sanitized outbound traces at runtime |
| `QUARKUS_OTEL_EXPORTER_OTLP_TRACES_ENDPOINT` | Explicit trusted collector URL; use HTTPS in production |
| `QUARKUS_OTEL_EXPORTER_OTLP_TRACES_PROTOCOL` | `grpc` (default) or `http/protobuf` |

Tokens require signed `sub`, `tenant_id`, `groups`, `iat` and `exp` claims. A token's
maximum lifetime and age are 900 seconds, with no expiry skew or future issue time.
Groups `toolgate-reader` and `toolgate-admin` may read directory/export/OpenAPI data;
only `toolgate-admin` may mutate, import or read audit entries. Tenant identity never
comes from a request body or header. Invalid identity claims fail closed.
Rotate verification keys through an explicitly coordinated IdP/key mount rollout.
There is no discovery/JWKS fetch, key cache or interactive login flow in Module 02.

Only 8082 belongs behind a trusted HTTPS ingress/proxy. Keep 9092 private to probes
and authorized monitoring. Proxy header forwarding and CORS are disabled. Do not
expose either internal cleartext listener directly to an untrusted network.
`/q/health/live`, `/q/health/ready` and `/q/metrics` run on 9092. Database loss makes
readiness fail and APIs return `DEPENDENCY_UNAVAILABLE`, without leaking SQL errors.
Logs contain fixed status/failure code, safe application call site, server request ID and trace ID; request bodies, identities,
tokens, paths and query strings are excluded. W3C `traceparent` propagates through
Quarkus/OpenTelemetry. Audit request IDs correlate with response headers and logs.
Before any configured span exporter receives data, an SDK customizer reduces spans
to trace IDs, timing, status, a fixed name, HTTP method/status and server request ID.
Raw URLs, paths, query strings, events, links, exception descriptions and trace-state
values are removed. SDK unit and real OTLP protobuf receiver tests verify this boundary.
The image builds the `cdi` exporter; runtime export remains disabled unless explicitly
enabled. Exporter selection is a build-time setting in
[Quarkus 3.40](https://quarkus.io/version/3.40/guides/opentelemetry/).

Requests are limited to 2 MiB, headers to 16 KiB, data nesting to 32, and pages to
100 entries. Each replica has a 16-connection pool with a 3-second acquisition
deadline. SQL and tenant-lock deadlines are 5 and 3 seconds, with a 10-second
driver socket deadline. Shutdown drains for 20 seconds; the chart allows 30 seconds.
Restrict ingress connections/rates at the organization edge. These are administrative
APIs, not the Gateway authorization hot path.

Startup refuses missing auth inputs, weak keys, identical migration/runtime logins
and runtime permission to change audit entries or create schema. Provision the
NOLOGIN role `toolgate_control_runtime` before migrations. The migrator owns the
dedicated database and `public` schema; the runtime login inherits the migration
grants only. Production migrators do not need `CREATEDB`; the isolated test helper
uses it to create fresh databases for clean-install/upgrade proofs. Supply credentials
via a secret manager/Kubernetes Secret, never the chart values or repository.

Use [deployment and upgrades](upgrades.md) for HA, migration and recovery. For a 503,
check database connectivity/TLS, NetworkPolicy DB/DNS peers, pool pressure and
readiness. For a 401, check issuer/audience/key, token claims and clock synchronization.
For a 409, refresh revisions and validate references, retired IDs and tenant quotas.
Do not bypass these checks to make a deployment ready.
