// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
//! One-shot, unprivileged Chrome host. No secrets, arbitrary commands, URLs or shell execution.
use std::io::{Read, Write};

#[derive(serde::Deserialize, Debug, PartialEq, Eq)]
#[serde(rename_all = "lowercase")]
enum Operation {
    Health,
    Enroll,
}
#[derive(serde::Deserialize)]
#[serde(deny_unknown_fields)]
struct Request {
    operation: Operation,
}
fn read_frame(input: &mut impl Read) -> std::io::Result<Request> {
    let mut size = [0; 4];
    input.read_exact(&mut size)?;
    let size = u32::from_le_bytes(size) as usize;
    if size == 0 || size > 1024 {
        return Err(std::io::ErrorKind::InvalidData.into());
    }
    let mut bytes = vec![0; size];
    input.read_exact(&mut bytes)?;
    serde_json::from_slice(&bytes).map_err(|_| std::io::ErrorKind::InvalidData.into())
}
fn main() {
    let origin = std::env::args().nth(1).unwrap_or_default();
    if origin != "chrome-extension://emmemldedebhbloibichmmdlbpjakfkf/" {
        std::process::exit(1);
    }
    let Ok(request) = read_frame(&mut std::io::stdin().lock()) else {
        std::process::exit(1);
    };
    let operation = match request.operation {
        Operation::Health => olo_toolgate_contracts::ClientIpcOperation::Health,
        Operation::Enroll => olo_toolgate_contracts::ClientIpcOperation::Enroll,
    };
    let response = olo_toolgate_client::runtime::executor().and_then(|runtime| {
        runtime.block_on(async {
            tokio::time::timeout(
                std::time::Duration::from_secs(20),
                olo_toolgate_client::ipc::call(
                    &olo_toolgate_client::install::ipc_endpoint(),
                    operation,
                ),
            )
            .await
            .map_err(|_| olo_toolgate_client::Failure::Expired)?
        })
    });
    let value = match response {
        Ok(value) => {
            serde_json::to_value(value).unwrap_or(serde_json::json!({"error":"UNAVAILABLE"}))
        }
        Err(_) => serde_json::json!({"error":"UNAVAILABLE"}),
    };
    let bytes = serde_json::to_vec(&value).unwrap_or_default();
    if bytes.is_empty() || bytes.len() > 16384 {
        std::process::exit(1);
    }
    let mut output = std::io::stdout().lock();
    if output
        .write_all(&(bytes.len() as u32).to_le_bytes())
        .is_err()
        || output.write_all(&bytes).is_err()
        || output.flush().is_err()
    {
        std::process::exit(1);
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    #[test]
    fn bounded_little_endian_protocol() {
        let body = br#"{"operation":"health"}"#;
        let mut bytes = (body.len() as u32).to_le_bytes().to_vec();
        bytes.extend(body);
        assert_eq!(
            read_frame(&mut bytes.as_slice()).unwrap().operation,
            Operation::Health
        );
        for invalid in [0u32, 1025, u32::MAX] {
            assert!(read_frame(&mut invalid.to_le_bytes().as_slice()).is_err());
        }
        assert!(read_frame(&mut [3, 0, 0, 0, 255, 255, 255].as_slice()).is_err());
        for invalid in [
            r#"{"operation":"health","operation":"enroll"}"#,
            r#"{"operation":"execute"}"#,
            r#"{"operation":"health","server":"https://evil.example"}"#,
        ] {
            let mut bytes = (invalid.len() as u32).to_le_bytes().to_vec();
            bytes.extend(invalid.as_bytes());
            assert!(read_frame(&mut bytes.as_slice()).is_err());
        }
    }
}
