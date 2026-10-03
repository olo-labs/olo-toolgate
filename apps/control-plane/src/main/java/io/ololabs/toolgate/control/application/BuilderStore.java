// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
package io.ololabs.toolgate.control.application;
import java.util.List;
/** Authoring state shares the parent tenant transaction, audit and replay boundary. */
public interface BuilderStore {
    enum Kind { DRAFT, VERSION, TEST }
    record Row(String id,long revision,String document) {}
    Row get(Kind kind,String id);
    List<Row> page(Kind kind,String after,int limit);
    long count(Kind kind);
    void save(Kind kind,Row row,long expected);
    void prune(long before);
}
