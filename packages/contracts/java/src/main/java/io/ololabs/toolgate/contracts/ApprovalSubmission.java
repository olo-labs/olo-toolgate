// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
// GENERATED FILE — DO NOT EDIT DIRECTLY; tools/contracts/generate.py
package io.ololabs.toolgate.contracts;

/** Gateway-only normalized ASK request. No raw arguments or credentials.
 *
 * @param input canonical input value
 * @param policyVersion canonical policyVersion value
 */
@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
public record ApprovalSubmission(
    @com.fasterxml.jackson.annotation.JsonProperty(value = "input", required = true) PolicyInput input,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "policyVersion", required = true) String policyVersion
) {
    /** Reject absent required references and copy collections to retain value semantics.
     *
     * @param input canonical input value
     * @param policyVersion canonical policyVersion value
     */
    public ApprovalSubmission {
        java.util.Objects.requireNonNull(input, "input");
        java.util.Objects.requireNonNull(policyVersion, "policyVersion");
    }
}
