// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
package io.ololabs.toolgate.control;

import io.ololabs.toolgate.control.adapter.*;
import io.ololabs.toolgate.control.application.*;
import io.ololabs.toolgate.control.domain.Ids;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

final class SqliteTest {
    @TempDir Path directory;
    @Test void configuredDefaultPoliciesInheritTeamWithoutGrantingDisabledUsers() throws Exception {
        var codec = new ContractCodec(); var store = new PostgresStore(SqliteState.open(directory.resolve("policy-membership.sqlite")),codec);
        var legacy = new DirectoryService(store,codec,512,1048576);
        var actor = new DirectoryService.Actor(new Ids.TenantId("tenant"),"a".repeat(64),true);
        legacy.mutate(actor,Ids.Kind.USER,"existing","CREATE",DomainTest.user("existing",1),0,"existing","request");
        var fixtures = new com.fasterxml.jackson.databind.ObjectMapper().readTree(Path.of(System.getProperty("toolgate.fixtures")).toFile());
        var tool = codec.model(codec.json(fixtures.get("ControlTool")),io.ololabs.toolgate.contracts.ControlTool.class);
        legacy.mutate(actor,Ids.Kind.TOOL,tool.id(),"CREATE",codec.json(tool),0,"tool","request");
        var resource = codec.model(codec.json(fixtures.get("ResourceDescriptor")),io.ololabs.toolgate.contracts.ResourceDescriptor.class);
        var policy = new io.ololabs.toolgate.contracts.ControlPolicy("default-policy","Default",true,1L,tool.id(),tool.definition().actions().getFirst().name(),resource,
            io.ololabs.toolgate.contracts.Decision.ALLOW,java.util.List.of("existing"),java.util.List.of(),java.util.List.of(),java.util.List.of());
        legacy.mutate(actor,Ids.Kind.POLICY,policy.id(),"CREATE",codec.json(policy),0,"policy","request");
        var service = new DirectoryService(store,codec,512,1048576,"team-default",java.util.List.of("default-policy"));
        var document = DomainTest.user("new-user",1).replace("true","false");
        service.mutate(actor,Ids.Kind.USER,"new-user","CREATE",document,0,"new-user","request");
        var assigned = codec.model(service.get(actor,Ids.Kind.POLICY,policy.id()).body(),io.ololabs.toolgate.contracts.ControlPolicy.class);
        assertEquals(java.util.List.of("team-default"),assigned.teamIds()); assertEquals(2L,assigned.revision());
        var compiler = new PolicyCompiler(codec);
        var compiled = store.transaction(actor.tenant(),false,tx->codec.model(compiler.compile(tx.load()),io.ololabs.toolgate.contracts.CompiledPolicy.class));
        assertEquals(java.util.List.of("existing"),compiled.rules().getFirst().userIds());
        service.mutate(actor,Ids.Kind.USER,"new-user","UPDATE",DomainTest.user("new-user",1),1,"enable-user","request");
        compiled = store.transaction(actor.tenant(),false,tx->codec.model(compiler.compile(tx.load()),io.ololabs.toolgate.contracts.CompiledPolicy.class));
        assertEquals(java.util.List.of("existing","new-user"),compiled.rules().getFirst().userIds());
    }
    @Test void defaultMembershipIsAtomicReplaySafeAndSupportsMultipleTeams() throws Exception {
        var codec = new ContractCodec(); var store = new PostgresStore(SqliteState.open(directory.resolve("membership.sqlite")),codec);
        var service = new DirectoryService(store,codec,512,1048576,"team-default");
        var actor = new DirectoryService.Actor(new Ids.TenantId("tenant"),"a".repeat(64),true);
        String user = DomainTest.user("new-user",1).replace("true","false");
        var created = service.mutate(actor,Ids.Kind.USER,"new-user","CREATE",user,0,"create-user","request");
        var team = codec.model(service.get(actor,Ids.Kind.TEAM,"team-default").body(),io.ololabs.toolgate.contracts.ControlTeam.class);
        assertEquals(java.util.List.of("new-user"),team.userIds()); assertTrue(team.enabled());
        assertFalse(codec.model(created.body(),io.ololabs.toolgate.contracts.ControlUser.class).enabled());
        assertEquals(created,service.mutate(actor,Ids.Kind.USER,"new-user","CREATE",user,0,"create-user","retry"));
        assertEquals(1,service.get(actor,Ids.Kind.TEAM,"team-default").revision());
        // Creating a user audits both the user and its automatic membership, once per transaction.
        var audits = new com.fasterxml.jackson.databind.ObjectMapper().readTree(service.audit(actor,0,20).body()).get("items");
        assertEquals(2,audits.size());
        assertEquals("users:new-user",audits.get(0).get("target").asText());
        assertEquals("teams:team-default",audits.get(1).get("target").asText());
        assertEquals("CREATE",audits.get(1).get("operation").asText());
        assertEquals(audits.get(0).get("requestId"),audits.get(1).get("requestId"));
        assertEquals(audits.get(0).get("revision"),audits.get(1).get("revision"));

        String other = codec.json(new io.ololabs.toolgate.contracts.ControlTeam("second-team","Second",true,1L,java.util.List.of("new-user"),null,null));
        service.mutate(actor,Ids.Kind.TEAM,"second-team","CREATE",other,0,"second-team","request");
        assertEquals(team.userIds(),codec.model(service.get(actor,Ids.Kind.TEAM,"second-team").body(),io.ololabs.toolgate.contracts.ControlTeam.class).userIds());
        service.mutate(actor,Ids.Kind.USER,"next-user","CREATE",DomainTest.user("next-user",1).replace("true","false"),0,"next-user","request");
        assertEquals(java.util.List.of("new-user","next-user"),codec.model(service.get(actor,Ids.Kind.TEAM,"team-default").body(),io.ololabs.toolgate.contracts.ControlTeam.class).userIds());
        assertThrows(Failure.class,()->service.mutate(actor,Ids.Kind.TEAM,"team-default","UPDATE",codec.json(team),1,"stale-team","request"));
        // Capacity failure must roll back user creation together with membership and replay state.
        var bounded = new DirectoryService(store,codec,4,1048576,"team-default");
        assertThrows(Failure.class,()->bounded.mutate(actor,Ids.Kind.USER,"overflow","CREATE",DomainTest.user("overflow",1),0,"overflow","request"));
        assertThrows(Failure.class,()->service.get(actor,Ids.Kind.USER,"overflow"));
        assertEquals(2,service.get(actor,Ids.Kind.TEAM,"team-default").revision());
        var finalAudits = new com.fasterxml.jackson.databind.ObjectMapper().readTree(service.audit(actor,0,20).body()).get("items");
        assertEquals(5,finalAudits.size());
        assertEquals("users:next-user",finalAudits.get(3).get("target").asText());
        assertEquals("teams:team-default",finalAudits.get(4).get("target").asText());
        assertEquals("UPDATE",finalAudits.get(4).get("operation").asText());
        assertEquals(finalAudits.get(3).get("requestId"),finalAudits.get(4).get("requestId"));

    }
    @Test void realTransactionsAuditReplayRestartAndImmutableTriggers() throws Exception {
        Path path=directory.resolve("control.sqlite"); var source=SqliteState.open(path); var codec=new ContractCodec();
        var store=new PostgresStore(source,codec);var service=new DirectoryService(store,codec,512,1048576);
        var actor=new DirectoryService.Actor(new Ids.TenantId("quickstart"),"a".repeat(64),true);
        var body="{\"id\":\"admin\",\"name\":\"Administrator\",\"enabled\":true,\"revision\":1}";
        var created=service.mutate(actor,Ids.Kind.USER,"admin","CREATE",body,0,"seed-user","req-create");assertEquals(201,created.status());
        assertEquals(created,service.mutate(actor,Ids.Kind.USER,"admin","CREATE",body,0,"seed-user","req-repeat"));
        var restarted=new PostgresStore(SqliteState.open(path),codec);
        assertEquals("Administrator",codec.model(new DirectoryService(restarted,codec,512,1048576).get(actor,Ids.Kind.USER,"admin").body(),io.ololabs.toolgate.contracts.ControlUser.class).name());
        assertTrue(restarted.transaction(actor.tenant(),false,tx->tx.auditPage(0,20)).contains("CREATE"));
        assertThrows(Failure.class,()->store.transaction(actor.tenant(),true,tx->{tx.audit(actor.id(),"CREATE","failed",1,"rollback","b".repeat(64));throw Failure.validation();}));
        try(var c=source.getConnection();var s=c.createStatement()){
            assertThrows(java.sql.SQLException.class,()->s.executeUpdate("UPDATE control_audit SET target='tampered'"));
            s.execute("INSERT INTO control_fleet VALUES('quickstart','RELEASE','test',1,'{}')");
            assertThrows(java.sql.SQLException.class,()->s.executeUpdate("UPDATE control_fleet SET revision=2 WHERE record_id='test'"));
            try(var r=s.executeQuery("SELECT count(*) FROM control_audit")){assertTrue(r.next());assertEquals(1,r.getInt(1));}
        }
    }
    @Test void cleanOrderedUpgradeAndFutureVersionFailClosed() throws Exception {
        var path=directory.resolve("control.sqlite");var source=SqliteState.open(path);
        try(var c=source.getConnection();var s=c.createStatement()){
            // Reconstruct the actual old directory constraint, then prove V3 retains its data.
            s.execute("DROP TABLE control_records");
            try(var input=SqliteTest.class.getResourceAsStream("/db/quickstart/V1.sql")) {
                var script=new String(java.util.Objects.requireNonNull(input).readAllBytes(),java.nio.charset.StandardCharsets.UTF_8);
                s.execute(script.split("(?m)^-- statement\\s*$")[1]);
            }
            s.execute("INSERT INTO control_records VALUES('tenant','USER','kept',1,'{\"id\":\"kept\",\"name\":\"Kept\",\"enabled\":true,\"revision\":1}')");
            s.execute("DROP TABLE quickstart_vault");
            for(var name:java.util.List.of("control_audit","control_approvals","control_permit_leases","control_enrollments","control_idempotency"))s.execute("DROP INDEX "+name+"_local_cursor");
            s.execute("DELETE FROM quickstart_migrations WHERE version>=2");
        }
        source=SqliteState.open(path);
        try(var c=source.getConnection();var s=c.createStatement()){
            try(var r=s.executeQuery("SELECT count(*) FROM quickstart_migrations")){assertTrue(r.next());assertEquals(3,r.getInt(1));}
            try(var r=s.executeQuery("SELECT document FROM control_records WHERE record_id='kept'")){assertTrue(r.next());assertTrue(r.getString(1).contains("Kept"));}
            s.execute("INSERT INTO quickstart_migrations VALUES(4,'future')");
        }
        assertThrows(IllegalStateException.class,()->SqliteState.open(path));
    }
    @Test void appliedMigrationChecksumTamperingFailsClosed() throws Exception {
        var path=directory.resolve("control.sqlite");var source=SqliteState.open(path);
        try(var c=source.getConnection();var s=c.createStatement()){
            s.execute("UPDATE quickstart_migrations SET checksum='tampered' WHERE version=1");
        }
        assertThrows(IllegalStateException.class,()->SqliteState.open(path));
    }
}
