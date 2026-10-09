// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
// GENERATED FILE — DO NOT EDIT DIRECTLY; tools/contracts/generate.py
package io.ololabs.toolgate.contracts;

/** Trusted UTC window; end must exceed start.
 *
 * @param dayOfWeek canonical dayOfWeek value
 * @param startMinute canonical startMinute value
 * @param endMinute canonical endMinute value
 */
@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
public record EnterpriseHours(
    @com.fasterxml.jackson.annotation.JsonProperty(value = "dayOfWeek", required = true) Long dayOfWeek,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "startMinute", required = true) Long startMinute,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "endMinute", required = true) Long endMinute
) {
    /** Reject absent required references and copy collections to retain value semantics.
     *
     * @param dayOfWeek canonical dayOfWeek value
     * @param startMinute canonical startMinute value
     * @param endMinute canonical endMinute value
     */
    public EnterpriseHours {
        java.util.Objects.requireNonNull(dayOfWeek, "dayOfWeek");
        java.util.Objects.requireNonNull(startMinute, "startMinute");
        java.util.Objects.requireNonNull(endMinute, "endMinute");
    }
}
