// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
// GENERATED FILE — DO NOT EDIT DIRECTLY; tools/contracts/generate.py
package io.ololabs.toolgate.contracts;

/** Independent decision for one exact operation obligation and approval revision.
 *
 * @param expectedRevision canonical expectedRevision value
 * @param obligationId canonical obligationId value
 * @param decision canonical decision value
 */
@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
public record EnterpriseApprovalDecisionRequest(
    @com.fasterxml.jackson.annotation.JsonProperty(value = "expectedRevision", required = true) Long expectedRevision,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "obligationId", required = true) String obligationId,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "decision", required = true) EnterpriseReviewDecision decision
) {
    /** Reject absent required references and copy collections to retain value semantics.
     *
     * @param expectedRevision canonical expectedRevision value
     * @param obligationId canonical obligationId value
     * @param decision canonical decision value
     */
    public EnterpriseApprovalDecisionRequest {
        java.util.Objects.requireNonNull(expectedRevision, "expectedRevision");
        java.util.Objects.requireNonNull(obligationId, "obligationId");
        java.util.Objects.requireNonNull(decision, "decision");
    }
}
