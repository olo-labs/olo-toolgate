// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
// GENERATED FILE — DO NOT EDIT DIRECTLY; tools/contracts/generate.py
package io.ololabs.toolgate.contracts;

/** Group/typed-role sourced complete runtime capability; never an individual ACL.
 *
 * @param id canonical id value
 * @param name canonical name value
 * @param enabled canonical enabled value
 * @param revision canonical revision value
 * @param sourceType canonical sourceType value
 * @param sourceId canonical sourceId value
 * @param purpose canonical purpose value
 * @param scope canonical scope value
 */
@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
public record ControlAccessGrant(
    @com.fasterxml.jackson.annotation.JsonProperty(value = "id", required = true) String id,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "name", required = true) String name,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "enabled", required = true) Boolean enabled,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "revision", required = true) Long revision,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "sourceType", required = true) EnterpriseSourceType sourceType,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "sourceId", required = true) String sourceId,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "purpose", required = true) EnterpriseGrantPurpose purpose,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "scope", required = true) EnterpriseScope scope
) {
    /** Reject absent required references and copy collections to retain value semantics.
     *
     * @param id canonical id value
     * @param name canonical name value
     * @param enabled canonical enabled value
     * @param revision canonical revision value
     * @param sourceType canonical sourceType value
     * @param sourceId canonical sourceId value
     * @param purpose canonical purpose value
     * @param scope canonical scope value
     */
    public ControlAccessGrant {
        java.util.Objects.requireNonNull(id, "id");
        java.util.Objects.requireNonNull(name, "name");
        java.util.Objects.requireNonNull(enabled, "enabled");
        java.util.Objects.requireNonNull(revision, "revision");
        java.util.Objects.requireNonNull(sourceType, "sourceType");
        java.util.Objects.requireNonNull(sourceId, "sourceId");
        java.util.Objects.requireNonNull(purpose, "purpose");
        java.util.Objects.requireNonNull(scope, "scope");
    }
}
