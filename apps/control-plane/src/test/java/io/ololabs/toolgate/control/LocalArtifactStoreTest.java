// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
package io.ololabs.toolgate.control;

import io.ololabs.toolgate.control.adapter.LocalArtifactStore;
import io.ololabs.toolgate.control.application.Failure;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;

class LocalArtifactStoreTest {
    @TempDir Path root;
    @Test void verifiesContentAndRejectsTraversalSizeAndCorruption() throws Exception {
        var data = "{}".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        var digest = java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(data));
        var file = root.resolve(digest + ".json");
        Files.write(file, data);
        var store = new LocalArtifactStore(root);
        assertArrayEquals(data, store.download(digest, 2));
        assertThrows(Failure.class, () -> store.download("../keys", 2));
        assertThrows(Failure.class, () -> store.download(digest, 1));
        Files.writeString(file, "[]");
        assertThrows(Failure.class, () -> store.download(digest, 2));
        Files.delete(file);
        assertThrows(Failure.class, () -> store.download(digest, 2));
    }
}
