// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
package io.ololabs.toolgate.control.adapter;
import io.ololabs.toolgate.control.application.Failure;
import io.ololabs.toolgate.control.application.Store;
import io.ololabs.toolgate.contracts.ErrorCode;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class EndpointSocketTest {
    @Test void transportJitterRetriesAnIdempotentCheckInOnce(){
        var attempts=new AtomicInteger();var reply=new Store.Reply(200,"{}",1);
        var actual=EndpointSocket.checkInWithJitter(()->{if(attempts.incrementAndGet()==1)throw Failure.conflict();return reply;});
        assertSame(reply,actual);assertEquals(2,attempts.get());
    }
    @Test void persistentSequenceConflictStillFailsAfterOneRetry(){
        var attempts=new AtomicInteger();
        var failure=assertThrows(Failure.class,()->EndpointSocket.checkInWithJitter(()->{attempts.incrementAndGet();throw Failure.conflict();}));
        assertEquals(ErrorCode.CONFLICT,failure.code());assertEquals(2,attempts.get());
    }
    @Test void authorizationDenialIsNeverRetried(){
        var attempts=new AtomicInteger();var denied=new Failure(ErrorCode.FORBIDDEN,403,"Denied");
        assertSame(denied,assertThrows(Failure.class,()->EndpointSocket.checkInWithJitter(()->{attempts.incrementAndGet();throw denied;})));
        assertEquals(1,attempts.get());
    }
}
