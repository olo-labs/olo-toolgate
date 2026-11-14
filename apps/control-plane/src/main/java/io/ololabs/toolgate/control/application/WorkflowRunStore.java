// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
package io.ololabs.toolgate.control.application;

/** Durable trigger admissions and pinned queued runs in the enclosing tenant transaction. */
public interface WorkflowRunStore {
    record Admission(String subscriptionUid, String cursor, String ownerId, String admissionId, String disposition, String runId) {}

    record QueuedRun(String id, String profileId, String profileDigest, String workflowId, String workflowVersionId,
                     String serviceAgentId, String serviceAgentGroupId, String remediationMode, long createdAt,
                     long keyVersion, String document) {
        @Override public String toString() { return "QueuedRun[id=" + id + "]"; }
    }

    Admission admission(String subscriptionUid, String cursor);
    void insertAdmission(Admission admission);
    void enqueue(QueuedRun run);
    QueuedRun queued(String id);
}
