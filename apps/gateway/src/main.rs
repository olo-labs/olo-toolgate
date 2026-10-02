// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
//! Service bootstrap. Secrets/config contents are never included in diagnostics.
use olo_toolgate_gateway::{
    application::Gateway,
    audit::JsonAudit,
    auth::{Authenticator, Credential},
    bundles::{BundleKeyring, BundleVerifier, VerifiedPolicy},
    config::{read_json, Config},
    extraction::Registry,
    http::{management_router, runtime_router, AppState},
    policy::PolicyEvaluator,
    server, unix_ms,
    validation::Contracts,
};
use std::{
    path::PathBuf,
    sync::{atomic::Ordering, Arc},
    time::Duration,
};
use tokio::{net::TcpListener, sync::watch};

#[tokio::main]
async fn main() {
    tracing_subscriber::fmt()
        .json()
        .with_target(false)
        .with_writer(std::io::stderr)
        .init();
    if let Err(message) = run().await {
        tracing::error!(
            service = "gateway",
            version = env!("CARGO_PKG_VERSION"),
            event = "startup_or_runtime_failure",
            reason = message
        );
        std::process::exit(1);
    }
}

async fn run() -> Result<(), &'static str> {
    let contracts = Contracts::new()?;
    let config_path = PathBuf::from(
        std::env::var("TOOLGATE_GATEWAY_CONFIG")
            .map_err(|_| "TOOLGATE_GATEWAY_CONFIG is required")?,
    );
    let credentials_path = PathBuf::from(
        std::env::var("TOOLGATE_GATEWAY_CREDENTIALS")
            .map_err(|_| "TOOLGATE_GATEWAY_CREDENTIALS is required")?,
    );
    let config: Config = read_json(&config_path)?;
    let credentials: Vec<Credential> = read_json(&credentials_path)?;
    let now = unix_ms().ok_or("system clock unavailable")?;
    config.validate(&contracts, now)?;
    let auth = Authenticator::new(credentials, &contracts, now)?;
    let extractors = Registry::new(config.extractors.clone())?;
    let runtime = TcpListener::bind(config.listen)
        .await
        .map_err(|_| "runtime listener bind failed")?;
    let management = TcpListener::bind(config.management_listen)
        .await
        .map_err(|_| "management listener bind failed")?;
    let (audit, audit_worker) =
        JsonAudit::new(config.limits.audit_queue_capacity, std::io::stdout());
    let (shutdown, receiver) = watch::channel(false);
    let mut bundle_receiver = receiver.clone();
    let (policy, mut bundle_worker): (Arc<dyn PolicyEvaluator>, _) =
        if let Some(source) = &config.bundle_source {
            let keyring: BundleKeyring = read_json(&source.keyring_path)?;
            let verified = Arc::new(VerifiedPolicy::new(BundleVerifier::new(
                source.clone(),
                keyring,
            )?));
            let polling = verified.clone();
            (
                verified,
                tokio::spawn(olo_toolgate_gateway::bundles::poll(
                    polling,
                    bundle_receiver,
                )),
            )
        } else {
            (
                Arc::new(config.policy.clone().ok_or("static policy required")?),
                tokio::spawn(async move {
                    let _ = bundle_receiver.changed().await;
                    Ok(())
                }),
            )
        };
    let gateway = Gateway {
        contracts,
        extractors,
        policy,
        audit: Arc::new(audit),
    };
    let state = Arc::new(AppState::new(gateway, auth, config.clone()));
    let runtime_task = tokio::spawn(server::serve(
        runtime,
        runtime_router(state.clone()),
        config.limits.clone(),
        receiver.clone(),
    ));
    let management_task = tokio::spawn(server::serve(
        management,
        management_router(state.clone()),
        config.limits.clone(),
        receiver,
    ));
    tracing::info!(
        service = "gateway",
        version = env!("CARGO_PKG_VERSION"),
        event = "started",
        runtime_port = config.listen.port(),
        management_port = config.management_listen.port()
    );
    let mut runtime_task = runtime_task;
    let mut management_task = management_task;
    let mut failed = false;
    tokio::select! {
        signal = shutdown_signal() => { if signal.is_err() { failed = true; } },
        _ = &mut runtime_task => { failed = true; },
        _ = &mut management_task => { failed = true; },
        _ = &mut bundle_worker => { failed = true; },
    }
    state.draining.store(true, Ordering::Release);
    let _ = shutdown.send(true);
    if !bundle_worker.is_finished()
        && tokio::time::timeout(
            Duration::from_millis(config.limits.shutdown_timeout_ms),
            &mut bundle_worker,
        )
        .await
        .is_err()
    {
        bundle_worker.abort();
    }
    tracing::info!(
        service = "gateway",
        version = env!("CARGO_PKG_VERSION"),
        event = "draining"
    );
    if !runtime_task.is_finished() {
        let _ = (&mut runtime_task).await;
    }
    if !management_task.is_finished() {
        let _ = (&mut management_task).await;
    }
    drop(state);
    // Closing every sender lets the acknowledged audit queue drain naturally.
    if tokio::time::timeout(
        Duration::from_millis(config.limits.shutdown_timeout_ms),
        audit_worker,
    )
    .await
    .is_err()
    {
        // Blocking stdout can be held by a failed collector. Bound process exit.
        tracing::error!(service = "gateway", event = "audit_drain_timeout");
        std::process::exit(1);
    }
    tracing::info!(
        service = "gateway",
        version = env!("CARGO_PKG_VERSION"),
        event = "stopped"
    );
    if failed {
        Err("listener or signal subsystem failed")
    } else {
        Ok(())
    }
}

async fn shutdown_signal() -> Result<(), std::io::Error> {
    #[cfg(unix)]
    {
        let mut term = tokio::signal::unix::signal(tokio::signal::unix::SignalKind::terminate())?;
        tokio::select! { result = tokio::signal::ctrl_c() => result, _ = term.recv() => Ok(()) }
    }
    #[cfg(not(unix))]
    {
        tokio::signal::ctrl_c().await
    }
}
