// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
// GENERATED FILE — DO NOT EDIT DIRECTLY; tools/contracts/generate.py
package io.ololabs.toolgate.contracts;

/** Versioned desired package assignments reconciled by an endpoint.
 *
 * @param deviceId canonical deviceId value
 * @param revision canonical revision value
 * @param assignments canonical assignments value
 */
@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
public record DesiredState(
    @com.fasterxml.jackson.annotation.JsonProperty(value = "deviceId", required = true) String deviceId,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "revision", required = true) Long revision,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "assignments", required = true) java.util.List<DeploymentAssignment> assignments
) {
    /** Reject absent required references and copy collections to retain value semantics.
     *
     * @param deviceId canonical deviceId value
     * @param revision canonical revision value
     * @param assignments canonical assignments value
     */
    public DesiredState {
        java.util.Objects.requireNonNull(deviceId, "deviceId");
        java.util.Objects.requireNonNull(revision, "revision");
        java.util.Objects.requireNonNull(assignments, "assignments");
        assignments = assignments == null ? null : java.util.List.copyOf(assignments);
    }
}
