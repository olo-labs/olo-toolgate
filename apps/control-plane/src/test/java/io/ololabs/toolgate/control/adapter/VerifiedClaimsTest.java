// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
package io.ololabs.toolgate.control.adapter;

import io.ololabs.toolgate.control.application.Failure;
import jakarta.json.Json;
import java.io.StringReader;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class VerifiedClaimsTest {
    @Test void jsonpCustomSessionEpochIsAnExactInteger() {
        try(var reader=Json.createReader(new StringReader("{\"epoch\":42,\"fraction\":1.5,\"overflow\":9223372036854775808}"))) {
            var claims=reader.readObject();
            assertEquals(42L,VerifiedActors.integerClaim(claims.get("epoch")));
            assertThrows(Failure.class,()->VerifiedActors.integerClaim(claims.get("fraction")));
            assertThrows(Failure.class,()->VerifiedActors.integerClaim(claims.get("overflow")));
            assertNull(VerifiedActors.integerClaim(null));
        }
    }
}
