# ToolGate Tool SDK: public delivery and publishing plan

Status: plan, revision 3, not implemented. Revision 3 corrects names against the code at commit `8ea5091` (npm scope `@olo-labs`, release template, version files). Companion to [plan.md](plan.md), which holds the normative design. Revision 2 required the complete contract to be frozen before SDK 0.1 and added the mode capability matrix, mandatory token verification, pinned official MCP SDKs and library composition rules.
Repository: `olo-labs/olo-toolgate` (Apache-2.0, Maven group `io.ololabs.toolgate`, version `0.10.0-dev`).

## 1. Goal

Anyone, inside or outside a ToolGate deployment, can install the SDK from their language's normal registry, generate a working MCP server from a starter in minutes, follow examples and docs, and later package the same code for governed execution in ToolGate without rewriting it.

Success measures for the first public release:
- a new developer has a running MCP tool, connected to an MCP client such as MCP Inspector or Claude Desktop, **in under 10 minutes** from the docs home page;
- every documented snippet compiles and runs in CI;
- the same tool source works in standalone mode and under ToolGate.

## 2. The key decision: standalone first, governance as an add-on

The platform pieces in plan.md (Tool Host, permits, ledger, broker) take time. The community shouldn't wait for them. So each SDK has **two runtimes behind one authoring API**:

| Mode | What it is | Needs ToolGate? | Available from |
|---|---|---|---|
| **Standalone** | The tool runs as an ordinary MCP server (stdio and Streamable HTTP, MCP 2026-07-28, with 2025-11-25 compatibility), built on the official MCP SDK for that language | No | Community preview 0.1 |
| **Governed** | The same code, packaged as a `.tgpkg` and executed by ToolGate's Tool Host or endpoint client under policy, permits, broker egress and audit | Yes | 0.5, when platform milestone M2 lands |

**The release is phased; the architecture is not.** The complete authoring contract, including the parts only governed mode exercises (error model, execution semantics, tasks, effect safety, business keys), is frozen before any SDK code is written: design gates D0 to D2 in [plan.md §22.1](plan.md#221-design-gates-pre-code). Standalone mode implements that whole contract, never a subset with different meaning.

Rules that make this safe and honest (normative text in plan.md):
- **One contract, two runtimes** ([§5.7](plan.md#57-the-complete-authoring-contract-frozen-at-d0-before-any-sdk-code)). Nothing exists in only one mode, so code never changes when it moves to ToolGate.
- **Capability matrix and invariant** ([§5.8](plan.md#58-runtime-modes-standalone-and-governed)). Schemas, API and routes are identical. Globals, secrets, identity, tasks and audit come from local providers in standalone mode and from approved configuration, Vault, the Gateway and durable infrastructure in governed mode. Client tools are unavailable in standalone mode where device services are absent. Moving between modes never silently gains a permission or changes an external effect, and a mode-equivalence test suite proves it on every change.
- **Verified identity** ([§5.9](plan.md#59-identity-in-standalone-mode)). In standalone production mode, claims come only from a verified token, and a server whose tools need claims refuses to start without a verifier. Fixture claims exist only in an explicitly selected development mode bound to stdio or loopback.
- **Standalone mode is not a security boundary.** It applies the same route, retry and effect rules so developers see the same behaviour early, but docs state plainly that network isolation, credential injection and governed audit come only from ToolGate.

## 3. Modular packages

Each SDK splits into small modules so a tool depends only on the authoring API, and the runtime is chosen when it is built or packaged.

| Module (role) | Java (Maven Central, `io.ololabs.toolgate`) | Python (PyPI) | TypeScript (npm) | .NET (NuGet) |
|---|---|---|---|---|
| **Authoring API**: annotations, `ToolContext`, types | `toolgate-sdk-api` | `toolgate-sdk` | `@olo-labs/toolgate-sdk` | `OloLabs.ToolGate.Sdk` |
| **Descriptor generation** | `toolgate-sdk-processor` (annotation processor, KSP for Kotlin) | part of `toolgate-sdk` (introspection) | part of `@olo-labs/toolgate-sdk` (Zod / TS types) | `OloLabs.ToolGate.Sdk.Generators` (source generator, Roslyn analyzer) |
| **Standalone runtime** (MCP server) | `toolgate-sdk-runtime-mcp` | `toolgate-sdk[mcp]` | `@olo-labs/toolgate-runtime-mcp` | `OloLabs.ToolGate.Runtime.Mcp` |
| **Governed runtime** (tool protocol v2, launcher) | `toolgate-sdk-runtime-host` | `toolgate-sdk[host]` | `@olo-labs/toolgate-runtime-host` | `OloLabs.ToolGate.Runtime.Host` |
| **Routes HTTP client** | `toolgate-sdk-http` | `toolgate-sdk[http]` | `@olo-labs/toolgate-http` | `OloLabs.ToolGate.Http` |
| **Testing**: `ToolHarness`, mock egress | `toolgate-sdk-testing` | `toolgate-testing` | `@olo-labs/toolgate-testing` | `OloLabs.ToolGate.Testing` |
| **Build integration** | Gradle plugin `io.ololabs.toolgate.sdk`, `toolgate-maven-plugin`, `toolgate-bom` | `toolgate` CLI wheel, Hatch/Poetry hook | `@olo-labs/toolgate-cli` (+ platform packages) | `ToolGate.Cli` dotnet tool, MSBuild targets |
| **ToolGate protocol layer**: Tasks extension, `requestState`, bearer verification, and the Java 2026-07-28 core where the official SDK lacks it ([plan.md §5.10](plan.md#510-official-mcp-sdk-dependencies-and-toolgate-adapters)) | `toolgate-protocol` | part of `toolgate-sdk` | `@olo-labs/toolgate-protocol` | `OloLabs.ToolGate.Protocol` |
| **Starters** | Gradle init template, Maven archetype | `toolgate init` (via `pipx`) | `npm create toolgate-tool` | `OloLabs.ToolGate.Templates` (`dotnet new toolgate-tool`) |

Language-neutral artifacts:
- **`toolgate` CLI** (Rust, plan.md §4): GitHub Releases, Homebrew, Scoop and winget, and wrapped by each registry above, so air-gapped mirrors work.
- **Spec and schemas** (`tool-sdk/spec`): published as versioned JSON Schema at a stable URL and as `toolgate-spec` packages in each registry, so community tools and other languages can validate descriptors.
- **Conformance kit**: the descriptor fixtures and runtime cases, published so third-party SDKs (Go, Rust, Kotlin-native) can certify themselves.

**Dependency rule.** Tool code imports only the authoring API (and optionally the HTTP client). Runtimes are added by the build plugin or starter. A tool library can therefore be shared as an ordinary Maven, PyPI, npm or NuGet package and reused by other tool projects. Its descriptor fragment is merged at packaging under the composition rules in [plan.md §7.7](plan.md#77-library-composition): any conflict fails packaging unless an explicit, reviewed mapping resolves it, and merging never broadens access.

**Official MCP SDKs are pinned**, each version changed only after a conformance re-run: TypeScript `@modelcontextprotocol/server` 2.3.1, Python `mcp` 2.3.0, .NET `ModelContextProtocol` 2.2.0, Java `io.modelcontextprotocol.sdk:mcp` 2.0.1. Conformance runs on 2026-10-10 (`@modelcontextprotocol/conformance@0.2.0-alpha.12`): TypeScript, Python and .NET pass 37 of 37 required 2026-07-28 server scenarios; Java passes 0, so Java's 2026-07-28 support comes from the ToolGate protocol core. Only .NET has Tasks, so ToolGate's Tasks adapter is used in all four. Results, gaps and adapter contracts are in plan.md §5.10 and [the 2026-10-10 compatibility record](../../tool-sdk/spec/sdk-compat/2026-10-10/results.md).

**Names to reserve now:** the `io.ololabs.toolgate` namespace on Maven Central, the `toolgate-*` names on PyPI, the existing `@olo-labs` npm scope (already used by `@olo-labs/toolgate-contracts`), and the `OloLabs.ToolGate` prefix on NuGet. I haven't checked whether these are free; reserving them is the first step in §8.

## 4. Versioning and compatibility

- **SemVer per SDK**, released independently, each declaring the spec version and conformance version it passes (plan.md §5.6).
- **0.x is a public preview.** Breaking changes are allowed between minors, with migration notes. **1.0** happens only after the design freeze (plan.md revision 8) and full conformance.
- **Java BOM** and matching version lines in the other SDKs keep modules aligned.
- **Support matrix** on the docs site: SDK version × language runtime × MCP protocol versions × ToolGate versions.
- Deprecations last at least two minor versions after 1.0.

## 5. Release pipeline

One workflow per SDK under `.github/workflows/sdk-<lang>-release.yml`, path-filtered so a Python change doesn't build the platform. It follows the repo's existing release template `release-foundation.yml` (protected environment, Trivy CycloneDX SBOM, `actions/attest-build-provenance`, SHA-pinned actions) and is gated by the reusable `admission.yml`. Each SDK has its own version file under `tool-sdk/<lang>/VERSION`, separate from the root `VERSION`, and its own changelog. Public-registry trusted publishing is new to the repo: today packages go only to GitHub Packages.

| Stage | What runs |
|---|---|
| Pull request | SDK unit tests, contract and descriptor suite, examples build and test, docs snippet check |
| Release candidate | Protocol compatibility suite, the SDK's slice of the language matrix, MCP Inspector smoke test of every example, reproducible-build check |
| Publish | **Trusted publishing with OIDC**, so no long-lived registry tokens: PyPI trusted publisher, npm with `--provenance`, NuGet trusted publishing, Maven Central through the Central Portal with GPG signing. Sigstore signatures, CycloneDX SBOM and SLSA provenance attached to every artifact and to the GitHub Release. |
| Approval | A protected `sdk-release` environment needing two maintainers |
| After publish | Docs version cut, changelog, a starter refresh PR, and an install smoke test from the public registries |

## 6. Examples

Examples live in `tool-sdk/examples/<name>/<lang>` and are **real projects**, built and tested in CI against the SDK in the same commit. Docs include code from them, so docs and examples can't drift.

| # | Example | Teaches | Languages |
|---|---|---|---|
| 1 | `hello` | Smallest tool, typed parameters, `toolgate dev`, connecting an MCP client | All four |
| 2 | `weather-api` | Named HTTP routes, a destination setting, an API key secret | All four |
| 3 | `orders` | JWT claims, effect safety, a business key, error handling (plan.md §5.1) | All four |
| 4 | `report-task` | A long-running tool as a task, progress, cancellation | All four |
| 5 | `hotfolder-checksum` | A client tool on a device | Java, .NET |
| 6 | `shared-library` | Publishing a reusable tool library and composing it into another package | Java, TypeScript |
| 7 | `openapi-import` | Turning an OpenAPI file into tools with the CLI, no code | CLI |
| 8 | `wrap-mcp-server` | Governing an existing MCP server through ToolGate (proxy) | CLI |
| 9 | `testing` | `ToolHarness`, mock egress, `@Example` cases | All four |

Each example has a README covering what it shows, how to run it standalone, how to connect MCP Inspector or Claude Desktop, and, from 0.5, how to package and upload it to ToolGate Quickstart.

## 7. Documentation

**Site:** versioned docs built from `tool-sdk/docs` with MkDocs Material (Markdown like the existing `docs/`, search, versioning through `mike`), published by CI. A `docs.` subdomain of the project site would host it; that domain choice is open.

**Structure (Diátaxis):**
- **Get started:** one 5-minute quickstart per language (install, `init`, run, connect a client).
- **Tutorials:** build `weather-api` end to end; take a tool from standalone to ToolGate.
- **How-to guides:** routes and destinations; secrets; JWT claims; tasks; client tools; testing; publishing a tool library; packaging and upload; migrating from the plain MCP SDK; OpenAPI import; wrapping an MCP server.
- **Reference:** annotations and `ToolContext` per language; API docs generated from source (Javadoc, pdoc, TypeDoc, DocFX); CLI reference generated from the CLI; manifest and descriptor schema; error codes.
- **Concepts:** standalone versus governed; effect safety; the egress model and transport matrix; versioning and descriptor generations.
- **Spec:** package format, invocation protocol v2, conformance, for people writing new SDKs.

**Docs quality gates:** every code block comes from a compiled example or a doctest; link checking; a "time to first tool" test that runs each quickstart from scratch in CI.

## 8. Phased delivery

| Phase | Ships | Depends on | Exit criteria |
|---|---|---|---|
| **P-0: Groundwork** | Registry names reserved; docs site and CI shell; contribution model. No SDK code. | Design gates D0, D1 and D2 passed: the **complete** authoring contract, contracts and trust model frozen (plan.md §22.1) | Empty packages publishable from CI with signatures and provenance; official SDK feature matrix verified |
| **P-1: Community preview 0.1** | All four SDKs in **standalone mode**, implementing the full frozen contract: authoring API, descriptor generation, error model, standalone runtime with local task and reservation stores, verified identity, routes HTTP client, testing; starters; examples 1 to 4 and 9; quickstarts and reference docs | P-0 | A newcomer reaches a working tool in under 10 minutes in each language; examples pass the MCP Inspector smoke test; the standalone side of the mode-equivalence fixtures passes |
| **P-2: 0.3** | Shared tool libraries (example 6); `toolgate dev` with hot reload; CLI `validate` and `pack` producing `.tgpkg` offline; community examples gallery | P-1 | A package built by each SDK passes the contract and descriptor suite |
| **P-3: 0.5, governed** | Governed runtime and launcher; `toolgate upload` to ToolGate; client tools (example 5); OpenAPI and MCP proxy examples (7, 8); the standalone-to-ToolGate tutorial | Platform milestone M2 (Tool Host, permits, broker) and M3 (proxy, OpenAPI) in plan.md §22 | Each example runs unchanged standalone and under ToolGate Quickstart, and the full mode-equivalence suite passes |
| **P-4: 1.0** | Stability promise, full conformance, support matrix, deprecation policy in force | Design freeze complete; conformance suites green | All four SDKs pass the published conformance version |
| **P-5: Marketplace** | `toolgate publish`, publisher verification, community listings | Marketplace modules 12 to 15 | A community package listed and installed into a tenant |

The ordering in plan.md §22 stays for the platform. This plan only moves the **standalone SDKs** earlier, because they don't depend on platform milestones. I'll fold this into §22 in revision 8.

## 9. Community and governance

- **Licence:** Apache-2.0, as the repository already uses.
- **Contributions:** Developer Certificate of Origin (`Signed-off-by`) rather than a CLA, since `CONTRIBUTING.md` defines neither today; maintainers can change this.
- `CODEOWNERS` per SDK, labelled good-first-issues, GitHub Discussions for questions, and an issue template that asks for SDK and protocol versions.
- **Security:** reports through the existing `SECURITY.md`; SDK advisories also published to GitHub Security Advisories and each registry.
- **Community examples gallery:** a curated list of community tools built with the SDK, separate from the Marketplace, with a template for adding an entry.
- **Third-party SDKs:** anyone can build an SDK for another language; passing the published conformance kit earns a "conformant" listing.

## 10. Open points with defaults

| Point | Default I picked |
|---|---|
| Where the SDK code lives | `tool-sdk/` in `olo-toolgate`, as originally specified, with path-filtered CI. It can move to its own repository later without changing package names. |
| Package names | As in §3, under `io.ololabs.toolgate` (existing Maven group), `@olo-labs` (existing npm scope) and `OloLabs.ToolGate` |
| Docs domain | A `docs.` subdomain of the project site, not yet chosen |
| .NET in the preview | Included in 0.1, because the official C# SDK is Tier 1. If it slips, it ships in 0.2 rather than holding the others back. |
