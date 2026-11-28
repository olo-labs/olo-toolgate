// Copyright 2026 OLO Labs; SPDX-License-Identifier: Apache-2.0
package org.olo.knowledge.packages;

import static org.junit.jupiter.api.Assertions.*;
import io.ololabs.toolgate.sdk.context.*;
import java.lang.reflect.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

final class KnowledgePackageBoundaryTest {
    private static final String ROOT_ID="10000000-0000-4000-8000-000000000001";
    private static final String REQUEST="{\"text\":\"fictional source\",\"userId\":\"not-authority\"}";
    private final String module=System.getProperty("knowledge.package");
    private final AtomicInteger network=new AtomicInteger();
    private Map<?,?> body;
    private String route;
    private String effectKey;

    private static <T> T proxy(Class<T> type,InvocationHandler handler) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(),new Class<?>[]{type},handler));
    }
    private ToolContext context(String operation,String tenant,String user,String agent,List<String> chain,int status) {
        var invocation=proxy(Invocation.class,(object,method,args)->switch(method.getName()) {
            case "tenantId"->Optional.ofNullable(tenant);
            case "userId"->Optional.ofNullable(user);
            case "agentId"->Optional.ofNullable(agent);
            case "operation"->Optional.ofNullable(operation);
            case "delegationChain"->chain;
            case "requireUser"->InvocationHandler.invokeDefault(object,method,args);
            default->throw new AssertionError("Unexpected invocation access: "+method.getName());
        });
        var call=proxy(RouteCall.class,(object,method,args)->switch(method.getName()) {
            case "body"->{body=(Map<?,?>)args[0];yield object;}
            case "effectKey"->{effectKey=(String)args[0];yield object;}
            case "sendRaw"->{
                network.incrementAndGet();
                yield new RawResponse() {
                    public int status(){return status;}
                    public Map<String,List<String>> headers(){return Map.of();}
                    public byte[] body(){return "{\"receipt\":\"synthetic\"}".getBytes(StandardCharsets.UTF_8);}
                };
            }
            default->throw new AssertionError("Unexpected route access: "+method.getName());
        });
        var http=proxy(HttpClient.class,(object,method,args)->{
            assertEquals("route",method.getName());route=(String)args[0];return call;
        });
        return proxy(ToolContext.class,(object,method,args)->switch(method.getName()) {
            case "invocation"->invocation;
            case "http"->http;
            default->throw new AssertionError("Unexpected context access: "+method.getName());
        });
    }
    private Object invoke(String operation,ToolContext context) throws Exception {
        var type=Class.forName("org.olo.knowledge.packages."+(module.equals("rag")?"RagPackage":"MemoryPackage"));
        try {
            return type.getMethod(operation,String.class,String.class,ToolContext.class)
                .invoke(type.getConstructor().newInstance(),ROOT_ID,REQUEST,context);
        } catch(InvocationTargetException error) {
            if(error.getCause() instanceof RuntimeException runtime)throw runtime;
            throw error;
        }
    }
    @Test void every_operation_preserves_exact_body_and_declared_route_for_verified_users() throws Exception {
        var operations=module.equals("rag")?List.of("query","ingest","update","delete","share"):
            List.of("read","append","delete","share","export");
        for(var operation:operations) {
            var result=invoke(operation,context(module+"."+operation,"tenant-a","user-a",null,List.of(),200));
            assertEquals(operation,route);
            if(!Set.of("read","query").contains(operation))assertEquals(module+"."+operation,effectKey);
            assertEquals(Map.of(module.equals("rag")?"kbId":"conversationId",ROOT_ID,"request",REQUEST),body);
            assertEquals("{\"receipt\":\"synthetic\"}",result.getClass().getMethod("json").invoke(result));
        }
        assertEquals(5,network.get());
    }
    @Test void agent_identity_and_delegation_are_rejected_before_any_network_request() {
        var operation=module.equals("rag")?"query":"read";
        for(var context:List.of(
                context(module+"."+operation,"tenant-a","user-a","agent-a",List.of(),200),
                context(module+"."+operation,"tenant-a","user-a",null,List.of("agent-a"),200))) {
            assertThrows(SecurityException.class,()->invoke(operation,context));
        }
        assertEquals(0,network.get());
    }
    @Test void missing_tenant_user_and_wrong_operation_cannot_be_repaired_by_arguments() {
        var operation=module.equals("rag")?"query":"read";
        for(var context:List.of(
                context(module+"."+operation,null,"user-a",null,List.of(),200),
                context(module+"."+operation,"tenant-a",null,null,List.of(),200),
                context("unrelated.operation","tenant-a","user-a",null,List.of(),200))) {
            assertThrows(RuntimeException.class,()->invoke(operation,context));
        }
        assertEquals(0,network.get());
    }
    @Test void refused_target_never_returns_a_success_result_or_retries() {
        var operation=module.equals("rag")?"query":"read";
        assertThrows(IllegalStateException.class,()->invoke(operation,
            context(module+"."+operation,"tenant-a","user-a",null,List.of(),503)));
        assertEquals(1,network.get());
    }
    @Test void only_rag_update_accepts_the_nondelegated_platform_service_principal() throws Exception {
        if(!module.equals("rag")) {
            for(var operation:List.of("read","append","delete","share","export"))
                assertThrows(RuntimeException.class,()->invoke(operation,context("memory."+operation,"tenant-a",null,null,List.of(),200)));
            assertEquals(0,network.get());
            return;
        }
        assertNotNull(invoke("update",context("rag.update","tenant-a",null,null,List.of(),200)));
        assertThrows(SecurityException.class,()->invoke("update",context("rag.update","tenant-a",null,"agent-a",List.of(),200)));
        assertEquals(1,network.get());
    }
}
