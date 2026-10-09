// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
// GENERATED FILE — DO NOT EDIT DIRECTLY; tools/contracts/generate.py
package io.ololabs.toolgate.contracts;

/** Target for current verified-human discovery.
 *
 * @param bindingId canonical bindingId value
 * @param deviceId canonical deviceId value
 */
@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
public record EnterpriseHumanTarget(
    @com.fasterxml.jackson.annotation.JsonProperty(value = "bindingId", required = true) String bindingId,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "deviceId", required = true) String deviceId
) {
    /** Reject absent required references and copy collections to retain value semantics.
     *
     * @param bindingId canonical bindingId value
     * @param deviceId canonical deviceId value
     */
    public EnterpriseHumanTarget {
        java.util.Objects.requireNonNull(bindingId, "bindingId");
        java.util.Objects.requireNonNull(deviceId, "deviceId");
    }
}
