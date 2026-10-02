// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
// GENERATED FILE — DO NOT EDIT DIRECTLY; tools/contracts/generate.py
package io.ololabs.toolgate.contracts;

/** Tenant-scoped policies configuration record. Not a runtime credential or policy grant.
 *
 * @param id canonical id value
 * @param name canonical name value
 * @param enabled canonical enabled value
 * @param revision canonical revision value
 * @param toolId canonical toolId value
 * @param action canonical action value
 * @param resource canonical resource value
 * @param decision canonical decision value
 * @param userIds canonical userIds value
 * @param teamIds canonical teamIds value
 * @param agentIds canonical agentIds value
 * @param deviceIds canonical deviceIds value
 */
@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
public record ControlPolicy(
    @com.fasterxml.jackson.annotation.JsonProperty(value = "id", required = true) String id,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "name", required = true) String name,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "enabled", required = true) Boolean enabled,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "revision", required = true) Long revision,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "toolId", required = true) String toolId,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "action", required = true) String action,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "resource", required = true) ResourceDescriptor resource,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "decision", required = true) Decision decision,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "userIds", required = true) java.util.List<String> userIds,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "teamIds", required = true) java.util.List<String> teamIds,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "agentIds", required = true) java.util.List<String> agentIds,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "deviceIds", required = true) java.util.List<String> deviceIds
) {
    /** Reject absent required references and copy collections to retain value semantics.
     *
     * @param id canonical id value
     * @param name canonical name value
     * @param enabled canonical enabled value
     * @param revision canonical revision value
     * @param toolId canonical toolId value
     * @param action canonical action value
     * @param resource canonical resource value
     * @param decision canonical decision value
     * @param userIds canonical userIds value
     * @param teamIds canonical teamIds value
     * @param agentIds canonical agentIds value
     * @param deviceIds canonical deviceIds value
     */
    public ControlPolicy {
        java.util.Objects.requireNonNull(id, "id");
        java.util.Objects.requireNonNull(name, "name");
        java.util.Objects.requireNonNull(enabled, "enabled");
        java.util.Objects.requireNonNull(revision, "revision");
        java.util.Objects.requireNonNull(toolId, "toolId");
        java.util.Objects.requireNonNull(action, "action");
        java.util.Objects.requireNonNull(resource, "resource");
        java.util.Objects.requireNonNull(decision, "decision");
        java.util.Objects.requireNonNull(userIds, "userIds");
        userIds = userIds == null ? null : java.util.List.copyOf(userIds);
        java.util.Objects.requireNonNull(teamIds, "teamIds");
        teamIds = teamIds == null ? null : java.util.List.copyOf(teamIds);
        java.util.Objects.requireNonNull(agentIds, "agentIds");
        agentIds = agentIds == null ? null : java.util.List.copyOf(agentIds);
        java.util.Objects.requireNonNull(deviceIds, "deviceIds");
        deviceIds = deviceIds == null ? null : java.util.List.copyOf(deviceIds);
    }
}
