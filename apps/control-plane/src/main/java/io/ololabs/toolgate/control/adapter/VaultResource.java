// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
package io.ololabs.toolgate.control.adapter;
import io.ololabs.toolgate.control.application.*;
import io.ololabs.toolgate.contracts.ErrorCode;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Response;
@Path("/api/control/v1/vault") @io.quarkus.security.Authenticated @io.smallrye.common.annotation.Blocking @Produces("application/json")
public class VaultResource {
    @Inject PostgresStore store;@Inject ContractCodec codec;@Inject VerifiedActors actors;@Inject org.eclipse.microprofile.config.Config config;
    private VaultService service(){var setting=config.getOptionalValue("toolgate.control.vault.key-path",String.class);if(setting.isEmpty())throw new Failure(ErrorCode.DEPENDENCY_UNAVAILABLE,503,"Secret custody unavailable");try{var path=java.nio.file.Path.of(setting.get());if(!path.isAbsolute()||!java.nio.file.Files.isRegularFile(path)||java.nio.file.Files.isSymbolicLink(path))throw new IllegalArgumentException();if(java.nio.file.Files.getFileAttributeView(path,java.nio.file.attribute.PosixFileAttributeView.class)!=null){var attrs=java.nio.file.Files.readAttributes(path,java.nio.file.attribute.PosixFileAttributes.class);if(attrs.permissions().contains(java.nio.file.attribute.PosixFilePermission.GROUP_WRITE)||attrs.permissions().contains(java.nio.file.attribute.PosixFilePermission.OTHERS_WRITE)||!(attrs.owner().getName().equals("root")||attrs.owner().getName().equals(System.getProperty("user.name"))))throw new IllegalArgumentException();}try(var in=java.nio.file.Files.newInputStream(path)){var bytes=in.readNBytes(33);return new VaultService(store,codec,bytes);}}catch(java.io.IOException|IllegalArgumentException invalid){throw Failure.unavailable();}}
    private Response reply(Store.Reply r){return Response.status(r.status()).header("Cache-Control","no-store").entity(r.body()).build();}
    @GET public Response list(){return reply(service().list(actors.management()));}
    @POST @Consumes("application/json") public Response write(@HeaderParam("Idempotency-Key")String key,String body){var actor=actors.management();if(body==null||body.getBytes(java.nio.charset.StandardCharsets.UTF_8).length>65536)throw Failure.validation();return reply(service().write(actor,body,key));}
}
