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
    @Inject org.eclipse.microprofile.config.Config config;
    @GET public Response home(){return Response.seeOther(java.net.URI.create("/console/")).build();}
    @GET @Path("api/public/v1/clients") @Produces("application/json")
    public Response manifest(){return Response.ok(artifacts.manifest()).header("Cache-Control","no-store").build();}
    @GET @Path("api/public/v1/installers") @Produces("application/json")
    public Response installers(){return Response.ok(artifacts.installers()).header("Cache-Control","no-store").build();}
    @GET @Path("api/public/v1/clients/extension") @Produces("application/json")
    public Response extension(){return Response.ok(artifacts.extension()).header("Cache-Control","no-store").build();}
    private String serverUrl(){
        var value=config.getOptionalValue("toolgate.control.endpoint.control-url",String.class).orElseThrow(()->new io.ololabs.toolgate.control.application.Failure(io.ololabs.toolgate.contracts.ErrorCode.DEPENDENCY_UNAVAILABLE,503,"Endpoint gateway is not configured"));
        var uri=java.net.URI.create(value);
        if(!"https".equals(uri.getScheme())||uri.getHost()==null||uri.getUserInfo()!=null||uri.getQuery()!=null||uri.getFragment()!=null||!(uri.getPath().isEmpty()||uri.getPath().equals("/")))throw new IllegalStateException("Invalid endpoint gateway URL");
        return value.endsWith("/")?value.substring(0,value.length()-1):value;
    }
    @GET @Path("api/public/v1/clients/configuration") @Produces("application/json")
    public Response configuration(){return Response.ok(java.util.Map.of("serverUrl",serverUrl())).header("Cache-Control","no-store").build();}
    @GET @Path("api/public/v1/clients/setup/{target}")
    public Response configuredSetup(@PathParam("target")String target){return artifacts.configuredWindowsInstaller(target,serverUrl());}
    @GET @Path("api/public/v1/clients/{filename}")
    public Response download(@PathParam("filename")String filename){return artifacts.download(filename);}
}
