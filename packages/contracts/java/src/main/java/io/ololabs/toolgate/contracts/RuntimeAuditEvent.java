// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
// GENERATED FILE — DO NOT EDIT DIRECTLY; tools/contracts/generate.py
package io.ololabs.toolgate.contracts;

/** Sanitized authorization evaluation event; hashes replace raw arguments and resource locators. A decision is not execution success.
 *
 * @param timestampUnixMs canonical timestampUnixMs value
 * @param context canonical context value
 * @param toolId canonical toolId value
 * @param action canonical action value
 * @param resourceDigest canonical resourceDigest value
 * @param argumentsDigest canonical argumentsDigest value
 * @param decision canonical decision value
 * @param traceId canonical traceId value
 */
@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
public record RuntimeAuditEvent(
    @com.fasterxml.jackson.annotation.JsonProperty(value = "timestampUnixMs", required = true) Long timestampUnixMs,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "context", required = true) RequestContext context,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "toolId", required = true) String toolId,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "action", required = true) String action,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "resourceDigest", required = true) String resourceDigest,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "argumentsDigest", required = true) String argumentsDigest,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "decision", required = true) PolicyDecision decision,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "traceId", required = true) String traceId
) {
    /** Reject absent required references and copy collections to retain value semantics.
     *
     * @param timestampUnixMs canonical timestampUnixMs value
     * @param context canonical context value
     * @param toolId canonical toolId value
     * @param action canonical action value
     * @param resourceDigest canonical resourceDigest value
     * @param argumentsDigest canonical argumentsDigest value
     * @param decision canonical decision value
     * @param traceId canonical traceId value
     */
    public RuntimeAuditEvent {
        java.util.Objects.requireNonNull(timestampUnixMs, "timestampUnixMs");
        java.util.Objects.requireNonNull(context, "context");
        java.util.Objects.requireNonNull(toolId, "toolId");
        java.util.Objects.requireNonNull(action, "action");
        java.util.Objects.requireNonNull(resourceDigest, "resourceDigest");
        java.util.Objects.requireNonNull(argumentsDigest, "argumentsDigest");
        java.util.Objects.requireNonNull(decision, "decision");
        java.util.Objects.requireNonNull(traceId, "traceId");
    }
}
