// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
// GENERATED FILE — DO NOT EDIT DIRECTLY; tools/contracts/generate.py
package io.ololabs.toolgate.contracts;

/** Administrator-selected immutable tool/runtime image; runtime provisioning is not execution authorization.
 *
 * @param id canonical id value
 * @param kind canonical kind value
 * @param image canonical image value
 * @param version canonical version value
 */
@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
public record ManagedRuntime(
    @com.fasterxml.jackson.annotation.JsonProperty(value = "id", required = true) String id,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "kind", required = true) LocalRuntimeKind kind,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "image", required = true) String image,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "version", required = true) String version
) {
    /** Reject absent required references and copy collections to retain value semantics.
     *
     * @param id canonical id value
     * @param kind canonical kind value
     * @param image canonical image value
     * @param version canonical version value
     */
    public ManagedRuntime {
        java.util.Objects.requireNonNull(id, "id");
        java.util.Objects.requireNonNull(kind, "kind");
        java.util.Objects.requireNonNull(image, "image");
        java.util.Objects.requireNonNull(version, "version");
    }
}
