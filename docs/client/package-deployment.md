# Managed package deployment: use, operate and debug

Module 09 adds fleet reconciliation to the protected endpoint service. It runs
without a desktop login when the OS service and system-accessible OCI engine run
at boot. The Module 08 Windows/macOS engine lifetime and ARM execution limits
still apply; unsupported packages fail instead of invoking host interpreters.
See [runtime configuration](local-runtimes.md), [service installation](install-and-enrollment.md)
and [ADR 010](../adr/010-signed-fleet-reconciliation.md).

## Trust and package format

Use the additive [fleet schema](../../packages/contracts/schemas/v1/fleet.schema.json)
and [Control API](../../packages/contracts/openapi/control-v1.yaml). Foundation
client/deployment wire contracts remain frozen. A descriptor is UTF-8 JSON, at
most 32 KiB, with packageId, immutable SemVer version, target platforms/architectures,
minimumClientVersion, digest-pinned OCI runtimes, tool registrations and a confined
JSON self-test for every tool. No ZIP/TAR extraction, install scripts or host hooks
exist. Native/Python/Node/PowerShell/Shell/Java/.NET follow Module 08 confinement;
Batch/CMD and WASM cannot activate.

An approved external release authority signs **exact descriptor bytes** using
RFC 7515 RS256 compact JWS. Header alg=RS256, typ=toolgate-package-release+jws,
kid names an approved release key. Canonical unpadded base64url is required;
unknown headers/algorithms and duplicate JSON keys fail. The release envelope
contains packageId/version/manifestDigest (lowercase SHA-256 of exact bytes),
sizeBytes and release.jws. Upload the raw descriptor to the immutable external
HTTPS mirror at `/DIGEST.json`, with Content-Type application/json. Uploading a
new descriptor under an old package/version is prohibited. Marketplace signing
is outside this module; configure valid independently approved inputs.

Organization assignment signs typ=toolgate-fleet-desired+jws and binds tenant,
server, enrolled device, monotonic generation, issue/expiry and the complete
assignment snapshot. Five-minute expiry blocks assigned tools; rollback is a
higher generation assigning a previously approved immutable version. Organization
deployment trust never grants runtime permission. The mTLS artifact grant has
its own typ=toolgate-fleet-artifact+jws, exact digest/size/device/generation and
60-second maximum lifetime. Every download rechecks current assignment/revocation.

Public key entries are `{kid,n,e}` with unpadded base64url RSA modulus, e=AQAB,
2048/3072/4096-bit RSA and at most four keys per domain. Release and organization
keys must be distinct; Control rejects reuse with IdP, policy signer or device CA.
Key rotation: deploy old+new public keys to Control and client first, switch kid
and signer second, wait for all clients and desired/grant expiry, then remove the
old key. Retain release keys needed to verify deployed/rollback versions.

## Production configuration

Existing Control PostgreSQL, direct TLS/mTLS and authenticated administrative APIs
are required. Run Flyway V6 with the migration role before serving; runtime role
cannot create tables or alter immutable releases. Fleet writes, generations,
idempotency and audit share one tenant-serialized transaction. Rollouts survive
replica restart. No new mandatory server or embedded artifact store is introduced.

Set TOOLGATE_CONTROL_FLEET_ENABLED=true and supply external mounted files:

| Setting suffix (prefix TOOLGATE_CONTROL_FLEET_) | Value |
|---|---|
| KEY_ID | Active organization signer kid |
| PRIVATE_KEY_PATH | Dedicated organization PKCS8 RSA key file |
| RELEASE_KEYS_PATH | JSON array of approved public release keys |
| ORGANIZATION_KEYS_PATH | JSON array including signer and overlapping rotation keys |
| ARTIFACT_ORIGIN | Fixed HTTPS origin without path/query/userinfo/fragment |
| ARTIFACT_CA_PATH | Optional mirror CA PEM bundle, at most four certificates |
| ARTIFACT_TOKEN_PATH | Optional private mirror bearer-token file |

No secret belongs in values, browser storage, package metadata, logs or a URL.
Mirror redirects and inherited proxies are disabled. The server allows two
concurrent transfers, at most 32 KiB, with a five-second total transfer deadline.
Configure immutable retention/backups and HTTPS authentication at the external
mirror. Private OCI registry images must be administrator-preloaded; this module
does not export registry credentials to tools. Existing allowFirstUsePull controls
approved public-image provisioning; unavailable images fail closed.

Helm control.fleet enables the feature. signingSecret has private.pem; trustSecret
has release.json and organization.json. artifactSecret, when present, has token
and ca.pem. Configure artifactOrigin, artifactPort and a narrow
control.networkPolicy.artifactTo peer/IP range; endpoint.enabled is mandatory.
The chart creates references and intentional egress, never signing keys or stores.

In the root-owned client configuration add deployment.releaseKeys and
deployment.organizationKeys arrays; supply existing execution engine settings and
tools Gateway/device identity settings. Empty execution.runtimes/tools is valid
for fleet-only registration. Local administrator registrations remain separate;
package tool/runtime IDs cannot collide with local or other assigned IDs. Reload
by restarting the OS service; IPC callers cannot upload trust keys or descriptors.

## Deploy, update, rollback and uninstall

1. Mirror the signed release descriptor and publish its release envelope using
   POST /api/control/v1/fleet/releases with a fresh admin token and Idempotency-Key.
2. Open **Packages** in the console, choose the release, enter enrolled device IDs,
   and create a canary rollout. The initial percentage selects at least one device
   using a stable hash order. Increasing it retains the original canary cohort.
3. Observe READY/FAILED/offline/waiting/pending/superseded from Control; advance
   with the current rollout revision. Offline and stale observations never count
   as READY. Revoked devices fail. A superseded rollout cannot silently advance.
4. Update by creating another rollout for a newer approved version. Roll back by
   creating a new rollout for an earlier approved version, never decreasing a
   generation. Uninstall by clearing desired presence; it removes registrations.
   Shared images remain cached, and no shared-engine image/container prune occurs.

Desired state is persisted as owner-only fleet-intent.json before downloads.
The client verifies descriptor hash/signature, platform/architecture/minimum
client version, closed registration graph and runtime constraints; it prepares
all candidate images, runs confined self-tests and atomically replaces
fleet-active.json before publishing READY. This record includes signed activation
metadata and observed status. Restart re-verifies both journals and repeats health
preparation; a saved READY flag alone cannot authorize tools. Interrupted downloads
are discarded and retried from the beginning. Failed install preserves the last
activation record while blocking tools from the superseded release. Recovery or
rollback requires a fresh verified desired assignment and online Gateway permission.

Bounds: 128 immutable releases and 256 rollouts per tenant, 256 explicit devices
per rollout, 16 assignments/device, desired snapshot at most 60,000 UTF-8 bytes,
16 runtimes/32 tools across package+local configuration. Pagination is 32 entries.
These are explicit foundation caps; plan retention/tenant partitions before the
cap. Do not delete records or reset generations in SQL to reclaim capacity.

## Debug environment and production troubleshooting

Build/check with make check (Windows: Python tools/check.py with
TOOLGATE_DOCKER_TOOLS=1), tools/control/check.py, and `python tools/deployment/check.py --control-image
olo-toolgate-control:module09 --binary PATH_TO_LINUX_CLIENT`. On Windows pass
`--docker-cli PATH_TO_REAL_LINUX_DOCKER_CLI`. Build the Control production image
from apps/control-plane/Dockerfile and the client release before this gate. Use disposable databases,
ephemeral dedicated keys and nonce-owned containers; test fixture keys are public
verification vectors, never deployment credentials. The integration harness's
static policy is valid test authorization, not a production bypass.

Use the console/API to inspect rollout counts and endpoint report.appliedRevision
and report.packages. Compare desired generation to applied revision; READY requires
exact generation, version and intended presence. Check structured fleet_reconcile
logs by generation/result; no tool arguments, credentials or raw stderr are logged.
Control exposes toolgate_control_fleet_operations_total with bounded operation
labels, existing HTTP latency/error metrics, OTLP correlation and mutation audit.
Client `health`, `runtimes status`, `runtimes prepare`, `run TOOL JSON` and OS service
logs distinguish device readiness, runtime health and Gateway permission. Inspect
protected journal metadata only as an authorized system administrator; never
export device-key/private keys while debugging.

| Symptom | Check/action |
|---|---|
| Publish rejected | Exact bytes/hash/size, release kid, closed schema and one self-test/tool |
| Rollout conflict | Fresh revision, existing release, active enrolled targets, caps, superseded canary |
| Offline | Service boot/lifetime, mTLS certificate/revocation, pinned Control/TLS origin |
| FAILED download | Fixed mirror CA/token/egress, immutable DIGEST.json bytes, redirects, 5s deadline |
| FAILED compatibility | OS/arch/min client; reviewed engine and supported runtime/image version |
| FAILED health | Confined JSON probe output/schema/timeout/memory; use isolated debug image |
| Staging after interruption | Leave journals intact; reconnect and allow a fresh signed generation/health retry |
| READY tool blocked | Desired expiry, device heartbeat, Gateway policy/ASK permit, tool resource binding |
| Wrong generation/report | Do not edit journals; restore server state or assign a new higher generation |

Back up PostgreSQL, mirror descriptors, trust-key history and managed engine cache.
Never restore client generation/identity independently of the fleet authority.
Upgrade older clients first; 0.8 clients accept only empty package reports and
cannot consume fleet assignments. 0.9 Control leaves fleet disabled until external
trust/store configuration validates. Remote image/chart/client release, signing,
SBOM and provenance use existing protected CI workflows.
