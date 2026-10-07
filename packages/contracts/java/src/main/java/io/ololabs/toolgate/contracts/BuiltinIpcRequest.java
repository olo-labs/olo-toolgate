// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
// GENERATED FILE — DO NOT EDIT DIRECTLY; tools/contracts/generate.py
package io.ololabs.toolgate.contracts;

/** Fixed service tool boundary; validate schema before use.
 *
 * @param protocolVersion canonical protocolVersion value
 * @param requestId canonical requestId value
 * @param operation canonical operation value
 * @param invocation canonical invocation value
 * @param agentId canonical agentId value
 */
@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
public record BuiltinIpcRequest(
    @com.fasterxml.jackson.annotation.JsonProperty(value = "protocolVersion", required = true) Long protocolVersion,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "requestId", required = true) String requestId,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "operation", required = true) BuiltinOperation operation,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "invocation", required = false) BuiltinInvocation invocation,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "agentId", required = false) String agentId
) {
    /** Reject absent required references and copy collections to retain value semantics.
     *
     * @param protocolVersion canonical protocolVersion value
     * @param requestId canonical requestId value
     * @param operation canonical operation value
     * @param invocation canonical invocation value
     * @param agentId canonical agentId value
     */
    public BuiltinIpcRequest {
        java.util.Objects.requireNonNull(protocolVersion, "protocolVersion");
        java.util.Objects.requireNonNull(requestId, "requestId");
        java.util.Objects.requireNonNull(operation, "operation");
    }
}
