// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
// GENERATED FILE — DO NOT EDIT DIRECTLY; tools/contracts/generate.py
package io.ololabs.toolgate.contracts;

/** All present conditions accumulate. Missing trusted evidence denies.
 *
 * @param notBeforeUnixMs canonical notBeforeUnixMs value
 * @param expiresAtUnixMs canonical expiresAtUnixMs value
 * @param networkCidrs canonical networkCidrs value
 * @param devicePosture canonical devicePosture value
 * @param regions canonical regions value
 * @param hoursUtc canonical hoursUtc value
 * @param requireOnline canonical requireOnline value
 * @param highRisk canonical highRisk value
 * @param maxAmountMinorUnits canonical maxAmountMinorUnits value
 * @param maxInvocationsPerMinute canonical maxInvocationsPerMinute value
 */
@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
public record EnterpriseConditions(
    @com.fasterxml.jackson.annotation.JsonProperty(value = "notBeforeUnixMs", required = true) Long notBeforeUnixMs,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "expiresAtUnixMs", required = true) Long expiresAtUnixMs,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "networkCidrs", required = true) java.util.List<String> networkCidrs,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "devicePosture", required = true) java.util.List<String> devicePosture,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "regions", required = true) java.util.List<String> regions,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "hoursUtc", required = true) java.util.List<EnterpriseHours> hoursUtc,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "requireOnline", required = true) Boolean requireOnline,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "highRisk", required = true) Boolean highRisk,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "maxAmountMinorUnits", required = false) Long maxAmountMinorUnits,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "maxInvocationsPerMinute", required = false) Long maxInvocationsPerMinute
) {
    /** Reject absent required references and copy collections to retain value semantics.
     *
     * @param notBeforeUnixMs canonical notBeforeUnixMs value
     * @param expiresAtUnixMs canonical expiresAtUnixMs value
     * @param networkCidrs canonical networkCidrs value
     * @param devicePosture canonical devicePosture value
     * @param regions canonical regions value
     * @param hoursUtc canonical hoursUtc value
     * @param requireOnline canonical requireOnline value
     * @param highRisk canonical highRisk value
     * @param maxAmountMinorUnits canonical maxAmountMinorUnits value
     * @param maxInvocationsPerMinute canonical maxInvocationsPerMinute value
     */
    public EnterpriseConditions {
        java.util.Objects.requireNonNull(notBeforeUnixMs, "notBeforeUnixMs");
        java.util.Objects.requireNonNull(expiresAtUnixMs, "expiresAtUnixMs");
        java.util.Objects.requireNonNull(networkCidrs, "networkCidrs");
        networkCidrs = networkCidrs == null ? null : java.util.List.copyOf(networkCidrs);
        java.util.Objects.requireNonNull(devicePosture, "devicePosture");
        devicePosture = devicePosture == null ? null : java.util.List.copyOf(devicePosture);
        java.util.Objects.requireNonNull(regions, "regions");
        regions = regions == null ? null : java.util.List.copyOf(regions);
        java.util.Objects.requireNonNull(hoursUtc, "hoursUtc");
        hoursUtc = hoursUtc == null ? null : java.util.List.copyOf(hoursUtc);
        java.util.Objects.requireNonNull(requireOnline, "requireOnline");
        java.util.Objects.requireNonNull(highRisk, "highRisk");
    }
}
