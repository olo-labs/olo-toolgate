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

/** Admin authoring and direct mTLS client routes. Uploaded code remains data in Control. */
@Path("/api/control/v1/builder") @Produces("application/json") @Blocking @PermitAll
public class BuilderResource {
    @Inject BuilderService service;@Inject JsonWebToken jwt;@Inject RoutingContext routing;@Inject Correlation correlation;@Inject io.micrometer.core.instrument.MeterRegistry metrics;
    private void tls(){if(!routing.request().isSSL())throw new Failure(ErrorCode.UNAUTHORIZED,401,"Direct TLS required");}
    private java.security.cert.X509Certificate peer(){tls();try{var peers=routing.request().sslSession().getPeerCertificates();if(peers.length==0)throw new IllegalArgumentException();return (java.security.cert.X509Certificate)peers[0];}catch(Exception failure){throw new Failure(ErrorCode.UNAUTHORIZED,401,"Device certificate required");}}
    private DirectoryService.Actor actor(){tls();Object tenant=jwt.getClaim("tenant_id");long now=java.time.Instant.now().getEpochSecond();
        if(!(tenant instanceof String value)||jwt.getSubject()==null||jwt.getIssuedAtTime()>now||jwt.getIssuedAtTime()<now-900||jwt.getExpirationTime()<=now||jwt.getExpirationTime()-jwt.getIssuedAtTime()>900||jwt.getExpirationTime()<=jwt.getIssuedAtTime())throw new Failure(ErrorCode.UNAUTHORIZED,401,"Valid user identity required");
        try{return new DirectoryService.Actor(new Ids.TenantId(value),DirectoryService.digest(jwt.getIssuer()+"\n"+jwt.getSubject()),jwt.getGroups().contains("toolgate-admin"));}catch(IllegalArgumentException failure){throw new Failure(ErrorCode.UNAUTHORIZED,401,"Valid tenant required");}}
    private String body(String value){if(value==null||value.getBytes(java.nio.charset.StandardCharsets.UTF_8).length>65536)throw Failure.validation();return value;}
    private Response response(Store.Reply reply,String operation){metrics.counter("toolgate_control_builder_operations_total","operation",operation).increment();return Response.status(reply.status()).header("Cache-Control","no-store").header("ETag","\""+reply.revision()+"\"").entity(reply.body()).build();}
    @GET @Path("drafts") @RolesAllowed("toolgate-admin") public Response drafts(@QueryParam("cursor")String cursor){return response(service.drafts(actor(),cursor),"LIST");}
    @POST @Path("drafts") @Consumes("application/json") @RolesAllowed("toolgate-admin") public Response save(@HeaderParam("Idempotency-Key")String key,String value){return response(service.save(actor(),body(value),key,correlation.id()),"SAVE");}
    @GET @Path("tests") @RolesAllowed("toolgate-admin") public Response tests(@QueryParam("cursor")String cursor){return response(service.tests(actor(),cursor),"TEST_LIST");}
    @POST @Path("tests") @Consumes("application/json") @RolesAllowed("toolgate-admin") public Response test(@HeaderParam("Idempotency-Key")String key,String value){return response(service.test(actor(),body(value),key,correlation.id()),"TEST");}
    @GET @Path("tests/poll") public Response poll(){return response(service.poll(peer(),correlation.id()),"POLL");}
    @POST @Path("tests/results") @Consumes("application/json") public Response result(String value){return response(service.result(peer(),body(value),correlation.id()),"RESULT");}
    @POST @Path("drafts/{id}/seal") @Consumes("application/json") @RolesAllowed("toolgate-admin") public Response seal(@PathParam("id")String id,@HeaderParam("Idempotency-Key")String key,String value){return response(service.seal(actor(),id,body(value),key,correlation.id()),"SEAL");}
    @POST @Path("drafts/{id}/publication") @RolesAllowed("toolgate-admin") public Response publication(@PathParam("id")String id){return response(service.publication(actor(),id,correlation.id()),"PUBLICATION");}
    @POST @Path("drafts/{id}/release") @Consumes("application/json") @RolesAllowed("toolgate-admin") public Response release(@PathParam("id")String id,@HeaderParam("Idempotency-Key")String key,String value){return response(service.publish(actor(),id,body(value),key,correlation.id()),"RELEASE");}
    @POST @Path("drafts/{id}/deploy") @Consumes("application/json") @RolesAllowed("toolgate-admin") public Response deploy(@PathParam("id")String id,@HeaderParam("Idempotency-Key")String key,String value){return response(service.deploy(actor(),id,body(value),key,correlation.id()),"DEPLOY");}
}
