# Gateway API

## Purpose

The canonical [OpenAPI](../../packages/contracts/openapi/gateway-v1.yaml) documents
the v1 decision surface. Runtime bearer credentials supply identity; caller bodies
and forwarded principal headers cannot. Runtime responses carry server-generated
X-Request-Id, validated/generated W3C traceparent and Cache-Control: no-store.

`POST /v1/authorize` accepts:

```json
{"toolId":"files.read","action":"read","arguments":{"path":"workspace/readme.txt"}}
```

HTTP 200 carries canonical PolicyDecision: ALLOW/BLOCK, reason, policyVersion and
requestId. BLOCK is an evaluation result. Errors use ErrorEnvelope: VALIDATION
400/413/415/431, UNAUTHORIZED 401, FORBIDDEN 403, DEPENDENCY_UNAVAILABLE 429/503,
TIMEOUT 504, UNSUPPORTED 501 and NOT_FOUND 404. Error bodies expose no exception detail.
`POST /v1/permits` is UNSUPPORTED: no execution authority can be minted.

Separate management serves /v1/health/live, /v1/health/ready and /v1/metrics. These
routes are absent from runtime ingress. Keep management network-restricted.

`POST /mcp` is a restricted [2026-07-28 transport](https://modelcontextprotocol.io/specification/2026-07-28/basic/transports/streamable-http).
One JSON-RPC request with integer/string ID and per-request metadata is accepted.
Ping and server/discover work. With the optional `localMcp` relay enabled, tools/list
returns device-scoped installed definitions applicable to this agent. ALLOW tools/call
requests are queued in Control for the next client poll and return a bounded result.
Managed tools use their fixed CUSTOM `runtime/<tool-id>` resource, and builtins use
registered extraction. ASK blocks. Without relay mode, discovery is empty and execution
is unsupported. Unknown methods, legacy initialization, notifications
and streaming subscriptions reject; GET/DELETE return 405 and no session is minted.
Mirrored version/method/name headers are checked, including encoded names. Early
transport/auth/limit failures use HTTP errors; parsed RPC failures use sanitized
JSON-RPC errors. See [client relay configuration and progress](../client/server-mcp.md)
for the device-certificate authorization, permission replacement and retry protocol.
Parse/invalid-frame errors include `id: null`; unsupported notifications receive
an ordinary HTTP error without a JSON-RPC response, consistent with
[JSON-RPC 2.0](https://www.jsonrpc.org/specification).
