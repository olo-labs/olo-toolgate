// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
//! Real native protocol fixture; never registered in production by default.
use std::io::Read;
fn main() {
    if std::env::args().skip(1).eq(["--version"]) {
        println!("1.0.0");
        return;
    }
    let mut bytes = Vec::new();
    std::io::stdin()
        .take(65537)
        .read_to_end(&mut bytes)
        .unwrap();
    let request: olo_toolgate_contracts::LocalToolInput = serde_json::from_slice(&bytes).unwrap();
    let response = olo_toolgate_contracts::LocalToolOutput {
        protocol_version: 1,
        request_id: request.request_id,
        output: std::collections::BTreeMap::from([(
            "text".into(),
            request.arguments["text"].clone(),
        )]),
    };
    println!("{}", serde_json::to_string(&response).unwrap());
}
