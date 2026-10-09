// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
// GENERATED FILE — DO NOT EDIT DIRECTLY; tools/contracts/generate.py
package io.ololabs.toolgate.contracts;

/** Side-effect-free complete request for simulation or trusted enforcement.
 *
 * @param context canonical context value
 * @param toolId canonical toolId value
 * @param action canonical action value
 * @param argumentsDigest canonical argumentsDigest value
 * @param resources canonical resources value
 * @param toolDigest canonical toolDigest value
 * @param packageDigest canonical packageDigest value
 * @param nowUnixMs canonical nowUnixMs value
 * @param authorityRevision canonical authorityRevision value
 * @param online canonical online value
 * @param amountMinorUnits canonical amountMinorUnits value
 * @param operation canonical operation value
 */
@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
public record EnterpriseEvaluation(
    @com.fasterxml.jackson.annotation.JsonProperty(value = "context", required = true) EnterpriseContext context,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "toolId", required = true) String toolId,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "action", required = true) String action,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "argumentsDigest", required = true) String argumentsDigest,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "resources", required = true) java.util.List<ResourceDescriptor> resources,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "toolDigest", required = true) String toolDigest,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "packageDigest", required = true) String packageDigest,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "nowUnixMs", required = true) Long nowUnixMs,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "authorityRevision", required = true) Long authorityRevision,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "online", required = true) Boolean online,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "amountMinorUnits", required = false) Long amountMinorUnits,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "operation", required = false) String operation
) {
    /** Reject absent required references and copy collections to retain value semantics.
     *
     * @param context canonical context value
     * @param toolId canonical toolId value
     * @param action canonical action value
     * @param argumentsDigest canonical argumentsDigest value
     * @param resources canonical resources value
     * @param toolDigest canonical toolDigest value
     * @param packageDigest canonical packageDigest value
     * @param nowUnixMs canonical nowUnixMs value
     * @param authorityRevision canonical authorityRevision value
     * @param online canonical online value
     * @param amountMinorUnits canonical amountMinorUnits value
     * @param operation canonical operation value
     */
    public EnterpriseEvaluation {
        java.util.Objects.requireNonNull(context, "context");
        java.util.Objects.requireNonNull(toolId, "toolId");
        java.util.Objects.requireNonNull(action, "action");
        java.util.Objects.requireNonNull(argumentsDigest, "argumentsDigest");
        java.util.Objects.requireNonNull(resources, "resources");
        resources = resources == null ? null : java.util.List.copyOf(resources);
        java.util.Objects.requireNonNull(toolDigest, "toolDigest");
        java.util.Objects.requireNonNull(packageDigest, "packageDigest");
        java.util.Objects.requireNonNull(nowUnixMs, "nowUnixMs");
        java.util.Objects.requireNonNull(authorityRevision, "authorityRevision");
        java.util.Objects.requireNonNull(online, "online");
    }
}
