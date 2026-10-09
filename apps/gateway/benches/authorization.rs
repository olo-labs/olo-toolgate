// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
//! Ingress parsing/contract validation microbenchmark. Online PDP latency is measured separately.
fn main() {
    let contracts = olo_toolgate_gateway::validation::Contracts::new().unwrap();
    let input =
        br#"{"toolId":"files.read","action":"read","arguments":{"path":"data/report.txt"}}"#;
    let start = std::time::Instant::now();
    for _ in 0..10000 {
        let value =
            olo_toolgate_gateway::validation::strict_json(std::hint::black_box(input)).unwrap();
        assert!(contracts.valid("AuthorizationRequest", &value));
    }
    println!(
        "{{\"benchmark\":\"ingress-validation\",\"iterations\":10000,\"elapsedMicros\":{}}}",
        start.elapsed().as_micros()
    );
}
