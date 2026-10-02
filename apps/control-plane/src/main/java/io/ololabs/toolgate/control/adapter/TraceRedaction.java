// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
package io.ololabs.toolgate.control.adapter;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.TraceState;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.trace.data.DelegatingSpanData;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.data.StatusData;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdkBuilder;
import io.quarkus.opentelemetry.runtime.AutoConfiguredOpenTelemetrySdkBuilderCustomizer;
import jakarta.inject.Singleton;
import java.util.Collection;
import java.util.List;

/** Export only approved response metadata; framework URLs, events and exception messages may contain secrets. */
@Singleton
public class TraceRedaction implements AutoConfiguredOpenTelemetrySdkBuilderCustomizer {
    @jakarta.inject.Inject org.eclipse.microprofile.config.Config config;
    public void customize(AutoConfiguredOpenTelemetrySdkBuilder builder) {
        var exportEnabled = config.getValue("toolgate.control.trace-export-enabled", Boolean.class);
        builder.addSpanExporterCustomizer((exporter, config) -> new SafeExporter(exporter, exportEnabled));
    }
    public static final class SafeExporter implements SpanExporter {
        private final SpanExporter delegate;
        private final boolean enabled;
        public SafeExporter(SpanExporter delegate) { this(delegate, true); }
        public SafeExporter(SpanExporter delegate, boolean enabled) { this.delegate = delegate; this.enabled = enabled; }
        public CompletableResultCode export(Collection<SpanData> spans) {
            return enabled ? delegate.export(spans.stream().map(SafeExporter::safe).toList()) : CompletableResultCode.ofSuccess();
        }
        public CompletableResultCode flush() { return delegate.flush(); }
        public CompletableResultCode shutdown() { return delegate.shutdown(); }
        private static SpanContext context(SpanContext original) {
            return SpanContext.create(original.getTraceId(), original.getSpanId(), original.getTraceFlags(), TraceState.getDefault());
        }
        private static SpanData safe(SpanData original) {
            var attributes = Attributes.builder();
            var method = original.getAttributes().get(AttributeKey.stringKey("http.request.method"));
            if (method == null) method = original.getAttributes().get(AttributeKey.stringKey("http.method"));
            if (method != null) attributes.put("http.request.method", java.util.Set.of("GET","POST","PUT","DELETE","HEAD","OPTIONS").contains(method) ? method : "OTHER");
            var status = original.getAttributes().get(AttributeKey.longKey("http.response.status_code"));
            if (status == null) status = original.getAttributes().get(AttributeKey.longKey("http.status_code"));
            if (status != null) attributes.put("http.response.status_code", status);
            var requestId = original.getAttributes().get(AttributeKey.stringKey("toolgate.request_id"));
            if (requestId != null && requestId.matches("[a-f0-9-]{36}")) attributes.put("toolgate.request_id", requestId);
            var approved = attributes.build();
            return new DelegatingSpanData(original) {
                @Override public String getName() { return "control.request"; }
                @Override public Attributes getAttributes() { return approved; }
                @Override public int getTotalAttributeCount() { return approved.size(); }
                @Override public List<io.opentelemetry.sdk.trace.data.EventData> getEvents() { return List.of(); }
                @Override public int getTotalRecordedEvents() { return 0; }
                @Override public List<io.opentelemetry.sdk.trace.data.LinkData> getLinks() { return List.of(); }
                @Override public int getTotalRecordedLinks() { return 0; }
                @Override public StatusData getStatus() { return StatusData.create(original.getStatus().getStatusCode(), ""); }
                @Override public SpanContext getSpanContext() { return context(original.getSpanContext()); }
                @Override public SpanContext getParentSpanContext() { return context(original.getParentSpanContext()); }
            };
        }
    }
}
