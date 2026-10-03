// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
// GENERATED FILE — DO NOT EDIT DIRECTLY; tools/contracts/generate.py
package io.ololabs.toolgate.contracts;

/** Canonical bounded authoring protocol; declarations never grant execution privileges.
 *
 * @param jobId canonical jobId value
 * @param leaseId canonical leaseId value
 * @param definitionDigest canonical definitionDigest value
 * @param success canonical success value
 * @param error canonical error value
 */
@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
public record BuilderTestResult(
    @com.fasterxml.jackson.annotation.JsonProperty(value = "jobId", required = true) String jobId,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "leaseId", required = true) String leaseId,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "definitionDigest", required = true) String definitionDigest,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "success", required = true) Boolean success,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "error", required = false) ErrorCode error
) {
    /** Reject absent required references and copy collections to retain value semantics.
     *
     * @param jobId canonical jobId value
     * @param leaseId canonical leaseId value
     * @param definitionDigest canonical definitionDigest value
     * @param success canonical success value
     * @param error canonical error value
     */
    public BuilderTestResult {
        java.util.Objects.requireNonNull(jobId, "jobId");
        java.util.Objects.requireNonNull(leaseId, "leaseId");
        java.util.Objects.requireNonNull(definitionDigest, "definitionDigest");
        java.util.Objects.requireNonNull(success, "success");
    }
}
