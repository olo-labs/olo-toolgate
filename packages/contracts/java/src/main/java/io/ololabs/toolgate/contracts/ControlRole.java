// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
// GENERATED FILE — DO NOT EDIT DIRECTLY; tools/contracts/generate.py
package io.ololabs.toolgate.contracts;

/** Typed role assigned only to compatible groups. Management rules cannot confer runtime access.
 *
 * @param id canonical id value
 * @param name canonical name value
 * @param enabled canonical enabled value
 * @param revision canonical revision value
 * @param portalRole canonical portalRole value
 * @param roleType canonical roleType value
 * @param managementRules canonical managementRules value
 */
@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
public record ControlRole(
    @com.fasterxml.jackson.annotation.JsonProperty(value = "id", required = true) String id,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "name", required = true) String name,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "enabled", required = true) Boolean enabled,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "revision", required = true) Long revision,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "portalRole", required = true) UserRole portalRole,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "roleType", required = true) EnterpriseRoleType roleType,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "managementRules", required = true) java.util.List<EnterpriseManagementRule> managementRules
) {
    /** Reject absent required references and copy collections to retain value semantics.
     *
     * @param id canonical id value
     * @param name canonical name value
     * @param enabled canonical enabled value
     * @param revision canonical revision value
     * @param portalRole canonical portalRole value
     * @param roleType canonical roleType value
     * @param managementRules canonical managementRules value
     */
    public ControlRole {
        java.util.Objects.requireNonNull(id, "id");
        java.util.Objects.requireNonNull(name, "name");
        java.util.Objects.requireNonNull(enabled, "enabled");
        java.util.Objects.requireNonNull(revision, "revision");
        java.util.Objects.requireNonNull(portalRole, "portalRole");
        java.util.Objects.requireNonNull(roleType, "roleType");
        java.util.Objects.requireNonNull(managementRules, "managementRules");
        managementRules = managementRules == null ? null : java.util.List.copyOf(managementRules);
    }
}
