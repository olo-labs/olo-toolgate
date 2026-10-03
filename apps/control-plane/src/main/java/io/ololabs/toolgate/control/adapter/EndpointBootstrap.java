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
import java.nio.file.*;

/** Opt-in direct TLS enrollment; missing external CA/TLS settings stop startup. */
@ApplicationScoped
public class EndpointBootstrap {
    @Inject Config config;
    @Inject ContractCodec codec;
    @Inject PostgresStore store;
    private EndpointService service;
    void start(@Observes StartupEvent ignored){if(service==null)service=load();}
    private String setting(String name){return config.getValue("toolgate.control.endpoint."+name,String.class);}
    private String pem(String name)throws java.io.IOException{
        try(var input=Files.newInputStream(Path.of(setting(name)))){byte[] bytes=input.readNBytes(16385);if(bytes.length>16384)throw new IllegalArgumentException();return new String(bytes,java.nio.charset.StandardCharsets.US_ASCII);}
    }
    private EndpointService load(){
        boolean enabled=config.getValue("toolgate.control.endpoint.enabled",Boolean.class);DeviceIssuer issuer=null;
        String tenant="disabled",server="disabled",organization="disabled",control="https://disabled.invalid",gateway="https://disabled.invalid";
        if(enabled)try{
            if(!config.getValue("quarkus.http.insecure-requests",String.class).equalsIgnoreCase("disabled")
                || !config.getValue("quarkus.http.ssl.client-auth",String.class).equalsIgnoreCase("request")
                || config.getOptionalValue("quarkus.http.ssl.certificate.trust-store-file",String.class).isEmpty())throw new IllegalArgumentException();
            issuer=new X509DeviceIssuer(pem("private-key-path"),pem("ca-certificate-path"),System.currentTimeMillis());
            // Device CA cannot reuse the administrative or policy private/public key domain.
            var ca=(java.security.cert.X509Certificate)java.security.cert.CertificateFactory.getInstance("X.509").generateCertificate(new java.io.ByteArrayInputStream(issuer.issuerCertificate().getBytes(java.nio.charset.StandardCharsets.US_ASCII)));
            var caKey=(java.security.interfaces.RSAPublicKey)ca.getPublicKey();
            var jwtLocation=config.getValue("mp.jwt.verify.publickey.location",String.class);
            var jwtPath=jwtLocation.startsWith("file:")?Path.of(java.net.URI.create(jwtLocation)):Path.of(jwtLocation);
            String jwtPem=Files.readString(jwtPath);var keyFactory=java.security.KeyFactory.getInstance("RSA");
            var idp=(java.security.interfaces.RSAPublicKey)keyFactory.generatePublic(new java.security.spec.X509EncodedKeySpec(java.util.Base64.getDecoder().decode(jwtPem.replace("-----BEGIN PUBLIC KEY-----","").replace("-----END PUBLIC KEY-----","").replaceAll("\\s",""))));
            if(caKey.getModulus().equals(idp.getModulus()))throw new IllegalArgumentException();
            if(config.getValue("toolgate.control.bundle.enabled",Boolean.class)){
                String policyPem=Files.readString(Path.of(config.getValue("toolgate.control.bundle.private-key-path",String.class)));
                var policy=(java.security.interfaces.RSAPrivateCrtKey)keyFactory.generatePrivate(new java.security.spec.PKCS8EncodedKeySpec(java.util.Base64.getDecoder().decode(policyPem.replace("-----BEGIN "+"PRIVATE KEY-----","").replace("-----END "+"PRIVATE KEY-----","").replaceAll("\\s",""))));
                if(caKey.getModulus().equals(policy.getModulus()))throw new IllegalArgumentException();
            }
            tenant=Ids.valid(setting("tenant-id"));server=Ids.valid(setting("server-id"));organization=setting("organization");control=setting("control-url");gateway=setting("gateway-url");
        }catch(Exception failure){throw new IllegalStateException("Endpoint enrollment requires dedicated external CA and direct HTTPS with requested client certificates");}
        return new EndpointService(store,codec,issuer,java.time.Clock.systemUTC(),enabled,tenant,server,organization,control,gateway);
    }
    @Produces @ApplicationScoped EndpointService endpoint(){if(service==null)service=load();return service;}
}
