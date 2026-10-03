// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
// GENERATED FILE — DO NOT EDIT DIRECTLY; tools/contracts/generate.py
package io.ololabs.toolgate.contracts;

/** Signed fleet lifecycle contract; organization deployment trust never grants runtime permission.
 *
 * @param deviceId canonical deviceId value
 * @param generation canonical generation value
 * @param assignments canonical assignments value
 */
@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
public record FleetDesiredSnapshot(
    @com.fasterxml.jackson.annotation.JsonProperty(value = "deviceId", required = true) String deviceId,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "generation", required = true) Long generation,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "assignments", required = true) java.util.List<FleetAssignment> assignments
) {
    /** Reject absent required references and copy collections to retain value semantics.
     *
     * @param deviceId canonical deviceId value
     * @param generation canonical generation value
     * @param assignments canonical assignments value
     */
    public FleetDesiredSnapshot {
        java.util.Objects.requireNonNull(deviceId, "deviceId");
        java.util.Objects.requireNonNull(generation, "generation");
        java.util.Objects.requireNonNull(assignments, "assignments");
        assignments = assignments == null ? null : java.util.List.copyOf(assignments);
    }
}
