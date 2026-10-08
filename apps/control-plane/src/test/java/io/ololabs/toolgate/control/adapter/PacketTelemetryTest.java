// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
package io.ololabs.toolgate.control.adapter;
import org.junit.jupiter.api.Test;
import com.fasterxml.jackson.databind.ObjectMapper;
import static org.junit.jupiter.api.Assertions.*;
class PacketTelemetryTest {
    @Test void hidesSecretsAndContentButKeepsPollAndJobMetadata() throws Exception {
        var value = new ObjectMapper().readTree("{\"sequence\":2,\"deviceCode\":\"private-code\",\"identity\":{\"certificatePem\":\"private-cert\"},\"task\":{\"requestId\":\"mcp-123\",\"leaseId\":\"private-lease\",\"request\":{\"toolId\":\"hotfolder.write_text\",\"arguments\":{\"text\":\"private-text\"}}},\"configuration\":{\"digest\":\"abc123\",\"permissions\":[{\"resource\":\"private-resource\"}]}}");
        var safe = PacketTelemetry.summary(value, 0);
        assertFalse(safe.toString().contains("private-"));
        assertEquals("hotfolder.write_text", safe.path("task").path("request").path("toolId").asText());
        assertEquals(1, safe.path("configuration").path("permissions").path("count").asInt());
    }
}
