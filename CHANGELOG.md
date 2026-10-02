# Changelog

All notable project changes should be documented here.

The project follows semantic versioning once stable versioning begins.

## Unreleased

### Added

- Initial architecture and contributor documentation.

### Changed

### Fixed

### Security

## 0.3.0-dev — Module 02 Control Plane backend

Java 21/Quarkus tenant-scoped directory CRUD, typed identifiers and shared Control
schemas/bindings; PostgreSQL/Flyway, atomic audit/replay, retired identities and
optimistic revisions; signed administrative JWTs, safe JSON/YAML import/export,
bounded pages and private observability. Added non-root production image, external
PostgreSQL Helm resources, protected GHCR pipeline, real database/HTTP tests and
container/cluster smoke. No custom execution, policy publication or later module
is implemented.

## 0.2.0-dev — Module 01 gateway core

Stateless Rust authorization with validated static identity/policy, exact extraction,
deny precedence, acknowledged sanitized audit, bounded ingress, separate probes/
metrics, structured tracing and graceful shutdown. Added shared runtime types,
offline schema embedding and Gateway OpenAPI. Added non-root image, protected
GHCR workflow, gateway Helm workload, Kubernetes render/cluster smoke and benchmark.
No execution, permits, bundles, approvals or later module is implemented.

## 0.1.0-dev — Module 00 foundation

Canonical v1 contracts and deterministic Java/Rust/TypeScript/PHP bindings;
Gradle/Cargo workspace builds and local Java publication proof; fixture, drift,
license and security gates; foundation CI and protected release assets; empty
Helm skeleton with validated values. No runtime module or workload is included.
