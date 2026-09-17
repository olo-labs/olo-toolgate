# CI/CD Requirements

## Pull Request CI

Run only relevant jobs where possible, but never skip required gates.

Expected jobs over project maturity:

```text
docs
contracts
gradle
rust
typescript
php/drupal
unit
integration
security
E2E
container-build
container-scan
helm-lint
k8s-render-validation
license
secret-scan
SBOM
```

## Main Branch

Main must remain releasable.

No direct insecure release credential use.

## Release Workflow

On signed/tagged release:

```text
validate version
build contracts
publish contracts
build containers
scan containers
generate SBOM
generate provenance
sign where configured
build endpoint clients
package Helm
publish Helm OCI
push containers
create GitHub Release
upload checksums/assets
publish compatibility matrix/release notes
```

## Permissions

GitHub Actions should use minimal permissions.

Prefer OIDC federation to cloud registries over long-lived secrets.

## Caching

Cache dependencies/build outputs safely.

Do not cache secrets.

## Environments

Use protected environments for:

```text
Maven publication
container publication
Helm publication
signing
production Marketplace deployment
```
