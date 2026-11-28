// Copyright 2026 OLO Labs; SPDX-License-Identifier: Apache-2.0
package io.ololabs.bots.documents;

import com.sun.net.httpserver.*;
import io.ololabs.toolgate.contracts.v2.*;
import io.ololabs.toolgate.service.*;
import javax.net.ssl.*;
import java.net.*;
import java.net.http.HttpClient;
import java.nio.file.*;
import java.security.*;
import java.sql.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;

/** Actual document-job mutation target. Control verifies every write, not an input approval flag. */
public final class JobTarget {
    private static final JdbcEffectClaims CLAIMS=new JdbcEffectClaims();
    private static final FencedTarget TARGET=new FencedTarget("document-jobs",FencedTargetClass.SHARED_SQL);
    private final JobStore store;
    private final ControlTargetAuthorization control;
    private final URI origin;
    private final Map<String,HttpRequestFingerprint.Profile> profiles;
    private final byte[] apiKey;
    private JobTarget(JobStore store,ControlTargetAuthorization control,URI origin,
            Map<String,HttpRequestFingerprint.Profile> profiles,byte[] apiKey) {
        this.store=store;this.control=control;this.origin=origin;this.profiles=Map.copyOf(profiles);this.apiKey=apiKey;
    }
    private void handle(HttpsExchange exchange)throws java.io.IOException {
        String stage="framing";
        System.err.println("document_job_request_received");
        try {
            var headers=exchange.getRequestHeaders();
            String path=exchange.getRequestURI().getRawPath();
            if(!exchange.getRequestMethod().equals("POST")||exchange.getRequestURI().getRawQuery()!=null
                ||!Set.of("/v1/submit","/v1/result").contains(path)
                ||!one(headers,"Content-Type").equals("application/json")||headers.containsKey("Transfer-Encoding")
                ||!one(headers,"Content-Length").matches("[0-9]{1,4}")
                ||!MessageDigest.isEqual(one(headers,"X-Api-Key").getBytes(java.nio.charset.StandardCharsets.UTF_8),apiKey)) {
                System.err.println("document_job_refused stage=framing code=REQUEST");
                reply(exchange,403,Map.of("code","FORBIDDEN"));return;
            }
            int size=Integer.parseInt(one(headers,"Content-Length"));
            if(size<1||size>4096)throw new IllegalArgumentException();
            byte[] body=exchange.getRequestBody().readNBytes(size+1);
            if(body.length!=size)throw new IllegalArgumentException();
            stage="request-validation";
            var request=JobStore.JSON.readerFor(JobStore.Request.class)
                .with(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES).readValue(body);
            var input=(JobStore.Request)request;
            input.validate(store.tenant);
            String operation=path.substring("/v1/".length());
            var accepted=new HashMap<String,List<String>>();headers.forEach((name,values)->accepted.put(name,List.copyOf(values)));
            var transport=new ServicePermitVerifier.Transport(profiles.get(operation),origin.resolve(path),"POST",accepted);
            var grant=new SignedEffectGrant(one(headers,"Toolgate-Effect-Permit"));
            stage="grant-verification";
            var verified=control.verifyWrite(grant,transport,body,exchange.getSSLSession());
            JdbcEffectClaims.Owner owner;
            stage="claim";
            try(var c=store.connection()) {
                var acquisition=CLAIMS.claim(c,verified.binding(),verified.expiresAt());
                if(!acquisition.acquired()) {reply(exchange,409,Map.of("code","RECONCILIATION_REQUIRED"));return;}
                owner=acquisition.owner();c.commit();
            }
            stage="online-consumption";
            var authority=control.consumeClaim(verified,owner);
            try(var c=store.connection()) {
                if(!CLAIMS.beginApply(c,owner))throw new SecurityException();c.commit();
            }
            JobStore.Result result;
            stage="job-transaction";
            try(var c=store.connection()) {
                try {
                    authority.checkCommitDeadline();
                    String sourceHash=store.sourceHash(input);
                    result=store.act(c,input,verified.binding().authNonce(),operation,sourceHash);
                    if(!CLAIMS.finish(c,owner,true,digest(JobStore.JSON.writeValueAsBytes(result))))throw new SecurityException();
                    authority.checkCommitDeadline();
                } catch(Exception failure) {
                    c.rollback();
                    try(var failed=store.connection()) {
                        if(!CLAIMS.finish(failed,owner,false,digest("confirmed-rollback".getBytes(java.nio.charset.StandardCharsets.UTF_8))))
                            throw new SecurityException();
                        failed.commit();
                    }
                    throw failure;
                }
                // Any exception from COMMIT is uncertain. Never change APPLYING to FAILED or call act again.
                stage="job-commit";c.commit();
            }
            deliver();reply(exchange,200,result);
        } catch(SecurityException refused) {
            System.err.println("document_job_refused stage="+stage+" code=AUTHORITY");
            reply(exchange,403,Map.of("code","FORBIDDEN"));
        } catch(IllegalArgumentException|com.fasterxml.jackson.core.JacksonException invalid) {
            reply(exchange,400,Map.of("code","VALIDATION"));
        } catch(Exception unresolved) {
            System.err.println("document_job_unresolved stage="+stage+" exception="+unresolved.getClass().getSimpleName());
            reply(exchange,503,Map.of("code","OUTCOME_UNKNOWN","reconciliationRequired",true));
        } finally { exchange.close(); }
    }
    private void deliver() {
        try(var c=store.connection()) {
            for(var outcome:CLAIMS.pendingOutcomes(c,store.tenant,100)) {
                var result=control.reportOutcome(outcome);
                if(result.status().name().equals(outcome.outcome().name()))CLAIMS.acknowledgeOutcome(c,outcome);
            }
            c.commit();
        } catch(Exception pending) {System.err.println("Document-job outcome evidence delivery pending");}
    }
    private void recover() {
        try {store.expireComputations();}catch(Exception pending){System.err.println("Document computation expiry unavailable");}
        try(var c=store.connection()) {
            for(var row:CLAIMS.expiredClaims(c,store.tenant,System.currentTimeMillis(),100))CLAIMS.abandon(c,row);
            c.commit();
        } catch(Exception pending) {System.err.println("Document-job claim evidence recovery pending");}
        deliver();
    }
    static String digest(byte[] raw)throws NoSuchAlgorithmException {
        return "sha256:"+HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(raw));
    }
    private static String one(Headers headers,String name) {
        var values=headers.get(name);if(values==null||values.size()!=1)throw new SecurityException();return values.getFirst();
    }
    private static void reply(HttpsExchange exchange,int status,Object value)throws java.io.IOException {
        byte[] raw=JobStore.JSON.writeValueAsBytes(value);exchange.getResponseHeaders().set("Content-Type","application/json");
        exchange.getResponseHeaders().set("Cache-Control","no-store");exchange.sendResponseHeaders(status,raw.length);
        exchange.getResponseBody().write(raw);
    }
    private static String required(Map<String,String> env,String key) {
        String value=env.get(key);if(value==null||value.isBlank())throw new IllegalArgumentException("Required document-job setting: "+key);return value;
    }
    static Map<String,HttpRequestFingerprint.Profile> profiles(Path file)throws Exception {
        var root=JobStore.JSON.readTree(Files.readAllBytes(file));
        if(root.size()!=3||root.path("schemaVersion").asInt()!=1||!root.path("origin").isTextual()
            ||!root.path("routes").isObject()||root.path("routes").size()!=2)throw new IllegalArgumentException("Invalid trusted target binding");
        var result=new HashMap<String,HttpRequestFingerprint.Profile>();
        for(String name:List.of("submit","result")) {
            var entry=root.path("routes").path(name);
            if(!entry.isObject()||entry.size()!=3||!entry.path("profileId").asText().matches("[A-Za-z0-9][A-Za-z0-9._:-]{0,127}")
                ||!entry.path("profileDigest").asText().matches("sha256:[a-f0-9]{64}")||!entry.path("routeId").asText().equals(name))
                throw new IllegalArgumentException("Invalid trusted route binding");
            // X-Api-Key is the declared broker credential and excluded by the canonical fingerprint algorithm.
            result.put(name,new HttpRequestFingerprint.Profile(entry.path("profileId").asText(),entry.path("profileDigest").asText(),name,"processor-key",List.of("x-api-key")));
        }
        return Map.copyOf(result);
    }
    public static void main(String[] args)throws Exception {
        var env=System.getenv();
        var store=new JobStore(required(env,"DATABASE_URL"),required(env,"DATABASE_USER"),required(env,"DATABASE_PASSWORD"),required(env,"TENANT_ID"));
        if(args.length==1&&args[0].equals("--initialize")) {store.initialize();return;}
        if(args.length!=0)throw new IllegalArgumentException("Unsupported document-job arguments");
        store.validate();
        char[] password=required(env,"TLS_STORE_PASSWORD").toCharArray();
        var keys=KeyStore.getInstance("PKCS12");try(var input=Files.newInputStream(Path.of(required(env,"TLS_KEYSTORE")))){keys.load(input,password);}
        var trust=KeyStore.getInstance("PKCS12");try(var input=Files.newInputStream(Path.of(required(env,"TLS_TRUSTSTORE")))){trust.load(input,password);}
        var km=KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());km.init(keys,password);
        var tm=TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());tm.init(trust);
        var tls=SSLContext.getInstance("TLS");tls.init(km.getKeyManagers(),tm.getTrustManagers(),null);
        // No redirects or plaintext fallback. The listener trusts the installation's executor CA;
        // the verifier additionally binds a write to the exact executor key in the current grant.
        var http=HttpClient.newBuilder().sslContext(tls).connectTimeout(Duration.ofSeconds(2)).followRedirects(HttpClient.Redirect.NEVER).build();
        store.processor(http,URI.create(required(env,"PROCESSOR_ORIGIN")),
            Files.readString(Path.of(required(env,"PROCESSOR_API_KEY_FILE"))).trim().getBytes(java.nio.charset.StandardCharsets.US_ASCII));
        Path credential=Path.of(required(env,"CONTROL_CREDENTIAL_FILE"));
        var control=new ControlTargetAuthorization(http,URI.create(required(env,"CONTROL_ORIGIN")),required(env,"CONTROL_ISSUER"),store.tenant,TARGET,
            ()->{try{return Files.readString(credential).trim();}catch(Exception unavailable){throw new SecurityException();}},Clock.systemUTC());
        Path binding=Path.of(required(env,"TRANSPORT_BINDINGS_FILE"));var config=JobStore.JSON.readTree(Files.readAllBytes(binding));
        URI origin=URI.create(config.path("origin").asText());
        if(!origin.getScheme().equals("https")||origin.getHost()==null||origin.getRawUserInfo()!=null||origin.getRawQuery()!=null
            ||origin.getRawFragment()!=null||!(origin.getRawPath().isEmpty()||origin.getRawPath().equals("/")))throw new IllegalArgumentException("Invalid trusted target origin");
        byte[] api=Files.readString(Path.of(required(env,"API_KEY_FILE"))).trim().getBytes(java.nio.charset.StandardCharsets.UTF_8);
        if(api.length<32||api.length>512)throw new IllegalArgumentException("Private generated document-job API credential required");
        var target=new JobTarget(store,control,origin,profiles(binding),api);
        System.setProperty("sun.net.httpserver.maxReqTime","10");System.setProperty("sun.net.httpserver.maxRspTime","10");
        var server=HttpsServer.create(new InetSocketAddress(8443),16);
        server.setHttpsConfigurator(new HttpsConfigurator(tls) {
            @Override public void configure(HttpsParameters parameters) {
                var ssl=tls.getDefaultSSLParameters();ssl.setProtocols(new String[]{"TLSv1.3","TLSv1.2"});ssl.setWantClientAuth(true);parameters.setSSLParameters(ssl);
            }
        });
        server.createContext("/v1/",exchange->target.handle((HttpsExchange)exchange));
        server.setExecutor(new ThreadPoolExecutor(4,4,0,TimeUnit.SECONDS,new ArrayBlockingQueue<>(16),new ThreadPoolExecutor.AbortPolicy()));
        var recovery=Executors.newSingleThreadScheduledExecutor();recovery.scheduleWithFixedDelay(target::recover,5,5,TimeUnit.SECONDS);
        var workers=Executors.newScheduledThreadPool(2);
        for(int n=0;n<2;n++)workers.scheduleWithFixedDelay(store::workOne,0,200,TimeUnit.MILLISECONDS);
        Runtime.getRuntime().addShutdownHook(new Thread(()->{server.stop(0);recovery.shutdownNow();workers.shutdownNow();}));server.start();
    }
}
