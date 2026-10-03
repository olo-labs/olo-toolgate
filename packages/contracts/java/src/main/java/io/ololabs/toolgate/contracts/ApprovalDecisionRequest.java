// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
// GENERATED FILE — DO NOT EDIT DIRECTLY; tools/contracts/generate.py
package io.ololabs.toolgate.contracts;

/** Optimistic human decision; duration is required only for temporary approval.
 *
 * @param decision canonical decision value
 * @param expectedRevision canonical expectedRevision value
 * @param durationMs canonical durationMs value
 */
@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
public record ApprovalDecisionRequest(
    @com.fasterxml.jackson.annotation.JsonProperty(value = "decision", required = true) ApprovalChoice decision,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "expectedRevision", required = true) Long expectedRevision,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "durationMs", required = false) Long durationMs
) {
    /** Reject absent required references and copy collections to retain value semantics.
     *
     * @param decision canonical decision value
     * @param expectedRevision canonical expectedRevision value
     * @param durationMs canonical durationMs value
     */
    public ApprovalDecisionRequest {
        java.util.Objects.requireNonNull(decision, "decision");
        java.util.Objects.requireNonNull(expectedRevision, "expectedRevision");
    }
}
