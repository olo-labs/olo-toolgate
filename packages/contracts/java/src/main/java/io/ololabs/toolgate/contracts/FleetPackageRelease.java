// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
// GENERATED FILE — DO NOT EDIT DIRECTLY; tools/contracts/generate.py
package io.ololabs.toolgate.contracts;

/** Signed fleet lifecycle contract; organization deployment trust never grants runtime permission.
 *
 * @param packageId canonical packageId value
 * @param version canonical version value
 * @param manifestDigest canonical manifestDigest value
 * @param sizeBytes canonical sizeBytes value
 * @param release canonical release value
 */
@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
public record FleetPackageRelease(
    @com.fasterxml.jackson.annotation.JsonProperty(value = "packageId", required = true) String packageId,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "version", required = true) String version,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "manifestDigest", required = true) String manifestDigest,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "sizeBytes", required = true) Long sizeBytes,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "release", required = true) FleetSignedDocument release
) {
    /** Reject absent required references and copy collections to retain value semantics.
     *
     * @param packageId canonical packageId value
     * @param version canonical version value
     * @param manifestDigest canonical manifestDigest value
     * @param sizeBytes canonical sizeBytes value
     * @param release canonical release value
     */
    public FleetPackageRelease {
        java.util.Objects.requireNonNull(packageId, "packageId");
        java.util.Objects.requireNonNull(version, "version");
        java.util.Objects.requireNonNull(manifestDigest, "manifestDigest");
        java.util.Objects.requireNonNull(sizeBytes, "sizeBytes");
        java.util.Objects.requireNonNull(release, "release");
    }
}
