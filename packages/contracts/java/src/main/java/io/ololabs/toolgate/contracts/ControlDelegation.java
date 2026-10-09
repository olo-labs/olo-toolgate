// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
// GENERATED FILE — DO NOT EDIT DIRECTLY; tools/contracts/generate.py
package io.ololabs.toolgate.contracts;

/** Delegated human operation requires grant and delegation through this same Team and capability through this same Agent Group.
 *
 * @param id canonical id value
 * @param name canonical name value
 * @param enabled canonical enabled value
 * @param revision canonical revision value
 * @param teamId canonical teamId value
 * @param agentGroupId canonical agentGroupId value
 * @param scope canonical scope value
 */
@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
public record ControlDelegation(
    @com.fasterxml.jackson.annotation.JsonProperty(value = "id", required = true) String id,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "name", required = true) String name,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "enabled", required = true) Boolean enabled,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "revision", required = true) Long revision,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "teamId", required = true) String teamId,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "agentGroupId", required = true) String agentGroupId,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "scope", required = true) EnterpriseScope scope
) {
    /** Reject absent required references and copy collections to retain value semantics.
     *
     * @param id canonical id value
     * @param name canonical name value
     * @param enabled canonical enabled value
     * @param revision canonical revision value
     * @param teamId canonical teamId value
     * @param agentGroupId canonical agentGroupId value
     * @param scope canonical scope value
     */
    public ControlDelegation {
        java.util.Objects.requireNonNull(id, "id");
        java.util.Objects.requireNonNull(name, "name");
        java.util.Objects.requireNonNull(enabled, "enabled");
        java.util.Objects.requireNonNull(revision, "revision");
        java.util.Objects.requireNonNull(teamId, "teamId");
        java.util.Objects.requireNonNull(agentGroupId, "agentGroupId");
        java.util.Objects.requireNonNull(scope, "scope");
    }
}
