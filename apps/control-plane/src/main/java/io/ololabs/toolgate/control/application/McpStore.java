// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
package io.ololabs.toolgate.control.application;

/** Tenant-transactional bounded request queue. No agent credentials or executable source are stored. */
public interface McpStore {
    record Row(String id,String deviceId,String state,long expiresAt,String record,String task,String result) {}
    Row get(String id);
    java.util.List<Row> page(int limit);
    java.util.List<Row> pending(String deviceId,int limit);
    void save(Row row);
    void prune(long before);
    long count();
}
