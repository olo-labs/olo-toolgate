// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
package io.ololabs.toolgate.control.adapter;
import io.ololabs.toolgate.control.application.*;
import io.ololabs.toolgate.contracts.ErrorCode;
import jakarta.annotation.security.PermitAll;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Response;
import io.smallrye.common.annotation.Blocking;
import io.vertx.ext.web.RoutingContext;

/** Signed discovery has its own root so generic directory routes cannot shadow enrollment endpoints. */
@Path("/.well-known/olo-toolgate-client") @Produces("application/json") @PermitAll @Blocking
public class ClientDiscoveryResource {
    @Inject EndpointService service;
    @Inject RoutingContext routing;
    @Inject io.micrometer.core.instrument.MeterRegistry metrics;
    @GET public Response discovery(){
        if(!routing.request().isSSL())throw new Failure(ErrorCode.UNAUTHORIZED,401,"Direct TLS required");
        var reply=service.discovery();metrics.counter("toolgate_control_endpoint_operations_total","operation","DISCOVERY").increment();
        return Response.status(reply.status()).header("Cache-Control","no-store").entity(reply.body()).build();
    }
}
