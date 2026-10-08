<!-- Copyright 2026 OLO Labs; SPDX-License-Identifier: Apache-2.0 -->
# ADR 012: client MCP requests delivered by polling

Status: accepted for the combined Windows installer and client relay flow.

## Context

The console must enroll installed clients and change their gateway without asking
for a URL during installation. Agents also need applicable local MCP definitions
and remote access to those tools through the Gateway. Clients may be behind NAT
and must receive permission changes while executing jobs.

## Decision

Ship one Windows setup EXE with the client service, Chrome extension, native bridge
and user-session tray. Configuration comes from the site's server-owned public
Control address, with no setup credentials. Chrome extension approval remains a
browser requirement. The native host accepts setup and health commands only and
uses the packaged protected CLI with Windows elevation for configuration changes.

Use the existing authenticated client check-in channel on a 500 ms healthy
cycle. Control stores a per-device effective permission snapshot, revision, digest,
acknowledgment and installed tool catalog. A changed set is sent as a full
replacement until acknowledged. Discovery narrows this set to the authenticated
agent and actual locally ready tools; cached permission data grants no execution.

Control stores a bounded tenant-scoped request queue in PostgreSQL or Quickstart
SQLite. The optional Gateway relay uses a dedicated short-lived service JWT and
fixed Control origin, and never forwards an agent bearer credential. Runtime
credentials bind the user, agent and enrolled device. Gateway policy and audit must
ALLOW the request before queueing. Managed local tools use the fixed CUSTOM
`runtime/<tool-id>` locator after device-scoped discovery and strict extraction.

The next poll delivers an exact request and a stable client lease. Before any
protected effect, the client validates the current grant through direct device mTLS.
The response is bound to that request and lease; Control checks current permissions
again before returning it to the agent. Requests expire within thirty seconds and
before the runtime credential expires. MCP ASK remains blocked and the existing
human approval/permit protocol stays authoritative.

Remote execution and builder jobs run asynchronously so check-ins continue.
Protected client journaling records an invocation before starting its effect and
its result before posting it. An interrupted invocation is not repeated: uncertainty
is reported as failure. Gateway switches clear old permission, enrollment, remote
lease and fleet state before restarting, retaining the device key.

The dashboard exposes received, waiting for poll, submitted, response received and
completed stages, including failures and expiry. Arguments, outputs and credentials
are excluded from the administrative progress table.

## Consequences

No inbound client listener or new queue infrastructure is required. Database
migrations add permission caching and request progress. Queue capacity, result size,
timeouts and retained history are bounded. Each client serves one remote request at
a time; retries preserve the lease and cannot repeat uncertain effects. Network
failure and runtime provisioning can delay polls, so delayed jobs may expire.

Automatic Chrome deployment requires a published Web Store identity and supported
browser approval or enterprise management. Fully silent arbitrary extension
installation is not assumed. Native installation/repair/uninstall smoke runs in
the existing Windows CI matrix; cross-compilation alone does not prove those flows.

See [client relay setup and protocol](../client/server-mcp.md).
