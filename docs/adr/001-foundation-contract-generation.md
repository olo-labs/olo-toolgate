# ADR 001: Canonical schemas and structural contract bindings

Status: accepted for Module 00, 2026-10-02.

Cross-component models must retain stable package identities after repository
separation. A checked-in JSON Schema Draft 2020-12 set under
`packages/contracts/schemas/v1` owns their wire shapes. OpenAPI and event folders
are reserved canonical sources; this module defines no HTTP endpoint or event.

A small deterministic generator emits Java records/enums, Rust Serde models,
TypeScript readonly interfaces/unions and PHP readonly DTOs/backed enums. It
supports named references, scalars, enums, objects, arrays and optional fields.
Unsupported structural shapes fail generation. It does not implement a second
JSON Schema validator. All consumers must validate canonical schemas before
trusting input; generated constructors and serializers provide structural checks.
TypeScript interfaces disappear at runtime and provide no input validation.

Security objects reject unknown fields. Missing decisions have no default and
unknown decisions fail. Marketplace release, organization assignment and runtime
decision are separate models. Signature strings are evidence to be verified in
later owning modules; no model or fixture is a verification implementation.

Closed objects require negotiated wire revision changes when adding fields an
old reader cannot accept. Compatibility fixtures and frozen schema snapshots are
test baselines, not alternate service model sources. Before stable release, define
and test an N/N-1 support window; the initial release has no prior stable version.

Java uses Jackson for lightweight interoperable serialization and JUnit only for
tests. Rust uses Serde/serde_json. TypeScript has no runtime dependencies; PHP
uses its built-in JSON and enum support. Version catalogs and lockfiles own
dependency resolution. Generator outputs contain no timestamps or absolute paths.

Java services select either project resolution or an exclusive local Maven
repository. The artifact proof checks real JAR/POM/sources/Javadoc files, runs all
service tests with the same imports and rejects an empty repository. This avoids
a cached workspace dependency masking a broken publication.

Helm is metadata-only until real workloads exist. Docker may run development
toolchains on Windows; it is optional for the core gates when native tools are
installed. Dependency/secret scan hooks use isolated scanner images. No runtime
image or service is introduced by this decision.
