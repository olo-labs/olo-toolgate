// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
// GENERATED FILE — DO NOT EDIT DIRECTLY; tools/contracts/generate.py
package io.ololabs.toolgate.contracts;

/** Bounded redacted device activity for OS-authenticated local status inspection.
 *
 * @param timestampUnixMs canonical timestampUnixMs value
 * @param name canonical name value
 * @param state canonical state value
 */
@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
public record ClientActivityEvent(
    @com.fasterxml.jackson.annotation.JsonProperty(value = "timestampUnixMs", required = true) Long timestampUnixMs,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "name", required = true) String name,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "state", required = true) String state
) {
    /** Reject absent required references and copy collections to retain value semantics.
     *
     * @param timestampUnixMs canonical timestampUnixMs value
     * @param name canonical name value
     * @param state canonical state value
     */
    public ClientActivityEvent {
        java.util.Objects.requireNonNull(timestampUnixMs, "timestampUnixMs");
        java.util.Objects.requireNonNull(name, "name");
        java.util.Objects.requireNonNull(state, "state");
    }
}
