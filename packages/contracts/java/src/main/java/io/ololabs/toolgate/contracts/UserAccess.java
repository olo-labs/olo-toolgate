// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
// GENERATED FILE — DO NOT EDIT DIRECTLY; tools/contracts/generate.py
package io.ololabs.toolgate.contracts;

/** Tenant-scoped role and privilege assignment. Device groups are directory teams containing device IDs.
 *
 * @param role canonical role value
 * @param templateIds canonical templateIds value
 * @param deviceGroupIds canonical deviceGroupIds value
 * @param roleIds canonical roleIds value
 */
@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
public record UserAccess(
    @com.fasterxml.jackson.annotation.JsonProperty(value = "role", required = true) UserRole role,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "templateIds", required = true) java.util.List<UserPrivilegeTemplate> templateIds,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "deviceGroupIds", required = true) java.util.List<String> deviceGroupIds,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "roleIds", required = false) java.util.List<String> roleIds
) {
    /** Reject absent required references and copy collections to retain value semantics.
     *
     * @param role canonical role value
     * @param templateIds canonical templateIds value
     * @param deviceGroupIds canonical deviceGroupIds value
     * @param roleIds canonical roleIds value
     */
    public UserAccess {
        java.util.Objects.requireNonNull(role, "role");
        java.util.Objects.requireNonNull(templateIds, "templateIds");
        templateIds = templateIds == null ? null : java.util.List.copyOf(templateIds);
        java.util.Objects.requireNonNull(deviceGroupIds, "deviceGroupIds");
        deviceGroupIds = deviceGroupIds == null ? null : java.util.List.copyOf(deviceGroupIds);
        roleIds = roleIds == null ? null : java.util.List.copyOf(roleIds);
    }
}
