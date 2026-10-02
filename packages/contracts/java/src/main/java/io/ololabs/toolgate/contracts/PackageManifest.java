// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
// GENERATED FILE — DO NOT EDIT DIRECTLY; tools/contracts/generate.py
package io.ololabs.toolgate.contracts;

/** Portable package metadata. Credential values are forbidden.
 *
 * @param schemaVersion canonical schemaVersion value
 * @param id canonical id value
 * @param version canonical version value
 * @param tools canonical tools value
 * @param artifacts canonical artifacts value
 * @param credentialReferences canonical credentialReferences value
 * @param compatibility canonical compatibility value
 */
@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
public record PackageManifest(
    @com.fasterxml.jackson.annotation.JsonProperty(value = "schemaVersion", required = true) Long schemaVersion,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "id", required = true) String id,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "version", required = true) String version,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "tools", required = true) java.util.List<ToolDefinition> tools,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "artifacts", required = true) java.util.List<ArtifactDescriptor> artifacts,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "credentialReferences", required = true) java.util.List<String> credentialReferences,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "compatibility", required = true) PackageCompatibility compatibility
) {
    /** Reject absent required references and copy collections to retain value semantics.
     *
     * @param schemaVersion canonical schemaVersion value
     * @param id canonical id value
     * @param version canonical version value
     * @param tools canonical tools value
     * @param artifacts canonical artifacts value
     * @param credentialReferences canonical credentialReferences value
     * @param compatibility canonical compatibility value
     */
    public PackageManifest {
        java.util.Objects.requireNonNull(schemaVersion, "schemaVersion");
        java.util.Objects.requireNonNull(id, "id");
        java.util.Objects.requireNonNull(version, "version");
        java.util.Objects.requireNonNull(tools, "tools");
        tools = tools == null ? null : java.util.List.copyOf(tools);
        java.util.Objects.requireNonNull(artifacts, "artifacts");
        artifacts = artifacts == null ? null : java.util.List.copyOf(artifacts);
        java.util.Objects.requireNonNull(credentialReferences, "credentialReferences");
        credentialReferences = credentialReferences == null ? null : java.util.List.copyOf(credentialReferences);
        java.util.Objects.requireNonNull(compatibility, "compatibility");
    }
}
