// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
// GENERATED FILE — DO NOT EDIT DIRECTLY; tools/contracts/generate.py
package io.ololabs.toolgate.contracts;

/** Reviewed JSON pointer to resources, including every batch member or source/destination.
 *
 * @param pointer canonical pointer value
 * @param kind canonical kind value
 * @param multiple canonical multiple value
 */
@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
public record ExtractorField(
    @com.fasterxml.jackson.annotation.JsonProperty(value = "pointer", required = true) String pointer,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "kind", required = true) ResourceKind kind,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "multiple", required = true) Boolean multiple
) {
    /** Reject absent required references and copy collections to retain value semantics.
     *
     * @param pointer canonical pointer value
     * @param kind canonical kind value
     * @param multiple canonical multiple value
     */
    public ExtractorField {
        java.util.Objects.requireNonNull(pointer, "pointer");
        java.util.Objects.requireNonNull(kind, "kind");
        java.util.Objects.requireNonNull(multiple, "multiple");
    }
}
