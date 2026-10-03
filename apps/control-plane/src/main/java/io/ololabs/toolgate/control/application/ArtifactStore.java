// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
package io.ololabs.toolgate.control.application;

/** Immutable descriptor mirror. Implementations bound origin, bytes, time and concurrency. */
public interface ArtifactStore {
    byte[] download(String digest,long expectedBytes);
}
