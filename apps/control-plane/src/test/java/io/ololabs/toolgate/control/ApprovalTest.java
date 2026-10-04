// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
package io.ololabs.toolgate.control;

import io.ololabs.toolgate.control.adapter.*;
import io.ololabs.toolgate.control.application.*;
import io.ololabs.toolgate.control.domain.*;
import io.ololabs.toolgate.contracts.*;
import org.junit.jupiter.api.Test;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

/** Real PostgreSQL transitions, HA races, exact permit binding and transaction failure proof. */
final class ApprovalTest {
    private final ContractCodec codec=new ContractCodec();
    private static final long START=1700000000000L;
    private static final class MutableClock extends Clock {
        long value=START;
        public ZoneId getZone() { return ZoneOffset.UTC; }
        public Clock withZone(ZoneId zone) { return this; }
        public Instant instant() { return Instant.ofEpochMilli(value); }
        public long millis() { return value; }
    }
    private PolicyInput input(String request,String digest) {
        return new PolicyInput(new RequestContext(request,"approval","requester","agent","device"),"tool","write",
            new ResourceDescriptor(ResourceKind.FILE,"/approved/file"),digest);
    }
    @Test void lifecycleIsExplicitAndTerminalDecisionsCannotBeReopened() {
        var pending=new ApprovalRecord("approval",1L,ApprovalState.PENDING,input("request","a".repeat(64)),"2.0.1",START,START+10000,null,null);
        assertEquals(ApprovalState.EXPIRED,Approvals.expire(pending,START+10000).state());
        for (var choice:ApprovalChoice.values()) {
            var decision=new ApprovalDecisionRequest(choice,1L,choice==ApprovalChoice.APPROVE_TEMPORARY?30000L:null);
            var decided=Approvals.decide(pending,decision,"approver",START,START+20000);
            assertEquals(2,decided.revision());
            assertThrows(IllegalStateException.class,()->Approvals.decide(decided,decision,"approver",START,START+20000));
            if (choice==ApprovalChoice.APPROVE_ONCE) {
                var spent=Approvals.spend(decided,START); assertEquals(ApprovalState.CONSUMED,spent.state());
                assertThrows(IllegalStateException.class,()->Approvals.spend(spent,START));
            } else assertThrows(IllegalStateException.class,()->Approvals.spend(decided,START));
            if (choice==ApprovalChoice.APPROVE_TEMPORARY) assertEquals(START+20000,decided.expiresAtUnixMs());
        }
        assertThrows(IllegalArgumentException.class,()->Approvals.decide(pending,new ApprovalDecisionRequest(ApprovalChoice.APPROVE_TEMPORARY,1L,null),"approver",START,START+30000));
        assertThrows(IllegalArgumentException.class,()->Approvals.decide(pending,new ApprovalDecisionRequest(ApprovalChoice.APPROVE_ONCE,1L,1000L),"approver",START,START+30000));
        assertThrows(IllegalStateException.class,()->Approvals.decide(pending,new ApprovalDecisionRequest(ApprovalChoice.DENY,2L,null),"approver",START,START+30000));
    }
    private record Setup(String url,String password,org.postgresql.ds.PGSimpleDataSource source,PostgresStore store,
                         DirectoryService directory,DirectoryService.Actor admin,ApprovalService.Actor gateway,
                         ApprovalService.Actor approver,MutableClock clock,BundleService bundles,ApprovalService approvals,
                         ControlPolicy policy,PolicyInput input) {}
    private Setup setup() throws Exception {
        var base=Objects.requireNonNull(System.getenv("CONTROL_TEST_URL"));var password=Objects.requireNonNull(System.getenv("CONTROL_TEST_PASSWORD"));
        var database="approval_"+UUID.randomUUID().toString().replace("-","");
        try(var connection=java.sql.DriverManager.getConnection(base,"control_migrator",password);var statement=connection.createStatement()) { statement.execute("CREATE DATABASE "+database); }
        var url=base.replace("/control?","/"+database+"?");
        var migration=org.flywaydb.core.Flyway.configure().dataSource(url,"control_migrator",password).target("3").load();migration.migrate();
        assertEquals(1,org.flywaydb.core.Flyway.configure().dataSource(url,"control_migrator",password).target("4").load().migrate().migrationsExecuted);
        org.flywaydb.core.Flyway.configure().dataSource(url,"control_migrator",password).load().migrate();
        var source=new org.postgresql.ds.PGSimpleDataSource();source.setURL(url);source.setUser("control_app");source.setPassword(password);
        var store=new PostgresStore(source,codec);var directory=new DirectoryService(store,codec,512,1048576);
        var admin=new DirectoryService.Actor(new Ids.TenantId("approval"),"a".repeat(64),true);
        var gateway=new ApprovalService.Actor(new DirectoryService.Actor(admin.tenant(),"b".repeat(64),false),null,false,true);
        var approver=new ApprovalService.Actor(new DirectoryService.Actor(admin.tenant(),"c".repeat(64),false),"approver",true,false);
        for(var id:List.of("requester","approver")) directory.mutate(admin,Ids.Kind.USER,id,"CREATE",DomainTest.user(id,1),0,id,"request");
        directory.mutate(admin,Ids.Kind.AGENT,"agent","CREATE",codec.json(new ControlAgent("agent","Agent",true,1L,"requester")),0,"agent","request");
        directory.mutate(admin,Ids.Kind.DEVICE,"device","CREATE",codec.json(new ControlDevice("device","Device",true,1L,"requester")),0,"device","request");
        var fixtures=new com.fasterxml.jackson.databind.ObjectMapper().readTree(Path.of(System.getProperty("toolgate.fixtures")).toFile());
        var definition=codec.model(codec.json(fixtures.get("ToolDefinition")),ToolDefinition.class);
        var tool=new ControlTool(definition.id(),"Tool",true,1L,definition);
        directory.mutate(admin,Ids.Kind.TOOL,tool.id(),"CREATE",codec.json(tool),0,"tool","request");
        var resource=new ResourceDescriptor(ResourceKind.CUSTOM,"sensitive-approval-resource");
        var policy=new ControlPolicy("ask","Ask",true,1L,tool.id(),definition.actions().getFirst().name(),resource,Decision.ASK,
            List.of("requester"),List.of(),List.of("agent"),List.of("device"));
        var clock=new MutableClock();var keyGenerator=java.security.KeyPairGenerator.getInstance("RSA");keyGenerator.initialize(2048);
        var signer=new RsaBundleSigner((java.security.interfaces.RSAPrivateCrtKey)keyGenerator.generateKeyPair().getPrivate(),"policy-key",codec);
        var bundles=new BundleService(store,codec,signer,"control","gateway",clock);
        var first=bundles.publish(admin,publish(5,0,null),"v1","request");assertEquals(1,first.revision());
        directory.mutate(admin,Ids.Kind.POLICY,policy.id(),"CREATE",codec.json(policy),0,"ask","request");
        bundles.publish(admin,publish(6,1,null),"v2","request");
        var input=new PolicyInput(new RequestContext("attempt","approval","requester","agent","device"),policy.toolId(),policy.action(),policy.resource(),"a".repeat(64));
        var approvals=new ApprovalService(store,codec,clock,true,6000,16,32,"control","gateway",null);
        return new Setup(url,password,source,store,directory,admin,gateway,approver,clock,bundles,approvals,policy,input);
    }
    private String publish(long revision,long sequence,Long rollback) { return codec.json(new BundlePublishRequest(revision,sequence,60000L,0L,null,rollback)); }
    private ApprovalResolution resolve(Setup s,PolicyInput input,String version) {
        return codec.model(s.approvals.resolve(s.gateway,codec.json(new ApprovalSubmission(input,version)),"request").body(),ApprovalResolution.class);
    }
    private PolicyInput attempt(PolicyInput input,String request,String digest) {
        var c=input.context();return new PolicyInput(new RequestContext(request,c.tenantId(),c.userId(),c.agentId(),c.deviceId()),input.toolId(),input.action(),input.resource(),digest);
    }
    private String decision(ApprovalChoice choice,long revision,Long duration) { return codec.json(new ApprovalDecisionRequest(choice,revision,duration)); }
    @Test void onceDecisionsAndConsumptionAreAtomicAcrossReplicas() throws Exception {
        var s=setup();var pending=resolve(s,s.input,"2.0.2");assertEquals(ApprovalState.PENDING,pending.state());
        assertEquals(pending.approvalId(),resolve(s,attempt(s.input,"different-request","a".repeat(64)),"2.0.2").approvalId());
        var self=new ApprovalService.Actor(s.approver.identity(),"requester",true,false);
        assertEquals(403,assertThrows(Failure.class,()->s.approvals.decide(self,pending.approvalId(),decision(ApprovalChoice.APPROVE_ONCE,1,null),"self","request")).status());
        var wrongRole=new ApprovalService.Actor(s.approver.identity(),"approver",false,false);
        assertEquals(403,assertThrows(Failure.class,()->s.approvals.get(wrongRole,pending.approvalId(),"request")).status());
        var unknown=new ApprovalService.Actor(s.approver.identity(),"missing",true,false);
        assertEquals(403,assertThrows(Failure.class,()->s.approvals.get(unknown,pending.approvalId(),"request")).status());
        var combined=new ApprovalService.Actor(s.gateway.identity(),"approver",true,true);
        assertEquals(403,assertThrows(Failure.class,()->s.approvals.get(combined,pending.approvalId(),"request")).status());
        assertEquals(403,assertThrows(Failure.class,()->s.approvals.decide(combined,pending.approvalId(),decision(ApprovalChoice.APPROVE_ONCE,1,null),"combined","request")).status());
        assertEquals(403,assertThrows(Failure.class,()->s.approvals.resolve(combined,codec.json(new ApprovalSubmission(s.input,"2.0.2")),"request")).status());
        assertEquals(403,assertThrows(Failure.class,()->s.approvals.consume(combined,codec.json(new ApprovalPermitUse(pending.approvalId(),"permit",s.input,"2.0.2")),"request")).status());
        assertEquals(403,assertThrows(Failure.class,()->s.approvals.resolve(s.approver,codec.json(new ApprovalSubmission(s.input,"2.0.2")),"request")).status());
        assertEquals(403,assertThrows(Failure.class,()->s.approvals.resolve(s.gateway,codec.json(new ApprovalSubmission(s.input,"2.0.1")),"request")).status());
        // Two contenders exercise the required double-decision/consume race without
        // turning the production three-second lock deadline into a throughput test.
        var pool=Executors.newFixedThreadPool(2);
        try {
            var results=new ArrayList<Future<Boolean>>();
            var decisionBarrier=new java.util.concurrent.CyclicBarrier(2);
            for(int i=0;i<2;i++) {final int n=i;results.add(pool.submit(()->{
                decisionBarrier.await();
                try {s.approvals.decide(s.approver,pending.approvalId(),decision(ApprovalChoice.APPROVE_ONCE,1,null),"decision-"+n,"request");return true;}
                catch(Failure conflict) {assertEquals(409,conflict.status());return false;}
            }));}
            int winners=0;for(var result:results) if(result.get()) winners++;assertEquals(1,winners);
            var attempts=new ArrayList<Future<ApprovalResolution>>();
            var resolveBarrier=new java.util.concurrent.CyclicBarrier(2);
            for(int i=0;i<2;i++) {final int n=i;attempts.add(pool.submit(()->{resolveBarrier.await();return resolve(s,attempt(s.input,"attempt-"+n,"a".repeat(64)),"2.0.2");}));}
            var permits=new ArrayList<ApprovalResolution>();for(var result:attempts) {var r=result.get();if(r.permitId()!=null) permits.add(r);}
            assertEquals(1,permits.size());var permit=permits.getFirst();assertEquals(ApprovalState.CONSUMED,permit.state());
            assertTrue(permit.permitExpiresAtUnixMs()<=START+6000);
            var altered=attempt(permit.input(),permit.input().context().requestId(),"b".repeat(64));
            assertEquals(409,assertThrows(Failure.class,()->s.approvals.consume(s.gateway,codec.json(new ApprovalPermitUse(permit.approvalId(),permit.permitId(),altered,"2.0.2")),"request")).status());
            var differentRequest=attempt(permit.input(),"wrong-request","a".repeat(64));
            assertEquals(409,assertThrows(Failure.class,()->s.approvals.consume(s.gateway,codec.json(new ApprovalPermitUse(permit.approvalId(),permit.permitId(),differentRequest,"2.0.2")),"request")).status());
            var c=permit.input().context();
            var wrongInputs=List.of(
                new PolicyInput(new RequestContext(c.requestId(),"other",c.userId(),c.agentId(),c.deviceId()),s.input.toolId(),s.input.action(),s.input.resource(),s.input.argumentsDigest()),
                new PolicyInput(new RequestContext(c.requestId(),c.tenantId(),"approver",c.agentId(),c.deviceId()),s.input.toolId(),s.input.action(),s.input.resource(),s.input.argumentsDigest()),
                new PolicyInput(new RequestContext(c.requestId(),c.tenantId(),c.userId(),"wrong-agent",c.deviceId()),s.input.toolId(),s.input.action(),s.input.resource(),s.input.argumentsDigest()),
                new PolicyInput(new RequestContext(c.requestId(),c.tenantId(),c.userId(),c.agentId(),null),s.input.toolId(),s.input.action(),s.input.resource(),s.input.argumentsDigest()),
                new PolicyInput(c,"wrong-tool",s.input.action(),s.input.resource(),s.input.argumentsDigest()),
                new PolicyInput(c,s.input.toolId(),"wrong-action",s.input.resource(),s.input.argumentsDigest()),
                new PolicyInput(c,s.input.toolId(),s.input.action(),new ResourceDescriptor(s.input.resource().kind(),"wrong-resource"),s.input.argumentsDigest()));
            for(var wrong:wrongInputs) assertThrows(Failure.class,()->s.approvals.consume(s.gateway,codec.json(new ApprovalPermitUse(permit.approvalId(),permit.permitId(),wrong,"2.0.2")),"request"));
            var consume=codec.json(new ApprovalPermitUse(permit.approvalId(),permit.permitId(),permit.input(),"2.0.2"));
            var consumeBarrier=new java.util.concurrent.CyclicBarrier(2);
            results.clear();for(int i=0;i<2;i++) results.add(pool.submit(()->{
                consumeBarrier.await();
                try {assertEquals(200,s.approvals.consume(s.gateway,consume,"request").status());return true;}
                catch(Failure conflict) {assertEquals(409,conflict.status());return false;}
            }));
            winners=0;for(var result:results) if(result.get()) winners++;assertEquals(1,winners);
        } finally {pool.shutdownNow();}
        var audits=codec.json(codec.value(s.directory.audit(s.admin,0,100).body()));
        assertEquals(1,count(s,"APPROVAL_CREATE"));assertEquals(1,count(s,"APPROVAL_DECIDE"));assertEquals(1,count(s,"APPROVAL_SPEND"));assertEquals(1,count(s,"PERMIT_CONSUME"));
        assertFalse(audits.contains(s.input.resource().locator()));
        assertEquals(1,codec.model(s.approvals.page(s.approver,null,1,"request").body(),ApprovalPage.class).items().size());
        assertThrows(Failure.class,()->s.approvals.page(s.approver,null,101,"request"));
    }
    @Test void activeStateLimitsAndOptionalNotificationCannotBroadenApproval() throws Exception {
        var s=setup();var deliveries=new java.util.concurrent.atomic.AtomicInteger();
        var bounded=new ApprovalService(s.store,codec,s.clock,true,6000,1,1,"control","gateway",(tenant,id)->{
            assertEquals("approval",tenant);assertFalse(id.isBlank());deliveries.incrementAndGet();throw new IllegalStateException("Notification unavailable");
        });
        var submission=codec.json(new ApprovalSubmission(s.input,"2.0.2"));
        var pending=codec.model(bounded.resolve(s.gateway,submission,"request").body(),ApprovalResolution.class);
        assertEquals(ApprovalState.PENDING,pending.state());assertEquals(1,deliveries.get());
        bounded.resolve(s.gateway,submission,"request");assertEquals(1,deliveries.get());
        var second=codec.json(new ApprovalSubmission(attempt(s.input,"second","b".repeat(64)),"2.0.2"));
        assertEquals(503,assertThrows(Failure.class,()->bounded.resolve(s.gateway,second,"request")).status());
        bounded.decide(s.approver,pending.approvalId(),decision(ApprovalChoice.APPROVE_TEMPORARY,1,20000L),"approve","request");
        assertNotNull(codec.model(bounded.resolve(s.gateway,submission,"request").body(),ApprovalResolution.class).permitId());
        assertEquals(503,assertThrows(Failure.class,()->bounded.resolve(s.gateway,submission,"request")).status());
        var disabledUser=new ControlUser("approver","Approver",false,1L,null);
        s.directory.mutate(s.admin,Ids.Kind.USER,"approver","UPDATE",codec.json(disabledUser),1,"disable-approver","request");
        assertEquals(403,assertThrows(Failure.class,()->bounded.get(s.approver,pending.approvalId(),"request")).status());
        var other=new ApprovalService.Actor(new DirectoryService.Actor(new Ids.TenantId("other"),"d".repeat(64),false),"approver",true,false);
        assertEquals(403,assertThrows(Failure.class,()->bounded.get(other,pending.approvalId(),"request")).status());
    }
    @Test void temporaryDenyExpiryVersionChangesAndClockRegressionFailClosed() throws Exception {
        var s=setup();var pending=resolve(s,s.input,"2.0.2");
        var decided=s.approvals.decide(s.approver,pending.approvalId(),decision(ApprovalChoice.APPROVE_TEMPORARY,1,20000L),"temporary","request");
        assertEquals(decided,s.approvals.decide(s.approver,pending.approvalId(),decision(ApprovalChoice.APPROVE_TEMPORARY,1,20000L),"temporary","request"));
        assertThrows(Failure.class,()->s.approvals.decide(s.approver,pending.approvalId(),decision(ApprovalChoice.DENY,1,null),"temporary","request"));
        var first=resolve(s,s.input,"2.0.2");var second=resolve(s,attempt(s.input,"second","a".repeat(64)),"2.0.2");
        assertNotEquals(first.permitId(),second.permitId());assertEquals(first.approvalId(),second.approvalId());
        var deniedInput=attempt(s.input,"deny","b".repeat(64));var denied=resolve(s,deniedInput,"2.0.2");
        s.approvals.decide(s.approver,denied.approvalId(),decision(ApprovalChoice.DENY,1,null),"deny","request");
        var tombstone=resolve(s,attempt(deniedInput,"new-request","b".repeat(64)),"2.0.2");
        assertEquals(denied.approvalId(),tombstone.approvalId());assertEquals(ApprovalState.DENIED,tombstone.state());assertNull(tombstone.permitId());
        var waiting=resolve(s,attempt(s.input,"waiting","c".repeat(64)),"2.0.2");
        s.clock.value=START+6000;
        assertEquals(ApprovalState.EXPIRED,codec.model(s.approvals.get(s.approver,waiting.approvalId(),"request").body(),ApprovalRecord.class).state());
        assertEquals(1,count(s,"APPROVAL_EXPIRE"));
        s.clock.value=START+10001;
        assertThrows(Failure.class,()->s.approvals.consume(s.gateway,codec.json(new ApprovalPermitUse(first.approvalId(),first.permitId(),first.input(),"2.0.2")),"request"));
        s.clock.value=START+5000;
        assertEquals(503,assertThrows(Failure.class,()->s.approvals.consume(s.gateway,codec.json(new ApprovalPermitUse(first.approvalId(),first.permitId(),first.input(),"2.0.2")),"request")).status());
        assertEquals(0,count(s,"PERMIT_CONSUME"));
        s.clock.value=START+20000;
        assertEquals(ApprovalState.EXPIRED,codec.model(s.approvals.get(s.approver,pending.approvalId(),"request").body(),ApprovalRecord.class).state());
        s.clock.value=START;
        assertEquals(503,assertThrows(Failure.class,()->resolve(s,s.input,"2.0.2")).status());
        s.clock.value=START+20001;
        s.bundles.publish(s.admin,publish(6,2,null),"new-policy","request");
        assertEquals(403,assertThrows(Failure.class,()->resolve(s,s.input,"2.0.2")).status());
        assertEquals(ApprovalState.PENDING,resolve(s,s.input,"2.0.3").state());
        // Forward rollback to the genuine v1 history cannot preserve an obsolete ASK grant.
        var rolled=s.bundles.publish(s.admin,publish(6,3,1L),"rollback-v1","request");
        var jws=codec.model(rolled.body(),SignedPolicyBundle.class).jws().split("\\.");
        var payload=codec.model(new String(Base64.getUrlDecoder().decode(jws[1]),StandardCharsets.UTF_8),BundlePayload.class);
        assertEquals("1.0.4",payload.version());assertEquals(1,payload.rollbackOf());
        assertEquals(403,assertThrows(Failure.class,()->resolve(s,s.input,"2.0.3")).status());
        var disabled=new ApprovalService(s.store,codec,s.clock,false,6000,16,32,"control","gateway",null);
        assertEquals(503,assertThrows(Failure.class,()->disabled.resolve(s.gateway,codec.json(new ApprovalSubmission(s.input,"1.0.4")),"request")).status());
    }
    @Test void auditFailuresRollBackSpendingAndExplicitBlockWins() throws Exception {
        var s=setup();var pending=resolve(s,s.input,"2.0.2");
        s.approvals.decide(s.approver,pending.approvalId(),decision(ApprovalChoice.APPROVE_ONCE,1,null),"approve","request");
        sql(s,"ALTER TABLE control_audit ADD CONSTRAINT fail_spend CHECK (operation <> 'APPROVAL_SPEND')");
        s.clock.value=START+1000;
        assertThrows(Failure.class,()->resolve(s,s.input,"2.0.2"));
        s.clock.value=START+500;
        assertEquals(503,assertThrows(Failure.class,()->resolve(s,s.input,"2.0.2")).status());
        s.clock.value=START+1000;
        assertEquals(ApprovalState.APPROVED_ONCE,codec.model(s.approvals.get(s.approver,pending.approvalId(),"request").body(),ApprovalRecord.class).state());
        sql(s,"ALTER TABLE control_audit DROP CONSTRAINT fail_spend");
        var permit=resolve(s,s.input,"2.0.2");var use=codec.json(new ApprovalPermitUse(permit.approvalId(),permit.permitId(),permit.input(),"2.0.2"));
        sql(s,"ALTER TABLE control_audit ADD CONSTRAINT fail_consume CHECK (operation <> 'PERMIT_CONSUME')");
        s.clock.value=START+2000;
        assertThrows(Failure.class,()->s.approvals.consume(s.gateway,use,"request"));
        s.clock.value=START+1500;
        assertEquals(503,assertThrows(Failure.class,()->s.approvals.consume(s.gateway,use,"request")).status());
        s.clock.value=START+2000;
        sql(s,"ALTER TABLE control_audit DROP CONSTRAINT fail_consume");assertEquals(200,s.approvals.consume(s.gateway,use,"request").status());
        var p=s.policy;var block=new ControlPolicy("block","Block",true,1L,p.toolId(),p.action(),p.resource(),Decision.BLOCK,p.userIds(),p.teamIds(),p.agentIds(),p.deviceIds());
        s.directory.mutate(s.admin,Ids.Kind.POLICY,"block","CREATE",codec.json(block),0,"block","request");
        s.bundles.publish(s.admin,publish(7,2,null),"block-policy","request");
        assertEquals(403,assertThrows(Failure.class,()->resolve(s,s.input,"2.0.3")).status());
        try(var connection=s.source.getConnection();var statement=connection.createStatement()) {
            assertThrows(java.sql.SQLException.class,()->statement.executeUpdate("DELETE FROM control_approvals"));
            assertThrows(java.sql.SQLException.class,()->statement.executeUpdate("DELETE FROM control_permit_leases"));
        }
    }
    private long count(Setup s,String operation) throws Exception {
        try(var connection=s.source.getConnection();var statement=connection.prepareStatement("SELECT count(*) FROM control_audit WHERE operation=?")) {
            statement.setString(1,operation);try(var rows=statement.executeQuery()) {rows.next();return rows.getLong(1);}
        }
    }
    private void sql(Setup s,String query) throws Exception {
        try(var connection=java.sql.DriverManager.getConnection(s.url,"control_migrator",s.password);var statement=connection.createStatement()) {statement.execute(query);}
    }
}
