// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
//! Negative filesystem and policy tests use the same fixed executor as the service.
use olo_toolgate_client::{
    builtins::{calculate, AuthorizationPort, Executor, Settings},
    hotfolder::{components, HotFolder, Settings as FolderSettings},
    transport::Call,
    Failure, Result,
};
use olo_toolgate_contracts::{AuthorizationRequest, BuiltinInvocation};
use serde_json::{json, Value};
use std::{
    path::PathBuf,
    sync::{Arc, Mutex},
};
struct Directory(PathBuf);
impl Directory {
    fn new() -> Self {
        #[cfg(windows)]
        let base = PathBuf::from(std::env::var_os("ProgramData").unwrap());
        // macOS /tmp is a symlink; exercise custody through its real protected parent.
        #[cfg(target_os = "macos")]
        let base = PathBuf::from("/private/tmp");
        #[cfg(all(unix, not(target_os = "macos")))]
        let base = std::env::temp_dir();
        let path = base.join(format!(
            "toolgate-builtins-{}",
            olo_toolgate_client::identity::nonce().unwrap()
        ));
        let _store = olo_toolgate_client::storage::ProtectedStore::open(path.clone()).unwrap();
        Self(path)
    }
    fn settings(&self) -> FolderSettings {
        FolderSettings {
            root: self.0.clone(),
            max_file_bytes: 64,
            max_entries: 10,
            extensions: vec!["txt".into(), "json".into()],
        }
    }
}
impl Drop for Directory {
    fn drop(&mut self) {
        std::fs::remove_dir_all(&self.0).unwrap();
    }
}
struct Policy {
    result: Result<()>,
    requests: Mutex<Vec<AuthorizationRequest>>,
}
impl AuthorizationPort for Policy {
    fn authorize(&self, request: AuthorizationRequest) -> Call<'_, ()> {
        self.requests.lock().unwrap().push(request);
        Box::pin(async { self.result })
    }
}
fn settings(dir: &Directory) -> Settings {
    Settings {
        hotfolder: dir.settings(),
        gateway_url: "https://gateway.example.test".into(),
        gateway_token_path: dir.0.join("token"),
        gateway_ca_path: None,
        device_id: "device-test".into(),
        authorization_profiles: olo_toolgate_client::authorization_profile::builtin_profiles()
            .unwrap(),
        web_search_token_path: None,
        web_search_allowed_domains: Vec::new(),
    }
}
fn invocation(tool: &str, args: Value) -> BuiltinInvocation {
    BuiltinInvocation {
        tool_id: tool.into(),
        arguments: serde_json::from_value(args).unwrap(),
    }
}
#[test]
fn portable_paths_reject_aliases_and_traversal() {
    for path in [
        "",
        "/etc/passwd",
        "../note.txt",
        "a/../note.txt",
        "a//b.txt",
        "a/./b.txt",
        "C:/note.txt",
        "a\\b.txt",
        "a.txt:stream",
        "%2e%2e/x.txt",
        "NUL.txt",
        "COM1.txt",
        "note.txt.",
        "a.txt ",
        ".secret.txt",
        "Notes/a.txt",
        "notes/A.txt",
    ] {
        assert!(components(path).is_err(), "{path}");
    }
    assert!(components("notes/a_b-1.txt").is_ok());
}
#[test]
fn limits_atomic_replacement_and_transfer() {
    let dir = Directory::new();
    let folder = HotFolder::open(dir.settings()).unwrap();
    folder.mkdir("notes").unwrap();
    folder.write("notes/a.txt", b"one", false).unwrap();
    folder.write("notes/a.txt", b" two", true).unwrap();
    assert_eq!(folder.read("notes/a.txt").unwrap(), b"one two");
    assert!(folder.write("bad.exe", b"one", false).is_err());
    assert!(folder.write("notes/a.txt", &[b'x'; 65], false).is_err());
    assert_eq!(folder.read("notes/a.txt").unwrap(), b"one two");
    folder.transfer("notes/a.txt", "copy.txt", false).unwrap();
    folder.transfer("copy.txt", "moved.txt", true).unwrap();
    assert!(folder.read("copy.txt").is_err());
    assert!(folder.transfer("notes/a.txt", "moved.txt", false).is_err());
    assert!(folder.read("../outside.txt").is_err());
    assert!(folder.read("missing/a.txt").is_err());
}
#[cfg(unix)]
#[test]
fn symlinks_hardlinks_and_fifos_are_rejected() {
    use std::os::unix::{ffi::OsStrExt, fs::symlink};
    let dir = Directory::new();
    let outside = Directory::new();
    let folder = HotFolder::open(dir.settings()).unwrap();
    folder.write("real.txt", b"original", false).unwrap();
    std::fs::hard_link(dir.0.join("real.txt"), dir.0.join("linked.txt")).unwrap();
    assert!(folder.read("linked.txt").is_err());
    assert!(folder.write("real.txt", b"changed", false).is_err());
    symlink(&outside.0, dir.0.join("escape")).unwrap();
    symlink(dir.0.join("real.txt"), dir.0.join("symlink.txt")).unwrap();
    assert!(folder.read("escape/a.txt").is_err());
    assert!(folder.read("symlink.txt").is_err());
    assert!(folder.write("symlink.txt", b"changed", false).is_err());
    let fifo = std::ffi::CString::new(dir.0.join("pipe.txt").as_os_str().as_bytes()).unwrap();
    assert_eq!(unsafe { libc::mkfifo(fifo.as_ptr(), 0o600) }, 0);
    assert!(folder.read("pipe.txt").is_err());
    assert!(folder.list().is_err());
}
#[tokio::test]
async fn every_call_requires_online_policy_and_copy_binds_both_paths() {
    let dir = Directory::new();
    let deny = Arc::new(Policy {
        result: Err(Failure::Unavailable),
        requests: Mutex::new(Vec::new()),
    });
    let mut executor = Executor::new(&settings(&dir), deny.clone()).unwrap();
    assert_eq!(
        executor
            .execute(
                invocation(
                    "hotfolder.write_text",
                    json!({"path":"a.txt","text":"test"})
                ),
                u64::MAX
            )
            .await,
        Err(Failure::Unavailable)
    );
    assert!(!dir.0.join("a.txt").exists());
    assert!(executor
        .execute(invocation("system.info", json!({})), u64::MAX)
        .await
        .is_err());
    assert!(executor
        .execute(
            invocation("hotfolder.delete", json!({"path":"a.txt"})),
            u64::MAX
        )
        .await
        .is_err());
    assert!(
        !executor
            .catalog()
            .iter()
            .find(|t| t.tool_id == "web.search")
            .unwrap()
            .enabled
    );
    let allow = Arc::new(Policy {
        result: Ok(()),
        requests: Mutex::new(Vec::new()),
    });
    let mut executor = Executor::new(&settings(&dir), allow.clone()).unwrap();
    executor
        .execute(
            invocation(
                "hotfolder.write_text",
                json!({"path":"a.txt","text":"test"}),
            ),
            u64::MAX,
        )
        .await
        .unwrap();
    executor
        .execute(
            invocation(
                "hotfolder.copy",
                json!({"path":"a.txt","destination":"b.txt"}),
            ),
            u64::MAX,
        )
        .await
        .unwrap();
    let requests = allow.requests.lock().unwrap();
    assert_eq!(requests.len(), 2);
    assert_eq!(requests[1].arguments["path"], "a.txt");
    assert_eq!(requests[1].arguments["destination"], "b.txt");
}
#[tokio::test]
async fn concurrent_append_is_serialized_and_inputs_are_closed() {
    let dir = Directory::new();
    let allow = Arc::new(Policy {
        result: Ok(()),
        requests: Mutex::new(Vec::new()),
    });
    let mut executor = Executor::new(&settings(&dir), allow).unwrap();
    executor
        .execute(
            invocation("hotfolder.write_text", json!({"path":"a.txt","text":""})),
            u64::MAX,
        )
        .await
        .unwrap();
    let executor = Arc::new(tokio::sync::Mutex::new(executor));
    let mut tasks = Vec::new();
    for _ in 0..20 {
        let executor = executor.clone();
        tasks.push(tokio::spawn(async move {
            executor
                .lock()
                .await
                .execute(
                    invocation("hotfolder.append_text", json!({"path":"a.txt","text":"x"})),
                    u64::MAX,
                )
                .await
                .unwrap();
        }));
    }
    for task in tasks {
        task.await.unwrap();
    }
    assert_eq!(std::fs::read(dir.0.join("a.txt")).unwrap(), vec![b'x'; 20]);
    assert!(executor
        .lock()
        .await
        .execute(
            invocation("system.info", json!({"command":"uname"})),
            u64::MAX
        )
        .await
        .is_err());
    assert!(executor
        .lock()
        .await
        .execute(invocation("system.info", json!({})), 0)
        .await
        .is_err());
}
#[test]
fn calculator_has_no_evaluation_or_nonfinite_results() {
    assert_eq!(calculate("2*(3+4)-1").unwrap(), 13.0);
    assert_eq!(calculate("-3 + 2 / 4").unwrap(), -2.5);
    for expression in [
        "1/0",
        "NaN",
        "system('id')",
        "1;2",
        "1e309",
        "((((((((((((((((((((((((((((((((((((((1))))))))))))))))))))))))))))))))))))))",
    ] {
        assert!(calculate(expression).is_err());
    }
}

#[tokio::test]
async fn fixed_tools_and_paged_event_cursor_preserve_authorized_results() {
    let dir = Directory::new();
    let allow = Arc::new(Policy {
        result: Ok(()),
        requests: Mutex::new(Vec::new()),
    });
    let mut executor = Executor::new(&settings(&dir), allow).unwrap();
    let output = executor
        .execute(
            invocation("text.transform", json!({"text":" Mixed ","mode":"trim"})),
            u64::MAX,
        )
        .await
        .unwrap();
    assert_eq!(output["text"], "Mixed");
    assert!(executor
        .execute(
            invocation("json.validate", json!({"text":"{\"valid\":true}"})),
            u64::MAX
        )
        .await
        .is_ok());
    assert!(executor
        .execute(invocation("json.validate", json!({"text":"{"})), u64::MAX)
        .await
        .is_err());
    assert_eq!(
        executor
            .execute(invocation("hash.sha256", json!({"text":"abc"})), u64::MAX)
            .await
            .unwrap()["sha256"],
        olo_toolgate_client::digest(b"abc")
    );
    assert_eq!(
        executor
            .execute(
                invocation("calculator.evaluate", json!({"expression":"2*(3+4)"})),
                u64::MAX
            )
            .await
            .unwrap()["value"],
        14.0
    );
    executor
        .execute(
            invocation("hotfolder.mkdir", json!({"path":"notes"})),
            u64::MAX,
        )
        .await
        .unwrap();
    for _ in 0..260 {
        executor
            .execute(
                invocation(
                    "hotfolder.write_text",
                    json!({"path":"notes/a.txt","text":"one\ntwo"}),
                ),
                u64::MAX,
            )
            .await
            .unwrap();
    }
    assert_eq!(
        executor
            .execute(
                invocation(
                    "hotfolder.search_text",
                    json!({"path":"notes/a.txt","query":"two"})
                ),
                u64::MAX
            )
            .await
            .unwrap()["lines"],
        json!([2])
    );
    assert_eq!(
        executor
            .execute(
                invocation("hotfolder.hash", json!({"path":"notes/a.txt"})),
                u64::MAX
            )
            .await
            .unwrap()["sha256"],
        olo_toolgate_client::digest(b"one\ntwo")
    );
    assert_eq!(
        executor
            .execute(
                invocation("hotfolder.file_info", json!({"path":"notes/a.txt"})),
                u64::MAX
            )
            .await
            .unwrap()["bytes"],
        7
    );
    assert_eq!(
        executor
            .execute(invocation("hotfolder.list", json!({})), u64::MAX)
            .await
            .unwrap()["paths"],
        json!(["notes", "notes/a.txt"])
    );
    let first = executor
        .execute(
            invocation("hotfolder.watch_events", json!({"after":0})),
            u64::MAX,
        )
        .await
        .unwrap();
    assert_eq!(first["reset"], true);
    assert_eq!(first["events"].as_array().unwrap().len(), 100);
    assert!(first["sequence"].as_u64().unwrap() < first["latestSequence"].as_u64().unwrap());
    let second = executor
        .execute(
            invocation(
                "hotfolder.list_events",
                json!({"after":first["sequence"],"epoch":first["epoch"]}),
            ),
            u64::MAX,
        )
        .await
        .unwrap();
    assert_eq!(second["reset"], false);
    assert_eq!(
        second["events"][0]["sequence"].as_u64().unwrap(),
        first["sequence"].as_u64().unwrap() + 1
    );
    let restarted = Executor::new(
        &settings(&dir),
        Arc::new(Policy {
            result: Ok(()),
            requests: Mutex::new(Vec::new()),
        }),
    )
    .unwrap();
    let mut restarted = restarted;
    assert_eq!(
        restarted
            .execute(
                invocation(
                    "hotfolder.watch_events",
                    json!({"after":0,"epoch":first["epoch"]})
                ),
                u64::MAX
            )
            .await
            .unwrap()["reset"],
        true
    );
}

#[test]
fn nested_duplicate_arguments_are_rejected_before_authorization() {
    let contracts = olo_toolgate_client::contracts::Contracts::new().unwrap();
    let bytes=br#"{"protocolVersion":2,"requestId":"test-request","operation":"CALL","invocation":{"toolId":"hotfolder.read_text","arguments":{"path":"a.txt","path":"b.txt"}}}"#;
    assert!(contracts
        .decode::<olo_toolgate_contracts::BuiltinIpcRequest>("BuiltinIpcRequest", bytes)
        .is_err());
}

#[cfg(windows)]
#[test]
fn windows_hardlinks_and_inherited_custody_are_enforced() {
    let dir = Directory::new();
    let folder = HotFolder::open(dir.settings()).unwrap();
    folder.mkdir("notes").unwrap();
    folder.write("notes/a.txt", b"original", false).unwrap();
    olo_toolgate_client::storage::check_owned(&dir.0.join("notes"), true).unwrap();
    olo_toolgate_client::storage::check_owned(&dir.0.join("notes/a.txt"), true).unwrap();
    std::fs::hard_link(dir.0.join("notes/a.txt"), dir.0.join("linked.txt")).unwrap();
    assert!(folder.read("linked.txt").is_err());
    assert!(folder.write("notes/a.txt", b"changed", false).is_err());
}

#[cfg(windows)]
#[test]
fn installer_rejects_a_directory_writable_by_the_ordinary_caller() {
    let dir = Directory::new();
    olo_toolgate_client::storage::check_owned(&dir.0, true).unwrap();
    assert_eq!(
        olo_toolgate_client::platform::windows::check_install_acl(&dir.0, true),
        Err(Failure::Unauthorized)
    );
}

#[cfg(windows)]
#[test]
fn windows_leaf_acl_cannot_reintroduce_untrusted_writers() {
    let dir = Directory::new();
    let folder = HotFolder::open(dir.settings()).unwrap();
    folder.write("a.txt", b"original", false).unwrap();
    let result = std::process::Command::new(r"C:\Windows\System32\icacls.exe")
        .arg(dir.0.join("a.txt"))
        .args(["/grant", "*S-1-5-32-545:(W)"])
        .output()
        .unwrap();
    assert!(result.status.success());
    assert!(folder.read("a.txt").is_err());
    assert!(folder.write("a.txt", b"changed", false).is_err());
    assert!(folder.list().is_err());
}
