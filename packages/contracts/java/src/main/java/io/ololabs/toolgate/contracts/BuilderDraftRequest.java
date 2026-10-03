// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
// GENERATED FILE — DO NOT EDIT DIRECTLY; tools/contracts/generate.py
package io.ololabs.toolgate.contracts;

/** Canonical bounded authoring protocol; declarations never grant execution privileges.
 *
 * @param id canonical id value
 * @param expectedRevision canonical expectedRevision value
 * @param definition canonical definition value
 */
@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
public record BuilderDraftRequest(
    @com.fasterxml.jackson.annotation.JsonProperty(value = "id", required = true) String id,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "expectedRevision", required = true) Long expectedRevision,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "definition", required = true) BuilderDefinition definition
) {
    /** Reject absent required references and copy collections to retain value semantics.
     *
     * @param id canonical id value
     * @param expectedRevision canonical expectedRevision value
     * @param definition canonical definition value
     */
    public BuilderDraftRequest {
        java.util.Objects.requireNonNull(id, "id");
        java.util.Objects.requireNonNull(expectedRevision, "expectedRevision");
        java.util.Objects.requireNonNull(definition, "definition");
    }
}
