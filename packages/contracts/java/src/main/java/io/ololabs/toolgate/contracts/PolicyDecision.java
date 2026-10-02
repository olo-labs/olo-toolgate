// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
// GENERATED FILE — DO NOT EDIT DIRECTLY; tools/contracts/generate.py
package io.ololabs.toolgate.contracts;

/** Explicit wire decision, with no implicit ALLOW default. ASK requires approval.
 *
 * @param decision canonical decision value
 * @param reason canonical reason value
 * @param policyVersion canonical policyVersion value
 * @param requestId canonical requestId value
 */
@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
public record PolicyDecision(
    @com.fasterxml.jackson.annotation.JsonProperty(value = "decision", required = true) Decision decision,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "reason", required = true) DecisionReason reason,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "policyVersion", required = true) String policyVersion,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "requestId", required = true) String requestId
) {
    /** Reject absent required references and copy collections to retain value semantics.
     *
     * @param decision canonical decision value
     * @param reason canonical reason value
     * @param policyVersion canonical policyVersion value
     * @param requestId canonical requestId value
     */
    public PolicyDecision {
        java.util.Objects.requireNonNull(decision, "decision");
        java.util.Objects.requireNonNull(reason, "reason");
        java.util.Objects.requireNonNull(policyVersion, "policyVersion");
        java.util.Objects.requireNonNull(requestId, "requestId");
    }
}
