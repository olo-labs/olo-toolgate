// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
package io.ololabs.toolgate.control.application;

import io.ololabs.toolgate.control.domain.Ids;
import io.ololabs.toolgate.contracts.*;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.Base64;

/** Synchronous publish/rollback state machine with one durable transaction boundary. */
public final class BundleService {
    private final Store store;
    private final Codec codec;
    private final BundleSigner signer;
    private final PolicyCompiler compiler;
    private final String issuer;
    private final String audience;
    private final Clock clock;
    public BundleService(Store store, Codec codec, BundleSigner signer, String issuer, String audience, Clock clock) {
        this.store = store; this.codec = codec; this.signer = signer; this.compiler = new PolicyCompiler(codec);
        this.issuer = Ids.valid(issuer); this.audience = Ids.valid(audience); this.clock = clock;
    }
    public Store.Reply current(DirectoryService.Actor actor) { return get(actor, 0); }
    public Store.Reply get(DirectoryService.Actor actor, long sequence) {
        if (sequence < 0 || sequence > 9007199254740991L) throw Failure.validation();
        return store.transaction(actor.tenant(), false, tx -> {
            var record = tx.bundle(sequence == 0 ? tx.bundleSequence() : sequence);
            if (record == null) throw new Failure(ErrorCode.NOT_FOUND, 404, "Bundle not found");
            return new Store.Reply(200, record.document(), record.sequence());
        });
    }
    public Store.Reply publish(DirectoryService.Actor actor, String document, String key, String requestId) {
        actor.requireAdmin(); Ids.valid(key); Ids.valid(requestId);
        var request = codec.model(document, BundlePublishRequest.class);
        var digest = DirectoryService.digest("BUNDLE\n" + codec.json(request));
        return store.transaction(actor.tenant(), true, tx -> {
            var replay = tx.replay(actor.id(), key, digest);
            if (replay != null) return replay;
            var directory = tx.load();
            long previous = tx.bundleSequence();
            if (directory.revision() != request.directoryRevision() || previous != request.expectedSequence()
                    || previous >= 9007199254740991L) throw Failure.conflict();
            String policy;
            long sourceRevision = directory.revision();
            if (request.rollbackOf() == null) policy = compiler.compile(directory, request.gracePolicyIds() == null ? java.util.List.of() : request.gracePolicyIds());
            else {
                if (request.gracePolicyIds() != null) throw Failure.validation();
                if (request.rollbackOf() > previous) throw Failure.conflict();
                var old = tx.bundle(request.rollbackOf());
                if (old == null) throw new Failure(ErrorCode.NOT_FOUND, 404, "Bundle not found");
                policy = old.policy(); codec.model(policy, CompiledPolicy.class);
                sourceRevision = old.directoryRevision();
            }
            long sequence = previous + 1;
            long issued = clock.millis();
            if (issued < 0 || issued > 9007199254740991L - request.lifetimeMs()) throw Failure.unavailable();
            var payload = new BundlePayload(1L, issuer, audience, actor.tenant().value(), sequence, "1.0." + sequence,
                sourceRevision, issued, issued + request.lifetimeMs(), request.graceMs(), DirectoryService.digest(policy),
                Base64.getUrlEncoder().withoutPadding().encodeToString(policy.getBytes(StandardCharsets.UTF_8)), request.rollbackOf());
            var signed = codec.json(signer.sign(payload));
            codec.model(signed, SignedPolicyBundle.class);
            tx.publishBundle(new Store.BundleRecord(sequence, signed, policy, sourceRevision));
            tx.audit(actor.id(), request.rollbackOf() == null ? "BUNDLE_PUBLISH" : "BUNDLE_ROLLBACK", "bundle:" + sequence,
                directory.revision(), requestId, digest);
            var reply = new Store.Reply(201, signed, sequence);
            tx.remember(actor.id(), key, digest, reply); return reply;
        });
    }
}
