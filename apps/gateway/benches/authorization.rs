// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
//! Reproducible core baseline; no network or collector latency is hidden in claims.
use olo_toolgate_contracts::{AuthorizationRequest, RequestContext, RuntimeAuditEvent};
use olo_toolgate_gateway::{
    application::Gateway, audit::AuditSink, config::Config, extraction::Registry,
    policy::PortFuture, unix_ms, validation::Contracts,
};
use std::{hint::black_box, sync::Arc, time::Instant};

struct Acknowledged;
impl AuditSink for Acknowledged {
    fn record(&self, event: RuntimeAuditEvent) -> PortFuture<'_, Result<(), &'static str>> {
        Box::pin(async move {
            black_box(event);
            Ok(())
        })
    }
    fn ready(&self) -> bool {
        true
    }
}

fn main() {
    let config: Config =
        serde_json::from_str(include_str!("../../../docs/examples/gateway-static.json")).unwrap();
    let gateway = Gateway {
        contracts: Contracts::new().unwrap(),
        extractors: Registry::new(config.extractors).unwrap(),
        policy: Arc::new(config.policy),
        audit: Arc::new(Acknowledged),
    };
    let request: AuthorizationRequest = serde_json::from_str(
        r#"{"toolId":"files.read","action":"read","arguments":{"path":"workspace/readme.txt"}}"#,
    )
    .unwrap();
    let context = RequestContext {
        request_id: "benchmark".into(),
        tenant_id: "tenant-demo".into(),
        user_id: "user-demo".into(),
        agent_id: "agent-demo".into(),
        device_id: None,
    };
    let runtime = tokio::runtime::Builder::new_current_thread()
        .enable_all()
        .build()
        .unwrap();
    runtime.block_on(async {
        let now = unix_ms().unwrap();
        for _ in 0..1000 { black_box(gateway.authorize(request.clone(), context.clone(), "1".repeat(32), now).await.unwrap()); }
        let mut samples = Vec::with_capacity(20000);
        for _ in 0..20000 {
            let started = Instant::now();
            black_box(gateway.authorize(request.clone(), context.clone(), "1".repeat(32), now).await.unwrap());
            samples.push(started.elapsed().as_nanos() as u64);
        }
        samples.sort_unstable();
        println!("{}", serde_json::json!({"benchmark":"authorization-core", "iterations":samples.len(), "warmup":1000, "p50Ns":samples[10000], "p95Ns":samples[19000], "p99Ns":samples[19800], "meanNs":samples.iter().sum::<u64>()/samples.len() as u64, "audit":"immediate acknowledgement; no collector IO", "profile":"release", "contracts":olo_toolgate_contracts::ContractSet::current().version}));
    });
}
