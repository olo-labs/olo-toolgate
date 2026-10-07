// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
// GENERATED FILE — DO NOT EDIT DIRECTLY; tools/contracts/generate.py
package io.ololabs.toolgate.contracts;

/** Exact leased execution response. Failed execution carries a sanitized error code.
 *
 * @param requestId canonical requestId value
 * @param leaseId canonical leaseId value
 * @param output canonical output value
 * @param error canonical error value
 */
@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
public record RemoteToolResult(
    @com.fasterxml.jackson.annotation.JsonProperty(value = "requestId", required = true) String requestId,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "leaseId", required = true) String leaseId,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "output", required = false) java.util.Map<String, com.fasterxml.jackson.databind.JsonNode> output,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "error", required = false) ErrorCode error
) {
    /** Reject absent required references and copy collections to retain value semantics.
     *
     * @param requestId canonical requestId value
     * @param leaseId canonical leaseId value
     * @param output canonical output value
     * @param error canonical error value
     */
    public RemoteToolResult {
        java.util.Objects.requireNonNull(requestId, "requestId");
        java.util.Objects.requireNonNull(leaseId, "leaseId");
        output = output == null ? null : java.util.Map.copyOf(output);
    }
}
