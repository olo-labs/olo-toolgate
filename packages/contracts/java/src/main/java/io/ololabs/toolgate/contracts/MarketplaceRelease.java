// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
// GENERATED FILE — DO NOT EDIT DIRECTLY; tools/contracts/generate.py
package io.ololabs.toolgate.contracts;

/** Marketplace trust only; never organization or runtime authorization.
 *
 * @param packageId canonical packageId value
 * @param version canonical version value
 * @param manifestDigest canonical manifestDigest value
 * @param marketplaceKeyId canonical marketplaceKeyId value
 * @param marketplaceSignature canonical marketplaceSignature value
 */
@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
public record MarketplaceRelease(
    @com.fasterxml.jackson.annotation.JsonProperty(value = "packageId", required = true) String packageId,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "version", required = true) String version,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "manifestDigest", required = true) String manifestDigest,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "marketplaceKeyId", required = true) String marketplaceKeyId,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "marketplaceSignature", required = true) String marketplaceSignature
) {
    /** Reject absent required references and copy collections to retain value semantics.
     *
     * @param packageId canonical packageId value
     * @param version canonical version value
     * @param manifestDigest canonical manifestDigest value
     * @param marketplaceKeyId canonical marketplaceKeyId value
     * @param marketplaceSignature canonical marketplaceSignature value
     */
    public MarketplaceRelease {
        java.util.Objects.requireNonNull(packageId, "packageId");
        java.util.Objects.requireNonNull(version, "version");
        java.util.Objects.requireNonNull(manifestDigest, "manifestDigest");
        java.util.Objects.requireNonNull(marketplaceKeyId, "marketplaceKeyId");
        java.util.Objects.requireNonNull(marketplaceSignature, "marketplaceSignature");
    }
}
