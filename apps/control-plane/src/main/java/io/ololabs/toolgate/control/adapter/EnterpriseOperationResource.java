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
import io.vertx.ext.web.RoutingContext;

/** Human management, authenticated Gateway and certificate-bound client routes are explicit trust boundaries. */
@Path("/api/control/v1/access") @Produces("application/json") @Blocking
public class EnterpriseOperationResource {
    @Inject EnterpriseOperations operations;@Inject VerifiedActors actors;@Inject ContractCodec codec;
    @Inject EndpointService endpoints;@Inject PostgresStore store;@Inject RoutingContext routing;
    @Inject io.quarkus.security.identity.SecurityIdentity security;
    @POST @Path("human/catalog") @Consumes("application/json") @io.quarkus.security.Authenticated
    public Response humanCatalog(String document){var actor=actors.human();return reply(new HumanRuntime(store,codec,operations,endpoints.relay()).catalog(actor,actors.humanEpoch(),body(document)));}
    @POST @Path("human/invocations") @Consumes("application/json") @io.quarkus.security.Authenticated
    public Response humanInvoke(@HeaderParam("Idempotency-Key")String key,String document){var actor=actors.human();return reply(new HumanRuntime(store,codec,operations,endpoints.relay()).invoke(actor,actors.humanEpoch(),body(document),key));}
    @GET @Path("human/invocations/{id}/result") @io.quarkus.security.Authenticated
    public Response humanResult(@PathParam("id")String id){var actor=actors.human();return reply(new HumanRuntime(store,codec,operations,endpoints.relay()).result(actor,actors.humanEpoch(),id));}
    private DirectoryService.Actor gateway(){return actors.service("toolgate-relay-gateway");}
    private DirectoryService.Actor reader(){return security.hasRole("toolgate-relay-gateway")?gateway():actors.human();}
    private Response reply(Store.Reply value){return Response.status(value.status()).header("Cache-Control","no-store").header("ETag","\""+value.revision()+"\"").entity(value.body()).build();}
    private String body(String value){if(value==null||value.getBytes(java.nio.charset.StandardCharsets.UTF_8).length>65536)throw Failure.validation();return value;}
    private java.security.cert.X509Certificate peer(){if(!routing.request().isSSL())throw unauthorized();try{return (java.security.cert.X509Certificate)routing.request().sslSession().getPeerCertificates()[0];}catch(Exception invalid){throw unauthorized();}}
    private Ids.TenantId tenant(){return new Ids.TenantId(org.eclipse.microprofile.config.ConfigProvider.getConfig().getValue("toolgate.control.endpoint.tenant-id",String.class));}
    private static Failure unauthorized(){return new Failure(ErrorCode.UNAUTHORIZED,401,"Direct device certificate required");}
    @POST @Path("invocations") @Consumes("application/json") @RolesAllowed("toolgate-relay-gateway")
    public Response submit(String document){return reply(operations.submit(gateway(),body(document)));}
    @POST @Path("invocations/reserve") @Consumes("application/json") @RolesAllowed("toolgate-relay-gateway")
    public Response reserve(String document){return reply(operations.reserve(gateway(),body(document)));}
    @GET @Path("invocations/{id}") @io.quarkus.security.Authenticated
    public Response invocation(@PathParam("id")String id){return reply(operations.get(reader(),id));}
    @POST @Path("invocations/{id}/cancel") @io.quarkus.security.Authenticated
    public Response cancel(@PathParam("id")String id,@HeaderParam("If-Match")String etag){if(etag==null||!etag.matches("\"[1-9][0-9]{0,15}\""))throw Failure.validation();long revision=Long.parseLong(etag.substring(1,etag.length()-1));if(revision>9007199254740991L)throw Failure.validation();return reply(operations.cancel(actors.human(),id,revision));}
    @POST @Path("permits/consume") @Consumes("application/json") @PermitAll
    public Response consume(String document){var certificate=peer();return reply(operations.consume(tenant(),tx->endpoints.authenticate(tx,certificate,System.currentTimeMillis()).deviceId(),body(document)));}
    @POST @Path("effects/report") @Consumes("application/json") @PermitAll
    public Response report(String document){var certificate=peer();return reply(operations.report(tenant(),tx->endpoints.authenticate(tx,certificate,System.currentTimeMillis()).deviceId(),body(document)));}
}
