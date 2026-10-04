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

/** Real retained SQLite transactions and canonical contracts exercise inherited role ceilings. */
final class ManagedRoleTest {
    @TempDir Path directory;
    private final ContractCodec codec=new ContractCodec();
    private final DirectoryService.Actor root=new DirectoryService.Actor(new Ids.TenantId("tenant"),"a".repeat(64),true,true);
    private final DirectoryService.Actor admin=new DirectoryService.Actor(root.tenant(),"b".repeat(64),true);
    private ControlRole role(String id,UserRole portal,RoleDeviceScope scope,List<String> groups) {
        return new ControlRole(id,id,true,1L,portal,new RoleRules(List.of(UserPrivilegeTemplate.IT_CLOUD_ADMIN,UserPrivilegeTemplate.APPROVER),scope,groups,List.of()));
    }
    private void create(DirectoryService service,Ids.Kind kind,String id,Object body) {
        service.mutate(root,kind,id,"CREATE",codec.json(body),0,"create-"+id,"request");
    }
    @Test void roleCrudInheritanceAndScopeNeverEscalateThroughOrdinaryAdmin() throws Exception {
        var store=new PostgresStore(SqliteState.open(directory.resolve("roles.sqlite")),codec);
        var service=new DirectoryService(store,codec,512,1048576);
        create(service,Ids.Kind.USER,"user",new ControlUser("user","User",true,1L,new UserAccess(UserRole.BASIC,List.of(),List.of(),List.of())));
        create(service,Ids.Kind.USER,"other",new ControlUser("other","Other",true,1L,new UserAccess(UserRole.BASIC,List.of(),List.of(),List.of())));
        create(service,Ids.Kind.DEVICE,"device",new ControlDevice("device","Device",true,1L,"user"));
        create(service,Ids.Kind.TEAM,"devices",new ControlTeam("devices","Device group",true,1L,List.of(),List.of("device"),List.of()));
        var scoped=role("scoped",UserRole.BASIC,RoleDeviceScope.GROUPS,List.of("devices"));
        assertEquals(403,assertThrows(Failure.class,()->service.mutate(admin,Ids.Kind.ROLE,"scoped","CREATE",codec.json(scoped),0,"bad-role","request")).status());
        create(service,Ids.Kind.ROLE,"scoped",scoped);
        create(service,Ids.Kind.TEAM,"operators",new ControlTeam("operators","Operators",true,1L,List.of("user"),List.of(),List.of("scoped")));
        var resolver=new RoleResolver(codec);
        store.transaction(root.tenant(),false,tx->{
            var user=codec.model(tx.load().entries().get(Ids.Kind.USER.id("user")).document(),ControlUser.class);
            assertEquals(java.util.Set.of("device"),resolver.devices(tx.load(),user,"tool"));
            assertTrue(resolver.resolve(tx.load(),user).approver());
            assertTrue(resolver.devices(tx.load(),codec.model(tx.load().entries().get(Ids.Kind.USER.id("other")).document(),ControlUser.class),"tool").isEmpty());
            return null;
        });
        var fixtures=new com.fasterxml.jackson.databind.ObjectMapper().readTree(Path.of(System.getProperty("toolgate.fixtures")).toFile());
        var tool=codec.model(codec.json(fixtures.get("ControlTool")),ControlTool.class);
        var resource=codec.model(codec.json(fixtures.get("ResourceDescriptor")),ResourceDescriptor.class);
        create(service,Ids.Kind.TOOL,tool.id(),tool);
        var policy=new ControlPolicy("allow","Allow",true,1L,tool.id(),tool.definition().actions().getFirst().name(),resource,Decision.ALLOW,List.of("user","other"),List.of(),List.of(),List.of());
        create(service,Ids.Kind.POLICY,policy.id(),policy);
        var compiler=new PolicyCompiler(codec);
        var compiled=store.transaction(root.tenant(),false,tx->codec.model(compiler.compile(tx.load()),CompiledPolicy.class));
        assertEquals(1,compiled.rules().size());assertEquals(List.of("user"),compiled.rules().getFirst().userIds());assertEquals(List.of("device"),compiled.rules().getFirst().deviceIds());
        var export=service.export(root,false);var snapshot=codec.model(export.body(),ControlSnapshot.class);
        var imported=codec.json(new ControlImportRequest(snapshot,ControlImportMode.REPLACE,false));
        assertEquals(403,assertThrows(Failure.class,()->service.importConfig(admin,imported,false,export.revision(),"import-escalation","request")).status());
        assertEquals(200,service.importConfig(root,imported,false,export.revision(),"import-root","request").status());
        var escalated=new ControlTeam("operators","Operators",true,1L,List.of("user","other"),List.of(),List.of("scoped"));
        assertEquals(403,assertThrows(Failure.class,()->service.mutate(admin,Ids.Kind.TEAM,"operators","UPDATE",codec.json(escalated),1,"bad-membership","request")).status());
        var input=new ControlUser("other","Other",true,1L,new UserAccess(UserRole.BASIC,List.of(),List.of(),List.of("scoped")));
        assertEquals(403,assertThrows(Failure.class,()->service.mutate(admin,Ids.Kind.USER,"other","UPDATE",codec.json(input),1,"bad-assignment","request")).status());
        assertThrows(Failure.class,()->service.mutate(root,Ids.Kind.ROLE,"scoped","DELETE",null,1,"referenced","request"));
        var disabled=new ControlRole(scoped.id(),scoped.name(),false,1L,scoped.portalRole(),scoped.rules());
        service.mutate(root,Ids.Kind.ROLE,"scoped","UPDATE",codec.json(disabled),1,"disabled","request");
        assertTrue(store.transaction(root.tenant(),false,tx->codec.model(compiler.compile(tx.load()),CompiledPolicy.class)).rules().isEmpty());
        var blocked=new ControlPolicy("deny","Deny",true,1L,tool.id(),policy.action(),resource,Decision.BLOCK,List.of("user","other"),List.of(),List.of(),List.of());
        create(service,Ids.Kind.POLICY,blocked.id(),blocked);
        var denial=store.transaction(root.tenant(),false,tx->codec.model(compiler.compile(tx.load()),CompiledPolicy.class));
        assertEquals(1,denial.rules().size());assertEquals(BundleEffect.BLOCK,denial.rules().getFirst().effect());
        store.transaction(root.tenant(),false,tx->{
            var user=codec.model(tx.load().entries().get(Ids.Kind.USER.id("user")).document(),ControlUser.class);
            assertTrue(resolver.devices(tx.load(),user,"tool").isEmpty()); assertFalse(resolver.resolve(tx.load(),user).approver());return null;
        });
        assertThrows(Failure.class,()->service.mutate(root,Ids.Kind.ROLE,"scoped","UPDATE",codec.json(disabled),1,"stale","request"));
        // Retained-state reopen includes ROLE in the migrated storage constraint.
        var reopened=new DirectoryService(new PostgresStore(SqliteState.open(directory.resolve("roles.sqlite")),codec),codec,512,1048576);
        assertEquals(2,reopened.get(root,Ids.Kind.ROLE,"scoped").revision());
    }
    @Test void allDevicesRequiresExplicitScopeAndPortalRequiresSignedAuthority() {
        var store=new PostgresStore(SqliteState.open(directory.resolve("all.sqlite")),codec);
        var service=new DirectoryService(store,codec,512,1048576);
        create(service,Ids.Kind.ROLE,"all",role("all",UserRole.ADMINISTRATOR,RoleDeviceScope.ALL,List.of()));
        create(service,Ids.Kind.USER,"user",new ControlUser("user","User",true,1L,new UserAccess(UserRole.BASIC,List.of(),List.of(),List.of("all"))));
        service.requirePortal(admin,"user");assertFalse(service.portalSuper(root,"user"));
        assertThrows(Failure.class,()->service.requirePortal(new DirectoryService.Actor(root.tenant(),"c".repeat(64),false),"user"));
        store.transaction(root.tenant(),false,tx->{var user=codec.model(tx.load().entries().get(Ids.Kind.USER.id("user")).document(),ControlUser.class);assertNull(new RoleResolver(codec).devices(tx.load(),user,"tool"));return null;});
        assertThrows(Failure.class,()->codec.entry(Ids.Kind.ROLE,codec.json(role("bad",UserRole.BASIC,RoleDeviceScope.GROUPS,List.of()))));
        assertThrows(Failure.class,()->codec.entry(Ids.Kind.ROLE,codec.json(role("bad",UserRole.BASIC,RoleDeviceScope.ALL,List.of("devices")))));
        assertThrows(Failure.class,()->codec.entry(Ids.Kind.ROLE,codec.json(role("bad",UserRole.BASIC,RoleDeviceScope.NONE,List.of())).replace("IT_CLOUD_ADMIN","ARBITRARY_EXECUTION")));
    }
}
