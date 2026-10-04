# Gateway and Control

Self-contained **local development/evaluation** stack: separate Gateway and Control
images, embedded Admin UI, PostgreSQL 17, authenticated Redis, HTTPS Nginx proxy and a bootstrap helper.
The helper reuses Quickstart's Python/cryptography runtime without starting Quickstart.
No source build or host SDK is required. State and credentials use named volumes.

## Start

Install Docker Desktop with Linux containers or Docker Engine with Compose v2.
Allow roughly 3 GB free RAM and internet for initial pulls. ToolGate images currently
support Linux AMD64; ARM hosts need Docker emulation. From this folder:

```powershell
.\manage.bat deploy
```

Linux/macOS:

```sh
sh manage.sh deploy
```

The script generates retained RSA identity/TLS keys and random DB credentials,
provisions separate migration/runtime SQL roles, starts services and checks both
Control and Gateway readiness. Re-running preserves database, keys and custom
Gateway policy. Only **127.0.0.1:8444** is published; DB and management stay internal.

The first deploy asks **Deploy bundled database and dependency containers? [Y/n]**.
Enter selects **Y**. `.env.example` is copied to `.env` only when missing. These
default image/port settings are used for Y; random database passwords are exported
to ignored `env/.env.db`, which Control loads through Compose `env_file` (raw format).
No default plaintext password is checked into the repository. Compose 2.30+ is
required for raw environment files and override/reset support.

On **n**, supply existing PostgreSQL host/port/database, separate runtime and
migration users/passwords and TLS mode. Default TLS is `verify-full`, which also
asks for a PEM CA file. `require`/`disable` are explicit development choices.
Passwords are hidden during interactive input and saved literally in `env/.env.db`;
the scripts never execute this file as shell code. Protect it with filesystem ACLs
(POSIX uses mode 0600) and never commit/share it. `env` and `.env` are git-ignored.

Existing DB must already have `toolgate_control_runtime` (NOLOGIN), a restricted
runtime user in that role, and a separate migration user owning the database/schema.
For the default names, run [provision-existing-db.sql](provision-existing-db.sql)
with PostgreSQL administrator `psql`; it prompts for role passwords. Edit identifiers
before running for custom names. Flyway applies application migrations on startup.
This script does not create/alter your existing database through an admin account.

After selecting n, a second prompt asks whether to deploy the bundled HTTPS proxy
(default Y). Selecting n requires an existing HTTPS proxy origin. Configure that
proxy to forward `/v1/authorize` and `/mcp` to host loopback `9081`, and console/API
paths to host loopback `9082`. Keep management paths inaccessible. These loopback
ports are published only in external-proxy mode. A proxy in another container needs
an explicitly shared network/host routing configuration; its `localhost` is not this
host. The public origin is recorded in `env/proxy-url.txt`. Startup verifies backend
readiness; you must also verify routing/TLS at your existing public proxy.

Saved choices in `env/mode.txt` are reused by all operations. Run `configure` to
change them, then `deploy`. Undeploy before switching modes so an old bundled
database/proxy is not left running; retained volumes allow returning to bundled mode.
An external database or proxy is never stopped by undeploy.

Open **https://localhost:8444/console/**. Export the self-signed local certificate:

```sh
docker compose --profile database --profile proxy cp proxy:/tls/server.crt ./localhost.crt
```

Verify and trust it in your local machine/browser according to local policy. It
is valid for localhost/127.0.0.1 for one year and retained across restarts. Never
distribute its private key. Production needs an organization-trusted certificate.

## Sign in

```sh
manage.bat token
# Linux/macOS: sh manage.sh token
```

Paste the printed JWT into the UI's **Access token** field. It lasts ten minutes,
uses RS256 and grants `toolgate-admin` within `tenant-local`. Mint again on expiry.
Its terminal output is secret. Control receives only the public verification key;
the signing key remains in helper custody. This local issuer is not an organization
OIDC/password service. Directory CRUD and anonymous client downloads are available;
the pinned release contains archives, and installers need a newer release.

Prefer `manage.bat token` / `sh manage.sh token`: these apply your saved overrides.
Bare `docker compose` commands below refer to bundled mode and need
`--profile database --profile proxy`; external DB mode additionally needs
`-f compose.yaml -f external-db.yaml`, and external proxy mode needs
`-f external-proxy.yaml`. The management scripts handle these arguments for you.

## Gateway policy and runtime API

The initial static policy has **no ALLOW rules**. Authenticated protected calls
return BLOCK. Get the private runtime token only when explicitly testing:

```sh
docker compose --profile database --profile proxy run --rm --no-deps --entrypoint /bin/cat token /custody/runtime
```

Use it as the Bearer token for `POST https://localhost:8444/v1/authorize` with:

```json
{"toolId":"files.read","action":"read","arguments":{"path":"workspace/readme.txt"}}
```

Missing/wrong token returns 401. Unknown tool/resource fails closed. Runtime token
validity is 30 days; rerunning startup refreshes it. Initial policy also expires in
30 days and is deliberately not silently renewed. Configure or renew explicitly:

```sh
docker compose --profile database --profile proxy cp gateway:/config/gateway.json ./env/gateway.json
# Edit policy rules and expiry according to the Gateway configuration contract.
manage.bat policy
# Linux/macOS: sh manage.sh policy
```

Invalid/expired configuration rejects startup. Keep a verified copy for rollback.
Gateway trusts this stack's TLS proxy and has no direct host port. The proxy routes
only `/v1/authorize` and `/mcp` to Gateway, and blocks management routes.

Control edits do not automatically update this static Gateway source. Signed bundle
distribution, ASK, device mTLS enrollment, fleet artifact storage and author sandbox
need their explicit authorities/configuration and are not enabled here. For a
seeded built-in tool/ASK demo use [QuickStart](../QuickStart/README.md). See the
[Gateway guide](../../../apps/gateway/README.md) for runtime configuration.

## Images and configuration

Bundled Y mode also deploys authenticated Redis 7.2 with a 128 MiB cache limit,
LRU eviction, no persistence and no host port. Its random password is generated in
custody and exported to ignored `env/.env.cache`; update retains it. Startup waits
for Redis readiness as well as Control/Gateway. Redis is provisioned as an available
cache backend; current Control/Gateway authorization and approval state remain in
PostgreSQL and do not depend on cached decisions.

External n mode asks for an existing Redis URL/password after the database settings.
Use `rediss://` for trusted TLS, or `redis://` only on a trusted local development
network. The saved raw `env/.env.cache` is supplied to Control and the startup probe.
The updated helper image with the Redis client is required. A cache PING failure
fails the startup probe; external Redis is never created or removed by this stack.
Admin JWT login stays required in GatewayControl; the password-disabled option is
specific to the local Quickstart composition.

Defaults use published `dev` images, which resolved to `dev-843aca0220f3` during
review/smoke on 2026-10-04. The generated ignored `.env` configures them together:

```dotenv
CONTROL_IMAGE=ololab/olo-toolgate-control:dev
GATEWAY_IMAGE=ololab/olo-toolgate-gateway:dev
HELPER_IMAGE=ololab/olo-toolgate-quickstart:dev
STACK_TLS_PORT=8444
CONTROL_HTTP_PORT=9082
GATEWAY_HTTP_PORT=9081
```

Use compatible commit tags/digests for repeatability; `dev` is mutable. PostgreSQL
is digest-pinned. Nginx provides TLS. Bootstrap runs as root only in its disposable
helper; Control/Gateway run as UID 65532. The proxy master reads its private key and
runs request workers under its image user.

Volumes hold DB (`postgres-data`), identity/source credentials (`custody`), Control
public key/DB credentials (`control-config`), policy/digested runtime credentials
(`gateway-config`), SQL bootstrap (`db-init`) and HTTPS keys (`proxy-tls`). Back up
matching database and custody/config securely. Flyway uses the migration role;
the runtime role cannot mutate audit or immutable bundle records.

## Debug and stop

```bat
manage.bat deploy
manage.bat update
manage.bat status
manage.bat logs
manage.bat undeploy
manage.bat configure
manage.bat token
manage.bat policy
```

Linux/macOS use `sh manage.sh` with the same commands. Update pulls configured tags
and recreates changed services, retaining DB/identity and reusing configuration.
Undeploy stops/removes only this Compose project's active services and networks,
retaining volumes. Neither command modifies your external DB/proxy configuration.

The bundled admin console is published at `https://localhost:8444/console/`.
Change `STACK_TLS_PORT` in `.env` to avoid conflicts. QuickStart's separate console
is `http://localhost:8080/console/`; both stacks can run simultaneously. Bindings
are deliberately loopback-only. Remote access requires trusted HTTPS and explicit
proxy/network configuration; QuickStart enforces local Host/Origin checks.

```sh
docker compose ps
docker compose logs --tail 100 postgres control gateway proxy
docker compose run --rm probe
docker compose restart control gateway proxy
docker compose down
```

`down` retains state; `--volumes` destroys local identity and DB. Scripts stop on
failure. `pg_isready` gates Control startup; the startup probe checks actual readiness
of both services. Proxy health tracks Control. Check memory/disk, database role
permissions and policy expiry if startup fails. 401 usually indicates wrong/expired
token; 403 indicates missing role; BLOCK is expected until an exact valid rule allows.
Do not print password files in shared diagnostics.

Use `pg_dump` through `docker compose exec -T postgres` for DB backup and separately
protect matching custody/config with encryption and restricted access. Test restores
in an isolated replacement stack. Back up before updating images; do not downgrade
migrated state in place. There is no automatic destructive reset.

## Production boundary

This topology enables `TOOLGATE_CONTROL_DEVELOPMENT_MODE=true` to permit non-TLS
PostgreSQL transport on its isolated Docker network. Production requires `verify-full`
DB TLS, external identity, trusted HTTPS, externally managed signing/device/approval
keys, network separation, backups, telemetry and compatible image digests/scaling.
See [production deployment](../../../docs/deployment/production.md). Redis is included
as the configured cache backend; Kafka is not required. Do not expose the local issuer or ports publicly.
