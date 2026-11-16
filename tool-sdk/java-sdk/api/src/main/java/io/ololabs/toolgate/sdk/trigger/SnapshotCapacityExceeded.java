// Copyright 2026 OLO Labs; SPDX-License-Identifier: Apache-2.0
package io.ololabs.toolgate.sdk.trigger;

/** An exact snapshot cannot admit another key. Never truncate or checkpoint a partial snapshot. */
public final class SnapshotCapacityExceeded extends IllegalStateException {
    private static final long serialVersionUID = 1L;
    public SnapshotCapacityExceeded(int capacity) {
        super("SnapshotCapacityExceeded: declared capacity " + capacity + " exhausted");
    }
}
