// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
// GENERATED FILE — DO NOT EDIT DIRECTLY; tools/contracts/generate.py
package io.ololabs.toolgate.contracts;

/** Acknowledgement after authorized encrypted custody and redacted audit commit.
 *
 * @param stored canonical stored value
 */
@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
public record EnterpriseVaultStored(
    @com.fasterxml.jackson.annotation.JsonProperty(value = "stored", required = true) Boolean stored
) {
    /** Reject absent required references and copy collections to retain value semantics.
     *
     * @param stored canonical stored value
     */
    public EnterpriseVaultStored {
        java.util.Objects.requireNonNull(stored, "stored");
    }
}
