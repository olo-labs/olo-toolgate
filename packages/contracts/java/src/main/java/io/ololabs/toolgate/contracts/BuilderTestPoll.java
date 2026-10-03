// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
// GENERATED FILE — DO NOT EDIT DIRECTLY; tools/contracts/generate.py
package io.ololabs.toolgate.contracts;

/** Canonical bounded authoring protocol; declarations never grant execution privileges.
 *
 * @param task canonical task value
 */
@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
public record BuilderTestPoll(
    @com.fasterxml.jackson.annotation.JsonProperty(value = "task", required = false) FleetSignedDocument task
) {
    /** Reject absent required references and copy collections to retain value semantics.
     *
     * @param task canonical task value
     */
    public BuilderTestPoll {
    }
}
