// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
package io.ololabs.toolgate.control.adapter;

import io.ololabs.toolgate.control.application.*;
import io.ololabs.toolgate.control.domain.Ids;
import io.ololabs.toolgate.contracts.ErrorCode;
import io.micrometer.core.instrument.MeterRegistry;
import io.smallrye.common.annotation.Blocking;
import jakarta.annotation.security.*;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.jwt.JsonWebToken;
import io.vertx.ext.web.RoutingContext;

/** Direct TLS peer authentication; proxy headers and request device assertions cannot authenticate. */
@Path("/api/control/v1/endpoint") @Produces("application/json") @Blocking @PermitAll
public class EndpointResource {
    @Inject EndpointService service;
    @Inject RoutingContext routing;
    @Inject JsonWebToken jwt;
    @Inject Correlation correlation;
    @Inject MeterRegistry metrics;
    private void tls(){if(!routing.request().isSSL())throw new Failure(ErrorCode.UNAUTHORIZED,401,"Direct TLS required");}
    private java.security.cert.X509Certificate peer(){
        tls();try{var certificates=routing.request().sslSession().getPeerCertificates();if(certificates.length==0)throw new IllegalArgumentException();return (java.security.cert.X509Certificate)certificates[0];}
        catch(Exception failure){throw new Failure(ErrorCode.UNAUTHORIZED,401,"Device certificate required");}
    }
    private DirectoryService.Actor actor(){
        tls();Object tenant=jwt.getClaim("tenant_id");long now=java.time.Instant.now().getEpochSecond();
        if(!(tenant instanceof String value)||jwt.getSubject()==null||jwt.getIssuedAtTime()>now||jwt.getIssuedAtTime()<now-900
            ||jwt.getExpirationTime()<=now||jwt.getExpirationTime()-jwt.getIssuedAtTime()>900||jwt.getExpirationTime()<=jwt.getIssuedAtTime())throw new Failure(ErrorCode.UNAUTHORIZED,401,"Valid user identity required");
        try{return new DirectoryService.Actor(new Ids.TenantId(value),DirectoryService.digest(jwt.getIssuer()+"\n"+jwt.getSubject()),jwt.getGroups().contains("toolgate-admin"));}
        catch(IllegalArgumentException failure){throw new Failure(ErrorCode.UNAUTHORIZED,401,"Valid tenant required");}
    }
    private String user(){Object user=jwt.getClaim("user_id");if(!(user instanceof String value))throw new Failure(ErrorCode.FORBIDDEN,403,"Enabled directory user required");return value;}
    private String body(String body){if(body==null||body.getBytes(java.nio.charset.StandardCharsets.UTF_8).length>65536)throw Failure.validation();return body;}
    private Response response(Store.Reply reply,String operation){metrics.counter("toolgate_control_endpoint_operations_total","operation",operation).increment();return Response.status(reply.status()).header("Cache-Control","no-store").header("ETag","\""+reply.revision()+"\"").entity(reply.body()).build();}
    @POST @Path("enrollments") @Consumes("application/json") public Response start(String document){tls();return response(service.start(body(document),correlation.id()),"START");}
    @POST @Path("enrollments/poll") @Consumes("application/json") public Response poll(String document){tls();return response(service.poll(body(document),correlation.id()),"POLL");}
    @GET @Path("enrollments/review") @RolesAllowed("toolgate-enroller") public Response review(@QueryParam("code")String code){return response(service.review(actor(),user(),code),"REVIEW");}
    @POST @Path("enrollments/decision") @Consumes("application/json") @RolesAllowed("toolgate-enroller") public Response decide(@HeaderParam("Idempotency-Key")String key,String document){return response(service.decide(actor(),user(),body(document),key,correlation.id()),"DECIDE");}
    @POST @Path("check-in") @Consumes("application/json") public Response checkIn(@HeaderParam("X-ToolGate-Poll-Interval-Unit")String unit,String document){return response(service.checkIn(peer(),body(document),correlation.id(),"milliseconds".equals(unit)),"CHECK_IN");}
    @GET @Path("devices/{id}") @RolesAllowed("toolgate-admin") public Response device(@PathParam("id")String id){return response(service.device(actor(),id),"DEVICE");}
    @POST @Path("devices/{id}/revoke") @Consumes("application/json") @RolesAllowed("toolgate-admin") public Response revoke(@PathParam("id")String id,@HeaderParam("Idempotency-Key")String key,String document){return response(service.revoke(actor(),id,body(document),key,correlation.id()),"REVOKE");}
}
