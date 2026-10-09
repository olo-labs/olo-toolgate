// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
// GENERATED FILE — DO NOT EDIT DIRECTLY; tools/contracts/generate.py
package io.ololabs.toolgate.contracts;

/** Online authoritative invocation status. Only a reserved signed permit can proceed to device-authenticated consumption.
 *
 * @param invocation canonical invocation value
 * @param reservation canonical reservation value
 */
@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
public record EnterpriseAuthorizationOutcome(
    @com.fasterxml.jackson.annotation.JsonProperty(value = "invocation", required = true) EnterpriseInvocation invocation,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "reservation", required = false) EnterpriseReservation reservation
) {
    /** Reject absent required references and copy collections to retain value semantics.
     *
     * @param invocation canonical invocation value
     * @param reservation canonical reservation value
     */
    public EnterpriseAuthorizationOutcome {
        java.util.Objects.requireNonNull(invocation, "invocation");
    }
}
