// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
// GENERATED FILE — DO NOT EDIT DIRECTLY; tools/contracts/generate.py
package io.ololabs.toolgate.contracts;

/** Exact-operation capability; maximum ten seconds; authoritative consume boundary enforces replay.
 *
 * @param permitVersion canonical permitVersion value
 * @param issuer canonical issuer value
 * @param audience canonical audience value
 * @param tenantId canonical tenantId value
 * @param userId canonical userId value
 * @param agentId canonical agentId value
 * @param deviceId canonical deviceId value
 * @param toolId canonical toolId value
 * @param action canonical action value
 * @param resource canonical resource value
 * @param argumentsDigest canonical argumentsDigest value
 * @param requestId canonical requestId value
 * @param policyVersion canonical policyVersion value
 * @param approvalId canonical approvalId value
 * @param jti canonical jti value
 * @param issuedAtUnixMs canonical issuedAtUnixMs value
 * @param expiresAtUnixMs canonical expiresAtUnixMs value
 */
@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
public record ExecutionPermitClaims(
    @com.fasterxml.jackson.annotation.JsonProperty(value = "permitVersion", required = true) Long permitVersion,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "issuer", required = true) String issuer,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "audience", required = true) String audience,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "tenantId", required = true) String tenantId,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "userId", required = true) String userId,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "agentId", required = true) String agentId,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "deviceId", required = false) String deviceId,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "toolId", required = true) String toolId,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "action", required = true) String action,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "resource", required = true) ResourceDescriptor resource,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "argumentsDigest", required = true) String argumentsDigest,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "requestId", required = true) String requestId,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "policyVersion", required = true) String policyVersion,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "approvalId", required = true) String approvalId,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "jti", required = true) String jti,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "issuedAtUnixMs", required = true) Long issuedAtUnixMs,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "expiresAtUnixMs", required = true) Long expiresAtUnixMs
) {
    /** Reject absent required references and copy collections to retain value semantics.
     *
     * @param permitVersion canonical permitVersion value
     * @param issuer canonical issuer value
     * @param audience canonical audience value
     * @param tenantId canonical tenantId value
     * @param userId canonical userId value
     * @param agentId canonical agentId value
     * @param deviceId canonical deviceId value
     * @param toolId canonical toolId value
     * @param action canonical action value
     * @param resource canonical resource value
     * @param argumentsDigest canonical argumentsDigest value
     * @param requestId canonical requestId value
     * @param policyVersion canonical policyVersion value
     * @param approvalId canonical approvalId value
     * @param jti canonical jti value
     * @param issuedAtUnixMs canonical issuedAtUnixMs value
     * @param expiresAtUnixMs canonical expiresAtUnixMs value
     */
    public ExecutionPermitClaims {
        java.util.Objects.requireNonNull(permitVersion, "permitVersion");
        java.util.Objects.requireNonNull(issuer, "issuer");
        java.util.Objects.requireNonNull(audience, "audience");
        java.util.Objects.requireNonNull(tenantId, "tenantId");
        java.util.Objects.requireNonNull(userId, "userId");
        java.util.Objects.requireNonNull(agentId, "agentId");
        java.util.Objects.requireNonNull(toolId, "toolId");
        java.util.Objects.requireNonNull(action, "action");
        java.util.Objects.requireNonNull(resource, "resource");
        java.util.Objects.requireNonNull(argumentsDigest, "argumentsDigest");
        java.util.Objects.requireNonNull(requestId, "requestId");
        java.util.Objects.requireNonNull(policyVersion, "policyVersion");
        java.util.Objects.requireNonNull(approvalId, "approvalId");
        java.util.Objects.requireNonNull(jti, "jti");
        java.util.Objects.requireNonNull(issuedAtUnixMs, "issuedAtUnixMs");
        java.util.Objects.requireNonNull(expiresAtUnixMs, "expiresAtUnixMs");
    }
}
