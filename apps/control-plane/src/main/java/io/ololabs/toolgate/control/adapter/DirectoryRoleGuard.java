// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
package io.ololabs.toolgate.control.adapter;

import io.ololabs.toolgate.control.application.DirectoryService;
import io.ololabs.toolgate.control.domain.Ids;
import jakarta.inject.Inject;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.jboss.resteasy.reactive.server.ServerRequestFilter;

/** Directory activation constrains verified administrative identities across all Control adapters. */
@jakarta.enterprise.context.ApplicationScoped
public class DirectoryRoleGuard {
    @Inject JsonWebToken jwt;
    @Inject DirectoryService service;
    @Inject io.quarkus.security.identity.SecurityIdentity identity;
    @ServerRequestFilter
    public void check(jakarta.ws.rs.container.ContainerRequestContext context) {
        String path=context.getUriInfo().getPath().replaceFirst("^/","");
        if (identity.isAnonymous() || !identity.hasRole("toolgate-admin") || !path.startsWith("api/control/v1/")) return;
        Object tenant=jwt.getClaim("tenant_id"), user=jwt.getClaim("user_id");
        if (!(tenant instanceof String tenantId) || user!=null && !(user instanceof String))
            throw new io.ololabs.toolgate.control.application.Failure(io.ololabs.toolgate.contracts.ErrorCode.UNAUTHORIZED,401,"Invalid identity claims");
        if(user==null) return;
        try {
            service.requirePortal(new DirectoryService.Actor(new Ids.TenantId(tenantId),
                DirectoryService.digest(jwt.getIssuer()+"\n"+jwt.getSubject()),true),(String)user);
        } catch(IllegalArgumentException failure) {
            throw new io.ololabs.toolgate.control.application.Failure(io.ololabs.toolgate.contracts.ErrorCode.UNAUTHORIZED,401,"Invalid identity claims");
        }
    }
}
