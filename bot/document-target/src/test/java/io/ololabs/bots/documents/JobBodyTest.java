// Copyright 2026 OLO Labs; SPDX-License-Identifier: Apache-2.0
package io.ololabs.bots.documents;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class JobBodyTest {
    static final class Subscription implements Flow.Subscription {
        boolean cancelled;long requested;
        public void request(long n){requested+=n;}public void cancel(){cancelled=true;}
    }
    @Test void oversized_stream_is_cancelled_before_allocation_and_never_returns_partial_json() {
        var body=new JobStore.BoundedBody();var subscription=new Subscription();body.onSubscribe(subscription);
        body.onNext(List.of(ByteBuffer.wrap(new byte[1_048_576])));
        body.onNext(List.of(ByteBuffer.wrap(new byte[1])));body.onComplete();
        assertTrue(subscription.cancelled);assertThrows(CompletionException.class,()->body.getBody().toCompletableFuture().join());
    }
    @Test void bounded_complete_stream_is_preserved_and_transport_failure_remains_failure() {
        var body=new JobStore.BoundedBody();var subscription=new Subscription();body.onSubscribe(subscription);
        body.onNext(List.of(ByteBuffer.wrap(new byte[]{1,2}),ByteBuffer.wrap(new byte[]{3})));body.onComplete();
        assertArrayEquals(new byte[]{1,2,3},body.getBody().toCompletableFuture().join());assertFalse(subscription.cancelled);
        var failed=new JobStore.BoundedBody();failed.onSubscribe(new Subscription());failed.onNext(List.of(ByteBuffer.wrap(new byte[]{1})));
        failed.onError(new java.io.IOException("transport stopped"));
        assertThrows(CompletionException.class,()->failed.getBody().toCompletableFuture().join());
    }
}
