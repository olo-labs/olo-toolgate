// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
// GENERATED FILE — DO NOT EDIT DIRECTLY; tools/contracts/generate.py
package io.ololabs.toolgate.contracts;

/** Reversible approval decision for an already enrolled key, preserving its owner and directory status.
 *
 * @param expectedApprovalRevision canonical expectedApprovalRevision value
 * @param approved canonical approved value
 * @param connectionExpiresAtUnixMs canonical connectionExpiresAtUnixMs value
 * @param unlimitedConnection canonical unlimitedConnection value
 */
@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
public record EndpointApprovalRequest(
    @com.fasterxml.jackson.annotation.JsonProperty(value = "expectedApprovalRevision", required = true) Long expectedApprovalRevision,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "approved", required = true) Boolean approved,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "connectionExpiresAtUnixMs", required = false) Long connectionExpiresAtUnixMs,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "unlimitedConnection", required = false) Boolean unlimitedConnection
) {
    /** Reject absent required references and copy collections to retain value semantics.
     *
     * @param expectedApprovalRevision canonical expectedApprovalRevision value
     * @param approved canonical approved value
     * @param connectionExpiresAtUnixMs canonical connectionExpiresAtUnixMs value
     * @param unlimitedConnection canonical unlimitedConnection value
     */
    public EndpointApprovalRequest {
        java.util.Objects.requireNonNull(expectedApprovalRevision, "expectedApprovalRevision");
        java.util.Objects.requireNonNull(approved, "approved");
    }
}
