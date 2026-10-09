// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
// GENERATED FILE — DO NOT EDIT DIRECTLY; tools/contracts/generate.py
package io.ololabs.toolgate.contracts;

/** Read-only inherited access provenance at the current authority revision. No direct individual access mapping is created. Bindings and candidate grants do not constitute execution authorization.
 *
 * @param entityId canonical entityId value
 * @param kind canonical kind value
 * @param directoryRevision canonical directoryRevision value
 * @param authorizationEpoch canonical authorizationEpoch value
 * @param memberships canonical memberships value
 * @param bindings canonical bindings value
 * @param managementRoles canonical managementRoles value
 */
@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
public record EnterpriseEffectiveAccess(
    @com.fasterxml.jackson.annotation.JsonProperty(value = "entityId", required = true) String entityId,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "kind", required = true) ControlEntityKind kind,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "directoryRevision", required = true) Long directoryRevision,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "authorizationEpoch", required = true) Long authorizationEpoch,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "memberships", required = true) java.util.List<EnterpriseEffectiveMembership> memberships,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "bindings", required = true) java.util.List<ControlExecutionBinding> bindings,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "managementRoles", required = true) java.util.List<ControlRole> managementRoles
) {
    /** Reject absent required references and copy collections to retain value semantics.
     *
     * @param entityId canonical entityId value
     * @param kind canonical kind value
     * @param directoryRevision canonical directoryRevision value
     * @param authorizationEpoch canonical authorizationEpoch value
     * @param memberships canonical memberships value
     * @param bindings canonical bindings value
     * @param managementRoles canonical managementRoles value
     */
    public EnterpriseEffectiveAccess {
        java.util.Objects.requireNonNull(entityId, "entityId");
        java.util.Objects.requireNonNull(kind, "kind");
        java.util.Objects.requireNonNull(directoryRevision, "directoryRevision");
        java.util.Objects.requireNonNull(authorizationEpoch, "authorizationEpoch");
        java.util.Objects.requireNonNull(memberships, "memberships");
        memberships = memberships == null ? null : java.util.List.copyOf(memberships);
        java.util.Objects.requireNonNull(bindings, "bindings");
        bindings = bindings == null ? null : java.util.List.copyOf(bindings);
        java.util.Objects.requireNonNull(managementRoles, "managementRoles");
        managementRoles = managementRoles == null ? null : java.util.List.copyOf(managementRoles);
    }
}
