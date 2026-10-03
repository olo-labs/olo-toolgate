// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
// GENERATED FILE — DO NOT EDIT DIRECTLY; tools/contracts/generate.py
package io.ololabs.toolgate.contracts;

/** Endpoint identity foundation wire model.
 *
 * @param protocolVersion canonical protocolVersion value
 * @param serverId canonical serverId value
 * @param organization canonical organization value
 * @param tenantId canonical tenantId value
 * @param controlUrl canonical controlUrl value
 * @param gatewayUrl canonical gatewayUrl value
 * @param verificationUri canonical verificationUri value
 * @param minimumClientVersion canonical minimumClientVersion value
 * @param issuedAtUnixMs canonical issuedAtUnixMs value
 * @param expiresAtUnixMs canonical expiresAtUnixMs value
 * @param issuerCertificatePem canonical issuerCertificatePem value
 */
@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
public record ClientDiscovery(
    @com.fasterxml.jackson.annotation.JsonProperty(value = "protocolVersion", required = true) Long protocolVersion,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "serverId", required = true) String serverId,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "organization", required = true) String organization,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "tenantId", required = true) String tenantId,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "controlUrl", required = true) String controlUrl,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "gatewayUrl", required = true) String gatewayUrl,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "verificationUri", required = true) String verificationUri,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "minimumClientVersion", required = true) String minimumClientVersion,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "issuedAtUnixMs", required = true) Long issuedAtUnixMs,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "expiresAtUnixMs", required = true) Long expiresAtUnixMs,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "issuerCertificatePem", required = true) String issuerCertificatePem
) {
    /** Reject absent required references and copy collections to retain value semantics.
     *
     * @param protocolVersion canonical protocolVersion value
     * @param serverId canonical serverId value
     * @param organization canonical organization value
     * @param tenantId canonical tenantId value
     * @param controlUrl canonical controlUrl value
     * @param gatewayUrl canonical gatewayUrl value
     * @param verificationUri canonical verificationUri value
     * @param minimumClientVersion canonical minimumClientVersion value
     * @param issuedAtUnixMs canonical issuedAtUnixMs value
     * @param expiresAtUnixMs canonical expiresAtUnixMs value
     * @param issuerCertificatePem canonical issuerCertificatePem value
     */
    public ClientDiscovery {
        java.util.Objects.requireNonNull(protocolVersion, "protocolVersion");
        java.util.Objects.requireNonNull(serverId, "serverId");
        java.util.Objects.requireNonNull(organization, "organization");
        java.util.Objects.requireNonNull(tenantId, "tenantId");
        java.util.Objects.requireNonNull(controlUrl, "controlUrl");
        java.util.Objects.requireNonNull(gatewayUrl, "gatewayUrl");
        java.util.Objects.requireNonNull(verificationUri, "verificationUri");
        java.util.Objects.requireNonNull(minimumClientVersion, "minimumClientVersion");
        java.util.Objects.requireNonNull(issuedAtUnixMs, "issuedAtUnixMs");
        java.util.Objects.requireNonNull(expiresAtUnixMs, "expiresAtUnixMs");
        java.util.Objects.requireNonNull(issuerCertificatePem, "issuerCertificatePem");
    }
}
