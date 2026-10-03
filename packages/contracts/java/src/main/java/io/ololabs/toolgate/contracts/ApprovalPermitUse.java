// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
// GENERATED FILE — DO NOT EDIT DIRECTLY; tools/contracts/generate.py
package io.ololabs.toolgate.contracts;

/** Gateway-only atomic lease consumption; repeat use never grants again.
 *
 * @param approvalId canonical approvalId value
 * @param permitId canonical permitId value
 * @param input canonical input value
 * @param policyVersion canonical policyVersion value
 */
@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
public record ApprovalPermitUse(
    @com.fasterxml.jackson.annotation.JsonProperty(value = "approvalId", required = true) String approvalId,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "permitId", required = true) String permitId,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "input", required = true) PolicyInput input,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "policyVersion", required = true) String policyVersion
) {
    /** Reject absent required references and copy collections to retain value semantics.
     *
     * @param approvalId canonical approvalId value
     * @param permitId canonical permitId value
     * @param input canonical input value
     * @param policyVersion canonical policyVersion value
     */
    public ApprovalPermitUse {
        java.util.Objects.requireNonNull(approvalId, "approvalId");
        java.util.Objects.requireNonNull(permitId, "permitId");
        java.util.Objects.requireNonNull(input, "input");
        java.util.Objects.requireNonNull(policyVersion, "policyVersion");
    }
}
