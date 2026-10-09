// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
package io.ololabs.toolgate.control.adapter;

import io.ololabs.toolgate.control.application.*;
import io.ololabs.toolgate.control.domain.Ids;
import io.ololabs.toolgate.contracts.ErrorCode;
import io.micrometer.core.instrument.MeterRegistry;
import io.smallrye.common.annotation.Blocking;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Response;

/** Human approval privilege comes from scoped Management Roles and policy reviewer Teams. */
@Path("/api/control/v1/approvals")
@Produces("application/json")
@Blocking
@io.quarkus.security.Authenticated
public class ApprovalResource {
    @Inject EnterpriseOperations service;
    @Inject Correlation correlation;
    @Inject MeterRegistry metrics;
    @Inject VerifiedActors actors;
    private DirectoryService.Actor actor() {return actors.human();}
    private String body(String document) {
        if (document==null || document.getBytes(java.nio.charset.StandardCharsets.UTF_8).length>16384) throw Failure.validation(); return document;
    }
    private Response response(Store.Reply reply,String operation) {
        metrics.counter("toolgate_control_approval_operations_total","operation",operation).increment();
        return Response.status(reply.status()).header("ETag","\""+reply.revision()+"\"").header("Cache-Control","no-store").entity(reply.body()).build();
    }
    @GET
    public Response page(@QueryParam("cursor") String cursor,@QueryParam("limit") @DefaultValue("50") int limit) {
        return response(service.approvals(actor(),cursor,limit),"READ");
    }
    @GET @Path("{id}")
    public Response get(@PathParam("id") String id) { return response(service.approval(actor(),id),"READ"); }
    @POST @Path("{id}/decision") @Consumes("application/json")
    public Response decide(@PathParam("id") String id,@HeaderParam("Idempotency-Key") String key,String document) {
        return response(service.decide(actor(),id,body(document),key),"DECIDE");
    }
}
