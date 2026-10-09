// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
// GENERATED FILE — DO NOT EDIT DIRECTLY; tools/contracts/generate.py
package io.ololabs.toolgate.contracts;

/** Separate exact-operation and configuration approval records. Reviewers cannot expand the approved binding.
 *
 * @param id canonical id value
 * @param approvalType canonical approvalType value
 * @param invocationId canonical invocationId value
 * @param requestDigest canonical requestDigest value
 * @param authorizationEpoch canonical authorizationEpoch value
 * @param directoryRevision canonical directoryRevision value
 * @param obligationIds canonical obligationIds value
 * @param reviews canonical reviews value
 * @param state canonical state value
 * @param revision canonical revision value
 * @param expiresAtUnixMs canonical expiresAtUnixMs value
 */
@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
public record EnterpriseApproval(
    @com.fasterxml.jackson.annotation.JsonProperty(value = "id", required = true) String id,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "approvalType", required = true) EnterpriseApprovalType approvalType,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "invocationId", required = true) String invocationId,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "requestDigest", required = true) String requestDigest,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "authorizationEpoch", required = true) Long authorizationEpoch,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "directoryRevision", required = true) Long directoryRevision,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "obligationIds", required = true) java.util.List<String> obligationIds,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "reviews", required = true) java.util.List<EnterpriseApprovalReview> reviews,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "state", required = true) EnterpriseApprovalState state,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "revision", required = true) Long revision,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "expiresAtUnixMs", required = true) Long expiresAtUnixMs
) {
    /** Reject absent required references and copy collections to retain value semantics.
     *
     * @param id canonical id value
     * @param approvalType canonical approvalType value
     * @param invocationId canonical invocationId value
     * @param requestDigest canonical requestDigest value
     * @param authorizationEpoch canonical authorizationEpoch value
     * @param directoryRevision canonical directoryRevision value
     * @param obligationIds canonical obligationIds value
     * @param reviews canonical reviews value
     * @param state canonical state value
     * @param revision canonical revision value
     * @param expiresAtUnixMs canonical expiresAtUnixMs value
     */
    public EnterpriseApproval {
        java.util.Objects.requireNonNull(id, "id");
        java.util.Objects.requireNonNull(approvalType, "approvalType");
        java.util.Objects.requireNonNull(invocationId, "invocationId");
        java.util.Objects.requireNonNull(requestDigest, "requestDigest");
        java.util.Objects.requireNonNull(authorizationEpoch, "authorizationEpoch");
        java.util.Objects.requireNonNull(directoryRevision, "directoryRevision");
        java.util.Objects.requireNonNull(obligationIds, "obligationIds");
        obligationIds = obligationIds == null ? null : java.util.List.copyOf(obligationIds);
        java.util.Objects.requireNonNull(reviews, "reviews");
        reviews = reviews == null ? null : java.util.List.copyOf(reviews);
        java.util.Objects.requireNonNull(state, "state");
        java.util.Objects.requireNonNull(revision, "revision");
        java.util.Objects.requireNonNull(expiresAtUnixMs, "expiresAtUnixMs");
    }
}
