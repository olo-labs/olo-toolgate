// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
// GENERATED FILE — DO NOT EDIT DIRECTLY; tools/contracts/generate.py
package io.ololabs.toolgate.contracts;

/** Organization assignment with independent Marketplace evidence; grants no runtime permission.
 *
 * @param assignmentId canonical assignmentId value
 * @param deviceId canonical deviceId value
 * @param release canonical release value
 * @param organizationKeyId canonical organizationKeyId value
 * @param organizationSignature canonical organizationSignature value
 * @param desiredPresence canonical desiredPresence value
 */
@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
public record DeploymentAssignment(
    @com.fasterxml.jackson.annotation.JsonProperty(value = "assignmentId", required = true) String assignmentId,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "deviceId", required = true) String deviceId,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "release", required = true) MarketplaceRelease release,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "organizationKeyId", required = true) String organizationKeyId,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "organizationSignature", required = true) String organizationSignature,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "desiredPresence", required = true) Boolean desiredPresence
) {
    /** Reject absent required references and copy collections to retain value semantics.
     *
     * @param assignmentId canonical assignmentId value
     * @param deviceId canonical deviceId value
     * @param release canonical release value
     * @param organizationKeyId canonical organizationKeyId value
     * @param organizationSignature canonical organizationSignature value
     * @param desiredPresence canonical desiredPresence value
     */
    public DeploymentAssignment {
        java.util.Objects.requireNonNull(assignmentId, "assignmentId");
        java.util.Objects.requireNonNull(deviceId, "deviceId");
        java.util.Objects.requireNonNull(release, "release");
        java.util.Objects.requireNonNull(organizationKeyId, "organizationKeyId");
        java.util.Objects.requireNonNull(organizationSignature, "organizationSignature");
        java.util.Objects.requireNonNull(desiredPresence, "desiredPresence");
    }
}
