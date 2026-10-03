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
    let mut settings = config.execution.clone().ok_or(Failure::Unsupported)?;
    settings.runtimes = vec![task.definition.runtime.clone()];
    settings.tools = vec![task.definition.tool.clone()];
    settings.validate()?;
    let gateway = config.tools.as_ref().ok_or(Failure::Unsupported)?;
    let auth = Arc::new(crate::tool_gateway::HttpsGateway::new(
        gateway,
        Arc::new(crate::contracts::Contracts::new()?),
    )?);
    let mut sandbox = execution::Manager::new(settings, auth)?;
    let result = tokio::time::timeout(
        std::time::Duration::from_millis(task.expires_at_unix_ms.saturating_sub(crate::now())),
        sandbox.probe(&task.definition.examples[task.job.example_index as usize]),
    )
    .await
    .unwrap_or(Err(Failure::Expired));
    sandbox.shutdown().await?;
    result
}
