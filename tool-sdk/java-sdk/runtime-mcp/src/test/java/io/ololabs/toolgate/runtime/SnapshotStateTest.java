// Copyright 2026 OLO Labs; SPDX-License-Identifier: Apache-2.0
package io.ololabs.toolgate.runtime;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.ololabs.toolgate.sdk.trigger.Checkpoint;
import io.ololabs.toolgate.sdk.trigger.SnapshotCapacityExceeded;
import io.ololabs.toolgate.sdk.trigger.SnapshotState;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

class SnapshotStateTest {
    private static SnapshotState state() { return SnapshotState.empty(1024, 32, 131072); }
    private static byte[] key(int value) { return ByteBuffer.allocate(32).putInt(value).array(); }

    @Test void fixedFileKeysFitWithoutHashesOrTruncation() throws Exception {
        var state = state();
        for (int i = 0; i < 1024; i++) assertTrue(state.add(key(i)));
        assertEquals(1024, state.size());
        assertFalse(state.add(key(1)), "existing key can be re-added at capacity");
        var checkpoint = state.checkpoint();
        assertTrue(new ObjectMapper().writeValueAsBytes(checkpoint.values()).length < 131072);
        var restored = SnapshotState.from(checkpoint, 1024, 32, 131072);
        for (int i = 0; i < 1024; i++) assertTrue(restored.contains(key(i)));
        assertEquals(checkpoint, restored.checkpoint());
    }

    @Test void overflowNeverEvictsAnExistingVersion() {
        var state = state();
        for (int i = 0; i < 1024; i++) state.add(key(i));
        var before = state.checkpoint();
        assertThrows(SnapshotCapacityExceeded.class, () -> state.add(key(1024)));
        assertEquals(before, state.checkpoint());
        assertFalse(state.contains(key(1024)));
    }

    @Test void inputArrayMutationCannotChangeStoredIdentity() {
        var state = state();
        var mutable = key(1);
        state.add(mutable);
        Arrays.fill(mutable, (byte) 99);
        assertTrue(state.contains(key(1)));
        assertFalse(state.contains(mutable));
    }

    @Test void pruningUsesContentEqualityAndValidatesTheWholeListingFirst() {
        var state = state();
        state.add(key(1));
        state.add(key(2));
        var before = state.checkpoint();
        assertThrows(IllegalArgumentException.class, () -> state.retainOnly(List.of(key(2), new byte[33])));
        assertEquals(before, state.checkpoint());
        state.retainOnly(List.of(key(2)));
        assertFalse(state.contains(key(1)));
        assertTrue(state.contains(key(2)));
    }

    @Test void serializationIsDeterministicForDifferentInsertionOrder() {
        var first = state();
        var second = state();
        first.add(key(2)); first.add(key(1));
        second.add(key(1)); second.add(key(2));
        assertEquals(first.checkpoint(), second.checkpoint());
    }

    @Test void declarationIncludesPerKeyBase64PaddingAndJsonFraming() {
        assertThrows(IllegalArgumentException.class, () -> SnapshotState.empty(1024, 32, 16384));
        assertThrows(IllegalArgumentException.class, () -> SnapshotState.empty(Integer.MAX_VALUE, Integer.MAX_VALUE, 131072));
        assertThrows(IllegalArgumentException.class, () -> SnapshotState.empty(1, 32, 131073));
        assertThrows(IllegalArgumentException.class, () -> SnapshotState.empty(0, 32, 131072));
        // 2000 one-byte keys need 4 base64 bytes + 3 JSON bytes each, not ceil(2000*4/3).
        assertThrows(IllegalArgumentException.class, () -> SnapshotState.empty(2000, 1, 4096));
    }

    @Test void restorationRejectsUnknownFormatsAndDifferentDeclarations() {
        Checkpoint checkpoint = state().checkpoint();
        assertThrows(IllegalArgumentException.class, () -> SnapshotState.from(checkpoint.with("format", 2), 1024, 32, 131072));
        assertThrows(IllegalArgumentException.class, () -> SnapshotState.from(checkpoint.with("extra", true), 1024, 32, 131072));
        assertThrows(IllegalArgumentException.class, () -> SnapshotState.from(checkpoint, 512, 32, 131072));
        assertThrows(IllegalArgumentException.class, () -> SnapshotState.from(checkpoint.with("keys", List.of("YQ")), 1024, 32, 131072));
        assertThrows(IllegalArgumentException.class, () -> SnapshotState.from(checkpoint.with("keys", List.of("YQ==", "YQ==")), 1024, 32, 131072));
    }

    @Test void rejectsMalformedOversizedAndEmptyKeys() {
        var state = state();
        assertThrows(IllegalArgumentException.class, () -> state.add(new byte[0]));
        assertThrows(IllegalArgumentException.class, () -> state.add(new byte[33]));
        assertThrows(IllegalArgumentException.class, () -> SnapshotState.from(state.checkpoint().with("keys", List.of("not/base64!")), 1024, 32, 131072));
        assertThrows(IllegalArgumentException.class, () -> SnapshotState.from(state.checkpoint().with("keys", List.of(1)), 1024, 32, 131072));
    }
}
