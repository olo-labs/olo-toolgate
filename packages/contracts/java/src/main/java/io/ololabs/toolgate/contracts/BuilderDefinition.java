// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
// GENERATED FILE — DO NOT EDIT DIRECTLY; tools/contracts/generate.py
package io.ololabs.toolgate.contracts;

/** Canonical bounded authoring protocol; declarations never grant execution privileges.
 *
 * @param packageId canonical packageId value
 * @param version canonical version value
 * @param name canonical name value
 * @param description canonical description value
 * @param useWhen canonical useWhen value
 * @param doNotUseWhen canonical doNotUseWhen value
 * @param runtime canonical runtime value
 * @param tool canonical tool value
 * @param platforms canonical platforms value
 * @param architectures canonical architectures value
 * @param examples canonical examples value
 * @param permissions canonical permissions value
 * @param resource canonical resource value
 * @param credentialRequirements canonical credentialRequirements value
 */
@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
public record BuilderDefinition(
    @com.fasterxml.jackson.annotation.JsonProperty(value = "packageId", required = true) String packageId,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "version", required = true) String version,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "name", required = true) String name,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "description", required = true) String description,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "useWhen", required = true) String useWhen,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "doNotUseWhen", required = true) String doNotUseWhen,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "runtime", required = true) ManagedRuntime runtime,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "tool", required = true) LocalToolRegistration tool,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "platforms", required = true) java.util.List<ClientPlatform> platforms,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "architectures", required = true) java.util.List<FleetArchitecture> architectures,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "examples", required = true) java.util.List<FleetSelfTest> examples,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "permissions", required = true) java.util.List<BuilderPermission> permissions,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "resource", required = true) ResourceDescriptor resource,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "credentialRequirements", required = true) java.util.List<String> credentialRequirements
) {
    /** Reject absent required references and copy collections to retain value semantics.
     *
     * @param packageId canonical packageId value
     * @param version canonical version value
     * @param name canonical name value
     * @param description canonical description value
     * @param useWhen canonical useWhen value
     * @param doNotUseWhen canonical doNotUseWhen value
     * @param runtime canonical runtime value
     * @param tool canonical tool value
     * @param platforms canonical platforms value
     * @param architectures canonical architectures value
     * @param examples canonical examples value
     * @param permissions canonical permissions value
     * @param resource canonical resource value
     * @param credentialRequirements canonical credentialRequirements value
     */
    public BuilderDefinition {
        java.util.Objects.requireNonNull(packageId, "packageId");
        java.util.Objects.requireNonNull(version, "version");
        java.util.Objects.requireNonNull(name, "name");
        java.util.Objects.requireNonNull(description, "description");
        java.util.Objects.requireNonNull(useWhen, "useWhen");
        java.util.Objects.requireNonNull(doNotUseWhen, "doNotUseWhen");
        java.util.Objects.requireNonNull(runtime, "runtime");
        java.util.Objects.requireNonNull(tool, "tool");
        java.util.Objects.requireNonNull(platforms, "platforms");
        platforms = platforms == null ? null : java.util.List.copyOf(platforms);
        java.util.Objects.requireNonNull(architectures, "architectures");
        architectures = architectures == null ? null : java.util.List.copyOf(architectures);
        java.util.Objects.requireNonNull(examples, "examples");
        examples = examples == null ? null : java.util.List.copyOf(examples);
        java.util.Objects.requireNonNull(permissions, "permissions");
        permissions = permissions == null ? null : java.util.List.copyOf(permissions);
        java.util.Objects.requireNonNull(resource, "resource");
        java.util.Objects.requireNonNull(credentialRequirements, "credentialRequirements");
        credentialRequirements = credentialRequirements == null ? null : java.util.List.copyOf(credentialRequirements);
    }
}
