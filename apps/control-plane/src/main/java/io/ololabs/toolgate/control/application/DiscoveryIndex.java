// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
package io.ololabs.toolgate.control.application;

import io.ololabs.toolgate.control.domain.*;
import io.ololabs.toolgate.control.domain.Ids.Kind;
import io.ololabs.toolgate.contracts.*;
import java.util.*;

/** Read-only metadata probes. Visibility never substitutes for authorizing real extracted arguments. */
public final class DiscoveryIndex {
    private final Codec codec;
    public DiscoveryIndex(Codec codec) {this.codec=codec;}
    public record Eligibility(String toolId,String action,ResourceDescriptor resource,Decision decision) {}
    public List<Eligibility> rules(Ids.TenantId tenant,Directory directory,RequestContext c,long now) {
        var result=new TreeMap<String,Eligibility>();var evaluator=new EnterpriseEvaluator(codec);
        var context=new EnterpriseContext(c.requestId(),c.tenantId(),c.mode(),c.userId(),c.agentId(),c.workloadBindingId(),c.chain(),c.sessionEpoch(),c.credentialEpoch(),c.bindingId(),c.deviceId());
        var resources=new TreeMap<String,ResourceDescriptor>();
        for(var entry:directory.entries().values())if(entry.id().kind()==Kind.GRANT&&entry.enabled())for(var r:codec.model(entry.document(),ControlAccessGrant.class).scope().resources()) {
            var resource=new ResourceDescriptor(r.kind(),r.match()==EnterpriseResourceMatch.ANY?(r.kind()==ResourceKind.URL?"https://discovery.invalid/":"discovery-probe"):r.locator());resources.put(codec.json(resource),resource);
        }
        for(var entry:directory.entries().values())if(entry.id().kind()==Kind.TOOL&&entry.enabled()) {
            var tool=codec.model(entry.document(),ControlTool.class);
            for(var action:tool.definition().actions())for(var resource:resources.values())if(action.resourceKinds().contains(resource.kind())) {
                var input=new EnterpriseEvaluation(context,tool.id(),action.name(),"0".repeat(64),List.of(resource),evaluator.toolDigest(directory,tool),tool.packageDigest(),now,directory.revision(),true,0L,"discovery");
                var decision=evaluator.evaluate(tenant,directory,input,0L);
                if(decision.decision()!=Decision.BLOCK) {var rule=new Eligibility(tool.id(),action.name(),resource,decision.decision());result.put(codec.json(rule),rule);}
            }
        }
        return List.copyOf(result.values());
    }
}
