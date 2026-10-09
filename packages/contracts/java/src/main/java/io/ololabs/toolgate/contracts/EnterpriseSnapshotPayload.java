// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
// GENERATED FILE — DO NOT EDIT DIRECTLY; tools/contracts/generate.py
package io.ololabs.toolgate.contracts;

/** Signed group graph for discovery and adoption. It is never an execution permit.
 *
 * @param formatVersion canonical formatVersion value
 * @param issuer canonical issuer value
 * @param audience canonical audience value
 * @param tenantId canonical tenantId value
 * @param sequence canonical sequence value
 * @param policyVersion canonical policyVersion value
 * @param directoryRevision canonical directoryRevision value
 * @param authorizationEpoch canonical authorizationEpoch value
 * @param issuedAtUnixMs canonical issuedAtUnixMs value
 * @param expiresAtUnixMs canonical expiresAtUnixMs value
 * @param graphSha256 canonical graphSha256 value
 * @param graphBase64 canonical graphBase64 value
 * @param rollbackOf canonical rollbackOf value
 */
@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
public record EnterpriseSnapshotPayload(
    @com.fasterxml.jackson.annotation.JsonProperty(value = "formatVersion", required = true) Long formatVersion,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "issuer", required = true) String issuer,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "audience", required = true) String audience,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "tenantId", required = true) String tenantId,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "sequence", required = true) Long sequence,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "policyVersion", required = true) String policyVersion,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "directoryRevision", required = true) Long directoryRevision,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "authorizationEpoch", required = true) Long authorizationEpoch,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "issuedAtUnixMs", required = true) Long issuedAtUnixMs,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "expiresAtUnixMs", required = true) Long expiresAtUnixMs,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "graphSha256", required = true) String graphSha256,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "graphBase64", required = true) String graphBase64,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "rollbackOf", required = false) Long rollbackOf
) {
    /** Reject absent required references and copy collections to retain value semantics.
     *
     * @param formatVersion canonical formatVersion value
     * @param issuer canonical issuer value
     * @param audience canonical audience value
     * @param tenantId canonical tenantId value
     * @param sequence canonical sequence value
     * @param policyVersion canonical policyVersion value
     * @param directoryRevision canonical directoryRevision value
     * @param authorizationEpoch canonical authorizationEpoch value
     * @param issuedAtUnixMs canonical issuedAtUnixMs value
     * @param expiresAtUnixMs canonical expiresAtUnixMs value
     * @param graphSha256 canonical graphSha256 value
     * @param graphBase64 canonical graphBase64 value
     * @param rollbackOf canonical rollbackOf value
     */
    public EnterpriseSnapshotPayload {
        java.util.Objects.requireNonNull(formatVersion, "formatVersion");
        java.util.Objects.requireNonNull(issuer, "issuer");
        java.util.Objects.requireNonNull(audience, "audience");
        java.util.Objects.requireNonNull(tenantId, "tenantId");
        java.util.Objects.requireNonNull(sequence, "sequence");
        java.util.Objects.requireNonNull(policyVersion, "policyVersion");
        java.util.Objects.requireNonNull(directoryRevision, "directoryRevision");
        java.util.Objects.requireNonNull(authorizationEpoch, "authorizationEpoch");
        java.util.Objects.requireNonNull(issuedAtUnixMs, "issuedAtUnixMs");
        java.util.Objects.requireNonNull(expiresAtUnixMs, "expiresAtUnixMs");
        java.util.Objects.requireNonNull(graphSha256, "graphSha256");
        java.util.Objects.requireNonNull(graphBase64, "graphBase64");
    }
}
