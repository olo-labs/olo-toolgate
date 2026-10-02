// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
// GENERATED FILE — DO NOT EDIT DIRECTLY; tools/contracts/generate.py
package io.ololabs.toolgate.contracts;

/** Immutable artifact identity; digest must be verified by consumers.
 *
 * @param uri canonical uri value
 * @param sha256 canonical sha256 value
 * @param sizeBytes canonical sizeBytes value
 */
@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
public record ArtifactDescriptor(
    @com.fasterxml.jackson.annotation.JsonProperty(value = "uri", required = true) String uri,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "sha256", required = true) String sha256,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "sizeBytes", required = true) Long sizeBytes
) {
    /** Reject absent required references and copy collections to retain value semantics.
     *
     * @param uri canonical uri value
     * @param sha256 canonical sha256 value
     * @param sizeBytes canonical sizeBytes value
     */
    public ArtifactDescriptor {
        java.util.Objects.requireNonNull(uri, "uri");
        java.util.Objects.requireNonNull(sha256, "sha256");
        java.util.Objects.requireNonNull(sizeBytes, "sizeBytes");
    }
}
