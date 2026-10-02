// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
package io.ololabs.toolgate.control.application;

import io.ololabs.toolgate.control.domain.Directory;
import io.ololabs.toolgate.control.domain.Ids.Kind;
import io.ololabs.toolgate.control.domain.Ids.TenantId;
import java.util.Map;

/** Shared contract conversion port. Untrusted data is validated before producing domain entries. */
public interface Codec {
    Directory.Entry entry(Kind kind, String document);
    Directory.Entry revision(Directory.Entry entry, long revision);
    Import input(String document, boolean yaml, TenantId tenant);
    String snapshot(TenantId tenant, Directory directory, boolean yaml);
    String json(Object value);
    Object value(String document);
    void validatePolicies(Directory directory);
    record Import(Directory directory, boolean replace, boolean dryRun) {}
    default String result(boolean applied, long revision, java.util.List<Map<String, String>> changes) {
        return json(Map.of("applied", applied, "revision", revision, "changes", changes));
    }
}
