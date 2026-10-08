# Gateway

Managed tool execution uses a trusted registered device, current owner/device enablement, installed-client connection approval, and the existing agent/tool/action/resource filters before every protected effect. Registration, green availability or deployment never grants execution.

See [Device registry and tool-call controls](../../docs/control-plane/device-registry.md).

**Status:** Authorization decisions, signed bundles, approvals and optional client MCP relay
**Primary stack:** Rust

## Responsibility

Runtime authorization, MCP routing, resource extraction, execution permits, runtime audit.

## Run and verify

```sh
python tools/gateway/local_credentials.py
export TOOLGATE_GATEWAY_CONFIG="$PWD/docs/examples/gateway-static.json"
export TOOLGATE_GATEWAY_CREDENTIALS="$PWD/.dev/gateway/credentials.json"
cargo run -p olo-toolgate-gateway --locked
```

Runtime binds 127.0.0.1:8081; management binds 127.0.0.1:9091. Generated tokens
expire after one hour and stay under ignored .dev. The example permits only the
exact demo files.read/read request. No database, tool execution or signed permits
exist in this module. Run `cargo test -p olo-toolgate-gateway --locked`, `make check`,
`make benchmark` and `make containers`.

Read [configuration](../../docs/gateway/configuration.md),
[API](../../docs/api/gateway-api.md), [deployment](../../docs/gateway/deployment.md)
and [ADR 002](../../docs/adr/002-gateway-static-foundation.md).

## Architecture

Read:

- `../../ARCHITECTURE.md`
- `../../docs/architecture/component-map.md`
- the matching section under `../../docs/`
- `../../docs/security/security-invariants.md`

Do not move responsibility across component boundaries without an ADR.

Module 04 adds signed bundle verification, background distribution and atomic
in-memory policy replacement. Choose `bundleSource` instead of static `policy`;
invalid/outdated bundles retain only a still-valid verified snapshot. See
[bundle trust, grace and operations](../../docs/control-plane/policy-bundles.md).

Module 05 adds opt-in ASK coordination, `/v2/authorize`, separately signed ten-second
maximum exact-bound permits and `/v1/permits/consume`. Approval outage or changed
policy blocks. Legacy v1/MCP block ASK. [Approval protocol/configuration](../../docs/control-plane/approvals.md).

The optional [client MCP relay](../../docs/client/server-mcp.md) exposes applicable
installed tools and queues ALLOW calls for the next enrolled client poll. Control
stores the permission cache and request progress; clients perform the protected effect.
