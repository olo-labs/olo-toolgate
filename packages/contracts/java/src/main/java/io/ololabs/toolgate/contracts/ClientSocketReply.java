// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
// GENERATED FILE — DO NOT EDIT DIRECTLY; tools/contracts/generate.py
package io.ololabs.toolgate.contracts;

/** Correlated status and response; body is validated again as the expected response contract.
 *
 * @param requestId canonical requestId value
 * @param status canonical status value
 * @param body canonical body value
 */
@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
public record ClientSocketReply(
    @com.fasterxml.jackson.annotation.JsonProperty(value = "requestId", required = true) String requestId,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "status", required = true) Long status,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "body", required = true) java.util.Map<String, com.fasterxml.jackson.databind.JsonNode> body
) {
    /** Reject absent required references and copy collections to retain value semantics.
     *
     * @param requestId canonical requestId value
     * @param status canonical status value
     * @param body canonical body value
     */
    public ClientSocketReply {
        java.util.Objects.requireNonNull(requestId, "requestId");
        java.util.Objects.requireNonNull(status, "status");
        java.util.Objects.requireNonNull(body, "body");
        body = body == null ? null : java.util.Map.copyOf(body);
    }
}
