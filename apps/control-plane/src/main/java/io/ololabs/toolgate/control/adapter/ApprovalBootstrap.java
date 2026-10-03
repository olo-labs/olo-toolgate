// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
package io.ololabs.toolgate.control.adapter;

import io.ololabs.toolgate.control.application.*;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Produces;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.Config;

/** Approval behavior is opt-in; absent notifications introduce no infrastructure requirement. */
@ApplicationScoped
public class ApprovalBootstrap {
    @Inject Config config;
    @Inject Instance<ApprovalNotification> notifications;
    private record Limits(boolean enabled,long pending,int active,int permits) {}
    private Limits limits() {
        var enabled=config.getValue("toolgate.control.approval.enabled",Boolean.class);
        var pending=config.getValue("toolgate.control.approval.pending-ttl-ms",Long.class);
        var active=config.getValue("toolgate.control.approval.max-active",Integer.class);
        var permits=config.getValue("toolgate.control.approval.max-active-permits",Integer.class);
        if (pending < 1000 || pending > 300000 || active < 1 || active > 10000 || permits < 1 || permits > 10000
                || (enabled && !config.getValue("toolgate.control.bundle.enabled",Boolean.class)) || notifications.isAmbiguous()) {
            throw new IllegalStateException("Invalid approval configuration; enabled approvals require bundle publication");
        }
        return new Limits(enabled,pending,active,permits);
    }
    void start(@Observes StartupEvent event) { limits(); }
    @Produces @ApplicationScoped
    ApprovalService service(PostgresStore store,ContractCodec codec) {
        var limits=limits();
        return new ApprovalService(store,codec,java.time.Clock.systemUTC(),limits.enabled(),limits.pending(),limits.active(),limits.permits(),
            config.getValue("toolgate.control.bundle.issuer",String.class),config.getValue("toolgate.control.bundle.audience",String.class),
            notifications.isResolvable()?notifications.get():null);
    }
}
