// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
// GENERATED FILE — DO NOT EDIT DIRECTLY; tools/contracts/generate.py
package io.ololabs.toolgate.contracts;

/** Version 1 deterministic exact-match rules, with unconditional default deny.
 *
 * @param formatVersion canonical formatVersion value
 * @param rules canonical rules value
 */
@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
public record CompiledPolicy(
    @com.fasterxml.jackson.annotation.JsonProperty(value = "formatVersion", required = true) Long formatVersion,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "rules", required = true) java.util.List<BundleRule> rules
) {
    /** Reject absent required references and copy collections to retain value semantics.
     *
     * @param formatVersion canonical formatVersion value
     * @param rules canonical rules value
     */
    public CompiledPolicy {
        java.util.Objects.requireNonNull(formatVersion, "formatVersion");
        java.util.Objects.requireNonNull(rules, "rules");
        rules = rules == null ? null : java.util.List.copyOf(rules);
    }
}
