// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
// GENERATED FILE — DO NOT EDIT DIRECTLY; tools/contracts/generate.py
package io.ololabs.toolgate.contracts;

/** Declared resource identity; does not grant access or validate a path.
 *
 * @param kind canonical kind value
 * @param locator canonical locator value
 */
@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
public record ResourceDescriptor(
    @com.fasterxml.jackson.annotation.JsonProperty(value = "kind", required = true) ResourceKind kind,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "locator", required = true) String locator
) {
    /** Reject absent required references and copy collections to retain value semantics.
     *
     * @param kind canonical kind value
     * @param locator canonical locator value
     */
    public ResourceDescriptor {
        java.util.Objects.requireNonNull(kind, "kind");
        java.util.Objects.requireNonNull(locator, "locator");
    }
}
