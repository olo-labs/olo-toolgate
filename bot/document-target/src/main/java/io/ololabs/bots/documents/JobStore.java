// Copyright 2026 OLO Labs; SPDX-License-Identifier: Apache-2.0
package io.ololabs.bots.documents;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.json.JsonMapper;
import io.ololabs.toolgate.service.JdbcEffectClaims;
import java.net.URI;
import java.net.http.*;
import java.nio.file.*;
import java.sql.*;
import java.time.Duration;
import java.util.*;

/** Fenced job submission and result-access receipts; background computation has no business effects. */
final class JobStore {
    static final JsonMapper JSON=JsonMapper.builder()
        .enable(com.fasterxml.jackson.core.StreamReadFeature.STRICT_DUPLICATE_DETECTION)
        .enable(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_TRAILING_TOKENS).build();
    final String url,user,password,tenant;
    private HttpClient http;private URI processor;private byte[] apiKey;
    JobStore(String url,String user,String password,String tenant) {
        if(!url.startsWith("jdbc:postgresql:")||!tenant.matches("[A-Za-z0-9_-]{1,128}"))throw new IllegalArgumentException();
        this.url=url;this.user=user;this.password=password;this.tenant=tenant;
    }
    void processor(HttpClient http,URI processor,byte[] key) {
        if(!"https".equals(processor.getScheme())||!"business-extractor".equals(processor.getHost())||processor.getPort()!=8443
            ||processor.getRawUserInfo()!=null||processor.getRawQuery()!=null||processor.getRawFragment()!=null
            ||!processor.getRawPath().isEmpty()||http.followRedirects()!=HttpClient.Redirect.NEVER||key.length<32||key.length>512)
            throw new IllegalArgumentException("Reviewed private extractor origin required");
        this.http=http;this.processor=processor;this.apiKey=key.clone();
    }
    Connection connection()throws SQLException {
        var c=DriverManager.getConnection(url,user,password);c.setAutoCommit(false);
        try(var q=c.createStatement()){q.execute("SET LOCAL lock_timeout='750ms'");q.execute("SET LOCAL statement_timeout='1000ms'");}
        try(var q=c.prepareStatement("SELECT set_config('olo.tenant',?,true)")){q.setString(1,tenant);q.execute();}
        return c;
    }
    void initialize()throws SQLException {
        try(var c=connection()) {
            try(var q=c.createStatement();var rows=q.executeQuery("SELECT count(*) FROM information_schema.tables WHERE table_schema='public' AND table_type='BASE TABLE'")) {
                rows.next();if(rows.getInt(1)!=0)throw new SQLException("Fresh empty document-job database required");
            }
            JdbcEffectClaims.initialize(c);
            try(var q=c.createStatement()) {
                q.executeUpdate("CREATE TABLE document_job_schema(format VARCHAR(64) PRIMARY KEY CHECK(format='olo-document-jobs-1'))");
                q.executeUpdate("INSERT INTO document_job_schema VALUES('olo-document-jobs-1')");
                q.executeUpdate("CREATE TABLE document_jobs(tenant_id VARCHAR(128) NOT NULL,use_case VARCHAR(32) NOT NULL,intake_id VARCHAR(96) NOT NULL,manifest_sha256 VARCHAR(71) NOT NULL,state VARCHAR(16) NOT NULL CHECK(state IN ('QUEUED','RUNNING','READY','FAILED','INTERRUPTED')),owner VARCHAR(64),lease_until BIGINT,result_json TEXT,PRIMARY KEY(tenant_id,use_case,intake_id))");
                q.executeUpdate("CREATE TABLE document_job_receipts(tenant_id VARCHAR(128) NOT NULL,auth_nonce VARCHAR(128) NOT NULL,operation VARCHAR(16) NOT NULL,use_case VARCHAR(32) NOT NULL,intake_id VARCHAR(96) NOT NULL,manifest_sha256 VARCHAR(71) NOT NULL,result_json TEXT NOT NULL,PRIMARY KEY(tenant_id,auth_nonce))");
                for(String table:List.of("document_jobs","document_job_receipts","toolgate_target_claims","toolgate_target_outcomes")) {
                    q.executeUpdate("ALTER TABLE "+table+" ENABLE ROW LEVEL SECURITY");q.executeUpdate("ALTER TABLE "+table+" FORCE ROW LEVEL SECURITY");
                    q.executeUpdate("CREATE POLICY tenant_rows ON "+table+" USING(tenant_id=current_setting('olo.tenant',true)) WITH CHECK(tenant_id=current_setting('olo.tenant',true))");
                }
            }
            c.commit();
        }
    }
    void validate()throws SQLException {
        try(var c=connection();var q=c.createStatement();var rows=q.executeQuery("SELECT format FROM document_job_schema")) {
            if(!rows.next()||!rows.getString(1).equals("olo-document-jobs-1")||rows.next())throw new SQLException("Unsupported document-job store");
            try(var check=c.createStatement()) {
                check.executeQuery("SELECT tenant_id,use_case,intake_id,manifest_sha256,state,owner,lease_until,result_json FROM document_jobs WHERE false").close();
                check.executeQuery("SELECT tenant_id,auth_nonce,operation,use_case,intake_id,manifest_sha256,result_json FROM document_job_receipts WHERE false").close();
                check.executeQuery("SELECT tenant_id,effect_id,authorization_generation,auth_nonce,request_digest,ownership_generation,claim_token,state,result_digest,authorization_expires_at FROM toolgate_target_claims WHERE false").close();
                check.executeQuery("SELECT tenant_id,effect_id,auth_nonce,claim_token,outcome,result_digest,delivered FROM toolgate_target_outcomes WHERE false").close();
                try(var marker=check.executeQuery("SELECT format FROM toolgate_target_schema")) {
                    if(!marker.next()||!marker.getString(1).equals("toolgate-target-claims-v1")||marker.next())throw new SQLException("Unsupported claim identity");
                }
            }
        }
    }
    record Request(String tenantId,String useCase,String intakeId) {
        void validate(String tenant) {
            if(!tenant.equals(tenantId)||useCase==null||!Set.of("ocr","medical","vlsic","loan","invoice","support","itsm","onboarding").contains(useCase)
                ||intakeId==null||!intakeId.matches("[A-Za-z0-9_-]{1,96}"))throw new SecurityException();
        }
    }
    record Result(String state,String intakeId,String manifestSha256,JsonNode processing) {}
    Result act(Connection c,Request request,String nonce,String operation,String sourceHash)throws Exception {
        request.validate(tenant);
        if(!Set.of("submit","result").contains(operation)||!sourceHash.matches("sha256:[a-f0-9]{64}"))throw new SecurityException();
        // Serialize both duplicate submissions and source-version checks.
        try(var q=c.prepareStatement("SELECT pg_advisory_xact_lock(hashtextextended(?,0))")) {
            q.setString(1,tenant);q.execute();
        }
        String state=null,previousHash=null,data=null;
        try(var q=c.prepareStatement("SELECT manifest_sha256,state,result_json FROM document_jobs WHERE tenant_id=? AND use_case=? AND intake_id=? FOR UPDATE")) {
            bind(q,request);try(var rows=q.executeQuery()){if(rows.next()){previousHash=rows.getString(1);state=rows.getString(2);data=rows.getString(3);}}
        }
        if(state==null) {
            if(!operation.equals("submit"))throw new SecurityException();
            try(var q=c.prepareStatement("SELECT count(*) FROM document_jobs WHERE tenant_id=?")) {
                q.setString(1,tenant);try(var rows=q.executeQuery()){rows.next();if(rows.getInt(1)>=1024)throw new IllegalStateException("Job capacity reached");}
            }
            try(var q=c.prepareStatement("INSERT INTO document_jobs(tenant_id,use_case,intake_id,manifest_sha256,state) VALUES(?,?,?,?,'QUEUED')")) {
                bind(q,request);q.setString(4,sourceHash);q.executeUpdate();
            }
            state="QUEUED";
        } else if(!sourceHash.equals(previousHash))throw new SecurityException("Intake version changed; use a fresh immutable intake");
        var result=new Result(state,request.intakeId,sourceHash,data==null?null:JSON.readTree(data));
        try(var q=c.prepareStatement("INSERT INTO document_job_receipts(tenant_id,auth_nonce,operation,use_case,intake_id,manifest_sha256,result_json) VALUES(?,?,?,?,?,?,?)")) {
            q.setString(1,tenant);q.setString(2,nonce);q.setString(3,operation);q.setString(4,request.useCase);q.setString(5,request.intakeId);
            q.setString(6,sourceHash);q.setString(7,JSON.writeValueAsString(result));q.executeUpdate();
        }
        return result;
    }
    private void bind(PreparedStatement q,Request request)throws SQLException {q.setString(1,tenant);q.setString(2,request.useCase);q.setString(3,request.intakeId);}
    JsonNode fetch(Request request,boolean inspect)throws Exception {
        var body=HttpRequest.BodyPublishers.ofByteArray(JSON.writeValueAsBytes(request));
        var call=HttpRequest.newBuilder(processor.resolve(inspect?"/v1/inspect-intake":"/v1/process-intake"))
            .timeout(Duration.ofSeconds(inspect?1:55)).header("Content-Type","application/json")
            .header("X-Api-Key",new String(apiKey,java.nio.charset.StandardCharsets.US_ASCII)).POST(body).build();
        var pending=http.sendAsync(call,ignored->new BoundedBody());HttpResponse<byte[]> response;
        try {response=pending.get(inspect?1:55,java.util.concurrent.TimeUnit.SECONDS);}
        catch(Exception failure){pending.cancel(true);if(failure instanceof InterruptedException)Thread.currentThread().interrupt();throw failure;}
        byte[] raw=response.body();
        if(response.statusCode()!=200)throw new SecurityException("Extractor refused");
        var value=JSON.readTree(raw);
        if(!value.isObject()||!value.path("intakeId").asText().equals(request.intakeId)
            ||!value.path("manifestSha256").asText().matches("sha256:[a-f0-9]{64}"))throw new SecurityException();
        return value;
    }
    String sourceHash(Request request)throws Exception {return fetch(request,true).path("manifestSha256").asText();}
    /** Claim one committed job. Computation reads sources only; it never replays a target effect. */
    void workOne() {
        Request request;String expected,owner=UUID.randomUUID().toString();
        try(var c=connection();var q=c.prepareStatement("SELECT use_case,intake_id,manifest_sha256 FROM document_jobs WHERE tenant_id=? AND state='QUEUED' ORDER BY intake_id LIMIT 1 FOR UPDATE SKIP LOCKED")) {
            q.setString(1,tenant);
            try(var rows=q.executeQuery()) {if(!rows.next())return;request=new Request(tenant,rows.getString(1),rows.getString(2));expected=rows.getString(3);}
            try(var update=c.prepareStatement("UPDATE document_jobs SET state='RUNNING',owner=?,lease_until=? WHERE tenant_id=? AND use_case=? AND intake_id=? AND state='QUEUED'")) {
                update.setString(1,owner);update.setLong(2,System.currentTimeMillis()+120_000);update.setString(3,tenant);update.setString(4,request.useCase);update.setString(5,request.intakeId);
                if(update.executeUpdate()!=1)throw new SecurityException();
            }
            c.commit();
        } catch(Exception unavailable){return;}
        String state="FAILED",data=null;
        try {
            var value=fetch(request,false);
            if(!value.path("manifestSha256").asText().equals(expected)||!value.path("processing").isObject())throw new SecurityException();
            data=JSON.writeValueAsString(value.get("processing"));state="READY";
        } catch(Exception unavailable) { /* Terminal computation failure; no automatic repetition. */ }
        try(var c=connection();var q=c.prepareStatement("UPDATE document_jobs SET state=?,result_json=? WHERE tenant_id=? AND use_case=? AND intake_id=? AND owner=? AND state='RUNNING' AND lease_until>?")) {
            q.setString(1,state);q.setString(2,data);q.setString(3,tenant);q.setString(4,request.useCase);q.setString(5,request.intakeId);q.setString(6,owner);q.setLong(7,System.currentTimeMillis());q.executeUpdate();c.commit();
        } catch(Exception unavailable){ /* Preserve RUNNING for explicit interrupted-work investigation. */ }
    }
    void expireComputations()throws SQLException {
        try(var c=connection();var q=c.prepareStatement("UPDATE document_jobs SET state='INTERRUPTED' WHERE tenant_id=? AND state='RUNNING' AND lease_until<=?")) {
            q.setString(1,tenant);q.setLong(2,System.currentTimeMillis());q.executeUpdate();c.commit();
        }
    }
    static final class BoundedBody implements HttpResponse.BodySubscriber<byte[]> {
        private final java.util.concurrent.CompletableFuture<byte[]> result=new java.util.concurrent.CompletableFuture<>();
        private final java.io.ByteArrayOutputStream bytes=new java.io.ByteArrayOutputStream();
        private java.util.concurrent.Flow.Subscription subscription;
        public java.util.concurrent.CompletionStage<byte[]> getBody(){return result;}
        public void onSubscribe(java.util.concurrent.Flow.Subscription value){subscription=value;value.request(1);}
        public void onNext(List<java.nio.ByteBuffer> buffers) {
            if(result.isDone())return;
            for(var buffer:buffers) {
                if(buffer.remaining()>1_048_576-bytes.size()) {
                    subscription.cancel();result.completeExceptionally(new java.io.IOException("Extractor response exceeds limit"));return;
                }
                byte[] part=new byte[buffer.remaining()];buffer.get(part);bytes.writeBytes(part);
            }
            subscription.request(1);
        }
        public void onError(Throwable failure){result.completeExceptionally(failure);}
        public void onComplete(){result.complete(bytes.toByteArray());}
    }
}
