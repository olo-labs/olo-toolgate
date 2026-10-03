// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
// GENERATED FILE — DO NOT EDIT DIRECTLY; tools/contracts/generate.py
package io.ololabs.toolgate.contracts;

/** Canonical redacted execution/status response for IPC revision 3.
 *
 * @param requestId canonical requestId value
 * @param health canonical health value
 * @param result canonical result value
 * @param error canonical error value
 */
@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
public record LocalRuntimeIpcResponse(
    @com.fasterxml.jackson.annotation.JsonProperty(value = "requestId", required = true) String requestId,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "health", required = false) LocalRuntimeHealth health,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "result", required = false) LocalToolOutput result,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "error", required = false) ErrorCode error
) {
    /** Reject absent required references and copy collections to retain value semantics.
     *
     * @param requestId canonical requestId value
     * @param health canonical health value
     * @param result canonical result value
     * @param error canonical error value
     */
    public LocalRuntimeIpcResponse {
        java.util.Objects.requireNonNull(requestId, "requestId");
    }
}
