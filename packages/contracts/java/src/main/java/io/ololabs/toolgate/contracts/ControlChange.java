// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
// GENERATED FILE — DO NOT EDIT DIRECTLY; tools/contracts/generate.py
package io.ololabs.toolgate.contracts;

/** Deterministic import diff without payload or credential material.
 *
 * @param kind canonical kind value
 * @param id canonical id value
 * @param operation canonical operation value
 */
@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
public record ControlChange(
    @com.fasterxml.jackson.annotation.JsonProperty(value = "kind", required = true) ControlEntityKind kind,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "id", required = true) String id,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "operation", required = true) ControlChangeKind operation
) {
    /** Reject absent required references and copy collections to retain value semantics.
     *
     * @param kind canonical kind value
     * @param id canonical id value
     * @param operation canonical operation value
     */
    public ControlChange {
        java.util.Objects.requireNonNull(kind, "kind");
        java.util.Objects.requireNonNull(id, "id");
        java.util.Objects.requireNonNull(operation, "operation");
    }
}
