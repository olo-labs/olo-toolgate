// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
// GENERATED FILE — DO NOT EDIT DIRECTLY; tools/contracts/generate.py
package io.ololabs.toolgate.contracts;

/** Initial tenant installation has one pinned installation authority. Existing tenant recovery requires at least two distinct pinned independent signing authorities.
 *
 * @param authorization canonical authorization value
 * @param proofs canonical proofs value
 */
@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
public record EnterpriseReviewedRecovery(
    @com.fasterxml.jackson.annotation.JsonProperty(value = "authorization", required = true) EnterpriseRecoveryAuthorization authorization,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "proofs", required = true) java.util.List<EnterpriseRecoveryProof> proofs
) {
    /** Reject absent required references and copy collections to retain value semantics.
     *
     * @param authorization canonical authorization value
     * @param proofs canonical proofs value
     */
    public EnterpriseReviewedRecovery {
        java.util.Objects.requireNonNull(authorization, "authorization");
        java.util.Objects.requireNonNull(proofs, "proofs");
        proofs = proofs == null ? null : java.util.List.copyOf(proofs);
    }
}
