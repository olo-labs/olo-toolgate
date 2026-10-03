// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
// GENERATED FILE — DO NOT EDIT DIRECTLY; tools/contracts/generate.py
package io.ololabs.toolgate.contracts;

/** Endpoint identity foundation wire model.
 *
 * @param enrollmentId canonical enrollmentId value
 * @param userCode canonical userCode value
 * @param verificationUri canonical verificationUri value
 * @param expiresAtUnixMs canonical expiresAtUnixMs value
 * @param pollIntervalSeconds canonical pollIntervalSeconds value
 * @param keyFingerprint canonical keyFingerprint value
 */
@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
public record EndpointEnrollmentPrompt(
    @com.fasterxml.jackson.annotation.JsonProperty(value = "enrollmentId", required = true) String enrollmentId,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "userCode", required = true) String userCode,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "verificationUri", required = true) String verificationUri,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "expiresAtUnixMs", required = true) Long expiresAtUnixMs,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "pollIntervalSeconds", required = true) Long pollIntervalSeconds,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "keyFingerprint", required = true) String keyFingerprint
) {
    /** Reject absent required references and copy collections to retain value semantics.
     *
     * @param enrollmentId canonical enrollmentId value
     * @param userCode canonical userCode value
     * @param verificationUri canonical verificationUri value
     * @param expiresAtUnixMs canonical expiresAtUnixMs value
     * @param pollIntervalSeconds canonical pollIntervalSeconds value
     * @param keyFingerprint canonical keyFingerprint value
     */
    public EndpointEnrollmentPrompt {
        java.util.Objects.requireNonNull(enrollmentId, "enrollmentId");
        java.util.Objects.requireNonNull(userCode, "userCode");
        java.util.Objects.requireNonNull(verificationUri, "verificationUri");
        java.util.Objects.requireNonNull(expiresAtUnixMs, "expiresAtUnixMs");
        java.util.Objects.requireNonNull(pollIntervalSeconds, "pollIntervalSeconds");
        java.util.Objects.requireNonNull(keyFingerprint, "keyFingerprint");
    }
}
