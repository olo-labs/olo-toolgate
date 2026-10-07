// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
// GENERATED FILE — DO NOT EDIT DIRECTLY; tools/contracts/generate.py
package io.ololabs.toolgate.contracts;

/** Server-cached complete replacement of device permissions, acknowledged by digest on the next authenticated poll.
 *
 * @param serverId canonical serverId value
 * @param deviceId canonical deviceId value
 * @param userId canonical userId value
 * @param revision canonical revision value
 * @param digest canonical digest value
 * @param permissions canonical permissions value
 */
@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
public record EndpointPermissionConfiguration(
    @com.fasterxml.jackson.annotation.JsonProperty(value = "serverId", required = true) String serverId,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "deviceId", required = true) String deviceId,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "userId", required = true) String userId,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "revision", required = true) Long revision,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "digest", required = true) String digest,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "permissions", required = true) java.util.List<EndpointPermissionRule> permissions
) {
    /** Reject absent required references and copy collections to retain value semantics.
     *
     * @param serverId canonical serverId value
     * @param deviceId canonical deviceId value
     * @param userId canonical userId value
     * @param revision canonical revision value
     * @param digest canonical digest value
     * @param permissions canonical permissions value
     */
    public EndpointPermissionConfiguration {
        java.util.Objects.requireNonNull(serverId, "serverId");
        java.util.Objects.requireNonNull(deviceId, "deviceId");
        java.util.Objects.requireNonNull(userId, "userId");
        java.util.Objects.requireNonNull(revision, "revision");
        java.util.Objects.requireNonNull(digest, "digest");
        java.util.Objects.requireNonNull(permissions, "permissions");
        permissions = permissions == null ? null : java.util.List.copyOf(permissions);
    }
}
