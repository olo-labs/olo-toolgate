# Admin UI

React 19 / TypeScript / Vite console, embedded in Control at `/console/`.
Dashboard and paginated users, teams, tools, policies, clients/device records and
agents use real Control v1 APIs. User create/edit/delete supports optimistic
revisions and exact-request idempotency. Other sections provide readable inspection.

## Develop and verify

Requires Node 22.12+ (Node 22 CI), Java 21, Python tooling and Docker for isolated
PostgreSQL. From the repository root:

```bash
npm ci --ignore-scripts
npm run ui:build
npm --workspace @olo-labs/toolgate-admin-ui test
npx --no-install playwright install --with-deps chromium
python tools/ui/check.py
```

The browser gate builds Control, generates isolated signed test identities,
starts real PostgreSQL, exercises embedded production assets and removes its owned
infrastructure. No company credentials are needed. For a local interactive
workspace run `python tools/ui/check.py --serve`, open the printed loopback URL
and read the temporary credential file's `admin` or `reader` field. Tokens expire
after five minutes; restart for fresh tokens. Ctrl+C removes owned resources.
This developer harness is not Quickstart.

`npm --workspace @olo-labs/toolgate-admin-ui run dev` serves the development
console at `/console/` and proxies only the versioned API to loopback port 8082.
It requires an independently running Control; it supplies no fake API.

## Contracts and packaging

Wire types come from `@olo-labs/toolgate-contracts`. Generate canonical OpenAPI
operation bindings with `python tools/ui/generate.py`; `--check` detects drift.
The transport wraps generated routes/shared types and centralizes cancellation,
timeouts and safe errors. No claim decoder, policy evaluator or server validation
is copied into the UI.

`./gradlew :control-plane:build` explicitly builds npm assets and embeds them under
`META-INF/resources/console`. Docker uses a pinned Node stage, verifies prebuilt
product/contracts versions and packages the JRE-only runtime.
`-PprebuiltAdminUi=true` is for this controlled pipeline and fails when matching
assets are missing. `node tools/ui/build.mjs` also creates release metadata and
runtime notices. `python tools/ui/package.py` produces a reproducible asset tar
and checksums in `build/ui/`. `make check` includes unit, drift and browser gates.

See [authentication and operations](../../docs/control-plane/admin-ui.md),
[ADR 004](../../docs/adr/004-embedded-admin-console.md) and
[coverage plan](../../docs/codex/modules/03-coverage.md).
