// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
// GENERATED FILE — DO NOT EDIT DIRECTLY; tools/contracts/generate.py
package io.ololabs.toolgate.contracts;

/** Tenant-scoped control-plane settings shown in the administrative Configuration page. Auto-approval issues bounded device identities without a human decision and is off unless an administrator enables it.
 *
 * @param formatVersion canonical formatVersion value
 * @param revision canonical revision value
 * @param autoApproveDevices canonical autoApproveDevices value
 * @param autoApproveDurationDays canonical autoApproveDurationDays value
 * @param autoApproveOwnerUserId canonical autoApproveOwnerUserId value
 * @param gatewayName canonical gatewayName value
 */
@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
public record ControlServerSettings(
    @com.fasterxml.jackson.annotation.JsonProperty(value = "formatVersion", required = true) Long formatVersion,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "revision", required = true) Long revision,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "autoApproveDevices", required = true) Boolean autoApproveDevices,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "autoApproveDurationDays", required = true) Long autoApproveDurationDays,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "autoApproveOwnerUserId", required = false) String autoApproveOwnerUserId,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "gatewayName", required = false) String gatewayName
) {
    /** Reject absent required references and copy collections to retain value semantics.
     *
     * @param formatVersion canonical formatVersion value
     * @param revision canonical revision value
     * @param autoApproveDevices canonical autoApproveDevices value
     * @param autoApproveDurationDays canonical autoApproveDurationDays value
     * @param autoApproveOwnerUserId canonical autoApproveOwnerUserId value
     * @param gatewayName canonical gatewayName value
     */
    public ControlServerSettings {
        java.util.Objects.requireNonNull(formatVersion, "formatVersion");
        java.util.Objects.requireNonNull(revision, "revision");
        java.util.Objects.requireNonNull(autoApproveDevices, "autoApproveDevices");
        java.util.Objects.requireNonNull(autoApproveDurationDays, "autoApproveDurationDays");
    }
}
