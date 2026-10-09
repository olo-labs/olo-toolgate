// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
// GENERATED FILE — DO NOT EDIT DIRECTLY; tools/contracts/generate.py
package io.ololabs.toolgate.contracts;

/** Runtime-only secret value; never returned by management, export, result or diagnostic routes.
 *
 * @param value canonical value value
 */
@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
public record EnterpriseSecretDelivery(
    @com.fasterxml.jackson.annotation.JsonProperty(value = "value", required = true) String value
) {
    /** Reject absent required references and copy collections to retain value semantics.
     *
     * @param value canonical value value
     */
    public EnterpriseSecretDelivery {
        java.util.Objects.requireNonNull(value, "value");
    }
}
