// Copyright 2026 OLO Labs; SPDX-License-Identifier: Apache-2.0
package io.ololabs.toolgate.runtime;

import io.ololabs.toolgate.sdk.trigger.Checkpoint;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Process-local bookkeeping for the PUSH emitter (plan 6.6). This is not a durable store or an authority:
 * only supervisor acknowledgements advance it. The owner serializes all calls and executes user callbacks
 * outside this state machine. No provider data, event payload or credential is retained here.
 *
 * <p>Entries remain bounded until Control acceptance, including locally resolved entries. Thus an offline
 * Control applies backpressure rather than allowing unbounded accepted-position bookkeeping. A new process
 * creates a new barrier and starts at emitSeq 1; it must restore only the supervisor-supplied checkpoint.
 */
final class TriggerEmitBarrier {
    static final long MAX_SEQUENCE = 9_007_199_254_740_991L;

    enum Resolution { LOCAL_DURABLE, RETRYABLE_REJECTION, TERMINAL_REJECTION }

    record Progress(long emitSeq, Optional<Checkpoint> position) {}

    private static final class Entry {
        final Checkpoint position;
        Resolution resolution;

        Entry(Checkpoint position) { this.position = position; }
    }

    private final int capacity;
    private final boolean haltOnTerminal;
    private final Map<Long, Entry> pending = new LinkedHashMap<>();
    private long emitted;
    private long local;
    private long accepted;
    private long haltAt = Long.MAX_VALUE;
    private Checkpoint localPosition;
    private Checkpoint acceptedPosition;
    private boolean closed;

    TriggerEmitBarrier(int capacity, boolean haltOnTerminal) {
        if (capacity < 1 || capacity > 1024) throw new IllegalArgumentException("emitter capacity must be 1-1024");
        this.capacity = capacity;
        this.haltOnTerminal = haltOnTerminal;
    }

    /** Reserve in emit-call order, after validating/encoding the event. Empty means BLOCK backpressure. */
    Optional<Long> register(Checkpoint position) {
        if (closed) throw new IllegalStateException("emitter is closed");
        if (haltAt != Long.MAX_VALUE) throw new IllegalStateException("emitter halted on terminal rejection");
        if (emitted == MAX_SEQUENCE) throw new IllegalStateException("emit sequence exhausted");
        if (pending.size() == capacity) return Optional.empty();
        long sequence = ++emitted;
        pending.put(sequence, new Entry(position));
        return Optional.of(sequence);
    }

    /** A retry keeps its original sequence unresolved; it never frees capacity or advances a position. */
    void acknowledge(long sequence, Resolution resolution) {
        if (closed) throw new IllegalStateException("emitter is closed");
        if (resolution == null) throw new IllegalArgumentException("missing emit resolution");
        checkSequence(sequence);
        // A duplicate for a retired entry cannot move either watermark.
        if (sequence <= accepted) return;
        Entry entry = pending.get(sequence);
        if (entry.resolution != null) {
            if (entry.resolution != resolution) throw new IllegalArgumentException("conflicting emit resolution");
            return;
        }
        if (resolution == Resolution.RETRYABLE_REJECTION) return;
        entry.resolution = resolution;
        if (resolution == Resolution.TERMINAL_REJECTION && haltOnTerminal) haltAt = Math.min(haltAt, sequence);
        while (local < emitted && local + 1 < haltAt) {
            Entry next = pending.get(local + 1);
            if (next.resolution == null) break;
            local++;
            if (next.position != null) localPosition = next.position;
        }
    }

    /**
     * Control's contiguous process watermark, not an outbox sequence. Local durability alone cannot call
     * this. Terminal resolutions may be included only under SKIP_AND_RECORD: the supervisor attests that
     * Control recorded the disposition. Invalid frames leave all state unchanged.
     */
    void acceptedUpTo(long sequence) {
        if (closed) throw new IllegalStateException("emitter is closed");
        if (sequence < accepted || sequence > local) throw new IllegalArgumentException("invalid accepted watermark");
        while (accepted < sequence) {
            Entry entry = pending.remove(++accepted);
            if (entry.position != null) acceptedPosition = entry.position;
        }
    }

    Progress local() { return new Progress(local, Optional.ofNullable(localPosition)); }
    Progress accepted() { return new Progress(accepted, Optional.ofNullable(acceptedPosition)); }
    long emitted() { return emitted; }
    int pending() { return pending.size(); }
    boolean halted() { return haltAt != Long.MAX_VALUE; }

    /** Lease loss/STOP invalidates this owner; stale callbacks cannot register or acknowledge work. */
    void close() {
        closed = true;
        pending.clear();
    }

    private void checkSequence(long sequence) {
        if (sequence < 1 || sequence > emitted) throw new IllegalArgumentException("unknown emit sequence");
    }
}
