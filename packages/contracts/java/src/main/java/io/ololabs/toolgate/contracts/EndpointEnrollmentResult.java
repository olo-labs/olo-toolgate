// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
// GENERATED FILE — DO NOT EDIT DIRECTLY; tools/contracts/generate.py
package io.ololabs.toolgate.contracts;

/** Endpoint identity foundation wire model.
 *
 * @param state canonical state value
 * @param identity canonical identity value
 */
@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
public record EndpointEnrollmentResult(
    @com.fasterxml.jackson.annotation.JsonProperty(value = "state", required = true) EnrollmentState state,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "identity", required = false) DeviceIdentity identity
) {
    /** Reject absent required references and copy collections to retain value semantics.
     *
     * @param state canonical state value
     * @param identity canonical identity value
     */
    public EndpointEnrollmentResult {
        java.util.Objects.requireNonNull(state, "state");
    }
}
