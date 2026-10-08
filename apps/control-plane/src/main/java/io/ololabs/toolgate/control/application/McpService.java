// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
package io.ololabs.toolgate.control.application;

import io.ololabs.toolgate.control.domain.Ids;
import io.ololabs.toolgate.contracts.*;
import java.time.Clock;
import java.util.*;

/** Durable Gateway-to-client relay. Device/agent scopes and exact arguments are rechecked at every handoff. */
public final class McpService {
    private final Store store;private final Codec codec;private final EndpointService endpoints;private final Clock clock;private final Ids.TenantId tenant;private final String server;
    public McpService(Store store,Codec codec,EndpointService endpoints,Clock clock,Ids.TenantId tenant,String server){this.store=store;this.codec=codec;this.endpoints=endpoints;this.clock=clock;this.tenant=tenant;this.server=server;}
    private Failure forbidden(){return new Failure(ErrorCode.FORBIDDEN,403,"Local tool request rejected");}
    private long now(Store.Session tx){long now=clock.millis();if(now<0||tx.approvalClock(now)>now)throw Failure.unavailable();return now;}
    private void gateway(DirectoryService.Actor actor,boolean gateway){if(!gateway||actor.admin()||!actor.tenant().equals(tenant))throw forbidden();}
    private Store.Reply reply(Object value){return new Store.Reply(200,codec.json(value),0);}
    private RemoteToolRecord record(McpStore.Row row){return codec.model(row.record(),RemoteToolRecord.class);}
    private RemoteToolTask task(McpStore.Row row){return codec.model(row.task(),RemoteToolTask.class);}
    private EndpointDeviceRecord device(Store.Session tx,RequestContext context){
        if(!tenant.value().equals(context.tenantId())||context.deviceId()==null)throw forbidden();
        var row=tx.endpoint(context.deviceId());if(row==null)throw forbidden();var device=codec.model(row.document(),EndpointDeviceRecord.class);endpoints.active(tx,device);
        var agent=tx.load().entries().get(Ids.Kind.AGENT.id(context.agentId()));
        if(!device.userId().equals(context.userId())||agent==null||!agent.enabled())throw forbidden();return device;
    }
    private EndpointPermissionConfiguration permissions(Store.Session tx,EndpointDeviceRecord device){
        var saved=tx.endpointConfiguration(device.deviceId());
        new EndpointPermissions(codec).poll(tx,device,server,saved==null?null:saved.acknowledgedDigest(),null);
        return codec.model(tx.endpointConfiguration(device.deviceId()).document(),EndpointPermissionConfiguration.class);
    }
    private boolean visible(EndpointPermissionConfiguration configuration,String agent,String tool,String action){
        var rules=configuration.permissions().stream().filter(r->r.toolId().equals(tool)&&r.action().equals(action)&&(r.agentIds().isEmpty()||r.agentIds().contains(agent))).toList();
        return rules.stream().noneMatch(r->r.decision()==Decision.BLOCK)&&rules.stream().anyMatch(r->r.decision()==Decision.ALLOW||r.decision()==Decision.ASK);
    }
    private void allowed(Store.Session tx,RemoteToolTask task){allowed(tx,task,task.input().resource());}
    private void allowed(Store.Session tx,RemoteToolTask task,ResourceDescriptor resource){
        var device=device(tx,task.input().context());var configuration=permissions(tx,device);
        var rules=configuration.permissions().stream().filter(r->r.toolId().equals(task.request().toolId())&&r.action().equals(task.request().action())&&r.resource().equals(resource)&&(r.agentIds().isEmpty()||r.agentIds().contains(task.input().context().agentId()))).toList();
        if(rules.stream().anyMatch(r->r.decision()!=Decision.ALLOW)||rules.stream().noneMatch(r->r.decision()==Decision.ALLOW))throw forbidden();
        var local=codec.model(tx.endpointConfiguration(device.deviceId()).localTools(),LocalToolCatalog.class);
        if(local.tools().stream().noneMatch(t->t.enabled()&&t.toolId().equals(task.request().toolId())&&t.action().equals(task.request().action())))throw forbidden();
    }
    public Store.Reply catalog(DirectoryService.Actor actor,boolean gateway,String body){gateway(actor,gateway);var context=codec.model(body,RequestContext.class);
        return store.transaction(tenant,true,tx->{long now=now(tx);if(!tenant.value().equals(context.tenantId()))throw forbidden();if(context.deviceId()==null||tx.endpoint(context.deviceId())==null)return reply(new LocalToolCatalog(List.of()));var device=device(tx,context);if(device.lastSeenUnixMs()==0||now-device.lastSeenUnixMs()>120000)return reply(new LocalToolCatalog(List.of()));
            var configuration=permissions(tx,device);var local=codec.model(tx.endpointConfiguration(device.deviceId()).localTools(),LocalToolCatalog.class);
            return reply(new LocalToolCatalog(local.tools().stream().filter(t->t.enabled()&&visible(configuration,context.agentId(),t.toolId(),t.action())).toList()));});}
    public Store.Reply submit(DirectoryService.Actor actor,boolean gateway,String body,String requestId){gateway(actor,gateway);var submission=codec.model(body,RemoteToolSubmission.class);var input=submission.input();
        if(!input.toolId().equals(submission.request().toolId())||!input.action().equals(submission.request().action()))throw forbidden();
        return store.transaction(tenant,true,tx->{long now=now(tx);var previous=tx.mcp().get(input.context().requestId());
            if(previous!=null){var old=task(previous);if(!old.input().equals(input)||!old.request().equals(submission.request()))throw Failure.conflict();return reply(new RemoteToolResponse(record(previous),null));}
            var device=device(tx,input.context());if(device.lastSeenUnixMs()==0||now-device.lastSeenUnixMs()>120000)throw Failure.unavailable();
            if(submission.expiresAtUnixMs()<=now||submission.expiresAtUnixMs()>now+30000)throw Failure.validation();
            long expires=device.connectionExpiresAtUnixMs()==null?submission.expiresAtUnixMs():Math.min(submission.expiresAtUnixMs(),device.connectionExpiresAtUnixMs());
            var task=new RemoteToolTask(input.context().requestId(),UUID.randomUUID().toString(),input,submission.request(),expires);allowed(tx,task);
            if(codec.json(task).getBytes(java.nio.charset.StandardCharsets.UTF_8).length>16384)throw Failure.validation();
            tx.mcp().prune(Math.max(0,now-86400000));if(tx.mcp().count()>=1000||tx.mcp().pending(device.deviceId(),17).size()>=16)throw Failure.conflict();
            var record=new RemoteToolRecord(task.requestId(),device.deviceId(),input.context().agentId(),input.toolId(),RemoteToolState.WAITING_FOR_POLL,now,task.expiresAtUnixMs(),null,null,null,null);
            tx.mcp().save(new McpStore.Row(record.requestId(),record.deviceId(),record.state().name(),record.expiresAtUnixMs(),codec.json(record),codec.json(task),null));
            tx.audit(actor.id(),"MCP_RECEIVED",record.requestId(),1,requestId,input.argumentsDigest());return reply(new RemoteToolResponse(record,null));});}
    private RemoteToolRecord transition(Store.Session tx,McpStore.Row row,RemoteToolState state,long now,ErrorCode error,String result,String requestId){
        var old=record(row);var next=new RemoteToolRecord(old.requestId(),old.deviceId(),old.agentId(),old.toolId(),state,old.receivedAtUnixMs(),old.expiresAtUnixMs(),state==RemoteToolState.SUBMITTED?Long.valueOf(now):old.submittedAtUnixMs(),state==RemoteToolState.RESPONSE_RECEIVED?Long.valueOf(now):old.responseAtUnixMs(),state==RemoteToolState.DONE||state==RemoteToolState.FAILED||state==RemoteToolState.EXPIRED?Long.valueOf(now):old.completedAtUnixMs(),error);
        tx.mcp().save(new McpStore.Row(row.id(),row.deviceId(),state.name(),row.expiresAt(),codec.json(next),row.task(),result));
        tx.audit("mcp-relay","MCP_"+(state==RemoteToolState.SUBMITTED?"DISPATCHED":state==RemoteToolState.RESPONSE_RECEIVED?"RESULT":state==RemoteToolState.DONE?"DONE":"FAILED"),row.id(),1,requestId,task(row).input().argumentsDigest());return next;
    }
    public RemoteToolTask poll(Store.Session tx,EndpointDeviceRecord device,long now,String requestId){
        for(var row:tx.mcp().pending(device.deviceId(),16)){
            if(row.expiresAt()<=now){transition(tx,row,RemoteToolState.EXPIRED,now,ErrorCode.TIMEOUT,null,requestId);continue;}
            var task=task(row);try{allowed(tx,task);}catch(Failure revoked){if(revoked.status()!=403)throw revoked;transition(tx,row,RemoteToolState.FAILED,now,ErrorCode.FORBIDDEN,null,requestId);continue;}
            if(record(row).state()==RemoteToolState.WAITING_FOR_POLL)transition(tx,row,RemoteToolState.SUBMITTED,now,null,null,requestId);
            return task;
        }return null;
    }
    public Store.Reply authorize(java.security.cert.X509Certificate peer,String body){var request=codec.model(body,RemoteToolAuthorization.class);
        return store.transaction(tenant,true,tx->{long now=now(tx);var device=endpoints.authenticate(tx,peer,now);var row=tx.mcp().get(request.requestId());if(row==null||!row.deviceId().equals(device.deviceId())||!row.state().equals("SUBMITTED")||row.expiresAt()<=now)throw forbidden();var task=task(row);
            if(!task.leaseId().equals(request.leaseId())||!task.request().toolId().equals(request.request().toolId())||!task.request().action().equals(request.request().action()))throw forbidden();
            var arguments=request.request().arguments();var expected=new HashMap<>(task.request().arguments());
            var resource=task.input().resource();
            if(arguments.get("input")!=null&&arguments.get("path")!=null&&arguments.get("runtimeImage")!=null){
                if(!arguments.get("input").equals(codec.value(codec.json(task.request().arguments())))||!arguments.get("path").asText().equals(resource.locator())||arguments.keySet().stream().anyMatch(k->!List.of("input","path","runtimeImage","registrationDigest").contains(k)))throw forbidden();
            }else{
                expected.putIfAbsent("path",new com.fasterxml.jackson.databind.node.TextNode("hotfolder"));
                if(!expected.equals(arguments)){
                    var destination=expected.get("destination");var source=expected.get("path");
                    if(destination==null||source==null)throw forbidden();expected.put("path",destination);expected.put("source",source);
                    if(!expected.equals(arguments))throw forbidden();resource=new ResourceDescriptor(resource.kind(),destination.asText());
                }
            }
            allowed(tx,task,resource);return reply(new RemoteToolAuthorizationAck(task.expiresAtUnixMs()));});}
    public Store.Reply result(java.security.cert.X509Certificate peer,String body,String requestId){var result=codec.model(body,RemoteToolResult.class);if((result.output()==null)==(result.error()==null))throw Failure.validation();
        if(codec.json(result).getBytes(java.nio.charset.StandardCharsets.UTF_8).length>65536)throw Failure.validation();
        return store.transaction(tenant,true,tx->{long now=now(tx);var device=endpoints.authenticate(tx,peer,now);var row=tx.mcp().get(result.requestId());if(row==null||!row.deviceId().equals(device.deviceId())||!task(row).leaseId().equals(result.leaseId()))throw forbidden();
            if(row.result()!=null){if(!codec.model(row.result(),RemoteToolResult.class).equals(result))throw Failure.conflict();return reply(record(row));}
            if(!row.state().equals("SUBMITTED")||row.expiresAt()<=now)throw Failure.conflict();
            var record=transition(tx,row,RemoteToolState.RESPONSE_RECEIVED,now,result.error(),codec.json(result),requestId);return reply(record);});}
    public Store.Reply response(DirectoryService.Actor actor,boolean gateway,String body,String requestId){gateway(actor,gateway);var context=codec.model(body,RequestContext.class);
        return store.transaction(tenant,true,tx->{long now=now(tx);var row=tx.mcp().get(context.requestId());if(row==null)throw forbidden();var task=task(row);if(!task.input().context().equals(context))throw forbidden();device(tx,context);var record=record(row);
            if(record.state()==RemoteToolState.RESPONSE_RECEIVED){try{allowed(tx,task);}catch(Failure revoked){if(revoked.status()!=403)throw revoked;record=transition(tx,row,RemoteToolState.FAILED,now,ErrorCode.FORBIDDEN,null,requestId);return reply(new RemoteToolResponse(record,null));}
                record=transition(tx,row,record.error()==null?RemoteToolState.DONE:RemoteToolState.FAILED,now,record.error(),row.result(),requestId);}
            else if((record.state()==RemoteToolState.WAITING_FOR_POLL||record.state()==RemoteToolState.SUBMITTED)&&row.expiresAt()<=now)record=transition(tx,row,RemoteToolState.EXPIRED,now,ErrorCode.TIMEOUT,null,requestId);
            if(record.state()==RemoteToolState.DONE)allowed(tx,task);
            return reply(new RemoteToolResponse(record,record.state()==RemoteToolState.DONE||record.state()==RemoteToolState.FAILED?row.result()==null?null:codec.model(row.result(),RemoteToolResult.class):null));});}
    public Store.Reply page(DirectoryService.Actor actor,String requestId){actor.requireAdmin();if(!actor.tenant().equals(tenant))throw forbidden();return store.transaction(tenant,true,tx->{long now=now(tx);var items=new ArrayList<RemoteToolRecord>();for(var row:tx.mcp().page(100)){var record=record(row);if((record.state()==RemoteToolState.WAITING_FOR_POLL||record.state()==RemoteToolState.SUBMITTED)&&row.expiresAt()<=now)record=transition(tx,row,RemoteToolState.EXPIRED,now,ErrorCode.TIMEOUT,null,requestId);items.add(record);}return reply(new RemoteToolPage(items));});}
}
