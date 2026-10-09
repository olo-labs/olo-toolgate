// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
// GENERATED FILE — DO NOT EDIT DIRECTLY; tools/contracts/generate.py
package io.ololabs.toolgate.contracts;

/** Request progress visible to an administrator without arguments or results.
 *
 * @param requestId canonical requestId value
 * @param deviceId canonical deviceId value
 * @param agentId canonical agentId value
 * @param toolId canonical toolId value
 * @param state canonical state value
 * @param receivedAtUnixMs canonical receivedAtUnixMs value
 * @param expiresAtUnixMs canonical expiresAtUnixMs value
 * @param submittedAtUnixMs canonical submittedAtUnixMs value
 * @param responseAtUnixMs canonical responseAtUnixMs value
 * @param completedAtUnixMs canonical completedAtUnixMs value
 * @param error canonical error value
 */
@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
public record RemoteToolRecord(
    @com.fasterxml.jackson.annotation.JsonProperty(value = "requestId", required = true) String requestId,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "deviceId", required = true) String deviceId,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "agentId", required = false) String agentId,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "toolId", required = true) String toolId,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "state", required = true) RemoteToolState state,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "receivedAtUnixMs", required = true) Long receivedAtUnixMs,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "expiresAtUnixMs", required = true) Long expiresAtUnixMs,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "submittedAtUnixMs", required = false) Long submittedAtUnixMs,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "responseAtUnixMs", required = false) Long responseAtUnixMs,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "completedAtUnixMs", required = false) Long completedAtUnixMs,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "error", required = false) ErrorCode error
) {
    /** Reject absent required references and copy collections to retain value semantics.
     *
     * @param requestId canonical requestId value
     * @param deviceId canonical deviceId value
     * @param agentId canonical agentId value
     * @param toolId canonical toolId value
     * @param state canonical state value
     * @param receivedAtUnixMs canonical receivedAtUnixMs value
     * @param expiresAtUnixMs canonical expiresAtUnixMs value
     * @param submittedAtUnixMs canonical submittedAtUnixMs value
     * @param responseAtUnixMs canonical responseAtUnixMs value
     * @param completedAtUnixMs canonical completedAtUnixMs value
     * @param error canonical error value
     */
    public RemoteToolRecord {
        java.util.Objects.requireNonNull(requestId, "requestId");
        java.util.Objects.requireNonNull(deviceId, "deviceId");
        java.util.Objects.requireNonNull(toolId, "toolId");
        java.util.Objects.requireNonNull(state, "state");
        java.util.Objects.requireNonNull(receivedAtUnixMs, "receivedAtUnixMs");
        java.util.Objects.requireNonNull(expiresAtUnixMs, "expiresAtUnixMs");
    }
}
