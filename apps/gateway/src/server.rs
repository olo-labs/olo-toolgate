// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
//! Bounded HTTP/1 connections with explicit drain and slow-client deadlines.
use crate::config::Limits;
use axum::Router;
use hyper_util::{
    rt::{TokioIo, TokioTimer},
    service::TowerToHyperService,
};
use std::sync::Arc;
use tokio::{
    net::TcpListener,
    sync::{watch, Semaphore},
    task::JoinSet,
    time::{timeout, Duration},
};

/// Stop accepting on shutdown, drain active requests and bound incomplete HTTP
/// headers/bodies. No permanent connection/session state is used for correctness.
pub async fn serve(
    listener: TcpListener,
    router: Router,
    limits: Limits,
    mut shutdown: watch::Receiver<bool>,
) -> std::io::Result<()> {
    let capacity = Arc::new(Semaphore::new(limits.max_connections));
    let mut tasks = JoinSet::new();
    loop {
        tokio::select! {
            biased;
            _ = shutdown.changed() => break,
            _ = tasks.join_next(), if !tasks.is_empty() => {},
            accepted = listener.accept() => {
                let (stream, _) = accepted?;
                let Ok(permit) = capacity.clone().try_acquire_owned() else { drop(stream); continue; };
                let service = TowerToHyperService::new(router.clone());
                let mut signal = shutdown.clone();
                let limits = limits.clone();
                tasks.spawn(async move {
                    let _permit = permit;
                    let mut builder = hyper::server::conn::http1::Builder::new();
                    builder.keep_alive(false).max_headers(64).max_buf_size(limits.max_header_bytes.max(8192))
                        .timer(TokioTimer::new()).header_read_timeout(Duration::from_millis(limits.request_timeout_ms));
                    let connection = builder.serve_connection(TokioIo::new(stream), service);
                    tokio::pin!(connection);
                    tokio::select! {
                        _ = timeout(Duration::from_millis(limits.connection_timeout_ms), &mut connection) => {},
                        _ = signal.changed() => {
                            connection.as_mut().graceful_shutdown();
                            let _ = timeout(Duration::from_millis(limits.shutdown_timeout_ms), connection).await;
                        }
                    }
                });
            }
        }
    }
    if timeout(Duration::from_millis(limits.shutdown_timeout_ms), async {
        while tasks.join_next().await.is_some() {}
    })
    .await
    .is_err()
    {
        tasks.abort_all();
    }
    Ok(())
}
