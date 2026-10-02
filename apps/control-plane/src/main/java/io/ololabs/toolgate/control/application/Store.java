// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
package io.ololabs.toolgate.control.application;

import io.ololabs.toolgate.control.domain.Directory;
import io.ololabs.toolgate.control.domain.Ids.TenantId;
import java.util.function.Function;

/** Transaction port. Data, audit and idempotency must share a commit/rollback boundary. */
public interface Store {
    <T> T transaction(TenantId tenant, boolean write, Function<Session, T> work);
    interface Session {
        Directory load();
        boolean used(io.ololabs.toolgate.control.domain.Ids.RecordId id);
        void save(Directory before, Directory after);
        Reply replay(String actor, String key, String digest);
        void remember(String actor, String key, String digest, Reply reply);
        void audit(String actor, String operation, String target, long revision, String requestId, String digest);
        String auditPage(long after, int limit);
    }
    record Reply(int status, String body, long revision) {}
}
