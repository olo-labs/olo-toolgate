// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
// GENERATED FILE — DO NOT EDIT DIRECTLY; tools/contracts/generate.py
package io.ololabs.toolgate.contracts;

/** Short-lived audience-bound permit for one exact invocation and target.
 *
 * @param issuer canonical issuer value
 * @param audience canonical audience value
 * @param nonce canonical nonce value
 * @param invocationId canonical invocationId value
 * @param requestDigest canonical requestDigest value
 * @param evaluationDigest canonical evaluationDigest value
 * @param authorizationEpoch canonical authorizationEpoch value
 * @param directoryRevision canonical directoryRevision value
 * @param issuedAtUnixMs canonical issuedAtUnixMs value
 * @param expiresAtUnixMs canonical expiresAtUnixMs value
 * @param toolDigest canonical toolDigest value
 * @param packageDigest canonical packageDigest value
 * @param bindingId canonical bindingId value
 * @param deviceId canonical deviceId value
 * @param approvalIds canonical approvalIds value
 */
@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
public record EnterprisePermitClaims(
    @com.fasterxml.jackson.annotation.JsonProperty(value = "issuer", required = true) String issuer,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "audience", required = true) String audience,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "nonce", required = true) String nonce,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "invocationId", required = true) String invocationId,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "requestDigest", required = true) String requestDigest,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "evaluationDigest", required = true) String evaluationDigest,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "authorizationEpoch", required = true) Long authorizationEpoch,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "directoryRevision", required = true) Long directoryRevision,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "issuedAtUnixMs", required = true) Long issuedAtUnixMs,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "expiresAtUnixMs", required = true) Long expiresAtUnixMs,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "toolDigest", required = true) String toolDigest,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "packageDigest", required = true) String packageDigest,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "bindingId", required = true) String bindingId,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "deviceId", required = true) String deviceId,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "approvalIds", required = true) java.util.List<String> approvalIds
) {
    /** Reject absent required references and copy collections to retain value semantics.
     *
     * @param issuer canonical issuer value
     * @param audience canonical audience value
     * @param nonce canonical nonce value
     * @param invocationId canonical invocationId value
     * @param requestDigest canonical requestDigest value
     * @param evaluationDigest canonical evaluationDigest value
     * @param authorizationEpoch canonical authorizationEpoch value
     * @param directoryRevision canonical directoryRevision value
     * @param issuedAtUnixMs canonical issuedAtUnixMs value
     * @param expiresAtUnixMs canonical expiresAtUnixMs value
     * @param toolDigest canonical toolDigest value
     * @param packageDigest canonical packageDigest value
     * @param bindingId canonical bindingId value
     * @param deviceId canonical deviceId value
     * @param approvalIds canonical approvalIds value
     */
    public EnterprisePermitClaims {
        java.util.Objects.requireNonNull(issuer, "issuer");
        java.util.Objects.requireNonNull(audience, "audience");
        java.util.Objects.requireNonNull(nonce, "nonce");
        java.util.Objects.requireNonNull(invocationId, "invocationId");
        java.util.Objects.requireNonNull(requestDigest, "requestDigest");
        java.util.Objects.requireNonNull(evaluationDigest, "evaluationDigest");
        java.util.Objects.requireNonNull(authorizationEpoch, "authorizationEpoch");
        java.util.Objects.requireNonNull(directoryRevision, "directoryRevision");
        java.util.Objects.requireNonNull(issuedAtUnixMs, "issuedAtUnixMs");
        java.util.Objects.requireNonNull(expiresAtUnixMs, "expiresAtUnixMs");
        java.util.Objects.requireNonNull(toolDigest, "toolDigest");
        java.util.Objects.requireNonNull(packageDigest, "packageDigest");
        java.util.Objects.requireNonNull(bindingId, "bindingId");
        java.util.Objects.requireNonNull(deviceId, "deviceId");
        java.util.Objects.requireNonNull(approvalIds, "approvalIds");
        approvalIds = approvalIds == null ? null : java.util.List.copyOf(approvalIds);
    }
}
