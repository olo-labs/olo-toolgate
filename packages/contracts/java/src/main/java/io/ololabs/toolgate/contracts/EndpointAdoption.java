// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
// GENERATED FILE — DO NOT EDIT DIRECTLY; tools/contracts/generate.py
package io.ololabs.toolgate.contracts;

/** Authenticated device adoption metadata. No owner or per-agent permission cache; discovery and effects require current online authority.
 *
 * @param serverId canonical serverId value
 * @param deviceId canonical deviceId value
 * @param revision canonical revision value
 * @param authorizationEpoch canonical authorizationEpoch value
 * @param digest canonical digest value
 */
@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
public record EndpointAdoption(
    @com.fasterxml.jackson.annotation.JsonProperty(value = "serverId", required = true) String serverId,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "deviceId", required = true) String deviceId,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "revision", required = true) Long revision,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "authorizationEpoch", required = true) Long authorizationEpoch,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "digest", required = true) String digest
) {
    /** Reject absent required references and copy collections to retain value semantics.
     *
     * @param serverId canonical serverId value
     * @param deviceId canonical deviceId value
     * @param revision canonical revision value
     * @param authorizationEpoch canonical authorizationEpoch value
     * @param digest canonical digest value
     */
    public EndpointAdoption {
        java.util.Objects.requireNonNull(serverId, "serverId");
        java.util.Objects.requireNonNull(deviceId, "deviceId");
        java.util.Objects.requireNonNull(revision, "revision");
        java.util.Objects.requireNonNull(authorizationEpoch, "authorizationEpoch");
        java.util.Objects.requireNonNull(digest, "digest");
    }
}
