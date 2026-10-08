// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
// GENERATED FILE — DO NOT EDIT DIRECTLY; tools/contracts/generate.py
package io.ololabs.toolgate.contracts;

/** Same-tenant administrator inspection of one completed response; excludes arguments and private lease credentials.
 *
 * @param record canonical record value
 * @param output canonical output value
 */
@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
public record RemoteToolInspection(
    @com.fasterxml.jackson.annotation.JsonProperty(value = "record", required = true) RemoteToolRecord record,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "output", required = false) java.util.Map<String, com.fasterxml.jackson.databind.JsonNode> output
) {
    /** Reject absent required references and copy collections to retain value semantics.
     *
     * @param record canonical record value
     * @param output canonical output value
     */
    public RemoteToolInspection {
        java.util.Objects.requireNonNull(record, "record");
        output = output == null ? null : java.util.Map.copyOf(output);
    }
}
