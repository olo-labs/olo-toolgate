// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
package io.ololabs.toolgate.control;

import io.ololabs.toolgate.control.adapter.HttpsArtifactStore;
import io.ololabs.toolgate.control.application.Failure;
import java.net.InetSocketAddress;
import java.security.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.*;
import org.bouncycastle.cert.jcajce.*;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import static org.junit.jupiter.api.Assertions.*;

/** Real HTTPS transfer boundaries: partial disconnect, streaming cap and total body deadline. */
final class ArtifactStoreTest {
    @Test void fixedOriginBoundedTransferAndInterruptedStream()throws Exception {
        var generator=KeyPairGenerator.getInstance("RSA");generator.initialize(2048);var key=generator.generateKeyPair();
        var subject=new X500Name("CN=127.0.0.1");long now=System.currentTimeMillis();
        var certificate=new JcaX509v3CertificateBuilder(subject,java.math.BigInteger.ONE,new Date(now-1000),new Date(now+600000),subject,key.getPublic());
        certificate.addExtension(Extension.subjectAlternativeName,false,new GeneralNames(new GeneralName(GeneralName.iPAddress,"127.0.0.1")));
        var cert=new JcaX509CertificateConverter().getCertificate(certificate.build(new JcaContentSignerBuilder("SHA256withRSA").build(key.getPrivate())));
        var keys=KeyStore.getInstance("PKCS12");keys.load(null,null);char[] password=UUID.randomUUID().toString().toCharArray();keys.setKeyEntry("server",key.getPrivate(),password,new java.security.cert.Certificate[]{cert});
        var km=javax.net.ssl.KeyManagerFactory.getInstance(javax.net.ssl.KeyManagerFactory.getDefaultAlgorithm());km.init(keys,password);
        var trusted=KeyStore.getInstance("PKCS12");trusted.load(null,null);trusted.setCertificateEntry("server",cert);
        var tm=javax.net.ssl.TrustManagerFactory.getInstance(javax.net.ssl.TrustManagerFactory.getDefaultAlgorithm());tm.init(trusted);
        var tls=javax.net.ssl.SSLContext.getInstance("TLS");tls.init(km.getKeyManagers(),tm.getTrustManagers(),null);
        var mode=new java.util.concurrent.atomic.AtomicReference<>("valid");var requests=new java.util.concurrent.atomic.AtomicInteger();
        byte[] bytes="{\"descriptor\":\"exact raw bytes\"}".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        var pool=java.util.concurrent.Executors.newCachedThreadPool();
        var server=com.sun.net.httpserver.HttpsServer.create(new InetSocketAddress("127.0.0.1",0),2);
        server.setHttpsConfigurator(new com.sun.net.httpserver.HttpsConfigurator(tls));server.setExecutor(pool);
        server.createContext("/"+"a".repeat(64)+".json",exchange->{
            requests.incrementAndGet();exchange.getResponseHeaders().set("Content-Type","application/json");
            try{
                switch(mode.get()){
                    case "redirect" -> {exchange.getResponseHeaders().set("Location","https://169.254.169.254/");exchange.sendResponseHeaders(302,-1);}
                    case "large" -> {exchange.sendResponseHeaders(200,bytes.length+1);exchange.getResponseBody().write(new byte[bytes.length+1]);}
                    case "disconnect" -> {exchange.sendResponseHeaders(200,bytes.length);exchange.getResponseBody().write(Arrays.copyOf(bytes,2));}
                    case "slow" -> {exchange.sendResponseHeaders(200,bytes.length);exchange.getResponseBody().write(Arrays.copyOf(bytes,2));exchange.getResponseBody().flush();try{Thread.sleep(10000);}catch(InterruptedException interrupted){Thread.currentThread().interrupt();}}
                    default -> {exchange.sendResponseHeaders(200,bytes.length);exchange.getResponseBody().write(bytes);}
                }
            }finally{exchange.close();}
        });
        server.start();
        try{
            String origin="https://127.0.0.1:"+server.getAddress().getPort();
            var store=new HttpsArtifactStore(origin,null,tls);assertArrayEquals(bytes,store.download("a".repeat(64),bytes.length));
            for(var negative:List.of("redirect","large","disconnect")){mode.set(negative);assertThrows(Failure.class,()->store.download("a".repeat(64),bytes.length));}
            assertEquals(4,requests.get());
            mode.set("slow");long started=System.nanoTime();assertThrows(Failure.class,()->store.download("a".repeat(64),bytes.length));assertTrue(System.nanoTime()-started<java.time.Duration.ofSeconds(7).toNanos());
            mode.set("valid");assertArrayEquals(bytes,store.download("a".repeat(64),bytes.length));
            assertThrows(Failure.class,()->store.download("../outside",bytes.length));
            assertThrows(IllegalArgumentException.class,()->new HttpsArtifactStore("http://127.0.0.1",null,tls));
            assertThrows(IllegalArgumentException.class,()->new HttpsArtifactStore(origin,"credential\nheader",tls));
        }finally{server.stop(0);pool.shutdownNow();}
    }
}
