// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
// GENERATED FILE — DO NOT EDIT DIRECTLY; tools/contracts/generate.py
package io.ololabs.toolgate.contracts;

/** Scoped operational health and configured hard bounds. Snapshot lag never authorizes a cached effect.
 *
 * @param pendingSnapshotEvents canonical pendingSnapshotEvents value
 * @param oldestUnpublishedUnixMs canonical oldestUnpublishedUnixMs value
 * @param unknownOutcomes canonical unknownOutcomes value
 * @param expiredRunningEffects canonical expiredRunningEffects value
 * @param permitLifetimeMs canonical permitLifetimeMs value
 * @param clockSkewMs canonical clockSkewMs value
 * @param authorityCacheGraceMs canonical authorityCacheGraceMs value
 * @param readyProbeFreshnessMs canonical readyProbeFreshnessMs value
 */
@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
public record EnterpriseOperationalStatus(
    @com.fasterxml.jackson.annotation.JsonProperty(value = "pendingSnapshotEvents", required = true) Long pendingSnapshotEvents,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "oldestUnpublishedUnixMs", required = true) Long oldestUnpublishedUnixMs,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "unknownOutcomes", required = true) Long unknownOutcomes,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "expiredRunningEffects", required = true) Long expiredRunningEffects,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "permitLifetimeMs", required = true) Long permitLifetimeMs,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "clockSkewMs", required = true) Long clockSkewMs,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "authorityCacheGraceMs", required = true) Long authorityCacheGraceMs,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "readyProbeFreshnessMs", required = true) Long readyProbeFreshnessMs
) {
    /** Reject absent required references and copy collections to retain value semantics.
     *
     * @param pendingSnapshotEvents canonical pendingSnapshotEvents value
     * @param oldestUnpublishedUnixMs canonical oldestUnpublishedUnixMs value
     * @param unknownOutcomes canonical unknownOutcomes value
     * @param expiredRunningEffects canonical expiredRunningEffects value
     * @param permitLifetimeMs canonical permitLifetimeMs value
     * @param clockSkewMs canonical clockSkewMs value
     * @param authorityCacheGraceMs canonical authorityCacheGraceMs value
     * @param readyProbeFreshnessMs canonical readyProbeFreshnessMs value
     */
    public EnterpriseOperationalStatus {
        java.util.Objects.requireNonNull(pendingSnapshotEvents, "pendingSnapshotEvents");
        java.util.Objects.requireNonNull(oldestUnpublishedUnixMs, "oldestUnpublishedUnixMs");
        java.util.Objects.requireNonNull(unknownOutcomes, "unknownOutcomes");
        java.util.Objects.requireNonNull(expiredRunningEffects, "expiredRunningEffects");
        java.util.Objects.requireNonNull(permitLifetimeMs, "permitLifetimeMs");
        java.util.Objects.requireNonNull(clockSkewMs, "clockSkewMs");
        java.util.Objects.requireNonNull(authorityCacheGraceMs, "authorityCacheGraceMs");
        java.util.Objects.requireNonNull(readyProbeFreshnessMs, "readyProbeFreshnessMs");
    }
}
