// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
// GENERATED FILE — DO NOT EDIT DIRECTLY; tools/contracts/generate.py
package io.ololabs.toolgate.contracts;

/** All unexpired pending requests in the authenticated tenant; enrollment quota is 32.
 *
 * @param items canonical items value
 */
@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
public record EndpointEnrollmentPage(
    @com.fasterxml.jackson.annotation.JsonProperty(value = "items", required = true) java.util.List<EndpointEnrollmentReview> items
) {
    /** Reject absent required references and copy collections to retain value semantics.
     *
     * @param items canonical items value
     */
    public EndpointEnrollmentPage {
        java.util.Objects.requireNonNull(items, "items");
        items = items == null ? null : java.util.List.copyOf(items);
    }
}
