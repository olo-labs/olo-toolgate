// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
// GENERATED FILE — DO NOT EDIT DIRECTLY; tools/contracts/generate.py
package io.ololabs.toolgate.contracts;

/** Durable tenant approval status; identity/resource and digest only, never raw arguments.
 *
 * @param id canonical id value
 * @param revision canonical revision value
 * @param state canonical state value
 * @param input canonical input value
 * @param policyVersion canonical policyVersion value
 * @param createdAtUnixMs canonical createdAtUnixMs value
 * @param expiresAtUnixMs canonical expiresAtUnixMs value
 * @param decidedBy canonical decidedBy value
 * @param decidedAtUnixMs canonical decidedAtUnixMs value
 */
@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
public record ApprovalRecord(
    @com.fasterxml.jackson.annotation.JsonProperty(value = "id", required = true) String id,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "revision", required = true) Long revision,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "state", required = true) ApprovalState state,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "input", required = true) PolicyInput input,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "policyVersion", required = true) String policyVersion,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "createdAtUnixMs", required = true) Long createdAtUnixMs,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "expiresAtUnixMs", required = true) Long expiresAtUnixMs,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "decidedBy", required = false) String decidedBy,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "decidedAtUnixMs", required = false) Long decidedAtUnixMs
) {
    /** Reject absent required references and copy collections to retain value semantics.
     *
     * @param id canonical id value
     * @param revision canonical revision value
     * @param state canonical state value
     * @param input canonical input value
     * @param policyVersion canonical policyVersion value
     * @param createdAtUnixMs canonical createdAtUnixMs value
     * @param expiresAtUnixMs canonical expiresAtUnixMs value
     * @param decidedBy canonical decidedBy value
     * @param decidedAtUnixMs canonical decidedAtUnixMs value
     */
    public ApprovalRecord {
        java.util.Objects.requireNonNull(id, "id");
        java.util.Objects.requireNonNull(revision, "revision");
        java.util.Objects.requireNonNull(state, "state");
        java.util.Objects.requireNonNull(input, "input");
        java.util.Objects.requireNonNull(policyVersion, "policyVersion");
        java.util.Objects.requireNonNull(createdAtUnixMs, "createdAtUnixMs");
        java.util.Objects.requireNonNull(expiresAtUnixMs, "expiresAtUnixMs");
    }
}
