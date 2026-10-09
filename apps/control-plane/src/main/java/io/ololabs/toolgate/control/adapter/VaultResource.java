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
    VaultService service(){var setting=config.getOptionalValue("toolgate.control.vault.key-path",String.class);if(setting.isEmpty())throw new Failure(ErrorCode.DEPENDENCY_UNAVAILABLE,503,"Secret custody unavailable");try{return new VaultService(store,codec,ProtectedCustody.read(java.nio.file.Path.of(setting.get()),32));}catch(java.io.IOException|IllegalArgumentException invalid){throw Failure.unavailable();}}
    private Response reply(Store.Reply r){return Response.status(r.status()).header("Cache-Control","no-store").entity(r.body()).build();}
    @GET public Response list(){return reply(service().list(actors.management()));}
    @POST @Consumes("application/json") public Response write(@HeaderParam("Idempotency-Key")String key,String body){var actor=actors.management();if(body==null||body.getBytes(java.nio.charset.StandardCharsets.UTF_8).length>65536)throw Failure.validation();return reply(service().write(actor,body,key));}
}
