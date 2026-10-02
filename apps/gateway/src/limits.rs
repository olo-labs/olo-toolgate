// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
//! Bounded admission and fixed-cardinality metrics; no identity-indexed memory.
use std::{
    sync::{
        atomic::{AtomicU64, Ordering},
        Mutex,
    },
    time::Duration,
};
use tokio::time::Instant;

/// Fixed-window capacity limiter per replica. This is not a fleet-wide quota.
pub struct RateLimit {
    max: u32,
    window: Mutex<(Instant, u32)>,
}
impl RateLimit {
    /// No per-user map or cleanup task is needed for bounded global admission.
    pub fn new(max: u32) -> Self {
        Self {
            max,
            window: Mutex::new((Instant::now(), 0)),
        }
    }
    /// Clock is injectable for deterministic boundary tests.
    pub fn admit(&self, now: Instant) -> bool {
        let Ok(mut window) = self.window.lock() else {
            return false;
        };
        if now.saturating_duration_since(window.0) >= Duration::from_secs(1) {
            *window = (now, 0);
        }
        if window.1 >= self.max {
            return false;
        }
        window.1 += 1;
        true
    }
}

/// Fixed result labels and cumulative latency buckets avoid untrusted cardinality.
#[derive(Default)]
pub struct Metrics {
    pub requests: AtomicU64,
    pub allow: AtomicU64,
    pub block: AtomicU64,
    pub errors: AtomicU64,
    pub limited: AtomicU64,
    pub duration_micros: AtomicU64,
    pub buckets: [AtomicU64; 6],
}
impl Metrics {
    /// Observe every runtime ingress request, including rejected admissions.
    pub fn observe(&self, duration: Duration) {
        self.requests.fetch_add(1, Ordering::Relaxed);
        let micros = u64::try_from(duration.as_micros()).unwrap_or(u64::MAX);
        self.duration_micros.fetch_add(micros, Ordering::Relaxed);
        for (bucket, bound) in self
            .buckets
            .iter()
            .zip([100, 500, 1000, 5000, 10000, u64::MAX])
        {
            if micros <= bound {
                bucket.fetch_add(1, Ordering::Relaxed);
            }
        }
    }
    /// Prometheus text output has no caller-controlled labels.
    pub fn render(&self) -> String {
        let load = |v: &AtomicU64| v.load(Ordering::Relaxed);
        let mut text = format!("# TYPE toolgate_requests_total counter\ntoolgate_requests_total {}\n# TYPE toolgate_authorization_total counter\ntoolgate_authorization_total{{decision=\"allow\"}} {}\ntoolgate_authorization_total{{decision=\"block\"}} {}\n# TYPE toolgate_errors_total counter\ntoolgate_errors_total {}\n# TYPE toolgate_limited_total counter\ntoolgate_limited_total {}\n# TYPE toolgate_request_duration_seconds histogram\n", load(&self.requests), load(&self.allow), load(&self.block), load(&self.errors), load(&self.limited));
        for (bucket, bound) in self
            .buckets
            .iter()
            .zip(["0.0001", "0.0005", "0.001", "0.005", "0.01", "+Inf"])
        {
            text.push_str(&format!(
                "toolgate_request_duration_seconds_bucket{{le=\"{bound}\"}} {}\n",
                load(bucket)
            ));
        }
        text.push_str(&format!("toolgate_request_duration_seconds_sum {}\ntoolgate_request_duration_seconds_count {}\n", load(&self.duration_micros) as f64 / 1_000_000.0, load(&self.requests)));
        text
    }
}
