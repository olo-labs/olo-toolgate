// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
// GENERATED FILE — DO NOT EDIT DIRECTLY; tools/contracts/generate.py
package io.ololabs.toolgate.contracts;

/** Authenticated runtime request to verify and atomically consume one exact-bound permit.
 *
 * @param permit canonical permit value
 * @param request canonical request value
 */
@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
public record ExecutionPermitUseRequest(
    @com.fasterxml.jackson.annotation.JsonProperty(value = "permit", required = true) SignedExecutionPermit permit,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "request", required = true) AuthorizationRequest request
) {
    /** Reject absent required references and copy collections to retain value semantics.
     *
     * @param permit canonical permit value
     * @param request canonical request value
     */
    public ExecutionPermitUseRequest {
        java.util.Objects.requireNonNull(permit, "permit");
        java.util.Objects.requireNonNull(request, "request");
    }
}
