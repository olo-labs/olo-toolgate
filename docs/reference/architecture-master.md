# MCP Team Access Control Platform
## Monorepo Architecture, Security, Endpoint Enforcement, Admin UX, API-to-MCP Builder, Deployment and Implementation Specification

**Status:** Architecture / build specification  
**Target audience:** AI coding agents, backend/frontend engineers, security engineers, platform engineers, DevOps/SRE, open-source contributors  
**Document version:** 0.1  
**Date:** 2026-09-17  
**Working project name:** `mcp-access` (placeholder; rename before public launch)

---

# 1. Executive Summary

This project is a **self-hosted MCP access-control platform for teams**.

It is not intended to be merely another MCP proxy. Its primary purpose is to answer, enforce, and audit:

> **Who, using which AI agent, may invoke which tool, against which resource, with which arguments, under which conditions?**

The production/enterprise system consists of **two independently scalable stateless application containers** plus a cross-platform endpoint client. The project also ships a third, all-in-one **Quickstart container** for single-user/non-HA use.

Every release therefore publishes:

```text
mcp-access-quickstart   # single-container community/personal mode
mcp-access-gateway      # production Container 1
mcp-access-control      # production Container 2
mcp-access-client       # native endpoint application via GitHub Releases
```

The production architecture is:

1. **Container 1 — Access Gateway / Data Plane**
   - Very small, very fast, security-focused.
   - Stateless from a deployment perspective.
   - Sits on the runtime path.
   - Authenticates and authorizes requests.
   - Routes MCP traffic and generated API-backed tools.
   - Issues short-lived signed execution permits for endpoint clients.
   - Evaluates policy from an in-memory signed policy snapshot.
   - Must not depend on Container 2 or its database during normal authorization.
   - Horizontally scalable behind an ordinary load balancer.

2. **Container 2 — Management Control Plane + Admin UI**
   - Stateless application container.
   - Provides the administrator-facing web UI and backend.
   - Manages users, teams, identities, agents, devices, MCP servers, tools, resources, policies, approvals, templates, API-to-MCP definitions, audit search, client enrollment, client downloads, configuration import/export, and system settings.
   - Compiles/validates versioned configuration into signed policy/configuration bundles consumed by Container 1.
   - Horizontally scalable; persistent state is externalized.

3. **Endpoint Enforcement Client — Optional but required for strong local enforcement**
   - Installed on Windows, Linux, or macOS endpoints and/or on privileged jump hosts.
   - Runs as a protected OS service with elevated privileges.
   - Provides a local MCP/tool endpoint for AI clients.
   - Before every controlled tool execution, contacts Container 1 and obtains authorization.
   - Executes an operation only when a short-lived permit is valid.
   - Can hold privileged capabilities that ordinary users and AI processes do not receive directly.
   - Can be used to perform authorized operations against remote systems.
   - Is downloaded and enrolled from Container 2.
   - Supports code signing, device identity, mTLS, revocation, controlled upgrade, and health reporting.

The platform must be:

- **Security-first**
- **Performance-first on the data plane**
- **Simple enough for a small team to deploy and operate**
- **Useful without Kubernetes**
- **Horizontally scalable when Kubernetes or another orchestrator is used**
- **Local/self-hosted by default**
- **Compatible with existing MCP clients and MCP servers**
- **Resource-aware, not merely tool-aware**
- **Human-readable in the UI**
- **Configurable through UI, REST API, JSON, and YAML**
- **Open-source friendly**
- **Quickstart friendly**
- **Community Marketplace friendly** — signed reusable integration packages from a Drupal-based public registry
- **Fail-safe**

The central product idea is:

```text
User + Team + Agent + Device + MCP Server + Tool + Arguments + Resource + Context
                                      |
                                      v
                              Policy Decision
                                      |
                              ALLOW / ASK / BLOCK
```

---

# 2. Product Vision

## 2.1 Problem

An organization may have:

- Claude, Codex, Cursor, VS Code, internal AI agents, or autonomous agents.
- Internal MCP servers.
- Third-party MCP servers.
- APIs that are not MCP servers.
- Local command-line tools.
- Privileged administration tools.
- Databases.
- Git repositories.
- Kubernetes clusters.
- Cloud accounts.
- Internal applications.
- Production resources.

Existing controls often stop at one of these levels:

```text
Can the user access the MCP server?
Can the user call the tool?
```

The real enterprise question is:

```text
Can this specific user,
through this specific AI agent,
from this managed device,
execute this specific action,
with these arguments,
against this specific resource,
at this time,
for this environment?
```

Examples:

```text
Engineering:
  GitHub MCP:
    read repositories:            ALLOW
    create PR in engineering/*:   ALLOW
    merge PR in engineering/*:    ASK
    access finance/*:             BLOCK
    delete repository:            BLOCK
```

```text
Operations:
  Kubernetes:
    get/list pods in dev:         ALLOW
    restart deployment in stage:  ALLOW
    restart production:           ASK
    delete production namespace:  BLOCK
```

```text
Database:
    SELECT development.*:         ALLOW
    SELECT production.analytics:  ALLOW
    UPDATE production.orders:     ASK
    DELETE production.*:          BLOCK
```

The product must make these policies understandable without forcing administrators to learn a policy language.

---

# 3. Core Differentiators

The project should not compete on basic MCP routing alone. Its differentiation must be the combination of:

## 3.1 Resource-Aware Authorization

Authorization evaluates not only the tool name but also the real target:

- GitHub organization/repository/branch.
- Database/database-schema-table-operation.
- Kubernetes cluster/namespace/resource.
- Filesystem path.
- Cloud account/project/resource ARN.
- Jira project.
- Slack workspace/channel.
- SSH host/environment.
- HTTP API tenant/account/resource.
- Custom business identifiers.

## 3.2 User + Agent Identity

A user's rights and an AI agent's rights are separate inputs.

Example:

```text
Rahul directly:
  merge PR: ALLOW

Rahul using interactive Codex:
  merge PR: ASK

Rahul's unattended deployment agent:
  merge PR: BLOCK
```

## 3.3 Just-in-Time Approval

Sensitive operations can result in `ASK` instead of only `ALLOW` or `DENY`.

Approvers may grant:

- Once.
- For this exact request.
- For N minutes.
- For this resource.
- For this user/agent combination.
- For this incident/change ticket.

## 3.4 Strong Endpoint Enforcement

A controlled local or privileged operation must not rely only on a wrapper command.

The architecture supports strong enforcement by:

- Keeping privileged credentials/capabilities outside the normal user's process.
- Running an elevated protected service.
- Requiring a Container 1 permit for execution.
- Optionally enforcing direct process/network/file access through OS security mechanisms.

## 3.5 Extreme Simplicity

Normal administrators see:

```text
WHO      Engineering
CAN USE  GitHub
ACTION   Merge Pull Request
WHERE    engineering/*
DECISION ASK
```

They should not need to see:

- JSON-RPC.
- Rego.
- Cedar syntax.
- OAuth internals.
- MCP transport internals.
- Kubernetes manifests.
- Reverse proxy internals.

## 3.6 No-Code / Low-Code API-to-MCP Builder

An administrator can create a governed MCP tool around an existing API by entering values into the UI, testing the endpoint, selecting request/response mappings, assigning risk/resource rules, and publishing it.

## 3.7 Import / Export Everything

All non-secret configuration must be exportable/importable as:

- JSON.
- YAML.

Import must support:

- Validation.
- Dry-run.
- Diff.
- Conflict handling.
- Version migration.
- Merge or replace.
- Secret-reference validation.

## 3.8 Quickstart Templates

A community user should be able to deploy the project and immediately have working example tools plus prebuilt integration templates.

---

# 4. Explicit Non-Goals for V1

V1 is not intended to:

- Replace the organization's Identity Provider.
- Replace GitHub/Jira/AWS native authorization.
- Build a general-purpose SIEM.
- Build a general-purpose secrets manager.
- Build an LLM or agent runtime.
- Let AI decide authorization.
- Use an LLM in the authorization path.
- Replace Kubernetes network policy.
- Guarantee control over a machine whose local root/Administrator account is hostile.
- Automatically trust arbitrary public MCP servers.
- Execute arbitrary code inside Container 1.

---

# 5. High-Level Architecture

```text
                         +-------------------------+
                         |      Identity Provider  |
                         | Entra / Okta / Google   |
                         | OIDC / OAuth / SAML*    |
                         +------------+------------+
                                      |
                                      v

+--------------+            +-----------------------+
| Claude       |            |                       |
| Codex        |----------->|  CONTAINER 1          |
| Cursor       |    MCP     |  ACCESS GATEWAY       |
| VS Code      |            |  DATA PLANE           |
| Custom Agent |            |                       |
+--------------+            +----+-------------+----+
                                 |             |
                                 |             |
                             MCP/API        Permit API
                                 |             |
                +----------------+             +------------------+
                |                                                     |
                v                                                     v
       +------------------+                                +-------------------+
       | MCP/API Targets  |                                | Endpoint Client   |
       | GitHub / Jira    |                                | Privileged Agent  |
       | DB / Internal    |                                | Local/Remote Ops  |
       +------------------+                                +---------+---------+
                                                                    |
                                                                    v
                                                           Protected Resources


                        MANAGEMENT / CONTROL PLANE

                    +-------------------------------+
                    | CONTAINER 2                   |
                    | ADMIN UI + MANAGEMENT BACKEND |
                    |                               |
                    | Users / Teams / Policies      |
                    | MCP Registry / API Builder    |
                    | Approvals / Audit / Devices   |
                    | Client Download / Enrollment  |
                    | Import / Export / Templates   |
                    +---------------+---------------+
                                    |
                     +--------------+-------------------+
                     |                                  |
                     v                                  v
              External State                      Config Distribution
          PostgreSQL/Object Store                 Signed Bundles/Event Bus
```

`*` SAML may be supported through the control plane or identity-broker integration. OIDC should be the primary native protocol.

---

# 6. Two-Container Boundary

The production architecture uses exactly **two primary stateless application images**. Every release additionally ships the all-in-one `mcp-access-quickstart` image for single-user/non-HA use.

External infrastructure is optional and is **not part of the application-container count**.

## Container 1: `mcp-access-gateway`

Responsibilities:

- MCP ingress.
- MCP egress/proxy.
- API-backed tool execution.
- Authentication token validation.
- Device/agent authentication.
- Authorization.
- Policy evaluation.
- Resource extraction.
- Permit issuance.
- Rate limiting.
- Request validation.
- Runtime risk checks.
- Runtime audit events.
- Optional credential broker integration.
- Health/metrics.
- No admin UI.
- No policy editing.
- No primary database ownership.

## Container 2: `mcp-access-control`

Responsibilities:

- Admin web application.
- Management REST API.
- Identity configuration.
- User/team mapping.
- MCP/server registration.
- API-to-MCP builder.
- Policy editor.
- Resource patterns.
- Approval UI/workflow.
- Audit viewer.
- Endpoint/device management.
- Client downloads.
- Client enrollment.
- Templates.
- Import/export.
- Policy/configuration bundle generation.
- Version management.
- External state ownership.

---

# 7. Statelessness and Horizontal Scaling

Both application containers must be horizontally replaceable.

## 7.1 Container 1

Container 1 stores no authoritative mutable state locally.

Allowed local runtime state:

- Read-only in-memory current policy/config bundle.
- Read-only in-memory public keys/JWK cache.
- Short TTL connection pools.
- Short TTL secret/token cache.
- Metrics.
- Best-effort replay cache when strict external replay storage is disabled.
- Temporary request buffers.

Container 1 must be safely restartable.

The 2026-07-28 MCP specification introduced a stateless protocol core, making ordinary load-balancer scaling a natural fit for this architecture.

## 7.2 Container 2

Container 2 stores no authoritative state in the container filesystem.

State must be externalized to one or more of:

- PostgreSQL — recommended authoritative metadata store.
- S3-compatible object storage — bundles, releases, artifacts, exports, optional audit archive.
- Redis/Valkey — optional distributed cache, approval signaling, replay protection, rate limits.
- NATS/Kafka/Redis Streams — optional policy bundle change notification.
- External secret manager — production recommended.

UI static files may be baked into Container 2's image.

## 7.3 No Sticky Sessions

Normal operation must not require sticky sessions.

If a particular upstream MCP protocol mode requires connection state, isolate that state in an adapter or shared store, but prefer the stateless 2026-07-28 MCP behavior.

---

# 8. Recommended Technology Baseline

The repository must document alternatives, but one implementation baseline should be chosen to avoid architecture paralysis.

## 8.1 Recommended Baseline

### Container 1 — Gateway

**Recommended:** Rust

Suggested stack:

- Rust stable.
- Tokio.
- Axum or Hyper.
- rustls.
- serde / serde_json.
- Tower middleware.
- OpenTelemetry.
- Prometheus metrics.
- Embedded policy evaluator.
- SQL parser/resource extractors only where needed.
- No JVM.
- No Node runtime.
- Distroless/scratch-compatible final image.

Reasons:

- Low memory.
- Low startup time.
- Strong performance.
- Memory safety.
- Static/small deployment binary.
- Strong fit for security-sensitive data-plane code.
- Good cross-platform reuse for endpoint client.

### Container 2 — Control Plane

**Recommended:** Java 21+ plus React/TypeScript.

Backend options:

1. **Quarkus** — recommended when low footprint/native compilation matters.
2. Spring Boot — strongest ecosystem and contributor familiarity.
3. Micronaut — good alternative.

Recommended starting choice:

- Java 21.
- Quarkus.
- RESTEasy Reactive.
- Hibernate ORM/Panache or jOOQ.
- Flyway.
- PostgreSQL.
- OpenAPI generation.
- OIDC.
- Micrometer/OpenTelemetry.

Frontend:

- React.
- TypeScript.
- Vite.
- TanStack Query.
- React Router.
- A mature accessible component library.
- JSON/YAML editor only in Advanced mode.
- Form-first UX.

The UI is compiled and served by Container 2 so no third UI container is required.

### Endpoint Client

**Recommended:** Rust.

- Single native binary.
- Privileged OS service.
- Optional Tauri system-tray application.
- rustls/mTLS.
- Platform security adapters.

## 8.2 Viable Alternative: All Java

If contributor familiarity with Java is more important than the smallest possible gateway:

- Java 21.
- Quarkus native image.
- Netty/Vert.x.
- Official MCP Java SDK where appropriate.
- GraalVM Native Image.

This reduces language count, but the gateway must be benchmarked against the Rust target.

## 8.3 Viable Alternative: Go Gateway

Go is an excellent alternative if contributor simplicity and fast development are prioritized.

Advantages:

- Single binary.
- Easy concurrency.
- Very good HTTP stack.
- Easy cross compilation.
- Easy operations.

Tradeoff:

- Rust offers stronger memory-safety control and can achieve lower-level optimization, while Go generally gives simpler implementation.

## 8.4 Technology Decision Rule

Container 1 must be selected based on measurable targets, not preference.

Build a gateway benchmark prototype in the leading candidate(s) and measure:

- p50/p95/p99 authorization latency.
- MCP pass-through latency.
- Requests/sec/core.
- Memory at idle and under load.
- Startup time.
- Container image size.
- TLS performance.
- Policy evaluation cost.
- JSON parsing overhead.

---

# 9. Container 1 Detailed Design — Access Gateway

## 9.1 Runtime Pipeline

```text
Request
  |
  v
TLS/mTLS termination
  |
  v
Protocol validation
  |
  v
Identity validation
  |
  v
Agent/device identification
  |
  v
Tool identification
  |
  v
Argument normalization
  |
  v
Resource extraction
  |
  v
Policy evaluation
  |
  +---------- BLOCK ----------> deny + audit
  |
  +---------- ASK ------------> approval flow + pending result
  |
  +---------- ALLOW
  |
  v
Credential delegation/broker
  |
  v
Upstream execution
  |
  v
Response validation/filtering
  |
  v
Audit event
  |
  v
Response
```

## 9.2 Hard Requirement: No Control-Plane Dependency in Normal Request Path

Container 1 must not query Container 2 for every request.

It should evaluate using:

- Current signed policy bundle.
- Current resource extractor definitions.
- Current server/tool catalog.
- Current public keys.
- Cached identity metadata where safe.

This is essential for:

- Performance.
- Availability.
- Security isolation.
- Independent scaling.

## 9.3 Signed Configuration Bundle

Container 2 generates a versioned bundle:

```json
{
  "schemaVersion": "1",
  "bundleVersion": 1842,
  "createdAt": "...",
  "expiresAt": "...",
  "tenants": [],
  "servers": [],
  "tools": [],
  "resourceExtractors": [],
  "policies": [],
  "riskRules": [],
  "approvalRules": [],
  "identityMappings": [],
  "publicKeys": []
}
```

Bundle requirements:

- Canonical serialization.
- SHA-256 or stronger content hash.
- Signed by control-plane signing key.
- Signature verified by gateway.
- Monotonic version.
- Rollback protection.
- Expiration.
- Last-known-good retention.
- Atomic activation.

Gateway never activates an invalid bundle.

## 9.4 Bundle Distribution

Support multiple strategies:

1. PostgreSQL polling.
2. S3/object-store fetch + event notification.
3. NATS.
4. Redis/Valkey pub/sub.
5. Kafka.
6. HTTPS pull from a protected control-plane publication endpoint.

Recommended production model:

```text
Control Plane -> Object Store
             -> notification
Gateway      -> fetch signed immutable bundle
```

The notification is not trusted; the signature is.

## 9.5 Authorization Decision

Internal decision:

```text
ALLOW
ASK
BLOCK
```

Optional internal states:

```text
NO_MATCH
ERROR
POLICY_EXPIRED
IDENTITY_INVALID
DEVICE_UNTRUSTED
RATE_LIMITED
```

`NO_MATCH` defaults to `BLOCK`.

Authorization must be deterministic.

No LLM may make a security decision.

## 9.6 Inputs to Policy Evaluation

Minimum inputs:

```text
tenant
user.id
user.groups
user.roles
agent.id
agent.type
agent.trustLevel
device.id
device.posture
source.ip
server.id
tool.id
tool.risk
arguments.normalized
resource.type
resource.id
resource.attributes
operation
environment
request.time
approval.context
```

## 9.7 Resource Extractors

A Resource Extractor converts tool arguments into policy-relevant context.

Examples:

### GitHub

Input:

```json
{
  "owner": "company",
  "repo": "payment-service",
  "branch": "main"
}
```

Output:

```json
{
  "resourceType": "github.repository",
  "resourceId": "company/payment-service",
  "attributes": {
    "owner": "company",
    "repo": "payment-service",
    "branch": "main"
  }
}
```

### SQL

Input:

```json
{
  "database": "production",
  "sql": "DELETE FROM customer WHERE id = ?"
}
```

Output:

```json
{
  "resourceType": "database.table",
  "resourceId": "production.public.customer",
  "operation": "DELETE"
}
```

Important:

- Do not rely only on regex for SQL.
- Use a parser for supported SQL dialects.
- Unknown/ambiguous destructive statements must be escalated or blocked.
- Prepared-statement values must not be copied unredacted into audit by default.

## 9.8 Tool Classification

Tools should support:

- Risk:
  - READ
  - WRITE
  - DESTRUCTIVE
  - PRIVILEGED
  - UNKNOWN

- Side effect:
  - NONE
  - IDEMPOTENT
  - MUTATING
  - DESTRUCTIVE

- Data classification:
  - PUBLIC
  - INTERNAL
  - CONFIDENTIAL
  - RESTRICTED

- Approval behavior.
- Rate limit.
- Timeout.
- Maximum payload.
- Resource extractor.

Automatic classification may suggest values, but an admin must be able to review/override.

## 9.9 Rate Limits

Support:

- Per tenant.
- Per team.
- Per user.
- Per agent.
- Per device.
- Per server.
- Per tool.
- Per resource.
- Global emergency limit.

Implementation:

- Local token bucket for low-overhead limits.
- Optional distributed rate limit using Redis/Valkey for global correctness.

## 9.10 Runtime Limits

Per tool:

- Request body max.
- Response body max.
- Timeout.
- Connect timeout.
- Retry policy.
- Concurrency limit.
- Stream duration.
- Header allowlist.
- Redirect behavior.
- DNS/IP allowlist.
- Egress destination restrictions.

---

# 10. Container 2 Detailed Design — Control Plane and UI

Container 2 is the product's human interface.

The UI must be optimized for the principle:

> **Simple first; power available when requested.**

## 10.1 Main Navigation

Recommended top-level navigation:

```text
Home
Access
  Users
  Teams
  Agents
  Devices
Tools
  MCP Servers
  Tools
  API -> MCP
  Templates
Policies
  Access Policies
  Resource Rules
  Approvals
Activity
  Audit
  Live Requests
Clients
  Downloads
  Enrollments
Settings
  Identity
  Secrets
  Gateway
  Import / Export
  System
```

## 10.2 Dashboard

Dashboard should answer:

- Is the system healthy?
- How many gateway replicas are online?
- How many MCP servers are healthy?
- How many users/agents/devices are active?
- What was allowed/blocked/approved?
- Are there pending approvals?
- Are policy bundles synchronized?
- Are clients outdated?
- Are there unknown/unmanaged MCP connections?

Example:

```text
Gateway replicas       6 healthy
Control replicas       2 healthy
MCP servers            31 / 32 healthy
Users                  187
Managed devices        143
Requests today         84,219
Allowed                82,991
Approval required      913
Blocked                315
Pending approvals      7
Current policy         v1842
```

## 10.3 Access Policy Wizard

Normal mode:

```text
WHO
  Team: Engineering

USING
  Any approved interactive AI

CAN USE
  GitHub

ACTION
  Create pull request

WHERE
  company/engineering/*

DECISION
  ALLOW
```

Advanced mode exposes:

- Conditions.
- Agent types.
- Device posture.
- Time window.
- IP/network.
- Arguments.
- Risk.
- Ticket requirement.
- Maximum duration.
- Approval group.

## 10.4 Policy Simulation

Before publishing a policy:

- Show newly allowed combinations.
- Show newly blocked combinations.
- Show overlap/conflicts.
- Show unreachable rules.
- Show risky expansions.
- Test against historical audit events.

Example:

```text
Policy Impact

Users affected:             37
Agents affected:            12
Resources newly allowed:     8
Resources newly blocked:     2
Historical requests changed: 41

High-risk expansion:
  production/payroll would become readable by Engineering
```

Admin can cancel or continue.

## 10.5 Policy Versioning

Every publish creates:

- Version.
- Author.
- Timestamp.
- Comment.
- Diff.
- Signature/bundle reference.

Support:

- Rollback.
- Compare.
- Draft.
- Scheduled activation.
- Emergency policy.
- Git export.

---

# 11. API-to-MCP Builder

This is a first-class feature, not an afterthought.

Goal:

> A user can create a governed MCP tool around an existing API without writing code.

## 11.1 Creation Paths

The UI offers:

```text
Create API Tool

[ Simple Form ]
[ Import OpenAPI ]
[ Import cURL ]
[ Import Postman Collection ]
[ Advanced JSON/YAML ]
```

V1 minimum:

- Simple Form.
- OpenAPI import.
- cURL import.

Postman and GraphQL can follow.

## 11.2 Simple Form

Fields:

### Basics

- Tool name.
- Display name.
- Description.
- Category.
- Tags.
- Icon optional.
- Risk classification.
- Side-effect classification.

### API

- Base URL.
- Method:
  - GET
  - POST
  - PUT
  - PATCH
  - DELETE
- Path.
- Content type.
- Accept type.
- Timeout.
- Redirect policy.
- Retry policy.

### Authentication

Options:

- None.
- Static bearer token.
- API key header.
- API key query parameter.
- Basic authentication.
- OAuth2 authorization code.
- OAuth2 client credentials.
- OIDC.
- mTLS.
- Custom header via secret reference.
- User-delegated OAuth.

Secrets must be stored by reference, not plaintext in tool definitions.

### Inputs

Admin can add fields:

```text
name
label
description
type
required
default
enum
validation
location
mapping
```

Locations:

- Path.
- Query.
- Header.
- JSON body.
- Form body.

Supported types:

- string.
- integer.
- number.
- boolean.
- object.
- array.
- enum.
- file/reference where supported.

### Response

Configure:

- Success status codes.
- JSONPath/JMESPath-style result extraction.
- Fields exposed to AI.
- Fields redacted.
- Maximum response size.
- Error mapping.
- Pagination.
- Optional response schema.

### Resource Mapping

This is critical.

The admin can map one or more inputs into resource identity.

Example:

```text
Tool: restart_service

resource.type = "service"
resource.id   = "${environment}/${service}"
operation     = "restart"
```

This allows normal access policies to govern generated API tools.

## 11.3 Test Console

Before publishing:

```text
Test Tool
```

UI shows:

- Rendered request with secrets redacted.
- Request URL.
- Headers names.
- Body.
- Response status.
- Response time.
- Parsed result.
- MCP-visible result.
- Extracted resource.
- Proposed risk.
- Authorization simulation.

## 11.4 Publish

Publishing:

1. Validates schema.
2. Validates destination restrictions.
3. Validates auth.
4. Validates resource mapping.
5. Assigns a version.
6. Creates tool manifest.
7. Includes tool in signed gateway bundle.
8. Makes the tool available through selected virtual MCP endpoint(s).

No gateway restart.

## 11.5 OpenAPI Import

Flow:

```text
Paste URL / Upload file
        |
        v
Parse OpenAPI
        |
        v
List operations
        |
        v
Select operations
        |
        v
Configure auth
        |
        v
Review risk/resource mapping
        |
        v
Publish
```

Allow admins to exclude destructive/admin endpoints.

## 11.6 cURL Import

Paste:

```bash
curl -X POST ... 
```

System derives:

- URL.
- Method.
- Headers.
- Body.
- Candidate inputs.

Secrets are detected and converted to secret references before saving.

---

# 12. Endpoint Enforcement Client

## 12.1 Purpose

The endpoint client solves two distinct problems:

1. Make locally available tools controllable by central policy.
2. Allow privileged operations without giving the AI process or ordinary user the privileged credentials directly.

## 12.2 Client Modes

### Mode A — User Endpoint

Installed on developer/user workstation.

Provides:

- Local MCP endpoint.
- Local tool registry.
- Per-call authorization through Container 1.
- Device identity.
- Managed local tool execution.
- Optional OS enforcement.

### Mode B — Privileged Executor / Jump Host

Installed on a secured administration host.

Provides controlled elevated operations against:

- Linux hosts via SSH.
- Windows via WinRM/PowerShell Remoting.
- Kubernetes clusters.
- Cloud CLIs/APIs.
- Databases.
- Internal systems.
- Network devices through supported adapters.

The same authorization model applies.

## 12.3 Privilege Separation

Do not run the whole UI/client as root/Administrator.

Recommended process split:

```text
User UI / Tray
      |
      | IPC
      v
Unprivileged Client Process
      |
      | authenticated local IPC
      v
Privileged Enforcement Service
      |
      +---- Container 1 authorization
      |
      +---- controlled execution
```

The privileged service:

- Has minimal OS permissions.
- Owns protected tool credentials.
- Verifies permit signature.
- Verifies operation hash.
- Verifies device/user/agent binding.
- Enforces tool path.
- Enforces arguments.
- Emits audit events.

## 12.4 Every Controlled Invocation Must Validate Through Container 1

Default strict flow:

```text
AI/local caller
      |
      v
Endpoint Client
      |
      | request authorization
      v
Container 1
      |
      | ALLOW + signed permit
      v
Endpoint Client
      |
      | verify permit
      v
Privileged Service
      |
      v
Execute
```

A permit should include:

```json
{
  "permitVersion": 1,
  "tenant": "...",
  "user": "...",
  "agent": "...",
  "device": "...",
  "tool": "...",
  "resource": "...",
  "operation": "...",
  "requestHash": "...",
  "policyVersion": 1842,
  "issuedAt": "...",
  "expiresAt": "...",
  "jti": "..."
}
```

Properties:

- Signed by Container 1.
- Very short TTL; typical 5–30 seconds.
- Bound to request hash.
- Bound to device.
- Bound to tool/action.
- Bound to resource.
- Prefer single use.
- Never usable to authorize a different command.

## 12.5 Replay Protection

Levels:

### Standard

- Short permit TTL.
- Device binding.
- Request hash.
- Client-side consumed-JTI cache.

### Strict

Use distributed Redis/Valkey or another atomic store to mark permit IDs as consumed.

Use strict mode for:

- Privileged actions.
- Destructive operations.
- Production changes.
- Credential issuance.

---

# 13. Direct Local Call Enforcement

This requirement needs precise treatment.

A normal wrapper is not sufficient.

If an ordinary user can execute:

```text
real-admin-tool ...
```

directly, they can bypass:

```text
controlled-wrapper real-admin-tool ...
```

Therefore the project must define enforcement strength.

## 13.1 Enforcement Levels

### Level 0 — Observe

- Detect and log client-managed tool calls.
- No bypass prevention.
- Useful for evaluation only.

### Level 1 — Managed Wrapper

- AI clients receive only managed tool endpoints.
- PATH shims/wrappers.
- Good user experience.
- Not a security boundary against a knowledgeable local user.

### Level 2 — Credential Custody

Strong baseline.

- Real credential/private key/API token is owned by privileged service.
- Normal user and AI process never receive it.
- Direct execution of underlying tool cannot perform protected operation without the credential.
- Each privileged execution requires Container 1 permit.

### Level 3 — OS-Enforced Execution / Network Policy

Strongest endpoint option.

Examples:

#### Linux

Consider:

- Unix permissions.
- sudoers with narrow commands.
- SELinux.
- AppArmor.
- systemd service sandboxing.
- namespaces.
- seccomp.
- eBPF/LSM where appropriate.
- nftables/iptables egress controls.

#### Windows

Consider:

- Windows service under dedicated identity.
- ACLs.
- Windows Defender Application Control.
- AppLocker where applicable.
- Windows Filtering Platform/network rules.
- Credential Manager / DPAPI / TPM.
- constrained PowerShell endpoints/JEA for relevant operations.

#### macOS

Consider:

- LaunchDaemon.
- Keychain.
- System Extensions where justified.
- Endpoint Security framework for higher-assurance enterprise mode.
- Network Extension for network enforcement.

## 13.2 Security Truth

The product must never claim:

> "No local Administrator/root user can bypass this."

A machine owner with full root/Administrator control may be able to disable enforcement.

The security claim should instead be:

> **Managed tools and protected credentials can be configured so ordinary user/agent processes cannot successfully perform protected operations without authorization from Container 1.**

---

# 14. Client Download, Install, and Registration

Container 2 must provide a dedicated **Clients** section.

## 14.1 Download Page

Example:

```text
Endpoint Client

Windows x64      [Download]
Windows ARM64    [Download]
Linux x64        [Download]
Linux ARM64      [Download]
macOS Intel      [Download]
macOS Apple      [Download]

Current stable: 1.2.4
```

All builds must be:

- Checksummed.
- Signed.
- Reproducible where practical.
- Published through release CI.
- Verified by client installer.

## 14.2 Enrollment

Admin creates enrollment:

```text
Add Client
  |
  v
Create one-time enrollment token
  |
  v
Download installer / copy command
```

Example:

```text
mcp-access-client enroll \
  --server https://control.company.example \
  --token <one-time-token>
```

Token properties:

- One-time.
- Short TTL.
- Optional team/site binding.
- Optional expected OS.
- Revocable.

## 14.3 Device Identity

During enrollment:

1. Client generates device key pair.
2. Private key stays on device.
3. Prefer TPM/Secure Enclave/OS protected storage where available.
4. Client submits public key/CSR.
5. Control plane registers device.
6. Device receives identity certificate/token.
7. Gateway trusts only active registered devices.

## 14.4 Device Lifecycle

States:

```text
PENDING
ACTIVE
QUARANTINED
REVOKED
RETIRED
```

Admin operations:

- Rename.
- Assign owner.
- Assign team.
- View version.
- View health.
- Quarantine.
- Revoke.
- Force re-enroll.
- Set minimum version.
- Schedule update.

---

# 15. Remote Elevated Operations

A key supported use case is:

> An AI agent requests an elevated operation against a remote system, but neither the AI nor the normal user receives unrestricted privileged credentials.

## 15.1 Example

```text
Codex
  |
  | restart production API
  v
Endpoint Client
  |
  | authorization request
  v
Container 1
  |
  | policy = ASK
  v
Approval
  |
  | approved once
  v
Container 1
  |
  | signed permit
  v
Privileged Executor
  |
  | managed credential
  v
Production Host
```

## 15.2 Credential Models

Preference order:

1. Native delegated user identity.
2. Short-lived federated credential.
3. Dynamic secret from Vault/PAM.
4. Device/service identity with strict resource policy.
5. Long-lived static secret only as last resort.

Static high-privilege secrets should never be copied into LLM context.

## 15.3 Remote Executor Adapters

Potential adapters:

- SSH.
- WinRM.
- PowerShell Remoting/JEA.
- Kubernetes.
- Docker.
- REST API.
- Database.
- Cloud providers.
- Custom command template.

Every adapter must define:

- Input schema.
- Resource extractor.
- Risk.
- Credential model.
- Audit redaction.
- Timeout.
- Allowed destinations.

---

# 16. Identity and Authentication

## 16.1 Human Identity

Primary:

- OIDC.

Integrations:

- Microsoft Entra ID.
- Okta.
- Google Workspace / Cloud Identity.
- Keycloak.
- Auth0.
- Generic OIDC.

Optional:

- SAML through supported enterprise identity integration/broker.

Community quickstart:

- Local admin account.
- Local users optional.
- Must warn that OIDC is recommended for team deployment.

## 16.2 MCP Authorization

Track the current MCP authorization specification.

Support the stable Enterprise-Managed Authorization extension where appropriate so organizations can centrally provision MCP access and reduce repeated per-server OAuth flows.

## 16.3 Agent Identity

Agent registration:

```text
id
name
type
owner
environment
interactive/unattended
trust level
public key/client credential
allowed clients
```

Examples:

- codex-interactive.
- claude-desktop.
- cursor.
- deploy-bot.
- incident-agent.

## 16.4 Device Identity

See endpoint enrollment.

## 16.5 Workload Identity

Service-to-service and unattended workloads should support:

- OAuth client credentials.
- mTLS.
- SPIFFE/SPIRE later.
- Cloud workload identities later.

---

# 17. Authorization Model

Policy decision:

```text
decision = f(
  user,
  team,
  role,
  agent,
  device,
  server,
  tool,
  resource,
  operation,
  arguments,
  environment,
  context
)
```

## 17.1 Policy Precedence

Recommended:

1. Emergency global deny.
2. Explicit deny/block.
3. Required approval.
4. Explicit allow.
5. Default block.

A lower-priority rule must not override an explicit higher-priority block.

## 17.2 Inheritance

Example:

```text
Organization
  -> Team
     -> User
        -> Agent
           -> Resource
```

UI should make inherited policy visible.

## 17.3 Temporary Access

Support:

- Start/end time.
- TTL.
- Change/incident ticket.
- Reason.
- Approver.
- Automatic expiry.

---

# 18. Policy Engine Technology

Options:

## Cedar

Strengths:

- Authorization-specific.
- Entity/resource model.
- Good fit for user/action/resource policies.
- Strong explainability potential.

## OPA/Rego

Strengths:

- Mature.
- General-purpose.
- Flexible.
- Can compile to WASM/embedded forms.

## Casbin

Strengths:

- Straightforward RBAC/ABAC.
- Broad ecosystem.

## Custom Policy Language

Not recommended as the underlying V1 security engine unless absolutely necessary.

A custom human-friendly policy representation may exist in the UI/export schema, but it should compile to a tested deterministic authorization engine.

**Recommended direction:** evaluate Cedar and embedded OPA with representative benchmarks before finalizing. The UI must remain independent of the underlying engine.

---

# 19. Approvals

## 19.1 Approval Object

```text
request id
user
agent
device
tool
resource
operation
arguments summary
risk
policy reason
requested at
expires at
approver group
decision
decision time
comment
```

## 19.2 Approval Channels

V1:

- Admin UI.
- Browser notification/live dashboard.

Later:

- Slack.
- Microsoft Teams.
- Email.
- Mobile push.
- Webhook.

## 19.3 Approval Choices

```text
Deny
Approve once
Approve for 10 minutes
Approve for 30 minutes
Approve this resource for session
```

Do not allow broad "always allow" from a request popup unless administrator permissions permit it.

---

# 20. Secrets, Credentials, and Vault Integration

Credential handling is a **core security subsystem**. No component may treat credentials as ordinary configuration.

The project must implement a provider-neutral **Credential Vault abstraction** used everywhere a password, token, key, certificate, OAuth client secret, database credential, SSH identity, API key, or privileged secret is required.

The standard rule is:

```text
Configuration / Policy / Tool Definition / Export
                    |
                    v
              secret reference
                    |
                    v
             Credential Broker
                    |
                    v
            Configured Vault
                    |
                    v
      short-lived use by authorized component
```

The LLM, MCP tool definition, ordinary audit event, and exported YAML/JSON should never receive the underlying secret.

## 20.1 Secret Reference Model

All components use canonical references such as:

```text
secret://github/team-oauth-client
secret://postgres/reporting-password
secret://ssh/prod-jump-host
secret://api/weather-key
```

Example:

```yaml
auth:
  type: bearer
  tokenRef: secret://api/orders-token
```

Never:

```yaml
auth:
  token: actual-secret-value
```

A secret reference must resolve only inside a trusted execution component.

## 20.2 Supported Vault Providers

The vault interface must be pluggable.

### Quickstart / Community Built-In Vault

The single-container Quickstart includes an encrypted built-in vault so the user has **no external dependency**.

Use:

- Envelope encryption.
- Authenticated encryption such as AES-256-GCM or equivalent.
- Per-secret data-encryption keys where practical.
- A master key stored separately from encrypted database rows.
- Key versioning.
- Rotation support.
- Secret metadata audit.
- Never expose plaintext through ordinary management APIs after creation.

The Quickstart master key should be persisted under the Quickstart data volume with strict filesystem permissions or provided by environment/OS secret mechanism.

Quickstart must warn that enterprise deployments should prefer a dedicated external vault/KMS when available.

### Production / Enterprise Vault Integrations

Support adapters for:

- HashiCorp Vault.
- AWS Secrets Manager.
- Azure Key Vault.
- Google Cloud Secret Manager.
- Kubernetes Secrets.
- External Secrets Operator-compatible stores.
- CyberArk / enterprise PAM as a later adapter.
- 1Password Connect or similar service as an optional community adapter.
- Generic provider SDK interface for custom enterprise vaults.

No vault provider should be hard-coded into gateway policy logic.

## 20.3 Vault Provider Interface

Canonical operations:

```text
createSecret()
updateSecret()
getSecretMetadata()
resolveSecretForUse()
rotateSecret()
disableSecret()
deleteSecret()
listSecretVersions()
healthCheck()
```

`resolveSecretForUse()` is highly privileged and must never be exposed directly to ordinary UI users or AI clients.

## 20.4 Secret Metadata

Safe metadata that may be stored in PostgreSQL/control-plane config:

```text
secret reference
display name
provider
provider path/id
credential type
owner/team
created_at
rotated_at
expires_at
version
status
scope
last_used_at
```

Do not store plaintext secret values.

## 20.5 Credential Types

The vault system must support at least:

- API key.
- Bearer token.
- Username/password.
- OAuth client ID/client secret.
- OAuth refresh token.
- SSH private key.
- SSH certificate.
- TLS private key/certificate.
- Database username/password.
- Cloud access credentials.
- Generic opaque secret.
- Client certificate material where storage model requires it.

Prefer dynamic/short-lived credentials over static ones.

## 20.6 User-Delegated Credentials

When possible, preserve the user's upstream identity:

```text
Rahul
  |
  v
Gateway authorization
  |
  v
Rahul's delegated OAuth token
  |
  v
GitHub/Jira/etc.
```

This is preferable to:

```text
All users
  |
  v
shared superuser token
```

The vault/broker must support **per-user credential bindings**.

Example:

```text
secret://users/{userId}/github/oauth
```

The control plane should display:

```text
GitHub
  Authentication: Per-user OAuth
  Rahul: Connected
  Alice: Connected
  Bob: Not connected
```

No administrator needs to see the user's token.

## 20.7 Credential Broker

Container 1 should contain a minimal credential-broker adapter layer.

Responsibilities:

- Resolve only the secret required for the authorized request.
- Scope the credential to the upstream target where possible.
- Cache only when explicitly safe.
- Use short TTLs.
- Immediately discard plaintext after use.
- Never place plaintext into structured application logs.
- Never send plaintext back to the AI client.
- Emit safe usage audit metadata.

The control plane may manage secret metadata, but normal runtime secret resolution should not require Container 2 to be online.

For external vault deployments, Container 1 should authenticate directly to the vault using workload identity.

## 20.8 Endpoint Client Credential Custody

The endpoint client may need credentials for privileged local/remote tools.

Preferred order:

1. Dynamic secret requested from external vault/PAM after Container 1 authorizes.
2. Short-lived federated credential.
3. Per-user delegated credential.
4. Device/service credential stored in OS secure storage.
5. Long-lived static credential only when unavoidable.

OS secure storage options include:

### Windows

- DPAPI.
- Windows Credential Manager.
- TPM-backed keys.

### macOS

- Keychain.
- Secure Enclave where applicable.

### Linux

- Kernel/desktop secret service where appropriate.
- TPM2.
- root-owned protected files only as fallback.

A privileged credential must not be written into an AI-visible environment variable.

## 20.9 API-to-MCP Builder Vault Integration

The API builder must never ask the user to paste secrets into tool JSON/YAML.

UI flow:

```text
Authentication
  API Key

Credential
  [Create New Credential]
  [Select Existing Credential]
```

Creating a credential:

```text
Name: Orders API Key
Provider: Built-in Vault / HashiCorp / AWS / ...
Value: ********
```

After saving, the tool sees only:

```text
secret://api/orders-key
```

Supported mappings:

- Header.
- Query parameter.
- Basic auth.
- Bearer.
- OAuth.
- mTLS.

Generated exports include only references.

## 20.10 MCP Server Vault Integration

When registering a remote MCP server, authentication fields must bind to vault references:

```yaml
server:
  name: internal-github
  url: https://mcp.internal.example

auth:
  type: oauth
  clientSecretRef: secret://mcp/github/oauth-client
```

If an MCP server uses per-user OAuth, user grants/tokens are managed as user-scoped credentials.

## 20.11 Identity Provider Secrets

OIDC/OAuth/SAML-related credentials must also use the vault abstraction:

- OIDC client secret.
- SAML signing key.
- SCIM bearer token.
- webhook signing secret.
- SMTP credential.
- external approval integration token.

## 20.12 Database and Infrastructure Credentials

All connector credentials must use the same vault abstraction:

- PostgreSQL.
- MySQL.
- SQL Server.
- SSH.
- Kubernetes.
- Docker remote daemon.
- cloud providers.
- Grafana/Prometheus APIs.
- Jira/Slack/GitHub/GitLab.

Do not create one-off secret storage mechanisms per integration.

## 20.13 Credential Permissions

Credential authorization is separate from tool authorization.

Example:

```text
Tool:
  postgres.query          ALLOW

Credential:
  prod-superuser          BLOCK
  prod-readonly           ALLOW
```

The policy system should be able to select/limit which credential binding a tool may use.

A user should never be able to request an arbitrary secret reference through tool arguments.

## 20.14 Dynamic Credentials

Enterprise integrations should support lease-based credentials where provider supports them.

Examples:

```text
Vault DB dynamic credential
AWS STS
Azure workload identity
GCP short-lived service account token
SSH certificate
Kubernetes projected token
```

Runtime:

```text
authorize
   |
   v
obtain short-lived credential
   |
   v
execute
   |
   v
revoke/expire
```

This should become the recommended model for privileged remote execution.

## 20.15 Rotation

Support:

- Manual rotation.
- Scheduled rotation.
- Provider-native rotation.
- Expiry warnings.
- Dual-version grace window where needed.
- Test-before-activate.
- Rollback to previous secret version when safe.

Tool configurations should reference logical secret names, not physical secret versions unless explicitly pinned.

## 20.16 Secret Import / Export

Default configuration export:

```yaml
credential:
  ref: secret://github/team-token
  provider: vault
```

No secret value.

Optional **encrypted backup export** may be added later, but must:

- Be explicitly requested.
- Use a user-supplied backup key or enterprise KMS.
- Be clearly distinguished from ordinary config export.
- Never be created silently.

## 20.17 Logging and Redaction

Redact:

- Authorization headers.
- cookies.
- passwords.
- API keys.
- private keys.
- OAuth tokens.
- refresh tokens.
- signed URLs where sensitive.
- known configured secret values.
- configurable sensitive request/response paths.

Logs should use:

```text
credentialRef=secret://github/team-token
credentialResolved=true
```

not the actual value.

## 20.18 Secret Scanning

Before saving:

- API definitions.
- MCP configuration.
- YAML/JSON imports.
- templates.

scan for likely embedded credentials.

If found:

```text
Potential secret detected.

Convert this value to a vault credential?

[Convert and Save]
[Cancel]
```

Do not silently persist the secret in normal configuration.

## 20.19 Vault Health and Failure Behavior

If the vault is unavailable:

- Requests requiring an uncached credential fail closed.
- Existing low-risk operations not requiring secrets may continue.
- Never fall back to a hard-coded or exported credential.
- UI shows clear vault-health status.

## 20.20 Vault Administration UI

Navigation:

```text
Settings
  -> Credentials & Vaults
```

Pages:

```text
Vault Providers
Credentials
User Connections
Rotation
Health
Audit
```

Normal users see:

```text
GitHub Account      Connected
Jira Account        Connected
AWS Production      Not available to you
```

Security administrators see metadata and bindings but not plaintext values.

## 20.21 Separation of Duties

Recommended permissions:

- Credential Admin can create/rotate credentials.
- Tool Admin can bind approved credential references to tools.
- Security Admin can define access.
- Auditor can inspect usage metadata.
- None of these roles automatically receives plaintext secret-read capability.

## 20.22 Credential Security Invariants

Automated tests must prove:

1. Plain secrets never appear in ordinary config export.
2. Plain secrets never appear in tool schemas.
3. Plain secrets never appear in MCP responses.
4. Plain secrets never appear in standard audit.
5. Unauthorized users cannot resolve secret references.
6. A tool cannot substitute an arbitrary secret reference.
7. A secret for Tool A cannot be reused by Tool B unless explicitly configured.
8. Disabled/revoked credentials fail closed.
9. Gateway cache never extends beyond configured credential TTL.
10. Quickstart vault remains encrypted at rest.
11. External-vault outage never causes unrestricted fallback.
12. Credential rotation does not require editing every policy/tool that references the logical secret.

---

# 21. Configuration Import / Export

Everything except unencrypted secrets must be representable in a versioned portable configuration model.

## 21.1 Formats

- YAML.
- JSON.

## 21.2 Example

```yaml
apiVersion: mcp-access.io/v1
kind: Configuration

metadata:
  name: company-default

teams:
  - name: engineering

servers:
  - name: github
    type: remote-mcp
    url: https://example.internal/mcp

policies:
  - name: engineering-github
    subject:
      team: engineering
    target:
      server: github
      tool: create_pull_request
      resource: company/engineering/*
    decision: ALLOW
```

## 21.3 Export Modes

- Full configuration.
- Team.
- MCP server.
- Policies.
- API-generated MCP.
- Templates.
- Client policy.
- System settings.

## 21.4 Import Workflow

```text
Upload/Paste
   |
   v
Parse
   |
   v
Schema validation
   |
   v
Security validation
   |
   v
Diff
   |
   v
Dry run
   |
   v
Apply as draft
   |
   v
Publish
```

## 21.5 GitOps

Later/V1.1:

- Export config to Git.
- Watch Git repository.
- Pull request preview.
- Signed commits optional.
- Policy impact in CI.

---

# 22. Quickstart / Community Edition

The Community/Quickstart edition must produce useful value immediately after deployment.

The project has **two deployment models**:

```text
QUICKSTART / SINGLE USER

One Docker container
    |
    +-- Gateway/data plane
    +-- Admin UI
    +-- Management backend
    +-- Embedded SQLite database
    +-- Embedded process-local cache
    +-- Built-in encrypted credential vault
    +-- Local policy/artifact storage
    +-- Built-in safe tool pack
    +-- Client download/enrollment endpoints
```

and:

```text
ENTERPRISE / STATELESS

Load Balancer
   |
   +--> Gateway Container x N
   |
   +--> Control/Admin Container x N

External state:
   PostgreSQL
   Redis/Valkey optional
   Object storage optional/recommended
   External IdP
   External vault/KMS recommended
```

Quickstart is intentionally **single-node/non-scalable**. Enterprise mode is designed for horizontal scaling.

## 22.1 Three Official Container Images

Every public release must build, test, sign, and publish all three application images:

```text
1. mcp-access-quickstart
2. mcp-access-gateway
3. mcp-access-control
```

### `mcp-access-quickstart`

Purpose:

- Single user.
- Personal use.
- Community evaluation.
- Fast proof-of-concept.
- Small non-HA deployment.

No mandatory external dependencies.

Contains:

- Container 1 runtime functionality.
- Container 2 management functionality.
- Admin UI.
- SQLite.
- Process-local cache.
- Built-in encrypted credential vault.
- Local signed-bundle/artifact storage.
- Built-in safe tool pack.
- Client installer/download endpoints.

Persistent Docker volume:

```text
/data
```

Stores:

- SQLite database.
- encrypted vault records.
- Quickstart master-key material or protected key reference.
- audit.
- configuration.
- signed policy bundles.
- client enrollment state.
- generated exports.

Quickstart must show:

```text
Single-node Community Mode
Not horizontally scalable
```

in System Information.

### `mcp-access-gateway`

Production Container 1:

- Stateless.
- Horizontally scalable.
- Lightweight/native.
- No embedded authoritative database.
- No embedded production credential store.
- Authorization/routing/permit data plane.

### `mcp-access-control`

Production Container 2:

- Stateless.
- Horizontally scalable.
- Admin UI + management backend.
- Persistent state is external.

## 22.2 Quickstart Startup Goal

Target experience:

```text
docker run ...
     |
     v
Open http://localhost:<port>
     |
     v
Login
     |
     v
Built-in tools already usable
     |
     v
Download endpoint client
     |
     v
Install using enrollment URL
     |
     v
Connect AI client
```

The user must not need to understand:

- PostgreSQL.
- Redis.
- Kubernetes.
- OIDC.
- MCP transport internals.
- policy engine syntax.
- secret-store setup.
- YAML.

## 22.3 First Login

Do not ship a universal fixed password.

Bootstrap methods:

1. `QUICKSTART_ADMIN_PASSWORD` environment variable.
2. Otherwise generate a cryptographically random one-time bootstrap password and print it once in logs.
3. Require/strongly prompt password change on first successful login.

Default user:

```text
admin
```

Quickstart automatically provisions:

```text
Organization:    Personal Workspace
Team:            Local User
Agent Profile:   Interactive AI
Policy Profile:  Quickstart Safe Defaults
Vault:           Built-in Encrypted Vault
```

The user can modify all settings after login.

## 22.4 HotFolder

Installing the endpoint client creates:

```text
HotFolder
```

Recommended defaults:

### Windows

```text
%LOCALAPPDATA%\MCPAccess\HotFolder
```

### Linux

```text
~/.local/share/mcp-access/HotFolder
```

### macOS

```text
~/Library/Application Support/MCPAccess/HotFolder
```

The resolved path is:

- Configurable.
- Reported during enrollment.
- Visible in admin UI.
- Used by the built-in HotFolder tools.

## 22.5 HotFolder Security

Built-in tools must never escape the configured root.

Required protections:

- Canonical path resolution.
- Reject traversal.
- Safe symlink handling.
- Maximum read/write size.
- Configurable file extension/MIME restrictions.
- Quotas.
- Operation audit.
- Relative paths in AI-visible interfaces.

### Standard Mode

```text
Standard HotFolder
```

Provides managed convenience and authorization but is not a security boundary against a local user who already owns the files.

### Protected Mode

```text
Protected HotFolder
```

For stronger enforcement:

- Privileged client service/dedicated OS identity owns protected content.
- AI/user processes do not receive unrestricted file-system access.
- Access occurs through endpoint-client RPC.
- Every managed operation is authorized through Container 1.
- OS ACL/security mechanisms restrict direct bypass.
- Exact assurance depends on OS and whether the user possesses root/Administrator.

## 22.6 Built-In Safe Tool Pack

Fresh Quickstart must expose **at least 10** useful tools. The baseline should ship at least these **18**.

### HotFolder filesystem tools

1. `hotfolder.list`

List files/directories recursively or one level, bounded by configured limits.

2. `hotfolder.read_text`

Read text from a HotFolder file with size/encoding limits.

3. `hotfolder.write_text`

Create or replace a text file inside HotFolder.

4. `hotfolder.append_text`

Append bounded text to a HotFolder file.

5. `hotfolder.mkdir`

Create a directory inside HotFolder.

6. `hotfolder.file_info`

Return safe metadata.

7. `hotfolder.search_text`

Search text under HotFolder with file/result/time limits.

8. `hotfolder.watch_events`

Receive create/change/delete/rename events through a controlled subscription/long-poll interface.

9. `hotfolder.hash`

Calculate SHA-256 for a file inside HotFolder.

10. `hotfolder.move`

Move/rename only within HotFolder.

11. `hotfolder.copy`

Copy only within HotFolder.

12. `hotfolder.list_events`

Return recent client-observed HotFolder events from a bounded local event ring/buffer.

Deletion must not be enabled by default.

If `hotfolder.delete` is offered later:

```text
Default decision: ASK
Preferred behavior: recoverable trash
```

### General non-destructive tools

13. `calculator.evaluate`

Evaluate bounded arithmetic expressions using a safe parser. Never shell/eval arbitrary code.

14. `text.transform`

Safe text operations such as:

- upper/lower/title case.
- trim.
- replace.
- split/join.
- JSON pretty-print where valid.

15. `json.validate`

Validate JSON and return parse/schema errors without executing code.

16. `hash.sha256`

Hash provided text/data with strict input limit.

17. `system.info`

Return a small allowlisted set of non-sensitive client information:

- OS.
- architecture.
- client version.
- HotFolder path alias.
- gateway connectivity status.

Do not expose arbitrary environment variables.

18. `web.search`

Optional built-in web-search abstraction.

Rules:

- Disabled automatically when no search provider has been configured.
- Quickstart setup wizard can configure a supported provider credential in the built-in vault.
- Search credential is represented only by a secret reference.
- Strict query/result limits.
- No arbitrary browser automation.
- No credential-bearing fetch by default.
- SafeSearch/provider safety controls where offered.
- Domain allow/block policy.
- Network egress restrictions.
- Audit query metadata according to privacy settings.

Because web search normally depends on an external provider, it must appear as:

```text
Web Search
  Status: Needs credential
  [Configure]
```

rather than pretending to be dependency-free.

## 22.7 Built-In Tool Default Policy

Fresh Quickstart defaults:

```text
hotfolder.list           ALLOW
hotfolder.read_text      ALLOW
hotfolder.write_text     ALLOW
hotfolder.append_text    ALLOW
hotfolder.mkdir          ALLOW
hotfolder.file_info      ALLOW
hotfolder.search_text    ALLOW
hotfolder.watch_events   ALLOW
hotfolder.hash           ALLOW
hotfolder.move           ALLOW
hotfolder.copy           ALLOW
hotfolder.list_events    ALLOW

calculator.evaluate      ALLOW
text.transform           ALLOW
json.validate            ALLOW
hash.sha256              ALLOW
system.info              ALLOW

web.search               ALLOW only after configured

unknown tools             BLOCK
shell execution           BLOCK
arbitrary filesystem      BLOCK
privileged execution      BLOCK/ASK
destructive operation     BLOCK
```

All defaults are editable in the admin UI.

## 22.8 Built-In Quickstart Credential Vault

Quickstart automatically provisions:

```text
Vault: Local Encrypted Vault
```

The user can add credentials through:

```text
Settings
  -> Credentials & Vaults
  -> Add Credential
```

Examples:

```text
Web Search API Key
GitHub OAuth
Jira Token
Custom REST API Key
Database Password
```

Tools/configuration store only:

```text
secret://...
```

The user may later configure an external vault even in Community mode if supported.

## 22.9 Client Download Page

Container Quickstart and Container 2 must expose:

```text
Clients
  -> Download & Install
```

Display release artifacts:

```text
Windows x64
Windows ARM64
Linux x64
Linux ARM64
macOS Intel
macOS Apple Silicon
```

Each entry shows:

- Version.
- SHA-256.
- Signature status.
- Release notes link.
- Install command.
- Enrollment URL.

## 22.10 URL-Based Client Enrollment

The desired user experience is:

```text
Install client with a server URL
       |
       v
Browser/device authentication
       |
       v
Client enrolled
```

Example installer/start command:

```text
mcp-access-client install \
  --server https://mcp.company.example
```

or Quickstart:

```text
mcp-access-client install \
  --server http://localhost:8080
```

Flow:

1. Client connects to the server's enrollment discovery endpoint.
2. Server returns:
   - control/gateway endpoints.
   - TLS requirements.
   - auth flow.
   - organization display name.
3. Client creates a device key pair.
4. Client opens the system browser to a short-lived device enrollment URL.
5. User authenticates.
6. User confirms the device.
7. Server binds user + device + client public key.
8. Client receives device certificate/token.
9. Client verifies gateway trust information.
10. Client starts protected local service.
11. HotFolder is created.
12. Built-in local tools are registered.
13. Client reports ready/healthy.

For headless installation, support one-time enrollment tokens generated by an administrator.

## 22.11 Quickstart Authentication Flow

For single-user Quickstart:

```text
client install --server <quickstart-url>
       |
       v
Browser opens Quickstart login
       |
       v
admin logs in
       |
       v
Approve this device?
       |
       v
enrolled
```

No copy/paste token should be required in the normal desktop flow.

## 22.12 Client Tool Control

All client-provided managed tools must use the central decision model.

Invocation:

```text
AI
 |
 v
Local Client Tool
 |
 v
Container 1 authorization
 |
 +-- BLOCK -> do not execute
 |
 +-- ASK -> wait for approval
 |
 +-- ALLOW -> short-lived permit
                  |
                  v
             execute locally
```

In Quickstart, the integrated gateway performs the Container 1 role.

## 22.13 Quickstart Templates

Ship editable templates.

### Personal Safe Starter

Enabled automatically:

- HotFolder tools.
- calculator.
- text transformations.
- JSON validation.
- hashes.
- system info.

### Developer Starter

Available to enable/configure:

- Git.
- GitHub.
- GitLab.
- Docker client.
- local project filesystem.
- HTTP/OpenAPI.

Dangerous operations default to ASK/BLOCK.

### Operations Starter

- Kubernetes.
- SSH.
- Docker.
- Prometheus.
- Grafana API.

Requires explicit setup and credentials.

### Data Starter

- PostgreSQL.
- MySQL.
- REST/OpenAPI.

Read-only profiles suggested first.

### Collaboration Starter

- Jira.
- Slack.
- GitHub issues.
- generic webhook/API.

Templates define:

- Connector.
- required credential type.
- resource extractor.
- default risk.
- recommended policies.
- safe defaults.

## 22.14 Enterprise Setup Expectations

Enterprise mode requires intentional setup:

1. Deploy external PostgreSQL.
2. Deploy `mcp-access-control`.
3. Deploy one or more `mcp-access-gateway` replicas.
4. Configure DNS/TLS.
5. Configure OIDC/enterprise identity.
6. Configure external vault or choose supported encrypted store.
7. Optionally configure Redis/Valkey.
8. Optionally configure object storage.
9. Configure MCP/API tools.
10. Configure teams/policies.
11. Enroll endpoint clients.
12. Put gateway replicas behind load balancer.

No production enterprise deployment should silently use embedded Quickstart state.

## 22.15 Migration from Quickstart to Enterprise

Provide:

```text
Settings
  -> Export / Migration
  -> Migrate Quickstart to Stateless Deployment
```

Export:

- users/team configuration where applicable.
- MCP registrations.
- API tools.
- policies.
- templates.
- settings.
- secret metadata/references.

Credential migration wizard must move secrets securely between vault providers; ordinary JSON/YAML export never contains plaintext secrets.

## 22.16 Quickstart Acceptance Test

A completely fresh user must be able to:

1. Run one Docker container.
2. Log in.
3. See at least 10 enabled built-in tools.
4. Download/install endpoint client from the UI.
5. Enroll client using only server URL + browser login.
6. Verify HotFolder is created.
7. Ask an AI client to create a text file in HotFolder.
8. Read it back.
9. Observe the file event in audit/activity.
10. Change `hotfolder.write_text` from ALLOW to ASK.
11. Observe an approval request on next write.
12. Add a web-search credential into the built-in vault.
13. Enable `web.search`.
14. Build a simple API-backed MCP tool from the UI.
15. Export configuration to YAML/JSON with **no plaintext credentials**.

---

# 23. Admin UX Principles

## 23.1 Simple Mode by Default

Avoid exposing:

- protocol version.
- transport implementation.
- raw JSON.
- policy engine syntax.

## 23.2 Advanced Mode

Advanced users may access:

- Raw tool schema.
- MCP headers.
- JSON/YAML.
- policy source.
- bundle version.
- trace IDs.
- transport configuration.

## 23.3 Progressive Disclosure

Example:

```text
Add MCP Server

URL: [_________________]
Name: [_______________]

[Connect]
```

Only after failure or clicking Advanced:

```text
Transport
Headers
TLS
OAuth
Timeout
Proxy
```

## 23.4 Explain Decisions

For every blocked/ask decision:

```text
Why?

Blocked by:
  Policy: Engineering Production Protection

Matched:
  Team = Engineering
  Agent = Codex
  Environment = production
  Operation = DELETE
```

This is crucial for administrator trust.

---

# 24. MCP Routing

## 24.1 Virtual MCP Endpoints

Support:

```text
/mcp
/mcp/team/{team}
/mcp/profile/{profile}
/mcp/server/{server}
```

Exact public route names may change, but conceptually administrators can expose:

- All permitted tools.
- Team-specific tool set.
- Agent-specific profile.
- Specific server.

## 24.2 Tool Discovery

Return only tools the current identity can potentially use.

Do not reveal restricted tool metadata unnecessarily.

## 24.3 Deterministic Tool Names

Avoid collision:

```text
github.search_code
jira.search_issues
postgres.query
```

Support aliases.

## 24.4 Dynamic Tool Catalog

Changes to published tool catalog should propagate without restarting gateway replicas.

---

# 25. Fail-Safe and Degraded Modes

Security behavior during failures must be explicit.

## 25.1 Container 2 Down

Container 1 continues using last valid signed policy bundle.

Normal requests continue if bundle is valid.

Admin changes are unavailable.

## 25.2 Policy Bundle Expired

Configurable:

### Strict

- All protected actions fail closed.

### Tiered

- READ may continue for a grace period.
- WRITE/DESTRUCTIVE/PRIVILEGED fail closed.

Default production recommendation:

- Privileged/destructive fail closed.
- Low-risk reads may use a short configured grace period.

## 25.3 Container 1 Unreachable from Endpoint Client

Default:

```text
Protected tool execution = BLOCK
```

Optional explicitly configured offline policy is allowed only for low-risk tools.

Never silently fall back to unrestricted execution.

## 25.4 Identity Provider Down

- Existing unexpired access tokens may continue.
- New logins fail.
- Privileged JIT operations may be configured to require fresh identity.

## 25.5 Audit Sink Down

Options:

- Buffer bounded events.
- Continue low-risk.
- Fail closed for required-compliance actions if configured.

Never allow unbounded memory buffering.

## 25.6 Secret Store Down

Calls requiring uncached secret fail closed.

## 25.7 Approval Service Down

`ASK` behaves as `BLOCK`, not `ALLOW`.

---

# 26. Data Model

Core entities:

```text
Tenant
User
Group/Team
Role
IdentityProvider
Agent
Device
ClientEnrollment
McpServer
Tool
ToolVersion
ResourceType
ResourceExtractor
ApiConnector
ApiOperation
SecretReference
Policy
PolicyVersion
PolicyAssignment
ApprovalRule
ApprovalRequest
TemporaryGrant
PolicyBundle
GatewayInstance
EndpointClient
AuditEvent
Template
ConfigurationExport
```

All mutable entities should have:

```text
id
tenant_id
version
created_at
created_by
updated_at
updated_by
```

Use immutable historical records for security-relevant versions.

---

# 27. Control-Plane API

Recommended REST API prefixes:

```text
/api/v1/users
/api/v1/teams
/api/v1/agents
/api/v1/devices
/api/v1/mcp-servers
/api/v1/tools
/api/v1/api-tools
/api/v1/policies
/api/v1/approvals
/api/v1/audit
/api/v1/templates
/api/v1/import
/api/v1/export
/api/v1/clients
/api/v1/enrollments
/api/v1/bundles
/api/v1/settings
```

Requirements:

- OpenAPI documented.
- Consistent pagination.
- ETag/version-based optimistic concurrency.
- Idempotency keys for mutations where useful.
- CSRF protection for cookie-based admin UI.
- Fine-grained admin permissions.
- Audit every security-sensitive mutation.

---

# 28. Gateway API

Potential endpoints:

```text
/mcp
/v1/authorize
/v1/permits
/v1/health/live
/v1/health/ready
/v1/metrics
```

Internal/admin-only gateway endpoints must not be publicly exposed.

## 28.1 Endpoint Authorization Request

Example:

```json
{
  "userToken": "...",
  "agent": "codex",
  "device": "device-123",
  "tool": "ssh.restart_service",
  "arguments": {
    "host": "api-prod-01",
    "service": "api"
  },
  "requestHash": "..."
}
```

Response:

```json
{
  "decision": "ALLOW",
  "policyVersion": 1842,
  "permit": "<signed-token>",
  "expiresAt": "..."
}
```

For `ASK`:

```json
{
  "decision": "ASK",
  "approvalRequestId": "...",
  "expiresAt": "..."
}
```

---

# 29. Audit Model

Audit must be first-class.

## 29.1 Runtime Event

Capture:

- timestamp.
- tenant.
- user.
- team snapshot.
- agent.
- device.
- source.
- server.
- tool.
- operation.
- normalized resource.
- risk.
- decision.
- policy version.
- matched rules.
- approval ID.
- duration.
- upstream status.
- bytes in/out.
- trace ID.

Do not log secrets.

## 29.2 Admin Event

Capture:

- actor.
- changed entity.
- before/after hash.
- diff.
- timestamp.
- IP.
- trace ID.
- approval if required.

## 29.3 Redaction

Implement field-level redaction:

- password.
- token.
- authorization.
- cookies.
- API keys.
- private keys.
- configurable PII fields.

---

# 30. Observability

Both containers support:

- OpenTelemetry traces.
- Prometheus metrics.
- Structured JSON logs.
- Correlation/trace IDs.

## 30.1 Gateway Metrics

Examples:

```text
requests_total
requests_allowed_total
requests_blocked_total
requests_ask_total
policy_eval_duration_seconds
upstream_duration_seconds
active_requests
bundle_version
bundle_age_seconds
auth_failures_total
permit_issued_total
rate_limit_total
```

## 30.2 Control Plane Metrics

```text
login_total
policy_publish_total
policy_compile_failure_total
approval_pending
approval_duration
client_enrollment_total
gateway_instances_seen
bundle_publish_duration
```

---

# 31. Performance Targets

Initial engineering targets, to be validated by benchmarks:

## Container 1

- Added policy/gateway overhead p50: **< 2 ms** excluding network/TLS/upstream.
- Added policy/gateway overhead p99: **< 10 ms** under normal target load.
- Policy evaluation: **sub-millisecond typical**.
- Startup: **< 1 second target** for native gateway.
- Idle memory: **< 50 MB target**, lower preferred.
- Horizontal scale: near-linear until external dependency bottleneck.
- No database query for normal authorization.

These are targets, not marketing claims until benchmarks prove them.

## Container 2

Latency is less critical, but common UI API operations should feel immediate:

- Typical read p95 < 300 ms excluding external IdP.
- Policy preview p95 < 1 s for normal organization size.
- Large audit queries may be asynchronous/exported.

---

# 32. Security Threat Model

Threat actors:

- Unauthorized internet attacker.
- Malicious/compromised AI agent.
- Malicious MCP server.
- Compromised endpoint user process.
- Curious employee.
- Stolen endpoint device.
- Compromised admin account.
- Supply-chain compromise.
- Misconfiguration.
- Rogue internal service.
- Network attacker.

## 32.1 Key Threats and Controls

### Credential leakage to LLM

Controls:

- Credentials not put into tool descriptions/results.
- Gateway/client injects credentials out of band.
- Redaction.
- User-delegated auth.
- Secret references.

### Prompt injection causes destructive tool call

Controls:

- Deterministic authorization.
- Risk classification.
- `ASK`.
- Resource rules.
- Argument rules.
- No LLM authorization.

### Direct local bypass

Controls:

- Credential custody.
- Privileged service.
- OS-level policy.
- Gateway permit.
- Network restrictions.

### Malicious MCP returns secret-like data

Controls:

- Response filtering.
- DLP optional.
- Size limits.
- Content-type checks.
- audit redaction.

### Policy bundle tampering

Controls:

- Digital signature.
- hash.
- monotonic version.
- TLS.
- immutable storage.
- rollback protection.

### Gateway compromise

Controls:

- Minimal image.
- non-root.
- read-only filesystem.
- seccomp.
- no shell.
- least egress.
- secret minimization.
- short-lived credentials.

---

# 33. Container Hardening

## Container 1

Preferred:

- `scratch` or distroless.
- Non-root UID.
- Read-only root filesystem.
- Drop all Linux capabilities.
- No package manager.
- No shell.
- No compiler.
- No SSH.
- Minimal CA bundle.
- Memory/CPU limits.
- seccomp/AppArmor.
- Explicit egress.
- TLS 1.3 preferred.
- FIPS option for enterprise deployments if required.

## Container 2

- Non-root.
- Read-only filesystem except temp.
- External persistence.
- CSP/security headers.
- CSRF protection.
- Secure session cookies.
- strict upload parsing.
- configuration upload size limits.
- malware scanning hook for uploaded client/API artifacts where applicable.

---

# 34. Network Architecture

Recommended zones:

```text
Internet/User Network
      |
      v
Load Balancer / Reverse Proxy
      |
      +---- Container 1 gateway
      |
      +---- Container 2 admin (restricted/admin network preferred)

Container 1
  -> approved MCP upstreams
  -> secret broker
  -> bundle store
  -> audit sink

Container 2
  -> PostgreSQL
  -> object store
  -> identity provider
  -> notification bus
```

Admin UI should be separately restrictable from runtime MCP endpoints.

---

# 35. Monorepo Structure

Recommended:

```text
mcp-access/
|
+-- README.md
+-- LICENSE
+-- SECURITY.md
+-- CONTRIBUTING.md
+-- CODE_OF_CONDUCT.md
+-- CHANGELOG.md
+-- Makefile
+-- docker-compose.yml
+-- .env.example
+-- .editorconfig
+-- .github/
|   +-- workflows/
|
+-- apps/
|   |
|   +-- marketplace-drupal/        # optional public community Marketplace application
|   |   +-- composer.json
|   |   +-- web/modules/custom/
|   |   +-- tests/
|   |
|   +-- gateway/                    # Container 1
|   |   +-- Cargo.toml
|   |   +-- src/
|   |   +-- tests/
|   |   +-- Dockerfile
|   |
|   +-- control-plane/              # Container 2 Java backend
|   |   +-- pom.xml
|   |   +-- src/
|   |   +-- Dockerfile
|   |
|   +-- admin-ui/                   # built into Container 2
|   |   +-- package.json
|   |   +-- src/
|   |   +-- tests/
|   |
|   +-- endpoint-client/            # installable native client
|       +-- Cargo.toml
|       +-- src/
|       +-- platform/
|       |   +-- linux/
|       |   +-- windows/
|       |   +-- macos/
|       +-- installers/
|
+-- crates/                          # shared Rust libraries
|   +-- protocol/
|   +-- policy/
|   +-- permits/
|   +-- resource-extractors/
|   +-- client-common/
|
+-- packages/                        # shared language-neutral schemas
|   +-- schemas/
|   |   +-- config/
|   |   +-- policy/
|   |   +-- tool/
|   |   +-- permit/
|   +-- generated/
|
+-- backend-libs/
|   +-- config-model/
|   +-- import-export/
|   +-- api-builder/
|
+-- templates/
|   +-- developer-starter/
|   +-- operations-starter/
|   +-- data-starter/
|   +-- collaboration-starter/
|
+-- deploy/
|   +-- compose/
|   +-- kubernetes/
|   +-- helm/
|   +-- systemd/
|   +-- terraform/
|
+-- docs/
|   +-- architecture/
|   +-- security/
|   +-- api/
|   +-- admin/
|   +-- endpoint-client/
|   +-- deployment/
|   +-- operations/
|   +-- development/
|   +-- adr/
|
+-- tests/
|   +-- e2e/
|   +-- performance/
|   +-- security/
|   +-- interoperability/
|
+-- tools/
    +-- dev/
    +-- release/
    +-- benchmark/
```

Use generated schemas rather than hand-maintaining incompatible data models across Rust/Java/TypeScript.

---

# 36. Language-Neutral Contracts

Maintain canonical JSON Schema/OpenAPI definitions in:

```text
packages/schemas
```

Generate:

- Rust structures where practical.
- Java DTOs.
- TypeScript types.

Contracts include:

- Policy configuration.
- Signed bundle.
- Tool definition.
- Resource definition.
- Permit.
- Audit event.
- Import/export model.

CI fails if generated artifacts are out of date.

---

# 37. Database

Recommended production database:

**PostgreSQL**

Reasons:

- Transactions.
- JSONB.
- indexing.
- mature HA.
- migrations.
- broad operator familiarity.

Do not use the database in the gateway authorization hot path.

Potential partitions:

- `audit_event` partitioned by time.
- policy/version history.
- approval records.

Large audit deployments may export to:

- OpenSearch.
- ClickHouse.
- object storage.
- SIEM.

---

# 38. Caching / Messaging

Redis/Valkey is optional but useful for:

- strict permit replay prevention.
- global rate limiting.
- approval notifications.
- short-lived distributed cache.
- gateway heartbeat registry.

Do not make Redis mandatory for basic request authorization.

Notification bus options:

- NATS.
- Redis Streams.
- Kafka.
- PostgreSQL LISTEN/NOTIFY for smaller deployments.

Recommended starting point for simplicity:

- PostgreSQL + LISTEN/NOTIFY or periodic pull.
- Add NATS/Redis adapter for scale.

---

# 39. Configuration Layers

Order of precedence:

```text
compiled defaults
< environment variables
< external config file
< database/control-plane configuration
< emergency runtime deny policy
```

Security-sensitive environment options should be documented.

---

# 40. Templates and Defaults

Templates must never silently grant dangerous access.

Example GitHub template:

```text
Read repository           ALLOW candidate
Search code               ALLOW candidate
Create issue              ASK candidate
Create pull request       ASK candidate
Merge pull request        BLOCK candidate
Delete repository         BLOCK
```

Admin explicitly publishes chosen values.

---

# 41. Client Auto-Configuration

Later or V1 if feasible, Container 2 can show setup instructions or downloadable configuration for:

- Claude.
- Codex.
- Cursor.
- VS Code.
- OpenCode.
- Generic MCP client.

Avoid writing arbitrary user files remotely unless the endpoint client is enrolled and the administrator/user explicitly approves.

---

# 42. Backup and Disaster Recovery

Back up:

- PostgreSQL.
- object-store policy bundles.
- encrypted secret metadata.
- CA/signing keys using secure process.
- configuration exports.
- audit archive according to retention policy.

Recovery sequence:

1. Restore DB.
2. Restore key material securely.
3. Restore object store.
4. Start control plane.
5. Verify/republish signed bundle.
6. Start gateways.
7. Verify client trust.
8. Resume approvals.

---

# 43. Key Management

Separate keys for:

- Control-plane bundle signing.
- Gateway permit signing.
- Device CA/cert issuance.
- Secret-store envelope encryption.

Do not reuse one key for all purposes.

Support:

- Rotation.
- overlap window.
- key ID (`kid`).
- revocation.
- HSM/KMS later.

---

# 44. Upgrade Strategy

## Gateway

- Rolling deployment.
- Backward-compatible bundle formats for at least one previous minor version.
- Readiness only after valid bundle loaded.

## Control Plane

- Database migration before/with deployment.
- Backward compatibility with current gateway N and N-1 where practical.

## Endpoint Client

- Signed updater.
- minimum supported version policy.
- staged rollout.
- rollback.

---

# 45. Testing Strategy

## Unit

- policy matching.
- resource extraction.
- token/permit validation.
- import/export.
- API mapping.
- secret redaction.
- risk classification.

## Integration

- MCP upstream.
- OAuth/OIDC.
- PostgreSQL.
- Redis optional.
- API-generated tool.
- endpoint client.

## Security

- authorization bypass attempts.
- JWT confusion.
- malformed MCP.
- oversized payload.
- SSRF.
- path traversal.
- header injection.
- command injection.
- SQL parser ambiguity.
- replay.
- expired policy.
- stale device.
- compromised bundle.
- secret leakage.

## Performance

- gateway throughput.
- policy evaluation.
- 1/10/100/1000 tool catalogs.
- 10/100/1,000/10,000 policies.
- concurrent approvals.
- large response behavior.

## Chaos

- Container 2 down.
- DB down.
- bundle store down.
- IdP down.
- Redis down.
- gateway rolling restart.
- stale/expired bundle.
- network partition.

---

# 46. CI/CD

Required pipeline:

```text
format
lint
unit tests
schema validation
dependency scan
SAST
secret scan
SBOM
build
container scan
integration test
e2e test
performance smoke test
sign artifacts
publish
```

Recommended:

- Sigstore/cosign for container signing.
- SBOM in SPDX or CycloneDX.
- SLSA-aligned provenance where feasible.
- Renovate/Dependabot.
- Reproducible endpoint-client release where practical.

---

# 47. Open-Source Repository Requirements

Root:

- Clear README.
- 5-minute quickstart.
- Architecture diagram.
- Security model.
- Supported clients/servers.
- Threat model.
- Contribution guide.
- Public roadmap.
- Good first issues.
- Example configurations.
- Demo GIF/video.
- Security disclosure instructions.
- Apache-2.0 or another permissive license after legal review.

---

# 48. Quickstart Deployment

Illustrative only; exact environment names must be finalized during implementation.

```yaml
services:
  gateway:
    image: ghcr.io/example/mcp-access-gateway:latest
    ports:
      - "8081:8081"
    environment:
      CONTROL_BUNDLE_SOURCE: ${CONTROL_BUNDLE_SOURCE}
      GATEWAY_PUBLIC_URL: ${GATEWAY_PUBLIC_URL}

  control:
    image: ghcr.io/example/mcp-access-control:latest
    ports:
      - "8080:8080"
    environment:
      DATABASE_URL: ${DATABASE_URL}
      OIDC_ISSUER: ${OIDC_ISSUER}
```

Production mode assumes external persistence.

A separate `demo` profile may use ephemeral/local demonstration state and must be clearly labeled non-HA.

---

# 49. Kubernetes / HA Deployment

Container 1:

- Deployment.
- HPA.
- PodDisruptionBudget.
- readiness/liveness.
- multiple replicas.
- topology spread.
- no persistent volume.

Container 2:

- Deployment.
- 2+ replicas.
- no persistent volume.
- external Postgres.
- external object store.
- optional distributed cache.

Ingress routes:

```text
mcp.company.example      -> Container 1
mcp-admin.company.example -> Container 2
```

Admin domain should support stricter network/identity controls.

---

# 50. Availability Targets

Suggested targets after project maturity:

## Gateway

- Designed for 99.99% deployment architecture where infrastructure supports it.
- No control-plane dependency for normal requests.
- Multi-replica.

## Control Plane

- 99.9% is sufficient for many deployments because gateway remains operational from signed bundles.
- Approval-dependent operations may be unavailable if control plane is down.

---

# 51. Data Retention

Configurable separately:

- Runtime audit.
- Admin audit.
- approval events.
- request payload excerpts.
- metrics.
- client health.

Default to metadata-only audit rather than full sensitive request bodies.

---

# 52. Privacy

Provide:

- Payload logging off by default.
- Header redaction.
- Field redaction.
- Audit retention.
- Export/delete controls where required.
- Regional/self-hosted deployment.
- No mandatory telemetry to project maintainers.

Community edition should be fully usable with telemetry disabled.

---

# 53. Emergency Controls

Admin UI requires an obvious emergency page:

```text
Emergency Controls

[ Block all destructive tools ]
[ Block production environment ]
[ Disable selected MCP server ]
[ Revoke device ]
[ Revoke agent ]
[ Roll back policy ]
```

Emergency rules must propagate quickly and take precedence over ordinary policies.

Optional CLI/API emergency mechanism should exist in case UI is unavailable.

---

# 54. SSRF and API-to-MCP Security

The API builder creates a major SSRF surface.

Controls:

- Destination allowlist.
- deny link-local/metadata IPs by default.
- DNS rebinding protection.
- resolved-IP validation.
- redirect restrictions.
- private-network access disabled unless explicitly allowed.
- maximum redirects.
- HTTP scheme restrictions.
- TLS verification required by default.
- request/response size limits.
- header allowlist.
- no arbitrary `Host` override by default.

---

# 55. MCP Server Trust

Server states:

```text
UNVERIFIED
APPROVED
QUARANTINED
BLOCKED
```

Metadata:

- source.
- version.
- publisher.
- checksum/image digest.
- capabilities.
- tool list.
- risk.
- last scan.
- administrator approval.

Future:

- registry signature verification.
- vulnerability intelligence.
- behavior diff across upgrades.

---

# 56. Shadow MCP Discovery — Future Differentiator

Endpoint client may inventory known MCP configuration locations and report:

```text
Approved
Unknown
Blocked
```

Possible clients:

- Claude.
- Cursor.
- VS Code.
- Codex.
- OpenCode.
- others.

The feature must be privacy-sensitive and administrator-configurable.

It should not upload unrelated configuration content.

---

# 57. Policy-as-Code

UI remains primary for normal users.

Advanced:

```yaml
apiVersion: mcp-access.io/v1
kind: AccessPolicy

metadata:
  name: engineering-github

subject:
  teams:
    - engineering

target:
  server: github
  tool: create_pull_request
  resource:
    match: company/engineering/*

decision: ALLOW
```

Policy-as-code should round-trip with UI wherever possible.

---

# 58. API-Generated Tool Example

```yaml
apiVersion: mcp-access.io/v1
kind: ApiTool

metadata:
  name: restart-service

spec:
  displayName: Restart Service
  description: Restart a named application service.

  request:
    method: POST
    baseUrl: https://ops-api.internal.example
    path: /v1/environments/{environment}/services/{service}/restart

  auth:
    type: bearer
    secretRef: secret://ops-api

  inputs:
    - name: environment
      type: string
      required: true
      enum: [dev, stage, prod]
      location: path

    - name: service
      type: string
      required: true
      location: path

  resource:
    type: service
    idTemplate: "${environment}/${service}"
    operation: restart

  risk:
    classification: PRIVILEGED

  response:
    expose:
      - status
      - operationId
```

The admin UI generates this; users do not need to write it.

---

# 59. Example Access Policy

```yaml
apiVersion: mcp-access.io/v1
kind: AccessPolicy

metadata:
  name: engineering-restart

subject:
  teams: [engineering]

conditions:
  agent:
    trustLevel: managed

target:
  tool: restart-service
  resource:
    match: "prod/*"

decision:
  type: ASK
  approverGroup: production-operations
  approvalTtl: 10m
```

---

# 60. Endpoint Client Example

User asks agent:

```text
Restart api on production.
```

Flow:

1. AI calls local client MCP tool.
2. Client normalizes:
   - tool = `restart-service`
   - resource = `prod/api`
   - operation = `restart`
3. Client sends authorization request to Container 1.
4. Gateway evaluates.
5. Decision = `ASK`.
6. Admin/approver approves once.
7. Client retries/receives signed permit.
8. Privileged service verifies:
   - signature.
   - TTL.
   - device.
   - request hash.
   - resource.
9. Privileged executor calls remote ops API.
10. Audit correlates all stages with same trace ID.

---

# 61. Management Roles

Built-in roles should be minimal and understandable:

- Platform Owner.
- Security Admin.
- Tool Admin.
- Team Admin.
- Approver.
- Auditor.
- Read Only.

Support custom roles later.

Separation of duties:

- A Tool Admin may create a tool but not necessarily approve its own production policy.
- Auditor cannot mutate policy.
- Approver may approve requests but not change rules unless separately authorized.

---

# 62. Multi-Tenancy

Architecture should be tenant-aware from the beginning even if Community Edition exposes only one organization.

Every authoritative row/event must have tenant/organization scope.

Gateway must prevent cross-tenant cache and policy leakage.

---

# 63. Client Local Tool Registry

Endpoint client may manage local tools:

```text
tool id
executable
allowed subcommands
argument schema
working directory
environment allowlist
credential binding
resource extractor
risk
OS enforcement mode
```

Do not permit free-form shell execution in default templates.

If shell is enabled, make it an explicit high-risk feature.

---

# 64. Command Execution Security

For managed executable tools:

- Never concatenate untrusted strings into a shell command.
- Use argv arrays.
- Disable shell expansion by default.
- Fixed executable path.
- Validate file ownership/permissions.
- Hash or signature pin executable optionally.
- Sanitize environment.
- Set working directory.
- Timeout.
- output limit.
- child-process control.
- no inherited sensitive environment by default.

---

# 65. File Tool Security

If filesystem operations are supported:

- Explicit roots.
- Canonicalize paths.
- Prevent `..` escape.
- Resolve symlinks safely.
- Optional no-follow.
- size limits.
- file type controls.
- audit actual resolved path.

---

# 66. Database Tool Security

Prefer:

- Separate read-only/read-write DB identities.
- Native database permissions.
- Query parser.
- operation classification.
- row limits.
- statement timeout.
- transaction timeout.
- no multi-statement by default.
- no DDL by default.
- no unrestricted superuser credential.

---

# 67. Upstream Native Authorization

The gateway should preserve upstream authorization whenever possible.

Desired:

```text
User -> Gateway Policy -> User's OAuth -> Upstream Native ACL
```

Avoid:

```text
All Users -> One Superuser Token -> Upstream
```

Native upstream ACL is an additional security layer, not something the gateway should replace.

---

# 68. Compatibility

MCP support must follow current official specification releases and maintain a documented compatibility matrix.

At time of this specification, MCP 2026-07-28 provides a stateless protocol core and authorization hardening useful for this design.

Official MCP transport direction continues to center on:

- STDIO for local deployments.
- Streamable HTTP for remote deployments.

---

# 69. Backward Compatibility

Gateway adapters may support older MCP versions if needed, but:

- Internals use one normalized model.
- Legacy support is isolated.
- Security semantics follow the strongest supported behavior.
- Deprecations are documented.

---

# 70. Error Model

Do not expose sensitive internals.

Examples:

```text
AUTHENTICATION_REQUIRED
NOT_AUTHORIZED
APPROVAL_REQUIRED
POLICY_UNAVAILABLE
RESOURCE_NOT_ALLOWED
TOOL_UNAVAILABLE
UPSTREAM_TIMEOUT
UPSTREAM_ERROR
RATE_LIMITED
INVALID_ARGUMENT
DEVICE_NOT_TRUSTED
```

A trace ID should be returned for troubleshooting.

---

# 71. Admin Audit Example

```text
2026-09-17 18:42:01

Actor:
  admin@company

Change:
  Policy engineering-production-restart

Before:
  BLOCK

After:
  ASK

Approver group:
  production-operations

Bundle:
  v1841 -> v1842
```

---

# 72. Product Editions

## Community

Recommended open-source capabilities:

- Self-hosted.
- MCP gateway.
- Local/remote MCP registration.
- Basic OIDC.
- Teams.
- ALLOW/ASK/BLOCK.
- Resource policies.
- API-to-MCP builder.
- Basic approvals.
- Audit.
- Client enrollment/download.
- JSON/YAML import/export.
- Templates.
- PostgreSQL support.
- Endpoint client.
- No artificial tiny user limit.

## Enterprise / Commercial Extensions, if the project later monetizes

Possible:

- SCIM.
- SAML enterprise features.
- HA support packages.
- advanced SIEM.
- advanced DLP.
- HSM/KMS integration.
- compliance reports.
- advanced approval workflows.
- PAM integrations.
- policy analytics.
- posture management.
- multi-region.
- support/SLA.

The open-source edition should remain genuinely useful.

---

# 73. Phased Implementation

## Phase 0 — Architecture and Contracts

- Monorepo.
- schemas.
- threat model.
- ADRs.
- benchmark harness.
- CI.

## Phase 1 — Gateway MVP

- Stateless MCP proxy.
- signed config bundle.
- identity validation.
- tool-level ALLOW/BLOCK.
- audit.
- metrics.

## Phase 2 — Control Plane MVP

- Admin UI.
- users/teams.
- MCP registration.
- policy UI.
- bundle publish.
- audit viewer.
- import/export.

## Phase 3 — Resource Authorization

- resource extractor framework.
- GitHub example.
- API resource mapping.
- policy simulation.
- ASK/approval.

## Phase 4 — API-to-MCP

- simple form.
- test console.
- OpenAPI import.
- cURL import.
- publish.

## Phase 5 — Endpoint Client

- enrollment/download.
- mTLS/device identity.
- local MCP endpoint.
- per-call gateway permit.
- privileged service.
- credential custody.

## Phase 6 — Strong Endpoint Enforcement

- Linux enforcement.
- Windows enforcement.
- macOS enforcement.
- privileged remote operations.

## Phase 7 — HA/Enterprise Readiness

- distributed replay.
- external secrets.
- SCIM.
- SIEM.
- policy GitOps.
- advanced observability.

---

# 74. MVP Acceptance Criteria

The first meaningful public release is complete when:

1. Quickstart single-container mode and production two-container mode both deploy successfully.
2. Both application containers are stateless in production mode.
3. Multiple Container 1 replicas can run behind a load balancer.
4. Multiple Container 2 replicas can run against the same PostgreSQL database.
5. Admin can create a team.
6. Admin can register a remote MCP server.
7. Admin can assign tool access to a team.
8. MCP client sees only permitted tools.
9. Blocked request generates audit.
10. `ASK` request can be approved.
11. Admin can build one API-backed MCP tool through UI.
12. Admin can import OpenAPI.
13. Admin can export/import complete config in YAML/JSON.
14. Quickstart contains working safe example tools.
15. Container 2 exposes endpoint-client download.
16. Endpoint client enrolls.
17. Endpoint client requests authorization from Container 1 before a managed tool executes.
18. A privileged tool cannot access its protected credential without the privileged service.
19. Gateway keeps operating if Container 2 is shut down while its signed bundle remains valid.
20. Invalid/tampered policy bundle is rejected.
21. No plaintext secret appears in normal exports or audit logs.
22. Performance benchmark results are published.

---

# 75. Important Security Invariants

These should become automated tests.

1. **Default deny.**
2. A policy parse/evaluation error cannot become ALLOW.
3. `ASK` cannot become ALLOW when approval subsystem is unavailable.
4. Container 2 outage does not require Container 1 to bypass policy.
5. An expired privileged permit is rejected.
6. A permit for one request cannot authorize a different request hash.
7. A permit for Device A cannot be used by Device B.
8. A blocked resource remains blocked even if the tool itself is allowed.
9. Plain secrets never appear in standard config export.
10. Gateway never trusts an unsigned bundle.
11. Bundle rollback is detected unless explicitly authorized.
12. Endpoint privileged service never executes an unpermitted managed action.
13. API-to-MCP cannot access prohibited metadata/link-local destinations by default.
14. Unknown risk/tool/resource defaults conservatively.
15. Audit failure behavior is explicit and configurable, never silent.
16. Credential references never resolve into LLM-visible payloads.
17. Vault outage never causes fallback to plaintext/static credentials.

---

# 76. Decisions Requiring ADRs

Create Architecture Decision Records for:

- Gateway language: Rust vs Go vs Java native.
- Policy engine: Cedar vs OPA.
- Bundle distribution.
- Audit storage.
- Secret store abstraction.
- Permit token format.
- Device certificate lifecycle.
- Endpoint IPC mechanism.
- Linux enforcement method.
- Windows enforcement method.
- macOS enforcement method.
- API-to-MCP schema.
- MCP version compatibility.
- Demo persistence model.

---

# 77. Suggested Initial ADR Decisions

Unless benchmark or implementation findings disagree:

```text
ADR-001  Rust for data plane
ADR-002  Java 21 + Quarkus for control plane
ADR-003  React + TypeScript for admin UI
ADR-004  Rust for endpoint client
ADR-005  PostgreSQL authoritative metadata
ADR-006  Signed immutable policy bundles
ADR-007  No database access in gateway hot authorization path
ADR-008  OIDC primary human identity protocol
ADR-009  Default deny
ADR-010  User-delegated upstream identity preferred
ADR-011  API-to-MCP requires resource/risk review before publish
ADR-012  Plain secrets excluded from exports
ADR-013  Production containers stateless
```

Policy-engine ADR remains open until benchmark/prototype.

---

# 78. Developer Experience

Root commands should be simple:

```text
make dev
make test
make lint
make e2e
make benchmark
make quickstart
make containers
```

A contributor should not need to understand all components to work on one component.

Provide:

- Local mock IdP profile.
- Mock MCP server.
- Mock API.
- Seed users/teams.
- Sample policies.
- Sample endpoint client in non-privileged developer mode.

---

# 79. Documentation Set

Monorepo should include at least:

```text
docs/
  architecture/
    overview.md
    data-plane.md
    control-plane.md
    endpoint-client.md
    policy-engine.md
    api-to-mcp.md

  security/
    threat-model.md
    endpoint-enforcement.md
    secrets.md
    identity.md
    permits.md

  deployment/
    quickstart.md
    docker-compose.md
    kubernetes.md
    production-hardening.md

  admin/
    getting-started.md
    teams.md
    policies.md
    approvals.md
    api-builder.md
    clients.md
    import-export.md

  operations/
    backup-restore.md
    policy-recovery.md
    incident-response.md
    upgrade.md

  development/
    setup.md
    testing.md
    benchmarking.md
    release.md

  adr/
```

---

# 80. Public Positioning

Avoid:

> "Another MCP Gateway"

Prefer:

> **The easiest self-hosted way to control exactly what AI agents can do inside your organization.**

Supporting line:

> **Control access by user, team, agent, tool, action and resource — with approvals, endpoint enforcement, and a no-code API-to-MCP builder.**

Quickstart promise:

> **Deploy. Add your identity provider. Add tools. Create teams. Set ALLOW / ASK / BLOCK.**

---

# 81. Future Direction

Once the security model is proven for MCP, the same policy plane can expand to:

```text
MCP
REST
OpenAPI
CLI
A2A
local tools
internal APIs
automation agents
```

The long-term product can evolve from an MCP access gateway into a broader:

> **AI Action Authorization Control Plane**

The initial project should remain sharply focused on MCP team governance so it can be understood, deployed, and adopted quickly.

---

# 82. External Standards and References

The implementation team should track authoritative upstream specifications rather than copying protocol behavior from third-party gateways.

Important current references:

- Model Context Protocol specification and changelog.
- MCP 2026-07-28 release: stateless protocol core, header-based routing, authorization hardening, extensions.
- MCP Enterprise-Managed Authorization extension: stable as of June 2026.
- OAuth 2.1 / OIDC relevant standards.
- W3C Trace Context.
- OpenTelemetry.
- OWASP API Security guidance.
- Supply-chain/SBOM/signing standards.

Reference URLs:

- https://modelcontextprotocol.io/
- https://blog.modelcontextprotocol.io/posts/2026-07-28/
- https://blog.modelcontextprotocol.io/posts/enterprise-managed-auth/

Protocol implementation must be verified against the current official MCP specification at build/release time.

---

# 83. Final Architecture Principle

The architecture should preserve the following separation:

```text
CONTAINER 2
"What should be allowed?"
"What tools exist?"
"Who belongs to which team?"
"What configuration should be active?"
"What needs approval?"
"How do administrators manage the system?"

          |
          | signed/versioned configuration
          v

CONTAINER 1
"Is THIS request allowed RIGHT NOW?"
"Where should it be routed?"
"Should it require approval?"
"What resource is being touched?"
"Can a client receive an execution permit?"

          |
          | short-lived permit
          v

ENDPOINT CLIENT
"May I execute THIS exact privileged action?"
"Is the permit valid for this device/request/resource?"
"Can I execute without exposing the privileged credential?"
```

This division is the foundation for both **high performance** and **high assurance**.

Container 1 must stay small and fast.

Container 2 must make administration easy.

The endpoint client must turn policy into enforceable local/remote execution controls without pretending that a simple wrapper is a security boundary.

That combination is the core of the product.

---

# 84. Release Artifacts and Distribution Contract

Every tagged public release must be treated as an atomic product release.

## 84.1 Docker Images

Publish exactly these primary application images on every release:

```text
mcp-access-quickstart:<version>
mcp-access-gateway:<version>
mcp-access-control:<version>
```

Also publish immutable digest references.

Recommended registries:

- GitHub Container Registry.
- Optional Docker Hub mirror.

Tags:

```text
:<semver>
:<major>.<minor>
:stable
:latest      # only if project chooses conventional latest behavior
```

Never reuse a released semantic-version tag for different bytes.

## 84.2 Client Application GitHub Release Assets

The endpoint client must be published on the GitHub Release page for the same version.

Required target artifacts:

```text
mcp-access-client-windows-x86_64
mcp-access-client-windows-arm64
mcp-access-client-linux-x86_64
mcp-access-client-linux-arm64
mcp-access-client-macos-x86_64
mcp-access-client-macos-arm64
```

Package using native installer formats where practical:

```text
Windows: MSI and/or signed EXE
Linux:   tar.gz plus optional .deb/.rpm
macOS:   signed/notarized .pkg/.dmg
```

Each GitHub release must include:

- SHA-256 checksum manifest.
- signatures.
- SBOM.
- provenance/attestation.
- release notes.
- upgrade notes.
- compatibility matrix.
- container image digests.
- installer command examples.

## 84.3 Client Install Contract

The client executable/installer must require only a server URL for the normal interactive enrollment path.

Example:

```bash
mcp-access-client install --server https://mcp.example.com
```

The client performs discovery and browser-based authentication.

Optional flags:

```text
--server
--channel stable|beta
--hotfolder <path>
--headless
--enrollment-token <token>
--proxy <url>
--ca-cert <path>
```

Do not require ordinary users to manually copy:

- device certificates.
- client IDs.
- gateway URLs.
- policy URLs.
- secret tokens.

## 84.4 Server-Side Client Manifest

Container 2/Quickstart exposes a signed discovery manifest, for example:

```text
/.well-known/mcp-access-client
```

It may advertise:

```json
{
  "organization": "Example",
  "controlUrl": "https://mcp-admin.example.com",
  "gatewayUrl": "https://mcp.example.com",
  "enrollmentUrl": "https://mcp-admin.example.com/device",
  "minimumClientVersion": "1.2.0",
  "recommendedClientVersion": "1.3.1",
  "releaseChannel": "stable"
}
```

Exact format must be versioned.

## 84.5 In-Product Download Links

Container 2 and Quickstart must provide:

```text
Clients -> Download & Install
```

The server may proxy or redirect to official GitHub Release assets.

Prefer redirect to immutable signed release asset where possible rather than storing installer binaries inside the container image.

Support air-gapped deployments by allowing an administrator to mirror approved client artifacts to internal object storage.

## 84.6 Release Compatibility

Every release must publish a tested matrix:

```text
Gateway version
Control version
Quickstart version
Client versions supported
Config schema version
Bundle schema version
MCP versions supported
```

At minimum, client N should normally interoperate with server N and a documented adjacent-version window.

## 84.7 Release CI Gate

A version cannot be marked stable until CI proves:

1. All 3 containers build.
2. All 3 containers pass vulnerability scan policy.
3. All 3 containers are signed.
4. Client builds for all supported OS/architectures.
5. Client installers are signed where platform supports signing.
6. Quickstart fresh-start E2E passes.
7. Stateless 2-container HA E2E passes.
8. Client URL-enrollment E2E passes.
9. HotFolder tool E2E passes.
10. Vault secret-leak tests pass.
11. Config import/export E2E passes.
12. Upgrade compatibility tests pass.
13. SBOM/provenance generated.
14. Release artifacts and image digests are cross-linked.

---

# 85. Final Distribution Model

The product therefore ships in two operating profiles but four user-facing artifact families:

```text
QUICKSTART
  mcp-access-quickstart container

PRODUCTION / ENTERPRISE
  mcp-access-gateway container
  mcp-access-control container

ENDPOINT
  mcp-access-client native application
```

Every release publishes all of them together.

The intended onboarding paths are:

```text
PERSONAL / COMMUNITY

docker run mcp-access-quickstart
       |
       v
login
       |
       v
built-in tools ready
       |
       v
client install --server <URL>
```

and:

```text
ENTERPRISE

deploy external state
       |
       v
deploy control + gateway replicas
       |
       v
configure IdP + vault
       |
       v
create teams/policies
       |
       v
client install --server <URL>
```

This distinction must remain visible throughout documentation so the project remains extremely easy for a single user while preserving a clean stateless architecture for scaled deployments.

---

---

# 86. Community Tool Marketplace — Administrator-Controlled Registry and Software Distribution

The Community Marketplace is a **community publishing registry plus controlled software-distribution source**. Drupal provides the public/community website, while a separate stateless Java Marketplace API owns package business logic and a separate Marketplace Worker performs asynchronous untrusted package processing.

It is not an end-user app store.

Its primary organization-side workflow is:

> A community author publishes a reusable MCP/tool package. An organization administrator reviews and imports the package, configures permissions and credentials, assigns it to managed endpoint clients, and the endpoint-client system securely downloads, verifies, installs, starts, monitors, updates, and removes it.

The reverse workflow is equally important:

> A user/tool author can create a custom local tool inside Quickstart or the Control Plane, test it internally, package it, remove organization-specific secrets/settings, and submit/publish it to the Community Marketplace for others to use.

The trust chain is:

```text
COMMUNITY PUBLISHING

Tool Author
   |
   v
Local Tool Builder / Existing Package
   |
   v
"Publish to Marketplace"
   |
   v
Sanitize + Validate + Package
   |
   v
Drupal Marketplace
   |
   v
Moderation / Security Checks
   |
   v
Signed Immutable Community Release


ORGANIZATION CONSUMPTION

Drupal Marketplace
   |
   | signed immutable package
   v
Organization Administrator
   |
   | review + permissions + deployment scope
   v
Container 2 / Control Plane
   |
   | signed deployment assignment
   v
Managed Endpoint Clients
   |
   | download + verify + install + start
   v
READY
   |
   | every controlled execution
   v
Container 1 / Gateway Authorization
```

The Marketplace is never required for ordinary runtime authorization or tool execution.

If the Marketplace is unavailable:

- Already imported organization packages remain available.
- Already deployed tools remain installed.
- Endpoint clients keep using the locally approved package versions.
- Container 1 continues authorization from its signed policy bundle.
- Container 2 can still manage already imported organization packages.
- Only Marketplace browsing, new imports, update discovery, and publication are unavailable.

---

# 87. Marketplace Roles in the Overall Product

The Marketplace has three distinct responsibilities:

1. **Community catalog**
   - Discover integrations.
   - Compare versions.
   - View capabilities.
   - View runtime requirements.
   - View permissions/risk.
   - Read documentation.

2. **Publishing registry**
   - Accept community-authored tool packages.
   - Validate them.
   - Moderate/review them.
   - Version them immutably.
   - Sign approved releases.

3. **Package source**
   - Allow authorized organization administrators to import package definitions and executable artifacts into their own Control Plane.

The Marketplace does **not**:

- Decide organization permissions.
- Store organization credentials.
- Push packages directly to organization endpoints.
- Authorize runtime tool execution.
- Replace Container 1.
- Automatically activate a package inside an organization.

---

# 88. Organization-Side Marketplace Access Is Administrative

The organization-side Marketplace page must be an administrative function.

Recommended roles:

```text
Marketplace Viewer
Tool Author
Tool Administrator
Security Administrator
Deployment Administrator
Marketplace Publisher
Platform Owner
```

Recommended defaults:

```text
Ordinary User
  browse organization-approved tools: ALLOW
  import Marketplace package:         BLOCK
  deploy package:                     BLOCK
  publish package externally:         BLOCK unless Tool Author/Publisher role

Tool Author
  create local tool:                  ALLOW
  test local tool:                    ALLOW
  submit publication request:         ALLOW
  directly deploy to all devices:     BLOCK by default

Tool Administrator
  import Marketplace package:         ALLOW
  configure tool:                     ALLOW
  approve internal tool metadata:     ALLOW

Security Administrator
  review requested permissions:       ALLOW
  approve elevated/risky package:     ALLOW

Deployment Administrator
  assign package to devices/groups:   ALLOW
  pause/rollback rollout:             ALLOW

Marketplace Publisher
  push approved package to Marketplace: ALLOW

Platform Owner
  all operations:                     ALLOW
```

Quickstart single-user mode may combine all roles into the initial administrator.

---

# 89. Marketplace Package Types

The Marketplace must support multiple package types through one canonical package model.

## 89.1 MCP Schema Package

Contains no local executable.

May describe:

- Remote MCP server.
- Tool schemas.
- MCP connection metadata.
- Authentication requirements.
- Resource extractors.
- Risk metadata.
- Recommended policies.
- AI-facing descriptions.
- Setup parameters.

## 89.2 API-to-MCP Package

Contains declarative API integration:

- Base URL template.
- OpenAPI-derived operations.
- Manual REST operations.
- Input/output schema.
- Authentication requirements.
- Secret placeholders.
- Response mapping.
- Resource extraction.
- Risk classification.
- Recommended permissions.
- AI description.

## 89.3 Local Executable Tool Package

Contains an executable or script intended to run through the endpoint client.

Supported runtime types:

```text
native-binary
python
javascript-node
powershell
batch-cmd
shell
java-jar
dotnet
wasm                # future/preferred sandbox-friendly form
```

Examples:

```text
tool.exe
tool-linux-amd64
main.py
main.js
tool.ps1
tool.cmd
tool.bat
tool.sh
tool.jar
tool.dll
```

## 89.4 Local Schema-Only Package

No binary is uploaded.

It describes a tool expected to exist on endpoints:

```text
git
ffmpeg
docker
kubectl
terraform
internal-company-cli
```

Contains:

- discovery rules.
- version requirements.
- command schema.
- input schema.
- resource mapping.
- AI description.
- risk.
- allowed subcommands.
- safe execution restrictions.

## 89.5 Policy Template

Reusable permission configuration.

Installed only as draft.

## 89.6 Integration Bundle

May combine:

```text
MCP schema
API definitions
local tool artifacts
runtime definitions
resource extractors
AI descriptions
credential requirements
policy templates
documentation
```

This becomes the preferred package model for richer community integrations.

---

# 90. Canonical Package Identity

Every Marketplace package has:

```text
<publisher>/<package>@<version>
```

Examples:

```text
official/github@2.1.0
community/pdf-tools@1.4.2
rahul/log-parser@0.3.0
```

Reserved namespaces:

```text
official/
system/
security/
```

must not be self-assigned by community publishers.

Package versions are immutable.

---

# 91. Marketplace Package Manifest

Example local-tool package:

```yaml
apiVersion: marketplace.mcp-access.io/v1
kind: LocalToolPackage

metadata:
  publisher: community/example
  name: pdf-extractor
  displayName: PDF Extractor
  version: 1.3.0
  license: Apache-2.0

spec:
  tool:
    id: pdf.extract_text
    displayName: Extract PDF Text

    ai:
      description: >
        Extract textual content from a PDF located inside the managed
        HotFolder. Use this when the user needs the text content of a PDF.
      examples:
        - "Read invoice.pdf"
        - "Extract the text from report.pdf"

  runtime:
    type: python
    entrypoint: src/main.py

    python:
      minimumVersion: "3.11"
      dependencyLock: requirements.lock

  inputs:
    - name: input_file
      label: Input PDF
      aiDescription: PDF file inside the managed HotFolder.
      type: file
      required: true
      scope: hotfolder
      extensions:
        - .pdf

  outputs:
    - name: text
      type: string

  execution:
    timeoutSeconds: 60
    maxMemoryMb: 256
    maxOutputBytes: 10485760
    network: false
    workingDirectory: isolated
    childProcesses: false

  resources:
    type: local.file
    idTemplate: "${input_file}"
    operation: read

  risk:
    classification: READ

  permissions:
    filesystem:
      read:
        - hotfolder://**/*.pdf
      write: []

    network:
      allow: []

  artifacts:
    - path: src/main.py
      sha256: "..."
```

---

# 92. Artifact Package Layout

Recommended package layout:

```text
package/
  manifest.yaml
  tool.schema.json

  docs/
    README.md
    CHANGELOG.md

  artifacts/
    windows-x64/
    windows-arm64/
    linux-x64/
    linux-arm64/
    macos-x64/
    macos-arm64/

  source/
    # optional, strongly encouraged

  dependencies/
    # optional offline dependency bundles

  sbom/
    sbom.cdx.json

  signatures/
    metadata.json
```

Archive format should be deterministic.

Recommended:

```text
.tar.zst
```

Exact format should be defined in an ADR.

---

# 93. Supported Local Runtime Types

The endpoint client must provide a standard execution abstraction across supported runtime types.

## 93.1 Native Binary

Package specifies:

```text
OS
architecture
entrypoint
arguments
hash
signature metadata
library requirements
```

Preferred for:

- performance.
- no interpreter dependency.
- easy execution isolation.

## 93.2 Python

Preferred modes:

### Managed Python

Endpoint client installs/maintains a project-approved Python runtime.

Package defines:

```text
version constraint
entrypoint
locked dependencies
```

Client creates an isolated virtual environment.

### System Python

Disabled by default in enterprise deployments.

Admin may enable it explicitly.

Client validates:

- interpreter path.
- version.
- ownership.
- runtime policy.

## 93.3 JavaScript / Node.js

Managed Node runtime preferred.

Require:

- Node version constraint.
- lockfile.
- no global install.
- isolated package directory.
- lifecycle scripts disabled by default during install.
- no network dependency fetch at execution time.

## 93.4 PowerShell

Supported:

```text
.ps1
```

Requirements:

- PowerShell version.
- explicit execution entrypoint.
- declared modules.
- sanitized environment.
- no arbitrary PowerShell profile loading.
- higher review requirements for privileged commands.

## 93.5 Batch / CMD

Supported:

```text
.bat
.cmd
```

Because shell parsing is dangerous:

- package defaults to elevated risk classification.
- no untrusted direct string concatenation.
- strongly typed/validated inputs.
- prefer temporary structured input files where possible.
- explicit working directory.
- no inherited secrets.
- security review recommended.

## 93.6 Shell

Supported:

```text
.sh
```

Requirements:

- explicit shell/interpreter.
- no `eval`.
- validated input binding.
- no uncontrolled environment inheritance.
- sandbox/namespace where possible.

## 93.7 Java JAR

Defines:

```text
minimum Java version
entrypoint JAR
main class optional
memory limit
allowed JVM arguments
```

Prefer client-managed Java runtime for predictable operation.

## 93.8 .NET

Defines:

```text
runtime version
assembly
entrypoint
memory/time limits
```

## 93.9 WASM

Future preferred model for portable lower-risk community tooling when practical.

Benefits:

- constrained filesystem.
- constrained network.
- deterministic runtime.
- portable sandbox.

---

# 94. Standard Local Tool Invocation Contract

To make Python, JS, native binaries, Java, PowerShell, and other runtimes consistent, define one standard invocation protocol.

Recommended V1:

```text
stdin  = one JSON request envelope
stdout = one JSON result envelope
stderr = diagnostic logs
exit 0 = success
non-zero = failure
```

Example request:

```json
{
  "invocationVersion": 1,
  "invocationId": "01H...",
  "tool": "pdf.extract_text",

  "inputs": {
    "input_file": "invoice.pdf"
  },

  "context": {
    "hotFolder": "hotfolder://",
    "temporaryDirectory": "temp://"
  }
}
```

Example result:

```json
{
  "success": true,
  "result": {
    "text": "..."
  }
}
```

This protocol avoids building each runtime around arbitrary command-line interpolation.

---

# 95. Arguments and Input Binding

Preferred binding:

1. JSON stdin.
2. Structured file.
3. Environment variables for explicitly approved non-secret values.
4. Positional/flag argv only where needed.
5. Shell interpolation should be avoided.

Never form:

```text
shellCommand = "tool " + untrustedInput
```

The runtime should launch executable + argv directly where possible.

---

# 96. Tool AI Description Model

Every tool maintains separate description layers:

```text
marketplaceDescription
organizationDescription
effectiveDescription
```

Default:

```text
effectiveDescription = marketplaceDescription
```

If an administrator adds a domain-specific override:

```text
effectiveDescription = organizationDescription
```

Admin UI:

```text
AI Description

Marketplace default:
  Extract text from a PDF file.

Organization override:
  Use this tool for Finance invoice PDFs placed in Finance HotFolder.
  Prefer this before asking the employee to copy PDF text manually.

[Reset to Marketplace Default]
```

The effective description is what AI clients receive.

The Marketplace description is retained for update comparison.

---

# 97. AI Examples and Selection Hints

Marketplace package may provide:

```text
positive examples
negative examples
keywords
categories
tool-selection notes
```

Example:

```yaml
ai:
  description: Extract PDF text.
  useWhen:
    - user asks to read a PDF
    - text extraction is needed
  doNotUseWhen:
    - user asks to visually render pages
    - file is outside managed HotFolder
```

Organization admin can add local hints without altering package signature.

---

# 98. Tool Input Schema

Supported input types:

```text
string
integer
number
boolean
enum
file
directory
object
array
resource-reference
secret-reference         # internal only; not freely chosen by AI
```

Metadata:

```text
name
label
AI description
human description
required
default
enum
minimum
maximum
pattern
file extension
MIME constraints
HotFolder scope
resource mapping
sensitivity
```

---

# 99. Organization Overrides

An imported Marketplace package has two layers:

```text
Immutable Marketplace Package
              +
Local Organization Override
```

Admin may override:

- AI description.
- display name.
- enabled state.
- team/user eligibility.
- resource scope.
- ALLOW/ASK/BLOCK.
- runtime timeout.
- memory.
- output size.
- network destinations.
- HotFolder paths.
- credential binding.
- rollout assignment.
- update policy.
- environment.
- risk upward.

Admin should not silently weaken a Marketplace hard security constraint without explicitly creating a local fork/custom package.

---

# 100. Marketplace Supply-Chain Security

Executable Marketplace packages introduce a software-supply-chain risk.

Required controls:

1. Immutable versions.
2. Canonical package manifest.
3. Marketplace package signature.
4. Artifact hashes.
5. SBOM where applicable.
6. Malware scan.
7. static scan where applicable.
8. publisher identity.
9. source repository metadata where available.
10. license declaration.
11. network destination declaration.
12. filesystem access declaration.
13. child process declaration.
14. elevated privilege declaration.
15. dependency declaration.
16. package-diff review.
17. moderation state.
18. security contact.
19. security advisory/revocation capability.

Never use an ambiguous badge such as:

```text
SAFE
```

Use factual badges:

```text
Signature Verified
Source Available
Automated Scan Passed
Security Reviewed
Verified Publisher
```

---

# 101. Marketplace Publisher Workflow

Community website workflow:

```text
Register / Login
      |
      v
Become Publisher
      |
      v
Create Package
      |
      v
Choose Tool Type
      |
      v
Upload Schema / Artifact / Source
      |
      v
Define Runtime
      |
      v
Define Inputs / Outputs
      |
      v
Write AI Description
      |
      v
Declare Permissions
      |
      v
Declare Resource Mapping
      |
      v
Validate
      |
      v
Submit for Review
      |
      v
Moderation
      |
      v
Signed Immutable Release
```

---

# 102. Publishing a Tool from Quickstart or Control Plane

The product must have a first-class:

```text
Publish to Marketplace
```

action.

It applies to:

- Custom Local Tools.
- Imported/modified private tools where license permits.
- API-to-MCP tools.
- Local executable packages.
- Policy templates.
- Integration bundles.

Recommended UI location:

```text
Tools
  -> My Tools
       -> <tool>
            -> Publish
```

or:

```text
Marketplace
  -> Publish
```

---

# 103. Local-to-Marketplace Publication Flow

Flow:

```text
Select Local Tool
      |
      v
Publication Readiness Check
      |
      v
Choose Publisher Account
      |
      v
Choose Package Name / Version
      |
      v
Strip Local Secrets
      |
      v
Convert Organization Values into Install Settings
      |
      v
Review AI Description
      |
      v
Review Permissions / Runtime
      |
      v
Generate Package
      |
      v
Run Local Validation
      |
      v
Preview Public Package
      |
      v
Authenticate to Marketplace
      |
      v
Upload
      |
      v
Marketplace Validation
      |
      v
Moderation
      |
      v
Published
```

---

# 104. Publication Sanitization

Before upload, the Control Plane must scan and block organization-specific sensitive values.

Examples to remove/convert:

```text
API keys
passwords
tokens
private keys
OAuth refresh tokens
company internal URLs
private IP addresses
internal hostnames
employee email addresses
internal database names
local absolute filesystem paths
organization IDs
tenant IDs when not intended as defaults
```

The wizard offers conversion:

```text
Detected organization-specific URL:

https://jira.internal.example

Convert to installation setting?

Name: Jira Base URL
Type: URL
Required: Yes

[Convert]
```

Similarly:

```text
Detected secret reference:
secret://jira/company-token

Convert to credential requirement?

Type: Bearer Token
Name: Jira Credential

[Convert]
```

Secret values themselves are never uploaded.

---

# 105. Publication Readiness Report

Before upload:

```text
Marketplace Publication Check

Tool schema                PASS
AI description             PASS
Input schema               PASS
Runtime                    PASS
Artifact hashes            PASS
License                    PASS
Secret scan                PASS
Internal hostname scan     WARNING
Source repository          PRESENT
SBOM                       PRESENT
Declared network access    PASS
Resource mapping           PASS
Risk classification        PASS

[Fix Issues]
[Preview Package]
```

Critical failures block submission.

---

# 106. Publishing Permissions in Enterprise Mode

Enterprise admins may control who can publish externally.

Policy:

```text
External Marketplace Publication

Tool Authors:
  Create publication draft: ALLOW
  Submit for internal approval: ALLOW
  Upload directly: BLOCK

Marketplace Publishers:
  Upload approved package: ALLOW

Security Admin:
  Approve externally visible permission metadata: ALLOW
```

Optional organization workflow:

```text
Tool Author
   |
   v
Internal Publication Request
   |
   v
Security/Legal Review
   |
   v
Marketplace Publisher
   |
   v
Community Marketplace
```

Quickstart can allow the single administrator to publish directly.

---

# 107. Marketplace Account Linking

Control Plane:

```text
Marketplace

[Connect Community Marketplace]
```

Flow:

1. Control Plane generates OAuth state/PKCE.
2. Browser opens Drupal Marketplace.
3. User authenticates.
4. User authorizes requested scopes.
5. Callback reaches Control Plane.
6. Marketplace access token is stored in Credential Vault.
7. Publisher identity is displayed.

Scopes should separate:

```text
marketplace.read
marketplace.import
marketplace.publish
marketplace.manage-own
```

A read/import token must not grant publication.

---

# 108. Drupal Marketplace Technical Design

Implement the community Marketplace on the existing Drupal site through custom modules.

Suggested module layout:

```text
web/modules/custom/

  mcp_marketplace_core/
  mcp_marketplace_package/
  mcp_marketplace_builder/
  mcp_marketplace_publisher/
  mcp_marketplace_review/
  mcp_marketplace_security/
  mcp_marketplace_api/
  mcp_marketplace_signing/
  mcp_marketplace_metrics/
  mcp_marketplace_advisory/
```

Use Drupal entities/revisions for:

- package metadata.
- publisher records.
- package versions.
- moderation state.
- reviews.
- advisory metadata.

Store large immutable package artifacts in object storage/CDN, not normal Drupal database blobs.

---

# 109. Marketplace Roles

Drupal roles:

```text
Anonymous Visitor
Community User
Publisher
Package Maintainer
Verified Publisher
Marketplace Reviewer
Security Reviewer
Marketplace Administrator
```

---

# 110. Marketplace Package Moderation

States:

```text
DRAFT
VALIDATING
IN_REVIEW
CHANGES_REQUESTED
APPROVED
PUBLISHED
DEPRECATED
QUARANTINED
REVOKED
```

Only approved/published versions can be imported by default.

Enterprise admin may optionally allow unpublished/private registries, but not the public production channel.

---

# 111. Marketplace Automated Validation

For every submission:

- JSON/YAML schema validation.
- manifest validation.
- artifact hash verification.
- secret scanning.
- internal-domain/private-IP scanning.
- malware scanning.
- package size limits.
- executable-type verification.
- permission consistency.
- resource mapping check.
- AI description presence.
- risk declaration.
- runtime compatibility.
- dependency declaration.
- external destination declaration.
- license presence.
- package diff from prior version.
- prohibited file type checks.
- archive traversal checks.
- zip/tar bomb checks.

Drupal itself must not execute uploaded community binaries/scripts during normal validation.

Dynamic execution testing, if introduced, must happen in an isolated disposable sandbox service.

---

# 112. Marketplace Package Signing

Marketplace signs each approved immutable package/version.

Signature covers:

- canonical manifest hash.
- artifact digest list.
- package identity.
- version.
- publisher identity.
- publication timestamp.

Signing private key:

- separate from ordinary Drupal config.
- preferably KMS/HSM protected.
- rotatable.
- uses `kid`.
- supports trust rollover.

---

# 113. Administrator Marketplace Browse Page

Organization Control Plane:

```text
Marketplace

Search: [________________]

Filters:
  Type
  Runtime
  Risk
  OS
  Verified Publisher
  Security Reviewed
  License

Results:
  PDF Extractor
  Git Tools
  FFmpeg Utilities
  Internal API Template
```

Ordinary users do not see this organization installation page unless explicitly permitted.

---

# 114. Package Detail Page in Control Plane

Show:

```text
Name
Publisher
Version
Trust status
License
Source

Package type
Runtime
Supported OS/architectures

AI tools
Inputs/outputs
Requested permissions
Network destinations
Filesystem scopes
Credential requirements
Elevated privilege requirements

Risk classification
Resource model

Organization install status
Existing version
Available update
```

---

# 115. Administrator Import Workflow

```text
Select Marketplace Package
        |
        v
Download Manifest
        |
        v
Verify Marketplace Signature
        |
        v
Download Artifacts
        |
        v
Verify Artifact Hashes
        |
        v
Organization Security Scan
        |
        v
Show Import Diff
        |
        v
Import as DRAFT
```

Import does not deploy.

---

# 116. Organization Artifact Mirror

Enterprise mode should copy administrator-approved Marketplace artifacts to organization-controlled storage.

Recommended:

- S3-compatible object store.
- internal artifact repository.
- internal CDN.

Benefits:

- Marketplace outage independence.
- deterministic rollout.
- internal retention.
- faster client deployment.
- organization-controlled lifecycle.

Quickstart caches artifacts under its persistent `/data` volume.

---

# 117. Organization Configuration after Import

Administrator configures:

```text
Enabled tools
AI description override
Credentials
Resource scope
Network access
Filesystem access
Run-as identity
ALLOW / ASK / BLOCK
Device targets
User/team targets
Runtime limits
Version pin
Update policy
```

Only after configuration does the package become deployable.

---

# 118. Device and Client Groups

Support groups:

```text
All Managed Clients
Engineering Laptops
Finance Laptops
Windows Workstations
Linux Workstations
macOS Workstations
Build Servers
Production Jump Hosts
Canary Devices
```

Membership can derive from:

- static selection.
- user team.
- OS.
- architecture.
- tag.
- endpoint-client version.
- device posture.
- site/location label.
- custom query later.

---

# 119. Deployment Assignment

Example:

```yaml
package: community/pdf-tools
version: 1.3.0

targets:
  deviceGroups:
    - finance-laptops

rollout:
  strategy: canary
  canaryPercent: 5
  stages:
    - 5
    - 25
    - 50
    - 100

policy:
  filesystem: hotfolder-only
  network: block
```

Deployment assignment is signed/versioned.

---

# 120. Client Distribution Flow

```text
Container 2
     |
     | signed deployment manifest
     v
Endpoint Client
     |
     | artifact request
     v
Organization Artifact Store
     |
     v
download
     |
     v
verify
     |
     v
install
     |
     v
health check
     |
     v
READY
```

Container 2 should not become a large binary streaming bottleneck in stateless enterprise mode.

Use short-lived signed download URLs or authenticated artifact service access.

---

# 121. Client Deployment State Machine

Canonical per-package/per-device state:

```text
NOT_ASSIGNED

QUEUED
ASSIGNMENT_RECEIVED

DOWNLOADING
DOWNLOADED

VERIFYING
VERIFIED

PREPARING_RUNTIME
INSTALLING
INSTALLED

STARTING
HEALTH_CHECKING
READY

FAILED_DOWNLOAD
FAILED_VERIFY
FAILED_RUNTIME
FAILED_INSTALL
FAILED_START
FAILED_HEALTH

INCOMPATIBLE
BLOCKED
QUARANTINED

UPDATE_AVAILABLE
UPDATING

ROLLING_BACK
ROLLED_BACK

REMOVING
REMOVED
```

Offline is tracked separately from installation state.

---

# 122. Admin Deployment Dashboard

Example:

```text
PDF Extractor 1.3.0

Assigned clients:      142

READY                   130
DOWNLOADING               3
INSTALLING                 4
FAILED_INSTALL             2
INCOMPATIBLE               1
OFFLINE                    2

Deployment: 91.5%

[Pause Rollout]
[Resume]
[Rollback]
[View Failures]
```

---

# 123. Per-Client Detail

Show:

```text
Device
User
Team
OS
Architecture
Client version

Package
Assigned version
Installed version
Desired version

Deployment state
Last state transition
Last error
Health state

Policy version
Last execution
Last check-in
```

---

# 124. Client Heartbeat and Inventory

Endpoint client periodically reports safe inventory:

```text
device identity
client version
OS/architecture
installed package IDs
installed package versions
runtime inventory
package health
deployment state
HotFolder readiness
current policy generation
last tool health check
```

Never report user file contents as inventory.

---

# 125. Rollout Strategies

Support:

```text
Immediate
Canary
Percentage staged
Device-group staged
Scheduled
Manual batches
```

Automatic promotion between stages can require:

```text
verification failures == 0
install failure rate below threshold
health failure rate below threshold
manual approval optional
```

---

# 126. Rollback

Clients keep at least the previous approved version during an update grace period.

Recommended install layout:

```text
<MCPAccessData>/
  tools/
    <publisher>/
      <package>/
        1.2.0/
        1.3.0/
        current -> 1.3.0
```

Rollback:

```text
stop new version
switch current pointer
start old version
health check
report ROLLED_BACK
```

---

# 127. Managed Runtime Inventory

Endpoint client manages optional runtimes:

```text
Python
Node.js
Java
PowerShell
.NET
WASM runtime
```

Admin policy controls:

```text
allow managed runtime download
allowed runtime versions
allowed runtime sources
system runtime allowed yes/no
```

Runtime binaries themselves must be signed/hash-pinned.

---

# 128. Python Package Installation

Requirements:

- lock dependencies.
- hash dependencies.
- isolate environment.
- no `pip install latest` during execution.
- prefer offline wheel bundle.
- dependency source allowlist.
- disable network after install unless tool requires it.

---

# 129. JavaScript Package Installation

Requirements:

- lockfile.
- isolated install.
- lifecycle scripts disabled by default.
- no global packages.
- no arbitrary postinstall.
- prefer bundled production dependency archive.
- network disabled at runtime unless declared.

---

# 130. Java Tool Installation

Options:

```text
managed JRE
system JRE
self-contained runtime image
```

Preferred enterprise mode:

- managed JRE or self-contained runtime.
- version pinned.
- JVM args controlled by manifest/admin.

---

# 131. Native Binary Tool Installation

Client verifies:

- package signature.
- artifact hash.
- OS.
- architecture.
- organization assignment.
- executable permissions.
- optional OS code signature.

Never execute from temp/download folder.

---

# 132. PowerShell / Batch / Shell Installation

Scripts are stored under immutable versioned tool directories.

Execution environment must:

- sanitize environment.
- restrict working directory.
- avoid inherited secrets.
- enforce timeout.
- enforce output limit.
- bind inputs safely.
- block arbitrary shell evaluation.

PowerShell/Batch/Shell packages should receive stricter default risk classification than structured native/WASM tools.

---

# 133. Tool Runtime Isolation

Depending on OS and configuration, endpoint client should support:

```text
process identity isolation
filesystem scope
working-directory isolation
network egress restriction
memory limit
CPU limit
timeout
child-process restriction
temporary directory
credential injection boundary
```

Runtime policy is controlled by organization override.

---

# 134. Credential Injection into Local Tools

Local tool credentials follow the vault architecture.

Flow:

```text
AI request
   |
   v
Endpoint Client
   |
   v
Container 1 authorize
   |
   v
short-lived permit
   |
   v
credential broker / endpoint secure store
   |
   v
tool process
```

Preferred credential exposure:

1. file descriptor/pipe.
2. OS credential API.
3. short-lived protected temp file.
4. environment variable only when tool requires it.

Never put secret value into:

- Marketplace manifest.
- AI description.
- normal tool arguments.
- audit log.

---

# 135. Runtime Authorization Still Goes Through Container 1

Installation approval does not imply unlimited execution.

Every controlled tool execution still follows:

```text
AI
 |
 v
Endpoint Client
 |
 v
Container 1
 |
 +---- BLOCK
 |
 +---- ASK
 |
 +---- ALLOW + short-lived permit
               |
               v
        Local Managed Runtime
```

This remains true for Marketplace-installed tools and locally authored tools.

---

# 136. Admin Custom Local Tool Builder

Container 2 and Quickstart must provide a first-class **Custom Local Tool Builder**.

Navigation:

```text
Tools
  -> Local Tools
       -> Create Tool
```

Creation options:

```text
[Write Code]
[Upload File]
[Use Existing Executable]
[Import Marketplace Package]
```

---

# 137. Custom Tool Builder — Write Code

Admin/tool author can create a tool by writing code directly in a browser code editor.

Fields:

```text
Tool ID
Display Name
AI Description
Runtime Type
Entry Point
Inputs
Outputs
Resource Mapping
Risk
Permissions
Runtime Limits
Target OS
```

Runtime choices:

```text
Python
JavaScript
PowerShell
Batch
Shell
Java
Other supported runtime
```

The code editor should provide:

- syntax highlighting.
- line numbers.
- search.
- format action where available.
- lint action.
- validation errors.
- no secret values inserted automatically.

---

# 138. Custom Tool Input Builder

Below the code editor:

```text
Inputs

+ Add Input
```

For each input:

```text
Name
Label
AI Description
Type
Required
Default
Allowed values
Validation
File scope
Resource mapping
```

Example:

```text
Name: filename
Type: file
Required: yes
Scope: HotFolder
Extensions: .csv
AI description:
  CSV file inside HotFolder to analyze.
```

The generated tool schema is visible in Advanced mode.

---

# 139. Custom Tool Output Builder

Fields:

```text
Name
Type
AI description
Maximum size
Sensitive yes/no
```

The runtime protocol remains JSON stdin/stdout.

---

# 140. Custom Tool AI Description

Author enters:

```text
What should the AI know about this tool?
```

Example:

```text
Use this tool to inspect CSV files generated by the finance export
process. It calculates totals and validation warnings. Only use files
inside HotFolder/Finance.
```

Optional:

```text
Use when
Do not use when
Examples
Keywords
```

This description is what AI sees after organization policy filtering.

---

# 141. Custom Tool Runtime Permissions

Author proposes; administrator approves:

```text
Filesystem
  read
  write

Network
  allowed hosts

Processes
  may spawn children

Credentials
  required credential references/types

Privilege
  normal
  elevated

HotFolder
  allowed subfolders

Timeout
Memory
Output limit
```

Default permissions should be restrictive.

---

# 142. Custom Tool Test Sandbox

Before deployment:

```text
[Test Tool]
```

The test environment should:

- use sample inputs.
- use a temporary isolated working directory.
- block network unless explicitly allowed.
- limit CPU/memory/time.
- capture stdout/stderr.
- display structured result.
- show detected resource.
- show requested permissions.
- show Container 1 simulated decision.

Do not automatically test unknown code with elevated privileges.

---

# 143. Custom Tool Versioning

Every save/publish creates versions:

```text
draft
0.1.0
0.1.1
1.0.0
```

Organization tool versions are immutable after being assigned to clients.

Edits create a new version.

---

# 144. Custom Tool Internal Deployment

After test:

```text
Save Draft
      |
      v
Security Review if required
      |
      v
Create Organization Package
      |
      v
Assign Device Groups
      |
      v
Rollout
```

Internal package goes through the same endpoint deployment system as Marketplace packages.

This guarantees one installation/runtime/status architecture.

---

# 145. Custom Tool Publish-to-Marketplace Button

Once a custom tool is valid:

```text
[Deploy Internally]
[Publish to Marketplace]
```

`Publish to Marketplace` launches the sanitization/publication workflow defined above.

A tool does not need to be deployed internally before publication, but internal testing should be strongly recommended.

---

# 146. Existing Local Tool → Marketplace

Admin/tool author may start with an existing tool:

```text
mytool.exe
mytool.py
mytool.js
script.ps1
tool.jar
```

Flow:

```text
Create Tool
   |
   v
Upload Artifact
   |
   v
Select Runtime
   |
   v
Define Inputs/Outputs
   |
   v
Write AI Description
   |
   v
Define Permissions
   |
   v
Test
   |
   +--> Deploy Internally
   |
   +--> Publish to Marketplace
```

---

# 147. Marketplace Pull and Deploy Flow — End to End

```text
ADMIN
  |
  v
Browse Marketplace
  |
  v
Select package
  |
  v
Verify signed package
  |
  v
Import as organization draft
  |
  v
Configure:
  - credentials
  - AI description override
  - resource scope
  - permissions
  - users/teams
  - device groups
  - runtime limits
  |
  v
Security approval
  |
  v
Create rollout
  |
  v
Control Plane publishes signed deployment assignment
  |
  v
Clients receive desired state
  |
  v
Clients download organization-cached artifacts
  |
  v
Verify
  |
  v
Install
  |
  v
Start / health check
  |
  v
READY
  |
  v
Admin sees status
```

---

# 148. Marketplace Push Flow — End to End

```text
TOOL AUTHOR
  |
  v
Create / Import Tool
  |
  v
Test Locally
  |
  v
Publish to Marketplace
  |
  v
Sanitize organization data
  |
  v
Convert credentials to requirements
  |
  v
Convert tenant-specific values to setup fields
  |
  v
Generate canonical package
  |
  v
Sign/upload submission
  |
  v
Drupal validation
  |
  v
Marketplace moderation
  |
  v
Marketplace signed release
  |
  v
Available to other organization administrators
```

---

# 149. Package Update Workflow

Marketplace discovers newer version:

```text
Installed: 1.3.0
Available: 1.4.0
```

Control Plane computes semantic diff:

```text
Tools added              2
Tools removed            0
Inputs changed           1
New filesystem write     YES
New external host        api.example.net
Risk increased           1 tool
Credential change        none
```

Admin decides:

```text
Ignore
Review
Import
Deploy
```

---

# 150. Update Safety Rules

Never automatically activate an update when:

- new destructive tool appears.
- risk classification increases.
- filesystem scope expands.
- network scope expands.
- new credential requirement appears.
- new elevated privilege appears.
- new external domain appears.
- executable runtime type changes.
- package signing trust changes.
- resource matcher becomes broader.

Default update mode:

```text
Notify only
```

Optional:

```text
Auto-import patch updates
```

but deployment still follows local policy.

---

# 151. Marketplace Security Advisories

Marketplace may issue signed advisories:

```text
INFO
WARNING
HIGH
CRITICAL
REVOKED
```

Local Control Plane displays affected installations.

Organization options:

```text
Notify only
Block new installs
Pause tool
Disable affected version
Force rollout of fixed version after approval
```

Runtime gateway must not synchronously call Marketplace to check advisories.

---

# 152. Marketplace Package Revocation

Revoked package behavior:

- Not offered to new installs by default.
- Existing organization gets alert.
- Control Plane can quarantine.
- Endpoint client can be instructed to stop/remove affected version.
- Admin retains audit of decision.

---

# 153. Marketplace Reviews and Popularity

Community features may include:

```text
ratings
reviews
install count
favorites
recently updated
verified publisher
source available
```

Popularity is not security.

Security metadata must be visually distinct from stars/downloads.

---

# 154. Marketplace Drupal Data Model

Recommended Drupal entities/content structures:

```text
Publisher
Package
PackageVersion
PackageArtifact
PackageRuntime
PackageTool
CredentialRequirement
PermissionDeclaration
ResourceDeclaration
SecurityReview
PackageAdvisory
Category
Review
```

Large binaries live in object storage.

Drupal stores references/metadata.

---

# 155. Marketplace API

Versioned API:

```text
/api/marketplace/v1/search
/api/marketplace/v1/packages
/api/marketplace/v1/packages/{publisher}/{package}
/api/marketplace/v1/packages/{publisher}/{package}/versions
/api/marketplace/v1/packages/{publisher}/{package}/versions/{version}
/api/marketplace/v1/advisories
/api/marketplace/v1/me

/api/marketplace/v1/publish
/api/marketplace/v1/publish/{submission}
/api/marketplace/v1/publish/{submission}/artifacts
```

Publication endpoints require scoped Marketplace OAuth.

---

# 156. Marketplace Authentication Separation

Three credential/security domains remain distinct:

```text
1. Marketplace account authentication
2. Organization Control Plane authentication
3. Tool/upstream credentials
```

Never reuse one for another.

Marketplace token is stored in organization Credential Vault:

```text
secret://marketplace/user/<id>
```

but it can only be used for Marketplace API scopes.

---

# 157. Private Marketplace/Registry Future

The same package protocol should support:

```text
Public Community Marketplace
Private Company Marketplace
Partner Marketplace
Air-Gapped Registry
```

Control Plane registry configuration:

```text
name
URL
trust keys
authentication
allowed publishers
priority
update policy
```

---

# 158. Marketplace Package Mirroring

Enterprise may configure:

```text
Pull from public Marketplace
      |
      v
Security review
      |
      v
Mirror approved package into private registry
      |
      v
Production Control Plane installs only from private registry
```

Useful for regulated organizations.

---

# 159. Air-Gapped Import

Admin can download:

```text
signed package archive
signature bundle
SBOM
advisory metadata
```

transfer offline and import into isolated Control Plane.

Local verification must use configured trusted Marketplace public keys.

---

# 160. Client Offline Behavior

If a client is offline during rollout:

```text
desired version = 1.4.0
installed = 1.3.0
status = OFFLINE / OUTDATED
```

When reconnecting:

1. Fetch signed desired-state manifest.
2. Download approved artifact.
3. Verify.
4. Install.
5. Report state.
6. Do not skip organization security policy.

---

# 161. Client Failure Reporting

Installation error should be structured:

```text
error code
phase
runtime
package
version
OS/architecture
safe diagnostic
timestamp
```

Examples:

```text
ARTIFACT_HASH_MISMATCH
RUNTIME_NOT_AVAILABLE
DEPENDENCY_INSTALL_FAILED
SIGNATURE_INVALID
PACKAGE_INCOMPATIBLE
ENTRYPOINT_NOT_FOUND
HEALTH_CHECK_FAILED
POLICY_BLOCKED
```

No secrets in error reports.

---

# 162. Tool Health Checks

Package may declare:

```text
startup health check
version check
self-test
```

Health checks must be non-destructive.

Control Plane status:

```text
Installed
Running
Ready
Degraded
Failed
```

---

# 163. Runtime Logs

Endpoint client captures bounded tool logs.

Admin access:

```text
Device -> Tool -> Logs
```

Controls:

- max size.
- retention.
- secret redaction.
- optional disabled by policy.
- download requires privilege.
- ordinary AI clients cannot retrieve unrestricted runtime logs unless a specific tool exposes safe logs.

---

# 164. Tool Uninstall

Admin:

```text
Remove package
```

Flow:

```text
desired state removes package
   |
   v
client stops runtime
   |
   v
remove package version
   |
   v
remove tool-specific temporary state
   |
   v
retain audit
   |
   v
REMOVED
```

Persistent tool data must be declared and handled separately.

---

# 165. Tool Data Ownership

Package manifest must declare writable state:

```text
ephemeral
cache
persistent
HotFolder
```

Uninstall UI warns before deleting persistent data.

Marketplace tool cannot silently write outside declared scope.

---

# 166. File Distribution Security

All client downloads require:

- TLS.
- short-lived URL or authenticated request.
- hash verification.
- signature verification.
- maximum package size.
- content-type verification.
- archive traversal protection.
- decompression-bomb limits.

---

# 167. Organization Package Trust States

Imported package can be:

```text
IMPORTED
UNDER_REVIEW
APPROVED
DEPLOYABLE
BLOCKED
QUARANTINED
DEPRECATED
```

Only `DEPLOYABLE` packages may be assigned to normal device groups.

---

# 168. Organization-Specific Tool Fork

Admin can:

```text
Clone as Custom Tool
```

This creates a local organization package.

Reasons:

- change code.
- add domain behavior.
- replace runtime.
- harden permissions.
- customize resource extraction.

Fork loses automatic Marketplace package identity unless explicitly linked as derivative.

It may later be published as a new Marketplace package if license permits.

---

# 169. Marketplace License Handling

Publisher must declare license.

If user attempts:

```text
Publish Local Fork to Marketplace
```

Control Plane displays original license metadata.

The product must not claim legal permission.

It should warn when license metadata indicates redistribution restrictions or is missing.

---

# 170. Marketplace Publication from Organization Fork

If permitted:

```text
Fork
  |
  v
Modify
  |
  v
Test
  |
  v
Publish to Marketplace
  |
  v
Choose new package namespace
```

Publication wizard includes attribution/source fields.

---

# 171. Custom Tool Source vs Binary Publishing

Publisher chooses:

```text
Source only
Binary only
Source + binary
Schema only
```

Marketplace can require stricter review for:

```text
Binary only
```

`Source + binary` is preferred for community trust.

---

# 172. Build Service — Future

Future Marketplace may provide reproducible builds:

```text
Source package
   |
   v
Marketplace build sandbox
   |
   v
signed binaries
```

This is not required for initial release.

Until then, publisher-provided binaries are clearly marked.

---

# 173. Marketplace Security Review Levels

Possible factual labels:

```text
Declarative Schema Checked
Source Scanned
Binary Scanned
Manual Review Completed
Reproducible Build Verified   # future
Official Package
```

Do not merge these into one vague rating.

---

# 174. Admin Tool Catalog

Container 2 should unify all tool origins:

```text
Built-In
Marketplace
Custom Local
API-to-MCP
Remote MCP
Existing Local Executable
```

Tool list columns:

```text
Tool
Origin
Package
Version
Runtime
Risk
Deployment
Ready Clients
Policy
Update
```

---

# 175. Tool Origin Metadata

Every tool retains:

```text
origin.type
origin.package
origin.publisher
origin.version
origin.marketplaceURL
origin.sourceRepository
origin.localFork
```

Useful for audit and updates.

---

# 176. Admin Tool Status Summary

Example:

```text
PDF Extractor
Origin: Marketplace
Version: 1.3.0

Deployment
  Assigned: 142
  Ready:    130
  Failed:     2
  Offline:    2
  Pending:    8

Policy
  Finance: ALLOW
  Other:   BLOCK
```

---

# 177. Client-Side Managed Tool Registry

Endpoint client stores only assigned tools.

Per tool:

```text
package identity
version
runtime
entrypoint
local install path
hash
organization policy ref
status
health
last execution
```

Client must not independently browse/install Marketplace content.

---

# 178. Client Does Not Trust Marketplace Directly

Important trust boundary:

```text
Marketplace package signature
      +
Organization deployment signature
      =
Client can install
```

Client should not install a public Marketplace package merely because Marketplace signed it.

Organization Control Plane must explicitly assign it.

---

# 179. Tool Execution Identity

Endpoint client executes each tool under a defined execution identity.

Options:

```text
endpoint service low-privilege identity
dedicated per-tool identity
ordinary logged-in user
privileged service identity
sandbox runtime
```

Default should be a constrained managed identity, not unrestricted Administrator/root.

---

# 180. Elevated Tools

A package that requests:

```text
elevated: true
```

must trigger stronger review.

Admin UI:

```text
This tool requests elevated local privileges.

Reason:
  Manage Windows Service

Requested:
  service-control

[Block]
[Require Approval]
[Approve for selected managed devices]
```

Every protected action still validates through Container 1.

---

# 181. Tool Capability Declaration

Tool manifest should use a normalized capability model:

```text
filesystem.read
filesystem.write
filesystem.delete
network.connect
process.spawn
service.manage
registry.read
registry.write
docker.control
kubernetes.control
credential.use
device.info
```

Platform adapters map normalized capabilities to OS-specific enforcement.

---

# 182. Marketplace AI Description Update Handling

If Marketplace version changes only AI description:

- show text diff.
- allow admin to accept.
- organization override continues to win unless admin resets it.

Do not overwrite a local domain-specific description silently.

---

# 183. Marketplace Schema Update Handling

If input schema changes:

- mark tool update requiring compatibility review.
- report affected policies.
- report affected resource extractors.
- client rollout does not begin until package update is approved.

---

# 184. Custom Code Editor Security

Browser code editor must never embed credentials automatically.

Provide a separate:

```text
Credentials
  + Add Credential Requirement
```

Code should reference a logical injected credential mechanism, not literal secret.

Secret scanner runs on every save/test/publish.

---

# 185. Custom Tool Templates

Provide starter templates:

```text
Python JSON Tool
Node JSON Tool
PowerShell JSON Tool
Java JSON Tool
HotFolder File Reader
HotFolder File Writer
HTTP Client Tool
CSV Processor
Log Parser
```

Starter templates use the standard stdin/stdout contract.

---

# 186. Custom Tool Example — Python

Generated template:

```python
import json
import sys

def main(request):
    inputs = request["inputs"]
    name = inputs["name"]

    return {
        "success": True,
        "result": {
            "message": f"Hello {name}"
        }
    }

request = json.load(sys.stdin)
result = main(request)
json.dump(result, sys.stdout)
```

The template itself is not privileged.

---

# 187. Custom Tool Example — PowerShell

Conceptual template:

```powershell
$inputJson = [Console]::In.ReadToEnd()
$request = $inputJson | ConvertFrom-Json

$result = @{
    success = $true
    result = @{
        value = "..."
    }
}

$result | ConvertTo-Json -Depth 10
```

Runtime wrapper still controls:

- timeout.
- environment.
- filesystem.
- credentials.
- Container 1 permit.

---

# 188. Custom Tool Preview for AI

Before deployment:

```text
AI will see:

Tool:
  finance.validate_export

Description:
  Validate Finance CSV exports placed in Finance HotFolder.

Inputs:
  file: CSV file inside Finance HotFolder

Use when:
  user asks to validate the daily finance export

Do not use when:
  file is outside Finance HotFolder
```

This lets admin tune selection quality.

---

# 189. Tool Simulation

Admin should be able to test:

```text
"What tools would the AI see for this user/agent/device?"
```

and:

```text
"Would this tool be allowed for this resource?"
```

Marketplace-installed and custom tools use the same simulation.

---

# 190. Marketplace Publication Audit

Organization records:

```text
tool
version
publisher identity
organization user who submitted
time
Marketplace submission ID
package hash
result
```

Marketplace records corresponding publisher/submission audit.

---

# 191. Marketplace Import Audit

Organization records:

```text
package
version
publisher
Marketplace signature
imported by
import time
local scan result
security reviewer
deployment approver
```

---

# 192. Drupal Marketplace Availability

The Drupal Marketplace should scale independently:

```text
CDN/WAF
   |
Drupal Web x N
   |
   +--> Drupal DB
   +--> Redis optional
   +--> Object Storage
   +--> Search backend optional
   +--> Malware/static-scan workers
   +--> Signing service/KMS/HSM
```

It remains outside runtime authorization.

---

# 193. Marketplace Search

Search/filter:

```text
name
description
publisher
tool type
runtime
OS
architecture
risk
permission
license
verified publisher
security review level
source available
```

Sort:

```text
relevance
downloads
recently updated
rating
new
```

---

# 194. Public Package Page

Public Drupal package page:

```text
Name
Publisher
Version
Last updated
Runtime
Supported platforms

Description
Tools
Inputs/outputs
AI description
Requested permissions
Risk
Network destinations
Credential requirements
Source
License
Versions
Changelog
Security
Reviews
```

Installation button should say:

```text
Copy Package ID
```

or:

```text
Open in MCP Access Admin
```

and should not imply that an ordinary end user can bypass the organization administrator.

---

# 195. Marketplace Deep Link

Optional:

```text
mcp-access://marketplace/import/<publisher>/<package>@<version>
```

When opened:

1. Opens local Control Plane/admin interface.
2. Requires administrator authentication.
3. Shows package review.
4. Never deploys silently.

---

# 196. Community Tool Publish Deep Link / Handoff

From local Control Plane:

```text
Publish to Marketplace
```

may open Marketplace browser approval/publisher page and then return a submission token.

The Control Plane uploads package directly to Marketplace authenticated API after user authorization.

---

# 197. Marketplace Private Package Drafts

Publisher may save:

```text
Private Draft
Unlisted Test Version
Public Submission
```

Unlisted packages can support testing before public release.

---

# 198. Organization Internal Marketplace — Future

Container 2 may later expose an internal catalog:

```text
Organization Tools
```

for internally authored packages not published publicly.

This uses the same package format and client deployment system.

---

# 199. Package Portability

A package created through:

```text
Custom Tool Builder
```

can be:

```text
deployed internally
exported YAML/package archive
published to Community Marketplace
published to private registry
```

One canonical format avoids reimplementation.

---

# 200. Marketplace MVP Acceptance Criteria

Marketplace + distribution MVP is complete when:

1. Drupal user can become a publisher.
2. Publisher can create a schema-only package.
3. Publisher can upload a Python tool package.
4. Publisher can upload a native binary package.
5. Publisher can define AI description.
6. Publisher can define input/output schema.
7. Publisher can declare filesystem/network permissions.
8. Publisher can declare runtime type.
9. Submission passes automated validation.
10. Reviewer can approve/reject.
11. Approved version is immutable and signed.
12. Organization admin can authenticate to Marketplace.
13. Organization admin can import package.
14. Control Plane verifies signature/hash.
15. Imported package is a draft.
16. Admin can override AI description.
17. Admin can configure permissions.
18. Admin can configure credentials from Vault.
19. Admin can assign device group.
20. Client receives deployment assignment.
21. Client downloads approved organization artifact.
22. Client verifies package and deployment signatures.
23. Client installs package.
24. Client reports status transitions.
25. Admin can see `DOWNLOADING`, `INSTALLING`, `READY`, and failures.
26. Installed tool executes only after Container 1 authorization.
27. Admin can roll back package version.
28. Admin can remove package.
29. Admin can create a custom Python tool in browser.
30. Admin can define tool inputs and AI description.
31. Admin can test custom tool.
32. Admin can deploy custom tool through same client system.
33. Authorized user/admin can click `Publish to Marketplace`.
34. Publication wizard removes secrets/local values.
35. Published local tool becomes a Marketplace submission.
36. Another organization administrator can later import the approved package.

---

# 201. Marketplace Security Invariants

Automated tests must prove:

1. Ordinary organization users cannot silently install Marketplace tools.
2. Marketplace package signature alone is insufficient for client installation; organization assignment is also required.
3. Client rejects artifact hash mismatch.
4. Client rejects invalid Marketplace signature.
5. Client rejects invalid organization deployment signature.
6. Client never executes an unassigned package.
7. Marketplace does not receive organization Vault secrets.
8. Public package export contains no plaintext organization credential.
9. Publish workflow detects secret-like values.
10. Local absolute paths are not accidentally published without explicit approval.
11. Published immutable version cannot be replaced.
12. Drupal does not execute uploaded community binaries during ordinary validation.
13. New elevated permission is surfaced during update.
14. New network destination is surfaced during update.
15. Organization AI-description override is not silently overwritten by update.
16. Client package execution continues to require Container 1 authorization.
17. Runtime tool cannot request arbitrary credential references.
18. Script inputs do not pass through unsafe shell concatenation.
19. Package archive traversal is blocked.
20. Decompression bombs are bounded.
21. Revoked package is not selected for fresh installation by default.
22. Marketplace outage does not break installed tools.
23. Endpoint-client status cannot be forged by an unauthenticated device.
24. Tool publication requires authenticated scoped Marketplace permission.
25. Enterprise publication can be gated by organization approval.

---

# 202. Marketplace and Tool Distribution Monorepo Additions

Recommended additions:

```text
apps/
  marketplace-drupal/
    composer.json
    web/
      modules/custom/
        mcp_marketplace_core/
        mcp_marketplace_package/
        mcp_marketplace_builder/
        mcp_marketplace_publisher/
        mcp_marketplace_review/
        mcp_marketplace_security/
        mcp_marketplace_api/
        mcp_marketplace_signing/
        mcp_marketplace_advisory/

packages/
  schemas/
    marketplace/
      package.schema.json
      local-tool.schema.json
      runtime.schema.json
      deployment.schema.json
      advisory.schema.json
      package-diff.schema.json

crates/
  package-verification/
  package-runtime/
  deployment-manifest/
  tool-protocol/

backend-libs/
  marketplace-client/
  package-import/
  package-publish/
  package-sanitizer/
  deployment-service/
```

The mandatory runtime Docker images remain:

```text
mcp-access-quickstart
mcp-access-gateway
mcp-access-control
```

The Drupal Marketplace may be deployed separately as part of the public website and is not a runtime authorization dependency.

---

# 203. Final Marketplace Principle

The Marketplace should be designed around this rule:

> **Community publishes capability. Organization administrators grant authority. Endpoint clients execute only assigned, verified capability. Container 1 still authorizes every protected action.**

And the tool-authoring rule is:

> **Anything created locally—binary integration, Python script, JavaScript tool, PowerShell tool, API-to-MCP package, or custom code written in the admin editor—should use the same package model so it can be tested internally, deployed to managed clients, exported, or published back to the community.**

This creates a full community loop:

```text
CREATE
  |
  v
TEST
  |
  v
DEPLOY INTERNALLY
  |
  v
IMPROVE
  |
  v
PUBLISH TO MARKETPLACE
  |
  v
OTHERS IMPORT
  |
  v
THEY CUSTOMIZE
  |
  v
COMMUNITY ECOSYSTEM GROWS
```

while keeping runtime permissions, credentials, deployment authority, and endpoint enforcement under each organization's control.

---

# 204. Implementation Hardening Changes Required Before Coding

The architecture is strong enough to proceed, but the following changes are **required implementation constraints** rather than optional future improvements. They close gaps between the conceptual design and a system that can be safely built and operated.

## 204.1 One Canonical Package Specification

There must be exactly one authoritative package model shared by:

- Quickstart.
- Control Plane.
- Endpoint Client.
- Drupal Marketplace.
- private registries.
- import/export.
- custom tool builder.
- API-to-MCP builder.
- CLI/package tooling.

Canonical schemas live in:

```text
packages/schemas/
```

The Drupal Marketplace consumes released schema artifacts. It must not define a competing package format.

Every schema must contain:

```text
schemaVersion
minimumReaderVersion
packageType
packageIdentity
packageVersion
toolDefinitions
runtimeDefinitions
artifacts
permissions
resourceMappings
credentialRequirements
AI metadata
compatibility
```

Unknown fields and future schema versions require explicit handling. Do not silently ignore security-sensitive fields.

## 204.2 Separate Marketplace Metadata from Artifact Storage

Drupal owns:

- publisher identity.
- package metadata.
- moderation state.
- package-version records.
- documentation.
- reviews.
- search/catalog metadata.
- security advisories.
- API authorization.

Drupal should not be used as the primary binary blob store for large executable packages.

Executable/source archives should be stored in:

```text
S3-compatible object storage / CDN
```

Each artifact record in Drupal stores:

```text
object key
size
SHA-256
media type
platform
architecture
runtime
signature metadata
scan status
```

## 204.3 Uploaded Code Must Never Execute Inside Drupal

Drupal is a public-facing application and cannot be the execution environment for submitted:

```text
.exe
.py
.js
.ps1
.bat
.cmd
.sh
.jar
.dll
```

Validation inside Drupal is limited to safe parsing and metadata validation.

Executable testing/scanning must occur in an isolated worker/sandbox environment with:

- no production secrets.
- disposable filesystem.
- no default network.
- strict CPU/memory/time limits.
- no privileged container capabilities.
- no host mounts.
- archive-bomb controls.
- artifact quarantine.

The Marketplace implementation may use independent scan workers because Marketplace infrastructure is not one of the three core runtime product containers.

## 204.4 Container 2 Must Not Execute Arbitrary Custom Tool Code

The Custom Local Tool Builder lives in Container 2, but the code entered into the browser editor must not execute inside the Control Plane JVM/process.

Testing options:

```text
1. Designated enrolled Test Client
2. Quickstart isolated local runner
3. Future isolated remote sandbox service
```

Preferred enterprise workflow:

```text
Admin edits code
    |
    v
Container 2 validates syntax/schema
    |
    v
Admin selects managed test endpoint
    |
    v
signed test assignment
    |
    v
Endpoint Client creates disposable sandbox
    |
    v
test execution
    |
    v
structured result returned
```

This keeps Container 2 stateless and removes a direct RCE path from the management backend.

## 204.5 Desired-State / Reported-State Client Model

Client software distribution must use a reconciliation model rather than imperative "push this file now" commands.

Container 2 stores **desired state**:

```text
Device A:
  package X -> 1.3.0
  package Y -> absent
```

Client reports **observed state**:

```text
Device A:
  package X -> 1.2.0 READY
  package Y -> 2.0.0 READY
```

The client reconciliation loop converges observed state toward desired state.

This gives:

- retries.
- offline device recovery.
- deterministic rollouts.
- idempotency.
- rollback.
- scalable control-plane behavior.

## 204.6 Deployment Generation

Each desired-state publication receives an immutable generation:

```text
deploymentGeneration
desiredStateHash
issuedAt
expiresAt optional
signature
```

Client only accepts a generation when:

- organization signature is valid.
- generation is newer than current accepted generation.
- device assignment matches.
- package versions are trusted/available.

## 204.7 Dual Trust Requirement for Community Packages

For a Marketplace-origin package, endpoint installation requires both:

```text
Marketplace release trust
       +
Organization deployment trust
```

Marketplace signature proves:

> this is the immutable Marketplace package reviewed/published under this identity/version.

Organization signature proves:

> this organization administrator approved this package/version for this device.

Neither signature replaces the other.

## 204.8 Organization Artifact Mirror

Enterprise mode should default to mirroring imported Marketplace artifacts into organization-controlled object storage.

Clients download organization-approved artifacts from:

```text
organization artifact mirror
```

rather than directly from the public Marketplace.

Benefits:

- Marketplace outage isolation.
- stable rollouts.
- package retention.
- local security scanning.
- internal egress controls.
- auditability.
- air-gap support.

Quickstart may cache artifacts inside `/data`.

## 204.9 Package Import Transaction

Marketplace import must be transactional at the logical level:

```text
FETCH METADATA
FETCH MANIFEST
VERIFY MARKETPLACE SIGNATURE
FETCH ARTIFACTS
VERIFY HASHES
ORGANIZATION SCAN
STORE IN MIRROR
CREATE LOCAL PACKAGE VERSION
CREATE IMPORT AUDIT
STATE = UNDER_REVIEW
```

A failure at any stage must not produce a deployable partial package.

## 204.10 Installation Transaction

Endpoint client should install to a staging path:

```text
download
  -> verify
  -> unpack to staging
  -> prepare runtime
  -> self-test
  -> atomically activate version
```

Never overwrite the currently working version in place.

## 204.11 Package Runtime Supervisor

The Endpoint Client is responsible for supervising managed tools.

Capabilities:

- process launch.
- runtime limits.
- health check.
- log capture.
- crash tracking.
- restart policy.
- version isolation.
- environment sanitization.
- sandbox selection.
- credential injection.
- termination.

A community package must not implement its own uncontrolled background daemon unless explicitly supported and declared.

## 204.12 Background Tool Types

Package execution mode must be explicit:

```text
ON_DEMAND
LONG_RUNNING_SERVICE
EVENT_CONSUMER
SCHEDULED
```

V1 should prioritize `ON_DEMAND`.

Long-running/event/scheduled tools require additional controls:

```text
restart policy
maximum lifetime
event source
queue limits
background authorization behavior
credential TTL
network policy
```

## 204.13 File Event Tools

HotFolder event tools should be implemented by the trusted Endpoint Client, not by arbitrary community watchers whenever possible.

Community tools may subscribe to normalized events:

```text
CREATE
MODIFY
MOVE
DELETE
```

subject to organization policy.

This avoids each package requiring unrestricted filesystem watchers.

## 204.14 Publish Workflow Must Create a New Public Artifact

`Publish to Marketplace` must not upload the internal organization package blindly.

It creates a sanitized publication artifact:

```text
Organization Package
      |
      v
Publication Transform
      |
      +-- remove organization overrides
      +-- remove credential bindings
      +-- convert settings to install parameters
      +-- remove internal URLs/IDs/paths
      +-- retain portable source/artifacts
      |
      v
Public Submission Package
```

The package hash changes because the public package is a different artifact.

## 204.15 Marketplace Publication Requires Explicit Versioning

The publishing user selects:

```text
publisher namespace
package slug
semantic version
release notes
license
source disclosure
```

A Marketplace version can never be overwritten.

## 204.16 Package Compatibility Must Be Machine-Readable

Manifest compatibility includes:

```text
minimum Control Plane version
minimum Gateway version where applicable
minimum Endpoint Client version
package schema version
runtime version
OS
architecture
required client capabilities
```

Admin UI must show why a device is `INCOMPATIBLE`.

## 204.17 Permission Expansion Diff Is Mandatory

Before:

- package import approval.
- package update.
- local custom-tool update.
- Marketplace publication.

compute security-semantic diff for:

```text
filesystem
network
process
privilege
credential
resource scope
tool risk
input schema
new external host
new runtime
```

The UI must not hide permission expansion inside a generic changelog.

## 204.18 Marketplace API Calls Need Idempotency

All mutating Marketplace APIs used by Container 2 must accept:

```text
Idempotency-Key
```

for operations such as:

- create submission.
- upload artifact completion.
- finalize version.
- withdraw submission.

Likewise Control Plane import/publish jobs must have stable operation IDs.

## 204.19 Asynchronous Marketplace Operations

Large uploads, scans, signing and moderation are asynchronous.

Never hold one HTTP request open while a binary is scanned.

Use jobs:

```text
SUBMISSION_CREATED
UPLOADING
VALIDATING
SCANNING
AWAITING_REVIEW
APPROVED
SIGNING
PUBLISHED
FAILED
```

Admin Panel polls or consumes events/webhooks.

## 204.20 Webhook Delivery

Drupal Marketplace should send signed webhook events to linked Control Planes only when the organization explicitly registers a webhook.

Events:

```text
package.version.published
package.advisory.created
package.version.revoked
submission.status.changed
```

Webhook payloads:

- signed.
- timestamped.
- replay protected.
- retryable.
- idempotent.

Polling remains supported so webhook availability is not mandatory.

## 204.21 Marketplace Token Scope Separation

Marketplace OAuth scopes must separate:

```text
catalog.read
package.import
submission.create
submission.upload
submission.publish
publisher.manage
```

A token used to browse/import must not automatically permit publishing.

## 204.22 Publisher Organization Separation

Marketplace publisher identity is distinct from an organization user role.

A person may:

```text
be Tool Admin in Organization A
and
Publisher for community namespace B
```

Do not infer publication rights from local organization admin privileges.

## 204.23 Signing Key Hierarchy

Separate trust domains:

```text
Marketplace package signing key
Organization deployment signing key
Control-plane bundle signing key
Gateway execution-permit signing key
Endpoint device identity CA/key
```

Never reuse one key for multiple purposes.

## 204.24 Revocation Metadata

Marketplace publishes a signed compact advisory/revocation index.

Control Plane periodically caches it.

Runtime Gateway does not synchronously depend on this index.

## 204.25 Package Deletion Semantics

Published Marketplace package versions are not physically deleted merely because a publisher deletes a project page.

Security/legal takedown should mark versions:

```text
WITHDRAWN
QUARANTINED
REVOKED
```

while preserving integrity/audit metadata as legally appropriate.

## 204.26 Control Plane Marketplace Adapter

All Drupal-specific protocol handling belongs in:

```text
backend-libs/marketplace-client
```

The rest of Container 2 depends on an internal registry abstraction:

```text
searchPackages()
getPackage()
getVersion()
downloadManifest()
createSubmission()
uploadArtifact()
getSubmissionStatus()
```

This permits future private registries without rewriting the UI/domain model.

## 204.27 Package Source Abstraction

Registry types should implement a shared interface:

```text
COMMUNITY_DRUPAL
PRIVATE_REGISTRY
AIRGAP_UPLOAD
LOCAL_PACKAGE
```

## 204.28 Admin UI Must Show Three Different Statuses

Do not collapse:

```text
Marketplace Status
Organization Approval Status
Device Deployment Status
```

Example:

```text
Marketplace:
  Published 1.4.0

Organization:
  Approved 1.3.0
  1.4.0 Under Review

Fleet:
  1.3.0 Ready 128/132
```

## 204.29 Package Provenance Must Be Visible

Admin should be able to see:

```text
Publisher
Source repository
Marketplace version
Marketplace signature
Organization import hash
Organization scan result
Deployment signer
Installed artifact hash
```

## 204.30 Marketplace Backend API Contract Is a Separate Specification

Implementation of the Drupal Marketplace must follow the companion document:

```text
MARKETPLACE_ADMIN_API_INTEGRATION.md
```

That document defines:

- Drupal data model.
- API endpoints.
- OAuth scopes.
- import flow.
- publish flow.
- artifact-upload flow.
- moderation flow.
- webhook flow.
- package signing.
- Admin Panel integration.
- retry/idempotency.
- error contracts.
- sequence diagrams.
- security requirements.

---

# 205. Marketplace Platform Architecture — Finalized Separation of Responsibilities

The Community Marketplace must be implemented as a **separate platform composed of multiple trust boundaries**.

Drupal is the public/community presentation layer. It is **not** the authoritative package-processing backend.

The finalized architecture is:

```text
                         COMMUNITY MARKETPLACE PLATFORM

                               CDN / WAF
                                  |
                                  v
                            +-------------+
                            |   Drupal    |
                            | Public Web  |
                            | Community   |
                            +------+------+
                                   |
                            OAuth / REST API
                                   |
                                   v
                         +----------------------+
                         | Marketplace API      |
                         | Java, stateless      |
                         |                      |
                         | Package CRUD         |
                         | Version metadata     |
                         | Publisher ownership  |
                         | Moderation state     |
                         | Search/catalog API   |
                         | Upload orchestration |
                         | Advisory API         |
                         | Job status           |
                         +-----+----------+-----+
                               |          |
                               |          +--------------------------+
                               |                                     |
                               v                                     v
                         PostgreSQL                           Object Storage
                               |                                     |
                               |                                     |
                               +--------------+----------------------+
                                              |
                                              v
                                      Durable Job Queue
                                              |
                                              v
                                   +-----------------------+
                                   | Marketplace Worker    |
                                   | Java, stateless       |
                                   |                       |
                                   | Validate              |
                                   | Scan                  |
                                   | Build/package         |
                                   | SBOM                  |
                                   | Security checks       |
                                   | Publication workflow  |
                                   +-----------+-----------+
                                               |
                                               v
                                     Isolated Sandbox
                                               |
                                               v
                                        Signing Service
                                          / KMS / HSM
                                               |
                                               v
                                        Immutable Release
```

The public/community Drupal site communicates only with the Marketplace API for package business operations.

The organization's Control Plane also communicates with the **same Marketplace API**.

This allows:

```text
Drupal Web
Control Plane
CLI
CI/CD
Private-registry sync
Future desktop/IDE integrations
        |
        +----> Marketplace API
```

without coupling consumers to Drupal entity internals.

---

# 206. Marketplace Components

## 206.1 Drupal Web

Drupal owns community-facing presentation and CMS concerns.

Responsibilities:

- Community login UX.
- Publisher profile pages.
- Marketplace browse/search pages.
- Package detail pages.
- Package documentation.
- Ratings and reviews.
- Categories/tags.
- Publisher dashboards.
- Moderation UI.
- Publication UI.
- SEO.
- Community content.
- Legal/policy pages.

Drupal obtains authoritative package state through Marketplace APIs.

Drupal must not:

- Own executable artifact processing.
- Execute uploaded tools.
- Hold package-signing private keys.
- Be the source of truth for deployment authorization.
- Be the source of truth for organization-side package installation.
- Directly mutate package-version storage outside Marketplace API rules.

## 206.2 Marketplace API

New application:

```text
apps/marketplace-api
```

Recommended implementation:

- Java 21+.
- Quarkus preferred for consistency with Control Plane.
- Stateless.
- Horizontally scalable.
- PostgreSQL for authoritative metadata.
- Object-storage integration.
- OAuth/OIDC resource server.
- OpenAPI 3.x.
- Idempotency support.
- Background-job orchestration.
- Search adapter.
- Webhook/advisory API.

Responsibilities:

- Package create/read/update metadata.
- Draft deletion.
- Publisher namespaces.
- Package ownership.
- Package versions.
- Immutable published-version enforcement.
- Submission orchestration.
- Upload-session creation.
- Artifact metadata.
- Moderation state.
- Publication requests.
- Job state.
- Search/catalog.
- Advisories.
- Revocation/deprecation metadata.
- Marketplace OAuth authorization checks.
- Webhook registration/delivery metadata.
- Signing-key public metadata.

## 206.3 Marketplace Worker

New application:

```text
apps/marketplace-worker
```

Recommended implementation:

- Java 21+.
- Stateless worker replicas.
- Queue-driven.
- No public HTTP exposure except health/metrics if required.

Responsibilities:

- Package validation.
- Artifact hash verification.
- Archive-safety validation.
- Secret scanning.
- Malware-scan orchestration.
- SBOM generation/validation.
- Static analysis orchestration.
- Build/package orchestration where supported.
- Permission consistency checks.
- Package-diff generation.
- Canonical release assembly.
- Signing request.
- Publishing immutable release artifacts.
- Cleanup of failed/quarantined submissions.

The worker must not directly expose general user APIs.

## 206.4 Isolated Scan/Build Sandbox

Untrusted community code must not execute:

- in Drupal.
- inside Marketplace API process.
- inside Marketplace Worker host namespace.
- inside Control Plane.

Dynamic build/test execution uses disposable sandboxes with:

```text
no production secrets
no host Docker socket
no privileged mode
no host filesystem mount
no internal network by default
limited CPU
limited memory
limited disk
execution timeout
ephemeral filesystem
controlled dependency egress when explicitly required
```

The sandbox may be implemented using:

- hardened containers.
- microVMs.
- dedicated sandbox workers.
- another isolation technology selected by ADR.

## 206.5 Signing Service

Marketplace signing must be a separate trust boundary.

Preferred:

```text
Marketplace Worker
       |
       | release digest
       v
KMS / HSM / Signing Service
       |
       v
signature
```

The worker does not receive/export the signing private key.

## 206.6 Object Storage

Marketplace executable/source artifacts belong in immutable object storage.

Examples:

- S3.
- MinIO.
- Azure Blob.
- Google Cloud Storage.

Drupal and PostgreSQL hold metadata/reference only.

---

# 207. Marketplace CRUD Semantics

Marketplace content does not use unrestricted CRUD after publication.

## 207.1 Draft Package

Allowed:

```text
CREATE
READ
UPDATE
DELETE
```

## 207.2 Published Package Metadata

Editable only for explicitly non-integrity-critical descriptive fields when policy permits, such as:

```text
support URL
documentation URL
categories
screenshots
```

All security-relevant changes require a new version.

## 207.3 Published Package Version

Allowed:

```text
READ
DEPRECATE
WITHDRAW
QUARANTINE
REVOKE
```

Not allowed:

```text
UPDATE BYTES
REPLACE MANIFEST
REPLACE ARTIFACT
CHANGE HASH
CHANGE SIGNATURE
DELETE SILENTLY
```

Any functional/security change creates:

```text
new semantic version
```

---

# 208. Publication Is an Asynchronous Workflow

Drupal must not wait synchronously for package publication.

Flow:

```text
Drupal / Control Plane
        |
        | POST publication request
        v
Marketplace API
        |
        | returns job ID immediately
        v
Durable Queue / Workflow State
        |
        v
Marketplace Worker
```

Canonical state machine:

```text
QUEUED
VALIDATING
SCANNING
BUILDING
GENERATING_SBOM
SECURITY_CHECK
AWAITING_REVIEW
APPROVED
SIGNING
PUBLISHING
PUBLISHED
```

Failure/alternate states:

```text
CHANGES_REQUESTED
REJECTED
FAILED
WITHDRAWN
QUARANTINED
```

The next stage must not run if a required previous stage failed.

---

# 209. Durable Workflow Implementation

V1 may use:

```text
PostgreSQL durable workflow/job tables
```

with:

- leases.
- retries.
- heartbeat.
- idempotency.
- state-transition constraints.

At larger scale, a dedicated queue may be introduced:

```text
NATS JetStream
RabbitMQ
Kafka
SQS
```

A workflow engine such as Temporal may be evaluated later if complexity justifies it.

Do not require Temporal for V1.

The workflow source of truth remains durable and recoverable.

---

# 210. Marketplace API Stateless Scaling

Run:

```text
marketplace-api x N
```

behind a load balancer.

No sticky sessions.

State external:

```text
PostgreSQL
Object Storage
Queue
Search backend optional
Vault/KMS
```

Likewise:

```text
marketplace-worker x N
```

processes jobs using distributed leases/queue semantics.

---

# 211. Direct Artifact Upload

Large artifacts must not traverse Drupal or Java API memory unnecessarily.

Correct flow:

```text
Browser/Admin Panel
       |
       | request upload session
       v
Marketplace API
       |
       | short-lived presigned upload URL
       v
Browser/Admin Panel -----------------> Object Storage
       |
       | upload completion
       v
Marketplace API
```

Marketplace API verifies:

```text
object exists
expected size
SHA-256
media type
submission ownership
upload expiry
quarantine location
```

before marking the artifact complete.

---

# 212. Drupal Uses Marketplace APIs for Package Operations

Drupal should call Marketplace API for:

```text
list packages
search packages
create draft package
edit draft metadata
delete draft
create package version
create publication
get publication status
deprecate package/version
request withdrawal
request revocation where authorized
publisher namespace management
```

Drupal must not directly write Marketplace package tables.

---

# 213. Same API Serves Organization Control Plane

Organization `mcp-access-control` uses Marketplace API for:

```text
search
package metadata
version metadata
artifact descriptors
release signature metadata
advisories
import
publication submission
submission status
publisher identity
```

This ensures one business-rule implementation.

---

# 214. Marketplace API and Organization Control Plane Are Different Applications

Do not merge:

```text
mcp-access-control
```

and:

```text
marketplace-api
```

They serve different security domains.

`mcp-access-control`:

- installed by customers.
- manages customer users/tools/devices/policies.
- may run entirely private.
- owns organization configuration.

`marketplace-api`:

- public/community infrastructure.
- manages global publisher/package registry.
- exposed to the Internet.
- owns Marketplace package metadata.

---

# 215. Marketplace Worker Build Policy

Not every package needs a build.

Package classes:

```text
DECLARATIVE
SOURCE_SCRIPT
SOURCE_BUILD
PUBLISHER_BINARY
```

## DECLARATIVE

Examples:

- API-to-MCP.
- Remote MCP schema.
- Policy template.

No code execution required.

## SOURCE_SCRIPT

Examples:

- Python.
- JavaScript.
- PowerShell.
- Shell.

Scanning and packaging required; optional isolated tests.

## SOURCE_BUILD

Examples:

- Java source.
- Rust/Go/C/C++ source where supported later.

Requires isolated build sandbox.

## PUBLISHER_BINARY

Binary is not rebuilt by Marketplace unless reproducible-build feature exists.

Scan/metadata/provenance is performed and UI clearly marks it as publisher-provided.

---

# 216. Package Job API Contract

Publication requests return:

```json
{
  "jobId": "pub_01...",
  "status": "QUEUED"
}
```

Status endpoint returns:

```json
{
  "jobId": "pub_01...",
  "status": "SCANNING",
  "progress": 55,
  "step": "malware-scan",
  "retryable": false
}
```

Exact progress values are advisory; state is authoritative.

---

# 217. Idempotency

Mutating Marketplace requests require:

```text
Idempotency-Key
```

including:

- draft creation when appropriate.
- submission creation.
- artifact declaration.
- upload completion.
- publication start.
- withdrawal request.
- webhook registration.

Same key + same request:

```text
returns same logical result
```

Same key + different request:

```text
IDEMPOTENCY_CONFLICT
```

---

# 218. Marketplace API Authentication Model

Use OAuth 2.x/OIDC-compatible access tokens.

Scopes:

```text
catalog.read
package.read
package.import

package.draft.create
package.draft.write
package.draft.delete

submission.create
submission.write
submission.read
submission.withdraw

publisher.read
publisher.manage

advisory.read
webhook.manage
```

Drupal receives appropriate user-delegated scopes.

Organization Control Plane receives only scopes authorized by the linked Marketplace user/account.

---

# 219. Marketplace Public Web UX

Drupal remains the preferred public/community experience.

Public package pages should show:

```text
package name
publisher
latest version
runtime
supported OS
source availability
permissions
risk
credential requirements
network destinations
filesystem scopes
AI description
versions
changelog
reviews
security advisories
```

The public website should never imply that clicking a package bypasses organization approval.

Recommended actions:

```text
Open in MCP Access Admin
Copy Package ID
View Source
View Versions
```

---

# 220. Drupal Publication UX

Drupal publisher dashboard may initiate publication through Marketplace API.

Example:

```text
Package: PDF Tools
Version: 1.4.0

Validation           ✓
Secret Scan          ✓
Malware Scan         ✓
SBOM                  ✓
Security Review      ...
Signing              waiting
Publishing           waiting
```

All statuses come from Marketplace API/job state.

---

# 221. Organization-Side Package Import UX

Control Plane shows:

```text
Community Marketplace
      |
      v
Import to Organization
      |
      v
Verify Marketplace release
      |
      v
Organization scan
      |
      v
Mirror artifact
      |
      v
Configure credentials
      |
      v
Configure permissions
      |
      v
Assign devices
      |
      v
Deploy
```

---

# 222. Monorepo Changes

Recommended final marketplace-related tree:

```text
apps/
  marketplace-drupal/
  marketplace-api/
  marketplace-worker/

  gateway/
  control-plane/
  admin-ui/
  endpoint-client/

backend-libs/
  marketplace-client/
  marketplace-domain/
  marketplace-jobs/
  package-import/
  package-publish/
  package-sanitizer/
  artifact-mirror/
  deployment-service/

packages/
  schemas/
    marketplace/
    deployment/
    package/
```

---

# 223. Marketplace Infrastructure Is Separate From Core Product Release Images

The three mandatory customer/runtime images remain:

```text
mcp-access-quickstart
mcp-access-gateway
mcp-access-control
```

Marketplace infrastructure has its own deployment/release lifecycle:

```text
marketplace-drupal
marketplace-api
marketplace-worker
```

because these run the public/community service rather than customer runtime.

Endpoint client remains a native GitHub Release artifact.

---

# 224. Marketplace Release Pipeline

Marketplace platform release should build:

```text
Drupal application release
Marketplace API container
Marketplace Worker container
schema package
OpenAPI contract
database migrations
scan-worker images if separately versioned
```

The customer product release remains independently versioned, but compatibility must be documented.

---

# 225. Marketplace / Product Compatibility

Marketplace package manifest includes:

```text
minimum control version
minimum gateway version where relevant
minimum endpoint-client version
schema version
required runtime capabilities
```

Marketplace catalog can list newer packages even if a customer's Control Plane cannot install them.

Admin Panel shows:

```text
Not compatible with your current MCP Access version.
Required: >= 1.6.0
Current: 1.4.2
```

---

# 226. Signing Key Separation

Mandatory separate keys:

```text
Marketplace package signing
Organization deployment signing
Control bundle signing
Gateway execution permit signing
Device identity CA
```

Marketplace Worker never directly stores organization keys.

Organization Control Plane never stores Marketplace private signing key.

---

# 227. Package Publication Approval vs Marketplace Moderation

Two separate workflows may exist:

```text
ORGANIZATION INTERNAL PUBLICATION APPROVAL
                 |
                 v
Marketplace Submission

MARKETPLACE MODERATION
                 |
                 v
Public Signed Release
```

Quickstart single-user can skip internal publication approval.

Enterprise may require:

```text
Tool Author
   -> Security Review
   -> Legal Review optional
   -> Marketplace Publisher
```

before submission.

---

# 228. Worker Sandbox Networking

Default:

```text
network = DENY
```

For controlled build dependency downloads:

- use dedicated build profile.
- allow only approved package registries.
- block internal/private address space.
- log egress domains.
- no organization/customer credentials.

---

# 229. Marketplace Worker Does Not Become Client Runtime

Marketplace Worker exists only to process public package submissions.

It is not used to execute organization tools.

Organization execution remains:

```text
Endpoint Client
   |
   v
Container 1 authorization
```

---

# 230. Final Marketplace Trust Boundary

```text
DRUPAL
"What does the community see and edit through the website?"

MARKETPLACE API
"What package/publisher/version state is authoritative?"

MARKETPLACE WORKER
"Can this submission be validated, scanned, packaged and published?"

SIGNING SERVICE
"Can this immutable release digest receive a Marketplace signature?"

ORGANIZATION CONTROL PLANE
"Does this organization trust/import/configure/deploy this package?"

ENDPOINT CLIENT
"Is this organization-assigned artifact valid, installed and healthy?"

ACCESS GATEWAY
"Is this exact execution allowed now?"
```

This separation is the recommended final architecture.
