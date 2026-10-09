// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
// GENERATED FILE — DO NOT EDIT DIRECTLY; tools/contracts/generate.py
package io.ololabs.toolgate.contracts;

/** Bounded downstream Agent Group delegation; every hop narrows capability.
 *
 * @param id canonical id value
 * @param name canonical name value
 * @param enabled canonical enabled value
 * @param revision canonical revision value
 * @param fromAgentGroupId canonical fromAgentGroupId value
 * @param toAgentGroupId canonical toAgentGroupId value
 * @param scope canonical scope value
 * @param maximumDepth canonical maximumDepth value
 */
@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
public record ControlAgentDelegation(
    @com.fasterxml.jackson.annotation.JsonProperty(value = "id", required = true) String id,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "name", required = true) String name,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "enabled", required = true) Boolean enabled,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "revision", required = true) Long revision,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "fromAgentGroupId", required = true) String fromAgentGroupId,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "toAgentGroupId", required = true) String toAgentGroupId,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "scope", required = true) EnterpriseScope scope,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "maximumDepth", required = true) Long maximumDepth
) {
    /** Reject absent required references and copy collections to retain value semantics.
     *
     * @param id canonical id value
     * @param name canonical name value
     * @param enabled canonical enabled value
     * @param revision canonical revision value
     * @param fromAgentGroupId canonical fromAgentGroupId value
     * @param toAgentGroupId canonical toAgentGroupId value
     * @param scope canonical scope value
     * @param maximumDepth canonical maximumDepth value
     */
    public ControlAgentDelegation {
        java.util.Objects.requireNonNull(id, "id");
        java.util.Objects.requireNonNull(name, "name");
        java.util.Objects.requireNonNull(enabled, "enabled");
        java.util.Objects.requireNonNull(revision, "revision");
        java.util.Objects.requireNonNull(fromAgentGroupId, "fromAgentGroupId");
        java.util.Objects.requireNonNull(toAgentGroupId, "toAgentGroupId");
        java.util.Objects.requireNonNull(scope, "scope");
        java.util.Objects.requireNonNull(maximumDepth, "maximumDepth");
    }
}
