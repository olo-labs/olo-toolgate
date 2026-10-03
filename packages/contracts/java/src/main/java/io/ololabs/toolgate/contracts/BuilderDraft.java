// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
// GENERATED FILE — DO NOT EDIT DIRECTLY; tools/contracts/generate.py
package io.ololabs.toolgate.contracts;

/** Canonical bounded authoring protocol; declarations never grant execution privileges.
 *
 * @param id canonical id value
 * @param revision canonical revision value
 * @param definition canonical definition value
 * @param definitionDigest canonical definitionDigest value
 * @param sealed canonical sealed value
 * @param packageDocument canonical packageDocument value
 */
@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
public record BuilderDraft(
    @com.fasterxml.jackson.annotation.JsonProperty(value = "id", required = true) String id,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "revision", required = true) Long revision,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "definition", required = true) BuilderDefinition definition,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "definitionDigest", required = true) String definitionDigest,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "sealed", required = true) Boolean sealed,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "packageDocument", required = true) FleetPackageDocument packageDocument
) {
    /** Reject absent required references and copy collections to retain value semantics.
     *
     * @param id canonical id value
     * @param revision canonical revision value
     * @param definition canonical definition value
     * @param definitionDigest canonical definitionDigest value
     * @param sealed canonical sealed value
     * @param packageDocument canonical packageDocument value
     */
    public BuilderDraft {
        java.util.Objects.requireNonNull(id, "id");
        java.util.Objects.requireNonNull(revision, "revision");
        java.util.Objects.requireNonNull(definition, "definition");
        java.util.Objects.requireNonNull(definitionDigest, "definitionDigest");
        java.util.Objects.requireNonNull(sealed, "sealed");
        java.util.Objects.requireNonNull(packageDocument, "packageDocument");
    }
}
