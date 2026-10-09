// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
// GENERATED FILE — DO NOT EDIT DIRECTLY; tools/contracts/generate.py
package io.ololabs.toolgate.contracts;

/** Device-bound durable effect outcome and completed resource subset.
 *
 * @param invocationId canonical invocationId value
 * @param expectedRevision canonical expectedRevision value
 * @param state canonical state value
 * @param completedResources canonical completedResources value
 * @param resultDigest canonical resultDigest value
 */
@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
public record EnterpriseEffectReport(
    @com.fasterxml.jackson.annotation.JsonProperty(value = "invocationId", required = true) String invocationId,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "expectedRevision", required = true) Long expectedRevision,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "state", required = true) EnterpriseInvocationState state,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "completedResources", required = true) java.util.List<ResourceDescriptor> completedResources,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "resultDigest", required = false) String resultDigest
) {
    /** Reject absent required references and copy collections to retain value semantics.
     *
     * @param invocationId canonical invocationId value
     * @param expectedRevision canonical expectedRevision value
     * @param state canonical state value
     * @param completedResources canonical completedResources value
     * @param resultDigest canonical resultDigest value
     */
    public EnterpriseEffectReport {
        java.util.Objects.requireNonNull(invocationId, "invocationId");
        java.util.Objects.requireNonNull(expectedRevision, "expectedRevision");
        java.util.Objects.requireNonNull(state, "state");
        java.util.Objects.requireNonNull(completedResources, "completedResources");
        completedResources = completedResources == null ? null : java.util.List.copyOf(completedResources);
    }
}
