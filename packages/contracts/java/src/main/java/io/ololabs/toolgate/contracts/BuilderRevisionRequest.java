// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
// GENERATED FILE — DO NOT EDIT DIRECTLY; tools/contracts/generate.py
package io.ololabs.toolgate.contracts;

/** Canonical bounded authoring protocol; declarations never grant execution privileges.
 *
 * @param expectedRevision canonical expectedRevision value
 */
@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
public record BuilderRevisionRequest(
    @com.fasterxml.jackson.annotation.JsonProperty(value = "expectedRevision", required = true) Long expectedRevision
) {
    /** Reject absent required references and copy collections to retain value semantics.
     *
     * @param expectedRevision canonical expectedRevision value
     */
    public BuilderRevisionRequest {
        java.util.Objects.requireNonNull(expectedRevision, "expectedRevision");
    }
}
