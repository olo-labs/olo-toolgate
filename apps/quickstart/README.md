# Quickstart composition

One non-HA Linux image contains the real Control JVM, Rust Gateway, fixed Rust
built-in executor and embedded console. SQLite and private generated keys live
under `/data`; no PostgreSQL, external identity provider or Docker socket is
required for its primary path.

Start with the [user guide](../../docs/getting-started/one-minute-quickstart.md).
Build, configure, debug, upgrade and recover using the
[operations guide](../../docs/deployment/quickstart.md). The accepted boundary
and storage exception are in [ADR 012](../../docs/adr/012-single-node-quickstart.md).

`python tools/quickstart/check.py --build` exercises the real production image,
including the browser. The SQLite store has JVM integration tests; Linux image
identity and backup tests live in `tests/quickstart`. Publication uses the
protected `quickstart-release` CI environment and the exact tested image.
