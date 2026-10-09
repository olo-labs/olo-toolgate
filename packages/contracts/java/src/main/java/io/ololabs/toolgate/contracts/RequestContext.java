// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
// GENERATED FILE — DO NOT EDIT DIRECTLY; tools/contracts/generate.py
package io.ololabs.toolgate.contracts;

/** Authenticated Gateway runtime context. All mode, chain, session, workload, binding and target fields are explicit. Legacy contexts are rejected.
 *
 * @param requestId canonical requestId value
 * @param tenantId canonical tenantId value
 * @param mode canonical mode value
 * @param userId canonical userId value
 * @param agentId canonical agentId value
 * @param workloadBindingId canonical workloadBindingId value
 * @param chain canonical chain value
 * @param sessionEpoch canonical sessionEpoch value
 * @param credentialEpoch canonical credentialEpoch value
 * @param bindingId canonical bindingId value
 * @param deviceId canonical deviceId value
 * @param credentialSha256 canonical credentialSha256 value
 */
@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
public record RequestContext(
    @com.fasterxml.jackson.annotation.JsonProperty(value = "requestId", required = true) String requestId,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "tenantId", required = true) String tenantId,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "mode", required = true) EnterpriseRequestMode mode,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "userId", required = false) String userId,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "agentId", required = false) String agentId,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "workloadBindingId", required = false) String workloadBindingId,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "chain", required = true) java.util.List<EnterpriseActorHop> chain,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "sessionEpoch", required = false) Long sessionEpoch,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "credentialEpoch", required = false) Long credentialEpoch,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "bindingId", required = true) String bindingId,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "deviceId", required = true) String deviceId,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "credentialSha256", required = false) String credentialSha256
) {
    /** Reject absent required references and copy collections to retain value semantics.
     *
     * @param requestId canonical requestId value
     * @param tenantId canonical tenantId value
     * @param mode canonical mode value
     * @param userId canonical userId value
     * @param agentId canonical agentId value
     * @param workloadBindingId canonical workloadBindingId value
     * @param chain canonical chain value
     * @param sessionEpoch canonical sessionEpoch value
     * @param credentialEpoch canonical credentialEpoch value
     * @param bindingId canonical bindingId value
     * @param deviceId canonical deviceId value
     * @param credentialSha256 canonical credentialSha256 value
     */
    public RequestContext {
        java.util.Objects.requireNonNull(requestId, "requestId");
        java.util.Objects.requireNonNull(tenantId, "tenantId");
        java.util.Objects.requireNonNull(mode, "mode");
        java.util.Objects.requireNonNull(chain, "chain");
        chain = chain == null ? null : java.util.List.copyOf(chain);
        java.util.Objects.requireNonNull(bindingId, "bindingId");
        java.util.Objects.requireNonNull(deviceId, "deviceId");
    }
}
