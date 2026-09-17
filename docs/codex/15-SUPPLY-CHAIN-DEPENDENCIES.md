# Supply Chain and Dependencies

## Dependencies

Before adding:

```text
maintenance
license
security history
size
transitive graph
necessity
```

must be considered.

## Locking

Use ecosystem lock/version catalogs where appropriate.

## SBOM

Generate SBOM for:

```text
release containers
endpoint client artifacts
Marketplace worker/API
Quickstart
```

## Container Images

Pin base images by controlled version and preferably digest in release pipelines.

## Package Integrity

Marketplace packages use:

```text
immutable version
manifest digest
artifact digests
Marketplace signature
```

## Organization Mirror

Imported packages should be mirrorable to organization-controlled artifact store.

## Build Provenance

Release workflow should generate provenance/attestation compatible with the selected signing tooling.
