// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
// GENERATED FILE — DO NOT EDIT DIRECTLY; tools/contracts/generate.py
package io.ololabs.toolgate.contracts;

/** Canonical bounded authoring protocol; declarations never grant execution privileges.
 *
 * @param code canonical code value
 * @param sha256 canonical sha256 value
 */
@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
public record LocalToolSource(
    @com.fasterxml.jackson.annotation.JsonProperty(value = "code", required = true) String code,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "sha256", required = true) String sha256
) {
    /** Reject absent required references and copy collections to retain value semantics.
     *
     * @param code canonical code value
     * @param sha256 canonical sha256 value
     */
    public LocalToolSource {
        java.util.Objects.requireNonNull(code, "code");
        java.util.Objects.requireNonNull(sha256, "sha256");
    }
}
