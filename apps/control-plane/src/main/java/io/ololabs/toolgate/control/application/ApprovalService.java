// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
package io.ololabs.toolgate.control.application;

import io.ololabs.toolgate.control.domain.Approvals;
import io.ololabs.toolgate.control.domain.Ids;
import io.ololabs.toolgate.contracts.*;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

/** Durable exact-operation approvals. Only ASK performs this bounded remote coordination. */
public final class ApprovalService {
    private static final System.Logger LOG=System.getLogger(ApprovalService.class.getName());
    private final Store store;
    private final Codec codec;
    private final Clock clock;
    private final boolean enabled;
    private final long pendingTtl;
    private final int maxActive;
    private final int maxPermits;
    private final String issuer;
    private final String audience;
    private final ApprovalNotification notification;
    public ApprovalService(Store store, Codec codec, Clock clock, boolean enabled, long pendingTtl,
                           int maxActive, int maxPermits, String issuer, String audience, ApprovalNotification notification) {
        if (pendingTtl < 1000 || pendingTtl > 300000 || maxActive < 1 || maxActive > 10000
                || maxPermits < 1 || maxPermits > 10000) throw new IllegalArgumentException("Invalid approval limits");
        this.store=store; this.codec=codec; this.clock=clock; this.enabled=enabled; this.pendingTtl=pendingTtl;
        this.maxActive=maxActive; this.maxPermits=maxPermits; this.issuer=Ids.valid(issuer); this.audience=Ids.valid(audience); this.notification=notification;
    }
    /** Roles come exclusively from a verified identity provider, never directory data. */
    public record Actor(DirectoryService.Actor identity, String userId, boolean approver, boolean gateway) {
        public Actor { java.util.Objects.requireNonNull(identity); if (userId != null) Ids.valid(userId); }
    }
    private void available() { if (!enabled) throw Failure.unavailable(); }
    private void requireGateway(Actor actor) {
        available(); if (!actor.gateway() || actor.approver()) throw new Failure(ErrorCode.FORBIDDEN,403,"Dedicated Gateway role required");
    }
    private void requireApprover(Actor actor, Store.Session tx) {
        available(); if (!actor.approver() || actor.gateway() || actor.userId() == null) throw new Failure(ErrorCode.FORBIDDEN,403,"Dedicated approver role required");
        requireEnabled(tx.load(), Ids.Kind.USER, actor.userId());
        var user=codec.model(tx.load().entries().get(Ids.Kind.USER.id(actor.userId())).document(),ControlUser.class);
        var resolved=new RoleResolver(codec).resolve(tx.load(),user);
        if((resolved.managed() || user.access()!=null && user.access().role()==UserRole.BASIC) && !resolved.approver())
            throw new Failure(ErrorCode.FORBIDDEN,403,"Approval template required");
    }
    private static final class Observation { long value=-1; }
    /** Failed authorization rolls back state, but authenticated observed time must not roll back. */
    private <T> T transaction(Actor actor,java.util.function.BiFunction<Store.Session,Observation,T> work) {
        var observation=new Observation();
        try { return store.transaction(actor.identity().tenant(),true,tx->work.apply(tx,observation)); }
        catch (RuntimeException failure) {
            if (observation.value>=0) {
                store.transaction(actor.identity().tenant(),true,tx->{ tx.approvalClock(observation.value); return null; });
            }
            throw failure;
        }
    }
    private long now(Store.Session tx,Observation observation) {
        long now=clock.millis();
        if (now < 0 || now > 9007199254740991L) throw Failure.unavailable();
        observation.value=now;
        long observed=tx.approvalClock(now); observation.value=Math.max(now,observed);
        if (observed>now) throw Failure.unavailable();
        return now;
    }
    private ApprovalRecord record(Store.ApprovalRecord row) {
        if (row == null) throw new Failure(ErrorCode.NOT_FOUND,404,"Approval not found");
        return codec.model(row.document(),ApprovalRecord.class);
    }
    private Store.ApprovalRecord row(ApprovalRecord record) {
        var document=codec.json(record); codec.model(document,ApprovalRecord.class);
        if (document.getBytes(StandardCharsets.UTF_8).length > 16384) throw Failure.validation();
        return new Store.ApprovalRecord(record.id(),binding(record.input(),record.policyVersion()),document,record.expiresAtUnixMs());
    }
    /** Exclude ingress correlation only; every authorization dimension remains exact. */
    public String binding(PolicyInput input,String version) {
        var c=input.context();
        var normalized=new PolicyInput(new RequestContext("binding",c.tenantId(),c.userId(),c.agentId(),c.deviceId()),input.toolId(),input.action(),input.resource(),input.argumentsDigest());
        return DirectoryService.digest(codec.json(new ApprovalSubmission(normalized,version)));
    }
    private ApprovalRecord expire(Store.Session tx,Actor actor,ApprovalRecord record,long now,String requestId) {
        var expired=Approvals.expire(record,now);
        if (!expired.equals(record)) {
            tx.saveApproval(row(expired)); audit(tx,actor,"APPROVAL_EXPIRE",expired,requestId);
        }
        return expired;
    }
    private void audit(Store.Session tx,Actor actor,String operation,ApprovalRecord record,String requestId) {
        tx.audit(actor.identity().id(),operation,"approval:"+record.id(),record.revision(),requestId,binding(record.input(),record.policyVersion()));
    }
    public Store.Reply get(Actor actor,String id,String requestId) {
        Ids.valid(id); Ids.valid(requestId);
        return transaction(actor,(tx,observation)->{
            requireApprover(actor,tx); var result=expire(tx,actor,record(tx.approval(id)),now(tx,observation),requestId);
            return new Store.Reply(200,codec.json(result),result.revision());
        });
    }
    public Store.Reply page(Actor actor,String cursor,int limit,String requestId) {
        if (limit < 1 || limit > 100) throw Failure.validation();
        String after=cursor == null ? "" : Ids.valid(cursor); Ids.valid(requestId);
        return transaction(actor,(tx,observation)->{
            requireApprover(actor,tx); long now=now(tx,observation); var rows=tx.approvals(after,limit+1);
            var items=rows.stream().limit(limit).map(r->expire(tx,actor,record(r),now,requestId)).toList();
            String next=rows.size()>limit ? items.getLast().id() : null;
            return new Store.Reply(200,codec.json(new ApprovalPage(items,next)),0);
        });
    }
    public Store.Reply decide(Actor actor,String id,String document,String key,String requestId) {
        available(); Ids.valid(id); Ids.valid(key); Ids.valid(requestId);
        var request=codec.model(document,ApprovalDecisionRequest.class);
        var digest=DirectoryService.digest("APPROVAL_DECIDE\n"+id+"\n"+codec.json(request));
        return transaction(actor,(tx,observation)->{
            requireApprover(actor,tx); long now=now(tx,observation);
            var current=record(tx.approval(id));
            if (current.input().context().userId().equals(actor.userId())) throw new Failure(ErrorCode.FORBIDDEN,403,"Requester cannot decide approval");
            // Authorization and current policy are rechecked before returning any durable replay.
            var policy=currentAsk(tx,actor,current.input(),current.policyVersion(),now);
            var replay=tx.replay(actor.identity().id(),key,digest); if (replay != null) return replay;
            var existing=expire(tx,actor,current,now,requestId);
            ApprovalRecord decided;
            try { decided=Approvals.decide(existing,request,actor.userId(),now,policy.expiresAtUnixMs()); }
            catch (IllegalArgumentException e) { throw Failure.validation(); }
            catch (IllegalStateException e) { throw Failure.conflict(); }
            tx.saveApproval(row(decided)); audit(tx,actor,"APPROVAL_DECIDE",decided,requestId);
            var reply=new Store.Reply(200,codec.json(decided),decided.revision());
            tx.remember(actor.identity().id(),key,digest,reply); return reply;
        });
    }
    public Store.Reply resolve(Actor actor,String document,String requestId) {
        requireGateway(actor); Ids.valid(requestId); var request=codec.model(document,ApprovalSubmission.class);
        record Resolved(Store.Reply reply,boolean created) {}
        var result=transaction(actor,(tx,observation)->{
            long now=now(tx,observation); var policy=currentAsk(tx,actor,request.input(),request.policyVersion(),now);
            var digest=binding(request.input(),request.policyVersion()); var stored=tx.approvalBinding(digest,now);
            ApprovalRecord approval;
            if (stored == null) {
                if (tx.activeApprovals(now) >= maxActive) throw Failure.unavailable();
                approval=new ApprovalRecord(UUID.randomUUID().toString(),1L,ApprovalState.PENDING,request.input(),request.policyVersion(),now,
                    Math.min(now+pendingTtl,policy.expiresAtUnixMs()),null,null);
                tx.saveApproval(row(approval)); audit(tx,actor,"APPROVAL_CREATE",approval,requestId);
            } else approval=expire(tx,actor,record(stored),now,requestId);
            String permitId=null; Long expires=null;
            var resultState=approval.state();
            if (approval.state() == ApprovalState.APPROVED_ONCE || approval.state() == ApprovalState.APPROVED_TEMPORARY) {
                if (tx.activePermits(now) >= maxPermits) throw Failure.unavailable();
                permitId=UUID.randomUUID().toString(); expires=Math.min(now+10000,Math.min(approval.expiresAtUnixMs(),policy.expiresAtUnixMs()));
                if (expires <= now) throw Failure.unavailable();
                if (approval.state() == ApprovalState.APPROVED_ONCE) {
                    approval=Approvals.spend(approval,now); tx.saveApproval(row(approval)); resultState=ApprovalState.CONSUMED;
                }
                tx.lease(new Store.PermitLease(permitId,approval.id(),digest,request.input().context().requestId(),now,expires,null));
                audit(tx,actor,"APPROVAL_SPEND",approval,requestId);
            }
            var resolution=new ApprovalResolution(approval.id(),resultState,request.input(),request.policyVersion(),permitId,expires);
            return new Resolved(new Store.Reply(200,codec.json(resolution),approval.revision()),stored==null);
        });
        var reply=result.reply();
        if (notification != null && result.created()) {
            var resolution=codec.model(reply.body(),ApprovalResolution.class);
            if (resolution.state() == ApprovalState.PENDING) {
                // Optional delivery never changes durable authorization and receives references only.
                try { notification.pending(actor.identity().tenant().value(),resolution.approvalId()); }
                catch (RuntimeException ignored) {
                    // Provider exceptions may contain recipient/token details; log only the fixed event.
                    LOG.log(System.Logger.Level.WARNING,"approval_notification_unavailable");
                }
            }
        }
        return reply;
    }
    public Store.Reply consume(Actor actor,String document,String requestId) {
        requireGateway(actor); Ids.valid(requestId); var request=codec.model(document,ApprovalPermitUse.class);
        return transaction(actor,(tx,observation)->{
            long now=now(tx,observation); currentAsk(tx,actor,request.input(),request.policyVersion(),now);
            var approval=expire(tx,actor,record(tx.approval(request.approvalId())),now,requestId);
            var lease=tx.permit(request.permitId()); var digest=binding(request.input(),request.policyVersion());
            if (lease == null || !lease.approvalId().equals(approval.id()) || !lease.bindingDigest().equals(digest)
                    || !lease.requestId().equals(request.input().context().requestId()) || lease.issuedAt() > now
                    || lease.expiresAt() <= now || lease.consumedAt() != null || approval.expiresAtUnixMs() <= now
                    || !(approval.state()==ApprovalState.CONSUMED || approval.state()==ApprovalState.APPROVED_TEMPORARY)) throw Failure.conflict();
            tx.consumePermit(lease.jti(),now);
            tx.audit(actor.identity().id(),"PERMIT_CONSUME","permit:"+lease.jti(),approval.revision(),requestId,digest);
            return new Store.Reply(200,codec.json(new ApprovalResolution(approval.id(),ApprovalState.CONSUMED,request.input(),request.policyVersion(),lease.jti(),lease.expiresAt())),approval.revision());
        });
    }
    private io.ololabs.toolgate.control.domain.Directory.Entry requireEnabled(io.ololabs.toolgate.control.domain.Directory directory,Ids.Kind kind,String id) {
        var entry=directory.entries().get(kind.id(id));
        if (entry == null || !entry.enabled()) throw new Failure(ErrorCode.FORBIDDEN,403,"Identity or tool is unavailable");
        return entry;
    }
    /** Trusted append-only publication bytes, independently compared to the runtime submission. */
    private ApprovalBundlePayload currentAsk(Store.Session tx,Actor actor,PolicyInput input,String version,long now) {
        if (!input.context().tenantId().equals(actor.identity().tenant().value())) throw new Failure(ErrorCode.FORBIDDEN,403,"Wrong tenant");
        var directory=tx.load(); requireEnabled(directory,Ids.Kind.USER,input.context().userId());
        var agent=codec.model(requireEnabled(directory,Ids.Kind.AGENT,input.context().agentId()).document(),ControlAgent.class);
        if (!agent.ownerUserId().equals(input.context().userId())) throw new Failure(ErrorCode.FORBIDDEN,403,"Agent owner mismatch");
        if (input.context().deviceId()!=null) {
            var device=codec.model(requireEnabled(directory,Ids.Kind.DEVICE,input.context().deviceId()).document(),ControlDevice.class);
            if (!device.ownerUserId().equals(input.context().userId())) throw new Failure(ErrorCode.FORBIDDEN,403,"Device owner mismatch");
        }
        requireEnabled(directory,Ids.Kind.TOOL,input.toolId());
        var bundle=tx.bundle(tx.bundleSequence()); if (bundle == null) throw Failure.unavailable();
        var signed=codec.model(bundle.document(),SignedPolicyBundle.class); var parts=signed.jws().split("\\.");
        ApprovalBundlePayload payload;
        try { payload=codec.model(new String(Base64.getUrlDecoder().decode(parts[1]),StandardCharsets.UTF_8),ApprovalBundlePayload.class); }
        catch (IllegalArgumentException | Failure e) { throw new Failure(ErrorCode.FORBIDDEN,403,"Current policy requires no approval"); }
        if (!payload.tenantId().equals(actor.identity().tenant().value()) || !payload.issuer().equals(issuer) || !payload.audience().equals(audience)
                || payload.sequence()!=bundle.sequence() || !payload.version().equals(version) || payload.issuedAtUnixMs()>now || payload.expiresAtUnixMs()<=now
                || !payload.policySha256().equals(DirectoryService.digest(bundle.policy()))) throw new Failure(ErrorCode.FORBIDDEN,403,"Policy no longer applicable");
        try {
            if (!java.util.Arrays.equals(Base64.getUrlDecoder().decode(payload.policy()),bundle.policy().getBytes(StandardCharsets.UTF_8))) throw Failure.unavailable();
        } catch (IllegalArgumentException e) { throw Failure.unavailable(); }
        var policy=codec.model(bundle.policy(),ApprovalCompiledPolicy.class); boolean ask=false;
        for (var rule:policy.rules()) {
            if (!rule.toolId().equals(input.toolId()) || !rule.action().equals(input.action()) || !rule.resource().equals(input.resource())
                    || !dimension(rule.userIds(),input.context().userId()) || !dimension(rule.agentIds(),input.context().agentId())
                    || !dimension(rule.deviceIds(),input.context().deviceId())) continue;
            if (rule.effect()==Decision.BLOCK) throw new Failure(ErrorCode.FORBIDDEN,403,"Policy blocks operation");
            if (rule.effect()==Decision.ASK) ask=true;
        }
        if (!ask) throw new Failure(ErrorCode.FORBIDDEN,403,"Policy requires no approval");
        return payload;
    }
    private boolean dimension(List<String> ids,String id) { return ids.isEmpty() || (id!=null && ids.contains(id)); }
}
