// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
// GENERATED FILE — DO NOT EDIT DIRECTLY; tools/contracts/generate.py
package io.ololabs.toolgate.contracts;

/** Certificate-bound single-use consumption of an exact effect capability.
 *
 * @param invocationId canonical invocationId value
 * @param permit canonical permit value
 * @param argumentsDigest canonical argumentsDigest value
 * @param resources canonical resources value
 * @param toolDigest canonical toolDigest value
 * @param packageDigest canonical packageDigest value
 */
@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
public record EnterprisePermitConsumption(
    @com.fasterxml.jackson.annotation.JsonProperty(value = "invocationId", required = true) String invocationId,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "permit", required = true) EnterpriseSignedPermit permit,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "argumentsDigest", required = true) String argumentsDigest,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "resources", required = true) java.util.List<ResourceDescriptor> resources,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "toolDigest", required = true) String toolDigest,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "packageDigest", required = true) String packageDigest
) {
    /** Reject absent required references and copy collections to retain value semantics.
     *
     * @param invocationId canonical invocationId value
     * @param permit canonical permit value
     * @param argumentsDigest canonical argumentsDigest value
     * @param resources canonical resources value
     * @param toolDigest canonical toolDigest value
     * @param packageDigest canonical packageDigest value
     */
    public EnterprisePermitConsumption {
        java.util.Objects.requireNonNull(invocationId, "invocationId");
        java.util.Objects.requireNonNull(permit, "permit");
        java.util.Objects.requireNonNull(argumentsDigest, "argumentsDigest");
        java.util.Objects.requireNonNull(resources, "resources");
        resources = resources == null ? null : java.util.List.copyOf(resources);
        java.util.Objects.requireNonNull(toolDigest, "toolDigest");
        java.util.Objects.requireNonNull(packageDigest, "packageDigest");
    }
}
