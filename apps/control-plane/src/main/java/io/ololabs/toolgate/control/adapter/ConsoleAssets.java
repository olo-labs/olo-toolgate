// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
package io.ololabs.toolgate.control.adapter;

import io.vertx.ext.web.Router;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;

/** Static shell is public; every data request still crosses the signed-token API.
 * Browser policy forbids external script, framing, inline execution and API hosts.
 */
@ApplicationScoped
public class ConsoleAssets {
    void routes(@Observes Router router) {
        router.route("/console/*").order(-1000).handler(context -> {
            context.addHeadersEndHandler(ignored -> {
            var headers = context.response().headers();
            headers.set("Content-Security-Policy", "default-src 'none'; script-src 'self'; style-src 'self'; img-src 'self'; connect-src 'self'; base-uri 'none'; frame-ancestors 'none'; form-action 'self'");
            headers.set("X-Content-Type-Options", "nosniff");
            headers.set("Referrer-Policy", "no-referrer");
            headers.set("Permissions-Policy", "camera=(), microphone=(), geolocation=()");
            headers.set("Cache-Control", context.normalizedPath().startsWith("/console/assets/") ? "public, max-age=31536000, immutable" : "no-store");
            });
            context.next();
        });
        for (var path : new String[]{"/", "/console"}) {
            router.get(path).handler(context -> {
                if (context.request().path().equals(path)) context.response().setStatusCode(302)
                    .putHeader("Location", "/console/").putHeader("Cache-Control", "no-store").end();
                else context.next();
            });
        }
    }
}
