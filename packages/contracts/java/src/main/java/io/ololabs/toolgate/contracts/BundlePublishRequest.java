// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
// GENERATED FILE — DO NOT EDIT DIRECTLY; tools/contracts/generate.py
package io.ololabs.toolgate.contracts;

/** Publish a consistent directory snapshot or roll back into a new sequence. Requires Idempotency-Key.
 *
 * @param directoryRevision canonical directoryRevision value
 * @param expectedSequence canonical expectedSequence value
 * @param lifetimeMs canonical lifetimeMs value
 * @param graceMs canonical graceMs value
 * @param gracePolicyIds canonical gracePolicyIds value
 * @param rollbackOf canonical rollbackOf value
 */
@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
public record BundlePublishRequest(
    @com.fasterxml.jackson.annotation.JsonProperty(value = "directoryRevision", required = true) Long directoryRevision,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "expectedSequence", required = true) Long expectedSequence,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "lifetimeMs", required = true) Long lifetimeMs,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "graceMs", required = true) Long graceMs,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "gracePolicyIds", required = false) java.util.List<String> gracePolicyIds,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "rollbackOf", required = false) Long rollbackOf
) {
    /** Reject absent required references and copy collections to retain value semantics.
     *
     * @param directoryRevision canonical directoryRevision value
     * @param expectedSequence canonical expectedSequence value
     * @param lifetimeMs canonical lifetimeMs value
     * @param graceMs canonical graceMs value
     * @param gracePolicyIds canonical gracePolicyIds value
     * @param rollbackOf canonical rollbackOf value
     */
    public BundlePublishRequest {
        java.util.Objects.requireNonNull(directoryRevision, "directoryRevision");
        java.util.Objects.requireNonNull(expectedSequence, "expectedSequence");
        java.util.Objects.requireNonNull(lifetimeMs, "lifetimeMs");
        java.util.Objects.requireNonNull(graceMs, "graceMs");
        gracePolicyIds = gracePolicyIds == null ? null : java.util.List.copyOf(gracePolicyIds);
    }
}
