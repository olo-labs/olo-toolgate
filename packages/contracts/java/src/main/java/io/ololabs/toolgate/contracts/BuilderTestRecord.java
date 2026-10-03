// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
// GENERATED FILE — DO NOT EDIT DIRECTLY; tools/contracts/generate.py
package io.ololabs.toolgate.contracts;

/** Canonical bounded authoring protocol; declarations never grant execution privileges.
 *
 * @param id canonical id value
 * @param draftId canonical draftId value
 * @param definitionDigest canonical definitionDigest value
 * @param deviceId canonical deviceId value
 * @param state canonical state value
 * @param revision canonical revision value
 * @param createdAtUnixMs canonical createdAtUnixMs value
 * @param expiresAtUnixMs canonical expiresAtUnixMs value
 * @param attempt canonical attempt value
 * @param leaseId canonical leaseId value
 * @param exampleIndex canonical exampleIndex value
 * @param error canonical error value
 */
@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
public record BuilderTestRecord(
    @com.fasterxml.jackson.annotation.JsonProperty(value = "id", required = true) String id,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "draftId", required = true) String draftId,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "definitionDigest", required = true) String definitionDigest,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "deviceId", required = true) String deviceId,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "state", required = true) BuilderTestState state,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "revision", required = true) Long revision,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "createdAtUnixMs", required = true) Long createdAtUnixMs,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "expiresAtUnixMs", required = true) Long expiresAtUnixMs,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "attempt", required = true) Long attempt,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "leaseId", required = true) String leaseId,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "exampleIndex", required = true) Long exampleIndex,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "error", required = false) ErrorCode error
) {
    /** Reject absent required references and copy collections to retain value semantics.
     *
     * @param id canonical id value
     * @param draftId canonical draftId value
     * @param definitionDigest canonical definitionDigest value
     * @param deviceId canonical deviceId value
     * @param state canonical state value
     * @param revision canonical revision value
     * @param createdAtUnixMs canonical createdAtUnixMs value
     * @param expiresAtUnixMs canonical expiresAtUnixMs value
     * @param attempt canonical attempt value
     * @param leaseId canonical leaseId value
     * @param exampleIndex canonical exampleIndex value
     * @param error canonical error value
     */
    public BuilderTestRecord {
        java.util.Objects.requireNonNull(id, "id");
        java.util.Objects.requireNonNull(draftId, "draftId");
        java.util.Objects.requireNonNull(definitionDigest, "definitionDigest");
        java.util.Objects.requireNonNull(deviceId, "deviceId");
        java.util.Objects.requireNonNull(state, "state");
        java.util.Objects.requireNonNull(revision, "revision");
        java.util.Objects.requireNonNull(createdAtUnixMs, "createdAtUnixMs");
        java.util.Objects.requireNonNull(expiresAtUnixMs, "expiresAtUnixMs");
        java.util.Objects.requireNonNull(attempt, "attempt");
        java.util.Objects.requireNonNull(leaseId, "leaseId");
        java.util.Objects.requireNonNull(exampleIndex, "exampleIndex");
    }
}
