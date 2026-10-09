// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
// GENERATED FILE — DO NOT EDIT DIRECTLY; tools/contracts/generate.py
package io.ololabs.toolgate.contracts;

/** Scoped durable effect states. A cursor does not disclose hidden rows.
 *
 * @param items canonical items value
 * @param nextCursor canonical nextCursor value
 */
@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
public record EnterpriseInvocationPage(
    @com.fasterxml.jackson.annotation.JsonProperty(value = "items", required = true) java.util.List<EnterpriseInvocation> items,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "nextCursor", required = false) String nextCursor
) {
    /** Reject absent required references and copy collections to retain value semantics.
     *
     * @param items canonical items value
     * @param nextCursor canonical nextCursor value
     */
    public EnterpriseInvocationPage {
        java.util.Objects.requireNonNull(items, "items");
        items = items == null ? null : java.util.List.copyOf(items);
    }
}
