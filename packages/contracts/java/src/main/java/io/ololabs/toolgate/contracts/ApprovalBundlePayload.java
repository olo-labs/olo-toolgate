// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
// GENERATED FILE — DO NOT EDIT DIRECTLY; tools/contracts/generate.py
package io.ololabs.toolgate.contracts;

/** Signed format 2 envelope; compilation adds ASK; old readers reject safely.
 *
 * @param formatVersion canonical formatVersion value
 * @param issuer canonical issuer value
 * @param audience canonical audience value
 * @param tenantId canonical tenantId value
 * @param sequence canonical sequence value
 * @param version canonical version value
 * @param directoryRevision canonical directoryRevision value
 * @param issuedAtUnixMs canonical issuedAtUnixMs value
 * @param expiresAtUnixMs canonical expiresAtUnixMs value
 * @param graceMs canonical graceMs value
 * @param policySha256 canonical policySha256 value
 * @param policy canonical policy value
 * @param rollbackOf canonical rollbackOf value
 */
@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
public record ApprovalBundlePayload(
    @com.fasterxml.jackson.annotation.JsonProperty(value = "formatVersion", required = true) Long formatVersion,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "issuer", required = true) String issuer,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "audience", required = true) String audience,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "tenantId", required = true) String tenantId,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "sequence", required = true) Long sequence,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "version", required = true) String version,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "directoryRevision", required = true) Long directoryRevision,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "issuedAtUnixMs", required = true) Long issuedAtUnixMs,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "expiresAtUnixMs", required = true) Long expiresAtUnixMs,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "graceMs", required = true) Long graceMs,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "policySha256", required = true) String policySha256,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "policy", required = true) String policy,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "rollbackOf", required = false) Long rollbackOf
) {
    /** Reject absent required references and copy collections to retain value semantics.
     *
     * @param formatVersion canonical formatVersion value
     * @param issuer canonical issuer value
     * @param audience canonical audience value
     * @param tenantId canonical tenantId value
     * @param sequence canonical sequence value
     * @param version canonical version value
     * @param directoryRevision canonical directoryRevision value
     * @param issuedAtUnixMs canonical issuedAtUnixMs value
     * @param expiresAtUnixMs canonical expiresAtUnixMs value
     * @param graceMs canonical graceMs value
     * @param policySha256 canonical policySha256 value
     * @param policy canonical policy value
     * @param rollbackOf canonical rollbackOf value
     */
    public ApprovalBundlePayload {
        java.util.Objects.requireNonNull(formatVersion, "formatVersion");
        java.util.Objects.requireNonNull(issuer, "issuer");
        java.util.Objects.requireNonNull(audience, "audience");
        java.util.Objects.requireNonNull(tenantId, "tenantId");
        java.util.Objects.requireNonNull(sequence, "sequence");
        java.util.Objects.requireNonNull(version, "version");
        java.util.Objects.requireNonNull(directoryRevision, "directoryRevision");
        java.util.Objects.requireNonNull(issuedAtUnixMs, "issuedAtUnixMs");
        java.util.Objects.requireNonNull(expiresAtUnixMs, "expiresAtUnixMs");
        java.util.Objects.requireNonNull(graceMs, "graceMs");
        java.util.Objects.requireNonNull(policySha256, "policySha256");
        java.util.Objects.requireNonNull(policy, "policy");
    }
}
