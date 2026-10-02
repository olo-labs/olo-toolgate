// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
package io.ololabs.toolgate.marketplace.api;

import io.ololabs.toolgate.contracts.ContractSet;

/**
 * Public Marketplace API Gradle module scaffold.
 *
 * <p>This is intentionally a buildable module marker. Runtime framework
 * initialization is delivered in the component implementation module.</p>
 */
public final class MarketplaceApiModule {
    private MarketplaceApiModule() {
    }

    public static ContractSet contracts() {
        return ContractSet.current();
    }
}
