// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
//! Bounded, acknowledged audit adapter; sink errors deny authorization.
use crate::policy::PortFuture;
use olo_toolgate_contracts::RuntimeAuditEvent;
use std::io::Write;
use tokio::sync::{mpsc, oneshot};

/// External audit boundary. Success means adapter acknowledgement, not durable
/// centralized storage unless the selected adapter explicitly guarantees it.
pub trait AuditSink: Send + Sync {
    /// No caller arguments, bearer credentials or resource locators may be logged.
    fn record(&self, event: RuntimeAuditEvent) -> PortFuture<'_, Result<(), &'static str>>;
    /// Unusable audit transport removes readiness.
    fn ready(&self) -> bool;
}

type AuditMessage = (RuntimeAuditEvent, oneshot::Sender<Result<(), &'static str>>);

/// Writes JSON lines to a collector-owned stream on a dedicated blocking worker.
pub struct JsonAudit {
    sender: mpsc::Sender<AuditMessage>,
}

impl JsonAudit {
    /// Queue is bounded; caller admission waits for a write/flush acknowledgement.
    pub fn new<W: Write + Send + 'static>(
        capacity: usize,
        mut writer: W,
    ) -> (Self, tokio::task::JoinHandle<()>) {
        let (sender, mut receiver) = mpsc::channel::<AuditMessage>(capacity);
        let worker = tokio::task::spawn_blocking(move || {
            while let Some((event, acknowledgement)) = receiver.blocking_recv() {
                let result = serde_json::to_writer(&mut writer, &event)
                    .map_err(|_| "audit serialization failed")
                    .and_then(|_| writer.write_all(b"\n").map_err(|_| "audit write failed"))
                    .and_then(|_| writer.flush().map_err(|_| "audit flush failed"));
                let failed = result.is_err();
                let _ = acknowledgement.send(result);
                if failed {
                    break;
                }
            }
        });
        (Self { sender }, worker)
    }
}

impl AuditSink for JsonAudit {
    fn record(&self, event: RuntimeAuditEvent) -> PortFuture<'_, Result<(), &'static str>> {
        Box::pin(async move {
            let (acknowledge, result) = oneshot::channel();
            self.sender
                .try_send((event, acknowledge))
                .map_err(|_| "audit queue unavailable")?;
            result.await.map_err(|_| "audit worker unavailable")?
        })
    }
    fn ready(&self) -> bool {
        !self.sender.is_closed()
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use std::{future::poll_fn, task::Poll};

    #[tokio::test]
    async fn queue_saturation_rejects_without_waiting_for_capacity() {
        let (sender, receiver) = mpsc::channel(1);
        let audit = JsonAudit { sender };
        let fixtures: serde_json::Value = serde_json::from_str(include_str!(
            "../../../tests/fixtures/contracts/v1/valid.json"
        ))
        .unwrap();
        let event: RuntimeAuditEvent =
            serde_json::from_value(fixtures["RuntimeAuditEvent"].clone()).unwrap();
        let mut first = audit.record(event.clone());
        poll_fn(|cx| {
            assert!(first.as_mut().poll(cx).is_pending());
            Poll::Ready(())
        })
        .await;
        assert!(audit.record(event).await.is_err());
        drop(receiver);
        assert!(first.await.is_err());
        assert!(!audit.ready());
    }
}
