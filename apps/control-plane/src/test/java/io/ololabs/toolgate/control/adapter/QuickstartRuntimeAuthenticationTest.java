// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
package io.ololabs.toolgate.control.adapter;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class QuickstartRuntimeAuthenticationTest {
    @Test void delegationCannotExpandToAdminDeviceOrProductionRoutes() {
        for(var path:new String[]{"/mcp","/access/invocations"}) {
            assertTrue(QuickstartRuntimeAuthentication.delegated(true,"POST",path));
            assertFalse(QuickstartRuntimeAuthentication.delegated(false,"POST",path));
            assertFalse(QuickstartRuntimeAuthentication.delegated(true,"GET",path));
        }
        for(var path:new String[]{"/v2/authorize","/v1/permits/consume","/api/control/v1/users","/api/control/v1/mcp/catalog","/api/control/v1/mcp/authorize","/api/control/v1/endpoint/check-in","/mcp/","/mcp/../api/control/v1/users"}) assertFalse(QuickstartRuntimeAuthentication.delegated(true,"POST",path));
    }
}
