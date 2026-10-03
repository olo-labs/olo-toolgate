// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
// GENERATED FILE — DO NOT EDIT DIRECTLY; tools/contracts/generate.py
package io.ololabs.toolgate.contracts;

/** Signed fleet lifecycle contract; organization deployment trust never grants runtime permission.
 *
 * @param generation canonical generation value
 * @param manifestDigest canonical manifestDigest value
 */
@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
public record FleetArtifactGrantRequest(
    @com.fasterxml.jackson.annotation.JsonProperty(value = "generation", required = true) Long generation,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "manifestDigest", required = true) String manifestDigest
) {
    /** Reject absent required references and copy collections to retain value semantics.
     *
     * @param generation canonical generation value
     * @param manifestDigest canonical manifestDigest value
     */
    public FleetArtifactGrantRequest {
        java.util.Objects.requireNonNull(generation, "generation");
        java.util.Objects.requireNonNull(manifestDigest, "manifestDigest");
    }
}
