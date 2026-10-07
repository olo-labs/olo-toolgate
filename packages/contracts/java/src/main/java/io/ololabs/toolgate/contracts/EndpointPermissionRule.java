// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
// GENERATED FILE — DO NOT EDIT DIRECTLY; tools/contracts/generate.py
package io.ololabs.toolgate.contracts;

/** Device-owner permission scope used only for local discovery; protected calls still require online authorization.
 *
 * @param toolId canonical toolId value
 * @param action canonical action value
 * @param agentIds canonical agentIds value
 * @param resource canonical resource value
 * @param decision canonical decision value
 */
@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
public record EndpointPermissionRule(
    @com.fasterxml.jackson.annotation.JsonProperty(value = "toolId", required = true) String toolId,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "action", required = true) String action,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "agentIds", required = true) java.util.List<String> agentIds,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "resource", required = true) ResourceDescriptor resource,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "decision", required = true) Decision decision
) {
    /** Reject absent required references and copy collections to retain value semantics.
     *
     * @param toolId canonical toolId value
     * @param action canonical action value
     * @param agentIds canonical agentIds value
     * @param resource canonical resource value
     * @param decision canonical decision value
     */
    public EndpointPermissionRule {
        java.util.Objects.requireNonNull(toolId, "toolId");
        java.util.Objects.requireNonNull(action, "action");
        java.util.Objects.requireNonNull(agentIds, "agentIds");
        agentIds = agentIds == null ? null : java.util.List.copyOf(agentIds);
        java.util.Objects.requireNonNull(resource, "resource");
        java.util.Objects.requireNonNull(decision, "decision");
    }
}
