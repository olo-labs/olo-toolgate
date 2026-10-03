// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
package io.ololabs.toolgate.control.application;

import java.util.List;

/** Tenant-scoped fleet records share the enclosing Store transaction/audit/replay commit. */
public interface FleetStore {
    enum Kind { RELEASE, DESIRED, ROLLOUT }
    record Row(String id,long revision,String document) {}
    Row get(Kind kind,String id);
    List<Row> page(Kind kind,String after,int limit);
    long count(Kind kind);
    /** Expected zero means immutable insert; positive means compare-and-swap under tenant serialization. */
    void save(Kind kind,Row row,long expectedRevision);
}
