// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
// GENERATED FILE — DO NOT EDIT DIRECTLY; tools/contracts/generate.py
package io.ololabs.toolgate.contracts;

/** Pending approvals carry no queued task or effect capability. Dispatched operations include the durable relay status.
 *
 * @param invocation canonical invocation value
 * @param dispatch canonical dispatch value
 */
@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
public record EnterpriseHumanOutcome(
    @com.fasterxml.jackson.annotation.JsonProperty(value = "invocation", required = true) EnterpriseInvocation invocation,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "dispatch", required = false) RemoteToolResponse dispatch
) {
    /** Reject absent required references and copy collections to retain value semantics.
     *
     * @param invocation canonical invocation value
     * @param dispatch canonical dispatch value
     */
    public EnterpriseHumanOutcome {
        java.util.Objects.requireNonNull(invocation, "invocation");
    }
}
