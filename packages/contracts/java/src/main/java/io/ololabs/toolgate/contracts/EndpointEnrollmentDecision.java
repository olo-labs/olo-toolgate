// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
// GENERATED FILE — DO NOT EDIT DIRECTLY; tools/contracts/generate.py
package io.ololabs.toolgate.contracts;

/** Endpoint identity foundation wire model.
 *
 * @param userCode canonical userCode value
 * @param keyFingerprint canonical keyFingerprint value
 * @param choice canonical choice value
 * @param connectionExpiresAtUnixMs canonical connectionExpiresAtUnixMs value
 */
@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
public record EndpointEnrollmentDecision(
    @com.fasterxml.jackson.annotation.JsonProperty(value = "userCode", required = true) String userCode,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "keyFingerprint", required = true) String keyFingerprint,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "choice", required = true) EnrollmentChoice choice,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "connectionExpiresAtUnixMs", required = false) Long connectionExpiresAtUnixMs
) {
    /** Reject absent required references and copy collections to retain value semantics.
     *
     * @param userCode canonical userCode value
     * @param keyFingerprint canonical keyFingerprint value
     * @param choice canonical choice value
     * @param connectionExpiresAtUnixMs canonical connectionExpiresAtUnixMs value
     */
    public EndpointEnrollmentDecision {
        java.util.Objects.requireNonNull(userCode, "userCode");
        java.util.Objects.requireNonNull(keyFingerprint, "keyFingerprint");
        java.util.Objects.requireNonNull(choice, "choice");
    }
}
