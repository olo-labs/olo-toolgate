// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
// GENERATED FILE — DO NOT EDIT DIRECTLY; tools/contracts/generate.py
package io.ololabs.toolgate.contracts;

/** Bounded per-service execution counters and sandbox state.
 *
 * @param ready canonical ready value
 * @param successfulExecutions canonical successfulExecutions value
 * @param failedExecutions canonical failedExecutions value
 * @param runtimes canonical runtimes value
 */
@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
public record LocalRuntimeHealth(
    @com.fasterxml.jackson.annotation.JsonProperty(value = "ready", required = true) Boolean ready,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "successfulExecutions", required = true) Long successfulExecutions,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "failedExecutions", required = true) Long failedExecutions,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "runtimes", required = true) java.util.List<LocalRuntimeStatus> runtimes
) {
    /** Reject absent required references and copy collections to retain value semantics.
     *
     * @param ready canonical ready value
     * @param successfulExecutions canonical successfulExecutions value
     * @param failedExecutions canonical failedExecutions value
     * @param runtimes canonical runtimes value
     */
    public LocalRuntimeHealth {
        java.util.Objects.requireNonNull(ready, "ready");
        java.util.Objects.requireNonNull(successfulExecutions, "successfulExecutions");
        java.util.Objects.requireNonNull(failedExecutions, "failedExecutions");
        java.util.Objects.requireNonNull(runtimes, "runtimes");
        runtimes = runtimes == null ? null : java.util.List.copyOf(runtimes);
    }
}
