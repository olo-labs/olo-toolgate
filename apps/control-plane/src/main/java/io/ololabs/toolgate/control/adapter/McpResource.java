// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
package io.ololabs.toolgate.control.adapter;

import io.ololabs.toolgate.control.application.*;
import io.ololabs.toolgate.control.domain.Ids;
import io.ololabs.toolgate.contracts.ErrorCode;
import io.smallrye.common.annotation.Blocking;
import jakarta.annotation.security.*;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.jwt.JsonWebToken;
import io.vertx.ext.web.RoutingContext;

/** Dedicated Gateway role and direct client mTLS keep relay assertions out of agent-controlled input. */
@Path("/api/control/v1/mcp") @Produces("application/json") @Blocking @PermitAll
public class McpResource {
    @Inject EndpointService endpoints;@Inject DirectoryService directory;@Inject JsonWebToken jwt;@Inject RoutingContext routing;@Inject Correlation correlation;
    private DirectoryService.Actor actor(){
        long now=java.time.Instant.now().getEpochSecond();Object tenant=jwt.getClaim("tenant_id");
        if(!(tenant instanceof String value)||jwt.getSubject()==null||jwt.getSubject().isBlank()||jwt.getIssuedAtTime()>now||jwt.getIssuedAtTime()<now-900||jwt.getExpirationTime()<=now||jwt.getExpirationTime()<=jwt.getIssuedAtTime()||jwt.getExpirationTime()-jwt.getIssuedAtTime()>900)throw new Failure(ErrorCode.UNAUTHORIZED,401,"Valid identity required");
        try {
            var actor=new DirectoryService.Actor(new Ids.TenantId(value),DirectoryService.digest(jwt.getIssuer()+"\n"+jwt.getSubject()),jwt.getGroups().contains("toolgate-admin"));
            Object user=jwt.getClaim("user_id");if(user!=null&&!(user instanceof String))throw new Failure(ErrorCode.UNAUTHORIZED,401,"Valid identity required");
            if(actor.admin())directory.requirePortal(actor,user instanceof String id?id:null);
            return actor;
        }catch(IllegalArgumentException failure){throw new Failure(ErrorCode.UNAUTHORIZED,401,"Valid identity required");}
    }
    private java.security.cert.X509Certificate peer(){if(!routing.request().isSSL())throw new Failure(ErrorCode.UNAUTHORIZED,401,"Direct TLS required");try{return (java.security.cert.X509Certificate)routing.request().sslSession().getPeerCertificates()[0];}catch(Exception failure){throw new Failure(ErrorCode.UNAUTHORIZED,401,"Device certificate required");}}
    private String body(String value){if(value==null||value.getBytes(java.nio.charset.StandardCharsets.UTF_8).length>65536)throw Failure.validation();return value;}
    private Response response(Store.Reply reply){return Response.status(reply.status()).header("Cache-Control","no-store").entity(reply.body()).build();}
    @POST @Path("catalog") @Consumes("application/json") @RolesAllowed("toolgate-relay-gateway") public Response catalog(String value){return response(endpoints.relay().catalog(actor(),true,body(value)));}
    @POST @Path("requests") @Consumes("application/json") @RolesAllowed("toolgate-relay-gateway") public Response submit(String value){return response(endpoints.relay().submit(actor(),true,body(value),correlation.id()));}
    @POST @Path("responses") @Consumes("application/json") @RolesAllowed("toolgate-relay-gateway") public Response get(String value){return response(endpoints.relay().response(actor(),true,body(value),correlation.id()));}
    @POST @Path("authorize") @Consumes("application/json") public Response authorize(String value){return response(endpoints.relay().authorize(peer(),body(value)));}
    @POST @Path("results") @Consumes("application/json") public Response result(String value){return response(endpoints.relay().result(peer(),body(value),correlation.id()));}
    @GET @Path("requests") @RolesAllowed("toolgate-admin") public Response page(){return response(endpoints.relay().page(actor(),correlation.id()));}
}
