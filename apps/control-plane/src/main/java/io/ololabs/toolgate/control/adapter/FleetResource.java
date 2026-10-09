// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
package io.ololabs.toolgate.control.adapter;

import io.ololabs.toolgate.control.application.*;
import io.ololabs.toolgate.control.domain.Ids;
import io.ololabs.toolgate.contracts.ErrorCode;
import io.smallrye.common.annotation.Blocking;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.security.*;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Response;
import io.vertx.ext.web.RoutingContext;

@Path("/api/control/v1/fleet") @Produces("application/json") @Blocking @PermitAll
public class FleetResource {
    @Inject FleetService service; @Inject ArtifactStore artifacts; @Inject RoutingContext routing;
     @Inject Correlation correlation; @Inject MeterRegistry metrics;
    private void tls(){if(!routing.request().isSSL())throw new Failure(ErrorCode.UNAUTHORIZED,401,"Direct TLS required");}
    private java.security.cert.X509Certificate peer(){
        tls();try{var certificates=routing.request().sslSession().getPeerCertificates();if(certificates.length==0)throw new IllegalArgumentException();return (java.security.cert.X509Certificate)certificates[0];}
        catch(Exception failure){throw new Failure(ErrorCode.UNAUTHORIZED,401,"Device certificate required");}
    }
    @Inject VerifiedActors actors;
    private DirectoryService.Actor actor(){tls();return actors.management();}


    private String body(String body){if(body==null||body.getBytes(java.nio.charset.StandardCharsets.UTF_8).length>131072)throw Failure.validation();return body;}
    private Response response(Store.Reply reply,String operation){metrics.counter("toolgate_control_fleet_operations_total","operation",operation).increment();return Response.status(reply.status()).header("Cache-Control","no-store").header("ETag","\""+reply.revision()+"\"").entity(reply.body()).build();}
    @GET @Path("releases") @io.quarkus.security.Authenticated public Response releases(@QueryParam("cursor")String cursor){return response(service.releases(actor(),cursor),"RELEASE_LIST");}
    @POST @Path("releases") @Consumes("application/json") @io.quarkus.security.Authenticated public Response publish(@HeaderParam("Idempotency-Key")String key,String body){return response(service.publish(actor(),body(body),key,correlation.id()),"PUBLISH");}
    @GET @Path("rollouts") @io.quarkus.security.Authenticated public Response rollouts(@QueryParam("cursor")String cursor){return response(service.rollouts(actor(),cursor),"ROLLOUT_LIST");}
    @POST @Path("rollouts") @Consumes("application/json") @io.quarkus.security.Authenticated public Response assign(@HeaderParam("Idempotency-Key")String key,String body){return response(service.rollout(actor(),body(body),key,correlation.id()),"ASSIGN");}
    @POST @Path("rollouts/{id}/advance") @Consumes("application/json") @io.quarkus.security.Authenticated public Response advance(@PathParam("id")String id,@HeaderParam("Idempotency-Key")String key,String body){return response(service.advance(actor(),id,body(body),key,correlation.id()),"ADVANCE");}
    @GET @Path("desired") public Response desired(){return response(service.desired(peer()),"DESIRED");}
    @POST @Path("artifact-grants") @Consumes("application/json") public Response grant(String body){return response(service.grant(peer(),body(body),correlation.id()),"GRANT");}
    @POST @Path("artifacts/download") @Consumes("application/json") public Response download(String body){var bytes=service.download(peer(),body(body),artifacts);metrics.counter("toolgate_control_fleet_operations_total","operation","DOWNLOAD").increment();return Response.ok(bytes).header("Cache-Control","no-store").build();}
}
