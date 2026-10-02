# OLO ToolGate Control Plane

Copyright 2026 OLO Labs. Licensed under Apache-2.0.

Runtime dependencies retain their original license/notice files inside the JARs
in `/app/lib`. The exact dependency versions are recorded in
`/usr/share/licenses/olo-toolgate/third-party/gradle.lockfile`; the release pipeline
publishes a CycloneDX SBOM. Eclipse Temurin retains its JRE legal notices under
`/opt/java/openjdk/legal`. Dependency licenses are checked by the repository scan
gate using Maven metadata. No private verification or signing keys are included.

Unmodified Jakarta/Parsson API/runtime dependencies use their EPL-2.0 option.
Exact versions, license URLs and matching source artifact URLs are distributed in
`/usr/share/licenses/olo-toolgate/third-party/dependency-license-reviews.json`.
The Quarkus build-only classfile backport is not distributed in the runtime image.
Upstream notices remain inside the original dependency JARs.
