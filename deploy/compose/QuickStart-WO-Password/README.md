# Password-free local Quickstart

This variant has its own `toolgate-quickstart-wo-password` project and data volume.
Its `.env.example` defaults `TOOLGATE_DISABLE_ADMIN_PASSWORD=true`, embedded cache
and SQLite. It requires the updated image implementing these options; startup
rejects an older image that silently ignores the password flag.

Windows:

```bat
manage.bat deploy
```

Linux/macOS:

```sh
sh manage.sh deploy
```

Open **http://localhost:8089/console/**; the console enters automatically without a
user/password form. TLS enrollment is **https://localhost:8449**. These ports can be
changed in the default `.env` copied at first deploy. Use the same scripts with
`update`, `undeploy`, `status` and `logs`. Undeploy retains data.

This is an explicit local demo mode. Anyone able to reach the console can administer
it, so ports bind only to loopback. Signed runtime identity, Gateway policy and ASK
approval remain enforced. The generated password identity is retained internally;
setting `TOOLGATE_DISABLE_ADMIN_PASSWORD=false` and redeploying restores normal login.

External Redis and PostgreSQL options work as in
[QuickStart](../QuickStart/README.md): copy its DB/cache example files into this
folder's `env/`, configure `.env` modes, and use a fresh data volume for a backend
change. The cache is non-authoritative and external PostgreSQL is still non-HA.

## Local image overrides

Published images such as `ololab/olo-toolgate-quickstart:dev` are pulled on deploy
and update. A local-only override such as `olo-toolgate-quickstart:cache-options`
is used from Docker's local image store, without a registry pull. If it is missing,
the script stops with instructions to build it or change `QUICKSTART_IMAGE` in
`.env` to the published image. Local-only updates require rebuilding the image.

The generated `.env` is retained across script runs; updating `.env.example` does
not replace existing overrides. `-SkipPull` (Windows) or `--no-pull` (shell) also
permits explicit use of an already downloaded image. Password-free mode requires
a build supporting `TOOLGATE_DISABLE_ADMIN_PASSWORD`; older published builds are
rejected by the startup option check.
