// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
// GENERATED FILE — DO NOT EDIT DIRECTLY; tools/contracts/generate.py
package io.ololabs.toolgate.contracts;

/** Visible secret name and group boundary; no plaintext or verifier bytes.
 *
 * @param name canonical name value
 * @param toolGroupId canonical toolGroupId value
 * @param deviceGroupId canonical deviceGroupId value
 */
@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
public record EnterpriseVaultReference(
    @com.fasterxml.jackson.annotation.JsonProperty(value = "name", required = true) String name,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "toolGroupId", required = true) String toolGroupId,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "deviceGroupId", required = true) String deviceGroupId
) {
    /** Reject absent required references and copy collections to retain value semantics.
     *
     * @param name canonical name value
     * @param toolGroupId canonical toolGroupId value
     * @param deviceGroupId canonical deviceGroupId value
     */
    public EnterpriseVaultReference {
        java.util.Objects.requireNonNull(name, "name");
        java.util.Objects.requireNonNull(toolGroupId, "toolGroupId");
        java.util.Objects.requireNonNull(deviceGroupId, "deviceGroupId");
    }
}
