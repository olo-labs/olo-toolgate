// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
// GENERATED FILE — DO NOT EDIT DIRECTLY; tools/contracts/generate.py
package io.ololabs.toolgate.contracts;

/** Machine-readable error without exception text or caller-controlled detail.
 *
 * @param code canonical code value
 * @param requestId canonical requestId value
 * @param retryable canonical retryable value
 */
@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
public record ErrorEnvelope(
    @com.fasterxml.jackson.annotation.JsonProperty(value = "code", required = true) ErrorCode code,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "requestId", required = true) String requestId,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "retryable", required = true) Boolean retryable
) {
    /** Reject absent required references and copy collections to retain value semantics.
     *
     * @param code canonical code value
     * @param requestId canonical requestId value
     * @param retryable canonical retryable value
     */
    public ErrorEnvelope {
        java.util.Objects.requireNonNull(code, "code");
        java.util.Objects.requireNonNull(requestId, "requestId");
        java.util.Objects.requireNonNull(retryable, "retryable");
    }
}
