// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
package io.ololabs.toolgate.control.adapter;

import io.ololabs.toolgate.control.application.ReviewedRecovery;
import io.ololabs.toolgate.contracts.FleetTrustKey;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import io.quarkus.runtime.StartupEvent;
import org.eclipse.microprofile.config.Config;
import java.nio.file.*;
import java.util.*;

/** A protected external packet is applied once; no authenticated API can enter bootstrap mode. */
@ApplicationScoped
public class RecoveryBootstrap {
    @Inject Config config;@Inject PostgresStore store;@Inject ContractCodec codec;
    private String read(String path,int limit)throws java.io.IOException{
        return new String(ProtectedCustody.read(Path.of(path),limit),java.nio.charset.StandardCharsets.UTF_8);
    }
    void start(@Observes @jakarta.annotation.Priority(10) StartupEvent event){
        var path=config.getOptionalValue("toolgate.control.recovery.packet-path",String.class);if(path.isEmpty())return;
        try{
            var trust=(com.fasterxml.jackson.databind.JsonNode)codec.value(read(config.getValue("toolgate.control.recovery.trust-path",String.class),16384));
            if(!trust.isObject()||trust.size()!=1||!trust.path("keys").isArray()||trust.path("keys").size()<1||trust.path("keys").size()>4)throw new IllegalArgumentException();
            var keys=new ArrayList<FleetTrustKey>();for(var key:trust.path("keys"))keys.add(codec.model(codec.json(key),FleetTrustKey.class));
            new ReviewedRecovery(store,codec,java.time.Clock.systemUTC()).apply(read(path.get(),2097152),keys);
        }catch(Exception invalid){throw new IllegalStateException("Reviewed recovery packet or pinned review authority rejected ("+invalid.getClass().getSimpleName()+")");}
    }
}
