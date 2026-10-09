// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
package io.ololabs.toolgate.marketplace.api;
import io.ololabs.toolgate.contracts.ContractSet;
import io.ololabs.toolgate.contracts.EnterprisePermitHeader;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
/** Proves identical imports in workspace and artifact dependency modes. */
final class ContractDependencyTest {
    @Test void consumesSharedContracts() {
        assertEquals(ContractSet.current(), MarketplaceApiModule.contracts());
        var decision = new EnterprisePermitHeader("RS256", "toolgate-effect-permit+jws", "contract-test");
        assertEquals("toolgate-effect-permit+jws", decision.typ());
        if (Boolean.getBoolean("toolgate.published")) {
            assertTrue(ContractSet.class.getProtectionDomain().getCodeSource().getLocation().toString().endsWith(".jar"));
        }
    }
}
