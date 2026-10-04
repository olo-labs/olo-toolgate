// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
package io.ololabs.toolgate.control.adapter;

import io.ololabs.toolgate.control.application.ArtifactStore;
import io.ololabs.toolgate.control.application.Failure;
import java.nio.file.*;

/** Quickstart-only content-addressed custody; fleet authorization still precedes reads. */
public final class LocalArtifactStore implements ArtifactStore {
    private final Path root;
    public LocalArtifactStore(Path root) {
        if (!root.isAbsolute() || Files.isSymbolicLink(root)) throw new IllegalArgumentException();
        this.root = root;
    }
    public byte[] download(String digest, long expectedBytes) {
        if (!digest.matches("[a-f0-9]{64}") || expectedBytes < 1 || expectedBytes > 32768) throw Failure.validation();
        try (var input = Files.newInputStream(root.resolve(digest + ".json"), LinkOption.NOFOLLOW_LINKS)) {
            var bytes = input.readNBytes((int) expectedBytes + 1);
            if (bytes.length != expectedBytes || !java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(bytes)).equals(digest)) throw Failure.unavailable();
            return bytes;
        } catch (java.io.IOException | java.security.GeneralSecurityException failure) { throw Failure.unavailable(); }
    }
}
