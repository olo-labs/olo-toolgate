// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
// GENERATED FILE — DO NOT EDIT DIRECTLY; tools/contracts/generate.py
package io.ololabs.toolgate.contracts;

/** Format 2 exact rule; BLOCK > ASK > ALLOW. ASK never uses grace.
 *
 * @param policyId canonical policyId value
 * @param userIds canonical userIds value
 * @param agentIds canonical agentIds value
 * @param deviceIds canonical deviceIds value
 * @param toolId canonical toolId value
 * @param action canonical action value
 * @param resource canonical resource value
 * @param graceAllowed canonical graceAllowed value
 * @param effect canonical effect value
 */
@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
public record ApprovalBundleRule(
    @com.fasterxml.jackson.annotation.JsonProperty(value = "policyId", required = true) String policyId,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "userIds", required = true) java.util.List<String> userIds,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "agentIds", required = true) java.util.List<String> agentIds,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "deviceIds", required = true) java.util.List<String> deviceIds,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "toolId", required = true) String toolId,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "action", required = true) String action,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "resource", required = true) ResourceDescriptor resource,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "graceAllowed", required = true) Boolean graceAllowed,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "effect", required = true) Decision effect
) {
    /** Reject absent required references and copy collections to retain value semantics.
     *
     * @param policyId canonical policyId value
     * @param userIds canonical userIds value
     * @param agentIds canonical agentIds value
     * @param deviceIds canonical deviceIds value
     * @param toolId canonical toolId value
     * @param action canonical action value
     * @param resource canonical resource value
     * @param graceAllowed canonical graceAllowed value
     * @param effect canonical effect value
     */
    public ApprovalBundleRule {
        java.util.Objects.requireNonNull(policyId, "policyId");
        java.util.Objects.requireNonNull(userIds, "userIds");
        userIds = userIds == null ? null : java.util.List.copyOf(userIds);
        java.util.Objects.requireNonNull(agentIds, "agentIds");
        agentIds = agentIds == null ? null : java.util.List.copyOf(agentIds);
        java.util.Objects.requireNonNull(deviceIds, "deviceIds");
        deviceIds = deviceIds == null ? null : java.util.List.copyOf(deviceIds);
        java.util.Objects.requireNonNull(toolId, "toolId");
        java.util.Objects.requireNonNull(action, "action");
        java.util.Objects.requireNonNull(resource, "resource");
        java.util.Objects.requireNonNull(graceAllowed, "graceAllowed");
        java.util.Objects.requireNonNull(effect, "effect");
    }
}
