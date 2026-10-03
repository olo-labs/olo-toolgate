// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
package io.ololabs.toolgate.control;

import io.ololabs.toolgate.control.adapter.*;
import io.ololabs.toolgate.control.application.*;
import io.ololabs.toolgate.control.domain.Ids;
import io.ololabs.toolgate.contracts.*;
import com.fasterxml.jackson.databind.*;
import java.nio.file.Path;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class BuilderTest {
    private final ContractCodec codec=new ContractCodec();
    private BuilderDefinition definition()throws Exception {
        return codec.model(new ObjectMapper().readTree(Path.of(System.getProperty("toolgate.fixtures")).toFile()).get("BuilderDefinition").toString(),BuilderDefinition.class);
    }
    @Test void schemasSecretsPermissionsResourcesAndCredentialBoundary()throws Exception {
        var definition=definition();assertEquals("custom-echo",BuilderValidation.validate(codec,definition,true).packageId());
        for(var text:List.of("api_key = '"+"fixture-sensitive-value"+"'","-----BEGIN "+"PRIVATE KEY-----","ghp_"+"x".repeat(36)))assertThrows(Failure.class,()->BuilderValidation.scan(text));
        BuilderValidation.scan("credentialRequirements use named references; no values");
        for(var field:List.of("permissions","resource","credentialRequirements","tool")) {
            var tree=(com.fasterxml.jackson.databind.node.ObjectNode)new ObjectMapper().readTree(codec.json(definition));
            if(field.equals("permissions"))tree.putArray(field).add("FILE_READ");
            if(field.equals("resource"))((com.fasterxml.jackson.databind.node.ObjectNode)tree.get(field)).put("locator","runtime/another-tool");
            if(field.equals("credentialRequirements"))tree.putArray(field).add("secret://provider");
            if(field.equals("tool"))((com.fasterxml.jackson.databind.node.ObjectNode)tree.get(field).get("inputSchema")).put("$ref","https://example.invalid/schema");
            assertThrows(Failure.class,()->BuilderValidation.validate(codec,codec.model(tree.toString(),BuilderDefinition.class),true),field);
        }
        var tree=(com.fasterxml.jackson.databind.node.ObjectNode)new ObjectMapper().readTree(codec.json(definition));
        ((com.fasterxml.jackson.databind.node.ObjectNode)tree.get("tool").get("source")).put("sha256","0".repeat(64));
        assertThrows(Failure.class,()->BuilderValidation.validate(codec,codec.model(tree.toString(),BuilderDefinition.class),true));
        assertThrows(Failure.class,()->codec.model(codec.json(definition).replace("\"COMPUTE\"","\"ROOT\""),BuilderDefinition.class));
    }
    @Test void durableRevisionRaceSealingAndDatabaseImmutability()throws Exception {
        var base=Objects.requireNonNull(System.getenv("CONTROL_TEST_URL"));var password=Objects.requireNonNull(System.getenv("CONTROL_TEST_PASSWORD"));var database="builder_"+UUID.randomUUID().toString().replace("-","");
        try(var connection=java.sql.DriverManager.getConnection(base,"control_migrator",password);var statement=connection.createStatement()){statement.execute("CREATE DATABASE "+database);}
        var url=base.replace("/control?","/"+database+"?");var migrations=org.flywaydb.core.Flyway.configure().dataSource(url,"control_migrator",password).load();migrations.migrate();migrations.validate();
        var source=new org.postgresql.ds.PGSimpleDataSource();source.setURL(url);source.setUser("control_app");source.setPassword(password);
        var store=new PostgresStore(source,codec);var tenant=new Ids.TenantId("builder");var admin=new DirectoryService.Actor(tenant,"a".repeat(64),true);
        var service=new BuilderService(store,codec,null,null,null,Clock.fixed(Instant.ofEpochMilli(1700000000000L),ZoneOffset.UTC),true,"builder","server");
        var definition=definition();var create=codec.json(new BuilderDraftRequest("draft",0L,definition));var first=service.save(admin,create,"create","request");assertEquals(first,service.save(admin,create,"create","request"));
        assertThrows(Failure.class,()->service.save(new DirectoryService.Actor(tenant,"b".repeat(64),false),create,"wrong-role","request"));
        assertThrows(Failure.class,()->service.drafts(new DirectoryService.Actor(new Ids.TenantId("other"),"a".repeat(64),true),null));
        assertThrows(Failure.class,()->service.seal(admin,"draft",codec.json(new BuilderRevisionRequest(1L)),"untested","request"));
        var pool=java.util.concurrent.Executors.newFixedThreadPool(2);
        try {
            var jobs=new ArrayList<java.util.concurrent.Future<Boolean>>();
            for(var key:List.of("update-a","update-b"))jobs.add(pool.submit(()->{try{service.save(admin,codec.json(new BuilderDraftRequest("draft",1L,definition)),key,"request");return true;}catch(Failure failure){assertEquals(409,failure.status());return false;}}));
            int winners=0;for(var job:jobs)if(job.get())winners++;assertEquals(1,winners);
        }finally{pool.shutdownNow();}
        // Domain seal consumes a durable designated-client result; real leased execution is covered by the TLS E2E.
        var current=codec.model(service.drafts(admin,null).body(),BuilderDraftPage.class).items().getFirst();
        store.transaction(tenant,true,tx->{var passed=new BuilderTestRecord("passed","draft",current.definitionDigest(),"device",BuilderTestState.PASSED,3L,1700000000000L,1700000060000L,1L,"lease",0L,null);tx.builder().save(BuilderStore.Kind.TEST,new BuilderStore.Row("passed",3,codec.json(passed)),0);return null;});
        var sealed=codec.model(service.seal(admin,"draft",codec.json(new BuilderRevisionRequest(2L)),"seal","request").body(),BuilderDraft.class);assertTrue(sealed.sealed());
        assertThrows(Failure.class,()->service.save(admin,codec.json(new BuilderDraftRequest("draft",3L,definition)),"edit-sealed","request"));
        assertEquals(definition,codec.model(service.publication(admin,"draft","export").body(),BuilderDefinition.class));
        try(var connection=source.getConnection();var statement=connection.createStatement()) {
            assertThrows(java.sql.SQLException.class,()->statement.executeUpdate("UPDATE control_builder SET revision=revision+1 WHERE kind='VERSION'"));
            assertThrows(java.sql.SQLException.class,()->statement.executeUpdate("UPDATE control_builder SET revision=revision+1 WHERE kind='DRAFT'"));
            assertThrows(java.sql.SQLException.class,()->statement.executeUpdate("DELETE FROM control_builder WHERE kind='VERSION'"));
        }
    }
}
