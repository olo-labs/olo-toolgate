// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
// GENERATED FILE — DO NOT EDIT DIRECTLY; tools/contracts/generate.py
package io.ololabs.toolgate.contracts;

/** Endpoint identity foundation wire model.
 *
 * @param deviceId canonical deviceId value
 * @param clientVersion canonical clientVersion value
 * @param platform canonical platform value
 * @param csrPem canonical csrPem value
 * @param capabilities canonical capabilities value
 */
@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
public record EndpointEnrollmentStart(
    @com.fasterxml.jackson.annotation.JsonProperty(value = "deviceId", required = true) String deviceId,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "clientVersion", required = true) String clientVersion,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "platform", required = true) ClientPlatform platform,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "csrPem", required = true) String csrPem,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "capabilities", required = true) java.util.List<String> capabilities
) {
    /** Reject absent required references and copy collections to retain value semantics.
     *
     * @param deviceId canonical deviceId value
     * @param clientVersion canonical clientVersion value
     * @param platform canonical platform value
     * @param csrPem canonical csrPem value
     * @param capabilities canonical capabilities value
     */
    public EndpointEnrollmentStart {
        java.util.Objects.requireNonNull(deviceId, "deviceId");
        java.util.Objects.requireNonNull(clientVersion, "clientVersion");
        java.util.Objects.requireNonNull(platform, "platform");
        java.util.Objects.requireNonNull(csrPem, "csrPem");
        java.util.Objects.requireNonNull(capabilities, "capabilities");
        capabilities = capabilities == null ? null : java.util.List.copyOf(capabilities);
    }
}
