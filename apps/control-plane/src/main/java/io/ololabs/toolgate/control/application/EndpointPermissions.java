// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
package io.ololabs.toolgate.control.application;

import io.ololabs.toolgate.contracts.*;
import java.util.*;

/** Durable per-device discovery state; a cached permission never authorizes execution. */
public final class EndpointPermissions {
    private final Codec codec;
    public EndpointPermissions(Codec codec) { this.codec=codec; }
    public EndpointPermissionConfiguration poll(Store.Session tx, EndpointDeviceRecord device, String server, String acknowledged, List<BuiltinToolInfo> localTools) {
        var directory=tx.load();
        var saved=tx.endpointConfiguration(device.deviceId());
        EndpointPermissionConfiguration configuration=saved==null?null:codec.model(saved.document(),EndpointPermissionConfiguration.class);
        if(saved==null || saved.sourceRevision()!=directory.revision()) {
            var compiled=new PolicyCompiler(codec).compile(directory);
            List<ApprovalBundleRule> rules;
            try {
                rules=codec.model(compiled,CompiledPolicy.class).rules().stream().map(r->new ApprovalBundleRule(r.policyId(),r.userIds(),r.agentIds(),r.deviceIds(),r.toolId(),r.action(),r.resource(),r.graceAllowed(),Decision.valueOf(r.effect().name()))).toList();
            } catch(Failure versionTwo) {
                rules=codec.model(compiled,ApprovalCompiledPolicy.class).rules();
            }
            var scopes=new TreeMap<String,EndpointPermissionRule>();
            for(var rule:rules) {
                if(!rule.userIds().isEmpty()&&!rule.userIds().contains(device.userId()) || !rule.deviceIds().isEmpty()&&!rule.deviceIds().contains(device.deviceId()))continue;
                var permission=new EndpointPermissionRule(rule.toolId(),rule.action(),rule.agentIds(),rule.resource(),rule.effect());
                scopes.put(codec.json(permission),permission);
            }
            var permissions=List.copyOf(scopes.values());
            var digest=DirectoryService.digest(codec.json(permissions));
            if(configuration==null || !configuration.digest().equals(digest)) {
                long revision=configuration==null?1:Math.addExact(configuration.revision(),1);
                configuration=new EndpointPermissionConfiguration(server,device.deviceId(),device.userId(),revision,digest,permissions);
            }
        }
        var document=codec.json(configuration);
        if(document.getBytes(java.nio.charset.StandardCharsets.UTF_8).length>98304)throw Failure.unavailable();
        codec.model(document,EndpointPermissionConfiguration.class);
        var received=configuration.digest().equals(acknowledged)?acknowledged:null;
        var catalog=localTools==null?(saved==null?codec.json(new LocalToolCatalog(List.of())):saved.localTools()):codec.json(new LocalToolCatalog(localTools));
        if(catalog.getBytes(java.nio.charset.StandardCharsets.UTF_8).length>49152)throw Failure.validation();
        if(saved==null || saved.sourceRevision()!=directory.revision() || !Objects.equals(saved.acknowledgedDigest(),received) || !Objects.equals(saved.localTools(),catalog))
            tx.saveEndpointConfiguration(new Store.EndpointConfiguration(device.deviceId(),directory.revision(),document,received,catalog));
        return received==null?configuration:null;
    }
}
