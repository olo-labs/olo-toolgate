// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
// GENERATED FILE — DO NOT EDIT DIRECTLY; tools/contracts/generate.py
package io.ololabs.toolgate.contracts;

/** Signed fleet lifecycle contract; organization deployment trust never grants runtime permission.
 *
 * @param generation canonical generation value
 * @param ready canonical ready value
 * @param packages canonical packages value
 * @param error canonical error value
 */
@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
public record FleetClientStatus(
    @com.fasterxml.jackson.annotation.JsonProperty(value = "generation", required = true) Long generation,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "ready", required = true) Boolean ready,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "packages", required = true) java.util.List<ReportedPackage> packages,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "error", required = false) ErrorCode error
) {
    /** Reject absent required references and copy collections to retain value semantics.
     *
     * @param generation canonical generation value
     * @param ready canonical ready value
     * @param packages canonical packages value
     * @param error canonical error value
     */
    public FleetClientStatus {
        java.util.Objects.requireNonNull(generation, "generation");
        java.util.Objects.requireNonNull(ready, "ready");
        java.util.Objects.requireNonNull(packages, "packages");
        packages = packages == null ? null : java.util.List.copyOf(packages);
    }
}
