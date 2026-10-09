// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
// GENERATED FILE — DO NOT EDIT DIRECTLY; tools/contracts/generate.py
package io.ololabs.toolgate.contracts;

/** Bounded redacted device activity for OS-authenticated local status inspection.
 *
 * @param active canonical active value
 * @param lastCommand canonical lastCommand value
 * @param events canonical events value
 * @param logAvailable canonical logAvailable value
 */
@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
public record ClientActivity(
    @com.fasterxml.jackson.annotation.JsonProperty(value = "active", required = true) java.util.List<ClientCommandActivity> active,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "lastCommand", required = false) ClientCommandActivity lastCommand,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "events", required = true) java.util.List<ClientActivityEvent> events,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "logAvailable", required = true) Boolean logAvailable
) {
    /** Reject absent required references and copy collections to retain value semantics.
     *
     * @param active canonical active value
     * @param lastCommand canonical lastCommand value
     * @param events canonical events value
     * @param logAvailable canonical logAvailable value
     */
    public ClientActivity {
        java.util.Objects.requireNonNull(active, "active");
        active = active == null ? null : java.util.List.copyOf(active);
        java.util.Objects.requireNonNull(events, "events");
        events = events == null ? null : java.util.List.copyOf(events);
        java.util.Objects.requireNonNull(logAvailable, "logAvailable");
    }
}
