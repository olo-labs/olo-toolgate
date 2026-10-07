// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
// GENERATED FILE — DO NOT EDIT DIRECTLY; tools/contracts/generate.py
package io.ololabs.toolgate.contracts;

/** Gateway-private queued request/result response.
 *
 * @param record canonical record value
 * @param result canonical result value
 */
@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
public record RemoteToolResponse(
    @com.fasterxml.jackson.annotation.JsonProperty(value = "record", required = true) RemoteToolRecord record,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "result", required = false) RemoteToolResult result
) {
    /** Reject absent required references and copy collections to retain value semantics.
     *
     * @param record canonical record value
     * @param result canonical result value
     */
    public RemoteToolResponse {
        java.util.Objects.requireNonNull(record, "record");
    }
}
