// Copyright 2026 OLO Labs; SPDX-License-Identifier: Apache-2.0
package io.ololabs.toolgate.sdk.trigger;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

/**
 * Exact reported version keys for a STATE_SNAPSHOT source (trigger plan 5.6).
 * This mutable value belongs to one single-flight reconcile. Keys are encoded, never hashed.
 * Construct a candidate from the last committed checkpoint, retain only after a complete listing,
 * add only durably emitted versions, then checkpoint. Failed/partial reconciles must discard the candidate.
 * Overflow is explicit; the executor/source must report degradation and enter durable rescan mode.
 */
public final class SnapshotState {
    public static final int MAX_CHECKPOINT_BYTES = 128 * 1024;
    private static final int HEADER_ALLOWANCE = 1024;
    private final int capacity;
    private final int maxKeyBytes;
    private final int maxCheckpointBytes;
    private final TreeSet<String> keys = new TreeSet<>();

    private SnapshotState(int capacity, int maxKeyBytes, int maxCheckpointBytes) {
        if (capacity < 1 || maxKeyBytes < 1 || maxCheckpointBytes < 1 || maxCheckpointBytes > MAX_CHECKPOINT_BYTES)
            throw new IllegalArgumentException("Invalid snapshot bounds");
        // Base64 rounds each key separately. Include JSON quotes/separators as well as the bounded header.
        long encodedKeyBytes = ((long) maxKeyBytes + 2) / 3 * 4;
        if ((encodedKeyBytes + 3) * capacity + HEADER_ALLOWANCE > maxCheckpointBytes)
            throw new IllegalArgumentException("Declared snapshot capacity exceeds maxCheckpointBytes");
        this.capacity = capacity;
        this.maxKeyBytes = maxKeyBytes;
        this.maxCheckpointBytes = maxCheckpointBytes;
    }

    public static SnapshotState empty(int capacity, int maxKeyBytes, int maxCheckpointBytes) {
        return new SnapshotState(capacity, maxKeyBytes, maxCheckpointBytes);
    }

    /** Restores only the current format and exactly the declared bounds; obsolete state is never converted. */
    public static SnapshotState from(Checkpoint checkpoint, int capacity, int maxKeyBytes, int maxCheckpointBytes) {
        Objects.requireNonNull(checkpoint, "checkpoint");
        SnapshotState state = empty(capacity, maxKeyBytes, maxCheckpointBytes);
        if (!checkpoint.values().keySet().equals(Set.of("format", "capacity", "maxKeyBytes", "maxCheckpointBytes", "keys"))
                || checkpoint.getLong("format") != 1 || checkpoint.getLong("capacity") != capacity
                || checkpoint.getLong("maxKeyBytes") != maxKeyBytes || checkpoint.getLong("maxCheckpointBytes") != maxCheckpointBytes)
            throw new IllegalArgumentException("Snapshot checkpoint format or declaration mismatch");
        List<Object> saved = checkpoint.getList("keys");
        if (saved.size() > capacity) throw new SnapshotCapacityExceeded(capacity);
        for (Object raw : saved) {
            if (!(raw instanceof String encoded)) throw new IllegalArgumentException("Invalid snapshot key encoding");
            if (encoded.length() > ((long) maxKeyBytes + 2) / 3 * 4)
                throw new IllegalArgumentException("Snapshot version key exceeds declared bounds");
            byte[] decoded;
            try { decoded = Base64.getDecoder().decode(encoded); }
            catch (IllegalArgumentException e) { throw new IllegalArgumentException("Invalid snapshot key encoding", e); }
            if (!state.encoded(decoded).equals(encoded)) throw new IllegalArgumentException("Noncanonical snapshot key encoding");
            if (!state.keys.add(encoded)) throw new IllegalArgumentException("Duplicate snapshot key");
        }
        state.checkpoint(); // Includes runtime serialized-size validation, not only declared count.
        return state;
    }

    public int size() { return keys.size(); }
    public int capacity() { return capacity; }
    public boolean contains(byte[] versionKey) { return keys.contains(encoded(versionKey)); }

    /** Returns false for an existing key, even at capacity. Overflow leaves this value unchanged. */
    public boolean add(byte[] versionKey) {
        String encoded = encoded(versionKey);
        if (keys.contains(encoded)) return false;
        if (keys.size() == capacity) throw new SnapshotCapacityExceeded(capacity);
        return keys.add(encoded);
    }

    /**
     * Removes keys confirmed absent by a complete authoritative listing. Callers must not invoke this on
     * a failed or partial listing. Validate the entire collection before mutating, including every key's bound.
     */
    public void retainOnly(Collection<byte[]> present) {
        Objects.requireNonNull(present, "present");
        Set<String> confirmed = new HashSet<>();
        for (byte[] key : present) confirmed.add(encoded(key));
        keys.retainAll(confirmed);
    }

    /** A deterministic current-format checkpoint containing no source secrets. */
    public Checkpoint checkpoint() {
        // All strings are canonical base64, so this is the exact encoded size of this JSON shape.
        String serialized = "{\"format\":1,\"capacity\":" + capacity + ",\"maxKeyBytes\":" + maxKeyBytes
                + ",\"maxCheckpointBytes\":" + maxCheckpointBytes + ",\"keys\":["
                + keys.stream().map(k -> "\"" + k + "\"").collect(java.util.stream.Collectors.joining(",")) + "]}";
        if (serialized.getBytes(StandardCharsets.UTF_8).length > maxCheckpointBytes)
            throw new SnapshotCapacityExceeded(capacity);
        return Checkpoint.of(Map.of("format", 1, "capacity", capacity, "maxKeyBytes", maxKeyBytes,
                "maxCheckpointBytes", maxCheckpointBytes, "keys", new ArrayList<>(keys)));
    }

    private String encoded(byte[] key) {
        Objects.requireNonNull(key, "versionKey");
        if (key.length == 0 || key.length > maxKeyBytes) throw new IllegalArgumentException("Snapshot version key exceeds declared bounds");
        return Base64.getEncoder().encodeToString(key);
    }
}
