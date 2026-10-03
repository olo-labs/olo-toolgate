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

## Module 09 managed package deployment

Product/contracts/chart advance to 0.9.0-dev; Control requires Flyway V6.
Fleet is opt-in and requires enrolled 0.9 clients, independent release/organization
keys, direct device mTLS and an external immutable HTTPS descriptor mirror.
Earlier clients retain their existing tools and cannot consume fleet assignments.
Module 09 uses immutable signed descriptors and monotonic device generations.
See the [fleet usage, upgrade and debug guide](../client/package-deployment.md) for external trust/store
configuration, health-gated activation, rollback/uninstall and client compatibility.

## Module 10 custom tool authoring

Product/contracts/chart advance to 0.10.0-dev and Control requires Flyway V7.
Fourteen builder/source models are additive. Optional signed tool source is
consumed only by 0.10 clients; authored packages set minimumClientVersion 0.10.0-dev.
Older package fixtures omit source and remain compatible. Organization-signed test
leases use a distinct JWS type and direct mTLS identity; release signing remains
independent. Sealed versions survive upgrades/restores and cannot be edited.
Python/Node/PowerShell/Shell source is confined; Native/Java/.NET use reviewed
image artifacts. No new Helm configuration or Gateway wire version is introduced.
See [production/debug authoring](../client/tool-builder.md) and its native limits.

## Module 11 Quickstart

This additive composition retains the unreleased 0.10.0-dev product/contracts
version and existing native download versions. Gateway runtime v2 and signed
policy format 2 are unchanged. Quickstart uses explicit Quarkus build profile
`quickstart`, local layout 1 and SQLite migrations 1–2; production Control keeps
PostgreSQL/Flyway V7. Newer unknown layouts/schema versions reject startup.
The first Quickstart package has no earlier released image compatibility claim;
its tested schema-1 volume upgrades to schema 2 without resetting identity or
defaults. Image rollback requires a pre-upgrade backup in a new volume.
Separate native OS/ARM service and managed-runtime certification limits remain
in Modules 06–10; shipping verified archives does not certify those paths.
