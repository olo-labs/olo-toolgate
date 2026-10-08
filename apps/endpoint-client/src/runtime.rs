// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
//! Protected service lifecycle, bounded IPC and one serialized heartbeat loop.
use crate::{
    config::Config, contracts::Contracts, identity::DeviceKey, service::ClientService,
    storage::ProtectedStore, transport::HttpsControl, Failure, Result,
};
use std::sync::Arc;
/// Fixed worker limits keep system services within their OS task/memory budget,
/// including machines with many logical CPUs. No environment-driven thread explosion.
pub fn executor() -> Result<tokio::runtime::Runtime> {
    tokio::runtime::Builder::new_multi_thread()
        .worker_threads(2)
        .max_blocking_threads(8)
        .enable_all()
        .build()
        .map_err(|_| Failure::Unavailable)
}
pub async fn run(config: Config, shutdown: tokio::sync::watch::Receiver<bool>) -> Result<()> {
    crate::platform::require_service_identity()?;
    let store = ProtectedStore::open(config.state_directory.clone())?;
    let key = Arc::new(DeviceKey::load_or_create(&store)?);
    let contracts = Arc::new(Contracts::new()?);
    let control = Arc::new(HttpsControl::new(
        config.clone(),
        key.clone(),
        contracts.clone(),
    )?);
    let service = Arc::new(tokio::sync::Mutex::new(ClientService::open(
        config.clone(),
        store,
        key,
        control,
    )?));
    let mut heartbeat_shutdown = shutdown.clone();
    let heartbeat_service = service.clone();
    let heartbeat = tokio::spawn(async move {
        let mut next = tokio::time::Instant::now();
        loop {
            tokio::select! {_=heartbeat_shutdown.changed()=>break,_=tokio::time::sleep_until(next)=>{}}
            let started = tokio::time::Instant::now();
            let delay = {
                let mut state = heartbeat_service.lock().await;
                let _ = state.tick().await;
                state.next_delay_millis()
            };
            // Network time counts toward the cycle; never overlap or catch up missed requests.
            next = (started + std::time::Duration::from_millis(delay))
                .max(tokio::time::Instant::now());
        }
    });
    tracing::info!(
        event = "client_service",
        result = "started",
        version = env!("CARGO_PKG_VERSION")
    );
    let result = crate::ipc::listen(
        &config.ipc_endpoint,
        config.authorized_peers,
        service.clone(),
        contracts,
        shutdown,
    )
    .await;
    heartbeat.abort();
    heartbeat.await.map(|_| ()).or_else(|e| {
        if e.is_cancelled() {
            Ok(())
        } else {
            Err(Failure::Unavailable)
        }
    })?;
    tracing::info!(event = "client_service", result = "stopped");
    let cleanup = service.lock().await.shutdown_runtimes().await;
    if let Err(failure) = cleanup {
        tracing::warn!(event="runtime_shutdown",error=?failure);
    }
    result.and(cleanup)
}
#[cfg(windows)]
pub mod windows {
    use super::*;
    use windows_service::{
        define_windows_service,
        service::*,
        service_control_handler::{self, ServiceControlHandlerResult},
        service_dispatcher,
    };
    define_windows_service!(ffi_service, entry);
    pub fn dispatch() -> Result<()> {
        service_dispatcher::start("OloToolGateClient", ffi_service)
            .map_err(|_| Failure::Unavailable)
    }
    fn entry(_arguments: Vec<std::ffi::OsString>) {
        let (tx, rx) = tokio::sync::watch::channel(false);
        let Ok(handle) =
            service_control_handler::register("OloToolGateClient", move |event| match event {
                ServiceControl::Stop | ServiceControl::Shutdown => {
                    let _ = tx.send(true);
                    ServiceControlHandlerResult::NoError
                }
                ServiceControl::Interrogate => ServiceControlHandlerResult::NoError,
                _ => ServiceControlHandlerResult::NotImplemented,
            })
        else {
            return;
        };
        let status = |state, code| ServiceStatus {
            service_type: ServiceType::OWN_PROCESS,
            current_state: state,
            controls_accepted: ServiceControlAccept::STOP | ServiceControlAccept::SHUTDOWN,
            exit_code: ServiceExitCode::Win32(code),
            checkpoint: 0,
            wait_hint: std::time::Duration::default(),
            process_id: None,
        };
        let outcome = (|| {
            let config = Config::load(&crate::install::config_path())?;
            handle
                .set_service_status(status(ServiceState::Running, 0))
                .map_err(|_| Failure::Unavailable)?;
            executor()?.block_on(run(config, rx))
        })();
        let _ = handle.set_service_status(status(
            ServiceState::Stopped,
            if outcome.is_ok() { 0 } else { 1 },
        ));
    }
}
