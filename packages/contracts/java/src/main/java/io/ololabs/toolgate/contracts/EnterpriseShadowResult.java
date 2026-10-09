// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
// GENERATED FILE — DO NOT EDIT DIRECTLY; tools/contracts/generate.py
package io.ololabs.toolgate.contracts;

/** No grant is applied and no approval, quota or permit is consumed.
 *
 * @param decision canonical decision value
 * @param observedLegacyDecision canonical observedLegacyDecision value
 * @param legacyEvidenceDigest canonical legacyEvidenceDigest value
 * @param proposedSnapshotDigest canonical proposedSnapshotDigest value
 * @param accessExpansion canonical accessExpansion value
 */
@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
public record EnterpriseShadowResult(
    @com.fasterxml.jackson.annotation.JsonProperty(value = "decision", required = true) EnterpriseDecision decision,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "observedLegacyDecision", required = true) Decision observedLegacyDecision,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "legacyEvidenceDigest", required = true) String legacyEvidenceDigest,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "proposedSnapshotDigest", required = true) String proposedSnapshotDigest,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "accessExpansion", required = true) Boolean accessExpansion
) {
    /** Reject absent required references and copy collections to retain value semantics.
     *
     * @param decision canonical decision value
     * @param observedLegacyDecision canonical observedLegacyDecision value
     * @param legacyEvidenceDigest canonical legacyEvidenceDigest value
     * @param proposedSnapshotDigest canonical proposedSnapshotDigest value
     * @param accessExpansion canonical accessExpansion value
     */
    public EnterpriseShadowResult {
        java.util.Objects.requireNonNull(decision, "decision");
        java.util.Objects.requireNonNull(observedLegacyDecision, "observedLegacyDecision");
        java.util.Objects.requireNonNull(legacyEvidenceDigest, "legacyEvidenceDigest");
        java.util.Objects.requireNonNull(proposedSnapshotDigest, "proposedSnapshotDigest");
        java.util.Objects.requireNonNull(accessExpansion, "accessExpansion");
    }
}
