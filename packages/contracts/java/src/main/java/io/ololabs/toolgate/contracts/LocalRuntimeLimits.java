// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
// GENERATED FILE — DO NOT EDIT DIRECTLY; tools/contracts/generate.py
package io.ololabs.toolgate.contracts;

/** Bounded local sandbox budget; limits never grant host access.
 *
 * @param timeoutMs canonical timeoutMs value
 * @param memoryMiB canonical memoryMiB value
 * @param maxInputBytes canonical maxInputBytes value
 * @param maxOutputBytes canonical maxOutputBytes value
 */
@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
public record LocalRuntimeLimits(
    @com.fasterxml.jackson.annotation.JsonProperty(value = "timeoutMs", required = true) Long timeoutMs,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "memoryMiB", required = true) Long memoryMiB,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "maxInputBytes", required = true) Long maxInputBytes,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "maxOutputBytes", required = true) Long maxOutputBytes
) {
    /** Reject absent required references and copy collections to retain value semantics.
     *
     * @param timeoutMs canonical timeoutMs value
     * @param memoryMiB canonical memoryMiB value
     * @param maxInputBytes canonical maxInputBytes value
     * @param maxOutputBytes canonical maxOutputBytes value
     */
    public LocalRuntimeLimits {
        java.util.Objects.requireNonNull(timeoutMs, "timeoutMs");
        java.util.Objects.requireNonNull(memoryMiB, "memoryMiB");
        java.util.Objects.requireNonNull(maxInputBytes, "maxInputBytes");
        java.util.Objects.requireNonNull(maxOutputBytes, "maxOutputBytes");
    }
}
