// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
// GENERATED FILE — DO NOT EDIT DIRECTLY; tools/contracts/generate.py
package io.ololabs.toolgate.contracts;

/** Canonical complete resource constraint; ANY is explicit privileged scope.
 *
 * @param kind canonical kind value
 * @param locator canonical locator value
 * @param match canonical match value
 */
@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
public record EnterpriseResourceRule(
    @com.fasterxml.jackson.annotation.JsonProperty(value = "kind", required = true) ResourceKind kind,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "locator", required = true) String locator,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "match", required = true) EnterpriseResourceMatch match
) {
    /** Reject absent required references and copy collections to retain value semantics.
     *
     * @param kind canonical kind value
     * @param locator canonical locator value
     * @param match canonical match value
     */
    public EnterpriseResourceRule {
        java.util.Objects.requireNonNull(kind, "kind");
        java.util.Objects.requireNonNull(locator, "locator");
        java.util.Objects.requireNonNull(match, "match");
    }
}
