// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
package io.ololabs.toolgate.control.adapter;

import io.ololabs.toolgate.control.application.*;
import io.ololabs.toolgate.control.domain.Ids;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.Config;
import java.nio.file.Files;
import java.nio.file.Path;

/** Opt-in external key configuration. Enabled but incomplete signing prevents startup. */
@ApplicationScoped
public class BundleBootstrap {
    @Inject Config config;
    @Inject ContractCodec codec;
    private BundleSigner signer;
    void start(@Observes StartupEvent event) { signer = loadSigner(); }
    private BundleSigner loadSigner() {
        if (!config.getValue("toolgate.control.bundle.enabled", Boolean.class)) return payload -> { throw Failure.unavailable(); };
        try {
            var keyId = Ids.valid(config.getValue("toolgate.control.bundle.key-id", String.class));
            Ids.valid(config.getValue("toolgate.control.bundle.issuer", String.class));
            Ids.valid(config.getValue("toolgate.control.bundle.audience", String.class));
            var path = Path.of(config.getValue("toolgate.control.bundle.private-key-path", String.class));
            byte[] raw=ProtectedCustody.privateKey(path,16384);
            var pem = new String(raw, java.nio.charset.StandardCharsets.US_ASCII);
            var begin = "-----BEGIN " + "PRIVATE KEY-----";
            var end = "-----END " + "PRIVATE KEY-----";
            if (!pem.startsWith(begin)) throw new IllegalArgumentException();
            var der = java.util.Base64.getDecoder().decode(pem.replace(begin, "").replace(end, "").replaceAll("\\s", ""));
            var factory = java.security.KeyFactory.getInstance("RSA");
            var key = (java.security.interfaces.RSAPrivateCrtKey) factory.generatePrivate(new java.security.spec.PKCS8EncodedKeySpec(der));
            var jwtPath = config.getValue("mp.jwt.verify.publickey.location", String.class);
            var identityPem = ProtectedCustody.text(jwtPath.startsWith("file:") ? Path.of(java.net.URI.create(jwtPath)) : Path.of(jwtPath),16384);
            var identity = (java.security.interfaces.RSAPublicKey) factory.generatePublic(new java.security.spec.X509EncodedKeySpec(
                java.util.Base64.getDecoder().decode(identityPem.replace("-----BEGIN PUBLIC KEY-----", "").replace("-----END PUBLIC KEY-----", "").replaceAll("\\s", ""))));
            if (key.getModulus().equals(identity.getModulus())) throw new IllegalArgumentException();
            return new RsaBundleSigner(key, keyId, codec);
        } catch (java.io.IOException | java.security.GeneralSecurityException | IllegalArgumentException | java.util.NoSuchElementException e) {
            throw new IllegalStateException("Dedicated external RSA policy bundle key and valid trust configuration required");
        }
    }
    @Produces @ApplicationScoped
    BundleService service(PostgresStore store) {
        // Configuration is immutable for this instance; rotation replaces/restarts pods.
        if (signer == null) signer = loadSigner();
        return new BundleService(store, codec, signer,
            config.getValue("toolgate.control.bundle.issuer", String.class),
            config.getValue("toolgate.control.bundle.audience", String.class), java.time.Clock.systemUTC());
    }
}
