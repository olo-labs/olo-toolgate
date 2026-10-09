// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
// GENERATED FILE — DO NOT EDIT DIRECTLY; tools/contracts/generate.py
package io.ololabs.toolgate.contracts;

/** Read-only comparison to a captured immutable legacy result; never a second runtime authority.
 *
 * @param snapshot canonical snapshot value
 * @param evaluation canonical evaluation value
 * @param observedLegacyDecision canonical observedLegacyDecision value
 * @param legacyEvidenceDigest canonical legacyEvidenceDigest value
 */
@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
public record EnterpriseShadowRequest(
    @com.fasterxml.jackson.annotation.JsonProperty(value = "snapshot", required = true) ControlSnapshot snapshot,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "evaluation", required = true) EnterpriseEvaluation evaluation,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "observedLegacyDecision", required = true) Decision observedLegacyDecision,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "legacyEvidenceDigest", required = true) String legacyEvidenceDigest
) {
    /** Reject absent required references and copy collections to retain value semantics.
     *
     * @param snapshot canonical snapshot value
     * @param evaluation canonical evaluation value
     * @param observedLegacyDecision canonical observedLegacyDecision value
     * @param legacyEvidenceDigest canonical legacyEvidenceDigest value
     */
    public EnterpriseShadowRequest {
        java.util.Objects.requireNonNull(snapshot, "snapshot");
        java.util.Objects.requireNonNull(evaluation, "evaluation");
        java.util.Objects.requireNonNull(observedLegacyDecision, "observedLegacyDecision");
        java.util.Objects.requireNonNull(legacyEvidenceDigest, "legacyEvidenceDigest");
    }
}
