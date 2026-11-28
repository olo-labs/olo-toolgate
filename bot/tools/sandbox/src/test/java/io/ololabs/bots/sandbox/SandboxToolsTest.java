// Copyright 2026 OLO Labs; SPDX-License-Identifier: Apache-2.0
package io.ololabs.bots.sandbox;

import io.ololabs.toolgate.sdk.context.*;
import java.lang.reflect.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SandboxToolsTest {
    Map<?,?> body;String route,key;int sends;
    static <T> T proxy(Class<T> type,InvocationHandler handler) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(),new Class<?>[]{type},handler));
    }
    ToolContext context(String operation,String tenant,String user,String agent,List<String> delegation,boolean refuse) {
        var invocation=proxy(Invocation.class,(object,method,args)->switch(method.getName()) {
            case "operation"->Optional.ofNullable(operation);
            case "tenantId"->Optional.ofNullable(tenant);
            case "userId"->Optional.ofNullable(user);
            case "agentId"->Optional.ofNullable(agent);
            case "delegationChain"->delegation;
            case "requireUser"->InvocationHandler.invokeDefault(object,method,args);
            default->throw new AssertionError(method.getName());
        });
        var call=proxy(RouteCall.class,(object,method,args)->switch(method.getName()) {
            case "effectKey"->{key=(String)args[0];yield object;}
            case "body"->{body=(Map<?,?>)args[0];yield object;}
            case "send"->{sends++;if(refuse)throw new IllegalStateException("OUTCOME_UNKNOWN");yield new SandboxTools.Result(route,"subject","reference","APPLIED","RUNNING",1,List.of(),List.of());}
            default->throw new AssertionError(method.getName());
        });
        var http=proxy(io.ololabs.toolgate.sdk.context.HttpClient.class,(object,method,args)->{route=(String)args[0];return call;});
        return proxy(ToolContext.class,(object,method,args)->switch(method.getName()) {
            case "invocation"->invocation;case "http"->http;default->throw new AssertionError(method.getName());
        });
    }
    @Test void only_verified_user_context_supplies_tenant_actor_and_fixed_operation() {
        var tools=new SandboxTools();
        tools.restart("service-a","incident-a",context("sandbox.restart","tenant-a","user-a",null,List.of(),false));
        assertEquals("restart",route);assertEquals("restart",key);
        assertEquals(Map.of("tenantId","tenant-a","actorId","user-a","operation","restart","subject","service-a","reference","incident-a"),body);
        tools.provision("joiner-a","joining-a",context("sandbox.provision","tenant-a","user-a",null,List.of(),false));
        assertEquals("provision",route);assertEquals("provision",key);assertEquals(2,sends);
        key=null;tools.inspect("joiner","joiner-a","joining-a",context("sandbox.inspect","tenant-a","user-a",null,List.of(),false));
        assertEquals("inspect",route);assertEquals("inspect",key);assertEquals("joiner",body.get("operation"));
    }
    @Test void absent_or_wrong_authority_never_sends() {
        var tools=new SandboxTools();
        for(var ctx:List.of(context("sandbox.restart",null,"user-a",null,List.of(),false),
            context("sandbox.restart","tenant-a",null,null,List.of(),false),
            context("sandbox.inspect","tenant-a","user-a",null,List.of(),false),
            context("sandbox.restart","tenant-a","user-a","agent-a",List.of(),false),
            context("sandbox.restart","tenant-a","user-a",null,List.of("agent-a"),false)))
            assertThrows(RuntimeException.class,()->tools.restart("service-a","reference",ctx));
        assertEquals(0,sends);
    }
    @Test void unknown_target_reply_never_retries_or_becomes_a_success() {
        assertThrows(IllegalStateException.class,()->new SandboxTools().restart("service-a","reference",
            context("sandbox.restart","tenant-a","user-a",null,List.of(),true)));
        assertEquals(1,sends);
    }
    @Test void uncertain_inspection_receipt_is_not_retried() {
        assertThrows(IllegalStateException.class,()->new SandboxTools().inspect("service","service-a","incident-a",
            context("sandbox.inspect","tenant-a","user-a",null,List.of(),true)));
        assertEquals("inspect",key);assertEquals(1,sends);
    }
}
