// Copyright 2026 OLO Labs; SPDX-License-Identifier: Apache-2.0
package io.ololabs.bots.sandbox;

import java.sql.*;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

/** Actual PostgreSQL transactions, least-privilege runtime role and forced tenant RLS; not Control qualification. */
class StoreTest {
    static String admin,user,password,prefix;String database;Store store;
    static Connection admin(String database)throws Exception {
        return DriverManager.getConnection(prefix+database,"postgres",password);
    }
    @BeforeAll static void prerequisites()throws Exception {
        admin=System.getenv("SANDBOX_TEST_JDBC");password=System.getenv("SANDBOX_TEST_PASSWORD");
        if(admin==null||password==null)throw new IllegalStateException("Run python bot/sandbox/test.py for an owned actual PostgreSQL fixture");
        prefix=admin.substring(0,admin.lastIndexOf('/')+1);user="sandbox_"+UUID.randomUUID().toString().replace("-","");
        try(var c=admin("postgres");var q=c.createStatement()){q.executeUpdate("CREATE ROLE "+user+" LOGIN NOSUPERUSER NOBYPASSRLS PASSWORD '"+password+"'");}
    }
    @AfterAll static void role()throws Exception {
        if(prefix!=null&&user!=null)try(var c=admin("postgres");var q=c.createStatement()){q.executeUpdate("DROP ROLE "+user);}
    }
    @BeforeEach void initialize()throws Exception {
        database="sandbox_"+UUID.randomUUID().toString().replace("-","");
        try(var c=admin("postgres");var q=c.createStatement()){q.executeUpdate("CREATE DATABASE "+database);}
        var owner=new Store(prefix+database,"postgres",password,"tenant-a");
        owner.initialize(Store.JSON.readTree("""
            {"tenantId":"tenant-a","subjects":[
              {"kind":"service","id":"api","allowedActor":"user-a","prerequisitesComplete":true},
              {"kind":"joiner","id":"ready","allowedActor":"user-a","prerequisitesComplete":true},
              {"kind":"joiner","id":"waiting","allowedActor":"user-a","prerequisitesComplete":false}]}
            """));
        try(var c=admin(database);var q=c.createStatement()) {
            q.executeUpdate("GRANT USAGE ON SCHEMA public TO "+user);
            q.executeUpdate("GRANT SELECT,INSERT,UPDATE,DELETE ON ALL TABLES IN SCHEMA public TO "+user);
        }
        store=new Store(prefix+database,user,password,"tenant-a");store.validate();
    }
    @AfterEach void drop()throws Exception {
        if(database!=null)try(var c=admin("postgres");var q=c.createStatement()){q.executeUpdate("DROP DATABASE "+database+" WITH (FORCE)");}
    }
    Store.Request request(String operation,String subject,String reference) {
        return new Store.Request("tenant-a","user-a",operation,subject,reference);
    }
    Store.Result act(Store.Request request)throws Exception {
        try(var c=store.connection()) {
            var result=store.act(c,request,TargetServer.digest(Store.JSON.writeValueAsBytes(request)));c.commit();return result;
        }
    }
    @Test void restart_changes_actual_state_once_per_reference_and_status_survives_reopen()throws Exception {
        var input=request("restart","api","incident-1");assertEquals(1,act(input).generation());assertEquals(1,act(input).generation());
        try(var c=store.connection()){assertEquals(1,store.inspect(c,request("service","api","incident-1")).generation());}
        assertEquals(2,act(request("restart","api","incident-2")).generation());
        store.validate();
    }
    @Test void concurrent_different_references_do_not_duplicate_joiner_tasks()throws Exception {
        try(var pool=Executors.newFixedThreadPool(4)) {
            var calls=new ArrayList<Future<Store.Result>>();
            for(int index=0;index<4;index++){String reference="joining-"+index;calls.add(pool.submit(()->act(request("provision","ready",reference))));}
            int applied=0;
            for(var future:calls){var result=future.get(10,TimeUnit.SECONDS);assertEquals(3,result.tasks().size());if(result.outcome().equals("APPLIED"))applied++;}
            assertEquals(1,applied);
        }
        try(var c=store.connection();var q=c.createStatement();var rows=q.executeQuery("SELECT count(*) FROM sandbox_tasks")){rows.next();assertEquals(3,rows.getInt(1));}
    }
    @Test void incomplete_sources_remain_pending_and_unknown_reference_is_not_failed()throws Exception {
        var result=act(request("provision","waiting","join-wait"));assertEquals("PENDING",result.outcome());assertEquals(List.of(),result.tasks());
        try(var c=store.connection()) {
            assertEquals("PENDING",store.inspect(c,request("joiner","waiting","join-wait")).outcome());
            assertEquals("UNKNOWN",store.inspect(c,request("joiner","ready","unresolved")).outcome());
        }
    }
    @Test void native_acl_cross_tenant_and_changed_reference_input_do_not_change_state()throws Exception {
        assertThrows(SecurityException.class,()->act(new Store.Request("tenant-a","other-user","restart","api","denied")));
        var foreign=new Store(store.url,store.user,store.password,"tenant-b");
        try(var c=foreign.connection()) {
            assertThrows(SecurityException.class,()->foreign.act(c,new Store.Request("tenant-b","user-a","restart","api","denied"),"sha256:"+"a".repeat(64)));
        }
        var input=request("restart","api","incident-1");act(input);
        try(var c=store.connection()){assertThrows(SecurityException.class,()->store.act(c,input,"sha256:"+"a".repeat(64)));}
        try(var c=store.connection()){assertEquals(1,store.inspect(c,request("service","api","incident-1")).generation());}
    }
    @Test void fresh_schema_only_and_rolled_back_business_transaction_has_no_receipt()throws Exception {
        var owner=new Store(store.url,"postgres",password,"tenant-a");
        assertThrows(SQLException.class,()->owner.initialize(Store.JSON.readTree("{\"tenantId\":\"tenant-a\",\"subjects\":[]}")));
        try(var c=store.connection()){store.act(c,request("restart","api","rolled-back"),"sha256:"+"a".repeat(64));c.rollback();}
        try(var c=store.connection()){var result=store.inspect(c,request("service","api","rolled-back"));assertEquals(0,result.generation());assertEquals("UNKNOWN",result.outcome());}
        try(var c=admin(database);var q=c.createStatement()){q.executeUpdate("DELETE FROM sandbox_schema");}
        assertThrows(SQLException.class,store::validate);
    }
}
