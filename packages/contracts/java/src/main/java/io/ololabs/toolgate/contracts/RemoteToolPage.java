// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
// GENERATED FILE — DO NOT EDIT DIRECTLY; tools/contracts/generate.py
package io.ololabs.toolgate.contracts;

/** Bounded recent local-tool request progress.
 *
 * @param items canonical items value
 */
@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
public record RemoteToolPage(
    @com.fasterxml.jackson.annotation.JsonProperty(value = "items", required = true) java.util.List<RemoteToolRecord> items
) {
    /** Reject absent required references and copy collections to retain value semantics.
     *
     * @param items canonical items value
     */
    public RemoteToolPage {
        java.util.Objects.requireNonNull(items, "items");
        items = items == null ? null : java.util.List.copyOf(items);
    }
}
