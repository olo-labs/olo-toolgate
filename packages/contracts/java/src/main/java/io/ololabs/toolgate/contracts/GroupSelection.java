// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
// GENERATED FILE — DO NOT EDIT DIRECTLY; tools/contracts/generate.py
package io.ololabs.toolgate.contracts;

/** Empty means none; all is explicit, tenant-scoped and requires privileged authoring.
 *
 * @param ids canonical ids value
 * @param all canonical all value
 */
@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
public record GroupSelection(
    @com.fasterxml.jackson.annotation.JsonProperty(value = "ids", required = true) java.util.List<String> ids,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "all", required = true) Boolean all
) {
    /** Reject absent required references and copy collections to retain value semantics.
     *
     * @param ids canonical ids value
     * @param all canonical all value
     */
    public GroupSelection {
        java.util.Objects.requireNonNull(ids, "ids");
        ids = ids == null ? null : java.util.List.copyOf(ids);
        java.util.Objects.requireNonNull(all, "all");
    }
}
