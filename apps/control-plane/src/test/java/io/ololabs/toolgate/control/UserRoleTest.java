// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
package io.ololabs.toolgate.control;

import io.ololabs.toolgate.control.adapter.*;
import io.ololabs.toolgate.control.application.*;
import io.ololabs.toolgate.control.domain.Ids;
import io.ololabs.toolgate.contracts.*;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

final class UserRoleTest {
    @TempDir Path directory;
    private final ContractCodec codec=new ContractCodec();
    private final DirectoryService.Actor admin=new DirectoryService.Actor(new Ids.TenantId("tenant"),"a".repeat(64),true);
    private final DirectoryService.Actor root=new DirectoryService.Actor(new Ids.TenantId("tenant"),"b".repeat(64),true,true);
    private String user(String id,UserRole role,boolean enabled,List<UserPrivilegeTemplate> templates,List<String> groups){
        return codec.json(new ControlUser(id,id,enabled,1L,new UserAccess(role,templates,groups,null)));
    }
    @Test void superAdminAuthorityPortalActivationAndLastRootAreEnforced() {
        var store=new PostgresStore(SqliteState.open(directory.resolve("roles.sqlite")),codec);
        var service=new DirectoryService(store,codec,512,1048576);
        String superUser=user("root",UserRole.SUPER_ADMIN,true,List.of(),List.of());
        assertEquals(403,assertThrows(Failure.class,()->service.mutate(admin,Ids.Kind.USER,"root","CREATE",superUser,0,"create-root","request")).status());
        service.mutate(root,Ids.Kind.USER,"root","CREATE",superUser,0,"root","request");
        assertTrue(service.portalSuper(root,"root")); service.requirePortal(root,"root");
        assertThrows(Failure.class,()->service.mutate(admin,Ids.Kind.USER,"root","DELETE",null,1,"delete-root","request"));
        assertThrows(Failure.class,()->service.mutate(root,Ids.Kind.USER,"root","UPDATE",user("root",UserRole.SUPER_ADMIN,false,List.of(),List.of()),1,"disable-root","request"));
        service.mutate(admin,Ids.Kind.USER,"basic","CREATE",user("basic",UserRole.BASIC,true,List.of(),List.of()),0,"basic","request");
        assertEquals(403,assertThrows(Failure.class,()->service.requirePortal(admin,"basic")).status());
        assertThrows(Failure.class,()->service.mutate(admin,Ids.Kind.USER,"basic","UPDATE",user("basic",UserRole.ADMINISTRATOR,true,List.of(),List.of()),1,"escalate","request"));
        service.mutate(root,Ids.Kind.USER,"administrator","CREATE",user("administrator",UserRole.ADMINISTRATOR,true,List.of(),List.of()),0,"admin","request");
        service.requirePortal(admin,"administrator"); assertFalse(service.portalSuper(root,"administrator"));
        service.mutate(root,Ids.Kind.USER,"disabled-admin","CREATE",user("disabled-admin",UserRole.ADMINISTRATOR,false,List.of(),List.of()),0,"disabled","request");
        assertThrows(Failure.class,()->service.requirePortal(root,"disabled-admin"));
        service.mutate(root,Ids.Kind.USER,"second-root","CREATE",user("second-root",UserRole.SUPER_ADMIN,true,List.of(),List.of()),0,"second-root","request");
        var before=store.transaction(root.tenant(),false,tx->tx.load());
        var remaining=new java.util.HashMap<>(before.entries());
        remaining.remove(Ids.Kind.USER.id("root")); remaining.remove(Ids.Kind.USER.id("second-root"));
        String replacement="{\"snapshot\":"+codec.snapshot(root.tenant(),new io.ololabs.toolgate.control.domain.Directory(before.revision(),remaining),false)+",\"mode\":\"REPLACE\",\"dryRun\":false}";
        assertThrows(Failure.class,()->service.importConfig(root,replacement,false,before.revision(),"remove-all-roots","request"));
        assertEquals(before,store.transaction(root.tenant(),false,tx->tx.load()));
    }
    @Test void unknownRolesAndPrivilegeInjectionFailCanonicalValidation() {
        String profile=user("user",UserRole.BASIC,false,List.of(UserPrivilegeTemplate.TOOL_USER),List.of());
        assertThrows(Failure.class,()->codec.entry(Ids.Kind.USER,profile.replace("BASIC","OWNER")));
        assertThrows(Failure.class,()->codec.entry(Ids.Kind.USER,profile.replace("TOOL_USER","EXECUTE_ANY_COMMAND")));
        assertThrows(Failure.class,()->codec.entry(Ids.Kind.USER,profile.replace("\"templateIds\"","\"permissions\"")));
    }
    @Test void cloudTemplatesKeepEachUserBoundToTheirOwnDevicesAndEmptyGroupsDeny() throws Exception {
        var store=new PostgresStore(SqliteState.open(directory.resolve("scopes.sqlite")),codec);
        var service=new DirectoryService(store,codec,512,1048576);
        service.mutate(root,Ids.Kind.USER,"owner","CREATE",user("owner",UserRole.SUPER_ADMIN,true,List.of(),List.of()),0,"owner","request");
        for(String suffix:List.of("a","b")) {
            var device=new ControlDevice("device-"+suffix,"Device",true,1L,"owner");
            service.mutate(admin,Ids.Kind.DEVICE,device.id(),"CREATE",codec.json(device),0,"device-"+suffix,"request");
            var group=new ControlTeam("group-"+suffix,"Group",true,1L,List.of(),List.of(device.id()),null);
            service.mutate(admin,Ids.Kind.TEAM,group.id(),"CREATE",codec.json(group),0,"group-"+suffix,"request");
            service.mutate(root,Ids.Kind.USER,"cloud-"+suffix,"CREATE",user("cloud-"+suffix,UserRole.BASIC,true,List.of(UserPrivilegeTemplate.IT_CLOUD_ADMIN),List.of(group.id())),0,"cloud-"+suffix,"request");
        }
        service.mutate(root,Ids.Kind.USER,"empty-cloud","CREATE",user("empty-cloud",UserRole.BASIC,true,List.of(UserPrivilegeTemplate.IT_CLOUD_ADMIN),List.of()),0,"empty-cloud","request");
        service.mutate(admin,Ids.Kind.USER,"no-template","CREATE",user("no-template",UserRole.BASIC,true,List.of(),List.of()),0,"no-template","request");
        var fixtures=new com.fasterxml.jackson.databind.ObjectMapper().readTree(Path.of(System.getProperty("toolgate.fixtures")).toFile());
        var tool=codec.model(codec.json(fixtures.get("ControlTool")),ControlTool.class);
        service.mutate(admin,Ids.Kind.TOOL,tool.id(),"CREATE",codec.json(tool),0,"tool","request");
        var resource=codec.model(codec.json(fixtures.get("ResourceDescriptor")),ResourceDescriptor.class);
        var policy=new ControlPolicy("cloud-policy","Cloud",true,1L,tool.id(),tool.definition().actions().getFirst().name(),resource,Decision.ALLOW,List.of("cloud-a","cloud-b","empty-cloud","no-template"),List.of(),List.of(),List.of());
        service.mutate(admin,Ids.Kind.POLICY,policy.id(),"CREATE",codec.json(policy),0,"policy","request");
        var compiler=new PolicyCompiler(codec);
        var compiled=store.transaction(admin.tenant(),false,tx->codec.model(compiler.compile(tx.load()),CompiledPolicy.class));
        assertEquals(2,compiled.rules().size());
        for(var rule:compiled.rules()) assertEquals(List.of("device-"+rule.userIds().getFirst().substring(6)),rule.deviceIds());
        var group=codec.model(service.get(admin,Ids.Kind.TEAM,"group-a").body(),ControlTeam.class);
        service.mutate(admin,Ids.Kind.TEAM,group.id(),"UPDATE",codec.json(new ControlTeam(group.id(),group.name(),false,group.revision(),group.userIds(),group.deviceIds(),null)),group.revision(),"disable-group","request");
        compiled=store.transaction(admin.tenant(),false,tx->codec.model(compiler.compile(tx.load()),CompiledPolicy.class));
        assertEquals(List.of("cloud-b"),compiled.rules().getFirst().userIds()); assertEquals(1,compiled.rules().size());
        for(String suffix:List.of("first","second")) {
            var longPolicy=new ControlPolicy("p".repeat(96)+suffix,"Long policy",true,1L,tool.id(),policy.action(),resource,Decision.ALLOW,List.of("cloud-b"),List.of(),List.of(),List.of());
            service.mutate(admin,Ids.Kind.POLICY,longPolicy.id(),"CREATE",codec.json(longPolicy),0,"long-"+suffix,"request");
        }
        compiled=store.transaction(admin.tenant(),false,tx->codec.model(compiler.compile(tx.load()),CompiledPolicy.class));
        assertEquals(3,compiled.rules().stream().map(BundleRule::policyId).distinct().count());
    }
}
