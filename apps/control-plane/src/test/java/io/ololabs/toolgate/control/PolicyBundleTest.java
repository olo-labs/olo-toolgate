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
import java.security.KeyPair;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/** Dedicated cryptographic/compiler tests plus durable publication boundaries in real PostgreSQL. */
final class PolicyBundleTest {
    private final ContractCodec codec = new ContractCodec();
    private final Clock clock = Clock.fixed(Instant.ofEpochMilli(1700000000000L), ZoneOffset.UTC);
    private com.fasterxml.jackson.databind.JsonNode fixtures() throws Exception {
        return new com.fasterxml.jackson.databind.ObjectMapper().readTree(Path.of(System.getProperty("toolgate.fixtures")).toFile());
    }
    private KeyPair keys() throws Exception {
        var generator = java.security.KeyPairGenerator.getInstance("RSA"); generator.initialize(2048); return generator.generateKeyPair();
    }
    private RsaBundleSigner signer(KeyPair keys, String keyId) {
        return new RsaBundleSigner((java.security.interfaces.RSAPrivateCrtKey) keys.getPrivate(), keyId, codec);
    }
    private BundlePayload verify(String document, KeyPair keys) throws Exception {
        var signed = codec.model(document, SignedPolicyBundle.class); var parts = signed.jws().split("\\.");
        var signature = java.security.Signature.getInstance("SHA256withRSA"); signature.initVerify(keys.getPublic());
        signature.update((parts[0]+"."+parts[1]).getBytes(StandardCharsets.US_ASCII));
        assertTrue(signature.verify(Base64.getUrlDecoder().decode(parts[2])));
        var payload = codec.model(new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8), BundlePayload.class);
        var policy = new String(Base64.getUrlDecoder().decode(payload.policy()), StandardCharsets.UTF_8);
        assertEquals(DirectoryService.digest(policy), payload.policySha256()); codec.model(policy, CompiledPolicy.class); return payload;
    }
    @Test void compilerIsDeterministicAndEmptyTeamDoesNotBecomeWildcard() throws Exception {
        var fixtures = fixtures();
        var tool = codec.model(codec.json(fixtures.get("ControlTool")), ControlTool.class);
        var resource = codec.model(codec.json(fixtures.get("ResourceDescriptor")), ResourceDescriptor.class);
        var entries = new HashMap<Ids.RecordId, Directory.Entry>();
        for (var id : List.of("bob","alice")) { var e = codec.entry(Ids.Kind.USER, DomainTest.user(id,1)); entries.put(e.id(),e); }
        var t = codec.entry(Ids.Kind.TOOL, codec.json(tool)); entries.put(t.id(),t);
        var team = codec.entry(Ids.Kind.TEAM, codec.json(new ControlTeam("team","Team",true,1L,List.of("bob","alice")))); entries.put(team.id(),team);
        var empty = codec.entry(Ids.Kind.TEAM, codec.json(new ControlTeam("empty","Empty",true,1L,List.of()))); entries.put(empty.id(),empty);
        var action = tool.definition().actions().getFirst().name();
        var allow = new ControlPolicy("allow","Allow",true,1L,tool.id(),action,resource,Decision.ALLOW,List.of("bob"),List.of("team"),List.of(),List.of());
        var emptyPolicy = new ControlPolicy("empty-policy","Empty",true,1L,tool.id(),action,resource,Decision.ALLOW,List.of(),List.of("empty"),List.of(),List.of());
        for (var p : List.of(allow,emptyPolicy)) { var e=codec.entry(Ids.Kind.POLICY,codec.json(p)); entries.put(e.id(),e); }
        var directory = new Directory(0,entries); var compiler = new PolicyCompiler(codec);
        var compiled = compiler.compile(directory); assertEquals(compiled,compiler.compile(directory));
        var policy = codec.model(compiled,CompiledPolicy.class);
        assertEquals(1,policy.rules().size()); assertEquals(List.of("alice","bob"),policy.rules().getFirst().userIds());
        var disabled = codec.entry(Ids.Kind.USER,DomainTest.user("alice",1).replace("true","false")); entries.put(disabled.id(),disabled);
        assertThrows(IllegalArgumentException.class,()->compiler.compile(new Directory(0,entries)));
    }
    @Test void signerUsesExactStandardJwsAndSeparateKeyIds() throws Exception {
        var payload = codec.model(codec.json(fixtures().get("BundlePayload")),BundlePayload.class); var keys=keys();
        var first=codec.json(signer(keys,"old-key").sign(payload)); assertEquals(payload,verify(first,keys));
        var parts=codec.model(first,SignedPolicyBundle.class).jws().split("\\.");
        assertEquals("old-key",codec.model(new String(Base64.getUrlDecoder().decode(parts[0]),StandardCharsets.UTF_8),BundleHeader.class).kid());
        assertEquals(first,codec.json(signer(keys,"old-key").sign(payload)));
        assertNotEquals(first,codec.json(signer(keys,"new-key").sign(payload)));
        var weak=java.security.KeyPairGenerator.getInstance("RSA"); weak.initialize(1024);
        assertThrows(IllegalArgumentException.class,()->signer(weak.generateKeyPair(),"weak"));
        var unsupported=java.security.KeyPairGenerator.getInstance("RSA");
        unsupported.initialize(new java.security.spec.RSAKeyGenParameterSpec(2048,java.math.BigInteger.valueOf(3)));
        assertThrows(IllegalArgumentException.class,()->signer(unsupported.generateKeyPair(),"unsupported"));
        unsupported.initialize(2050);
        assertThrows(IllegalArgumentException.class,()->signer(unsupported.generateKeyPair(),"unsupported-size"));
        assertThrows(Failure.class,()->codec.model("{\"alg\":\"none\",\"kid\":\"k\",\"typ\":\"toolgate-policy-bundle+jws\"}",BundleHeader.class));
        assertThrows(Failure.class,()->codec.model("{\"jws\":\"a.b.c\",\"jws\":\"a.b.d\"}",SignedPolicyBundle.class));
    }
    private String publish(long revision,long sequence, Long rollback) {
        return codec.json(new BundlePublishRequest(revision,sequence,60000L,0L,null,rollback));
    }
    @Test void publicationRollbackConcurrencyAuditAndSigningFailureAreAtomic() throws Exception {
        var base=Objects.requireNonNull(System.getenv("CONTROL_TEST_URL")); var password=Objects.requireNonNull(System.getenv("CONTROL_TEST_PASSWORD"));
        var database="bundle_"+UUID.randomUUID().toString().replace("-","");
        try (var connection=java.sql.DriverManager.getConnection(base,"control_migrator",password);var statement=connection.createStatement()) { statement.execute("CREATE DATABASE "+database); }
        var url=base.replace("/control?","/"+database+"?");
        var flyway=org.flywaydb.core.Flyway.configure().dataSource(url,"control_migrator",password).target("2").load(); flyway.migrate();
        assertEquals(1,org.flywaydb.core.Flyway.configure().dataSource(url,"control_migrator",password).load().migrate().migrationsExecuted);
        var source=new org.postgresql.ds.PGSimpleDataSource(); source.setURL(url);source.setUser("control_app");source.setPassword(password);
        var store=new PostgresStore(source,codec); var directory=new DirectoryService(store,codec,512,1048576);
        var actor=new DirectoryService.Actor(new Ids.TenantId("bundle-tenant"),"a".repeat(64),true);
        var keys=keys(); var service=new BundleService(store,codec,signer(keys,"bundle-key"),"control","gateway",clock);
        assertThrows(Failure.class,()->service.current(actor));
        directory.mutate(actor,Ids.Kind.USER,"alice","CREATE",DomainTest.user("alice",1),0,"user","req");
        var pool=java.util.concurrent.Executors.newFixedThreadPool(6);
        var replies=new ArrayList<java.util.concurrent.Future<Store.Reply>>();
        try { for(int i=0;i<6;i++) replies.add(pool.submit(()->service.publish(actor,publish(1,0,null),"publish-one","req")));
            String first=replies.getFirst().get().body();
            for(var reply:replies) assertEquals(first,reply.get().body());
            assertEquals(1,verify(first,keys).sequence());
        } finally { pool.shutdownNow(); }
        assertThrows(Failure.class,()->service.publish(actor,publish(1,0,null),"stale","req"));
        assertThrows(Failure.class,()->service.publish(actor,publish(1,1,null),"publish-one","req"));
        assertThrows(Failure.class,()->service.publish(new DirectoryService.Actor(actor.tenant(),actor.id(),false),publish(1,1,null),"denied","req"));
        var second=service.publish(actor,publish(1,1,null),"publish-two","req"); assertEquals(2,verify(second.body(),keys).sequence());
        var rotated=keys(); var rotation=new BundleService(store,codec,signer(rotated,"new-key"),"control","gateway",clock);
        var third=rotation.publish(actor,publish(1,2,1L),"rollback","req"); var payload=verify(third.body(),rotated);
        assertEquals(3,payload.sequence());assertEquals(1,payload.rollbackOf());assertEquals(verify(second.body(),keys).policy(),payload.policy());
        assertEquals(second.body(),service.get(actor,2).body());
        var other=new DirectoryService.Actor(new Ids.TenantId("other"),actor.id(),true);assertThrows(Failure.class,()->service.get(other,1));
        var failing=new BundleService(store,codec,p->{throw Failure.unavailable();},"control","gateway",clock);
        assertThrows(Failure.class,()->failing.publish(actor,publish(1,3,null),"failure","req"));assertEquals(3,service.current(actor).revision());
        try(var connection=source.getConnection();var statement=connection.createStatement()) {
            assertThrows(java.sql.SQLException.class,()->statement.executeUpdate("UPDATE control_policy_bundles SET policy='{}'"));
            assertThrows(java.sql.SQLException.class,()->statement.executeUpdate("DELETE FROM control_policy_bundles"));
        }
        try(var connection=java.sql.DriverManager.getConnection(url,"control_migrator",password);var statement=connection.createStatement()) {
            statement.execute("ALTER TABLE control_audit ADD CONSTRAINT bundle_audit_failure CHECK (target <> 'bundle:4')");
        }
        assertThrows(Failure.class,()->service.publish(actor,publish(1,3,null),"failure","req"));assertEquals(3,service.current(actor).revision());
        try(var connection=java.sql.DriverManager.getConnection(url,"control_migrator",password);var statement=connection.createStatement()) {
            statement.execute("ALTER TABLE control_audit DROP CONSTRAINT bundle_audit_failure");
        }
        assertEquals(4,service.publish(actor,publish(1,3,null),"failure","req").revision());
        try(var connection=source.getConnection();var statement=connection.createStatement();var rows=statement.executeQuery("SELECT count(*) FROM control_audit WHERE operation LIKE 'BUNDLE_%' AND revision=1")) {
            rows.next();assertEquals(4,rows.getLong(1));
        }
        assertThrows(Failure.class,()->service.publish(actor,publish(0,4,null),"stale-directory","req"));
        assertThrows(Failure.class,()->service.publish(actor,publish(1,4,5L),"future-rollback","req"));
    }
}
