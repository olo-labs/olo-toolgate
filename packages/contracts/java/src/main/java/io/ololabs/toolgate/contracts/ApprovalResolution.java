// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
// GENERATED FILE — DO NOT EDIT DIRECTLY; tools/contracts/generate.py
package io.ololabs.toolgate.contracts;

/** Authenticated Control result for this exact Gateway attempt; lease fields occur only on a successful grant.
 *
 * @param approvalId canonical approvalId value
 * @param state canonical state value
 * @param input canonical input value
 * @param policyVersion canonical policyVersion value
 * @param permitId canonical permitId value
 * @param permitExpiresAtUnixMs canonical permitExpiresAtUnixMs value
 */
@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
public record ApprovalResolution(
    @com.fasterxml.jackson.annotation.JsonProperty(value = "approvalId", required = true) String approvalId,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "state", required = true) ApprovalState state,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "input", required = true) PolicyInput input,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "policyVersion", required = true) String policyVersion,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "permitId", required = false) String permitId,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "permitExpiresAtUnixMs", required = false) Long permitExpiresAtUnixMs
) {
    /** Reject absent required references and copy collections to retain value semantics.
     *
     * @param approvalId canonical approvalId value
     * @param state canonical state value
     * @param input canonical input value
     * @param policyVersion canonical policyVersion value
     * @param permitId canonical permitId value
     * @param permitExpiresAtUnixMs canonical permitExpiresAtUnixMs value
     */
    public ApprovalResolution {
        java.util.Objects.requireNonNull(approvalId, "approvalId");
        java.util.Objects.requireNonNull(state, "state");
        java.util.Objects.requireNonNull(input, "input");
        java.util.Objects.requireNonNull(policyVersion, "policyVersion");
    }
}
