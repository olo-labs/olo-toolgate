// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
// GENERATED FILE — DO NOT EDIT DIRECTLY; tools/contracts/generate.py
package io.ololabs.toolgate.contracts;

/** Bounded canonical directory cursor page.
 *
 * @param items canonical items value
 * @param nextCursor canonical nextCursor value
 */
@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
public record ControlExecutionBindingPage(
    @com.fasterxml.jackson.annotation.JsonProperty(value = "items", required = true) java.util.List<ControlExecutionBinding> items,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "nextCursor", required = false) String nextCursor
) {
    /** Reject absent required references and copy collections to retain value semantics.
     *
     * @param items canonical items value
     * @param nextCursor canonical nextCursor value
     */
    public ControlExecutionBindingPage {
        java.util.Objects.requireNonNull(items, "items");
        items = items == null ? null : java.util.List.copyOf(items);
    }
}
