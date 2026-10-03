# One-Minute Quickstart

The real Gateway, SQLite-backed Control, Admin UI and Rust fixed tools run in one
non-root container. **Single-node, non-HA; local evaluation only.** No external
PostgreSQL, Redis, Vault or Kubernetes is needed. Custom author code still runs
only on separately configured designated clients.

The GHCR image is published by protected tagged CI. Until a release is published,
[build locally](../deployment/quickstart.md) and substitute
`olo-toolgate-quickstart:module11` below. Docker 24+ and approximately 1 GiB available
memory are required. Image pull time is additional to first-boot key generation.

```bash
docker run -d --name olo-toolgate --restart unless-stopped \
  -p 127.0.0.1:8080:8080 -p 127.0.0.1:8443:8443 \
  -v olo-toolgate-data:/data \
  ghcr.io/olo-labs/olo-toolgate-quickstart:<released-version>
```

Open [http://localhost:8080](http://localhost:8080). Keep HTTP bound to host loopback.
Retrieve the generated bootstrap password from its private file:

```bash
docker exec olo-toolgate cat /data/bootstrap-password
```

Enter it in **Password**, and a different 16–128-character password with eight
or more distinct printable ASCII characters in **New password**. First login
requires the change and deletes the bootstrap file. Subsequent logins need only
Password. Credentials never appear in service logs; the signed 15-minute browser
session stays in memory. Disconnect clears it.

1. Open **Built-in tools and vault**. Run the calculator; the default result is 14.
2. Select `hotfolder.read_text` and enter `{"path":"welcome.txt"}`.
3. Select `hotfolder.write_text`, enter
   `{"path":"welcome.txt","text":"My approved note"}`, and run. ASK changes no
   file. Open **Approvals**, approve once, then rerun the exact call.
4. Store a named vault credential. Only names are listed; no plaintext read/export
   API exists. Custom credential binding remains unavailable and fails closed.
5. Download a Windows, macOS or Linux client from the anonymous home page.

Defaults grant compute and exact demo reads. File writes require ASK; unselected
paths, unknown tools and deletion remain blocked. `web.search` stays disabled
without a configured provider. Gateway failure prevents protected execution.
The immediately useful HotFolder belongs to this container, not a host directory.

## Client enrollment

The independent device CA is public and can be exported:

```bash
docker cp olo-toolgate:/data/keys/device-ca.crt ./toolgate-quickstart-ca.crt
```

Trust it on the evaluation machine; never disable TLS checks. Follow the
[client guide](../client/hotfolder.md) with `https://localhost:8443` and this CA.
Browser review opens the password-enabled TLS console. Review the fingerprint,
approve, and the client receives its bound mTLS identity for check-in. Client
execution separately needs its Gateway credential/policy and system engine
configuration. Native logout/boot/ARM certification remains open in client reports.

## Persistence

Retain `/data` across replacement containers: directory, audit, approvals, device
identity, encrypted vault, private custody and HotFolder persist. One instance
owns the volume. Container removal preserves a named volume; volume removal loses
state. See [build/configuration, upgrades, backup/restore and debugging](../deployment/quickstart.md).
Production uses separate stateless services and external state/custody.
