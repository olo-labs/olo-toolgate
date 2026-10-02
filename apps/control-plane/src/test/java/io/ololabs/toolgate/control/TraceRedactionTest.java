// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
package io.ololabs.toolgate.control;

import io.ololabs.toolgate.control.adapter.TraceRedaction;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import java.util.Collection;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Exercises an actual SDK span/export boundary with hostile request and exception metadata. */
final class TraceRedactionTest {
    @Test void exportedSpansExcludeRawUrlsEventsAndExceptionText() {
        var exported = new java.util.ArrayList<SpanData>();
        var sink = new SpanExporter() {
            public CompletableResultCode export(Collection<SpanData> spans) { exported.addAll(spans); return CompletableResultCode.ofSuccess(); }
            public CompletableResultCode flush() { return CompletableResultCode.ofSuccess(); }
            public CompletableResultCode shutdown() { return CompletableResultCode.ofSuccess(); }
        };
        try (var provider = SdkTracerProvider.builder().addSpanProcessor(SimpleSpanProcessor.create(new TraceRedaction.SafeExporter(sink))).build()) {
            var span = provider.get("test").spanBuilder("secret-path").startSpan();
            span.setAttribute("url.full", "https://user:secret@example.invalid/?password=secret");
            span.setAttribute("http.request.method", "GET"); span.setAttribute("http.response.status_code", 400L);
            span.setAttribute("toolgate.request_id", "11111111-1111-1111-1111-111111111111");
            span.recordException(new IllegalArgumentException("secret-body")); span.setStatus(StatusCode.ERROR, "secret-error"); span.end();
        }
        assertEquals(1, exported.size()); var span = exported.getFirst();
        assertEquals("control.request", span.getName()); assertTrue(span.getEvents().isEmpty());
        assertEquals("", span.getStatus().getDescription());
        assertEquals(3, span.getAttributes().size()); assertNull(span.getAttributes().get(AttributeKey.stringKey("url.full")));
        assertEquals("GET", span.getAttributes().get(AttributeKey.stringKey("http.request.method")));
    }
}
