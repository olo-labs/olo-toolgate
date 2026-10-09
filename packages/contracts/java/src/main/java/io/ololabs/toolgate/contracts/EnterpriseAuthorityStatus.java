// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
// GENERATED FILE — DO NOT EDIT DIRECTLY; tools/contracts/generate.py
package io.ololabs.toolgate.contracts;

/** Saved authority, explicitly published signed snapshot and independently acknowledged device adoption are distinct states.
 *
 * @param directoryRevision canonical directoryRevision value
 * @param authorizationEpoch canonical authorizationEpoch value
 * @param snapshotSequence canonical snapshotSequence value
 * @param publishedRevision canonical publishedRevision value
 * @param adoptions canonical adoptions value
 */
@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
public record EnterpriseAuthorityStatus(
    @com.fasterxml.jackson.annotation.JsonProperty(value = "directoryRevision", required = true) Long directoryRevision,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "authorizationEpoch", required = true) Long authorizationEpoch,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "snapshotSequence", required = true) Long snapshotSequence,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "publishedRevision", required = false) Long publishedRevision,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "adoptions", required = true) java.util.List<EnterpriseAdoptionStatus> adoptions
) {
    /** Reject absent required references and copy collections to retain value semantics.
     *
     * @param directoryRevision canonical directoryRevision value
     * @param authorizationEpoch canonical authorizationEpoch value
     * @param snapshotSequence canonical snapshotSequence value
     * @param publishedRevision canonical publishedRevision value
     * @param adoptions canonical adoptions value
     */
    public EnterpriseAuthorityStatus {
        java.util.Objects.requireNonNull(directoryRevision, "directoryRevision");
        java.util.Objects.requireNonNull(authorizationEpoch, "authorizationEpoch");
        java.util.Objects.requireNonNull(snapshotSequence, "snapshotSequence");
        java.util.Objects.requireNonNull(adoptions, "adoptions");
        adoptions = adoptions == null ? null : java.util.List.copyOf(adoptions);
    }
}
