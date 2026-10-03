// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
// GENERATED FILE — DO NOT EDIT DIRECTLY; tools/contracts/generate.py
package io.ololabs.toolgate.contracts;

/** Canonical bounded authoring protocol; declarations never grant execution privileges.
 *
 * @param id canonical id value
 * @param draftId canonical draftId value
 * @param expectedRevision canonical expectedRevision value
 * @param deviceId canonical deviceId value
 * @param exampleIndex canonical exampleIndex value
 */
@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
public record BuilderTestRequest(
    @com.fasterxml.jackson.annotation.JsonProperty(value = "id", required = true) String id,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "draftId", required = true) String draftId,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "expectedRevision", required = true) Long expectedRevision,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "deviceId", required = true) String deviceId,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "exampleIndex", required = true) Long exampleIndex
) {
    /** Reject absent required references and copy collections to retain value semantics.
     *
     * @param id canonical id value
     * @param draftId canonical draftId value
     * @param expectedRevision canonical expectedRevision value
     * @param deviceId canonical deviceId value
     * @param exampleIndex canonical exampleIndex value
     */
    public BuilderTestRequest {
        java.util.Objects.requireNonNull(id, "id");
        java.util.Objects.requireNonNull(draftId, "draftId");
        java.util.Objects.requireNonNull(expectedRevision, "expectedRevision");
        java.util.Objects.requireNonNull(deviceId, "deviceId");
        java.util.Objects.requireNonNull(exampleIndex, "exampleIndex");
    }
}
