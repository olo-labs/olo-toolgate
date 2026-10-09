// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
// GENERATED FILE — DO NOT EDIT DIRECTLY; tools/contracts/generate.py
package io.ololabs.toolgate.contracts;

/** Administrator/attestation sourced facts; callers cannot supply authoritative evidence.
 *
 * @param id canonical id value
 * @param name canonical name value
 * @param enabled canonical enabled value
 * @param revision canonical revision value
 * @param deviceId canonical deviceId value
 * @param posture canonical posture value
 * @param region canonical region value
 * @param verifiedNetworkAddress canonical verifiedNetworkAddress value
 * @param verifiedAtUnixMs canonical verifiedAtUnixMs value
 * @param expiresAtUnixMs canonical expiresAtUnixMs value
 */
@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
public record ControlDeviceEvidence(
    @com.fasterxml.jackson.annotation.JsonProperty(value = "id", required = true) String id,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "name", required = true) String name,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "enabled", required = true) Boolean enabled,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "revision", required = true) Long revision,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "deviceId", required = true) String deviceId,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "posture", required = true) java.util.List<String> posture,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "region", required = true) String region,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "verifiedNetworkAddress", required = true) String verifiedNetworkAddress,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "verifiedAtUnixMs", required = true) Long verifiedAtUnixMs,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "expiresAtUnixMs", required = true) Long expiresAtUnixMs
) {
    /** Reject absent required references and copy collections to retain value semantics.
     *
     * @param id canonical id value
     * @param name canonical name value
     * @param enabled canonical enabled value
     * @param revision canonical revision value
     * @param deviceId canonical deviceId value
     * @param posture canonical posture value
     * @param region canonical region value
     * @param verifiedNetworkAddress canonical verifiedNetworkAddress value
     * @param verifiedAtUnixMs canonical verifiedAtUnixMs value
     * @param expiresAtUnixMs canonical expiresAtUnixMs value
     */
    public ControlDeviceEvidence {
        java.util.Objects.requireNonNull(id, "id");
        java.util.Objects.requireNonNull(name, "name");
        java.util.Objects.requireNonNull(enabled, "enabled");
        java.util.Objects.requireNonNull(revision, "revision");
        java.util.Objects.requireNonNull(deviceId, "deviceId");
        java.util.Objects.requireNonNull(posture, "posture");
        posture = posture == null ? null : java.util.List.copyOf(posture);
        java.util.Objects.requireNonNull(region, "region");
        java.util.Objects.requireNonNull(verifiedNetworkAddress, "verifiedNetworkAddress");
        java.util.Objects.requireNonNull(verifiedAtUnixMs, "verifiedAtUnixMs");
        java.util.Objects.requireNonNull(expiresAtUnixMs, "expiresAtUnixMs");
    }
}
