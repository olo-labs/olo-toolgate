// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
// GENERATED FILE — DO NOT EDIT DIRECTLY; tools/contracts/generate.py
package io.ololabs.toolgate.contracts;

/** Import validation/diff result with the current tenant revision.
 *
 * @param applied canonical applied value
 * @param revision canonical revision value
 * @param changes canonical changes value
 */
@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
public record ControlImportResult(
    @com.fasterxml.jackson.annotation.JsonProperty(value = "applied", required = true) Boolean applied,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "revision", required = true) Long revision,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "changes", required = true) java.util.List<ControlChange> changes
) {
    /** Reject absent required references and copy collections to retain value semantics.
     *
     * @param applied canonical applied value
     * @param revision canonical revision value
     * @param changes canonical changes value
     */
    public ControlImportResult {
        java.util.Objects.requireNonNull(applied, "applied");
        java.util.Objects.requireNonNull(revision, "revision");
        java.util.Objects.requireNonNull(changes, "changes");
        changes = changes == null ? null : java.util.List.copyOf(changes);
    }
}
