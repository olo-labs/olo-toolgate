# Release Process

Module 07's Control workflow calls the six-target native client workflow before
embedding the verified public bundle. Native jobs run unit/security checks and
real system-service installation smoke. Control verifies anonymous browser
downloads and enrolled TLS/Gateway tools alongside image/cluster gates.
The protected tagged publish job uploads immutable native archives, checksums
and manifest to GitHub Releases and attests the bundle. Assets are never
overwritten. Native signing hooks require external OS/KMS identities;
development binaries are unsigned and macOS notarization is not configured.
See [the native operations guide](../client/hotfolder.md).

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

Module 04 adds the reusable signed-policy compatibility workflow as a required
dependency of both image publishers and the foundation Maven/chart/assets release.
It builds both production images and exercises genuine JWS publication, verified
Gateway updates, rollback, key overlap, Control outage, expiry and restart recovery.
The release matrix includes bundle format/algorithm and Flyway V3. Image scans and
CycloneDX SBOMs include ring, rustls and reqwest. Runtime bundle signing keys remain
external application inputs; they are unrelated to CI provenance or image signing.
See [bundle operations](../control-plane/policy-bundles.md).

Module 05 extends the mandatory reusable compatibility gate with genuine ASK,
human decision, signature/binding, atomic consumption, outage and revocation E2E.
It builds production images once, then also executes prior signed-bundle E2E and
approval Helm install/upgrade/rotation negatives. Existing protected Maven/GHCR/OCI
publication, scans, SBOMs and provenance remain required. No remote credentials are
introduced for local checks; `make approval-e2e` produces local evidence.

## Module 08 runtime dependencies

Native client archives include managed runtime documentation and the fixed seccomp
profile. Interpreter/tool image layers remain separate approved OCI dependencies;
record their exact digest, runtime version, license inventory, SBOM, vulnerability
scan and provenance before deployment. First-use pull requires an explicitly
approved pinned image and local system-accessible engine. No engine installer or
registry secret is embedded. Native Linux CI executes real adapter/sandbox tests.
Batch/CMD has no secure backend yet; native cross-platform gates must run before
claiming module completion.
