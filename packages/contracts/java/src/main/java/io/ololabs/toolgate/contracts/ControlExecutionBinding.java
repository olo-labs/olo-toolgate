// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
// GENERATED FILE — DO NOT EDIT DIRECTLY; tools/contracts/generate.py
package io.ololabs.toolgate.contracts;

/** Explicit Tool Group to Device Group binding; one binding is selected per invocation.
 *
 * @param id canonical id value
 * @param name canonical name value
 * @param enabled canonical enabled value
 * @param revision canonical revision value
 * @param toolGroupId canonical toolGroupId value
 * @param deviceGroupId canonical deviceGroupId value
 * @param actions canonical actions value
 * @param allowedPackageDigests canonical allowedPackageDigests value
 * @param requireOnline canonical requireOnline value
 * @param ownerDependency canonical ownerDependency value
 */
@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
public record ControlExecutionBinding(
    @com.fasterxml.jackson.annotation.JsonProperty(value = "id", required = true) String id,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "name", required = true) String name,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "enabled", required = true) Boolean enabled,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "revision", required = true) Long revision,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "toolGroupId", required = true) String toolGroupId,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "deviceGroupId", required = true) String deviceGroupId,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "actions", required = true) java.util.List<String> actions,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "allowedPackageDigests", required = true) java.util.List<String> allowedPackageDigests,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "requireOnline", required = true) Boolean requireOnline,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "ownerDependency", required = true) Boolean ownerDependency
) {
    /** Reject absent required references and copy collections to retain value semantics.
     *
     * @param id canonical id value
     * @param name canonical name value
     * @param enabled canonical enabled value
     * @param revision canonical revision value
     * @param toolGroupId canonical toolGroupId value
     * @param deviceGroupId canonical deviceGroupId value
     * @param actions canonical actions value
     * @param allowedPackageDigests canonical allowedPackageDigests value
     * @param requireOnline canonical requireOnline value
     * @param ownerDependency canonical ownerDependency value
     */
    public ControlExecutionBinding {
        java.util.Objects.requireNonNull(id, "id");
        java.util.Objects.requireNonNull(name, "name");
        java.util.Objects.requireNonNull(enabled, "enabled");
        java.util.Objects.requireNonNull(revision, "revision");
        java.util.Objects.requireNonNull(toolGroupId, "toolGroupId");
        java.util.Objects.requireNonNull(deviceGroupId, "deviceGroupId");
        java.util.Objects.requireNonNull(actions, "actions");
        actions = actions == null ? null : java.util.List.copyOf(actions);
        java.util.Objects.requireNonNull(allowedPackageDigests, "allowedPackageDigests");
        allowedPackageDigests = allowedPackageDigests == null ? null : java.util.List.copyOf(allowedPackageDigests);
        java.util.Objects.requireNonNull(requireOnline, "requireOnline");
        java.util.Objects.requireNonNull(ownerDependency, "ownerDependency");
    }
}
