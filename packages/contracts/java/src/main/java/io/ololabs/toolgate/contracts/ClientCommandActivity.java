// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
// GENERATED FILE — DO NOT EDIT DIRECTLY; tools/contracts/generate.py
package io.ololabs.toolgate.contracts;

/** Bounded redacted device activity for OS-authenticated local status inspection.
 *
 * @param id canonical id value
 * @param name canonical name value
 * @param startedAtUnixMs canonical startedAtUnixMs value
 * @param finishedAtUnixMs canonical finishedAtUnixMs value
 * @param state canonical state value
 * @param progressPercent canonical progressPercent value
 */
@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
public record ClientCommandActivity(
    @com.fasterxml.jackson.annotation.JsonProperty(value = "id", required = true) String id,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "name", required = true) String name,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "startedAtUnixMs", required = true) Long startedAtUnixMs,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "finishedAtUnixMs", required = false) Long finishedAtUnixMs,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "state", required = true) ClientCommandState state,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "progressPercent", required = false) Long progressPercent
) {
    /** Reject absent required references and copy collections to retain value semantics.
     *
     * @param id canonical id value
     * @param name canonical name value
     * @param startedAtUnixMs canonical startedAtUnixMs value
     * @param finishedAtUnixMs canonical finishedAtUnixMs value
     * @param state canonical state value
     * @param progressPercent canonical progressPercent value
     */
    public ClientCommandActivity {
        java.util.Objects.requireNonNull(id, "id");
        java.util.Objects.requireNonNull(name, "name");
        java.util.Objects.requireNonNull(startedAtUnixMs, "startedAtUnixMs");
        java.util.Objects.requireNonNull(state, "state");
    }
}
