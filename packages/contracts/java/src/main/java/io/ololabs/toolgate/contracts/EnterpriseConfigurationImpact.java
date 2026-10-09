// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
// GENERATED FILE — DO NOT EDIT DIRECTLY; tools/contracts/generate.py
package io.ololabs.toolgate.contracts;

/** Reviewed configuration workflow bound to the exact group graph revision.
 *
 * @param kind canonical kind value
 * @param entityId canonical entityId value
 * @param operation canonical operation value
 * @param beforeDigest canonical beforeDigest value
 * @param afterDigest canonical afterDigest value
 */
@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
public record EnterpriseConfigurationImpact(
    @com.fasterxml.jackson.annotation.JsonProperty(value = "kind", required = true) ControlEntityKind kind,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "entityId", required = true) String entityId,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "operation", required = true) EnterpriseConfigurationImpactOperation operation,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "beforeDigest", required = true) String beforeDigest,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "afterDigest", required = true) String afterDigest
) {
    /** Reject absent required references and copy collections to retain value semantics.
     *
     * @param kind canonical kind value
     * @param entityId canonical entityId value
     * @param operation canonical operation value
     * @param beforeDigest canonical beforeDigest value
     * @param afterDigest canonical afterDigest value
     */
    public EnterpriseConfigurationImpact {
        java.util.Objects.requireNonNull(kind, "kind");
        java.util.Objects.requireNonNull(entityId, "entityId");
        java.util.Objects.requireNonNull(operation, "operation");
        java.util.Objects.requireNonNull(beforeDigest, "beforeDigest");
        java.util.Objects.requireNonNull(afterDigest, "afterDigest");
    }
}
