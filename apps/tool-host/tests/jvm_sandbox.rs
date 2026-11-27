// Copyright 2026 OLO Labs; SPDX-License-Identifier: Apache-2.0
//! Live Java launcher/lifecycle vectors. These are not Marketplace acceptance.
#![cfg(target_os = "linux")]
use olo_toolgate_contracts::v2::ToolInput;
use serde_json::{json, Value};
use std::{path::Path, sync::Arc, time::Duration};
use tool_host::{
    broker::Invocation,
    runner::Ran,
    sandbox::{Config, Sandbox},
};

fn configuration() -> Config {
    Config {
        podman_path: "/usr/bin/podman".into(),
        cgroup_parent: std::env::var("TOOLGATE_SANDBOX_CGROUP_PARENT").unwrap(),
        python_image: std::env::var("TOOLGATE_SANDBOX_IMAGE").unwrap(),
        jvm_image: Some(
            std::env::var("TOOLGATE_SANDBOX_JVM_IMAGE").expect("qualified JVM image required"),
        ),
        broker_image: std::env::var("TOOLGATE_SANDBOX_BROKER_IMAGE").unwrap(),
        io_device: std::env::var("TOOLGATE_SANDBOX_IO_DEVICE").unwrap(),
        io_bytes_per_second: 64 * 1024 * 1024,
        scratch_bytes: 8 * 1024 * 1024,
    }
}
fn input(id: &str, deadline: u64) -> ToolInput {
    let root = Path::new(env!("CARGO_MANIFEST_DIR")).join("../..");
    let fixtures: Value = serde_json::from_slice(
        &std::fs::read(root.join("tests/fixtures/contracts/v2/valid.json")).unwrap(),
    )
    .unwrap();
    let mut value = fixtures["ToolInput"].clone();
    value["invocationId"] = json!(id);
    value["entryPoint"] = json!("JavaWorker");
    value["arguments"] = json!({"mode": id});
    value["deadlineUnixMs"] = json!(deadline);
    serde_json::from_value(value).unwrap()
}
fn broker(input: &ToolInput) -> Arc<Invocation> {
    let control = tool_host::control::Control::new(&tool_host::config::ControlConfig {
        tenant_id: "10000000-0000-4000-8000-000000000001".into(),
        url: "http://127.0.0.1:1".into(),
        identity_trust_directory: None,
        client_certificate_path: None,
        client_key_path: None,
        ca_certificate_path: None,
        development_loopback_http: true,
    })
    .unwrap();
    let mut broker = Invocation::new(
        input.invocation_id.clone(),
        "fence".into(),
        input.deadline_unix_ms,
        "fixture".into(),
        format!("sha256:{}", "a".repeat(64)),
        vec![],
        vec![],
        vec![],
        vec![],
        vec![],
        Default::default(),
        true,
        Arc::new(control),
    );
    broker.invocation_permit = Some(olo_toolgate_contracts::v2::SignedInvocationPermit {
        jws: "containment-fixture-only".into(),
    });
    Arc::new(broker)
}

#[tokio::test]
#[ignore = "requires a delegated rootless service and JVM image; run tools/security/sandbox.py --jvm-image"]
async fn java_worker_preserves_protocol_denies_network_and_cleans_up_after_failure_timeout_and_cancel(
) {
    let directory = std::env::temp_dir().join(format!("tg-java-{}", tool_host::random_hex(16)));
    std::fs::create_dir_all(&directory).unwrap();
    std::fs::write(directory.join("JavaWorker.java"), r#"
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
public final class JavaWorker {
 public static void main(String[] args) throws Exception {
  var in = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
  var input = in.readLine();
  if (input.contains("crash")) System.exit(23);
  if (input.contains("timeout") || input.contains("cancel")) Thread.sleep(60000);
  boolean denied = false;
  try (var socket = new ServerSocket(0)) {} catch (SocketException failure) {
   denied = failure.getMessage().contains("Operation not permitted");
  }
  if (!denied) throw new IllegalStateException("network available");
  System.out.println("{\"kind\":\"BROKER_REQUEST\",\"requestId\":\"denied\",\"operation\":\"ROUTE_CALL\",\"request\":{\"route\":\"not-granted\",\"method\":\"GET\",\"destination\":\"not-granted\"}}");
  System.out.flush();
  if (!in.readLine().contains("refused")) throw new IllegalStateException("broker granted undeclared route");
  System.out.println("{\"kind\":\"RESULT\",\"resultType\":\"complete\",\"content\":[],\"structuredContent\":{\"denied\":true},\"isError\":false}");
 }
}
"#).unwrap();
    for (command, args) in [
        ("javac", vec!["--release", "21", "JavaWorker.java"]),
        (
            "jar",
            vec!["--create", "--file", "worker.jar", "JavaWorker.class"],
        ),
    ] {
        assert!(std::process::Command::new(command)
            .current_dir(&directory)
            .args(args)
            .status()
            .unwrap()
            .success());
    }
    let pool = Sandbox::open(configuration(), &directory, "java-fixture", 1).unwrap();
    assert!(!pool.available());
    pool.qualify().await.unwrap();
    for mode in ["success", "crash", "timeout", "cancel"] {
        let deadline = tool_host::now_ms() + if mode == "timeout" { 3000 } else { 20_000 };
        let input = input(mode, deadline);
        let (sink, _source) = tool_host::events::channel();
        let run = {
            let pool = pool.clone();
            let artifact = directory.join("worker.jar");
            tokio::spawn(async move {
                pool.run(
                    mode,
                    "jvm",
                    &artifact,
                    &input,
                    128 * 1024 * 1024,
                    1024 * 1024,
                    Arc::new(sink),
                    broker(&input),
                )
                .await
            })
        };
        if mode == "cancel" {
            tokio::time::sleep(Duration::from_millis(100)).await;
            assert!(
                pool.stop(mode).await,
                "cancellation must confirm paired cleanup"
            );
        }
        let result = tokio::time::timeout(Duration::from_secs(30), run)
            .await
            .unwrap()
            .unwrap();
        match mode {
            "success" => assert!(
                matches!(result, Ran::Frame(ref frame) if !frame.is_error && frame.structured_content.as_ref().and_then(|value| value.get("denied")) == Some(&json!(true))),
                "{result:?}"
            ),
            "crash" => assert!(matches!(result, Ran::Crashed(_)), "{result:?}"),
            "timeout" => assert!(matches!(result, Ran::TimedOut), "{result:?}"),
            _ => assert!(
                matches!(result, Ran::TimedOut | Ran::Crashed(_)),
                "{result:?}"
            ),
        }
        assert!(
            pool.available(),
            "confirmed teardown must leave the pool usable"
        );
        assert!(std::fs::read_dir(directory.join("sandbox-spool"))
            .unwrap()
            .all(|entry| entry.unwrap().file_name() == "seccomp.json"));
    }
    // Optional real SDK artifacts are supplied by a producing package gate.
    // No expected-output fixture is substituted for an executed result.
    if let Ok(path) = std::env::var("TOOLGATE_SANDBOX_JVM_SAMPLES") {
        let samples: Vec<Value> = serde_json::from_slice(&std::fs::read(path).unwrap()).unwrap();
        assert!(!samples.is_empty());
        for (number, sample) in samples.iter().enumerate() {
            let artifact = std::path::PathBuf::from(sample["artifact"].as_str().unwrap());
            assert_eq!(
                toolgate_cli::canonical::sha256_tagged(&std::fs::read(&artifact).unwrap()),
                sample["artifactDigest"].as_str().unwrap()
            );
            // Admission unpacks artifacts into the service-owned content store.
            // Match that ownership and filesystem instead of copying directly
            // from an arbitrary caller or Windows mount.
            let staged = directory.join(format!("sdk-{number}.jar"));
            std::fs::copy(&artifact, &staged).unwrap();
            let mut input = input(&format!("sdk-{number}"), tool_host::now_ms() + 20_000);
            input.entry_point = "io.ololabs.toolgate.runtime.Main".into();
            input.tool_id = sample["toolId"].as_str().unwrap().into();
            input.arguments = serde_json::from_value(sample["arguments"].clone()).unwrap();
            let (sink, _source) = tool_host::events::channel();
            let result = pool
                .run(
                    &input.invocation_id,
                    "jvm",
                    &staged,
                    &input,
                    256 * 1024 * 1024,
                    1024 * 1024,
                    Arc::new(sink),
                    broker(&input),
                )
                .await;
            assert!(
                matches!(result, Ran::Frame(ref frame) if !frame.is_error),
                "SDK sample {}: {result:?}",
                input.tool_id
            );
            assert!(pool.available());
            println!("SDK sample executed: {}", input.tool_id);
        }
    }
    std::fs::remove_dir_all(directory).unwrap();
}
