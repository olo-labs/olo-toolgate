// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
// GENERATED FILE — DO NOT EDIT DIRECTLY; tools/contracts/generate.py
package io.ololabs.toolgate.contracts;

/** Deterministic decision, safe reasons, complete witnesses and accumulated obligations.
 *
 * @param decision canonical decision value
 * @param reason canonical reason value
 * @param witnesses canonical witnesses value
 * @param obligations canonical obligations value
 * @param revision canonical revision value
 * @param authorizationEpoch canonical authorizationEpoch value
 * @param validUntilUnixMs canonical validUntilUnixMs value
 * @param diagnosticId canonical diagnosticId value
 */
@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
public record EnterpriseDecision(
    @com.fasterxml.jackson.annotation.JsonProperty(value = "decision", required = true) Decision decision,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "reason", required = true) EnterpriseDecisionReason reason,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "witnesses", required = true) java.util.List<EnterpriseWitness> witnesses,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "obligations", required = true) java.util.List<String> obligations,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "revision", required = true) Long revision,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "authorizationEpoch", required = true) Long authorizationEpoch,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "validUntilUnixMs", required = true) Long validUntilUnixMs,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "diagnosticId", required = true) String diagnosticId
) {
    /** Reject absent required references and copy collections to retain value semantics.
     *
     * @param decision canonical decision value
     * @param reason canonical reason value
     * @param witnesses canonical witnesses value
     * @param obligations canonical obligations value
     * @param revision canonical revision value
     * @param authorizationEpoch canonical authorizationEpoch value
     * @param validUntilUnixMs canonical validUntilUnixMs value
     * @param diagnosticId canonical diagnosticId value
     */
    public EnterpriseDecision {
        java.util.Objects.requireNonNull(decision, "decision");
        java.util.Objects.requireNonNull(reason, "reason");
        java.util.Objects.requireNonNull(witnesses, "witnesses");
        witnesses = witnesses == null ? null : java.util.List.copyOf(witnesses);
        java.util.Objects.requireNonNull(obligations, "obligations");
        obligations = obligations == null ? null : java.util.List.copyOf(obligations);
        java.util.Objects.requireNonNull(revision, "revision");
        java.util.Objects.requireNonNull(authorizationEpoch, "authorizationEpoch");
        java.util.Objects.requireNonNull(validUntilUnixMs, "validUntilUnixMs");
        java.util.Objects.requireNonNull(diagnosticId, "diagnosticId");
    }
}
