// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
// GENERATED FILE — DO NOT EDIT DIRECTLY; tools/contracts/generate.py
package io.ololabs.toolgate.contracts;

/** Durable invocation reservation and its exact signed capability.
 *
 * @param invocation canonical invocation value
 * @param permit canonical permit value
 */
@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
public record EnterpriseReservation(
    @com.fasterxml.jackson.annotation.JsonProperty(value = "invocation", required = true) EnterpriseInvocation invocation,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "permit", required = true) EnterpriseSignedPermit permit
) {
    /** Reject absent required references and copy collections to retain value semantics.
     *
     * @param invocation canonical invocation value
     * @param permit canonical permit value
     */
    public EnterpriseReservation {
        java.util.Objects.requireNonNull(invocation, "invocation");
        java.util.Objects.requireNonNull(permit, "permit");
    }
}
