# How to run and use ToolGate

This guide describes the implemented 0.7.0-dev system: Control with its embedded
Admin UI, Gateway, and the native endpoint client. Read the
[debugging guide](debugging.md) when startup, enrollment or tool execution fails.
The repository is pre-alpha; native OS service CI gates remain open in the
[Module 07 report](../codex/modules/07-completion.md).

## Choose an environment

| Environment | Execution | Configuration and identity |
|---|---|---|
| Local UI demonstration | `tools/ui/check.py --serve`, real temporary PostgreSQL and Control | Generated short-lived test tokens; loopback only; removed on exit |
| Contributor debugging | Local Java/Rust processes and optionally Vite | Isolated database, explicit environment variables and protected JSON files |
| Production | Reviewed container images through Helm; native OS client services | External PostgreSQL, IdP, TLS and distinct signing keys; existing Secret references |

Run commands from the repository root unless stated otherwise. Examples use
`python`; substitute `python3` on systems where that is its name. `make dev`,
`make client`, the Compose stack and the README quickstart image are currently
scaffolds or future packaging plans. Use the commands below for current execution.

## Run the UI locally

Install Java 21, Node 22.12+, Python 3.12+ and Docker. Start Docker, then:

```sh
python -m venv .venv
```

Activate with `. .venv/bin/activate` on Linux/macOS or
`.\.venv\Scripts\Activate.ps1` in PowerShell, then:

```sh
python -m pip install -r tools/requirements.txt
npm ci --ignore-scripts
python tools/ui/check.py --serve
```

Open the printed `http://127.0.0.1:<port>/console/` URL. The port is selected
dynamically. Read the printed private credential file locally and paste its
`admin` value into the connection screen; `reader` demonstrates restricted access.
Tokens expire after five minutes. Restart the harness to create fresh tokens.
Use Users to create/edit records; Teams, Agents, Tools, Policies and Clients
provide inspection. Directory records do not create IdP accounts.

Keep the terminal open. Ctrl+C stops the owned Control process and database and
removes temporary credentials. Do not use these credentials or the temporary
database in production. This harness does not enable public binary downloads,
endpoint enrollment or a complete client/Gateway tool environment.

For browser tests instead of an interactive session:

```sh
npx --no-install playwright install --with-deps chromium
python tools/ui/check.py
```

## Run Gateway locally

Install the repository's pinned Rust toolchain. Generate expiring development
credentials and start the static example (Linux/macOS):

```sh
python tools/gateway/local_credentials.py
export TOOLGATE_GATEWAY_CONFIG="$PWD/docs/examples/gateway-static.json"
export TOOLGATE_GATEWAY_CREDENTIALS="$PWD/.dev/gateway/credentials.json"
cargo run -p olo-toolgate-gateway --locked
```

PowerShell uses:

```powershell
python tools/gateway/local_credentials.py
$env:TOOLGATE_GATEWAY_CONFIG = "$PWD/docs/examples/gateway-static.json"
$env:TOOLGATE_GATEWAY_CREDENTIALS = "$PWD/.dev/gateway/credentials.json"
cargo run -p olo-toolgate-gateway --locked
```

Runtime listens at `127.0.0.1:8081`, management at `127.0.0.1:9091`.
The example grants only the exact demo `files.read` operation; it does not
authorize the built-in catalog. Generated credentials expire after one hour.
The private `.dev/gateway/token.secret` contains the bearer token; do not paste it
into logs or commit it. Static mode has no ASK. Use the
[Gateway API](../api/gateway-api.md) for request shapes and the
[configuration reference](../gateway/configuration.md) for extractor bindings.

Use the implemented isolated integration gates to exercise signed bundles, ASK,
and real client enrollment/tool calls rather than assembling insecure substitutes:

```sh
python tools/policy/check.py --build
python tools/approval/check.py --build
python tools/client/integration.py
```

These are verification harnesses, not persistent production launchers. Read each
script's `--help` and prerequisites before running it; client integration requires
built images/binaries as described in its source and the Module 07 report.

## Prepare production configuration

Deploy only reviewed, compatible images and archives. The release matrix and
[release process](../development/release-process.md) describe version matching,
SBOMs, checksums and external signing hooks. Development archives are unsigned;
macOS notarization is not configured.

Provision these inputs before enabling their features:

| Component | Required inputs | Configuration reference |
|---|---|---|
| Control | External PostgreSQL with trusted TLS; separate migration/runtime logins; `toolgate_control_runtime` role; RSA IdP public key; exact issuer/audience | [Control configuration](../control-plane/configuration.md) |
| Gateway | Credential digest JSON bound to tenant/principal/device; extractor registry; static reviewed policy or verified bundle source; trusted runtime TLS edge | [Gateway configuration](../gateway/configuration.md) |
| Signed policies | Dedicated Control policy private key; Gateway public keyring and refreshed bundle-reader token | [Bundle operations](../control-plane/policy-bundles.md) |
| ASK | Enabled signed bundles; distinct Gateway permit key and machine token; enabled human approver directory user and role | [Approval operations](../control-plane/approvals.md) |
| Enrollment | Dedicated device CA; Control server TLS; device CA trust store; exact HTTPS Control/Gateway origins | [Client guide](../client/hotfolder.md), [client README](../../apps/endpoint-client/README.md) |
| Downloads | Verified three-platform package manifest embedded in Control image; feature enabled | [Client packaging](../client/hotfolder.md#downloads-deployment-and-release) |

Control defaults to ports 8082/9092, Gateway to 8081/9091. Management ports are
private probes/metrics, not public UI routes. Ordinary Control UI/API traffic may
use a trusted HTTPS ingress. Device enrollment/check-in requires TLS with client
certificates reaching Control directly, or an appropriately configured TLS
passthrough edge; TLS-terminating HTTP ingress cannot authenticate device mTLS.

Helm defaults disable both workloads and optional features. Copy/review
`deploy/helm/olo-toolgate/values.yaml` into an operator-owned values file outside
source control. Configure the following fields; no secret contents belong in it:

| Helm fields | Operator action |
|---|---|
| `control.enabled`, `gateway.enabled` | Explicitly enable required services |
| `*.image.digest`, `global.imageRegistry` | Pin tested images from the selected registry |
| `control.database.*` | Set host/name/port, `sslMode: verify-full`, credentials Secret and CA Secret |
| `control.publicKeySecret`, `control.jwt.*` | Reference IdP verification key and exact token claims |
| `gateway.credentialsSecret`, `gateway.config.extractors` | Mount runtime identity digests and bind each supported tool/action |
| `control.bundle.*`, `gateway.bundle.*` | Enable signing/distribution with separate key/token Secret references |
| `control.approval.*`, `gateway.approval.*` | Enable ASK with dedicated permit signing/token references |
| `control.endpoint.*` | Set tenant/server/org/origins and device CA/TLS/trust-store Secret names |
| `control.clientDownloads.enabled` | Enable only for an image with verified packages |
| `*.networkPolicy.*`, `*.ingress.*` | Allow explicit edge, monitoring, DB, Control and DNS peers; configure TLS |

The Control database credentials Secret keys are `username`, `password`,
`migrationUsername`, `migrationPassword`. IdP defaults to `public.pem`, database
CA to `ca.crt`, bundle key to `private.pem`, Gateway credentials to
`credentials.json`, public keyring to `keyring.json`, machine tokens to
`access-token`. Enrollment uses device CA `private.pem`/`ca.pem`, server
`tls.crt`/`tls.key`, and trust-store `truststore.p12`/`password`. Configure renamed
keys only where chart settings explicitly support them. Secrets must exist in
the release namespace. Apply external credential renewal and key rotation procedures.

Review configuration against the chart's closed values schema, then:

```sh
helm lint --strict deploy/helm/olo-toolgate -f /secure/toolgate-values.yaml
helm template toolgate deploy/helm/olo-toolgate --namespace toolgate -f /secure/toolgate-values.yaml > /secure/toolgate-rendered.yaml
helm upgrade --install toolgate deploy/helm/olo-toolgate --namespace toolgate --create-namespace -f /secure/toolgate-values.yaml --atomic --wait --timeout 5m
kubectl -n toolgate get deployments,pods,services
```

Review rendered network access before applying. Default empty peer lists do not
provide a usable production topology. Configure replicas/resources/PDBs and
monitoring to match capacity; ServiceMonitor requires its cluster CRD. Verify
every replica's readiness and bundle sequence before accepting traffic.
Back up PostgreSQL before rollout. Flyway migrations run on Control startup;
Helm rollback does not undo schema, policy sequences, audit or spent approvals.
See [Control upgrades](../control-plane/upgrades.md) for database recovery.

## Supply client downloads

Native release CI builds x64/ARM64 packages on the three operating systems.
For a binary already built and tested for its actual target:

```sh
python tools/client/package.py --binary /absolute/path/olo-toolgate-client --target x86_64-unknown-linux-gnu
python tools/client/manifest.py --source build/client/release --output deploy/client-assets/release
python tools/control/container.py --image olo-toolgate-control:verified-downloads --client-assets deploy/client-assets/release
python tools/client/downloads.py --image olo-toolgate-control:verified-downloads
```

Package each selected OS/architecture first; manifest assembly requires all three
platforms and validates executable headers and checksums. Windows takes a real
`.exe` and its appropriate target. The Control helper builds and smoke-tests the
image; it does not publish it. Publish via the protected release process and deploy
its digest. No ready-made `latest` image is guaranteed to exist.

Open the production Control origin: `/` redirects to `/console/`. Before login,
select OS/CPU, download and verify the displayed SHA-256, then extract. Downloading
does not grant enrollment or authorization. The default image reports unavailable
downloads rather than presenting fake artifacts.

## Install, enroll and execute protected tools

Run `install --server https://control.example.com` using the extracted binary,
with an elevated PowerShell on Windows or `sudo` on Linux/macOS. An administrator
reviews the protected configuration's `authorizedPeers` before ordinary users
use IPC. Run `enroll` from an explicitly authorized OS account and confirm the
browser code and key fingerprint using an organization-issued enroller token.
Wait for `health` to report ready.

An administrator then adds the `tools` object described in the
[HotFolder guide](../client/hotfolder.md#protected-configuration), provisions a
device-bound expiring Gateway token, configures matching tools/policies/extractors,
publishes the reviewed policy bundle, and restarts the client system service.
Tools are disabled on a fresh installation. Configuration paths are:

| OS | Protected client configuration | Service |
|---|---|---|
| Linux | `/etc/olo-toolgate/client.json` | `olo-toolgate-client.service` |
| macOS | `/Library/Application Support/OLO/ToolGate/client.json` | `system/io.ololabs.toolgate.client` |
| Windows | `C:\ProgramData\OLO\ToolGate\client.json` | `OloToolGateClient` |

From the extracted binary directory (PowerShell uses `.\olo-toolgate-client.exe`
in place of `./olo-toolgate-client`):

```sh
./olo-toolgate-client version
./olo-toolgate-client health
./olo-toolgate-client check-in
./olo-toolgate-client tools
./olo-toolgate-client tools hotfolder.read_text '{"path":"notes.txt"}'
```

Use catalog schemas for each operation's arguments. Paths are lowercase ASCII,
relative to the protected HotFolder. Check exit codes and canonical response
errors. Copy/move needs authorization for both paths. Never retry a mutation
blindly after losing its response. ASK requires an authorized human decision and
fresh exact-operation permit consumption; outage, revocation or expiry blocks.

The installed system service continues after logout/lock and at boot, independently
of the browser or CLI. It requires an awake powered machine and network access;
it is not an arbitrary scheduler. Verify native service startup and logout behavior
on your target OS before production rollout. To retire a device, revoke it in
Control, then run elevated `uninstall`; `--purge` removes fixed identity files
while preserving HotFolder documents.
