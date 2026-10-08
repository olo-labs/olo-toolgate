# Signed policy bundles — protocol and operations

Device gates complement signed policy bundles. HotFolder authorization is bound to local-hotfolder so its device-specific filters match; server-managed approval does not bypass deterministic policy or ASK permits.

See [Device registry and tool-call controls](device-registry.md).

Module 04 compiles reviewed directory policies in Control and distributes verified,
immutable snapshots to Gateway. [ADR 005](../adr/005-signed-policy-bundles.md) records
the trust model. Software/contracts/chart are `0.4.0-dev`; bundle format is v1,
independent of software version. No execution permits or ASK approvals are issued.

## Protocol and compilation

The canonical types are in
[bundle.schema.json](../../packages/contracts/schemas/v1/bundle.schema.json).
`SignedPolicyBundle` contains one RFC 7515 compact JWS. The protected header has
exactly `alg: RS256`, `typ: toolgate-policy-bundle+jws`, and a local `kid`.
Only dedicated RSA 2048-, 3072- or 4096-bit policy keys with exponent 65537 are supported.
Control signs the exact UTF-8 payload bytes with JCA SHA256withRSA; Gateway verifies
those bytes with ring before parsing the signed payload. This is
[standard JWS](https://www.rfc-editor.org/rfc/rfc7515), not JSON reserialization
or RFC 8785 canonicalization. Encodings use unpadded canonical base64url.

The payload binds issuer, audience, tenant, `formatVersion: 1`, sequence,
`version: 1.0.<sequence>`, source directory revision, issue/expiry UTC milliseconds,
grace, policy bytes, SHA-256 of those bytes and optional `rollbackOf`. All objects
reject unknown fields and duplicate JSON keys, including signed nested policy.
Unsupported versions, algorithms, headers, ASK, key URLs, unknown keys, wrong trust
claims, future issue times, expired input and bad hashes/signatures reject.

The compiler orders enabled policies by ID, sorts expanded identities, unions
explicit users and enabled team members, and intersects user/agent/device
dimensions. An unselected dimension is unrestricted inside the signed tenant;
a selected empty team contributes no rule. Enabled references and declared tool
actions/resource kinds must validate. Matching is exact, with BLOCK precedence
and default BLOCK. Directory changes require explicit publication to reach Gateway.
Choose short lifetimes according to revocation needs; changing a directory record
alone does not revoke a last-known-good snapshot.

Directory input is bounded to 512 active records/1 MiB. Compiled policy is at most
786432 bytes; the schema allows at most 4096 rules and the current compiler can
produce at most 512. Wire ingestion is capped at 1500100 bytes. No external schema,
tool code, URL or filesystem path in a rule is resolved during compilation/evaluation.

## Publish and rollback API

Use authenticated `toolgate-admin` access on `/api/control/v1`. The tenant is
derived from the verified JWT. Every POST requires a new `Idempotency-Key`.

| Endpoint | Behavior |
|---|---|
| `GET /bundles/current` | Current immutable JWS; admin/reader or dedicated `toolgate-bundle-reader`; no-store; sequence ETag |
| `GET /bundles/versions/{sequence}` | Same-tenant immutable history; admin/reader |
| `POST /bundles/publish` | Compile current directory; return 201 signed bundle |
| `POST /bundles/rollback` | Copy historical compiled bytes into a new signed sequence; return 201 |

Publication body example (values must match your current directory/sequence):

```json
{"directoryRevision":12,"expectedSequence":0,"lifetimeMs":60000,"graceMs":0}
```

`directoryRevision` must match `/config/export` and `expectedSequence` must match
the latest publication (zero initially). Lifetime is 1000–86400000 ms. Grace is
0–300000 ms, and additionally capped at each Gateway. Optional `gracePolicyIds`
is the administrator's explicit assertion that selected enabled ALLOW policies
represent low-risk reads. Unknown, disabled or BLOCK selections reject. All other
ALLOW rules block at expiry, including write, destructive and privileged operations.
Action names are not risk classifications. Prefer zero grace.

Rollback additionally requires `rollbackOf` identifying an existing older sequence,
and forbids `gracePolicyIds`. It preserves the historical policy bytes, their risk
classification and **source** directory revision, while the request revision is a
precondition on the current directory. It signs a new higher sequence with fresh
issue/expiry and the requested grace. Review historical grants before rollback.
It never rewinds Gateway or overwrites historical signed bytes.

Tenant transaction locking serializes publication against directory writes and
other publishers. Immutable history, audit and replay response commit together.
Same-actor identical replay returns the original bytes without another audit;
different content under the same key or stale preconditions returns 409. Signing
or database/audit failure rolls back all effects. Auth precedes replay. Missing
history is 404; disabled/unavailable signing is 503; invalid bodies are 400.

## External trust configuration

Control bundle publication is disabled by default. To enable it set:

| Environment variable | Meaning |
|---|---|
| `TOOLGATE_CONTROL_BUNDLE_ENABLED` | `true`; missing/invalid keys fail startup |
| `TOOLGATE_CONTROL_BUNDLE_PRIVATE_KEY_PATH` | External unencrypted PKCS8 PEM RSA signing key file, at most 16 KiB |
| `TOOLGATE_CONTROL_BUNDLE_KEY_ID` | Dedicated policy key identifier |
| `TOOLGATE_CONTROL_BUNDLE_ISSUER` | Exact issuer; default `control` |
| `TOOLGATE_CONTROL_BUNDLE_AUDIENCE` | Exact audience; default `gateway` |

The application `BundleSigner` port separates signing from compilation. The
delivered adapter uses an externally mounted private key; no KMS adapter is
claimed. Startup rejects reuse of the configured administrative IdP RSA modulus.
Keep Marketplace, deployment, permit and device keys separate as well. Never
store signing keys, plaintext fetch JWTs or runtime credentials in values, images,
source, logs or release assets.

In Gateway JSON remove `policy` and supply `bundleSource` (exactly one mode):

```json
{
  "url":"https://control.example.org/api/control/v1/bundles/current",
  "tenantId":"example","issuer":"control","audience":"gateway",
  "keyringPath":"/etc/toolgate/bundle-keys/keyring.json",
  "tokenPath":"/etc/toolgate/bundle-access/token",
  "minimumSequence":0,"maxGraceMs":0,"pollIntervalMs":5000,
  "fetchTimeoutMs":3000,"developmentLoopbackHttp":false
}
```

The keyring is a closed `{"keys":[{"keyId":"bundle-key-1","modulus":"<base64url RSA n>","exponent":"AQAB"}]}`
object with 1–16 unique IDs and moduli. It contains only public key components.
The token file is refreshed externally before its 900-second JWT lifetime ends.
Use a dedicated tenant-scoped IdP subject with only `toolgate-bundle-reader`.
This role can fetch current policy but cannot read directory/history or mutate.
Gateway rereads the bounded token file each fetch; keyring/config changes require
restart. Initial fetch is immediate, then polling is non-overlapping per replica.

Only a fixed administrator-selected HTTPS destination is supported in production.
TLS hostname/chain verification is mandatory; redirects and ambient proxies are
disabled. Explicit literal loopback HTTP is available only for isolated development.
Link-local, multicast, unspecified literal addresses and known metadata hosts
reject. DNS/HTTPS endpoint selection is an administrator trust decision: restrict
egress to the intended TLS endpoint and DNS resolver. Control's internal listener
requires a trusted TLS terminator; do not point production Gateway at cleartext.
Fetch interval is 100–60000 ms and total fetch deadline 100–30000 ms; response
status, media type, headers and streamed size are checked. Errors retain policy
only within its existing validity, and never fall back to static rules.

## Freshness, HA and monitoring

Each replica verifies before swapping one immutable `Arc` snapshot under a short
write lock. Concurrent evaluation uses a whole snapshot. No network/database or
signature work occurs in the authorization path. A post-audit reevaluation checks
the same normalized request against current policy before an ALLOW response.
An audit records the evaluated decision; if replacement/expiry occurs during its
acknowledgement, the response can conservatively BLOCK instead.

| State | Authorization | Readiness |
|---|---|---|
| EMPTY | BLOCK | 503 |
| FRESH | Signed matching policy | 200 |
| GRACE | Only explicitly classified low-risk read ALLOW; BLOCK precedence retained | 200 |
| EXPIRED | BLOCK for every action | 503 |

The deadline is expiry plus the smaller of signed/local grace. Wall-clock
high-water and monotonic deadlines prevent clock rollback extending validity or
reviving expired state. Same sequence/identical JWS is a no-op; lower sequences or
same-sequence different bytes reject. Failures cannot extend expiry. Liveness
continues during outages/expiry so recovery can fetch a fresh bundle.

Metrics on private management are `toolgate_bundle_state` (0/1/2/3),
`toolgate_bundle_sequence`, `toolgate_bundle_accepted_total`,
`toolgate_bundle_rejected_total`, and `toolgate_bundle_fetch_failed_total`, without
identity labels. Adoption logs include sequence, rollback flag and policy hash;
rejection/fetch logs use fixed reasons. Control audit includes BUNDLE_PUBLISH or
BUNDLE_ROLLBACK in the publication transaction. Fetch propagates W3C trace context.

Alert on EMPTY/EXPIRED, sustained fetch/rejection increments, and replicas lagging
in sequence. Check refreshed JWT claims, clocks, TLS/DNS/egress and key ID first.
Do not bypass verification or extend grace to restore readiness. Each Gateway
tenant configuration and keyring is independent; HA replicas may temporarily
differ while polling, so observe every replica before declaring revocation complete.
Restart starts EMPTY and requires a fresh verified current bundle; there is no
trusted on-disk cache. Bound poll load by replicas/interval and monitor Control.

## Deployment, rotation, migration and recovery

Helm requires existing `control.bundle.signingSecret`, `gateway.bundle.keyringSecret`
and `gateway.bundle.tokenSecret` when respective bundle modes are enabled. Secret
keys default to `private.pem`, `keyring.json`, `access-token`. Enable both components
explicitly. Gateway signed mode requires nonempty `gateway.networkPolicy.controlTo`
and `dnsTo`, and `gateway.bundle.controlPort` identifies the TLS endpoint port.
Configure runtime/management ingress peers separately. Private signing material
is mounted only in Control; all mounts are read-only and workloads remain non-root.
Use `control.bundle.keyRevision` and `gateway.bundle.trustRevision` to trigger
rollouts after external Secret updates. Pin tested image digests.

Rotation: deploy old+new public keyring to all Gateways and verify readiness;
change Control's external private key, key ID and keyRevision; publish a fresh
bundle; observe the new sequence on every replica; then remove old public keys and
roll out Gateways again. Removal always restarts from EMPTY and re-verifies current
policy. Key compromise requires replacement, an increased minimumSequence, zero
grace and reviewed deny publication; remaining old-key replicas must be drained.
Do not reuse IdP or release provenance keys.

Flyway V3 adds immutable tenant-scoped bundle history and audit operations. Runtime
grants are SELECT/INSERT only; UPDATE/DELETE are forbidden. Fresh install and V2→V3
upgrade are tested against PostgreSQL. Back up history/audit/tenant revisions and
replay together; config export is not a bundle or database backup. Roll out new
Gateway readers before publishing v1 bundles from new Control. Older software has
no signed-bundle reader, so do not claim N/N-1 signed-runtime compatibility; frozen
preexisting v1 contracts remain compatible. Future bundle readers must explicitly
retain supported formats and reject unknown ones.

Helm rollback restores workload/config, never rewinds publication/schema. Keep
V3 history intact; old Control can serve its earlier directory paths but cannot
manage bundles. Application downgrade after signed-mode adoption needs coordinated
reader/source planning. A restored database may have lower sequence than live
Gateways: keep serving a valid LKG only through its deadline, restore/advance the
authoritative sequence under privileged incident procedures, configure a minimum
accepted sequence, and publish a higher reviewed version before restarting replicas.
Anti-replay high-water is per process; a stateless restart alone does not preserve
that floor. Treat database rollback as an explicit incident, not ordinary policy rollback.

## Verification and release evidence

Run `make check`, `make policy-e2e`, `make benchmark`, `make security`, and
`python tools/control/cluster.py` with the documented toolchain. Tests use only
ephemeral private keys; frozen compatibility vectors contain public keys and
genuine signatures only. `tools/policy/test_vectors.py` regenerates vectors
explicitly for reviewed protocol changes; normal code generation never rewrites them.

The reusable [policy CI gate](../../.github/workflows/policy.yml) builds both real
images, runs real PostgreSQL/Control/Gateway tests, signed Helm render negatives,
contract compatibility and the signed evaluator benchmark. Protected image and
foundation releases depend on this gate. Existing image scans, CycloneDX SBOMs,
checksums and provenance cover new dependencies. Runtime policy JWS is distinct
from release image signing, which remains unconfigured. Local Kind proves external
signing mount, signed publication, HA persistence and Helm upgrade/rollback;
production HTTPS fetch/egress needs the operator's TLS endpoint and enforcing CNI.
