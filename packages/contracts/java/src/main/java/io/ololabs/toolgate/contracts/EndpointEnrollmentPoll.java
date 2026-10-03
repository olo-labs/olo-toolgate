// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
// GENERATED FILE — DO NOT EDIT DIRECTLY; tools/contracts/generate.py
package io.ololabs.toolgate.contracts;

/** Endpoint identity foundation wire model.
 *
 * @param enrollmentId canonical enrollmentId value
 * @param deviceCode canonical deviceCode value
 */
@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
public record EndpointEnrollmentPoll(
    @com.fasterxml.jackson.annotation.JsonProperty(value = "enrollmentId", required = true) String enrollmentId,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "deviceCode", required = true) String deviceCode
) {
    /** Reject absent required references and copy collections to retain value semantics.
     *
     * @param enrollmentId canonical enrollmentId value
     * @param deviceCode canonical deviceCode value
     */
    public EndpointEnrollmentPoll {
        java.util.Objects.requireNonNull(enrollmentId, "enrollmentId");
        java.util.Objects.requireNonNull(deviceCode, "deviceCode");
    }
}
