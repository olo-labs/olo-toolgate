// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
// GENERATED FILE — DO NOT EDIT DIRECTLY; tools/contracts/generate.py
package io.ololabs.toolgate.contracts;

/** Fixed capability templates narrowed by device scope and optional tool allowlist. GROUPS requires nonempty group IDs; NONE and ALL require empty groups, validated by Control. Policies authorize each call.
 *
 * @param templateIds canonical templateIds value
 * @param deviceScope canonical deviceScope value
 * @param deviceGroupIds canonical deviceGroupIds value
 * @param toolIds canonical toolIds value
 */
@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
public record RoleRules(
    @com.fasterxml.jackson.annotation.JsonProperty(value = "templateIds", required = true) java.util.List<UserPrivilegeTemplate> templateIds,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "deviceScope", required = true) RoleDeviceScope deviceScope,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "deviceGroupIds", required = true) java.util.List<String> deviceGroupIds,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "toolIds", required = true) java.util.List<String> toolIds
) {
    /** Reject absent required references and copy collections to retain value semantics.
     *
     * @param templateIds canonical templateIds value
     * @param deviceScope canonical deviceScope value
     * @param deviceGroupIds canonical deviceGroupIds value
     * @param toolIds canonical toolIds value
     */
    public RoleRules {
        java.util.Objects.requireNonNull(templateIds, "templateIds");
        templateIds = templateIds == null ? null : java.util.List.copyOf(templateIds);
        java.util.Objects.requireNonNull(deviceScope, "deviceScope");
        java.util.Objects.requireNonNull(deviceGroupIds, "deviceGroupIds");
        deviceGroupIds = deviceGroupIds == null ? null : java.util.List.copyOf(deviceGroupIds);
        java.util.Objects.requireNonNull(toolIds, "toolIds");
        toolIds = toolIds == null ? null : java.util.List.copyOf(toolIds);
    }
}
