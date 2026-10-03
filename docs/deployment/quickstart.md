# Quickstart deployment, production/debug execution and recovery

[Walkthrough](../getting-started/one-minute-quickstart.md) ·
[ADR 012](../adr/012-single-node-quickstart.md) ·
[canonical API](../../packages/contracts/openapi/quickstart-v1.yaml).
Quickstart is a local, single-node/non-HA evaluation package. Production uses the
separate Gateway/Control images, external PostgreSQL, identity and secret custody.
Do not mount host home directories, Docker sockets or custom runtime engines.

## Build and run

First obtain verified three-platform client archives using `tools/client/package.py`
and `tools/client/manifest.py`, or the CI bundle. Empty placeholders are rejected.
Java/Gradle, Node, Docker and pinned `tools/requirements.txt` validation tools are
needed for source development.

```bash
python tools/quickstart/check.py --build
docker run -d --name olo-toolgate --restart unless-stopped \
  --read-only --cap-drop ALL --security-opt no-new-privileges \
  --memory 1g --cpus 2 \
  --tmpfs /tmp:rw,noexec,nosuid,size=128m,uid=65532,gid=65532 \
  -p 127.0.0.1:8080:8080 -p 127.0.0.1:8443:8443 \
  -v olo-toolgate-data:/data olo-toolgate-quickstart:module11
```

The helper builds Control with the `quickstart` Quarkus build profile, composes the
single image, and executes real Docker/browser smoke. Production Control retains
`prod` and PostgreSQL. SQLite JNI loads from immutable image storage; /tmp may stay
noexec. The service runs as 65532:65532; /data must be owned by that UID, mode 0700.
A volume lock rejects a second instance. The healthcheck observes composed readiness;
child failure stops the composition. SIGTERM drains all children. Administrative
machine JWTs renew every ten minutes; signed snapshots renew hourly. Private runtime
credentials renew before their 24-hour expiry, briefly restarting Gateway/tools.
Expiration and approval outages stay fail closed. No new Helm workload is introduced.

## Configuration and local state

| Setting/location | Behavior |
|---|---|
| `TOOLGATE_BOOTSTRAP_PASSWORD` | Optional first-boot strong password. Prefer the private generated file over a Docker environment credential. Never resets existing identity. First login still requires change. |
| `/data/layout.json` | Layout version 1; unknown newer layouts reject startup. |
| `/data/state/control.sqlite` | WAL/FULL, 3-second busy timeout, IMMEDIATE transactions, checksummed ordered migrations and append-only/immutable triggers. |
| `/data/identity.json` | Private PBKDF2-HMAC-SHA256 verifier: 600,000 iterations, random 32-byte salt, durable password generation. |
| `/data/keys` | Separate identity/policy/permit/device/TLS RSA keys and independent AES-256 vault key. Private files mode 0600. Back up together. |
| `/data/hotfolder` | Fixed tools: 64 KiB/file, 256 entries, txt/md/json/csv. Demo mutation scope is exact welcome.txt. |
| `/data/run` | Private generated process settings/credentials; rebuilt on boot and excluded from backups. |
| `/api/quickstart/v1/status` | Public mode/non-HA/version/readiness only. |
| `/api/quickstart/v1/login` | Ten global attempts/minute; first-login change; signed 15-minute memory-only session. |
| `/api/quickstart/v1/tools`, `invoke`, `vault` | Admin session; canonical fixed tool input; fresh Gateway/ASK; encrypted name-only vault. |
| `:8443` | Direct TLS enrollment/browser review and mTLS check-in; certificate for localhost/127.0.0.1. |

Keep public HTTP loopback-only. Exact local Host and same Origin are enforced.
Do not override generated identity/internal port settings. The device CA certificate
is public, its key is private. The public truststore password `changeit` is not
credential custody. Renew expired server certificates with retained CA/key; never
silently replace the device CA. Advanced fleet/builder runtime configuration still
requires the existing configured client sandbox/artifact/release authority boundary;
the contained demo does not create that infrastructure or enable arbitrary code.

Vault writes use AES-256-GCM, a fresh 96-bit nonce and name-associated data, committing
ciphertext plus audit atomically. No plaintext export/read API exists. Full volume
access includes custody; use encrypted host storage and protected backups. An
unbound credential never enables web search or author code execution.

Password changes append an audit intent before atomically replacing the private
verifier and a completion event afterward. An interrupted change retains its intent
and durable generation; file custody and the audit database are separate commit
boundaries. No password or verifier bytes enter either audit event.

## Offline backup and restore

Stop the service. Provision a service-owned backup volume; the snapshot must be a
new absolute path outside /data. This root command only provisions the backup volume.

```bash
docker stop -t 35 olo-toolgate
docker run --rm --user 0 --entrypoint sh -v olo-toolgate-backup:/backup \
  olo-toolgate-quickstart:module11 -c 'chown 65532:65532 /backup && chmod 700 /backup'
docker run --rm -v olo-toolgate-data:/data -v olo-toolgate-backup:/backup \
  olo-toolgate-quickstart:module11 --backup /backup/snapshot-1
docker start olo-toolgate
docker run --rm -v olo-toolgate-restored:/data -v olo-toolgate-backup:/backup:ro \
  olo-toolgate-quickstart:module11 --restore /backup/snapshot-1
```

Launch a replacement using the restored volume. Backup holds the exclusive data
lock, uses SQLite's backup API and includes all private custody and HotFolder.
Restore requires an empty volume; every checksummed file is verified, extra files
and symlinks are rejected. Checksums detect corruption, not an external signature.
Protect backup ownership. Do not recover using a live copy of WAL files.

## Upgrade and rollback

Back up, stop, and replace with a pinned compatible newer image using the same
volume. Migration text normalizes CRLF to LF before hashing. Migrations are transactional/checksummed; passwords and existing defaults
are retained. Schema 1→2, restart and backup restore are tested. This is the first
Quickstart package; no previous released Quickstart tag is claimed compatible.
Newer unknown schema/layout and checksum drift reject startup. Restore a pre-upgrade
backup to a new volume before image rollback; never downgrade newer state in place.
Verify readiness and a protected call after upgrade/restore.

## Debug and operations

```bash
docker inspect olo-toolgate --format '{{json .State.Health}}'
docker logs --tail 100 olo-toolgate
curl --fail http://localhost:8080/api/quickstart/v1/status
```

Service logs include safe event/error/request IDs, never passwords/vault values or
raw tool arguments. Existing trace propagation and Gateway/Control metrics remain;
management ports 9091/9092 are internal. Inspect them through docker exec and a local
Python HTTP request; never publish them. Debug API/schema/negative-path tests use
`python tools/quickstart/check.py --build`; evidence is in build/quickstart/smoke.json.

| Symptom | Action |
|---|---|
| Startup exits | Inspect ownership, exclusive lock, disk capacity, migration/layout compatibility and key integrity. Preserve failed state; restore a verified backup instead of deleting DB. |
| First login 400 | Supply a different strong replacement password. The bootstrap file is valid only before successful change. |
| Login 401/429 | Current password and rate window; startup variables do not reset identity. |
| Tool ASK | Approve exact operation in Approvals, then retry identical arguments. |
| Tool BLOCK/403 | Check exact path/current policy; defaults protect welcome.txt only. Unknown paths/deletion stay blocked. |
| Tool 503 | Check internal Gateway/Control readiness; never bypass authorization. |
| Enrollment TLS failure | Trust exported public CA; use localhost/127.0.0.1 with published 8443; check certificate expiry/fingerprint. |
| Vault recovery failure | Preserve the matching key and database together; check capacity/private ownership. |

Run the dedicated image security suite with:

```bash
docker run --rm --entrypoint /opt/quickstart-python/bin/python -v "$PWD:/work:ro" \
  olo-toolgate-quickstart:module11 /work/tests/quickstart/test_security.py
```

Remote image publication uses protected CI with SBOM/scan/provenance gates. Local
build/smoke never invents credentials or publishes an unverified remote image.

Before enabling publication, repository administrators must configure required
reviewers/tag restrictions on the `quickstart-release` GitHub environment and
OCI/attestation permissions. Declaring an environment in YAML alone does not
configure those repository protection rules. Local runs never require remote
registry credentials.
