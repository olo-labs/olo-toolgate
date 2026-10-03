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
        default FleetStore fleet() { throw Failure.unavailable(); }
        default BuilderStore builder() { throw Failure.unavailable(); }
        Directory load();
        boolean used(io.ololabs.toolgate.control.domain.Ids.RecordId id);
        void save(Directory before, Directory after);
        Reply replay(String actor, String key, String digest);
        void remember(String actor, String key, String digest, Reply reply);
        void audit(String actor, String operation, String target, long revision, String requestId, String digest);
        String auditPage(long after, int limit);
        long bundleSequence();
        BundleRecord bundle(long sequence);
        void publishBundle(BundleRecord bundle);
        ApprovalRecord approval(String id);
        ApprovalRecord approvalBinding(String digest, long now);
        java.util.List<ApprovalRecord> approvals(String after, int limit);
        long activeApprovals(long now);
        void saveApproval(ApprovalRecord approval);
        PermitLease permit(String jti);
        void lease(PermitLease permit);
        void consumePermit(String jti, long consumedAt);
        long approvalClock(long now);
        long activePermits(long now);
        EnrollmentRecord enrollment(String id);
        EnrollmentRecord enrollmentCode(String codeDigest);
        void saveEnrollment(EnrollmentRecord enrollment);
        long pendingEnrollments(long now);
        void pruneEnrollments(long now);
        EndpointRecord endpoint(String deviceId);
        EndpointRecord endpointKey(String fingerprint);
        void saveEndpoint(EndpointRecord endpoint);
    }
    /** Immutable signed wire bytes and compiler bytes, committed with audit/replay. */
    record BundleRecord(long sequence, String document, String policy, long directoryRevision) {}
    /** Approval snapshots include only exact normalized identities and digests, never raw arguments. */
    record ApprovalRecord(String id, String bindingDigest, String document, long expiresAt) {}
    /** Globally unique random attempt IDs cannot issue or consume two capabilities. */
    record PermitLease(String jti, String approvalId, String bindingDigest, String requestId,
                       long issuedAt, long expiresAt, Long consumedAt) {}
    record Reply(int status, String body, long revision) {}
    /** Device codes are hashed; CSR and certificate data are public key material. */
    record EnrollmentRecord(String id, String codeDigest, String deviceDigest, String document,
                            String csr, String userId, String certificate, long expiresAt, long lastPoll) {}
    record EndpointRecord(String id, String fingerprint, String document, String csr,
                          String reportDigest, String acknowledgment) {}
}
