// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
// GENERATED FILE — DO NOT EDIT DIRECTLY; tools/contracts/generate.py
package io.ololabs.toolgate.contracts;

/** Endpoint identity foundation wire model.
 *
 * @param payload canonical payload value
 * @param signature canonical signature value
 */
@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
public record SignedClientDiscovery(
    @com.fasterxml.jackson.annotation.JsonProperty(value = "payload", required = true) String payload,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "signature", required = true) String signature
) {
    /** Reject absent required references and copy collections to retain value semantics.
     *
     * @param payload canonical payload value
     * @param signature canonical signature value
     */
    public SignedClientDiscovery {
        java.util.Objects.requireNonNull(payload, "payload");
        java.util.Objects.requireNonNull(signature, "signature");
    }
}
