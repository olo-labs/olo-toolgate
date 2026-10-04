# Docker Compose

## Purpose

Reference development/small production-style composition. Do not pretend embedded Quickstart is HA.

Published-image setups:

- [QuickStart](../../deploy/compose/QuickStart/README.md): single container and persistent SQLite/vault.
- [GatewayControl](../../deploy/compose/GatewayControl/README.md): separate services, PostgreSQL and HTTPS proxy, with optional existing DB/proxy configuration.

In either folder use `manage.bat deploy` on Windows or `sh manage.sh deploy` on
Linux/macOS. The same script accepts `update`, `undeploy`, `status` and `logs`.
GatewayControl defaults to bundled dependencies and saves external choices when
selected. `.env.example` supplies defaults; secret `.env`/`env/.env.db` remain ignored.
Both consoles publish separate loopback ports; these are local/evaluation setups,
not a replacement for production TLS, identity, custody or HA configuration.
