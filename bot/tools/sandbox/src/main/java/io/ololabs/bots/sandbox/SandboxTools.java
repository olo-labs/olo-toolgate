// Copyright 2026 OLO Labs; SPDX-License-Identifier: Apache-2.0
package io.ololabs.bots.sandbox;

import io.ololabs.toolgate.sdk.annotations.*;
import io.ololabs.toolgate.sdk.context.ToolContext;
import java.util.List;
import java.util.Map;

@ToolPackage(id="olo.sandbox",name="Governed business sandbox",publisher="OLO Labs",
    description="Real state changes within a declared demonstration sandbox; never production accounts or hosts.")
@Destination(id="sandbox",hostSetting="SandboxHost",port=8443)
@Global(name="SandboxHost",type=ValueType.STRING,defaultValue="business-sandbox",
    pattern="^[A-Za-z0-9.-]{1,253}$",description="Operator-owned fenced sandbox DNS authority")
@HttpAuth(id="sandbox-key",type=AuthType.API_KEY,header="X-Api-Key",secret="businesssandboxapikey")
@HttpRoute(id="restart",destination="sandbox",method=HttpMethod.POST,path="/v1/restart",auth="sandbox-key",
    maxRequestBytes=4096,maxResponseBytes=16384,timeoutMs=5000)
@HttpRoute(id="provision",destination="sandbox",method=HttpMethod.POST,path="/v1/provision",auth="sandbox-key",
    maxRequestBytes=4096,maxResponseBytes=16384,timeoutMs=5000)
@HttpRoute(id="inspect",destination="sandbox",method=HttpMethod.POST,path="/v1/inspect",auth="sandbox-key",
    maxRequestBytes=4096,maxResponseBytes=16384,timeoutMs=5000)
public final class SandboxTools {
    public record Result(
        @Param(description="Sandbox operation or status query") String operation,
        @Param(description="Declared sandbox service or joiner ID") String subject,
        @Param(description="Stable caller reference") String reference,
        @Param(description="APPLIED, DUPLICATE, PENDING, UNKNOWN or STATUS; separate from runtime state") String outcome,
        @Param(description="Actual persisted sandbox state") String state,
        @Param(description="Sandbox service generation; zero for a joiner") long generation,
        @Param(description="Persisted sandbox onboarding task IDs") List<String> tasks,
        @Param(description="Source prerequisites or reconciliation work still required") List<String> pending) {}

    @Authorization(action="sandbox.restart",resources="sandbox://{tenant}/service/{serviceId}")
    @ServerTool(name="restart",title="Restart a sandbox service",
        description="Increment the generation and mark running for a seeded sandbox service. Requires current Control authority and an independently reviewed SHARED_SQL fenced target. An uncertain effect must be reconciled, never retried automatically.",
        effect=EffectSafety.NON_IDEMPOTENT,businessKeyArgument="reference",routes="restart",reconciliation=Reconciliation.MANUAL)
    public Result restart(
        @Param(description="Seeded sandbox service ID",minLength=1,maxLength=64,pattern="^[A-Za-z0-9_-]+$") String serviceId,
        @Param(description="Stable incident/change operation reference",minLength=1,maxLength=64,pattern="^[A-Za-z0-9_-]+$") String reference,
        ToolContext context) {
        return send("restart",serviceId,reference,context);
    }

    @Authorization(action="sandbox.provision",resources="sandbox://{tenant}/joiner/{joinerId}")
    @ServerTool(name="provision",title="Provision sandbox onboarding tasks",
        description="Create one task per required sandbox onboarding step for a seeded joiner whose operator-owned document prerequisites are complete. Existing tasks are deduplicated; missing prerequisites remain pending. No real identity, payroll or equipment systems are accessed.",
        effect=EffectSafety.NON_IDEMPOTENT,businessKeyArgument="reference",routes="provision",reconciliation=Reconciliation.MANUAL)
    public Result provision(
        @Param(description="Seeded sandbox joiner ID",minLength=1,maxLength=64,pattern="^[A-Za-z0-9_-]+$") String joinerId,
        @Param(description="Stable joining operation reference",minLength=1,maxLength=64,pattern="^[A-Za-z0-9_-]+$") String reference,
        ToolContext context) {
        return send("provision",joinerId,reference,context);
    }

    @Authorization(action="sandbox.inspect",resources="sandbox://{tenant}/{kind}/{subject}")
    @ServerTool(name="inspect",title="Inspect sandbox state",
        description="Read current sandbox service generation or joiner tasks and a prior reference receipt. A missing receipt cannot establish that an uncertain effect will never apply.",
        effect=EffectSafety.READ_ONLY,stateless=true,routes="inspect")
    public Result inspect(
        @Param(description="Resource kind",enumValues={"service","joiner"}) String kind,
        @Param(description="Seeded sandbox subject",minLength=1,maxLength=64,pattern="^[A-Za-z0-9_-]+$") String subject,
        @Param(description="Prior reference to inspect, or status",minLength=1,maxLength=64,pattern="^[A-Za-z0-9_-]+$") String reference,
        ToolContext context) {
        var invocation=context.invocation();
        if(invocation.agentId().isPresent()||!invocation.delegationChain().isEmpty())throw new SecurityException("Human platform call required");
        if(!invocation.operation().filter("sandbox.inspect"::equals).isPresent())throw new SecurityException("Verified operation required");
        String tenant=invocation.tenantId().orElseThrow(()->new SecurityException("Verified tenant required"));
        String user=invocation.requireUser();
        return context.http().route("inspect").effectKey("inspect").body(Map.of("tenantId",tenant,"actorId",user,"operation",kind,
            "subject",subject,"reference",reference)).send(Result.class);
    }
    private Result send(String operation,String subject,String reference,ToolContext context) {
        var invocation=context.invocation();
        if(invocation.agentId().isPresent()||!invocation.delegationChain().isEmpty())throw new SecurityException("Human platform call required");
        if(!invocation.operation().filter(("sandbox."+operation)::equals).isPresent())throw new SecurityException("Verified operation required");
        String tenant=invocation.tenantId().orElseThrow(()->new SecurityException("Verified tenant required"));
        String user=invocation.requireUser();
        return context.http().route(operation).effectKey(operation).body(Map.of("tenantId",tenant,"actorId",user,"operation",operation,
            "subject",subject,"reference",reference)).send(Result.class);
    }
}
