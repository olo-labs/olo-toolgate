// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
package io.ololabs.toolgate.control.domain;

import java.util.Map;
import java.util.Set;
import io.ololabs.toolgate.control.domain.Ids.RecordId;

/** Validated administrative graph. Configuration is data and does not execute tool code. */
public record Directory(long revision, Map<RecordId, Entry> entries) {
    public Directory {
        if (revision < 0 || revision > 9007199254740991L) throw new IllegalArgumentException("Invalid revision");
        entries = Map.copyOf(entries);
    }
    public record Entry(RecordId id, boolean enabled, long revision, String document,
                        Set<RecordId> references) {
        public Entry {
            java.util.Objects.requireNonNull(id);
            java.util.Objects.requireNonNull(document);
            if (revision < 1 || revision > 9007199254740991L) throw new IllegalArgumentException("Invalid revision");
            references = Set.copyOf(references);
        }
    }
    /** Reject dangling or disabled dependencies and keep export/import bounded. */
    public void validate(int maxRecords, int maxBytes) {
        if (entries.size() > maxRecords) throw new IllegalArgumentException("Directory record limit exceeded");
        long size = 0;
        for (var entry : entries.values()) {
            size += entry.document().getBytes(java.nio.charset.StandardCharsets.UTF_8).length;
            for (var ref : entry.references()) {
                var target = entries.get(ref);
                if (target == null || (entry.enabled() && !target.enabled())) {
                    throw new IllegalArgumentException("Missing or disabled dependency");
                }
            }
        }
        if (size > maxBytes) throw new IllegalArgumentException("Directory byte limit exceeded");
    }
}
