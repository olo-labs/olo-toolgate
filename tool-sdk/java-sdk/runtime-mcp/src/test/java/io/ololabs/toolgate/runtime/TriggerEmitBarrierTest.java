// Copyright 2026 OLO Labs; SPDX-License-Identifier: Apache-2.0
package io.ololabs.toolgate.runtime;

import static io.ololabs.toolgate.runtime.TriggerEmitBarrier.Resolution.*;
import static org.junit.jupiter.api.Assertions.*;

import io.ololabs.toolgate.sdk.trigger.Checkpoint;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;
import org.junit.jupiter.api.Test;

class TriggerEmitBarrierTest {
    private static Checkpoint position(long offset) { return Checkpoint.of("offset", offset); }

    @Test void durabilityAndAcceptanceAreSeparateContiguousBarriers() {
        var barrier = new TriggerEmitBarrier(3, true);
        for (int n = 1; n <= 3; n++) assertEquals((long) n, barrier.register(position(n)).orElseThrow());
        barrier.acknowledge(3, LOCAL_DURABLE);
        assertEquals(0, barrier.local().emitSeq());
        assertTrue(barrier.local().position().isEmpty());
        assertThrows(IllegalArgumentException.class, () -> barrier.acceptedUpTo(3));
        barrier.acknowledge(1, LOCAL_DURABLE);
        assertEquals(position(1), barrier.local().position().orElseThrow());
        assertEquals(0, barrier.accepted().emitSeq());
        barrier.acceptedUpTo(1);
        assertEquals(position(1), barrier.accepted().position().orElseThrow());
        barrier.acknowledge(2, LOCAL_DURABLE);
        assertEquals(3, barrier.local().emitSeq());
        assertEquals(position(3), barrier.local().position().orElseThrow());
        assertEquals(1, barrier.accepted().emitSeq(), "local ACKs never stand in for Control");
        barrier.acceptedUpTo(3);
        assertEquals(position(3), barrier.accepted().position().orElseThrow());
        assertEquals(0, barrier.pending());
    }

    @Test void retriesHoldSequenceAndCapacityUntilResolvedAndAccepted() {
        var barrier = new TriggerEmitBarrier(1, true);
        assertEquals(1L, barrier.register(position(1)).orElseThrow());
        barrier.acknowledge(1, RETRYABLE_REJECTION);
        barrier.acknowledge(1, RETRYABLE_REJECTION);
        assertEquals(0, barrier.local().emitSeq());
        assertTrue(barrier.register(position(2)).isEmpty());
        barrier.acknowledge(1, LOCAL_DURABLE);
        assertTrue(barrier.register(position(2)).isEmpty(), "offline Control still bounds memory");
        barrier.acceptedUpTo(1);
        assertEquals(2L, barrier.register(position(2)).orElseThrow());
    }

    @Test void haltCannotCheckpointPastTerminalRejectionEvenWhenLaterAcksArriveFirst() {
        var barrier = new TriggerEmitBarrier(4, true);
        for (int n = 1; n <= 4; n++) barrier.register(position(n));
        barrier.acknowledge(4, LOCAL_DURABLE);
        barrier.acknowledge(3, LOCAL_DURABLE);
        barrier.acknowledge(2, TERMINAL_REJECTION);
        barrier.acknowledge(1, LOCAL_DURABLE);
        assertTrue(barrier.halted());
        assertEquals(1, barrier.local().emitSeq());
        barrier.acceptedUpTo(1);
        assertThrows(IllegalArgumentException.class, () -> barrier.acceptedUpTo(2));
        assertThrows(IllegalStateException.class, () -> barrier.register(position(5)));
        assertEquals(position(1), barrier.accepted().position().orElseThrow());
    }

    @Test void recordedTerminalDispositionResolvesOnlyLocalLevelUntilControlAcknowledges() {
        var barrier = new TriggerEmitBarrier(2, false);
        barrier.register(position(1));
        barrier.register(position(2));
        barrier.acknowledge(2, TERMINAL_REJECTION);
        barrier.acknowledge(1, LOCAL_DURABLE);
        assertFalse(barrier.halted());
        assertEquals(2, barrier.local().emitSeq());
        assertEquals(0, barrier.accepted().emitSeq());
        barrier.acceptedUpTo(2);
        assertEquals(position(2), barrier.accepted().position().orElseThrow());
    }

    @Test void invalidAndConflictingAcknowledgementsHaveNoEffect() {
        var barrier = new TriggerEmitBarrier(2, true);
        barrier.register(position(1));
        assertThrows(IllegalArgumentException.class, () -> barrier.acknowledge(0, LOCAL_DURABLE));
        assertThrows(IllegalArgumentException.class, () -> barrier.acknowledge(2, LOCAL_DURABLE));
        assertThrows(IllegalArgumentException.class, () -> barrier.acknowledge(1, null));
        assertThrows(IllegalArgumentException.class, () -> barrier.acceptedUpTo(-1));
        assertThrows(IllegalArgumentException.class, () -> barrier.acceptedUpTo(1));
        assertEquals(0, barrier.local().emitSeq());
        assertEquals(1, barrier.pending());
        barrier.acknowledge(1, LOCAL_DURABLE);
        assertThrows(IllegalArgumentException.class, () -> barrier.acknowledge(1, TERMINAL_REJECTION));
        barrier.acknowledge(1, LOCAL_DURABLE);
        assertFalse(barrier.halted());
        barrier.acceptedUpTo(1);
        barrier.acceptedUpTo(1);
        assertThrows(IllegalArgumentException.class, () -> barrier.acceptedUpTo(0));
        barrier.acknowledge(1, LOCAL_DURABLE);
        assertEquals(0, barrier.pending());
    }

    @Test void positionlessEventsDoNotEraseTheLastCheckpoint() {
        var barrier = new TriggerEmitBarrier(3, true);
        barrier.register(position(1));
        barrier.register(null);
        barrier.register(null);
        for (int n = 1; n <= 3; n++) barrier.acknowledge(n, LOCAL_DURABLE);
        barrier.acceptedUpTo(3);
        assertEquals(3, barrier.accepted().emitSeq());
        assertEquals(position(1), barrier.accepted().position().orElseThrow());
    }

    @Test void closedGenerationCannotBeReusedAndNewProcessRestartsSequence() {
        var old = new TriggerEmitBarrier(1, true);
        old.register(position(10));
        old.close();
        old.close();
        assertEquals(0, old.pending());
        assertThrows(IllegalStateException.class, () -> old.register(position(11)));
        assertThrows(IllegalStateException.class, () -> old.acknowledge(1, LOCAL_DURABLE));
        assertThrows(IllegalStateException.class, () -> old.acceptedUpTo(1));
        var fresh = new TriggerEmitBarrier(1, true);
        assertEquals(1L, fresh.register(position(10)).orElseThrow());
        assertTrue(fresh.accepted().position().isEmpty());
    }

    @Test void randomizedCompletionsNeverAdvancePastAGapOrInventAcceptance() {
        for (int seed = 0; seed < 200; seed++) {
            var random = new Random(seed);
            var barrier = new TriggerEmitBarrier(32, false);
            var order = new ArrayList<Integer>();
            var resolved = new boolean[33];
            for (int n = 1; n <= 32; n++) { barrier.register(position(n)); order.add(n); }
            Collections.shuffle(order, random);
            long accepted = 0;
            for (int n : order) {
                barrier.acknowledge(n, RETRYABLE_REJECTION);
                barrier.acknowledge(n, random.nextBoolean() ? LOCAL_DURABLE : TERMINAL_REJECTION);
                resolved[n] = true;
                int contiguous = 0;
                while (contiguous < 32 && resolved[contiguous + 1]) contiguous++;
                assertEquals(contiguous, barrier.local().emitSeq(), "seed " + seed);
                assertEquals(accepted, barrier.accepted().emitSeq());
                if (random.nextBoolean()) {
                    accepted = contiguous;
                    barrier.acceptedUpTo(accepted);
                }
                assertEquals(32 - accepted, barrier.pending());
            }
            barrier.acceptedUpTo(32);
            assertEquals(position(32), barrier.accepted().position().orElseThrow());
            assertEquals(0, barrier.pending());
        }
    }

    @Test void randomizedHaltAndProcessLossNeverPromoteUnacceptedPositions() {
        for (int seed = 0; seed < 200; seed++) {
            var random = new Random(seed);
            var barrier = new TriggerEmitBarrier(16, true);
            var order = new ArrayList<Integer>();
            var resolved = new boolean[17];
            int rejected = 1 + random.nextInt(16);
            for (int n = 1; n <= 16; n++) { barrier.register(position(n)); order.add(n); }
            Collections.shuffle(order, random);
            int accepted = 0;
            for (int n : order) {
                barrier.acknowledge(n, n == rejected ? TERMINAL_REJECTION : LOCAL_DURABLE);
                resolved[n] = true;
                int contiguous = 0;
                while (contiguous + 1 < rejected && resolved[contiguous + 1]) contiguous++;
                assertEquals(contiguous, barrier.local().emitSeq(), "seed " + seed);
                if (random.nextBoolean()) {
                    accepted = contiguous;
                    barrier.acceptedUpTo(accepted);
                }
                assertEquals(accepted, barrier.accepted().emitSeq());
            }
            var durable = barrier.accepted();
            barrier.close(); // crash/lease loss: all in-flight bookkeeping is lost
            var restarted = new TriggerEmitBarrier(16, true);
            assertEquals(0, restarted.local().emitSeq());
            assertTrue(restarted.accepted().position().isEmpty());
            // Only the external supervisor may supply durable.position() as restored state.
            assertEquals(accepted, durable.emitSeq());
            assertTrue(accepted < rejected);
        }
    }

    @Test void capacityIsAlwaysExplicitAndBounded() {
        assertThrows(IllegalArgumentException.class, () -> new TriggerEmitBarrier(0, true));
        assertThrows(IllegalArgumentException.class, () -> new TriggerEmitBarrier(1025, true));
        var barrier = new TriggerEmitBarrier(1024, true);
        for (int n = 1; n <= 1024; n++) assertTrue(barrier.register(null).isPresent());
        assertTrue(barrier.register(null).isEmpty());
        assertEquals(1024, barrier.emitted());
    }
}
