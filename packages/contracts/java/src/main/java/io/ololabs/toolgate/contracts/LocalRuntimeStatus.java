// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
// GENERATED FILE — DO NOT EDIT DIRECTLY; tools/contracts/generate.py
package io.ololabs.toolgate.contracts;

/** Redacted runtime readiness and capability, never engine output or credential contents.
 *
 * @param runtimeId canonical runtimeId value
 * @param kind canonical kind value
 * @param state canonical state value
 * @param version canonical version value
 */
@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
public record LocalRuntimeStatus(
    @com.fasterxml.jackson.annotation.JsonProperty(value = "runtimeId", required = true) String runtimeId,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "kind", required = true) LocalRuntimeKind kind,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "state", required = true) LocalRuntimeState state,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "version", required = true) String version
) {
    /** Reject absent required references and copy collections to retain value semantics.
     *
     * @param runtimeId canonical runtimeId value
     * @param kind canonical kind value
     * @param state canonical state value
     * @param version canonical version value
     */
    public LocalRuntimeStatus {
        java.util.Objects.requireNonNull(runtimeId, "runtimeId");
        java.util.Objects.requireNonNull(kind, "kind");
        java.util.Objects.requireNonNull(state, "state");
        java.util.Objects.requireNonNull(version, "version");
    }
}
