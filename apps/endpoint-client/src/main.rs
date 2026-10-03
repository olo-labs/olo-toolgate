// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
//! Unprivileged CLI; only the OS service entry point loads device secrets.
use olo_toolgate_client::{Failure, Result};
use olo_toolgate_contracts::ClientIpcOperation;
fn main() {
    tracing_subscriber::fmt()
        .json()
        .with_target(false)
        .with_writer(std::io::stderr)
        .init();
    let arguments: Vec<String> = std::env::args().skip(1).collect();
    #[cfg(windows)]
    if arguments == ["service"] {
        if let Err(failure) = olo_toolgate_client::runtime::windows::dispatch() {
            tracing::error!(event="client_startup",error=?failure);
            std::process::exit(1);
        }
        return;
    }
    let result = tokio::runtime::Runtime::new()
        .map_err(|_| Failure::Unavailable)
        .and_then(|runtime| runtime.block_on(command(arguments)));
    if let Err(failure) = result {
        tracing::error!(event="client_command",result="rejected",error=?failure);
        std::process::exit(1);
    }
}
async fn command(arguments: Vec<String>) -> Result<()> {
    let Some(operation) = arguments.first().map(String::as_str) else {
        return Err(Failure::Validation);
    };
    match operation {
        "version" if arguments.len() == 1 => {
            println!("{}", env!("CARGO_PKG_VERSION"));
            Ok(())
        }
        "install" if arguments.len() == 3 && arguments[1] == "--server" => {
            olo_toolgate_client::install::install(&arguments[2])
        }
        "uninstall" if arguments.len() == 1 || arguments == ["uninstall", "--purge"] => {
            olo_toolgate_client::install::uninstall(arguments.len() == 2)
        }
        "service" if arguments.len() == 1 || arguments.len() == 3 && arguments[1] == "--config" => {
            let path = if arguments.len() == 3 {
                std::path::PathBuf::from(&arguments[2])
            } else {
                olo_toolgate_client::install::config_path()
            };
            let config = olo_toolgate_client::config::Config::load(&path)?;
            let (tx, rx) = tokio::sync::watch::channel(false);
            tokio::spawn(async move {
                #[cfg(unix)]
                {
                    if let Ok(mut signal) =
                        tokio::signal::unix::signal(tokio::signal::unix::SignalKind::terminate())
                    {
                        tokio::select! {_=signal.recv()=>{},_=tokio::signal::ctrl_c()=>{}}
                    }
                }
                #[cfg(windows)]
                {
                    let _ = tokio::signal::ctrl_c().await;
                }
                let _ = tx.send(true);
            });
            olo_toolgate_client::runtime::run(config, rx).await
        }
        "health" | "enroll" | "check-in" if arguments.len() == 1 => {
            let request = match operation {
                "health" => ClientIpcOperation::Health,
                "enroll" => ClientIpcOperation::Enroll,
                _ => ClientIpcOperation::CheckIn,
            };
            let response = tokio::time::timeout(
                std::time::Duration::from_secs(20),
                olo_toolgate_client::ipc::call(
                    &olo_toolgate_client::install::ipc_endpoint(),
                    request,
                ),
            )
            .await
            .map_err(|_| Failure::Unavailable)??;
            if response.error.is_some() {
                return Err(Failure::Unavailable);
            }
            if let Some(challenge) = response.challenge {
                let url = format!(
                    "{}?code={}",
                    challenge.verification_uri, challenge.user_code
                );
                println!(
                    "Open {url} to confirm this device. Enrollment expires at {}.",
                    challenge.expires_at_unix_ms
                );
                println!(
                    "Compare this key fingerprint in your browser: {}",
                    challenge.key_fingerprint
                );
                // Browser launching belongs to the ordinary user process, never the protected service.
                if let Err(failure) = olo_toolgate_client::platform::open_browser(&url) {
                    tracing::warn!(event="browser_open",error=?failure);
                }
            }
            if let Some(health) = response.health {
                println!(
                    "{}",
                    serde_json::to_string(&health).map_err(|_| Failure::Validation)?
                );
            }
            Ok(())
        }
        _ => Err(Failure::Unsupported),
    }
}
