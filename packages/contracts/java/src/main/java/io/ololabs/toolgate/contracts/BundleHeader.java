// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
// GENERATED FILE — DO NOT EDIT DIRECTLY; tools/contracts/generate.py
package io.ololabs.toolgate.contracts;

/** Strict JWS protected header; no remote or embedded keys and no algorithm negotiation.
 *
 * @param alg canonical alg value
 * @param typ canonical typ value
 * @param kid canonical kid value
 */
@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
public record BundleHeader(
    @com.fasterxml.jackson.annotation.JsonProperty(value = "alg", required = true) String alg,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "typ", required = true) String typ,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "kid", required = true) String kid
) {
    /** Reject absent required references and copy collections to retain value semantics.
     *
     * @param alg canonical alg value
     * @param typ canonical typ value
     * @param kid canonical kid value
     */
    public BundleHeader {
        java.util.Objects.requireNonNull(alg, "alg");
        java.util.Objects.requireNonNull(typ, "typ");
        java.util.Objects.requireNonNull(kid, "kid");
    }
}
