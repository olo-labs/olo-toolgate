// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
package io.ololabs.toolgate.control.application;

import io.ololabs.toolgate.contracts.*;
import java.util.List;

/** All methods run inside the tenant transaction which also commits audit and directory state. */
public interface EnterpriseStore {
    long authorizationEpoch();
    EnterpriseInvocation invocation(String id);
    void saveInvocation(EnterpriseInvocation invocation,long createdAt,long expectedRevision);
    long invocationsSince(long since);
    EnterpriseApproval approval(String id);
    List<EnterpriseApproval> approvals(String after,int limit);
    void saveApproval(EnterpriseApproval approval,long expectedRevision);
    Nonce nonce(String id);
    void reserveNonce(Nonce nonce);
    void consumeNonce(String id,long now);
    boolean credentialUsed(String sha256);
    void rememberCredential(String sha256);
    boolean recoveryApplied(String digest);
    void rememberRecovery(String digest);
    EnterpriseConfigurationChange configuration(String id);
    List<EnterpriseConfigurationChange> configurations(String after,int limit);
    void saveConfiguration(EnterpriseConfigurationChange change,long expectedRevision);
    Secret secret(String name);
    List<Secret> secrets();
    void saveSecret(Secret secret);
    void acknowledge(EnterpriseAdoptionStatus status);
    List<EnterpriseAdoptionStatus> adoptions();
    void published(long revision,long now);
    record Secret(String name,String toolGroupId,String deviceGroupId,String cipher) {}
    record Nonce(String id,String invocationId,String evaluationDigest,long authorizationEpoch,long expiresAt,Long consumedAt) {}
}
