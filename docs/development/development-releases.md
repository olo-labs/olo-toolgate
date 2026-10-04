# Copyright 2026 OLO Labs
# SPDX-License-Identifier: Apache-2.0
# Development releases from main

The development-release workflow runs after Control CI completes successfully on
main. It waits up to 30 minutes for Control, Gateway, Quickstart, Foundation and
Endpoint Client CI to pass for the same commit. Failed, cancelled or missing gates
prevent publication. Manual dispatch on main follows the same gates.

Configure the `development-release` GitHub environment. Repository secrets also
work: `DOCKERHUB_USERNAME` and `DOCKERHUB_TOKEN` are required. The optional repository
variable `DOCKERHUB_NAMESPACE` overrides the Docker Hub username as the destination.
The token must have push access to the destination repositories:

- `olo-toolgate-gateway`
- `olo-toolgate-control`
- `olo-toolgate-quickstart`

Images come from checksummed CI image archives, without rebuilding. Each gets a
`dev-<12-character-commit>` tag. A successful build of the current main commit also
updates `dev`. No production `latest` tag is changed.

GitHub Releases receives an immutable prerelease named
`dev-<full-commit>-<publishing-run-id>`, including six native client archives
(Windows, macOS and Linux; x64 and ARM64), checksums, embedded client SBOMs, image
SBOMs, raw contracts, Java JAR/source/Javadoc/POM and development-release.json.
The native archives retain their tested VERSION; release metadata ties them to
the exact commit and CI run IDs. The image digests are recorded in the metadata.
No installer/runtime signing is claimed by this workflow.

The Maven library is `io.ololabs.toolgate:toolgate-contracts`. Each publishing run
uses `<core-version>-dev.<publishing-run-id>.g<12-character-commit>`, avoiding reuse
of the checked-in development coordinate. Source version changes occur only in the
CI checkout. Java contract tests run before remote publication.

Maven defaults to `https://maven.pkg.github.com/olo-labs/olo-toolgate`, authenticated
with the CI GITHUB_TOKEN. Consumers need GitHub Packages read access. To use another
repository, set `MAVEN_REPOSITORY_URL`, `MAVEN_USERNAME` and `MAVEN_PASSWORD`. For a
custom repository use both Maven secrets; the workflow does not implement Maven
Central staging or signing. Existing tag-based GA publication remains separate.

Publishing requires GitHub Actions write access for repository contents/packages.
Existing package access controls must allow this repository's GITHUB_TOKEN. Keep
credentials in GitHub Secrets, never in files. Review environment protection and
Docker Hub repository access before the first publication.

If a run fails after one registry succeeds, partial artifacts may exist. Do not
claim an atomic multi-registry release. Inspect the publishing run before retrying;
Maven coordinates and GitHub prerelease names are unique per workflow run. A new
manual run on main can recover after credentials or registry access are corrected.

Release notes and licenses
-------------------------

`python tools/contracts/version.py` generates `RELEASE-NOTES.md` from canonical
versions and CHANGELOG.md; `--check` detects drift. Regenerate bindings afterward
with `python tools/contracts/generate.py` to synchronize library documentation.
GitHub release creation also generates commit/contributor notes automatically.

Client archives, raw contract bundles and language packages include LICENSE,
NOTICE.md and RELEASE-NOTES.md. Every Java main/source/Javadoc JAR includes them
under META-INF. Container images expose notes at
`/usr/share/doc/olo-toolgate/RELEASE-NOTES.md` and license notices under
`/usr/share/licenses/olo-toolgate/`. Existing dependency notices and SBOMs are
preserved. Release notes and notices are included in asset checksums.
