# Compatibility

Every release should publish:

```text
Gateway version
Control Plane version
Endpoint Client versions
Quickstart version
Package schema version
Policy bundle version
Marketplace API version
MCP versions
```

Packages declare minimum compatible:

```text
Control Plane
Gateway when relevant
Endpoint Client
runtime
OS/architecture
schema reader
```

Unsupported packages may be visible but cannot be installed.

## Module 04 compatibility baseline

| Component/artifact | Version | Compatibility |
|---|---|---|
| Control Plane | 0.5.0-dev | Java 21, Quarkus 3.40.1, PostgreSQL 17.11 tested; Flyway V3 required |
| Shared contracts | 0.5.0-dev | Canonical v1; additive Control/bundle models; frozen foundation corpus compatible |
| Gateway | product 0.5.0-dev | Runtime v1 retained; signed bundle format v1/RS256 supported |
| Helm chart | 0.5.0-dev | Services opt-in; empty default; external database/secrets |
| Config snapshot | formatVersion 1 | Same-tenant import with optimistic tenant revision; unknown versions rejected |
| Admin UI | 0.5.0-dev development baseline | Embedded Control v1 consumer; same-origin signed-token session; no wire/schema change |
| Policy bundle | formatVersion 1 | Exact JWS RS256, tenant-bound monotonic sequence; forward rollback; earlier runtime has no signed reader |
| Endpoint/Marketplace | pending | No cross-component workflow delivered by this module |

Module 05: product/contracts/chart 0.5.0-dev, PostgreSQL Flyway V4, signed policies
formats 1 and 2. ASK requires format 2 and runtime v2. Earlier Gateway releases
reject format 2 safely and cannot process approvals/permits. All sixteen new wire
types are additive. ControlPolicy widens its administrative decision restriction to
ASK, which its existing generated Decision already decodes. Earlier administrators
may reject ASK on schema validation; runtime readers require format 2. The original
foundation frozen corpus remains unchanged.
Gateway-issued permits use separate RS256 trust and strict Control-backed consumption.

## Module 07

Product, shared contracts and chart are 0.7.0-dev. Endpoint tools add protocol 2;
the frozen protocol 1 health/enrollment/check-in corpus remains supported. The
canonical built-in catalog and public download manifest are additive v1 models.
Control still requires Flyway V5. Gateway runtime v2 is required for client tools
and ASK; source/destination operations require separate exact-path grants. Older
clients lack tool IPC and fail unsupported operations safely. Discovery SemVer
minimum accepts a newer compatible client; origin, CA and identity verification
remain mandatory. The Windows local cross-build uses GNU x64 while native release
CI produces MSVC x64/ARM64; macOS/Linux CI produces native x64/ARM64 archives.

## Module 08 managed execution

Product/contracts/chart advance to 0.8.0-dev. Twelve new execution types and IPC
revision 3 are additive; frozen identity/built-in protocols remain unchanged.
No database or REST route is added. Explicit local registration and Linux OCI
engine are opt-in. Batch/CMD and WASM are unsupported. Existing HotFolder stays
available without an engine. Read docs/client/local-runtimes.md for migration.
