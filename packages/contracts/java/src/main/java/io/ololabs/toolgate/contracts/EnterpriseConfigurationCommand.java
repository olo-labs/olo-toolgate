// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
// GENERATED FILE — DO NOT EDIT DIRECTLY; tools/contracts/generate.py
package io.ololabs.toolgate.contracts;

/** Reviewed configuration workflow bound to the exact group graph revision.
 *
 * @param operation canonical operation value
 * @param kind canonical kind value
 * @param entityId canonical entityId value
 * @param document canonical document value
 * @param expectedRevision canonical expectedRevision value
 */
@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
public record EnterpriseConfigurationCommand(
    @com.fasterxml.jackson.annotation.JsonProperty(value = "operation", required = true) EnterpriseConfigurationOperation operation,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "kind", required = true) ControlEntityKind kind,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "entityId", required = true) String entityId,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "document", required = true) String document,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "expectedRevision", required = true) Long expectedRevision
) {
    /** Reject absent required references and copy collections to retain value semantics.
     *
     * @param operation canonical operation value
     * @param kind canonical kind value
     * @param entityId canonical entityId value
     * @param document canonical document value
     * @param expectedRevision canonical expectedRevision value
     */
    public EnterpriseConfigurationCommand {
        java.util.Objects.requireNonNull(operation, "operation");
        java.util.Objects.requireNonNull(kind, "kind");
        java.util.Objects.requireNonNull(entityId, "entityId");
        java.util.Objects.requireNonNull(document, "document");
        java.util.Objects.requireNonNull(expectedRevision, "expectedRevision");
    }
}
