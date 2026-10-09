// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
// GENERATED FILE — DO NOT EDIT DIRECTLY; tools/contracts/generate.py
package io.ololabs.toolgate.contracts;

/** Maker and independent eligible checker resolve an unknown effect; evidence stays external.
 *
 * @param id canonical id value
 * @param invocationId canonical invocationId value
 * @param requesterUserId canonical requesterUserId value
 * @param request canonical request value
 * @param requestDigest canonical requestDigest value
 * @param revision canonical revision value
 * @param reviewerUserId canonical reviewerUserId value
 * @param state canonical state value
 */
@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
public record EnterpriseReconciliation(
    @com.fasterxml.jackson.annotation.JsonProperty(value = "id", required = true) String id,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "invocationId", required = true) String invocationId,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "requesterUserId", required = true) String requesterUserId,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "request", required = true) EnterpriseReconciliationRequest request,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "requestDigest", required = true) String requestDigest,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "revision", required = true) Long revision,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "reviewerUserId", required = false) String reviewerUserId,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "state", required = true) EnterpriseReconciliationState state
) {
    /** Reject absent required references and copy collections to retain value semantics.
     *
     * @param id canonical id value
     * @param invocationId canonical invocationId value
     * @param requesterUserId canonical requesterUserId value
     * @param request canonical request value
     * @param requestDigest canonical requestDigest value
     * @param revision canonical revision value
     * @param reviewerUserId canonical reviewerUserId value
     * @param state canonical state value
     */
    public EnterpriseReconciliation {
        java.util.Objects.requireNonNull(id, "id");
        java.util.Objects.requireNonNull(invocationId, "invocationId");
        java.util.Objects.requireNonNull(requesterUserId, "requesterUserId");
        java.util.Objects.requireNonNull(request, "request");
        java.util.Objects.requireNonNull(requestDigest, "requestDigest");
        java.util.Objects.requireNonNull(revision, "revision");
        java.util.Objects.requireNonNull(state, "state");
    }
}
