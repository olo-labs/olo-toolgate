// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
// GENERATED FILE — DO NOT EDIT DIRECTLY; tools/contracts/generate.py
package io.ololabs.toolgate.contracts;

/** A complete group/action/device/resource tuple; independent scopes are never multiplied.
 *
 * @param toolGroups canonical toolGroups value
 * @param deviceGroups canonical deviceGroups value
 * @param actions canonical actions value
 * @param allActions canonical allActions value
 * @param resources canonical resources value
 * @param conditions canonical conditions value
 */
@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
public record EnterpriseScope(
    @com.fasterxml.jackson.annotation.JsonProperty(value = "toolGroups", required = true) GroupSelection toolGroups,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "deviceGroups", required = true) GroupSelection deviceGroups,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "actions", required = true) java.util.List<String> actions,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "allActions", required = true) Boolean allActions,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "resources", required = true) java.util.List<EnterpriseResourceRule> resources,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "conditions", required = true) EnterpriseConditions conditions
) {
    /** Reject absent required references and copy collections to retain value semantics.
     *
     * @param toolGroups canonical toolGroups value
     * @param deviceGroups canonical deviceGroups value
     * @param actions canonical actions value
     * @param allActions canonical allActions value
     * @param resources canonical resources value
     * @param conditions canonical conditions value
     */
    public EnterpriseScope {
        java.util.Objects.requireNonNull(toolGroups, "toolGroups");
        java.util.Objects.requireNonNull(deviceGroups, "deviceGroups");
        java.util.Objects.requireNonNull(actions, "actions");
        actions = actions == null ? null : java.util.List.copyOf(actions);
        java.util.Objects.requireNonNull(allActions, "allActions");
        java.util.Objects.requireNonNull(resources, "resources");
        resources = resources == null ? null : java.util.List.copyOf(resources);
        java.util.Objects.requireNonNull(conditions, "conditions");
    }
}
