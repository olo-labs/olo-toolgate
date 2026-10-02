# Release Process

## Purpose

Build/test/sign three customer containers, native clients, SBOM/provenance/checksums, compatibility matrix and separate Marketplace platform artifacts.

Module 02 adds `.github/workflows/control.yml`: build, real PostgreSQL/API tests,
Helm render, non-root/read-only container smoke, HIGH/CRITICAL image scan,
CycloneDX SBOM and isolated Kind install/upgrade/rollback. The protected
`control-release` environment publishes the exact saved and scanned image to
`ghcr.io/olo-labs/olo-toolgate-control:<VERSION>` and attaches provenance. Matching
version tags are required. Remote publication and GitHub jobs were not invoked by
local module development. Existing foundation release gates publish contracts,
chart and raw/checksummed assets. Configure signing before claiming signed images.

See [Control deployment](../control-plane/upgrades.md) and
[compatibility](../architecture/compatibility.md). Runtime JARs retain upstream
notices and the image includes dependency versions and Temurin legal notices.

Module 03 embeds the console in the same Control image. CI adds locked npm build,
component/transport coverage, canonical operation drift, browser/axe E2E against
real Control/PostgreSQL, a reproducible versioned UI tar/checksums and production
npm SBOM. Runtime MIT notices are included in the served artifact. Image scanning
does not discover minified-library metadata, so the verified production
npm graph is merged into the image SBOM before checksumming and attestation. No separate UI
image/chart registry is introduced. Console and API upgrade/rollback together;
see [console operations](../control-plane/admin-ui.md).
