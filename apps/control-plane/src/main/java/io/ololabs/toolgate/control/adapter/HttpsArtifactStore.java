// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
package io.ololabs.toolgate.control.adapter;

import io.ololabs.toolgate.control.application.*;
import java.net.*;
import java.net.http.*;
import java.time.Duration;
import java.util.concurrent.Semaphore;

/** Fixed HTTPS content-addressed mirror; redirects, inherited proxies and unbounded bodies are denied. */
public final class HttpsArtifactStore implements ArtifactStore {
    private final String origin,token;
    private final HttpClient client;
    private final Semaphore slots=new Semaphore(2);
    public HttpsArtifactStore(String origin,String token,javax.net.ssl.SSLContext tls){
        this.origin=EndpointService.origin(origin);this.token=token;
        if(token!=null&&(token.isBlank()||token.length()>4096||token.indexOf('\r')>=0||token.indexOf('\n')>=0))throw new IllegalArgumentException("Invalid external mirror credential");
        var builder=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).followRedirects(HttpClient.Redirect.NEVER)
            .proxy(new ProxySelector(){public java.util.List<Proxy> select(URI uri){return java.util.List.of(Proxy.NO_PROXY);}public void connectFailed(URI uri,SocketAddress address,java.io.IOException failure){}});
        if(tls!=null)builder.sslContext(tls);client=builder.build();
    }
    public byte[] download(String digest,long expectedBytes){
        if(!digest.matches("[a-f0-9]{64}")||expectedBytes<1||expectedBytes>32768)throw Failure.validation();
        if(!slots.tryAcquire())throw Failure.unavailable();
        try{
            var builder=HttpRequest.newBuilder(URI.create(origin+"/"+digest+".json")).timeout(Duration.ofSeconds(5)).header("Accept","application/json").GET();
            if(token!=null)builder.header("Authorization","Bearer "+token);
            // Body subscriber enforces its cap while streaming, rather than after allocation.
            var body=new java.util.concurrent.atomic.AtomicReference<BoundedBody>();
            var future=client.sendAsync(builder.build(),info->{var subscriber=new BoundedBody(expectedBytes);body.set(subscriber);return subscriber;});
            HttpResponse<byte[]> response;
            try{response=future.get(5,java.util.concurrent.TimeUnit.SECONDS);}
            catch(java.util.concurrent.ExecutionException|java.util.concurrent.TimeoutException failure){
                future.cancel(true);var subscriber=body.get();if(subscriber!=null)subscriber.cancel();throw Failure.unavailable();
            }catch(InterruptedException failure){future.cancel(true);var subscriber=body.get();if(subscriber!=null)subscriber.cancel();throw failure;}

            if(response.statusCode()!=200||!response.headers().firstValue("Content-Type").orElse("").split(";",2)[0].equalsIgnoreCase("application/json")
                ||response.body().length!=expectedBytes)throw Failure.unavailable();
            return response.body();
        }catch(InterruptedException failure){Thread.currentThread().interrupt();throw Failure.unavailable();}
        catch(IllegalArgumentException failure){throw Failure.unavailable();}
        finally{slots.release();}
    }
    private static final class BoundedBody implements HttpResponse.BodySubscriber<byte[]> {
        private final java.util.concurrent.CompletableFuture<byte[]> result=new java.util.concurrent.CompletableFuture<>();
        private final java.io.ByteArrayOutputStream bytes=new java.io.ByteArrayOutputStream();
        private final long limit;private java.util.concurrent.Flow.Subscription subscription;
        BoundedBody(long limit){this.limit=limit;}
        public java.util.concurrent.CompletionStage<byte[]> getBody(){return result;}
        public void onSubscribe(java.util.concurrent.Flow.Subscription subscription){this.subscription=subscription;subscription.request(1);}
        public void onNext(java.util.List<java.nio.ByteBuffer> chunks){
            for(var chunk:chunks){if(bytes.size()+chunk.remaining()>limit){subscription.cancel();result.completeExceptionally(new java.io.IOException("Descriptor size limit"));return;}
                var data=new byte[chunk.remaining()];chunk.get(data);bytes.writeBytes(data);}
            subscription.request(1);
        }
        void cancel(){if(subscription!=null)subscription.cancel();}
        public void onError(Throwable failure){result.completeExceptionally(failure);}
        public void onComplete(){result.complete(bytes.toByteArray());}
    }
}
