// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
// GENERATED FILE — DO NOT EDIT DIRECTLY; tools/contracts/generate.py
package io.ololabs.toolgate.contracts;

/** Signed fleet lifecycle contract; organization deployment trust never grants runtime permission.
 *
 * @param rollout canonical rollout value
 * @param ready canonical ready value
 * @param failed canonical failed value
 * @param offline canonical offline value
 * @param waiting canonical waiting value
 * @param pending canonical pending value
 * @param superseded canonical superseded value
 */
@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
public record FleetRolloutStatus(
    @com.fasterxml.jackson.annotation.JsonProperty(value = "rollout", required = true) FleetRolloutRecord rollout,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "ready", required = true) Long ready,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "failed", required = true) Long failed,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "offline", required = true) Long offline,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "waiting", required = true) Long waiting,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "pending", required = true) Long pending,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "superseded", required = true) Long superseded
) {
    /** Reject absent required references and copy collections to retain value semantics.
     *
     * @param rollout canonical rollout value
     * @param ready canonical ready value
     * @param failed canonical failed value
     * @param offline canonical offline value
     * @param waiting canonical waiting value
     * @param pending canonical pending value
     * @param superseded canonical superseded value
     */
    public FleetRolloutStatus {
        java.util.Objects.requireNonNull(rollout, "rollout");
        java.util.Objects.requireNonNull(ready, "ready");
        java.util.Objects.requireNonNull(failed, "failed");
        java.util.Objects.requireNonNull(offline, "offline");
        java.util.Objects.requireNonNull(waiting, "waiting");
        java.util.Objects.requireNonNull(pending, "pending");
        java.util.Objects.requireNonNull(superseded, "superseded");
    }
}
