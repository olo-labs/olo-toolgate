// Copyright 2026 OLO Labs; SPDX-License-Identifier: Apache-2.0
package io.ololabs.bots.sandbox;

import java.nio.file.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class TargetBindingTest {
    @TempDir Path directory;
    @Test void every_operation_requires_an_installed_exact_binding()throws Exception {
        var root=Store.JSON.createObjectNode();root.put("schemaVersion",1);
        root.put("origin","https://business-sandbox:8443");var routes=root.putObject("routes");
        Path file=directory.resolve("bindings.json");
        for(String name:java.util.List.of("restart","provision","inspect")) {
            var route=routes.putObject(name);route.put("profileId","installed-profile");
            route.put("profileDigest","sha256:"+"a".repeat(64));route.put("routeId",name);
        }
        Files.write(file,Store.JSON.writeValueAsBytes(root));
        assertEquals(java.util.Set.of("restart","provision","inspect"),TargetServer.profiles(file).keySet());
        routes.remove("inspect");Files.write(file,Store.JSON.writeValueAsBytes(root));
        assertThrows(IllegalArgumentException.class,()->TargetServer.profiles(file));
        var route=routes.putObject("inspect");route.put("profileId","installed-profile");
        route.put("profileDigest","sha256:"+"a".repeat(64));route.put("routeId","restart");
        Files.write(file,Store.JSON.writeValueAsBytes(root));
        assertThrows(IllegalArgumentException.class,()->TargetServer.profiles(file));
    }
}
