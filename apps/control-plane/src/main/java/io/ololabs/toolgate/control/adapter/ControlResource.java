// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
package io.ololabs.toolgate.control.adapter;

import io.ololabs.toolgate.control.application.DirectoryService;
import io.ololabs.toolgate.control.application.Failure;
import io.ololabs.toolgate.control.application.Store;
import io.ololabs.toolgate.control.domain.Ids.Kind;
import io.ololabs.toolgate.control.domain.Ids.TenantId;
import io.micrometer.core.instrument.MeterRegistry;
import io.smallrye.common.annotation.Blocking;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.jwt.JsonWebToken;

/** Versioned organization management adapter. Directory records never supply authentication roles. */
@Path("/api/control/v1")
@Produces("application/json")
@Blocking
@RolesAllowed({"toolgate-admin", "toolgate-reader"})
public class ControlResource {
    private static final String COLLECTION = "{kind:users|teams|agents|tools|policies|devices}";
    @Inject DirectoryService service;
    @Inject io.ololabs.toolgate.control.application.BundleService bundles;
    @Inject JsonWebToken jwt;
    @Inject Correlation correlation;
    @Inject MeterRegistry metrics;
    private DirectoryService.Actor actor() {
        Object tenant = jwt.getClaim("tenant_id");
        var now = java.time.Instant.now().getEpochSecond();
        if (!(tenant instanceof String value) || jwt.getSubject() == null || jwt.getSubject().isBlank()
                || jwt.getIssuedAtTime() > now || jwt.getIssuedAtTime() < now - 900
                || jwt.getExpirationTime() <= now || jwt.getExpirationTime() <= jwt.getIssuedAtTime()
                || jwt.getExpirationTime() - jwt.getIssuedAtTime() > 900) {
            throw new Failure(io.ololabs.toolgate.contracts.ErrorCode.UNAUTHORIZED, 401, "Invalid identity claims");
        }
        try {
            return new DirectoryService.Actor(new TenantId(value), DirectoryService.digest(jwt.getIssuer() + "\n" + jwt.getSubject()), jwt.getGroups().contains("toolgate-admin"));
        } catch (IllegalArgumentException e) { throw new Failure(io.ololabs.toolgate.contracts.ErrorCode.UNAUTHORIZED, 401, "Invalid tenant identity"); }
    }
    private Response response(Store.Reply reply, String operation, String kind) {
        metrics.counter("toolgate_control_operations_total", "operation", operation, "kind", kind).increment();
        var response = Response.status(reply.status()).header("ETag", "\"" + reply.revision() + "\"");
        if (reply.status() != 204) response.entity(reply.body()); return response.build();
    }
    private long expected(String value) {
        if (value == null || !value.matches("\"(0|[1-9][0-9]{0,15})\"")) throw Failure.validation();
        try { var result = Long.parseLong(value.substring(1, value.length() - 1)); if (result > 9007199254740991L) throw Failure.validation(); return result; }
        catch (NumberFormatException e) { throw Failure.validation(); }
    }
    @GET @Path("bundles/current") @RolesAllowed({"toolgate-admin", "toolgate-reader", "toolgate-bundle-reader"})
    public Response currentBundle() {
        return Response.fromResponse(response(bundles.current(actor()), "READ", "bundle")).header("Cache-Control", "no-store").build();
    }
    @GET @Path("bundles/versions/{sequence}")
    public Response bundle(@PathParam("sequence") long sequence) {
        if (sequence < 1) throw Failure.validation();
        return Response.fromResponse(response(bundles.get(actor(), sequence), "READ", "bundle")).header("Cache-Control", "no-store").build();
    }
    @POST @Path("bundles/publish") @Consumes("application/json") @RolesAllowed("toolgate-admin")
    public Response publishBundle(@HeaderParam("Idempotency-Key") String key, String document) {
        if (codec.model(document, io.ololabs.toolgate.contracts.BundlePublishRequest.class).rollbackOf() != null) throw Failure.validation();
        return response(bundles.publish(actor(), document, key, correlation.id()), "PUBLISH", "bundle");
    }
    @POST @Path("bundles/rollback") @Consumes("application/json") @RolesAllowed("toolgate-admin")
    public Response rollbackBundle(@HeaderParam("Idempotency-Key") String key, String document) {
        if (codec.model(document, io.ololabs.toolgate.contracts.BundlePublishRequest.class).rollbackOf() == null) throw Failure.validation();
        return response(bundles.publish(actor(), document, key, correlation.id()), "ROLLBACK", "bundle");
    }
    @GET @Path(COLLECTION)
    public Response page(@PathParam("kind") String kind, @QueryParam("cursor") String cursor, @QueryParam("limit") @DefaultValue("50") int limit) {
        return response(service.page(actor(), Kind.path(kind), cursor, limit), "READ", kind);
    }
    @GET @Path(COLLECTION + "/{id:.+}")
    public Response get(@PathParam("kind") String kind, @PathParam("id") String id) {
        return response(service.get(actor(), Kind.path(kind), id), "READ", kind);
    }
    @POST @Path(COLLECTION) @Consumes("application/json") @RolesAllowed("toolgate-admin")
    public Response create(@PathParam("kind") String kind, @HeaderParam("Idempotency-Key") String key, String document) {
        // Canonical validation happens before extracting the identity used in the application use case.
        var entry = codec.entry(Kind.path(kind), document);
        return response(service.mutate(actor(), Kind.path(kind), entry.id().value(), "CREATE", document, 0, key, correlation.id()), "CREATE", kind);
    }
    @Inject ContractCodec codec;
    @GET @Path("openapi")
    public String openapi() { actor(); return codec.openapi(); }
    @PUT @Path(COLLECTION + "/{id:.+}") @Consumes("application/json") @RolesAllowed("toolgate-admin")
    public Response update(@PathParam("kind") String kind, @PathParam("id") String id, @HeaderParam("If-Match") String etag,
                           @HeaderParam("Idempotency-Key") String key, String document) {
        return response(service.mutate(actor(), Kind.path(kind), id, "UPDATE", document, expected(etag), key, correlation.id()), "UPDATE", kind);
    }
    @DELETE @Path(COLLECTION + "/{id:.+}") @RolesAllowed("toolgate-admin")
    public Response delete(@PathParam("kind") String kind, @PathParam("id") String id, @HeaderParam("If-Match") String etag,
                           @HeaderParam("Idempotency-Key") String key) {
        return response(service.mutate(actor(), Kind.path(kind), id, "DELETE", null, expected(etag), key, correlation.id()), "DELETE", kind);
    }
    @GET @Path("config/export") @Produces({"application/json", "application/yaml"})
    public Response export(@HeaderParam("Accept") @DefaultValue("application/json") String accept) {
        if (!accept.equals("application/json") && !accept.equals("application/yaml") && !accept.equals("*/*")) throw new NotAcceptableException();
        var yaml = accept.equals("application/yaml");
        return Response.fromResponse(response(service.export(actor(), yaml), "EXPORT", "config")).type(yaml ? "application/yaml" : "application/json").build();
    }
    @POST @Path("config/import") @Consumes({"application/json", "application/yaml"}) @RolesAllowed("toolgate-admin")
    public Response importConfig(@HeaderParam("Content-Type") String type, @HeaderParam("If-Match") String etag,
                                 @HeaderParam("Idempotency-Key") String key, String document) {
        return response(service.importConfig(actor(), document, type != null && type.split(";", 2)[0].trim().equals("application/yaml"), expected(etag), key, correlation.id()), "IMPORT", "config");
    }
    @GET @Path("audit") @RolesAllowed("toolgate-admin")
    public Response audit(@QueryParam("cursor") @DefaultValue("0") long cursor, @QueryParam("limit") @DefaultValue("50") int limit) {
        return response(service.audit(actor(), cursor, limit), "READ", "audit");
    }
}
