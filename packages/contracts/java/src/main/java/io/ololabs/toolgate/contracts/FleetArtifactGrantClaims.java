// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
// GENERATED FILE — DO NOT EDIT DIRECTLY; tools/contracts/generate.py
package io.ololabs.toolgate.contracts;

/** Signed fleet lifecycle contract; organization deployment trust never grants runtime permission.
 *
 * @param tenantId canonical tenantId value
 * @param serverId canonical serverId value
 * @param deviceId canonical deviceId value
 * @param generation canonical generation value
 * @param manifestDigest canonical manifestDigest value
 * @param sizeBytes canonical sizeBytes value
 * @param grantId canonical grantId value
 * @param issuedAtUnixMs canonical issuedAtUnixMs value
 * @param expiresAtUnixMs canonical expiresAtUnixMs value
 */
@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
public record FleetArtifactGrantClaims(
    @com.fasterxml.jackson.annotation.JsonProperty(value = "tenantId", required = true) String tenantId,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "serverId", required = true) String serverId,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "deviceId", required = true) String deviceId,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "generation", required = true) Long generation,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "manifestDigest", required = true) String manifestDigest,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "sizeBytes", required = true) Long sizeBytes,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "grantId", required = true) String grantId,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "issuedAtUnixMs", required = true) Long issuedAtUnixMs,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "expiresAtUnixMs", required = true) Long expiresAtUnixMs
) {
    /** Reject absent required references and copy collections to retain value semantics.
     *
     * @param tenantId canonical tenantId value
     * @param serverId canonical serverId value
     * @param deviceId canonical deviceId value
     * @param generation canonical generation value
     * @param manifestDigest canonical manifestDigest value
     * @param sizeBytes canonical sizeBytes value
     * @param grantId canonical grantId value
     * @param issuedAtUnixMs canonical issuedAtUnixMs value
     * @param expiresAtUnixMs canonical expiresAtUnixMs value
     */
    public FleetArtifactGrantClaims {
        java.util.Objects.requireNonNull(tenantId, "tenantId");
        java.util.Objects.requireNonNull(serverId, "serverId");
        java.util.Objects.requireNonNull(deviceId, "deviceId");
        java.util.Objects.requireNonNull(generation, "generation");
        java.util.Objects.requireNonNull(manifestDigest, "manifestDigest");
        java.util.Objects.requireNonNull(sizeBytes, "sizeBytes");
        java.util.Objects.requireNonNull(grantId, "grantId");
        java.util.Objects.requireNonNull(issuedAtUnixMs, "issuedAtUnixMs");
        java.util.Objects.requireNonNull(expiresAtUnixMs, "expiresAtUnixMs");
    }
}
