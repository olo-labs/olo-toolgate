// Copyright 2026 OLO Labs; SPDX-License-Identifier: Apache-2.0
package io.ololabs.bots.sandbox;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.json.JsonMapper;
import io.ololabs.toolgate.service.JdbcEffectClaims;
import java.sql.*;
import java.util.*;

/** Single-tenant target with tenant-keyed rows and forced PostgreSQL RLS. No migrations. */
final class Store {
    static final JsonMapper JSON=JsonMapper.builder()
        .enable(com.fasterxml.jackson.core.StreamReadFeature.STRICT_DUPLICATE_DETECTION)
        .enable(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_TRAILING_TOKENS).build();
    final String url,user,password,tenant;
    Store(String url,String user,String password,String tenant) {
        if(!url.startsWith("jdbc:postgresql:")||!tenant.matches("[A-Za-z0-9_-]{1,128}"))
            throw new IllegalArgumentException("Current PostgreSQL target configuration required");
        this.url=url;this.user=user;this.password=password;this.tenant=tenant;
    }
    Connection connection()throws SQLException {
        var c=DriverManager.getConnection(url,user,password);c.setAutoCommit(false);
        try(var q=c.prepareStatement("SELECT set_config('olo.tenant',?,true)")){q.setString(1,tenant);q.execute();}
        return c;
    }
    void validate()throws SQLException {
        try(var c=connection();var q=c.createStatement();var rows=q.executeQuery("SELECT format FROM sandbox_schema")) {
            if(!rows.next()||!rows.getString(1).equals("olo-business-sandbox-1")||rows.next())
                throw new SQLException("Unsupported sandbox store; a fresh current schema is required");
            try(var check=c.createStatement()) {
                check.executeQuery("SELECT tenant_id,kind,subject,allowed_actor,prerequisites_complete,generation,state FROM sandbox_subjects WHERE false").close();
                check.executeQuery("SELECT tenant_id,operation,subject,reference,request_digest,result_json FROM sandbox_receipts WHERE false").close();
                check.executeQuery("SELECT tenant_id,subject,task,state FROM sandbox_tasks WHERE false").close();
                check.executeQuery("SELECT tenant_id,effect_id,authorization_generation,auth_nonce,request_digest,ownership_generation,claim_token,state,result_digest,authorization_expires_at FROM toolgate_target_claims WHERE false").close();
                check.executeQuery("SELECT tenant_id,effect_id,auth_nonce,claim_token,outcome,result_digest,delivered FROM toolgate_target_outcomes WHERE false").close();
                try(var marker=check.executeQuery("SELECT format FROM toolgate_target_schema")) {
                    if(!marker.next()||!marker.getString(1).equals("toolgate-target-claims-v1")||marker.next())
                        throw new SQLException("Unsupported target claim identity");
                }
            }
        }
    }
    void initialize(JsonNode seed)throws Exception {
        if(!seed.isObject()||seed.size()!=2||!seed.path("tenantId").asText().equals(tenant)||!seed.path("subjects").isArray()
                ||seed.path("subjects").size()>100)throw new IllegalArgumentException("Invalid current sandbox seed");
        try(var c=connection()) {
            try(var q=c.createStatement();var rows=q.executeQuery("SELECT count(*) FROM information_schema.tables WHERE table_schema='public' AND table_type='BASE TABLE'")) {
                rows.next();if(rows.getInt(1)!=0)throw new SQLException("Initialization requires an empty fresh database");
            }
            JdbcEffectClaims.initialize(c);
            try(var q=c.createStatement()) {
                q.executeUpdate("CREATE TABLE sandbox_schema(format VARCHAR(64) PRIMARY KEY CHECK(format='olo-business-sandbox-1'))");
                q.executeUpdate("INSERT INTO sandbox_schema VALUES('olo-business-sandbox-1')");
                q.executeUpdate("CREATE TABLE sandbox_subjects(tenant_id VARCHAR(128) NOT NULL,kind VARCHAR(16) NOT NULL CHECK(kind IN ('service','joiner')),subject VARCHAR(64) NOT NULL,allowed_actor VARCHAR(128) NOT NULL,prerequisites_complete BOOLEAN NOT NULL,generation BIGINT NOT NULL DEFAULT 0,state VARCHAR(32) NOT NULL,PRIMARY KEY(tenant_id,kind,subject))");
                q.executeUpdate("CREATE TABLE sandbox_tasks(tenant_id VARCHAR(128) NOT NULL,subject VARCHAR(64) NOT NULL,task VARCHAR(32) NOT NULL,state VARCHAR(32) NOT NULL,PRIMARY KEY(tenant_id,subject,task))");
                q.executeUpdate("CREATE TABLE sandbox_receipts(tenant_id VARCHAR(128) NOT NULL,operation VARCHAR(16) NOT NULL,subject VARCHAR(64) NOT NULL,reference VARCHAR(64) NOT NULL,request_digest VARCHAR(71) NOT NULL,result_json TEXT NOT NULL,PRIMARY KEY(tenant_id,operation,subject,reference))");
                for(String table:List.of("sandbox_subjects","sandbox_tasks","sandbox_receipts","toolgate_target_claims","toolgate_target_outcomes")) {
                    q.executeUpdate("ALTER TABLE "+table+" ENABLE ROW LEVEL SECURITY");
                    q.executeUpdate("ALTER TABLE "+table+" FORCE ROW LEVEL SECURITY");
                    q.executeUpdate("CREATE POLICY tenant_scope ON "+table+" USING (tenant_id=current_setting('olo.tenant',true)) WITH CHECK (tenant_id=current_setting('olo.tenant',true))");
                }
            }
            for(var item:seed.path("subjects")) {
                if(!item.isObject()||item.size()!=4||!Set.of("service","joiner").contains(item.path("kind").asText())
                    ||!item.path("id").asText().matches("[A-Za-z0-9_-]{1,64}")
                    ||!item.path("allowedActor").asText().matches("[A-Za-z0-9._:@-]{1,128}")
                    ||!item.path("prerequisitesComplete").isBoolean())throw new IllegalArgumentException("Invalid sandbox subject seed");
                try(var q=c.prepareStatement("INSERT INTO sandbox_subjects(tenant_id,kind,subject,allowed_actor,prerequisites_complete,generation,state) VALUES(?,?,?,?,?,0,?)")) {
                    q.setString(1,tenant);q.setString(2,item.path("kind").asText());q.setString(3,item.path("id").asText());
                    q.setString(4,item.path("allowedActor").asText());q.setBoolean(5,item.path("prerequisitesComplete").asBoolean());
                    q.setString(6,item.path("kind").asText().equals("service")?"STOPPED":"PENDING");q.executeUpdate();
                }
            }
            c.commit();
        }
    }
    record Request(String tenantId,String actorId,String operation,String subject,String reference) {
        void validate(String tenant,boolean read) {
            if(!tenant.equals(tenantId)||actorId==null||!actorId.matches("[A-Za-z0-9._:@-]{1,128}")
                ||subject==null||!subject.matches("[A-Za-z0-9_-]{1,64}")||reference==null||!reference.matches("[A-Za-z0-9_-]{1,64}")
                ||operation==null||!(read?Set.of("service","joiner"):Set.of("restart","provision")).contains(operation))throw new SecurityException("Sandbox request refused");
        }
    }
    record Result(String operation,String subject,String reference,String outcome,String state,long generation,List<String> tasks,List<String> pending) {}
    Result act(Connection c,Request request,String digest)throws Exception {
        request.validate(tenant,false);
        String kind=request.operation.equals("restart")?"service":"joiner";
        // Lock the subject before inspecting the business deduplication row: different references serialize too.
        Subject subject=subject(c,kind,request.subject,request.actorId,true);
        var previous=receipt(c,request,digest);
        if(previous!=null)return previous;
        Result result;
        if(kind.equals("service")) {
            try(var q=c.prepareStatement("UPDATE sandbox_subjects SET generation=generation+1,state='RUNNING' WHERE tenant_id=? AND kind='service' AND subject=?")) {
                q.setString(1,tenant);q.setString(2,request.subject);q.executeUpdate();
            }
            result=new Result("restart",request.subject,request.reference,"APPLIED","RUNNING",subject.generation+1,List.of(),List.of());
        } else if(!subject.complete) {
            result=new Result("provision",request.subject,request.reference,"PENDING","PENDING",0,List.of(),List.of("Operator must verify joining documents before provisioning"));
        } else {
            int inserted=0;
            for(String task:List.of("identity","equipment","payroll"))try(var q=c.prepareStatement("INSERT INTO sandbox_tasks(tenant_id,subject,task,state) VALUES(?,?,?,'CREATED') ON CONFLICT DO NOTHING")) {
                q.setString(1,tenant);q.setString(2,request.subject);q.setString(3,task);inserted+=q.executeUpdate();
            }
            try(var q=c.prepareStatement("UPDATE sandbox_subjects SET state='PROVISIONED' WHERE tenant_id=? AND kind='joiner' AND subject=?")) {
                q.setString(1,tenant);q.setString(2,request.subject);q.executeUpdate();
            }
            result=new Result("provision",request.subject,request.reference,inserted==0?"DUPLICATE":"APPLIED","PROVISIONED",0,tasks(c,request.subject),List.of());
        }
        try(var q=c.prepareStatement("INSERT INTO sandbox_receipts VALUES(?,?,?,?,?,?)")) {
            q.setString(1,tenant);q.setString(2,request.operation);q.setString(3,request.subject);q.setString(4,request.reference);q.setString(5,digest);q.setString(6,JSON.writeValueAsString(result));q.executeUpdate();
        }
        return result;
    }
    Result inspect(Connection c,Request request)throws Exception {
        request.validate(tenant,true);
        Subject subject=subject(c,request.operation,request.subject,request.actorId,false);
        String action=request.operation.equals("service")?"restart":"provision";
        try(var q=c.prepareStatement("SELECT result_json FROM sandbox_receipts WHERE tenant_id=? AND operation=? AND subject=? AND reference=?")) {
            q.setString(1,tenant);q.setString(2,action);q.setString(3,request.subject);q.setString(4,request.reference);
            try(var row=q.executeQuery()){if(row.next())return JSON.readValue(row.getString(1),Result.class);}
        }
        return new Result(request.operation,request.subject,request.reference,request.reference.equals("status")?"STATUS":"UNKNOWN",subject.state,subject.generation,
            request.operation.equals("joiner")?tasks(c,request.subject):List.of(),
            List.of("No receipt for this reference: uncertain effects require independent reconciliation; do not replay"));
    }
    private Result receipt(Connection c,Request request,String digest)throws Exception {
        try(var q=c.prepareStatement("SELECT request_digest,result_json FROM sandbox_receipts WHERE tenant_id=? AND operation=? AND subject=? AND reference=?")) {
            q.setString(1,tenant);q.setString(2,request.operation);q.setString(3,request.subject);q.setString(4,request.reference);
            try(var row=q.executeQuery()) {
                if(!row.next())return null;
                if(!digest.equals(row.getString(1)))throw new SecurityException("Reference reused with different business input");
                return JSON.readValue(row.getString(2),Result.class);
            }
        }
    }
    private record Subject(boolean complete,long generation,String state) {}
    private Subject subject(Connection c,String kind,String subject,String actor,boolean lock)throws SQLException {
        try(var q=c.prepareStatement("SELECT prerequisites_complete,generation,state,allowed_actor FROM sandbox_subjects WHERE tenant_id=? AND kind=? AND subject=?"+(lock?" FOR UPDATE":""))) {
            q.setString(1,tenant);q.setString(2,kind);q.setString(3,subject);
            try(var rows=q.executeQuery()) {
                if(!rows.next()||!actor.equals(rows.getString(4)))throw new SecurityException("Sandbox native ACL refused");
                return new Subject(rows.getBoolean(1),rows.getLong(2),rows.getString(3));
            }
        }
    }
    private List<String> tasks(Connection c,String subject)throws SQLException {
        var result=new ArrayList<String>();
        try(var q=c.prepareStatement("SELECT task FROM sandbox_tasks WHERE tenant_id=? AND subject=? ORDER BY task")) {
            q.setString(1,tenant);q.setString(2,subject);try(var rows=q.executeQuery()){while(rows.next())result.add(rows.getString(1));}
        }
        return List.copyOf(result);
    }
}
