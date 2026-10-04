// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
package io.ololabs.toolgate.control.adapter;
import jakarta.annotation.security.PermitAll;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Response;
import io.smallrye.common.annotation.Blocking;

/** Anonymous installation entry points. Enrollment and organization APIs keep their existing authorization. */
@Path("/") @PermitAll @Blocking
public class ClientDownloadResource {
    @Inject ClientArtifacts artifacts;
    @GET public Response home(){return Response.seeOther(java.net.URI.create("/console/")).build();}
    @GET @Path("api/public/v1/clients") @Produces("application/json")
    public Response manifest(){return Response.ok(artifacts.manifest()).header("Cache-Control","no-store").build();}
    @GET @Path("api/public/v1/installers") @Produces("application/json")
    public Response installers(){return Response.ok(artifacts.installers()).header("Cache-Control","no-store").build();}
    @GET @Path("api/public/v1/clients/{filename}")
    public Response download(@PathParam("filename")String filename){return artifacts.download(filename);}
}
