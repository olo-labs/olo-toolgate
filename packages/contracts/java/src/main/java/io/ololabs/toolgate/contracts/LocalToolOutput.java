// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
// GENERATED FILE — DO NOT EDIT DIRECTLY; tools/contracts/generate.py
package io.ololabs.toolgate.contracts;

/** One bounded JSON stdout document; request binding and output schema are verified.
 *
 * @param protocolVersion canonical protocolVersion value
 * @param requestId canonical requestId value
 * @param output canonical output value
 */
@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
public record LocalToolOutput(
    @com.fasterxml.jackson.annotation.JsonProperty(value = "protocolVersion", required = true) Long protocolVersion,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "requestId", required = true) String requestId,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "output", required = true) java.util.Map<String, com.fasterxml.jackson.databind.JsonNode> output
) {
    /** Reject absent required references and copy collections to retain value semantics.
     *
     * @param protocolVersion canonical protocolVersion value
     * @param requestId canonical requestId value
     * @param output canonical output value
     */
    public LocalToolOutput {
        java.util.Objects.requireNonNull(protocolVersion, "protocolVersion");
        java.util.Objects.requireNonNull(requestId, "requestId");
        java.util.Objects.requireNonNull(output, "output");
        output = output == null ? null : java.util.Map.copyOf(output);
    }
}
