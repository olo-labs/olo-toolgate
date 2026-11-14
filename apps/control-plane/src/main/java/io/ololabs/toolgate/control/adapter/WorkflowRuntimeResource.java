// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
package io.ololabs.toolgate.control.adapter;

import com.fasterxml.jackson.databind.JsonNode;
import io.ololabs.toolgate.contracts.ErrorCode;
import io.ololabs.toolgate.control.application.*;
import io.smallrye.common.annotation.Blocking;
import io.vertx.ext.web.RoutingContext;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Response;
import java.time.Clock;

/** Workload-only claim and completion. A listener credential has no access to run inputs. */
@Path("/internal/v1/runtime") @Produces("application/json") @Blocking
@RolesAllowed(WorkflowRuntimeResource.ROLE)
public class WorkflowRuntimeResource {
    static final String ROLE = "toolgate-workflow-runtime";

    @Inject PostgresStore store;
    @Inject ContractCodec codec;
    @Inject VerifiedActors actors;
    @Inject Correlation correlation;
    @Inject RoutingContext routing;
    @Inject TriggerBootstrap triggers;
    @Inject VaultKeyBootstrap keys;

    private DirectoryService.Actor worker() {
        if (!routing.request().isSSL()) throw new Failure(ErrorCode.UNAUTHORIZED, 401, "Direct TLS required");
        return actors.service(ROLE);
    }

    private WorkflowLeases leases(DirectoryService.Actor worker) {
        var clock = Clock.systemUTC();
        var vault = keys.required();
        var profiles = new WorkflowProfiles(store, codec, new WorkflowProfileDependencies(codec, clock,
            triggers.subscriptions(worker.tenant()), triggers.forTenant(worker.tenant()), worker.tenant()), vault);
        return new WorkflowLeases(store, codec, clock, profiles, vault);
    }

    @POST @Path("claim")
    public Response claim() {
        var worker = worker();
        return reply(leases(worker).claim(worker, correlation.id()));
    }

    @POST @Path("runs/{runId}/finish") @Consumes("application/json")
    public Response finish(@PathParam("runId") String runId, String document) {
        var worker = worker();
        if (document == null || document.length() > 1024) throw Failure.validation();
        if (!(codec.value(document) instanceof JsonNode value) || !value.isObject() || value.size() != 3
                || !value.path("leaseId").isTextual() || !value.path("leaseToken").isTextual() || !value.path("state").isTextual())
            throw Failure.validation();
        return reply(leases(worker).finish(worker, runId, value.path("leaseId").textValue(), value.path("leaseToken").textValue(),
            value.path("state").textValue(), correlation.id()));
    }

    private static Response reply(Store.Reply value) {
        return Response.status(value.status()).header("Cache-Control", "no-store").entity(value.body()).build();
    }
}
