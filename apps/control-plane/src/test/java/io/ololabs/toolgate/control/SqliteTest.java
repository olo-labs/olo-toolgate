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
            s.execute("DROP TABLE quickstart_vault");
            for(var name:java.util.List.of("control_audit","control_approvals","control_permit_leases","control_enrollments","control_idempotency"))s.execute("DROP INDEX "+name+"_local_cursor");
            s.execute("DELETE FROM quickstart_migrations WHERE version=2");
        }
        source=SqliteState.open(path);
        try(var c=source.getConnection();var s=c.createStatement()){
            try(var r=s.executeQuery("SELECT count(*) FROM quickstart_migrations")){assertTrue(r.next());assertEquals(2,r.getInt(1));}
            s.execute("INSERT INTO quickstart_migrations VALUES(3,'future')");
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
