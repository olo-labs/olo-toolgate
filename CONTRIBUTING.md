# Contributing to OLO ToolGate

Thank you for helping build ToolGate.

You do not need to understand the whole system before contributing.

## Start in 60 Seconds

```bash
git clone https://github.com/olo-labs/olo-toolgate.git
cd olo-toolgate
make dev
```

The repository should keep `make dev` working as the single supported contributor bootstrap.

Then:

```bash
make check
```

before opening a pull request.

## Pick Your Area

| Area | Primary stack | Start here |
|---|---|---|
| Gateway | Rust | `apps/gateway` |
| Control Plane | Java | `apps/control-plane` |
| Admin UI | React / TypeScript | `apps/admin-ui` |
| Endpoint Client | Rust | `apps/endpoint-client` |
| Marketplace API | Java | `apps/marketplace-api` |
| Marketplace Worker | Java | `apps/marketplace-worker` |
| Marketplace Website | Drupal / PHP | `apps/marketplace-drupal` |
| Shared schemas | JSON Schema/OpenAPI | `packages/contracts/schemas` |
| Documentation | Markdown | `docs` |

## Before Coding

Read:

1. [`ARCHITECTURE.md`](ARCHITECTURE.md)
2. [`docs/architecture/component-map.md`](docs/architecture/component-map.md)
3. Relevant component README.
4. [`docs/security/security-invariants.md`](docs/security/security-invariants.md) for security-sensitive changes.

## Contribution Types

Welcome:

- bug fixes
- tests
- performance work
- security hardening
- UI/UX improvements
- documentation
- new Vault adapters
- new resource extractors
- new local runtimes
- Marketplace improvements
- deployment tooling
- example packages

## Architecture Rule

Before moving responsibility between components, changing the trust model, cryptography, package format, policy semantics, or public API compatibility:

> Create an ADR first.

Template:

```text
docs/adr/000-template.md
```

## Security Rule

Never make a security failure silently become `ALLOW`.

Examples:

```text
policy error        -> BLOCK
approval unavailable -> BLOCK
credential unavailable -> fail request
invalid signature   -> reject
unknown package     -> reject
```

## Schema Rule

Cross-component contracts live under:

```text
packages/contracts/schemas/
```

Do not independently invent equivalent Rust/Java/TypeScript structures.

## Pull Request Checklist

Run:

```bash
make check
```

For Java/Gradle changes also run:

```bash
./gradlew javaCheck
```

Then verify:

- [ ] tests added or updated
- [ ] docs updated
- [ ] security implications reviewed
- [ ] no credentials or private data in logs/examples
- [ ] schema changes are versioned
- [ ] generated bindings are current
- [ ] compatibility impact documented
- [ ] cross-component change has integration/E2E coverage

## Pull Requests

Keep PRs:

- focused
- reviewable
- documented
- independently testable

Large features should be split into a sequence of working changes.

## Commit Messages

Prefer:

```text
gateway: add resource extractor cache
client: verify deployment generation
marketplace: add package advisory endpoint
docs: explain endpoint trust chain
```

## Discussions Before Large Work

Open a GitHub Discussion or issue before spending significant time on:

- new subsystem
- major dependency
- new package runtime
- new persistent service
- public API redesign
- cryptographic change

This avoids parallel incompatible implementations.

## Contributor Conduct

By participating, you agree to [`CODE_OF_CONDUCT.md`](CODE_OF_CONDUCT.md).

## Need Help?

See [`SUPPORT.md`](SUPPORT.md) or open a GitHub Discussion.
