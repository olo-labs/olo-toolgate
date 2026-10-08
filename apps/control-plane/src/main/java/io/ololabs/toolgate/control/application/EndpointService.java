// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
package io.ololabs.toolgate.control.application;

import io.ololabs.toolgate.control.domain.*;
import io.ololabs.toolgate.contracts.*;
import java.time.Clock;
import java.util.*;

/** Enrollment and check-in use cases; all authoritative mutable state is transactional. */
public final class EndpointService {
    private static final String CONTRACT_VERSION=contractVersion();
    private static String contractVersion() {
        try(var input=ContractSet.class.getResourceAsStream("/META-INF/toolgate-contracts.version")) {
            if(input==null)throw new IllegalStateException("Shared contract version required");
            String version=new String(input.readNBytes(129),java.nio.charset.StandardCharsets.US_ASCII).trim();
            if(version.length()>128 || !version.matches("[0-9]+\\.[0-9]+\\.[0-9]+(?:-[0-9A-Za-z.-]+)?"))throw new IllegalStateException("Shared contract version required");
            return version;
        }catch(java.io.IOException failure){throw new IllegalStateException("Shared contract version required");}
    }
    private final Store store;
    private final Codec codec;
    private final DeviceIssuer issuer;
    private final Clock clock;
    private final Ids.TenantId tenant;
    private final String server,organization,control,gateway;
    private final boolean enabled;
    public McpService relay(){available();return new McpService(store,codec,this,clock,tenant,server);}
    public EndpointService(Store store,Codec codec,DeviceIssuer issuer,Clock clock,boolean enabled,
                           String tenant,String server,String organization,String control,String gateway) {
        this.store=store;this.codec=codec;this.issuer=issuer;this.clock=clock;this.enabled=enabled;
        this.tenant=new Ids.TenantId(tenant);this.server=Ids.valid(server);this.organization=organization;
        this.control=origin(control);this.gateway=origin(gateway);
    }
    /** Fixed HTTPS origins exclude redirects, userinfo, paths and fragments. */
    public static String origin(String value) {
        try {var uri=java.net.URI.create(value);
            if(!"https".equals(uri.getScheme()) || uri.getHost()==null || uri.getRawUserInfo()!=null || uri.getRawQuery()!=null
                || uri.getRawFragment()!=null || !(uri.getPath().isEmpty() || uri.getPath().equals("/"))) throw new IllegalArgumentException();
            return "https://"+uri.getRawAuthority();
        }catch(IllegalArgumentException e){throw new IllegalArgumentException("HTTPS server origin required");}
    }
    private void available(){if(!enabled)throw Failure.unavailable();}
    private static final class Observation {long time=-1;}
    private <T> T transaction(java.util.function.BiFunction<Store.Session,Long,T> action) {
        available();var observation=new Observation();
        try {return store.transaction(tenant,true,tx->{long now=clock.millis();if(now<0||now>9007199254740991L)throw Failure.unavailable();
            observation.time=Math.max(now,tx.approvalClock(now));if(observation.time>now)throw Failure.unavailable();return action.apply(tx,now);});}
        catch(RuntimeException failure){if(observation.time>=0)store.transaction(tenant,true,tx->{tx.approvalClock(observation.time);return null;});throw failure;}
    }
    private Directory.Entry enabledUser(Store.Session tx,String user) {
        var entry=tx.load().entries().get(Ids.Kind.USER.id(user));if(entry==null||!entry.enabled())throw forbidden();return entry;
    }
    private static Failure forbidden(){return new Failure(ErrorCode.FORBIDDEN,403,"Device identity unavailable");}
    private void human(DirectoryService.Actor actor,String user,Store.Session tx) {
        if(!actor.tenant().equals(tenant)||user==null)throw forbidden();Ids.valid(user);enabledUser(tx,user);
    }
    private Store.Reply reply(Object body,long revision){return new Store.Reply(200,codec.json(body),revision);}
    private static String random(int bytes){byte[] value=new byte[bytes];new java.security.SecureRandom().nextBytes(value);return HexFormat.of().formatHex(value);}
    private String verification(){return control+"/console/#enroll";}
    public Store.Reply discovery(){available();long now=clock.millis();
        var manifest=new ClientDiscovery(1L,server,organization,tenant.value(),control,gateway,verification(),CONTRACT_VERSION,now,now+300000,issuer.issuerCertificate());
        return reply(issuer.discovery(codec.json(manifest)),0);
    }
    public Store.Reply start(String body,String requestId) {
        available();
        var start=codec.model(body,EndpointEnrollmentStart.class);var fingerprint=issuer.fingerprint(start.csrPem());Ids.valid(requestId);
        return transaction((tx,now)->{
            tx.pruneEnrollments(now);if(tx.pendingEnrollments(now)>=32 || tx.endpoint(start.deviceId())!=null || tx.endpointKey(fingerprint)!=null)throw Failure.conflict();
            if(tx.load().entries().values().stream().noneMatch(e->e.id().kind()==Ids.Kind.USER&&e.enabled()))throw forbidden();
            String id=random(16),deviceCode=random(32),userCode=random(8).toUpperCase(Locale.ROOT);long expires=now+600000;
            var review=new EndpointEnrollmentReview(id,userCode,start.deviceId(),start.platform(),fingerprint,EnrollmentState.PENDING,expires);
            tx.saveEnrollment(new Store.EnrollmentRecord(id,DirectoryService.digest(userCode),DirectoryService.digest(deviceCode),codec.json(review),start.csrPem(),null,null,expires,0));
            tx.audit(fingerprint,"ENROLLMENT_CREATE","endpoint:"+start.deviceId(),1,requestId,DirectoryService.digest(codec.json(start)));
            return reply(new EndpointEnrollmentChallenge(id,deviceCode,userCode,verification(),expires,5L),1);
        });
    }
    public Store.Reply review(DirectoryService.Actor actor,String user,String code) {
        if(code==null||!code.matches("[A-F0-9]{16}"))throw Failure.validation();
        return transaction((tx,now)->{human(actor,user,tx);var row=tx.enrollmentCode(DirectoryService.digest(code));
            if(row==null)throw new Failure(ErrorCode.NOT_FOUND,404,"Enrollment not found");
            var review=codec.model(row.document(),EndpointEnrollmentReview.class);
            if(row.expiresAt()<=now)return reply(withState(review,EnrollmentState.EXPIRED),1);
            return reply(review,1);
        });
    }
    private EndpointEnrollmentReview withState(EndpointEnrollmentReview r,EnrollmentState state){return new EndpointEnrollmentReview(r.enrollmentId(),r.userCode(),r.deviceId(),r.platform(),r.keyFingerprint(),state,r.expiresAtUnixMs());}
    public Store.Reply decide(DirectoryService.Actor actor,String user,String body,String key,String requestId) {
        var decision=codec.model(body,EndpointEnrollmentDecision.class);Ids.valid(key);Ids.valid(requestId);var digest=DirectoryService.digest(body);
        return transaction((tx,now)->{
            human(actor,user,tx);var replay=tx.replay(actor.id(),key,digest);if(replay!=null)return replay;
            var row=tx.enrollmentCode(DirectoryService.digest(decision.userCode()));if(row==null||row.expiresAt()<=now)throw Failure.conflict();
            var review=codec.model(row.document(),EndpointEnrollmentReview.class);
            if(review.state()!=EnrollmentState.PENDING||!review.keyFingerprint().equals(decision.keyFingerprint()))throw Failure.conflict();
            boolean approve=decision.choice()==EnrollmentChoice.APPROVE;
            var state=approve?EnrollmentState.APPROVED:EnrollmentState.DENIED;String certificate=null;
            if(approve){
                if(tx.endpoint(review.deviceId())!=null||tx.endpointKey(review.keyFingerprint())!=null)throw Failure.conflict();
                var identity=issuer.issue(row.csr(),review.deviceId(),tenant.value(),user,server,now);certificate=codec.json(identity);
                var endpoint=new EndpointDeviceRecord(review.deviceId(),tenant.value(),user,review.keyFingerprint(),EndpointState.ACTIVE,1L,0L,0L,null);
                tx.saveEndpoint(new Store.EndpointRecord(review.deviceId(),review.keyFingerprint(),codec.json(endpoint),row.csr(),"0".repeat(64),"{}"));
                // Directory metadata cannot grant device credentials; enrollment creates the matching bounded record atomically.
                var before=tx.load();var id=Ids.Kind.DEVICE.id(review.deviceId());if(tx.used(id)||before.entries().containsKey(id))throw Failure.conflict();
                var entry=codec.entry(Ids.Kind.DEVICE,codec.json(new ControlDevice(review.deviceId(),"Enrolled "+review.platform(),true,1L,user)));
                var entries=new HashMap<>(before.entries());entries.put(id,entry);var after=new Directory(before.revision()+1,entries);after.validate(512,1048576);tx.save(before,after);
            }
            var updated=withState(review,state);tx.saveEnrollment(new Store.EnrollmentRecord(row.id(),row.codeDigest(),row.deviceDigest(),codec.json(updated),row.csr(),approve?user:null,certificate,row.expiresAt(),row.lastPoll()));
            tx.audit(actor.id(),approve?"ENROLLMENT_APPROVE":"ENROLLMENT_DENY","endpoint:"+review.deviceId(),1,requestId,digest);
            var result=reply(updated,1);tx.remember(actor.id(),key,digest,result);return result;
        });
    }
    public Store.Reply poll(String body,String requestId) {
        var poll=codec.model(body,EndpointEnrollmentPoll.class);Ids.valid(requestId);
        return transaction((tx,now)->{var row=tx.enrollment(poll.enrollmentId());
            if(row==null||!java.security.MessageDigest.isEqual(row.deviceDigest().getBytes(java.nio.charset.StandardCharsets.US_ASCII),DirectoryService.digest(poll.deviceCode()).getBytes(java.nio.charset.StandardCharsets.US_ASCII)))throw forbidden();
            if(row.expiresAt()<=now)return reply(new EndpointEnrollmentResult(EnrollmentState.EXPIRED,null),1);
            if(row.lastPoll()!=0 && now-row.lastPoll()<5000)throw Failure.conflict();
            var review=codec.model(row.document(),EndpointEnrollmentReview.class);var state=review.state();DeviceIdentity identity=null;
            if(state==EnrollmentState.APPROVED || state==EnrollmentState.CONSUMED){
                var device=tx.endpoint(review.deviceId());if(device==null||codec.model(device.document(),EndpointDeviceRecord.class).state()!=EndpointState.ACTIVE)throw forbidden();
                active(tx,codec.model(device.document(),EndpointDeviceRecord.class));identity=codec.model(row.certificate(),DeviceIdentity.class);
                if(identity.expiresAtUnixMs()<=now)throw forbidden();
                // Exact code may recover a lost response until challenge expiry; it cannot issue another identity.
                if(state==EnrollmentState.APPROVED)tx.audit(review.keyFingerprint(),"ENROLLMENT_CONSUME","endpoint:"+review.deviceId(),1,requestId,DirectoryService.digest(row.id()));
                state=EnrollmentState.CONSUMED;
            }
            tx.saveEnrollment(new Store.EnrollmentRecord(row.id(),row.codeDigest(),row.deviceDigest(),codec.json(withState(review,state)),row.csr(),row.userId(),row.certificate(),row.expiresAt(),now));
            return reply(new EndpointEnrollmentResult(state,identity),1);
        });
    }
    public Store.Reply checkIn(java.security.cert.X509Certificate peer,String body,String requestId) {
        return checkIn(peer,body,requestId,false);
    }
    public String issuerCertificate() { available();return issuer.issuerCertificate(); }
    public void verifySocketPeer(java.security.cert.X509Certificate peer) {
        transaction((tx,now)->{authenticate(tx,peer,now);return null;});
    }
    private Store.Reply interval(Store.Reply reply,boolean milliseconds) {
        var ack=codec.model(reply.body(),EndpointCheckInAck.class);
        var negotiated=new EndpointCheckInAck(ack.deviceId(),ack.sequence(),ack.serverTimeUnixMs(),ack.nextIntervalSeconds(),milliseconds?500L:null,ack.identity(),ack.configuration(),ack.task());
        return new Store.Reply(reply.status(),codec.json(negotiated),reply.revision());
    }
    public Store.Reply checkIn(java.security.cert.X509Certificate peer,String body,String requestId,boolean milliseconds) {
        available();
        var check=codec.model(body,EndpointCheckIn.class);Ids.valid(requestId);var fingerprint=issuer.peerFingerprint(peer,clock.millis());
        var digest=DirectoryService.digest(codec.json(check));
        return transaction((tx,now)->{var row=tx.endpointKey(fingerprint);if(row==null)throw forbidden();
            var device=codec.model(row.document(),EndpointDeviceRecord.class);active(tx,device);
            if(!device.deviceId().equals(check.report().deviceId()))throw forbidden();
            if(check.report().appliedRevision()!=0||!check.report().packages().isEmpty())FleetService.validateReport(tx,codec,check.report());
            if(check.sequence().equals(device.reportSequence())){if(!digest.equals(row.reportDigest()))throw Failure.conflict();return interval(new Store.Reply(200,row.acknowledgment(),device.revision()),milliseconds);}
            if(check.sequence()!=device.reportSequence()+1)throw Failure.conflict();
            // Allow the 500 ms cycle with transport jitter; reject request bursts.
            if(device.reportSequence()>0 && now-device.lastSeenUnixMs()<250)throw Failure.conflict();
            DeviceIdentity renewed=null;if(peer.getNotAfter().getTime()-now<43200000)renewed=issuer.issue(row.csr(),device.deviceId(),tenant.value(),device.userId(),server,now);
            var configuration=new EndpointPermissions(codec).poll(tx,device,server,check.configurationDigest(),check.localTools());
            var task=relay().poll(tx,device,now,requestId);
            var ack=new EndpointCheckInAck(device.deviceId(),check.sequence(),now,2L,null,renewed,configuration,task);
            var updated=new EndpointDeviceRecord(device.deviceId(),tenant.value(),device.userId(),fingerprint,EndpointState.ACTIVE,device.revision()+1,now,check.sequence(),check.report());
            var response=reply(ack,updated.revision());tx.saveEndpoint(new Store.EndpointRecord(row.id(),row.fingerprint(),codec.json(updated),row.csr(),digest,response.body()));
            tx.audit(fingerprint,renewed==null?"DEVICE_CHECK_IN":"DEVICE_RENEW","endpoint:"+row.id(),updated.revision(),requestId,digest);return interval(response,milliseconds);
        });
    }
    public EndpointDeviceRecord authenticate(Store.Session tx,java.security.cert.X509Certificate peer,long now){
        available();var fingerprint=issuer.peerFingerprint(peer,now);var row=tx.endpointKey(fingerprint);
        if(row==null)throw forbidden();var device=codec.model(row.document(),EndpointDeviceRecord.class);active(tx,device);return device;
    }
    public void active(Store.Session tx,EndpointDeviceRecord device){
        if(device.state()!=EndpointState.ACTIVE)throw forbidden();enabledUser(tx,device.userId());
        var entry=tx.load().entries().get(Ids.Kind.DEVICE.id(device.deviceId()));
        if(entry==null||!entry.enabled()||!device.userId().equals(codec.model(entry.document(),ControlDevice.class).ownerUserId()))throw forbidden();
    }
    public Store.Reply device(DirectoryService.Actor actor,String id) {
        actor.requireAdmin();if(!actor.tenant().equals(tenant))throw forbidden();return transaction((tx,now)->{var row=tx.endpoint(Ids.valid(id));if(row==null)throw new Failure(ErrorCode.NOT_FOUND,404,"Device not found");return reply(codec.value(row.document()),0);});
    }
    public Store.Reply revoke(DirectoryService.Actor actor,String id,String body,String key,String requestId) {
        actor.requireAdmin();if(!actor.tenant().equals(tenant))throw forbidden();Ids.valid(id);Ids.valid(key);Ids.valid(requestId);
        var request=codec.model(body,EndpointRevokeRequest.class);var digest=DirectoryService.digest(id+"\n"+body);
        return transaction((tx,now)->{var replay=tx.replay(actor.id(),key,digest);if(replay!=null)return replay;
            var row=tx.endpoint(id);if(row==null)throw Failure.conflict();var device=codec.model(row.document(),EndpointDeviceRecord.class);
            if(!request.expectedRevision().equals(device.revision())||device.state()==EndpointState.REVOKED)throw Failure.conflict();
            var updated=new EndpointDeviceRecord(id,tenant.value(),device.userId(),row.fingerprint(),EndpointState.REVOKED,device.revision()+1,device.lastSeenUnixMs(),device.reportSequence(),device.report());
            tx.saveEndpoint(new Store.EndpointRecord(id,row.fingerprint(),codec.json(updated),row.csr(),row.reportDigest(),row.acknowledgment()));
            tx.audit(actor.id(),"DEVICE_REVOKE","endpoint:"+id,updated.revision(),requestId,digest);var result=reply(updated,updated.revision());tx.remember(actor.id(),key,digest,result);return result;
        });
    }
}
