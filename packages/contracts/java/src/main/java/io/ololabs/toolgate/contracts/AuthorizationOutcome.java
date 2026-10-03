// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
// GENERATED FILE — DO NOT EDIT DIRECTLY; tools/contracts/generate.py
package io.ololabs.toolgate.contracts;

/** V2 ASK outcome; pending ASK grants no execution; ALLOW for approved ASK requires a signed permit.
 *
 * @param decision canonical decision value
 * @param approvalId canonical approvalId value
 * @param permit canonical permit value
 */
@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
public record AuthorizationOutcome(
    @com.fasterxml.jackson.annotation.JsonProperty(value = "decision", required = true) PolicyDecision decision,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "approvalId", required = false) String approvalId,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "permit", required = false) SignedExecutionPermit permit
) {
    /** Reject absent required references and copy collections to retain value semantics.
     *
     * @param decision canonical decision value
     * @param approvalId canonical approvalId value
     * @param permit canonical permit value
     */
    public AuthorizationOutcome {
        java.util.Objects.requireNonNull(decision, "decision");
    }
}
