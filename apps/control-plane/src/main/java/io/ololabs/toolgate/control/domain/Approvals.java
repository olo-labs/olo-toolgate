// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
package io.ololabs.toolgate.control.domain;

import io.ololabs.toolgate.contracts.*;

/** Pure lifecycle transitions. Security authorization remains in the application use case. */
public final class Approvals {
    private Approvals() {}
    public static ApprovalRecord expire(ApprovalRecord record, long now) {
        if (now < record.expiresAtUnixMs() || record.state() == ApprovalState.EXPIRED
                || record.state() == ApprovalState.DENIED || record.state() == ApprovalState.CONSUMED) return record;
        return copy(record, ApprovalState.EXPIRED, record.expiresAtUnixMs(), record.decidedBy(), record.decidedAtUnixMs());
    }
    public static ApprovalRecord decide(ApprovalRecord record, ApprovalDecisionRequest request,
                                        String approver, long now, long policyExpiry) {
        if (record.state() != ApprovalState.PENDING || record.expiresAtUnixMs() <= now
                || record.revision().longValue() != request.expectedRevision().longValue()) throw new IllegalStateException("Decision conflict");
        var state = switch (request.decision()) {
            case APPROVE_ONCE -> ApprovalState.APPROVED_ONCE;
            case APPROVE_TEMPORARY -> ApprovalState.APPROVED_TEMPORARY;
            case DENY -> ApprovalState.DENIED;
        };
        long expires = record.expiresAtUnixMs();
        if (state == ApprovalState.APPROVED_TEMPORARY) {
            if (request.durationMs() == null || request.durationMs() < 1 || request.durationMs() > 1800000) throw new IllegalArgumentException("Invalid temporary duration");
            expires = Math.min(policyExpiry, Math.addExact(now, request.durationMs()));
        } else if (request.durationMs() != null) throw new IllegalArgumentException("Unexpected duration");
        if (expires <= now) throw new IllegalStateException("Approval expired");
        return copy(record, state, expires, Ids.valid(approver), now);
    }
    public static ApprovalRecord spend(ApprovalRecord record, long now) {
        if (record.state() != ApprovalState.APPROVED_ONCE || record.expiresAtUnixMs() <= now) throw new IllegalStateException("Approval not spendable");
        return copy(record, ApprovalState.CONSUMED, record.expiresAtUnixMs(), record.decidedBy(), record.decidedAtUnixMs());
    }
    private static ApprovalRecord copy(ApprovalRecord record, ApprovalState state, long expires, String actor, Long at) {
        if (record.revision() >= 9007199254740991L) throw new IllegalStateException("Revision exhausted");
        return new ApprovalRecord(record.id(), record.revision()+1, state, record.input(), record.policyVersion(),
            record.createdAtUnixMs(), expires, actor, at);
    }
}
