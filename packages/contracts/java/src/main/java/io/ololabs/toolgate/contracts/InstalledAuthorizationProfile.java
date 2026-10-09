// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
// GENERATED FILE — DO NOT EDIT DIRECTLY; tools/contracts/generate.py
package io.ololabs.toolgate.contracts;

/** Reviewed deployment metadata, never a grant. Local executors independently verify the installed package and resource extraction definition.
 *
 * @param tool canonical tool value
 * @param extractor canonical extractor value
 */
@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
public record InstalledAuthorizationProfile(
    @com.fasterxml.jackson.annotation.JsonProperty(value = "tool", required = true) ControlTool tool,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "extractor", required = true) ControlResourceExtractor extractor
) {
    /** Reject absent required references and copy collections to retain value semantics.
     *
     * @param tool canonical tool value
     * @param extractor canonical extractor value
     */
    public InstalledAuthorizationProfile {
        java.util.Objects.requireNonNull(tool, "tool");
        java.util.Objects.requireNonNull(extractor, "extractor");
    }
}
