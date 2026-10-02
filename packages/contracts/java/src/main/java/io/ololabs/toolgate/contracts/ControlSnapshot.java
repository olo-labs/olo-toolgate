// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
// GENERATED FILE — DO NOT EDIT DIRECTLY; tools/contracts/generate.py
package io.ololabs.toolgate.contracts;

/** Versioned configuration data. Contains no credentials or executable code.
 *
 * @param formatVersion canonical formatVersion value
 * @param tenantId canonical tenantId value
 * @param revision canonical revision value
 * @param users canonical users value
 * @param teams canonical teams value
 * @param agents canonical agents value
 * @param tools canonical tools value
 * @param policies canonical policies value
 * @param devices canonical devices value
 */
@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
public record ControlSnapshot(
    @com.fasterxml.jackson.annotation.JsonProperty(value = "formatVersion", required = true) Long formatVersion,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "tenantId", required = true) String tenantId,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "revision", required = true) Long revision,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "users", required = true) java.util.List<ControlUser> users,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "teams", required = true) java.util.List<ControlTeam> teams,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "agents", required = true) java.util.List<ControlAgent> agents,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "tools", required = true) java.util.List<ControlTool> tools,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "policies", required = true) java.util.List<ControlPolicy> policies,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "devices", required = true) java.util.List<ControlDevice> devices
) {
    /** Reject absent required references and copy collections to retain value semantics.
     *
     * @param formatVersion canonical formatVersion value
     * @param tenantId canonical tenantId value
     * @param revision canonical revision value
     * @param users canonical users value
     * @param teams canonical teams value
     * @param agents canonical agents value
     * @param tools canonical tools value
     * @param policies canonical policies value
     * @param devices canonical devices value
     */
    public ControlSnapshot {
        java.util.Objects.requireNonNull(formatVersion, "formatVersion");
        java.util.Objects.requireNonNull(tenantId, "tenantId");
        java.util.Objects.requireNonNull(revision, "revision");
        java.util.Objects.requireNonNull(users, "users");
        users = users == null ? null : java.util.List.copyOf(users);
        java.util.Objects.requireNonNull(teams, "teams");
        teams = teams == null ? null : java.util.List.copyOf(teams);
        java.util.Objects.requireNonNull(agents, "agents");
        agents = agents == null ? null : java.util.List.copyOf(agents);
        java.util.Objects.requireNonNull(tools, "tools");
        tools = tools == null ? null : java.util.List.copyOf(tools);
        java.util.Objects.requireNonNull(policies, "policies");
        policies = policies == null ? null : java.util.List.copyOf(policies);
        java.util.Objects.requireNonNull(devices, "devices");
        devices = devices == null ? null : java.util.List.copyOf(devices);
    }
}
