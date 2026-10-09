// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
package io.ololabs.toolgate.control;
import io.ololabs.toolgate.control.application.*;
import io.ololabs.toolgate.control.domain.*;
import io.ololabs.toolgate.contracts.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class EnterpriseConfigurationTest {
    @TempDir java.nio.file.Path temp;EnterpriseAdministrationTest fixture;ConfigurationChanges changes;DirectoryService.Actor requester,checker;
    @BeforeEach void setup(){fixture=new EnterpriseAdministrationTest();fixture.temp=temp;fixture.seedReviewedRecovery();var codec=fixture.graph.codec;
        fixture.store.transaction(EnterpriseConformanceTest.TENANT,true,tx->{var before=tx.load();var entries=new HashMap<>(before.entries());var r=codec.model(entries.get(Ids.Kind.ROLE.id("recovery-role")).document(),ControlRole.class);var rules=r.managementRules().stream().map(rule->{var actions=new ArrayList<>(rule.actions());actions.add("approve-configuration");return new EnterpriseManagementRule(actions,rule.groupType(),rule.groups(),rule.grantableScopes(),rule.conditions());}).toList();entries.put(Ids.Kind.ROLE.id(r.id()),codec.entry(Ids.Kind.ROLE,codec.json(new ControlRole(r.id(),r.name(),true,2L,r.portalRole(),r.roleType(),rules))));entries.put(Ids.Kind.TEAM.id("recovery-team"),codec.entry(Ids.Kind.TEAM,codec.json(new ControlTeam("recovery-team","Reviewers",true,2L,List.of("admin","alice"),List.of("recovery-role")))));tx.save(before,new Directory(before.revision()+1,entries));return null;});
        requester=fixture.directory.portalActor(EnterpriseConformanceTest.TENANT,"a".repeat(64),"admin");checker=fixture.directory.portalActor(EnterpriseConformanceTest.TENANT,"b".repeat(64),"alice");changes=new ConfigurationChanges(fixture.store,codec,Clock.systemUTC(),512,1048576);
    }
    EnterpriseConfigurationChange propose(){var c=new EnterpriseConfigurationCommand(EnterpriseConfigurationOperation.CREATE,ControlEntityKind.USER,"pending-user",fixture.graph.codec.json(new ControlUser("pending-user","Pending",false,1L)),0L);return fixture.graph.codec.model(changes.create(requester,c,"proposal-key","request").body(),EnterpriseConfigurationChange.class);}
    EnterpriseConfigurationChange transition(DirectoryService.Actor actor,EnterpriseConfigurationChange c,String action){return fixture.graph.codec.model(changes.transition(actor,c.id(),fixture.graph.codec.json(new EnterpriseConfigurationTransition(c.revision(),EnterpriseConfigurationAction.valueOf(action))),UUID.randomUUID().toString(),"request").body(),EnterpriseConfigurationChange.class);}
    @Test void makerCheckerPreviewAndAtomicApply(){var c=propose();assertEquals(EnterpriseConfigurationState.DRAFT,c.state());assertTrue(c.affectedIndividuals().contains("USER:pending-user"));fixture.store.transaction(requester.tenant(),false,tx->{assertNull(tx.load().entries().get(Ids.Kind.USER.id("pending-user")));return null;});c=transition(requester,c,"SUBMIT");var pending=c;assertThrows(Failure.class,()->transition(requester,pending,"APPROVE"));c=transition(checker,c,"APPROVE");assertEquals(EnterpriseConfigurationState.APPROVED,c.state());c=transition(requester,c,"APPLY");assertEquals(EnterpriseConfigurationState.APPLIED,c.state());fixture.store.transaction(requester.tenant(),false,tx->{assertEquals(List.of("team-default"),GroupGraph.memberships(tx.load(),fixture.graph.codec,Ids.Kind.USER,"pending-user",false));assertTrue(tx.auditPage(0,100).contains("CONFIGURATION_APPLY"));return null;});assertEquals(c.id(),propose().id());}
    @Test void concurrentGraphChangeInvalidatesReview(){var c=transition(requester,propose(),"SUBMIT");fixture.directory.mutate(requester,Ids.Kind.USER,"other","CREATE",fixture.graph.codec.json(new ControlUser("other","Other",false,1L)),0,"other-key","request");assertEquals(409,assertThrows(Failure.class,()->transition(checker,c,"APPROVE")).status());}
    @Test void revokedReviewerCannotApplyOrReplay(){var c=transition(checker,transition(requester,propose(),"SUBMIT"),"APPROVE");fixture.store.transaction(requester.tenant(),true,tx->{var before=tx.load();var entries=new HashMap<>(before.entries());entries.put(Ids.Kind.TEAM.id("recovery-team"),fixture.graph.codec.entry(Ids.Kind.TEAM,fixture.graph.codec.json(new ControlTeam("recovery-team","Reviewers",true,3L,List.of("admin"),List.of("recovery-role")))));tx.save(before,new Directory(before.revision()+1,entries));return null;});assertThrows(Failure.class,()->transition(requester,c,"APPLY"));}
    @Test void cancellationNeverMutatesDirectory(){var c=transition(requester,propose(),"CANCEL");assertEquals(EnterpriseConfigurationState.CANCELLED,c.state());assertThrows(Failure.class,()->transition(requester,c,"SUBMIT"));}
}
