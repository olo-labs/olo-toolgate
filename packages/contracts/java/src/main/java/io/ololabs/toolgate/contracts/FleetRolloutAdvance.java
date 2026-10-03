// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
// GENERATED FILE — DO NOT EDIT DIRECTLY; tools/contracts/generate.py
package io.ololabs.toolgate.contracts;

/** Signed fleet lifecycle contract; organization deployment trust never grants runtime permission.
 *
 * @param expectedRevision canonical expectedRevision value
 * @param percentage canonical percentage value
 */
@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
public record FleetRolloutAdvance(
    @com.fasterxml.jackson.annotation.JsonProperty(value = "expectedRevision", required = true) Long expectedRevision,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "percentage", required = true) Long percentage
) {
    /** Reject absent required references and copy collections to retain value semantics.
     *
     * @param expectedRevision canonical expectedRevision value
     * @param percentage canonical percentage value
     */
    public FleetRolloutAdvance {
        java.util.Objects.requireNonNull(expectedRevision, "expectedRevision");
        java.util.Objects.requireNonNull(percentage, "percentage");
    }
}
