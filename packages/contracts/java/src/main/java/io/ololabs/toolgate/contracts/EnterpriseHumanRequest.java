// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
// GENERATED FILE — DO NOT EDIT DIRECTLY; tools/contracts/generate.py
package io.ololabs.toolgate.contracts;

/** Verified human selects an exact execution binding and target. User and session facts are always supplied by the authenticated adapter.
 *
 * @param bindingId canonical bindingId value
 * @param deviceId canonical deviceId value
 * @param request canonical request value
 */
@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
public record EnterpriseHumanRequest(
    @com.fasterxml.jackson.annotation.JsonProperty(value = "bindingId", required = true) String bindingId,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "deviceId", required = true) String deviceId,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "request", required = true) AuthorizationRequest request
) {
    /** Reject absent required references and copy collections to retain value semantics.
     *
     * @param bindingId canonical bindingId value
     * @param deviceId canonical deviceId value
     * @param request canonical request value
     */
    public EnterpriseHumanRequest {
        java.util.Objects.requireNonNull(bindingId, "bindingId");
        java.util.Objects.requireNonNull(deviceId, "deviceId");
        java.util.Objects.requireNonNull(request, "request");
    }
}
