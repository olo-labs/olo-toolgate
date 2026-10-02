// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
// GENERATED FILE — DO NOT EDIT DIRECTLY; tools/contracts/generate.py
package io.ololabs.toolgate.contracts;

/** Named operation and declared resource kinds.
 *
 * @param name canonical name value
 * @param resourceKinds canonical resourceKinds value
 */
@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
public record ToolAction(
    @com.fasterxml.jackson.annotation.JsonProperty(value = "name", required = true) String name,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "resourceKinds", required = true) java.util.List<ResourceKind> resourceKinds
) {
    /** Reject absent required references and copy collections to retain value semantics.
     *
     * @param name canonical name value
     * @param resourceKinds canonical resourceKinds value
     */
    public ToolAction {
        java.util.Objects.requireNonNull(name, "name");
        java.util.Objects.requireNonNull(resourceKinds, "resourceKinds");
        resourceKinds = resourceKinds == null ? null : java.util.List.copyOf(resourceKinds);
    }
}
