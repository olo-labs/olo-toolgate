# ToolGate QuickStart

One published container contains Gateway, Control, Admin UI, SQLite, an encrypted
vault and fixed built-in tools. This is a persistent, single-node non-HA evaluation.
No host Java, Node, Python or PostgreSQL installation is required.

## Start

Install Docker Desktop with Linux containers, or Docker Engine with Compose v2.
Allow about 2 GB free RAM and internet access for the first image pull. The reviewed
images support Linux AMD64; ARM hosts need Docker AMD64 emulation.

From this folder on Windows:

```powershell
.\manage.bat deploy
```

Linux/macOS:

```sh
sh manage.sh deploy
```

The script copies `.env.example` to `.env` when absent, pulls the image and waits
for readiness. `start.ps1` and `start.sh` remain deploy aliases.
Open **http://localhost:8080/console/**. Obtain the generated first-boot password:

```sh
docker compose exec quickstart cat /data/bootstrap-password
```

Treat the output as a secret. Change it at first login; the bootstrap file is then
removed. Restarting does not reset identity. Use the console for built-ins, ASK
approvals and public client downloads. Enrollment uses **https://localhost:8443**;
follow the [client trust/install guide](../../../docs/client/installers.md).

## Configure

The cache/database/password options below require an image built from this change
or a successful subsequent development release. Scripts verify reported options
and reject older images that silently ignore them. For source builds see
[Quickstart operations](../../../docs/deployment/quickstart.md).

Defaults in `.env`:

```dotenv
TOOLGATE_DISABLE_ADMIN_PASSWORD=false
TOOLGATE_QUICKSTART_DATABASE_MODE=sqlite
TOOLGATE_CACHE_MODE=embedded
```

The embedded cache is a bounded in-process, 60-second cache of immutable tool
catalog metadata. It is empty after restart. Policies, credentials, approvals,
permits and authorization decisions are never cached there.

To use external Redis, set `TOOLGATE_CACHE_MODE=redis`, copy
`env/.env.cache.example` to `env/.env.cache`, and configure URL/password. Use `rediss://`
for TLS with a trusted server certificate. Cache connection failure prevents initial
startup; a later cache outage falls back to the same immutable local catalog, never
an ALLOW decision. Redis metadata is checked against the image's canonical catalog.

To use external PostgreSQL, set `TOOLGATE_QUICKSTART_DATABASE_MODE=postgresql`, copy
`env/.env.db.example` to `env/.env.db`, supply separate runtime/migration credentials,
and place the DB CA at `env/db-ca.crt` for `verify-full`. Existing roles/database must
be provisioned as described in [GatewayControl](../GatewayControl/README.md).
Control migrates its PostgreSQL schema; identity audit and encrypted vault records
use that same external database. Local keys/identity/HotFolder still live in `/data`.
Protect `.env.db`/`.env.cache` with host ACLs; they are raw environment files, not
shell scripts. On POSIX use directory mode 0711, secrets 0600 and public CA 0644.

Use a **separate empty data volume/project** when changing database backend. Startup
rejects implicit switching of an existing SQLite/PG layout; this is not an automatic
data migration. External PG requires `pg_dump` plus matching local custody backup;
the built-in SQLite backup command deliberately rejects external PG mode. Neither
cache nor PG changes make Quickstart HA.

Set `TOOLGATE_DISABLE_ADMIN_PASSWORD=true` to skip the password form and enter the
console automatically. Default is false. The browser receives a short-lived internal
admin JWT and renews it; runtime authorization and ASK still apply. Keep this mode
on loopback for a local demo: anyone able to reach its local console can administer
it. Re-enable the variable and redeploy to restore the login form. Existing password
identity is retained, not erased. Use the dedicated
[QuickStart-WO-Password](../QuickStart-WO-Password/README.md) scripts for an isolated
password-free demo with its own ports and data volume.

The default image is `ololab/olo-toolgate-quickstart:dev`. On 2026-10-04 Docker Hub
resolved it to `dev-843aca0220f3`, used for these smoke checks. That build predates
the interactive-installer commit: archives are
available, while installers require a newer successful publication. There is no
assumption that a `latest` tag exists. The generated ignored `.env` contains:

```dotenv
QUICKSTART_IMAGE=ololab/olo-toolgate-quickstart:dev
QUICKSTART_HTTP_PORT=8080
QUICKSTART_TLS_PORT=8443
```

Use a reviewed commit tag or digest for repeatability; `dev` is mutable. Restart
with the script to pull updates. The named `data` volume contains database, keys,
identity and HotFolder. UID 65532 runs with a read-only root and bounded CPU/memory.
Management ports stay internal; published ports bind to loopback only. Never mount
your home directory or Docker socket. For port conflicts, change host ports in
`.env` and use the corresponding browser URL; container ports stay fixed.

## Debug and stop

All operations are available through the same entry point:

```bat
manage.bat deploy
manage.bat update
manage.bat status
manage.bat logs
manage.bat undeploy
```

Linux/macOS use `sh manage.sh` with the same operation names. `update` pulls the
configured image and recreates changed services while retaining data. `undeploy`
stops/removes this project's containers and network, keeping volumes. It does not
stop unrelated containers or update your saved `.env` settings.

```sh
docker compose ps
docker compose logs --tail 100 quickstart
docker compose restart quickstart
docker compose down
```

`down` preserves data. Adding `--volumes` intentionally destroys database and keys.
Readiness also appears at `/api/quickstart/v1/status`. Check Docker memory/disk,
volume ownership, exclusive locks and layout compatibility on startup failure.
Do not delete state to clear an error. A second instance cannot share live data.

## Backup and upgrade

Stop before an offline backup. Use the built-in `--backup`/`--restore` procedure in
the [operations guide](../../../docs/deployment/quickstart.md) with this image and
volume (normally `toolgate-quickstart_data`). Protect matching database and keys
together. Restore into an empty replacement volume; do not copy a live SQLite WAL
or downgrade migrated state. Back up before changing image, then verify readiness
and a protected tool call. Restore the pre-upgrade backup for rollback.

See [GatewayControl](../GatewayControl/README.md) for separate services. Production
needs external identity, trusted TLS, custody and HA planning.
