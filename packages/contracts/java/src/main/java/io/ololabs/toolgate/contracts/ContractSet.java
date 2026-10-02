// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
// GENERATED FILE — DO NOT EDIT DIRECTLY; tools/contracts/generate.py
package io.ololabs.toolgate.contracts;

/** Identifies the shared contract set, independently of product versions.
 *
 * @param name canonical name value
 * @param version canonical version value
 */
@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
public record ContractSet(
    @com.fasterxml.jackson.annotation.JsonProperty(value = "name", required = true) String name,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "version", required = true) String version
) {
    /** Reject absent required references and copy collections to retain value semantics.
     *
     * @param name canonical name value
     * @param version canonical version value
     */
    public ContractSet {
        java.util.Objects.requireNonNull(name, "name");
        java.util.Objects.requireNonNull(version, "version");
    }
    /** Stable package identity. */
    public static final String NAME = "olo-toolgate-contracts";
    /** Canonical contract-set version. */
    public static final String VERSION = "0.3.0-dev";
    /** Current contract-set marker.
     * @return the canonical identity and version
     */
    public static ContractSet current() { return new ContractSet(NAME, VERSION); }
}
