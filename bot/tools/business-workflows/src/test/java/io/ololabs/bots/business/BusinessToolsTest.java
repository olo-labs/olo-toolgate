// Copyright 2026 OLO Labs; SPDX-License-Identifier: Apache-2.0
package io.ololabs.bots.business;

import static org.junit.jupiter.api.Assertions.*;
import io.ololabs.toolgate.sdk.context.*;
import java.lang.reflect.*;
import java.util.*;
import org.junit.jupiter.api.Test;

final class BusinessToolsTest {
    private final List<String> sent=new ArrayList<>();
    private Map<?,?> body;
    private final Deque<BusinessTools.JobStatus> replies=new ArrayDeque<>();
    private String failureRoute;
    private static <T>T proxy(Class<T> type,InvocationHandler handler) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(),new Class<?>[]{type},handler));
    }
    private ToolContext context(String tenant,String returnedId,boolean refuse) {
        var invocation=proxy(Invocation.class,(o,m,a)->{
            assertEquals("tenantId",m.getName());return Optional.ofNullable(tenant);
        });
        var http=proxy(HttpClient.class,(o,m,a)->{
            assertEquals("route",m.getName());String route=(String)a[0];
            return proxy(RouteCall.class,(call,method,args)->switch(method.getName()) {
                case "body"->{body=(Map<?,?>)args[0];yield call;}
                case "effectKey"->{assertEquals(route.equals("submit")?"document.submit":"document.result."+(sent.size()-1),args[0]);yield call;}
                case "send"->{
                    sent.add(route);
                    if(refuse||route.equals(failureRoute))throw new IllegalStateException("Target refused or unavailable");
                    assertEquals(BusinessTools.JobStatus.class,args[0]);
                    if(!replies.isEmpty())yield replies.removeFirst();
                    yield new BusinessTools.JobStatus("READY",returnedId,"sha256:"+"a".repeat(64),new BusinessTools.Result(
                        new BusinessTools.Report("Processed",List.of(),"",List.of("Review")),List.of()));
                }
                default->throw new AssertionError(method.getName());
            });
        });
        return proxy(ToolContext.class,(o,m,a)->switch(m.getName()) {
            case "invocation"->invocation;case "http"->http;default->throw new AssertionError(m.getName());
        });
    }
    @Test void exact_admitted_intake_and_verified_tenant_reach_one_reviewable_route() {
        var result=new BusinessTools().processIntake("ocr","intake-1",context("tenant-a","intake-1",false));
        assertEquals(List.of("submit"),sent);
        assertEquals(Map.of("tenantId","tenant-a","useCase","ocr","intakeId","intake-1"),body);
        assertEquals("intake-1",result.intakeId());
    }
    @Test void unavailable_or_refused_target_is_not_retried() {
        assertThrows(IllegalStateException.class,()->new BusinessTools().processIntake("ocr","intake-1",context("tenant-a","intake-1",true)));
        assertEquals(List.of("submit"),sent);
    }
    @Test void absent_tenant_or_invalid_identifier_cannot_be_repaired_by_input() {
        assertThrows(SecurityException.class,()->new BusinessTools().processIntake("ocr","intake-1",context(null,"intake-1",false)));
        assertThrows(RuntimeException.class,()->new BusinessTools().processIntake("ocr","../intake",context("tenant-a","intake-1",false)));
        assertThrows(RuntimeException.class,()->new BusinessTools().processIntake("unknown","intake-1",context("tenant-a","intake-1",false)));
        assertTrue(sent.isEmpty());assertNull(body);
    }
    @Test void another_returned_intake_never_becomes_success() {
        assertThrows(RuntimeException.class,()->new BusinessTools().processIntake("ocr","intake-1",context("tenant-a","other-intake",false)));
        assertEquals(List.of("submit"),sent);
    }
    @Test void pending_job_is_observed_under_a_distinct_effect_without_repeating_submission() {
        replies.add(new BusinessTools.JobStatus("QUEUED","intake-1","sha256:"+"a".repeat(64),null));
        var result=new BusinessTools().processIntake("ocr","intake-1",context("tenant-a","intake-1",false));
        assertEquals(List.of("submit","result"),sent);assertEquals("intake-1",result.intakeId());
    }
    @Test void uncertain_result_access_does_not_repeat_it_or_resubmit_the_job() {
        replies.add(new BusinessTools.JobStatus("RUNNING","intake-1","sha256:"+"a".repeat(64),null));failureRoute="result";
        assertThrows(IllegalStateException.class,()->new BusinessTools().processIntake("ocr","intake-1",context("tenant-a","intake-1",false)));
        assertEquals(List.of("submit","result"),sent);
    }
    @Test void changed_manifest_during_collection_stops_before_releasing_a_report() {
        replies.add(new BusinessTools.JobStatus("QUEUED","intake-1","sha256:"+"a".repeat(64),null));
        replies.add(new BusinessTools.JobStatus("RUNNING","intake-1","sha256:"+"b".repeat(64),null));
        assertThrows(RuntimeException.class,()->new BusinessTools().processIntake("ocr","intake-1",context("tenant-a","intake-1",false)));
        assertEquals(List.of("submit","result"),sent);
    }
}
