// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
// GENERATED FILE — DO NOT EDIT DIRECTLY; tools/contracts/generate.py
package io.ololabs.toolgate.contracts;

/** Protected operator review for group-only bootstrap or bounded recovery.
 *
 * @param keyId canonical keyId value
 * @param signature canonical signature value
 */
@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
public record EnterpriseRecoveryProof(
    @com.fasterxml.jackson.annotation.JsonProperty(value = "keyId", required = true) String keyId,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "signature", required = true) String signature
) {
    /** Reject absent required references and copy collections to retain value semantics.
     *
     * @param keyId canonical keyId value
     * @param signature canonical signature value
     */
    public EnterpriseRecoveryProof {
        java.util.Objects.requireNonNull(keyId, "keyId");
        java.util.Objects.requireNonNull(signature, "signature");
    }
}
