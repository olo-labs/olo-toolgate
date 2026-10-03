// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
// GENERATED FILE — DO NOT EDIT DIRECTLY; tools/contracts/generate.py
package io.ololabs.toolgate.contracts;

/** Signed fleet lifecycle contract; organization deployment trust never grants runtime permission.
 *
 * @param formatVersion canonical formatVersion value
 * @param tenantId canonical tenantId value
 * @param serverId canonical serverId value
 * @param deviceId canonical deviceId value
 * @param generation canonical generation value
 * @param issuedAtUnixMs canonical issuedAtUnixMs value
 * @param expiresAtUnixMs canonical expiresAtUnixMs value
 * @param assignments canonical assignments value
 */
@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
public record FleetDesiredDocument(
    @com.fasterxml.jackson.annotation.JsonProperty(value = "formatVersion", required = true) Long formatVersion,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "tenantId", required = true) String tenantId,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "serverId", required = true) String serverId,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "deviceId", required = true) String deviceId,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "generation", required = true) Long generation,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "issuedAtUnixMs", required = true) Long issuedAtUnixMs,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "expiresAtUnixMs", required = true) Long expiresAtUnixMs,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "assignments", required = true) java.util.List<FleetAssignment> assignments
) {
    /** Reject absent required references and copy collections to retain value semantics.
     *
     * @param formatVersion canonical formatVersion value
     * @param tenantId canonical tenantId value
     * @param serverId canonical serverId value
     * @param deviceId canonical deviceId value
     * @param generation canonical generation value
     * @param issuedAtUnixMs canonical issuedAtUnixMs value
     * @param expiresAtUnixMs canonical expiresAtUnixMs value
     * @param assignments canonical assignments value
     */
    public FleetDesiredDocument {
        java.util.Objects.requireNonNull(formatVersion, "formatVersion");
        java.util.Objects.requireNonNull(tenantId, "tenantId");
        java.util.Objects.requireNonNull(serverId, "serverId");
        java.util.Objects.requireNonNull(deviceId, "deviceId");
        java.util.Objects.requireNonNull(generation, "generation");
        java.util.Objects.requireNonNull(issuedAtUnixMs, "issuedAtUnixMs");
        java.util.Objects.requireNonNull(expiresAtUnixMs, "expiresAtUnixMs");
        java.util.Objects.requireNonNull(assignments, "assignments");
        assignments = assignments == null ? null : java.util.List.copyOf(assignments);
    }
}
