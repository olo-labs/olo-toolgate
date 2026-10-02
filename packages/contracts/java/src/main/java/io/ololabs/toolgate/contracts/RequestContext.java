// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
// GENERATED FILE — DO NOT EDIT DIRECTLY; tools/contracts/generate.py
package io.ololabs.toolgate.contracts;

/** Safe correlation and principal identifiers; contains no credentials.
 *
 * @param requestId canonical requestId value
 * @param tenantId canonical tenantId value
 * @param userId canonical userId value
 * @param agentId canonical agentId value
 * @param deviceId canonical deviceId value
 */
@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
public record RequestContext(
    @com.fasterxml.jackson.annotation.JsonProperty(value = "requestId", required = true) String requestId,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "tenantId", required = true) String tenantId,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "userId", required = true) String userId,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "agentId", required = true) String agentId,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "deviceId", required = false) String deviceId
) {
    /** Reject absent required references and copy collections to retain value semantics.
     *
     * @param requestId canonical requestId value
     * @param tenantId canonical tenantId value
     * @param userId canonical userId value
     * @param agentId canonical agentId value
     * @param deviceId canonical deviceId value
     */
    public RequestContext {
        java.util.Objects.requireNonNull(requestId, "requestId");
        java.util.Objects.requireNonNull(tenantId, "tenantId");
        java.util.Objects.requireNonNull(userId, "userId");
        java.util.Objects.requireNonNull(agentId, "agentId");
    }
}
