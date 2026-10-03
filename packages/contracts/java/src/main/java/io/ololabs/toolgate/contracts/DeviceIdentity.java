// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
// GENERATED FILE — DO NOT EDIT DIRECTLY; tools/contracts/generate.py
package io.ololabs.toolgate.contracts;

/** Endpoint identity foundation wire model.
 *
 * @param deviceId canonical deviceId value
 * @param tenantId canonical tenantId value
 * @param userId canonical userId value
 * @param serverId canonical serverId value
 * @param certificatePem canonical certificatePem value
 * @param issuerCertificatePem canonical issuerCertificatePem value
 * @param expiresAtUnixMs canonical expiresAtUnixMs value
 */
@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
public record DeviceIdentity(
    @com.fasterxml.jackson.annotation.JsonProperty(value = "deviceId", required = true) String deviceId,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "tenantId", required = true) String tenantId,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "userId", required = true) String userId,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "serverId", required = true) String serverId,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "certificatePem", required = true) String certificatePem,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "issuerCertificatePem", required = true) String issuerCertificatePem,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "expiresAtUnixMs", required = true) Long expiresAtUnixMs
) {
    /** Reject absent required references and copy collections to retain value semantics.
     *
     * @param deviceId canonical deviceId value
     * @param tenantId canonical tenantId value
     * @param userId canonical userId value
     * @param serverId canonical serverId value
     * @param certificatePem canonical certificatePem value
     * @param issuerCertificatePem canonical issuerCertificatePem value
     * @param expiresAtUnixMs canonical expiresAtUnixMs value
     */
    public DeviceIdentity {
        java.util.Objects.requireNonNull(deviceId, "deviceId");
        java.util.Objects.requireNonNull(tenantId, "tenantId");
        java.util.Objects.requireNonNull(userId, "userId");
        java.util.Objects.requireNonNull(serverId, "serverId");
        java.util.Objects.requireNonNull(certificatePem, "certificatePem");
        java.util.Objects.requireNonNull(issuerCertificatePem, "issuerCertificatePem");
        java.util.Objects.requireNonNull(expiresAtUnixMs, "expiresAtUnixMs");
    }
}
