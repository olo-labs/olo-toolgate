// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
//! Organization-signed designated-client tests. This path cannot register tools or authorize protected effects.
use crate::{config::Config, deployment::Settings, execution, Failure, Result};
use olo_toolgate_contracts::*;
use std::sync::Arc;

/// Verify the complete lease before any sandbox preparation; even a valid signature cannot select another device.
pub fn verify(
    settings: &Settings,
    signed: &FleetSignedDocument,
    identity: &DeviceIdentity,
    server: &str,
    now: u64,
) -> Result<BuilderTestTask> {
    settings.separate_device_ca(&identity.issuer_certificate_pem)?;
    let (task, _): (BuilderTestTask, _) =
        settings.verify(signed, "toolgate-builder-test+jws", "BuilderTestTask")?;
    if task.device_id != identity.device_id
        || task.tenant_id != identity.tenant_id
        || task.server_id != server
        || task.job.device_id != identity.device_id
        || task.job.state != BuilderTestState::Running
        || task.job.attempt != 1
        || task.job.created_at_unix_ms > now
        || task.expires_at_unix_ms <= now
        || task.expires_at_unix_ms != task.job.expires_at_unix_ms
        || task.expires_at_unix_ms > now.saturating_add(120000)
    {
        return Err(Failure::Unauthorized);
    }
    let definition = &task.definition;
    if definition.permissions != vec![BuilderPermission::Compute]
        || !definition.credential_requirements.is_empty()
        || definition.resource.kind != ResourceKind::Custom
        || definition.resource.locator != format!("runtime/{}", definition.tool.tool_id)
        || definition.tool.runtime_id != definition.runtime.id
        || !definition.platforms.contains(&crate::platform::current())
        || task.job.example_index as usize >= definition.examples.len()
    {
        return Err(Failure::Unsupported);
    }
    let arch = match std::env::consts::ARCH {
        "x86_64" => FleetArchitecture::X8664,
        "aarch64" => FleetArchitecture::Aarch64,
        _ => return Err(Failure::Unsupported),
    };
    if !definition.architectures.contains(&arch)
        || definition
            .examples
            .iter()
            .any(|e| e.tool_id != definition.tool.tool_id)
    {
        return Err(Failure::Unsupported);
    }
    execution::source::validate(&definition.runtime.kind, &definition.tool)?;
    Ok(task)
}
/// Run a fixed invocation in the existing confined engine. Test success grants no ordinary invocation capability.
pub async fn run(config: &Config, task: &BuilderTestTask) -> Result<()> {
    let (_cancel, receiver) = tokio::sync::watch::channel(false);
    run_cancellable(config, task, receiver).await
}

async fn run_cancellable(
    config: &Config,
    task: &BuilderTestTask,
    mut cancelled: tokio::sync::watch::Receiver<bool>,
) -> Result<()> {
    if *cancelled.borrow() {
        return Err(Failure::Revoked);
    }
    let mut settings = config.execution.clone().ok_or(Failure::Unsupported)?;
    // Separate ownership prevents deployment recovery from reaping a live test sandbox.
    settings.state_directory = settings.state_directory.join("builder");
    settings.runtimes = vec![task.definition.runtime.clone()];
    settings.tools = vec![task.definition.tool.clone()];
    settings.validate()?;
    let gateway = config.tools.as_ref().ok_or(Failure::Unsupported)?;
    let auth = Arc::new(crate::tool_gateway::HttpsGateway::new(
        gateway,
        config,
        Arc::new(crate::contracts::Contracts::new()?),
    )?);
    let mut sandbox = execution::Manager::new(settings, auth)?;
    let execution = tokio::time::timeout(
        std::time::Duration::from_millis(task.expires_at_unix_ms.saturating_sub(crate::now())),
        sandbox.probe(&task.definition.examples[task.job.example_index as usize]),
    );
    let result = tokio::select! {
        _ = cancelled.changed() => Err(Failure::Revoked),
        result = execution => result.unwrap_or(Err(Failure::Expired)),
    };
    sandbox.shutdown().await?;
    result
}

/// One leased job runs independently of check-ins and is cancelled on loss of authority.
pub(crate) struct Job {
    task: BuilderTestTask,
    handle: Option<tokio::task::JoinHandle<Result<()>>>,
    cancel: tokio::sync::watch::Sender<bool>,
}
impl Job {
    #[cfg(test)]
    pub(crate) fn waiting(task: BuilderTestTask) -> Self {
        let (cancel, mut receiver) = tokio::sync::watch::channel(false);
        let handle = tokio::spawn(async move {
            let _ = receiver.changed().await;
            Err(Failure::Revoked)
        });
        Self {
            task,
            handle: Some(handle),
            cancel,
        }
    }
    pub fn start(config: Config, task: BuilderTestTask) -> Self {
        let (cancel, receiver) = tokio::sync::watch::channel(false);
        let running = task.clone();
        let handle =
            tokio::spawn(async move { run_cancellable(&config, &running, receiver).await });
        Self {
            task,
            handle: Some(handle),
            cancel,
        }
    }
    pub fn cancel(&self) {
        let _ = self.cancel.send(true);
    }
    pub async fn completed(&mut self) -> Option<BuilderTestResult> {
        if !self.handle.as_ref()?.is_finished() {
            return None;
        }
        let outcome = self
            .handle
            .take()?
            .await
            .unwrap_or(Err(Failure::Unavailable));
        Some(BuilderTestResult {
            job_id: self.task.job.id.clone(),
            lease_id: self.task.job.lease_id.clone(),
            definition_digest: self.task.job.definition_digest.clone(),
            success: outcome.is_ok(),
            error: outcome.err().map(|failure| match failure {
                Failure::Unsupported => ErrorCode::Unsupported,
                Failure::Expired => ErrorCode::Timeout,
                Failure::Unauthorized | Failure::Revoked => ErrorCode::Forbidden,
                _ => ErrorCode::Validation,
            }),
        })
    }
    pub async fn shutdown(&mut self) -> Result<()> {
        self.cancel();
        if let Some(mut handle) = self.handle.take() {
            if tokio::time::timeout(std::time::Duration::from_secs(10), &mut handle)
                .await
                .is_err()
            {
                handle.abort();
                let _ = handle.await;
                return Err(Failure::Unavailable);
            }
        }
        Ok(())
    }
}
impl Drop for Job {
    fn drop(&mut self) {
        self.cancel();
    }
}
