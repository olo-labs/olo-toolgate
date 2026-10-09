// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
// GENERATED FILE — DO NOT EDIT DIRECTLY; tools/contracts/generate.py
package io.ololabs.toolgate.contracts;

/** Certificate-authenticated current device acknowledgement. Acknowledgement is metadata, never an execution capability.
 *
 * @param deviceId canonical deviceId value
 * @param directoryRevision canonical directoryRevision value
 * @param authorizationEpoch canonical authorizationEpoch value
 * @param graphDigest canonical graphDigest value
 * @param observedAtUnixMs canonical observedAtUnixMs value
 */
@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
public record EnterpriseAdoptionStatus(
    @com.fasterxml.jackson.annotation.JsonProperty(value = "deviceId", required = true) String deviceId,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "directoryRevision", required = true) Long directoryRevision,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "authorizationEpoch", required = true) Long authorizationEpoch,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "graphDigest", required = true) String graphDigest,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "observedAtUnixMs", required = true) Long observedAtUnixMs
) {
    /** Reject absent required references and copy collections to retain value semantics.
     *
     * @param deviceId canonical deviceId value
     * @param directoryRevision canonical directoryRevision value
     * @param authorizationEpoch canonical authorizationEpoch value
     * @param graphDigest canonical graphDigest value
     * @param observedAtUnixMs canonical observedAtUnixMs value
     */
    public EnterpriseAdoptionStatus {
        java.util.Objects.requireNonNull(deviceId, "deviceId");
        java.util.Objects.requireNonNull(directoryRevision, "directoryRevision");
        java.util.Objects.requireNonNull(authorizationEpoch, "authorizationEpoch");
        java.util.Objects.requireNonNull(graphDigest, "graphDigest");
        java.util.Objects.requireNonNull(observedAtUnixMs, "observedAtUnixMs");
    }
}
