// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
// GENERATED FILE — DO NOT EDIT DIRECTLY; tools/contracts/generate.py
package io.ololabs.toolgate.contracts;

/** Atomic complete mandatory membership replacement guarded by directory revision.
 *
 * @param entityType canonical entityType value
 * @param entityId canonical entityId value
 * @param groupIds canonical groupIds value
 * @param revision canonical revision value
 */
@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
public record GroupMembership(
    @com.fasterxml.jackson.annotation.JsonProperty(value = "entityType", required = true) EnterpriseMemberType entityType,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "entityId", required = true) String entityId,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "groupIds", required = true) java.util.List<String> groupIds,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "revision", required = true) Long revision
) {
    /** Reject absent required references and copy collections to retain value semantics.
     *
     * @param entityType canonical entityType value
     * @param entityId canonical entityId value
     * @param groupIds canonical groupIds value
     * @param revision canonical revision value
     */
    public GroupMembership {
        java.util.Objects.requireNonNull(entityType, "entityType");
        java.util.Objects.requireNonNull(entityId, "entityId");
        java.util.Objects.requireNonNull(groupIds, "groupIds");
        groupIds = groupIds == null ? null : java.util.List.copyOf(groupIds);
        java.util.Objects.requireNonNull(revision, "revision");
    }
}
