// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
package io.ololabs.toolgate.control;
import io.ololabs.toolgate.contracts.ContractSet;
import io.ololabs.toolgate.contracts.Decision;
import io.ololabs.toolgate.contracts.DecisionReason;
import io.ololabs.toolgate.contracts.PolicyDecision;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
/** Proves identical imports in workspace and artifact dependency modes. */
final class ContractDependencyTest {
    @Test void consumesSharedContracts() {
        assertEquals(ContractSet.current(), ControlPlaneModule.contracts());
        var decision = new PolicyDecision(Decision.BLOCK, DecisionReason.NO_MATCH, ContractSet.VERSION, "request-1");
        assertEquals(Decision.BLOCK, decision.decision());
        if (Boolean.getBoolean("toolgate.published")) {
            assertTrue(ContractSet.class.getProtectionDomain().getCodeSource().getLocation().toString().endsWith(".jar"));
        }
    }
}
