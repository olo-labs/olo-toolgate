// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
// GENERATED FILE — DO NOT EDIT DIRECTLY; tools/contracts/generate.py
package io.ololabs.toolgate.contracts;

/** Verified identity/delegation chain entry; not caller-asserted authority.
 *
 * @param agentId canonical agentId value
 * @param workloadBindingId canonical workloadBindingId value
 */
@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
public record EnterpriseActorHop(
    @com.fasterxml.jackson.annotation.JsonProperty(value = "agentId", required = true) String agentId,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "workloadBindingId", required = true) String workloadBindingId
) {
    /** Reject absent required references and copy collections to retain value semantics.
     *
     * @param agentId canonical agentId value
     * @param workloadBindingId canonical workloadBindingId value
     */
    public EnterpriseActorHop {
        java.util.Objects.requireNonNull(agentId, "agentId");
        java.util.Objects.requireNonNull(workloadBindingId, "workloadBindingId");
    }
}
