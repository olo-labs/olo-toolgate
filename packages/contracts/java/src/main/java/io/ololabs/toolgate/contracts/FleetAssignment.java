// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
// GENERATED FILE — DO NOT EDIT DIRECTLY; tools/contracts/generate.py
package io.ololabs.toolgate.contracts;

/** Signed fleet lifecycle contract; organization deployment trust never grants runtime permission.
 *
 * @param release canonical release value
 * @param desiredPresence canonical desiredPresence value
 */
@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
public record FleetAssignment(
    @com.fasterxml.jackson.annotation.JsonProperty(value = "release", required = true) FleetPackageRelease release,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "desiredPresence", required = true) Boolean desiredPresence
) {
    /** Reject absent required references and copy collections to retain value semantics.
     *
     * @param release canonical release value
     * @param desiredPresence canonical desiredPresence value
     */
    public FleetAssignment {
        java.util.Objects.requireNonNull(release, "release");
        java.util.Objects.requireNonNull(desiredPresence, "desiredPresence");
    }
}
