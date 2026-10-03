// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
// GENERATED FILE — DO NOT EDIT DIRECTLY; tools/contracts/generate.py
package io.ololabs.toolgate.contracts;

/** Explicit RSA public trust key; no private material or implicit domain sharing.
 *
 * @param kid canonical kid value
 * @param n canonical n value
 * @param e canonical e value
 */
@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
public record FleetTrustKey(
    @com.fasterxml.jackson.annotation.JsonProperty(value = "kid", required = true) String kid,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "n", required = true) String n,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "e", required = true) String e
) {
    /** Reject absent required references and copy collections to retain value semantics.
     *
     * @param kid canonical kid value
     * @param n canonical n value
     * @param e canonical e value
     */
    public FleetTrustKey {
        java.util.Objects.requireNonNull(kid, "kid");
        java.util.Objects.requireNonNull(n, "n");
        java.util.Objects.requireNonNull(e, "e");
    }
}
