// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
// GENERATED FILE — DO NOT EDIT DIRECTLY; tools/contracts/generate.py
package io.ololabs.toolgate.contracts;

/** Signed fleet lifecycle contract; organization deployment trust never grants runtime permission.
 *
 * @param id canonical id value
 * @param packageId canonical packageId value
 * @param version canonical version value
 * @param deviceIds canonical deviceIds value
 * @param desiredPresence canonical desiredPresence value
 * @param percentage canonical percentage value
 */
@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
public record FleetRolloutRequest(
    @com.fasterxml.jackson.annotation.JsonProperty(value = "id", required = true) String id,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "packageId", required = true) String packageId,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "version", required = true) String version,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "deviceIds", required = true) java.util.List<String> deviceIds,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "desiredPresence", required = true) Boolean desiredPresence,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "percentage", required = true) Long percentage
) {
    /** Reject absent required references and copy collections to retain value semantics.
     *
     * @param id canonical id value
     * @param packageId canonical packageId value
     * @param version canonical version value
     * @param deviceIds canonical deviceIds value
     * @param desiredPresence canonical desiredPresence value
     * @param percentage canonical percentage value
     */
    public FleetRolloutRequest {
        java.util.Objects.requireNonNull(id, "id");
        java.util.Objects.requireNonNull(packageId, "packageId");
        java.util.Objects.requireNonNull(version, "version");
        java.util.Objects.requireNonNull(deviceIds, "deviceIds");
        deviceIds = deviceIds == null ? null : java.util.List.copyOf(deviceIds);
        java.util.Objects.requireNonNull(desiredPresence, "desiredPresence");
        java.util.Objects.requireNonNull(percentage, "percentage");
    }
}
