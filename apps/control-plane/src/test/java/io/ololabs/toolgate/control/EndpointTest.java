// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
package io.ololabs.toolgate.control;

import io.ololabs.toolgate.control.adapter.*;
import io.ololabs.toolgate.control.application.*;
import io.ololabs.toolgate.control.domain.*;
import io.ololabs.toolgate.contracts.*;
import java.time.*;
import java.util.*;
import java.security.*;
import java.io.*;
import org.junit.jupiter.api.Test;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.*;
import org.bouncycastle.cert.jcajce.*;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.bouncycastle.pkcs.jcajce.JcaPKCS10CertificationRequestBuilder;
import static org.junit.jupiter.api.Assertions.*;

/** Real issuer/PostgreSQL enrollment, migration, replay, revocation, renewal and race proofs. */
final class EndpointTest {
    private final ContractCodec codec=new ContractCodec();
    private static final long START=1700000000000L;
    private static final class MutableClock extends Clock{
        long time=START;public ZoneId getZone(){return ZoneOffset.UTC;}public Clock withZone(ZoneId zone){return this;}public Instant instant(){return Instant.ofEpochMilli(time);}public long millis(){return time;}
    }
    private static String pem(Object value)throws IOException{var result=new StringWriter();try(var writer=new JcaPEMWriter(result)){writer.writeObject(value);}return result.toString();}
    private static X509DeviceIssuer issuer()throws Exception{
        var generator=KeyPairGenerator.getInstance("RSA");generator.initialize(3072);var pair=generator.generateKeyPair();var subject=new X500Name("O=ToolGate Tests,CN=Test Device Identity CA");
        var builder=new JcaX509v3CertificateBuilder(subject,java.math.BigInteger.ONE,new Date(START-1000),new Date(START+864000000),subject,pair.getPublic());
        builder.addExtension(Extension.basicConstraints,true,new BasicConstraints(0));builder.addExtension(Extension.keyUsage,true,new KeyUsage(KeyUsage.keyCertSign|KeyUsage.cRLSign));
        var holder=builder.build(new JcaContentSignerBuilder("SHA256withRSA").build(pair.getPrivate()));
        return new X509DeviceIssuer(pem(new org.bouncycastle.util.io.pem.PemObject("PRIVATE KEY",pair.getPrivate().getEncoded())),pem(holder),START);
    }
    private static String csr(String device)throws Exception{
        var generator=KeyPairGenerator.getInstance("EC");generator.initialize(new java.security.spec.ECGenParameterSpec("secp256r1"));var pair=generator.generateKeyPair();
        return pem(new JcaPKCS10CertificationRequestBuilder(new X500Name("CN="+device),pair.getPublic()).build(new JcaContentSignerBuilder("SHA256withECDSA").build(pair.getPrivate())));
    }
    private record Setup(EndpointService service,DirectoryService directory,DirectoryService.Actor admin,PostgresStore store,MutableClock clock,X509DeviceIssuer issuer){}
    private Setup setup()throws Exception{
        var base=Objects.requireNonNull(System.getenv("CONTROL_TEST_URL"));var password=Objects.requireNonNull(System.getenv("CONTROL_TEST_PASSWORD"));var database="endpoint_"+UUID.randomUUID().toString().replace("-","");
        try(var connection=java.sql.DriverManager.getConnection(base,"control_migrator",password);var statement=connection.createStatement()){statement.execute("CREATE DATABASE "+database);}
        var url=base.replace("/control?","/"+database+"?");
        org.flywaydb.core.Flyway.configure().dataSource(url,"control_migrator",password).target("4").load().migrate();
        assertEquals(6,org.flywaydb.core.Flyway.configure().dataSource(url,"control_migrator",password).load().migrate().migrationsExecuted);
        var source=new org.postgresql.ds.PGSimpleDataSource();source.setURL(url);source.setUser("control_app");source.setPassword(password);
        return configured(new PostgresStore(source,codec));
    }
    private Setup configured(PostgresStore store)throws Exception{
        var directory=new DirectoryService(store,codec,512,1048576);var admin=new DirectoryService.Actor(new Ids.TenantId("endpoint"),"a".repeat(64),true);
        for(var user:List.of("owner","other"))directory.mutate(admin,Ids.Kind.USER,user,"CREATE",DomainTest.user(user,1),0,user,"request");
        var clock=new MutableClock();var issuer=issuer();var service=new EndpointService(store,codec,issuer,clock,true,"endpoint","server","Organization","https://control.example.test","https://gateway.example.test");
        return new Setup(service,directory,admin,store,clock,issuer);
    }
    private EndpointEnrollmentChallenge start(Setup s,String device)throws Exception{return codec.model(s.service.start(codec.json(new EndpointEnrollmentStart(device,"0.6.0-dev",ClientPlatform.LINUX,csr(device),List.of("endpoint.identity.v1"))),"request").body(),EndpointEnrollmentChallenge.class);}
    private EndpointEnrollmentReview review(Setup s,EndpointEnrollmentChallenge challenge){return codec.model(s.service.review(s.admin,"owner",challenge.userCode()).body(),EndpointEnrollmentReview.class);}
    private void decide(Setup s,EndpointEnrollmentChallenge challenge,EnrollmentChoice choice,String key){var review=review(s,challenge);s.service.decide(s.admin,"owner",codec.json(new EndpointEnrollmentDecision(challenge.userCode(),review.keyFingerprint(),choice)),key,"request");}
    private EndpointEnrollmentResult poll(Setup s,EndpointEnrollmentChallenge challenge){return codec.model(s.service.poll(codec.json(new EndpointEnrollmentPoll(challenge.enrollmentId(),challenge.deviceCode())),"request").body(),EndpointEnrollmentResult.class);}
    private java.security.cert.X509Certificate certificate(DeviceIdentity identity)throws Exception{return (java.security.cert.X509Certificate)java.security.cert.CertificateFactory.getInstance("X.509").generateCertificate(new ByteArrayInputStream(identity.certificatePem().getBytes(java.nio.charset.StandardCharsets.US_ASCII)));}
    @Test void enrollmentBindsOwnerKeyAndDurableSequenceAndRevocation()throws Exception{
        var s=setup();var challenge=start(s,"device");var review=review(s,challenge);assertEquals(EnrollmentState.PENDING,poll(s,challenge).state());
        assertEquals(403,assertThrows(Failure.class,()->s.service.review(new DirectoryService.Actor(new Ids.TenantId("other"),"a".repeat(64),true),"owner",challenge.userCode())).status());
        assertEquals(403,assertThrows(Failure.class,()->s.service.review(s.admin,"unknown",challenge.userCode())).status());
        assertEquals(409,assertThrows(Failure.class,()->s.service.decide(s.admin,"owner",codec.json(new EndpointEnrollmentDecision(challenge.userCode(),"0".repeat(64),EnrollmentChoice.APPROVE)),"wrong-key","request")).status());
        decide(s,challenge,EnrollmentChoice.APPROVE,"approve");decide(s,challenge,EnrollmentChoice.APPROVE,"approve");
        assertEquals(409,assertThrows(Failure.class,()->decide(s,challenge,EnrollmentChoice.DENY,"other-key")).status());
        s.clock.time+=5000;var result=poll(s,challenge);assertEquals(EnrollmentState.CONSUMED,result.state());var identity=result.identity();assertEquals("owner",identity.userId());assertEquals("endpoint",identity.tenantId());
        var peer=certificate(identity);assertEquals(review.keyFingerprint(),s.issuer.peerFingerprint(peer,s.clock.time));assertEquals(-1,peer.getBasicConstraints());
        s.service.verifySocketPeer(peer);
        assertEquals(List.of("1.3.6.1.5.5.7.3.2"),peer.getExtendedKeyUsage());
        s.clock.time+=5000;assertEquals(identity,poll(s,challenge).identity());
        var report=new ClientReport("device","0.6.0-dev",0L,List.of());var body=codec.json(new EndpointCheckIn(1L,report,null,null));
        var ack=s.service.checkIn(peer,body,"request");assertEquals(ack.body(),s.service.checkIn(peer,body,"retry").body());
        assertEquals(409,assertThrows(Failure.class,()->s.service.checkIn(peer,codec.json(new EndpointCheckIn(2L,report,null,null)),"too-fast")).status());
        assertEquals(409,assertThrows(Failure.class,()->s.service.checkIn(peer,codec.json(new EndpointCheckIn(1L,new ClientReport("device","0.6.1",0L,List.of()),null,null)),"changed")).status());
        assertEquals(403,assertThrows(Failure.class,()->s.service.checkIn(peer,codec.json(new EndpointCheckIn(2L,new ClientReport("other","0.6.0-dev",0L,List.of()),null,null)),"wrong")).status());
        assertEquals(409,assertThrows(Failure.class,()->s.service.checkIn(peer,codec.json(new EndpointCheckIn(3L,report,null,null)),"gap")).status());
        assertFalse(ack.body().contains("nextIntervalMs"));
        var optedIn=codec.model(s.service.checkIn(peer,body,"millisecond-retry",true).body(),EndpointCheckInAck.class);
        assertEquals(500L,optedIn.nextIntervalMs());
        assertEquals(ack.body(),s.service.checkIn(peer,body,"legacy-retry").body());
        s.clock.time+=249;
        assertEquals(409,assertThrows(Failure.class,()->s.service.checkIn(peer,codec.json(new EndpointCheckIn(2L,report,null,null)),"burst",true)).status());
        s.clock.time+=1;
        var fast=codec.model(s.service.checkIn(peer,codec.json(new EndpointCheckIn(2L,report,null,null)),"millisecond",true).body(),EndpointCheckInAck.class);
        assertEquals(2L,fast.nextIntervalSeconds());
        assertEquals(500L,fast.nextIntervalMs());
        s.clock.time=START+50000000;var renewed=codec.model(s.service.checkIn(peer,codec.json(new EndpointCheckIn(3L,report,null,null)),"renew").body(),EndpointCheckInAck.class).identity();assertNotNull(renewed);assertTrue(renewed.expiresAtUnixMs()>identity.expiresAtUnixMs());
        var device=codec.model(s.service.device(s.admin,"device").body(),EndpointDeviceRecord.class);assertEquals(3,device.reportSequence());
        var revoked=s.service.revoke(s.admin,"device",codec.json(new EndpointRevokeRequest(device.revision())),"revoke","request");assertEquals(revoked.body(),s.service.revoke(s.admin,"device",codec.json(new EndpointRevokeRequest(device.revision())),"revoke","request").body());
        assertEquals(403,assertThrows(Failure.class,()->s.service.checkIn(certificate(renewed),codec.json(new EndpointCheckIn(4L,report,null,null)),"revoked")).status());
        assertEquals(403,assertThrows(Failure.class,()->s.service.checkIn(peer,body,"replay-after-revoke")).status());
        assertEquals(403,assertThrows(Failure.class,()->s.service.verifySocketPeer(peer)).status());
        var audit=s.directory.audit(s.admin,0,100).body();for(var event:List.of("ENROLLMENT_CREATE","ENROLLMENT_APPROVE","ENROLLMENT_CONSUME","DEVICE_CHECK_IN","DEVICE_RENEW","DEVICE_REVOKE"))assertTrue(audit.contains(event));
        assertFalse(audit.contains(challenge.deviceCode()));assertFalse(s.directory.export(s.admin,false).body().contains("certificatePem"));
    }
    @Test void deniedExpiredWrongCodeInvalidCsrAndDisabledOwnersFailClosed()throws Exception{
        var s=setup();var denied=start(s,"denied");decide(s,denied,EnrollmentChoice.DENY,"deny");assertEquals(EnrollmentState.DENIED,poll(s,denied).state());
        var pending=start(s,"pending");assertEquals(403,assertThrows(Failure.class,()->s.service.poll(codec.json(new EndpointEnrollmentPoll(pending.enrollmentId(),"f".repeat(64))),"wrong")).status());
        assertThrows(Failure.class,()->s.service.start(codec.json(new EndpointEnrollmentStart("bad","0.6.0-dev",ClientPlatform.LINUX,"not a CSR",List.of())),"invalid"));
        var request=csr("tampered");var parsed=new org.bouncycastle.util.io.pem.PemReader(new StringReader(request)).readPemObject();
        byte[] corrupted=parsed.getContent();corrupted[corrupted.length-1]^=1;
        var text=new StringWriter();try(var writer=new org.bouncycastle.util.io.pem.PemWriter(text)){writer.writeObject(new org.bouncycastle.util.io.pem.PemObject(parsed.getType(),corrupted));}
        assertThrows(Failure.class,()->s.issuer.fingerprint(text.toString()));
        s.clock.time=pending.expiresAtUnixMs();assertEquals(EnrollmentState.EXPIRED,poll(s,pending).state());assertEquals(409,assertThrows(Failure.class,()->decide(s,pending,EnrollmentChoice.APPROVE,"expired")).status());
        s.clock.time-=1;assertEquals(503,assertThrows(Failure.class,()->s.service.start(codec.json(new EndpointEnrollmentStart("regression","0.6.0-dev",ClientPlatform.LINUX,request,List.of())),"clock")).status());
    }
    @Test void competingDecisionsHaveOneWinnerAndAuditFailureRollsBack()throws Exception{
        var s=setup();var challenge=start(s,"race");var review=review(s,challenge);var body=codec.json(new EndpointEnrollmentDecision(challenge.userCode(),review.keyFingerprint(),EnrollmentChoice.APPROVE));
        var pool=java.util.concurrent.Executors.newFixedThreadPool(2);var barrier=new java.util.concurrent.CyclicBarrier(2);
        try{var tasks=new ArrayList<java.util.concurrent.Future<Boolean>>();for(int i=0;i<2;i++){final int n=i;tasks.add(pool.submit(()->{barrier.await();try{s.service.decide(s.admin,"owner",body,"race-"+n,"request");return true;}catch(Failure failure){assertEquals(409,failure.status());return false;}}));}
            int winners=0;for(var task:tasks)if(task.get())winners++;assertEquals(1,winners);
        }finally{pool.shutdownNow();}
        var second=start(s,"audit");var secondReview=review(s,second);
        Store failing=new Store(){public<T>T transaction(Ids.TenantId tenant,boolean write,java.util.function.Function<Session,T> work){return s.store.transaction(tenant,write,tx->work.apply((Session)java.lang.reflect.Proxy.newProxyInstance(getClass().getClassLoader(),new Class<?>[]{Session.class},(proxy,method,args)->{if(method.getName().equals("audit"))throw Failure.unavailable();try{return method.invoke(tx,args);}catch(java.lang.reflect.InvocationTargetException failure){throw failure.getCause();}})));}};
        var service=new EndpointService(failing,codec,s.issuer,s.clock,true,"endpoint","server","Organization","https://control.example.test","https://gateway.example.test");
        assertThrows(Failure.class,()->service.decide(s.admin,"owner",codec.json(new EndpointEnrollmentDecision(second.userCode(),secondReview.keyFingerprint(),EnrollmentChoice.APPROVE)),"audit-fail","request"));
        assertEquals(EnrollmentState.PENDING,review(s,second).state());assertThrows(Failure.class,()->s.service.device(s.admin,"audit"));
    }
    @Test void permissionReplacementAndServerRelayedCallsTrackEveryHandoff()throws Exception{relayFlow(setup());}
    @Test void quickstartPermissionReplacementAndRelayedCallsUseDurableSqlite(@org.junit.jupiter.api.io.TempDir java.nio.file.Path directory)throws Exception{
        relayFlow(configured(new PostgresStore(SqliteState.open(directory.resolve("relay.sqlite")),codec)));
    }
    private void relayFlow(Setup s)throws Exception{
        var challenge=start(s,"client");decide(s,challenge,EnrollmentChoice.APPROVE,"approve-client");var identity=poll(s,challenge).identity();var peer=certificate(identity);
        for(var agent:List.of("agent-one","agent-two"))s.directory.mutate(s.admin,Ids.Kind.AGENT,agent,"CREATE",codec.json(new ControlAgent(agent,agent,true,1L,"owner")),0,"create-"+agent,"request");
        var definition=new ToolDefinition("local.tool","Local tool","Installed local tool",List.of(new ToolAction("invoke",List.of(ResourceKind.CUSTOM))),Map.of(),Map.of());
        s.directory.mutate(s.admin,Ids.Kind.TOOL,"local.tool","CREATE",codec.json(new ControlTool("local.tool","Local tool",true,1L,definition)),0,"create-tool","request");
        var resource=new ResourceDescriptor(ResourceKind.CUSTOM,"runtime/local.tool");
        var policy=new ControlPolicy("local-permission","Local permission",true,1L,"local.tool","invoke",resource,Decision.ALLOW,List.of("owner"),List.of(),List.of("agent-one"),List.of("client"));
        s.directory.mutate(s.admin,Ids.Kind.POLICY,policy.id(),"CREATE",codec.json(policy),0,"create-policy","request");
        var local=new BuiltinToolInfo("local.tool","invoke","Installed local tool",true,Map.of());var report=new ClientReport("client","0.10.0-dev",0L,List.of());
        var first=codec.model(s.service.checkIn(peer,codec.json(new EndpointCheckIn(1L,report,null,List.of(local))),"poll-one",true).body(),EndpointCheckInAck.class);
        assertNotNull(first.configuration());assertEquals(1L,first.configuration().revision());assertEquals(1,first.configuration().permissions().size());
        var digest=first.configuration().digest();s.clock.time+=500;
        assertNull(codec.model(s.service.checkIn(peer,codec.json(new EndpointCheckIn(2L,report,digest,List.of(local))),"poll-two",true).body(),EndpointCheckInAck.class).configuration());
        var gateway=new DirectoryService.Actor(new Ids.TenantId("endpoint"),DirectoryService.digest("gateway"),false);var relay=s.service.relay();
        var context=new RequestContext("relay-request","endpoint","owner","agent-one","client");
        assertEquals(1,codec.model(relay.catalog(gateway,true,codec.json(context)).body(),LocalToolCatalog.class).tools().size());
        assertTrue(codec.model(relay.catalog(gateway,true,codec.json(new RequestContext("other-catalog","endpoint","owner","agent-two","client"))).body(),LocalToolCatalog.class).tools().isEmpty());
        assertThrows(Failure.class,()->relay.catalog(gateway,false,codec.json(context)));
        var arguments=Map.of("text",new com.fasterxml.jackson.databind.node.TextNode("hello"));var request=new AuthorizationRequest("local.tool","invoke",Map.copyOf(arguments));
        var input=new PolicyInput(context,"local.tool","invoke",resource,DirectoryService.digest(codec.json(arguments)));
        var submission=new RemoteToolSubmission(input,request,s.clock.time+29000);
        var waiting=codec.model(relay.submit(gateway,true,codec.json(submission),"agent-request").body(),RemoteToolResponse.class);
        assertEquals(RemoteToolState.WAITING_FOR_POLL,waiting.record().state());
        assertEquals(waiting,codec.model(relay.submit(gateway,true,codec.json(submission),"agent-retry").body(),RemoteToolResponse.class));
        s.clock.time+=500;var delivered=codec.model(s.service.checkIn(peer,codec.json(new EndpointCheckIn(3L,report,digest,List.of(local))),"poll-three",true).body(),EndpointCheckInAck.class).task();assertNotNull(delivered);
        var bound=new AuthorizationRequest("local.tool","invoke",Map.of("path",new com.fasterxml.jackson.databind.node.TextNode(resource.locator()),"input",(com.fasterxml.jackson.databind.JsonNode)codec.value(codec.json(arguments)),"runtimeImage",new com.fasterxml.jackson.databind.node.TextNode("reviewed-image")));
        assertEquals(delivered.expiresAtUnixMs(),codec.model(relay.authorize(peer,codec.json(new RemoteToolAuthorization(delivered.requestId(),delivered.leaseId(),bound))).body(),RemoteToolAuthorizationAck.class).expiresAtUnixMs());
        var result=new RemoteToolResult(delivered.requestId(),delivered.leaseId(),Map.of("text",new com.fasterxml.jackson.databind.node.TextNode("hello")),null);
        assertEquals(403,assertThrows(Failure.class,()->relay.result(peer,codec.json(new RemoteToolResult(delivered.requestId(),"wrong-lease",result.output(),null)),"wrong")).status());
        assertEquals(RemoteToolState.RESPONSE_RECEIVED,codec.model(relay.result(peer,codec.json(result),"client-result").body(),RemoteToolRecord.class).state());
        var observed=codec.model(relay.page(s.admin,"admin-progress").body(),RemoteToolPage.class).items().getFirst();assertNotNull(observed.submittedAtUnixMs());assertNotNull(observed.responseAtUnixMs());assertNull(observed.completedAtUnixMs());
        var done=codec.model(relay.response(gateway,true,codec.json(context),"agent-response").body(),RemoteToolResponse.class);assertEquals(RemoteToolState.DONE,done.record().state());assertEquals(result,done.result());assertNotNull(done.record().completedAtUnixMs());
        assertEquals(RemoteToolState.DONE,codec.model(relay.result(peer,codec.json(result),"client-retry").body(),RemoteToolRecord.class).state());
        var nextContext=new RequestContext("revoked-request","endpoint","owner","agent-one","client");relay.submit(gateway,true,codec.json(new RemoteToolSubmission(new PolicyInput(nextContext,input.toolId(),input.action(),resource,input.argumentsDigest()),request,s.clock.time+29000)),"queued-before-change");
        var block=new ControlPolicy(policy.id(),policy.name(),true,1L,policy.toolId(),policy.action(),resource,Decision.BLOCK,policy.userIds(),policy.teamIds(),policy.agentIds(),policy.deviceIds());s.directory.mutate(s.admin,Ids.Kind.POLICY,policy.id(),"UPDATE",codec.json(block),1,"block-policy","request");
        assertTrue(codec.model(relay.catalog(gateway,true,codec.json(context)).body(),LocalToolCatalog.class).tools().isEmpty());
        s.clock.time+=500;var changed=codec.model(s.service.checkIn(peer,codec.json(new EndpointCheckIn(4L,report,digest,List.of(local))),"changed-poll",true).body(),EndpointCheckInAck.class);assertNotNull(changed.configuration());assertEquals(2L,changed.configuration().revision());assertNotEquals(digest,changed.configuration().digest());assertNull(changed.task());
        assertEquals(RemoteToolState.FAILED,codec.model(relay.response(gateway,true,codec.json(nextContext),"failed-response").body(),RemoteToolResponse.class).record().state());
        assertThrows(Failure.class,()->relay.response(gateway,true,codec.json(context),"old-result-after-change"));
        s.clock.time+=500;assertNull(codec.model(s.service.checkIn(peer,codec.json(new EndpointCheckIn(5L,report,changed.configuration().digest(),List.of(local))),"ack-change",true).body(),EndpointCheckInAck.class).configuration());
    }
}
