// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
// GENERATED FILE — DO NOT EDIT DIRECTLY; tools/contracts/generate.py
package io.ololabs.toolgate.contracts;

/** Group-scoped guardrails. ALLOW does not create a grant; BLOCK overrides and ASK accumulates.
 *
 * @param id canonical id value
 * @param name canonical name value
 * @param enabled canonical enabled value
 * @param revision canonical revision value
 * @param decision canonical decision value
 * @param scope canonical scope value
 * @param teams canonical teams value
 * @param agentGroups canonical agentGroups value
 * @param approverTeams canonical approverTeams value
 */
@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
public record ControlPolicy(
    @com.fasterxml.jackson.annotation.JsonProperty(value = "id", required = true) String id,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "name", required = true) String name,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "enabled", required = true) Boolean enabled,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "revision", required = true) Long revision,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "decision", required = true) Decision decision,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "scope", required = true) EnterpriseScope scope,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "teams", required = true) GroupSelection teams,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "agentGroups", required = true) GroupSelection agentGroups,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "approverTeams", required = true) GroupSelection approverTeams
) {
    /** Reject absent required references and copy collections to retain value semantics.
     *
     * @param id canonical id value
     * @param name canonical name value
     * @param enabled canonical enabled value
     * @param revision canonical revision value
     * @param decision canonical decision value
     * @param scope canonical scope value
     * @param teams canonical teams value
     * @param agentGroups canonical agentGroups value
     * @param approverTeams canonical approverTeams value
     */
    public ControlPolicy {
        java.util.Objects.requireNonNull(id, "id");
        java.util.Objects.requireNonNull(name, "name");
        java.util.Objects.requireNonNull(enabled, "enabled");
        java.util.Objects.requireNonNull(revision, "revision");
        java.util.Objects.requireNonNull(decision, "decision");
        java.util.Objects.requireNonNull(scope, "scope");
        java.util.Objects.requireNonNull(teams, "teams");
        java.util.Objects.requireNonNull(agentGroups, "agentGroups");
        java.util.Objects.requireNonNull(approverTeams, "approverTeams");
    }
}
