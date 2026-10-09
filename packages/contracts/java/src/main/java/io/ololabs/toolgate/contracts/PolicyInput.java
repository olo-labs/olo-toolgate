// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
// GENERATED FILE — DO NOT EDIT DIRECTLY; tools/contracts/generate.py
package io.ololabs.toolgate.contracts;

/** Exact request identity and argument digest supplied to authorization.
 *
 * @param context canonical context value
 * @param toolId canonical toolId value
 * @param action canonical action value
 * @param resource canonical resource value
 * @param argumentsDigest canonical argumentsDigest value
 * @param toolDigest canonical toolDigest value
 * @param packageDigest canonical packageDigest value
 */
@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
public record PolicyInput(
    @com.fasterxml.jackson.annotation.JsonProperty(value = "context", required = true) RequestContext context,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "toolId", required = true) String toolId,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "action", required = true) String action,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "resource", required = true) ResourceDescriptor resource,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "argumentsDigest", required = true) String argumentsDigest,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "toolDigest", required = true) String toolDigest,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "packageDigest", required = true) String packageDigest
) {
    /** Reject absent required references and copy collections to retain value semantics.
     *
     * @param context canonical context value
     * @param toolId canonical toolId value
     * @param action canonical action value
     * @param resource canonical resource value
     * @param argumentsDigest canonical argumentsDigest value
     * @param toolDigest canonical toolDigest value
     * @param packageDigest canonical packageDigest value
     */
    public PolicyInput {
        java.util.Objects.requireNonNull(context, "context");
        java.util.Objects.requireNonNull(toolId, "toolId");
        java.util.Objects.requireNonNull(action, "action");
        java.util.Objects.requireNonNull(resource, "resource");
        java.util.Objects.requireNonNull(argumentsDigest, "argumentsDigest");
        java.util.Objects.requireNonNull(toolDigest, "toolDigest");
        java.util.Objects.requireNonNull(packageDigest, "packageDigest");
    }
}
