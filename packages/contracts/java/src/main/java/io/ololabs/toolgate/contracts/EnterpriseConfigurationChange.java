// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
// GENERATED FILE — DO NOT EDIT DIRECTLY; tools/contracts/generate.py
package io.ololabs.toolgate.contracts;

/** Reviewed configuration workflow bound to the exact group graph revision.
 *
 * @param id canonical id value
 * @param requesterUserId canonical requesterUserId value
 * @param command canonical command value
 * @param requestDigest canonical requestDigest value
 * @param directoryRevision canonical directoryRevision value
 * @param authorizationEpoch canonical authorizationEpoch value
 * @param state canonical state value
 * @param revision canonical revision value
 * @param createdAtUnixMs canonical createdAtUnixMs value
 * @param expiresAtUnixMs canonical expiresAtUnixMs value
 * @param requiredReviews canonical requiredReviews value
 * @param impact canonical impact value
 * @param affectedGroups canonical affectedGroups value
 * @param affectedIndividuals canonical affectedIndividuals value
 * @param reviews canonical reviews value
 */
@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
public record EnterpriseConfigurationChange(
    @com.fasterxml.jackson.annotation.JsonProperty(value = "id", required = true) String id,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "requesterUserId", required = true) String requesterUserId,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "command", required = true) EnterpriseConfigurationCommand command,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "requestDigest", required = true) String requestDigest,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "directoryRevision", required = true) Long directoryRevision,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "authorizationEpoch", required = true) Long authorizationEpoch,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "state", required = true) EnterpriseConfigurationState state,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "revision", required = true) Long revision,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "createdAtUnixMs", required = true) Long createdAtUnixMs,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "expiresAtUnixMs", required = true) Long expiresAtUnixMs,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "requiredReviews", required = true) Long requiredReviews,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "impact", required = true) java.util.List<EnterpriseConfigurationImpact> impact,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "affectedGroups", required = true) java.util.List<String> affectedGroups,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "affectedIndividuals", required = true) java.util.List<String> affectedIndividuals,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "reviews", required = true) java.util.List<EnterpriseConfigurationReview> reviews
) {
    /** Reject absent required references and copy collections to retain value semantics.
     *
     * @param id canonical id value
     * @param requesterUserId canonical requesterUserId value
     * @param command canonical command value
     * @param requestDigest canonical requestDigest value
     * @param directoryRevision canonical directoryRevision value
     * @param authorizationEpoch canonical authorizationEpoch value
     * @param state canonical state value
     * @param revision canonical revision value
     * @param createdAtUnixMs canonical createdAtUnixMs value
     * @param expiresAtUnixMs canonical expiresAtUnixMs value
     * @param requiredReviews canonical requiredReviews value
     * @param impact canonical impact value
     * @param affectedGroups canonical affectedGroups value
     * @param affectedIndividuals canonical affectedIndividuals value
     * @param reviews canonical reviews value
     */
    public EnterpriseConfigurationChange {
        java.util.Objects.requireNonNull(id, "id");
        java.util.Objects.requireNonNull(requesterUserId, "requesterUserId");
        java.util.Objects.requireNonNull(command, "command");
        java.util.Objects.requireNonNull(requestDigest, "requestDigest");
        java.util.Objects.requireNonNull(directoryRevision, "directoryRevision");
        java.util.Objects.requireNonNull(authorizationEpoch, "authorizationEpoch");
        java.util.Objects.requireNonNull(state, "state");
        java.util.Objects.requireNonNull(revision, "revision");
        java.util.Objects.requireNonNull(createdAtUnixMs, "createdAtUnixMs");
        java.util.Objects.requireNonNull(expiresAtUnixMs, "expiresAtUnixMs");
        java.util.Objects.requireNonNull(requiredReviews, "requiredReviews");
        java.util.Objects.requireNonNull(impact, "impact");
        impact = impact == null ? null : java.util.List.copyOf(impact);
        java.util.Objects.requireNonNull(affectedGroups, "affectedGroups");
        affectedGroups = affectedGroups == null ? null : java.util.List.copyOf(affectedGroups);
        java.util.Objects.requireNonNull(affectedIndividuals, "affectedIndividuals");
        affectedIndividuals = affectedIndividuals == null ? null : java.util.List.copyOf(affectedIndividuals);
        java.util.Objects.requireNonNull(reviews, "reviews");
        reviews = reviews == null ? null : java.util.List.copyOf(reviews);
    }
}
