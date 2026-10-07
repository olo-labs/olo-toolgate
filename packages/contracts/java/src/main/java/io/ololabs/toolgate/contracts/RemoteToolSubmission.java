// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
// GENERATED FILE — DO NOT EDIT DIRECTLY; tools/contracts/generate.py
package io.ololabs.toolgate.contracts;

/** Dedicated Gateway-authenticated request for one device-local tool.
 *
 * @param input canonical input value
 * @param request canonical request value
 * @param expiresAtUnixMs canonical expiresAtUnixMs value
 */
@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
public record RemoteToolSubmission(
    @com.fasterxml.jackson.annotation.JsonProperty(value = "input", required = true) PolicyInput input,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "request", required = true) AuthorizationRequest request,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "expiresAtUnixMs", required = true) Long expiresAtUnixMs
) {
    /** Reject absent required references and copy collections to retain value semantics.
     *
     * @param input canonical input value
     * @param request canonical request value
     * @param expiresAtUnixMs canonical expiresAtUnixMs value
     */
    public RemoteToolSubmission {
        java.util.Objects.requireNonNull(input, "input");
        java.util.Objects.requireNonNull(request, "request");
        java.util.Objects.requireNonNull(expiresAtUnixMs, "expiresAtUnixMs");
    }
}
