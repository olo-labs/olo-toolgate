// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
// GENERATED FILE — DO NOT EDIT DIRECTLY; tools/contracts/generate.py
package io.ololabs.toolgate.contracts;

/** Audited independent review of one bound obligation.
 *
 * @param obligationId canonical obligationId value
 * @param reviewerUserId canonical reviewerUserId value
 * @param decision canonical decision value
 * @param decidedAtUnixMs canonical decidedAtUnixMs value
 */
@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
public record EnterpriseApprovalReview(
    @com.fasterxml.jackson.annotation.JsonProperty(value = "obligationId", required = true) String obligationId,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "reviewerUserId", required = true) String reviewerUserId,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "decision", required = true) EnterpriseReviewDecision decision,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "decidedAtUnixMs", required = true) Long decidedAtUnixMs
) {
    /** Reject absent required references and copy collections to retain value semantics.
     *
     * @param obligationId canonical obligationId value
     * @param reviewerUserId canonical reviewerUserId value
     * @param decision canonical decision value
     * @param decidedAtUnixMs canonical decidedAtUnixMs value
     */
    public EnterpriseApprovalReview {
        java.util.Objects.requireNonNull(obligationId, "obligationId");
        java.util.Objects.requireNonNull(reviewerUserId, "reviewerUserId");
        java.util.Objects.requireNonNull(decision, "decision");
        java.util.Objects.requireNonNull(decidedAtUnixMs, "decidedAtUnixMs");
    }
}
