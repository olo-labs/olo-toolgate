// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
// GENERATED FILE — DO NOT EDIT DIRECTLY; tools/contracts/generate.py
package io.ololabs.toolgate.contracts;

/** Signed enterprise effect permit; claims are bound and consumption is durably atomic.
 *
 * @param jws canonical jws value
 */
@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
public record EnterpriseSignedPermit(
    @com.fasterxml.jackson.annotation.JsonProperty(value = "jws", required = true) String jws
) {
    /** Reject absent required references and copy collections to retain value semantics.
     *
     * @param jws canonical jws value
     */
    public EnterpriseSignedPermit {
        java.util.Objects.requireNonNull(jws, "jws");
    }
}
