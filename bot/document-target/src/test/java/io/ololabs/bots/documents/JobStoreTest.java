// Copyright 2026 OLO Labs; SPDX-License-Identifier: Apache-2.0
package io.ololabs.bots.documents;

import java.sql.*;
import java.util.*;
import java.util.concurrent.*;
import io.ololabs.toolgate.service.JdbcEffectClaims;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

/** Actual PostgreSQL/RLS and atomic claim/job receipts. Live Control qualifies separately. */
class JobStoreTest {
    static String prefix,password,user;String database;JobStore store;
    static Connection admin(String db)throws Exception{return DriverManager.getConnection(prefix+db,"postgres",password);}
    @BeforeAll static void prerequisites()throws Exception {
        String jdbc=System.getenv("SANDBOX_TEST_JDBC");password=System.getenv("SANDBOX_TEST_PASSWORD");
        if(jdbc==null||password==null)throw new IllegalStateException("Run python bot/document-target/test.py");
        prefix=jdbc.substring(0,jdbc.lastIndexOf('/')+1);user="docs_"+UUID.randomUUID().toString().replace("-","");
        try(var c=admin("postgres");var q=c.createStatement()){q.executeUpdate("CREATE ROLE "+user+" LOGIN NOSUPERUSER NOBYPASSRLS PASSWORD '"+password+"'");}
    }
    @AfterAll static void role()throws Exception {
        if(prefix!=null&&user!=null)try(var c=admin("postgres");var q=c.createStatement()){q.executeUpdate("DROP ROLE "+user);}
    }
    @BeforeEach void initialize()throws Exception {
        database="docs_"+UUID.randomUUID().toString().replace("-","");
        try(var c=admin("postgres");var q=c.createStatement()){q.executeUpdate("CREATE DATABASE "+database);}
        new JobStore(prefix+database,"postgres",password,"tenant-a").initialize();
        try(var c=admin(database);var q=c.createStatement()) {
            q.executeUpdate("GRANT USAGE ON SCHEMA public TO "+user);
            q.executeUpdate("GRANT SELECT,INSERT,UPDATE,DELETE ON ALL TABLES IN SCHEMA public TO "+user);
        }
        store=new JobStore(prefix+database,user,password,"tenant-a");store.validate();
    }
    @AfterEach void drop()throws Exception {
        if(database!=null)try(var c=admin("postgres");var q=c.createStatement()){q.executeUpdate("DROP DATABASE "+database+" WITH (FORCE)");}
    }
    final String hash="sha256:"+"a".repeat(64);
    JobStore.Request input(){return new JobStore.Request("tenant-a","ocr","sample");}
    JobStore.Result act(String nonce,String operation,String hash)throws Exception {
        try(var c=store.connection()){var result=store.act(c,input(),nonce,operation,hash);c.commit();return result;}
    }
    long count(String table)throws Exception {
        try(var c=store.connection();var q=c.createStatement();var rows=q.executeQuery("SELECT count(*) FROM "+table)){rows.next();return rows.getLong(1);}
    }
    @Test void duplicate_submission_reuses_one_job_and_retains_each_fenced_access_receipt()throws Exception {
        assertEquals("QUEUED",act("submit-1","submit",hash).state());
        assertEquals("QUEUED",act("submit-2","submit",hash).state());
        assertEquals("QUEUED",act("poll-1","result",hash).state());
        assertEquals(1,count("document_jobs"));assertEquals(3,count("document_job_receipts"));
        new JobStore(prefix+database,user,password,"tenant-a").validate();
    }
    @Test void source_version_change_missing_job_or_foreign_tenant_does_not_release_prior_result()throws Exception {
        assertThrows(SecurityException.class,()->act("poll-missing","result",hash));
        act("submit","submit",hash);
        assertThrows(SecurityException.class,()->act("changed","submit","sha256:"+"b".repeat(64)));
        try(var c=store.connection()) {
            assertThrows(SecurityException.class,()->store.act(c,new JobStore.Request("tenant-b","ocr","sample"),"foreign","submit",hash));
        }
        assertEquals(1,count("document_jobs"));assertEquals(1,count("document_job_receipts"));
        var foreign=new JobStore(prefix+database,user,password,"tenant-b");
        try(var c=foreign.connection();var q=c.createStatement();var rows=q.executeQuery("SELECT count(*) FROM document_jobs")){rows.next();assertEquals(0,rows.getInt(1));}
    }
    @Test void concurrent_submissions_create_one_computation_and_two_confirmed_accesses()throws Exception {
        try(var pool=Executors.newFixedThreadPool(2)) {
            var first=pool.submit(()->act("first","submit",hash));var second=pool.submit(()->act("second","submit",hash));
            assertEquals("QUEUED",first.get().state());assertEquals("QUEUED",second.get().state());
        }
        assertEquals(1,count("document_jobs"));assertEquals(2,count("document_job_receipts"));
    }
    @Test void computation_expiry_is_terminal_and_does_not_enqueue_an_automatic_retry()throws Exception {
        act("submit","submit",hash);
        try(var c=store.connection();var q=c.createStatement()){q.executeUpdate("UPDATE document_jobs SET state='RUNNING',owner='lost-worker',lease_until=0");c.commit();}
        store.expireComputations();assertEquals("INTERRUPTED",act("inspect","result",hash).state());
        assertEquals("INTERRUPTED",act("repeat","submit",hash).state());assertEquals(1,count("document_jobs"));
    }
    @Test void job_and_claim_outcome_commit_together_and_rollback_preserves_applying()throws Exception {
        var claims=new JdbcEffectClaims();var binding=new JdbcEffectClaims.Binding("tenant-a","effect-1",1,"nonce-1","sha256:"+"c".repeat(64));
        JdbcEffectClaims.Owner owner;
        try(var c=store.connection()){owner=claims.claim(c,binding,System.currentTimeMillis()+10_000).owner();c.commit();}
        try(var c=store.connection()){assertTrue(claims.beginApply(c,owner));c.commit();}
        try(var c=store.connection()) {
            store.act(c,input(),"nonce-1","submit",hash);
            assertTrue(claims.finish(c,owner,true,"sha256:"+"d".repeat(64)));c.rollback();
        }
        assertEquals(0,count("document_jobs"));assertEquals(0,count("document_job_receipts"));assertEquals(0,count("toolgate_target_outcomes"));
        try(var c=store.connection();var q=c.createStatement();var rows=q.executeQuery("SELECT state FROM toolgate_target_claims")){rows.next();assertEquals("APPLYING",rows.getString(1));}
        // Known transaction rollback can record failure; it must not reapply the job.
        try(var c=store.connection()){assertTrue(claims.finish(c,owner,false,"sha256:"+"e".repeat(64)));c.commit();}
        assertEquals(1,count("toolgate_target_outcomes"));assertEquals(0,count("document_jobs"));
    }
    @Test void initialization_and_startup_refuse_foreign_schema_without_changing_it()throws Exception {
        assertThrows(SQLException.class,()->new JobStore(prefix+database,"postgres",password,"tenant-a").initialize());
        try(var c=admin(database);var q=c.createStatement()){
            q.executeUpdate("ALTER TABLE document_job_schema DROP CONSTRAINT document_job_schema_format_check");
            q.executeUpdate("UPDATE document_job_schema SET format='unsupported'");
        }
        assertThrows(SQLException.class,store::validate);assertEquals(0,count("document_jobs"));
        try(var c=admin(database);var q=c.createStatement();var rows=q.executeQuery("SELECT format FROM document_job_schema")){
            rows.next();assertEquals("unsupported",rows.getString(1));
        }
    }
}
