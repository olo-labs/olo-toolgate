# MCP SDK compatibility vs. spec 2026-07-28 — results 2026-10-10

## Purpose

The first compatibility record (D0 artifact): what each pinned official MCP SDK actually does against the 2026-07-28 and 2025-11-25 conformance requirements, with the commands run and raw outputs in [raw/](raw/). Written as the run log of the person who ran it.

Evidence-backed results for four official MCP server SDKs. Each cell is labelled:
**PASS**: an official conformance run passed (scenario names and check counts given).
**SUPPORTED-BY-SOURCE**: no conformance scenario covers it, but it shows up in source or docs at the pinned tag (file:line cited), or in a local probe I ran against the registry package.
**MISSING**: evidence that the feature is absent.
**UNKNOWN**: I could not determine it.
For each claim the evidence section says whether it comes from a command I ran or from a file I read.

## Coordinates and resolved versions

| SDK | Registry coordinate (resolved 2026-10-10) | Source used | Commit |
|---|---|---|---|
| TypeScript | npm `@modelcontextprotocol/server@2.3.1` (shasum a15440d5…), `@modelcontextprotocol/client@2.3.1` (shasum 32207356…). `npm view` shows `latest` = 2.3.1 for both (published 2026-10-05) | git tag `@modelcontextprotocol/server@2.3.1` (same commit as `v2.3.1`) | fcef852f6cdafea07f36cf6fae69b37c8adf85be |
| Python | PyPI `mcp==2.3.0` (latest; wheel sha256 dd0c44c0…, uploaded 2026-10-02) | git tag `v2.3.0` | 2118f14f8a19bc158d8a1cf90af58d85d187f849 |
| .NET | NuGet `ModelContextProtocol` 2.2.0 (latest per the api.nuget.org flat container; `.Core`, `.AspNetCore` and `.Extensions.Tasks` are also at 2.2.0) | git tag `v2.2.0` (`src/Directory.Build.props:8` `<VersionPrefix>2.2.0`) | 6fa3825973949a9c4f0cd8af344e15a8db09dc35 |
| Java | Maven `io.modelcontextprotocol.sdk:mcp:2.0.1` and BOM `mcp-bom:2.0.1` (`<release>` 2.0.1 in repo.maven.apache.org maven-metadata.xml for both) | git tag `v2.0.1` | c7e1cfe90edcd9cbe030924310a194dc5492eab2 |

**Registry vs. tag (commands run):**
- `diff -rq dl/py-sdk/src/mcp <venv>/site-packages/mcp` (PyPI mcp 2.3.0) found no differences.
- `diff -rq` of `packages/server/dist` (built at the tag) against npm `@modelcontextprotocol/server@2.3.1/dist` found no differences.
- .NET: the C# stdio probe was built against the **NuGet** packages ModelContextProtocol 2.2.0 and ModelContextProtocol.Core 2.2.0 (versions confirmed in `obj/project.assets.json`). The conformance server was built from the tag source.
- Java: the conformance server was built from the tag source with `mvn install`. I did not compare it with the Central jar.

**Conformance suite:**
- `@modelcontextprotocol/conformance@0.2.0-alpha.12` (npm dist-tag `alpha`, published 2026-10-01, shasum d6f5776d…).
- The `latest` tag, **0.1.16, does not cover 2026-07-28**: `grep -rl 2026-07-28` over its unpacked package returns nothing.
- alpha.12 ships `requirements/2026-07-28.yaml`. `conformance list --requirements 2026-07-28` reports **37 required server scenarios**, plus 10 `tasks-*` extension scenarios and 3 pending ones that run but are never scored.
- The 2025-11-25 requirement set has 30 required server scenarios.

**Toolchains:**
- node v22.22.0 and pnpm 10.26.1
- Python 3.13 with uv
- OpenJDK 21.0.12.1 and Maven 3.9.11
- dotnet-sdk-10.0 10.0.112, installed with apt from Ubuntu noble-updates. The proxy blocks `builds.dotnet.microsoft.com`, `dotnetcli.azureedge.net` and `download.visualstudio.microsoft.com` (403); api.nuget.org is reachable.
- The C# build needed `-p:NuGetAudit=false`: advisory GHSA-23fw-v26w-5fgq on `Microsoft.Build.Tasks.Git` 8.0.0 is turned into an error by warnings-as-errors.

**Command lines** (wrapper script: `raw/probe-sources/run_legs.sh`):
```
# each leg: start the SDK's own conformance server, then
conformance server --url <URL> --requirements <REV> -o <dir>/checks      # REV in {2026-07-28, 2025-11-25}

TS    : (test/conformance) node --import tsx ./src/everythingServer.ts          URL http://localhost:3000/mcp  (both revs)
        build: pnpm install --frozen-lockfile --ignore-scripts && pnpm run build:all
Python: uv sync --frozen --all-extras --package mcp-everything-server && uv sync --frozen --all-extras --package mcp --inexact
        uv run --frozen mcp-everything-server --port 3001                        URL http://localhost:3001/mcp  (both revs)
.NET  : dotnet build tests/ModelContextProtocol.ConformanceServer -c Release -f net10.0 -p:NuGetAudit=false
        dotnet ModelContextProtocol.ConformanceServer.dll --urls http://localhost:3002
        URL 2026-07-28: http://localhost:3002/stateless ; 2025-11-25: http://localhost:3002  (per the suite's KNOWN_SDKS specOverrides)
Java  : mvn -B -DskipTests -pl conformance-tests/server-servlet -am install
        (conformance-tests/server-servlet) mvn exec:java -Dexec.mainClass=io.modelcontextprotocol.conformance.server.ConformanceServlet
        URL http://localhost:8080/mcp (both revs)
stdio probes (my own minimal servers on the *registry* packages; raw/probe-sources/):
        node stdio_probe.mjs <server cmd>          # 2026-07-28: server/discover, tools/list, tools/call, custom acme/echo (all with _meta envelope)
        node stdio_probe_legacy.mjs <server cmd>   # 2025-11-25: initialize, tools/list, custom acme/echo
```

## Feature × SDK

| # | Feature | TypeScript 2.3.1 | Python 2.3.0 | .NET 2.2.0 | Java 2.0.1 |
|---|---|---|---|---|---|
| 1 | Stateless 2026-07-28 core (no initialize, `server/discover`, per-request `_meta` protocolVersion/clientCapabilities) | **PASS**: `server-stateless` 30/30 checks; 37/37 required 2026-07-28 server scenarios (120 checks, 0 failed) | **PASS**: `server-stateless` 30/30; 37/37 required (120 checks, 0 failed) | **PASS**: `server-stateless` 28 passed + 2 skipped, 0 failed; 37/37 required (117 checks, 0 failed). Only at the stateless endpoint; stateful HTTP mode refuses 2026-07-28 (docs) | **MISSING**: 0/37 required; `server-stateless` 7 passed / 20 failed; no "2026-07-28" string anywhere in the Java sources |
| 2 | MRTR (`resultType:"input_required"`, `requestState`) | **PASS**: all 14 `input-required-result-*` scenarios (37 checks) | **PASS**: all 14 (37 checks) | **PASS**: all 14 (36 checks) | **MISSING**: all 14 fail ("Expected InputRequiredResult", "No result in response") |
| 3 | Tasks extension `io.modelcontextprotocol/tasks` | **MISSING** (built in): 9 of the 10 `tasks-*` scenarios fail, 0 checks reach a task. Repo baseline and ROADMAP say not implemented. Only a manual hook exists: `tasks/get`/`tasks/cancel` handlers can be registered (CHANGELOG 2.3.0) | **MISSING**: 9 of the 10 `tasks-*` fail (e.g. `tasks-extension-advertised`, tasks/get answers -32601); ROADMAP says deferred | **PASS**: 9 `tasks-*` scenarios pass, 44 checks, 0 failed (`tasks-lifecycle` 9, `tasks-dispatch-and-envelope` 9, `tasks-capability-negotiation` 5, `tasks-request-headers` 5, `tasks-wire-fields` 4, `tasks-mrtr-input` 4, `tasks-request-state-removal` 3, `tasks-required-task-error` 3, `tasks-mrtr-composition` 2). `tasks-status-notifications` is skipped by the harness for every SDK; docs say push status notifications are not implemented | **MISSING**: every `tasks-*` scenario fails or errors (server/discover → "Session ID required") |
| 4 | `subscriptions/listen` | **PASS** (subchecks of `server-stateless`): `sep-2575-server-sends-subscription-ack`, `-tags-subscription-id`, `-honors-notification-filter`, `-sends-tools-list-changed-on-subscription`, `-sends-prompts-list-changed-on-subscription` all SUCCESS | **PASS**: same 5 checks SUCCESS | **PASS (partial coverage)**: ack, tag-subscription-id and honors-filter SUCCESS. Both list_changed-on-subscription checks were SKIPPED because the fixture does not declare `listChanged` | **MISSING**: the listen checks fail inside `server-stateless`; no `subscriptions/listen` in the sources |
| 5 | Legacy 2025-11-25 serving | **PASS**: 30/30 required 2025-11-25 scenarios (70 checks), same `/mcp` endpoint as 2026 | **PASS**: 30/30 (70 checks), same endpoint | **PASS**: 30/30 (70 checks) at the stateful root endpoint (separate from `/stateless`) | **PASS**: 30/30 (70 checks). The only failure is the unscored pending `server-sse-polling` (timeout) |
| 6 | Transports: stdio + Streamable HTTP | **PASS** (HTTP, both eras) + **SUPPORTED-BY-SOURCE/probe** (stdio, both eras): `serveStdio` on npm 2.3.1 answered 2026 `server/discover`/`tools/call` and, on a fresh process, the 2025 `initialize` | **PASS** (HTTP) + **probe** (stdio, both eras) on PyPI 2.3.0 | **PASS** (HTTP) + **probe** (stdio, both eras) on NuGet 2.2.0 | HTTP: **PASS** for 2025 only. stdio: **SUPPORTED-BY-SOURCE** for 2025 only (`StdioServerTransportProvider`). Neither serves 2026-07-28 |
| 7 | Hook for custom JSON-RPC methods / extensions | **SUPPORTED-BY-SOURCE + probe**: `setRequestHandler(method, {params,result}, handler)`. `acme/echo` answered in both eras over stdio; capabilities can declare `extensions` | **SUPPORTED-BY-SOURCE + probe**: `Extension` + `MethodBinding` (or lowlevel `add_request_handler`). `acme/echo` answered in both eras; `capabilities.extensions["com.example/acme"]` advertised in `server/discover` | **SUPPORTED-BY-SOURCE + probe**: `McpServerOptions.RequestHandlers` (**experimental, MCPEXP002**). `acme/echo` answered in both eras, but the 2026 result had **no `resultType`** (TS/Py stamp `"complete"`) | **MISSING**: the handler map is built in private `prepareRequestHandlers()`; no builder method registers arbitrary methods |
| 8 | Hook to verify bearer tokens (HTTP) | **SUPPORTED-BY-SOURCE**: `requireBearerAuth` / `verifyBearerToken` with an `OAuthTokenVerifier.verifyAccessToken` (web-standard), and an Express variant | **SUPPORTED-BY-SOURCE**: `TokenVerifier.verify_token` protocol + `BearerAuthBackend`; `MCPServer(token_verifier=…)` | **SUPPORTED-BY-SOURCE**: standard ASP.NET Core auth (`AddAuthentication().AddJwtBearer(...).AddMcp(...)` + `RequireAuthorization()`); `McpAuthenticationHandler` adds resource-metadata challenges | **SUPPORTED-BY-SOURCE (generic only)**: `ServerTransportSecurityValidator.validateHeaders(headers)` can reject with a status code, and `McpTransportContextExtractor` exposes the request. There is no bearer/OAuth verifier abstraction ("bearer"/"OAuth" do not appear in `mcp*/src/main`); otherwise use a servlet filter |

No auth scenario is in the 2026-07-28 *server* requirement set (they are all client-side), so no SDK gets a PASS on row 8.

## Known gaps

1. **Java 2.0.1 has no 2026-07-28 support at all.**
   - Rows 1–4 and the 2026 halves of row 6 fail; scored 0/37 against the 2026-07-28 requirement set.
   - ROADMAP says 2026-07-28 comes in **3.x**, with milestones "planned for September 2026". Maven Central has no 3.x artifact yet; the latest is 2.0.1.
   - Java also has no public hook for custom methods.
   - On 2026-style requests, the Java servlet transport returns an HTTP 400 body that serializes the `McpError` exception, Java stack trace included, instead of a JSON-RPC error. This was seen in `raw/java-2026-07-28/conformance.txt`, e.g. the `tasks-capability-negotiation` error.
2. **The Tasks extension (SEP-2663) ships only in .NET** (`ModelContextProtocol.Extensions.Tasks` 2.2.0).
   - TypeScript 2.3.1: not implemented (ROADMAP tracks it in #2189). You can hand-register `tasks/get`/`tasks/cancel` handlers yourself.
   - Python 2.3.0: not implemented (ROADMAP tracks it in #2806).
   - Java: not implemented.
3. **.NET Tasks limitations (from its docs):**
   - Server-push task status notifications on `subscriptions/listen` are not implemented, so clients must poll.
   - Task creation is eager.
   - A tool cannot promote itself to a task mid-execution.
   - The conformance suite itself skips `tasks-status-notifications` for every SDK ("pending subscriptions/listen rewrite"), so this behaviour is unverified everywhere.
4. **.NET HTTP era split:**
   - 2026-07-28 is served only in Stateless session mode (the default) or in hybrid `StatefulForInitializeClients` mode.
   - `SessionMode = Stateful` refuses 2026-07-28.
   - The conformance fixture uses two endpoints (`/` and `/stateless`). TypeScript and Python served both eras on one `/mcp` URL in my runs.
5. **.NET custom request handlers** are `[Experimental]` (MCPEXP002). Their 2026-era results came back without `resultType` in my probe, while `server/discover`, `tools/*` and the TS/Python custom-method results all carried `resultType:"complete"`.
6. **.NET `json-schema-2020-12`** (pending, unscored) fails at 2026-07-28: 3 checks (`sep-2106-composition-keywords-preserved`, `-conditional-keywords-preserved`, `-anchor-keyword-preserved`). It passes for TS and Python.
7. **Bearer verification** has no conformance coverage on the server side for any SDK. All four rows are source-only. Java offers only a generic header-validator hook.
8. **Era pinning on stdio differs:**
   - TypeScript and Python pin a stdio connection to the era of its first message. An `initialize` after 2026 traffic gets `-32022`.
   - .NET answered an `initialize` on the same connection after 2026 requests.
   - This is observed behaviour, not a spec judgement.

## Evidence

### Conformance runs (commands run; raw in `raw/<sdk>-<rev>/`: conformance.txt, summary.txt, server.log.tail; the per-check checks.tar.gz archives are kept outside the repository)

Scored summary from `raw/scored-summary.txt`, computed by `raw/probe-sources/score.py` from checks.json against `conformance list --requirements <rev>`:
```
ts-2026-07-28:   required server scenarios 37/37 passing; checks passed=120 failed=0
ts-2025-11-25:   required server scenarios 30/30 passing; checks passed=70  failed=0
py-2026-07-28:   required server scenarios 37/37 passing; checks passed=120 failed=0
py-2025-11-25:   required server scenarios 30/30 passing; checks passed=70  failed=0
cs-2026-07-28:   required server scenarios 37/37 passing; checks passed=117 failed=0
cs-2025-11-25:   required server scenarios 30/30 passing; checks passed=70  failed=0
java-2026-07-28: required server scenarios 0/37 passing;  checks passed=12  failed=91
java-2025-11-25: required server scenarios 30/30 passing; checks passed=70  failed=0
```

Runner totals, which include unscored extension and pending scenarios:

| Run | Totals | Exit |
|---|---|---|
| TS 2026 | 164 passed / 30 failed | 0 |
| TS 2025 | 84 / 0 | 0 |
| Python 2026 | 169 / 25 | 0 |
| Python 2025 | 84 / 0 | 0 |
| .NET 2026 | 190 / 3 | 0 |
| .NET 2025 | 81 / 0 | 0 |
| Java 2026 | 12 / 142 | 1 |
| Java 2025 | 78 / 1 | 0 |

Exit 0 means every scored scenario passed.

Failing checks per SDK:
- **TS 2026, unscored `tasks-*` only.**
  - `tasks-lifecycle` 1 passed / 8 failed, e.g. `sep-2663-result-type-task-on-create`: "Tool slow_compute not found".
  - `tasks-capability-negotiation` 1/4, `tasks-wire-fields` 1/3, `tasks-request-state-removal` 1/1, `tasks-mrtr-input` 1/3, `tasks-request-headers` 2/3, `tasks-dispatch-and-envelope` 3/6, `tasks-required-task-error` 1/1, `tasks-mrtr-composition` 1/1.
  - The TS fixture registers none of the task tools, which matches `test/conformance/expected-failures.yaml:41-55` ("server SDK does not implement the tasks extension").
- **Python 2026, unscored `tasks-*` only.**
  - `tasks-capability-negotiation`: `tasks-extension-advertised` "capabilities.extensions MUST be advertised"; `sep-2663-tasks-methods-non-declaring` "tasks/get MUST return -32021; got -32601"; `tasks-per-request-meta-opt-in` "got 'complete'".
  - `tasks-lifecycle` 2/7, `tasks-dispatch-and-envelope` 6/3 (`sep-2663-tasks-get-invalid-task-id-32602`: got -32601), `tasks-wire-fields` 1/3, `tasks-mrtr-input` 1/3, `tasks-request-headers` 2/3, `tasks-required-task-error` 1/1, `tasks-request-state-removal` 1/1, `tasks-mrtr-composition` 1/1.
  - This matches the repo baseline `.github/actions/conformance/expected-failures.yml:47-55`.
- **.NET 2026:** only `json-schema-2020-12` (pending, unscored) fails 5/3. In `server-stateless`, `sep-2575-server-sends-{tools,prompts}-list-changed-on-subscription` were SKIPPED ("Server did not declare tools.listChanged/prompts.listChanged capability in server/discover").
- **Java 2026:**
  - `server-stateless` 7/20. Examples: `sep-2575-request-meta-invalid-missing-meta` "Expected error code -32602, got undefined"; `sep-2575-server-implements-discover` "Missing mandatory fields in discover response setup".
  - `caching` 0/7 (tools/list → HTTP 400 `-32601` "Session ID required in mcp-session-id header").
  - All 14 `input-required-result-*` fail.
  - `dns-rebinding-protection` 1/1 (`localhost-host-valid-accepted` got 400).
  - Every tools/resources/prompts scenario fails at the 2026 wire.
- **Java 2025:** only `server-sse-polling` (pending, unscored) failed with `scenario-timeout`.

Subscription-related check IDs that passed (from `checks/server-server-stateless-*/checks.json`):
- TS and Python, all SUCCESS: `sep-2575-server-sends-subscription-ack`, `sep-2575-server-tags-subscription-id`, `sep-2575-server-honors-notification-filter`, `sep-2575-server-sends-tools-list-changed-on-subscription`, `sep-2575-server-sends-prompts-list-changed-on-subscription`.
- .NET: the first three SUCCESS, the last two SKIPPED.

### stdio / custom-method probes (commands run; raw `raw/{ts,py,cs}-stdio-probe.txt`, sources `raw/probe-sources/`)
- **TS** (npm server 2.3.1, `serveStdio` + `McpServer`).
  - 2026: `server/discover` → `supportedVersions:["2026-07-28"]` with `_meta.io.modelcontextprotocol/serverInfo`.
  - `acme/echo` → `{"echoed":"custom","resultType":"complete",…}`.
  - A later `initialize` on the same connection → `-32022` "Unsupported protocol version".
  - Fresh process with 2025 `initialize` → `protocolVersion:"2025-11-25"`; `acme/echo` → `{"echoed":"custom-legacy"}`.
- **Python** (PyPI mcp 2.3.0, `MCPServer(extensions=[Acme()]).run("stdio")`).
  - 2026: discover advertises `capabilities.extensions:{"com.example/acme":{}}`.
  - `acme/echo` → `resultType:"complete"`.
  - `initialize` after modern traffic → `-32022` "connection is serving the 2026-07-28 protocol".
  - Fresh 2025 `initialize` → OK; `acme/echo` OK.
- **.NET** (NuGet ModelContextProtocol 2.2.0, `WithStdioServerTransport()` + `RequestHandlers`).
  - 2026: discover OK (`supportedVersions:["2026-07-28"]`).
  - `acme/echo` → `{"echoed":"custom","_meta":{…}}` with **no `resultType`**.
  - `initialize` afterwards on the same connection → accepted (`protocolVersion:"2025-11-25"`).
  - Fresh 2025 session OK.

### Source / release-notes citations (files read at the pinned tags)

**TypeScript** (`typescript-sdk@fcef852`)
- Tasks:
  - `ROADMAP.md:30` lists Tasks under Extensions, tracked in #2189.
  - `ROADMAP.md:8`: "minus the experimental tasks component (SEP-1686), which v2 does not serve".
  - `test/conformance/expected-failures.yaml:41-55` baselines all `tasks-*` with "server SDK does not implement the tasks extension".
  - `packages/server/CHANGELOG.md:41` (2.3.0, #2599): a server *can* serve `tasks/get`/`tasks/cancel` on a 2026-07-28 connection "when the handler is registered", so a manual hook exists.
  - `packages/core-internal/src/shared/inboundClassification.ts:486-493`: the `Mcp-Name` table includes `tasks/get|update|cancel`.
- Custom methods: `packages/core-internal/src/shared/protocol.ts:1686-1710` (`setRequestHandler('acme/search', {params,result}, handler)` example). Extensions capability: `packages/core-internal/src/types/spec.types.2026-07-28.ts:869`.
- Transports:
  - stdio: `packages/server/src/server/serveStdio.ts:1-30,375` (`serveStdio`, era decision per connection, legacy default `'serve'`) and `packages/server/src/server/stdio.ts:35`.
  - HTTP: `packages/server/src/server/createMcpHandler.ts:681`; listen router at `packages/server/src/server/listenRouter.ts` and `serveStdio.ts:302` (`StdioListenRouter`).
- Bearer:
  - `packages/server/src/server/middleware/bearerAuth.ts:27` (`verifyAccessToken`), `:252` (`requireBearerAuth`).
  - Exported at `packages/server/src/index.ts:42`.
  - Express variant at `packages/middleware/express/src/auth/bearerAuth.ts:26`.

**Python** (`python-sdk@2118f14`)
- Tasks:
  - `ROADMAP.md:13`: "Tasks extension … deferred at 2.0 … tracked in #2806".
  - `.github/actions/conformance/expected-failures.yml:38-55` lists all `tasks-*`.
- Custom methods / extensions:
  - `src/mcp/server/extension.py:58` (`MethodBinding`), `:96` (`Extension`, advertised under `ServerCapabilities.extensions`), `:133` (`methods()`).
  - `src/mcp/server/lowlevel/server.py:484` (`add_request_handler`).
- Bearer:
  - `src/mcp/server/auth/provider.py:124-127` (`TokenVerifier.verify_token`).
  - `src/mcp/server/auth/middleware/bearer_auth.py:44-66` (`BearerAuthBackend`).
  - `src/mcp/server/mcpserver/server.py:168` (`token_verifier=`).
- Transports and subscriptions:
  - `src/mcp/server/mcpserver/server.py:371-389` (`run("stdio" | "streamable-http")`).
  - `src/mcp/server/_streamable_http_modern.py:429` (`subscriptions/listen` on HTTP).
  - `src/mcp/server/subscriptions.py:72,94`.
  - MRTR state: `src/mcp/server/request_state.py:46-80`.

**.NET** (`csharp-sdk@6fa3825`)
- Tasks:
  - `docs/concepts/tasks/tasks.md:13-15`: "provided by the `ModelContextProtocol.Extensions.Tasks` package and require MCP protocol version 2026-07-28 … follows SEP-2663".
  - Limitations at `:349-372`.
  - Methods at `src/ModelContextProtocol.Extensions.Tasks/TasksProtocol.cs:16,21,26` (`tasks/get`, `tasks/update`, `tasks/cancel`).
  - Fixture uses `.WithTasks(` at `tests/ModelContextProtocol.ConformanceServer/Program.cs:56`.
- Core:
  - `src/ModelContextProtocol.Core/Server/McpServerImpl.cs:729` (`server/discover` handler), `:824` (`subscriptions/listen` handler), `:1155-1190` (custom handler registration; built-in methods cannot be overridden).
  - `src/ModelContextProtocol.Core/Server/McpServerOptions.cs:222` (`RequestHandlers`).
  - `src/ModelContextProtocol.Core/Server/McpServerRequestHandler.cs:19` (`[Experimental(Extensibility)]`).
- Era / mode:
  - `docs/concepts/mrtr/mrtr.md:32`: Stateful HTTP refuses 2026-07-28; `StatefulForInitializeClients` hybrid accepts it.
  - `docs/concepts/transports/transports.md:189`: stateless is the default and matches the 2026-07-28 wire.
- Transports: stdio at `src/ModelContextProtocol.Core/Server/StdioServerTransport.cs:9`.
- Bearer:
  - `docs/concepts/filters.md:553-555` (`AddAuthentication("Bearer").AddJwtBearer(...).AddMcp(...)`).
  - `samples/ProtectedMcpServer/Program.cs:35-77`.
  - `src/ModelContextProtocol.AspNetCore/Authentication/McpAuthenticationHandler.cs:11-15`.

**Java** (`java-sdk@c7e1cfe`)
- Version support:
  - `CHANGELOG.md:16`: "2.0.x … 2025-11-25 … Active development".
  - `ROADMAP.md:11` (2.x implements 2025-11-25) and `ROADMAP.md:24-26` ("3.x — 2026-07-28 Spec Support … first 3.0.0 milestone releases are planned for September 2026").
  - Protocol constants: `mcp-core/src/main/java/io/modelcontextprotocol/spec/ProtocolVersions.java:27` (latest is `MCP_2025_11_25`).
  - `spec/McpTransport.java:104-105` and `spec/McpStatelessServerTransport.java:32-33` list nothing newer than 2025-11-25.
  - `grep -rn "2026-07-28\|server/discover\|subscriptions/listen\|input_required\|requestState\|io.modelcontextprotocol/tasks" --include=*.java .` returns no matches.
- Custom methods: `server/McpAsyncServer.java:209-249` (private `prepareRequestHandlers()`, fixed spec methods only). The `McpServer` builder methods are capabilities, completions, immediateExecution, instructions, jsonMapper, jsonSchemaValidator, prompts, requestTimeout, resourceTemplates, resources, rootsChangeHandler(s), serverInfo, strictToolNameValidation, toolCall, tools, uriTemplateManagerFactory and validateToolInputs; none registers a custom method.
- Transports: stdio at `server/transport/StdioServerTransportProvider.java:42`; Streamable HTTP at `server/transport/HttpServletStreamableServerTransportProvider.java:59`.
- Auth hooks:
  - `server/transport/ServerTransportSecurityValidator.java` (`validateHeaders(Map<String,List<String>>)`), called at `HttpServletStreamableServerTransportProvider.java:278`.
  - `ServerTransportSecurityException.java:22` takes `(int statusCode, String message)`.
  - `server/McpTransportContextExtractor.java:17`.
  - `grep -i "bearer\|OAuth"` over `mcp*/src/main` returns no matches.
- Repo claims: the conformance fixture README says "40 out of 40 tests passing", which refers to the older 2025 suite.

### Caveats
- All conformance runs use each SDK's **own fixture server** from the tag, because that is how the suite's `KNOWN_SDKS` drives them. A PASS shows that the SDK *can* meet the scenario. A failure caused by a missing fixture tool can be a fixture gap rather than an SDK gap, so source and baselines were checked for the `tasks-*` failures in TS and Python.
- Conformance suite alpha.12 is newer than what TS/Python pin in CI (alpha.11). Results are unchanged on the scored set.
- Only server roles were tested. Client conformance was not run.
