// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
// GENERATED FILE — DO NOT EDIT DIRECTLY; tools/contracts/generate.py
package io.ololabs.toolgate.contracts;

/** A leased client rechecks the exact pending operation online before execution.
 *
 * @param requestId canonical requestId value
 * @param leaseId canonical leaseId value
 * @param request canonical request value
 */
@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
public record RemoteToolAuthorization(
    @com.fasterxml.jackson.annotation.JsonProperty(value = "requestId", required = true) String requestId,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "leaseId", required = true) String leaseId,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "request", required = true) AuthorizationRequest request
) {
    /** Reject absent required references and copy collections to retain value semantics.
     *
     * @param requestId canonical requestId value
     * @param leaseId canonical leaseId value
     * @param request canonical request value
     */
    public RemoteToolAuthorization {
        java.util.Objects.requireNonNull(requestId, "requestId");
        java.util.Objects.requireNonNull(leaseId, "leaseId");
        java.util.Objects.requireNonNull(request, "request");
    }
}
