// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
// GENERATED FILE — DO NOT EDIT DIRECTLY; tools/contracts/generate.py
package io.ololabs.toolgate.contracts;

/** One gateway connection held by the device service; the focused connection serves local tool commands.
 *
 * @param serverUrl canonical serverUrl value
 * @param serverName canonical serverName value
 * @param focused canonical focused value
 * @param health canonical health value
 * @param activity canonical activity value
 */
@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
public record ClientGatewayConnection(
    @com.fasterxml.jackson.annotation.JsonProperty(value = "serverUrl", required = true) String serverUrl,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "serverName", required = false) String serverName,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "focused", required = true) Boolean focused,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "health", required = true) ClientHealth health,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "activity", required = false) ClientActivity activity
) {
    /** Reject absent required references and copy collections to retain value semantics.
     *
     * @param serverUrl canonical serverUrl value
     * @param serverName canonical serverName value
     * @param focused canonical focused value
     * @param health canonical health value
     * @param activity canonical activity value
     */
    public ClientGatewayConnection {
        java.util.Objects.requireNonNull(serverUrl, "serverUrl");
        java.util.Objects.requireNonNull(focused, "focused");
        java.util.Objects.requireNonNull(health, "health");
    }
}
