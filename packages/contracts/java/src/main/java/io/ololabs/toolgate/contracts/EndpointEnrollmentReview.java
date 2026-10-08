// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
// GENERATED FILE — DO NOT EDIT DIRECTLY; tools/contracts/generate.py
package io.ololabs.toolgate.contracts;

/** Endpoint identity foundation wire model.
 *
 * @param enrollmentId canonical enrollmentId value
 * @param userCode canonical userCode value
 * @param deviceId canonical deviceId value
 * @param platform canonical platform value
 * @param keyFingerprint canonical keyFingerprint value
 * @param state canonical state value
 * @param expiresAtUnixMs canonical expiresAtUnixMs value
 * @param connectionExpiresAtUnixMs canonical connectionExpiresAtUnixMs value
 */
@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
public record EndpointEnrollmentReview(
    @com.fasterxml.jackson.annotation.JsonProperty(value = "enrollmentId", required = true) String enrollmentId,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "userCode", required = true) String userCode,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "deviceId", required = true) String deviceId,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "platform", required = true) ClientPlatform platform,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "keyFingerprint", required = true) String keyFingerprint,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "state", required = true) EnrollmentState state,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "expiresAtUnixMs", required = true) Long expiresAtUnixMs,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "connectionExpiresAtUnixMs", required = false) Long connectionExpiresAtUnixMs
) {
    /** Reject absent required references and copy collections to retain value semantics.
     *
     * @param enrollmentId canonical enrollmentId value
     * @param userCode canonical userCode value
     * @param deviceId canonical deviceId value
     * @param platform canonical platform value
     * @param keyFingerprint canonical keyFingerprint value
     * @param state canonical state value
     * @param expiresAtUnixMs canonical expiresAtUnixMs value
     * @param connectionExpiresAtUnixMs canonical connectionExpiresAtUnixMs value
     */
    public EndpointEnrollmentReview {
        java.util.Objects.requireNonNull(enrollmentId, "enrollmentId");
        java.util.Objects.requireNonNull(userCode, "userCode");
        java.util.Objects.requireNonNull(deviceId, "deviceId");
        java.util.Objects.requireNonNull(platform, "platform");
        java.util.Objects.requireNonNull(keyFingerprint, "keyFingerprint");
        java.util.Objects.requireNonNull(state, "state");
        java.util.Objects.requireNonNull(expiresAtUnixMs, "expiresAtUnixMs");
    }
}
