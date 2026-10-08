// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
// GENERATED FILE — DO NOT EDIT DIRECTLY; tools/contracts/generate.py
package io.ololabs.toolgate.contracts;

/** Correlated bounded request; body is validated again as the selected operation contract.
 *
 * @param requestId canonical requestId value
 * @param operation canonical operation value
 * @param body canonical body value
 */
@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
public record ClientSocketRequest(
    @com.fasterxml.jackson.annotation.JsonProperty(value = "requestId", required = true) String requestId,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "operation", required = true) ClientSocketOperation operation,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "body", required = false) java.util.Map<String, com.fasterxml.jackson.databind.JsonNode> body
) {
    /** Reject absent required references and copy collections to retain value semantics.
     *
     * @param requestId canonical requestId value
     * @param operation canonical operation value
     * @param body canonical body value
     */
    public ClientSocketRequest {
        java.util.Objects.requireNonNull(requestId, "requestId");
        java.util.Objects.requireNonNull(operation, "operation");
        body = body == null ? null : java.util.Map.copyOf(body);
    }
}
