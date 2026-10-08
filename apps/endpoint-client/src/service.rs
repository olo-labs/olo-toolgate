// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
//! Durable identity/check-in state machine. Only this protected process owns network credentials.
use crate::{
    config::Config,
    identity::{verify_discovery, verify_identity, DeviceKey},
    storage::{ProtectedStore, ServiceLease},
    transport::ControlPort,
    Failure, Result,
};
use olo_toolgate_contracts::*;
use serde::{Deserialize, Serialize};
use std::{sync::Arc, time::Instant};

#[derive(Default, Deserialize, Serialize)]
#[serde(rename_all = "camelCase", deny_unknown_fields)]
struct Journal {
    manifest: Option<ClientDiscovery>,
    challenge: Option<EndpointEnrollmentChallenge>,
    identity: Option<DeviceIdentity>,
    sequence: u64,
    last_success: Option<u64>,
    pending_report: Option<EndpointCheckIn>,
    revoked: bool,
    #[serde(default)]
    observed_time: u64,
}
#[derive(Deserialize, Serialize)]
#[serde(rename_all = "camelCase", deny_unknown_fields)]
struct RemoteJournal {
    server_url: String,
    request_id: String,
    lease_id: String,
    result: Option<RemoteToolResult>,
}

/// One instance per machine, protected by OS lease and serialized command handling.
pub struct ClientService {
    remote_job: Option<crate::remote::Job>,
    remote_result: Option<RemoteToolResult>,
    permissions: Option<EndpointPermissionConfiguration>,
    builder_job: Option<crate::builder::Job>,
    builder_result: Option<BuilderTestResult>,
    deployment: crate::deployment::State,
    execution: Option<crate::execution::Manager>,
    tools: Option<crate::builtins::Executor>,
    store: ProtectedStore,
    _lease: ServiceLease,
    key: Arc<DeviceKey>,
    control: Arc<dyn ControlPort>,
    config: Config,
    journal: Journal,
    state: EndpointState,
    started: Instant,
    successes: u64,
    failures: u64,
    observed: u64,
    boot_time: u64,
}
impl ClientService {
    pub fn open(
        config: Config,
        store: ProtectedStore,
        key: Arc<DeviceKey>,
        control: Arc<dyn ControlPort>,
    ) -> Result<Self> {
        config.validate()?;
        let lease = store.lease()?;
        let journal: Journal = match store.read("journal.json")? {
            Some(bytes) => serde_json::from_slice(&bytes).map_err(|_| Failure::Validation)?,
            None => Journal::default(),
        };
        let observed = crate::now().max(journal.observed_time);
        if let Some(manifest) = &journal.manifest {
            if crate::config::origin(&manifest.control_url)?
                != crate::config::origin(&config.server_url)?
            {
                return Err(Failure::Unauthorized);
            }
        }
        if let (Some(identity), Some(manifest)) = (&journal.identity, &journal.manifest) {
            verify_identity(
                identity,
                manifest,
                &key,
                &format!("device-{}", &key.fingerprint()[..32]),
                observed.min(identity.expires_at_unix_ms.saturating_sub(1000)),
            )?;
        }
        if (journal.identity.is_some() || journal.challenge.is_some()) && journal.manifest.is_none()
        {
            return Err(Failure::Validation);
        }
        let state = if journal.revoked {
            EndpointState::Revoked
        } else if journal.identity.is_some() {
            EndpointState::Offline
        } else if journal.challenge.is_some() {
            EndpointState::Pending
        } else {
            EndpointState::Unenrolled
        };
        let tools = if let Some(settings) = &config.tools {
            let contracts = Arc::new(crate::contracts::Contracts::new()?);
            Some(
                crate::builtins::Executor::new(
                    settings,
                    Arc::new(crate::tool_gateway::HttpsGateway::new(settings, contracts)?),
                )?
                .with_packet_log(crate::diagnostics::PacketLog::open(
                    config.state_directory.clone(),
                )?),
            )
        } else {
            None
        };
        let execution = if let (Some(settings), Some(tools)) = (&config.execution, &config.tools) {
            Some(crate::execution::Manager::new(
                settings.clone(),
                Arc::new(crate::tool_gateway::HttpsGateway::new(
                    tools,
                    Arc::new(crate::contracts::Contracts::new()?),
                )?),
            )?)
        } else {
            None
        };
        let deployment = if let Some(settings) = &config.deployment {
            crate::deployment::State::open(&store, settings)?
        } else {
            crate::deployment::State::default()
        };
        Ok(Self {
            remote_job: None,
            remote_result: None,
            permissions: match store.read("permissions.json")? {
                Some(bytes) => Some(
                    crate::contracts::Contracts::new()?
                        .decode("EndpointPermissionConfiguration", &bytes)?,
                ),
                None => None,
            },
            builder_job: None,
            builder_result: None,
            deployment,
            execution,
            tools,
            store,
            _lease: lease,
            key,
            control,
            config,
            journal,
            state,
            started: Instant::now(),
            successes: 0,
            failures: 0,
            observed,
            boot_time: observed,
        })
    }
    fn clock(&mut self) -> Result<u64> {
        let wall = crate::now();
        if wall.saturating_add(1000) < self.observed {
            return Err(Failure::Expired);
        }
        let now = wall.max(
            self.boot_time.saturating_add(
                self.started
                    .elapsed()
                    .as_millis()
                    .try_into()
                    .unwrap_or(u64::MAX),
            ),
        );
        self.observed = now;
        self.journal.observed_time = now;
        self.save()?;
        Ok(now)
    }
    fn local_catalog(&self) -> Vec<BuiltinToolInfo> {
        let mut tools = self
            .tools
            .as_ref()
            .map(|e| e.catalog())
            .unwrap_or_else(crate::remote::builtin_catalog);
        if let Some(manager) = &self.execution {
            tools.extend(manager.catalog());
        }
        tools.retain(|tool| tool.enabled);
        tools.sort_by(|a, b| a.tool_id.cmp(&b.tool_id));
        tools.dedup_by(|a, b| a.tool_id == b.tool_id);
        tools
    }
    pub fn tool_catalog(&self, agent: Option<&str>) -> Vec<BuiltinToolInfo> {
        if !self.health().ready {
            return vec![];
        }
        let Some(permissions) = &self.permissions else {
            return vec![];
        };
        let Some(identity) = &self.journal.identity else {
            return vec![];
        };
        let Some(manifest) = &self.journal.manifest else {
            return vec![];
        };
        if permissions.server_id != manifest.server_id
            || permissions.device_id != identity.device_id
            || permissions.user_id != identity.user_id
        {
            return vec![];
        }
        let mut tools = self.local_catalog();
        tools.retain(|tool| {
            tool.enabled
                && crate::permissions::visible(permissions, agent, &tool.tool_id, &tool.action)
        });
        tools.sort_by(|a, b| a.tool_id.cmp(&b.tool_id));
        tools.dedup_by(|a, b| a.tool_id == b.tool_id);
        tools
    }
    pub async fn execute_tool(
        &mut self,
        invocation: BuiltinInvocation,
    ) -> Result<std::collections::BTreeMap<String, serde_json::Value>> {
        if self.remote_job.is_some() {
            return Err(Failure::Conflict);
        }
        let now = self.clock()?;
        if !self.health().ready {
            return Err(Failure::Unavailable);
        }
        let identity = self
            .journal
            .identity
            .as_ref()
            .ok_or(Failure::Unauthorized)?;
        if self
            .config
            .tools
            .as_ref()
            .is_none_or(|s| s.device_id != identity.device_id)
        {
            return Err(Failure::Unauthorized);
        }
        let valid_until = identity.expires_at_unix_ms.min(
            self.journal
                .last_success
                .unwrap_or(0)
                .saturating_add(120000),
        );
        if valid_until < now.saturating_add(20000) {
            return Err(Failure::Expired);
        }
        self.tools
            .as_mut()
            .ok_or(Failure::Unsupported)?
            .execute(invocation, valid_until)
            .await
    }
    fn save(&self) -> Result<()> {
        self.store.write(
            "journal.json",
            &serde_json::to_vec(&self.journal).map_err(|_| Failure::Validation)?,
        )
    }
    /// Runtime status is distinct from enrolled-device readiness.
    pub fn runtime_health(&self) -> Result<LocalRuntimeHealth> {
        self.execution
            .as_ref()
            .map(|m| m.health())
            .ok_or(Failure::Unsupported)
    }
    pub async fn prepare_runtimes(&mut self) -> Result<LocalRuntimeHealth> {
        if self.remote_job.is_some() {
            return Err(Failure::Conflict);
        }
        self.execution
            .as_mut()
            .ok_or(Failure::Unsupported)?
            .prepare()
            .await
    }
    pub async fn shutdown_runtimes(&mut self) -> Result<()> {
        let remote_cleanup = if let Some(mut job) = self.remote_job.take() {
            job.shutdown().await
        } else {
            Ok(())
        };
        let job_cleanup = if let Some(mut job) = self.builder_job.take() {
            job.shutdown().await
        } else {
            Ok(())
        };
        if let Some(manager) = &self.execution {
            manager.shutdown().await?;
        }
        job_cleanup.and(remote_cleanup)
    }
    pub async fn invoke_runtime(&mut self, input: LocalToolInput) -> Result<LocalToolOutput> {
        if self.remote_job.is_some() {
            return Err(Failure::Conflict);
        }
        self.clock()?;
        if !self.health().ready {
            return Err(Failure::Unavailable);
        }
        let identity = self
            .journal
            .identity
            .as_ref()
            .ok_or(Failure::Unauthorized)?;
        if self
            .config
            .tools
            .as_ref()
            .is_none_or(|s| s.device_id != identity.device_id)
        {
            return Err(Failure::Unauthorized);
        }
        let valid_until = identity.expires_at_unix_ms.min(
            self.journal
                .last_success
                .unwrap_or(0)
                .saturating_add(120000),
        );
        let valid_until = if self.config.deployment.is_some()
            && self
                .config
                .execution
                .as_ref()
                .is_none_or(|s| !s.tools.iter().any(|t| t.tool_id == input.tool_id))
        {
            valid_until.min(self.deployment.deadline()?)
        } else {
            valid_until
        };
        self.execution
            .as_mut()
            .ok_or(Failure::Unsupported)?
            .invoke(input, valid_until)
            .await
    }
    pub fn fleet_status(&self) -> FleetClientStatus {
        self.deployment.status.clone()
    }
    async fn reconcile(&mut self, identity: &DeviceIdentity) -> Result<()> {
        let Some(settings) = self.config.deployment.clone() else {
            return Ok(());
        };
        let signed = self.control.desired(identity.clone()).await?;
        let now = self.clock()?;
        settings.separate_device_ca(&identity.issuer_certificate_pem)?;
        let server = self
            .journal
            .manifest
            .as_ref()
            .ok_or(Failure::Unauthorized)?
            .server_id
            .clone();
        let changed =
            self.deployment
                .accept(&self.store, &settings, &signed, identity, &server, now)?;
        if !changed {
            return Ok(());
        }
        let base = self.config.execution.clone().ok_or(Failure::Unsupported)?;
        let gateway_settings = self.config.tools.as_ref().ok_or(Failure::Unsupported)?;
        let authorization = Arc::new(crate::tool_gateway::HttpsGateway::new(
            gateway_settings,
            Arc::new(crate::contracts::Contracts::new()?),
        )?);
        // Fence old assigned tools immediately, preserving only administrator-local registrations.
        if let Some(old) = self.execution.take() {
            old.shutdown().await?;
        }
        self.execution = Some(crate::execution::Manager::new(
            base.clone(),
            authorization.clone(),
        )?);
        let result: Result<crate::execution::Manager> = async {
            let (candidate, documents) = self.deployment.candidate(&settings, &base)?;
            let desired = self
                .deployment
                .desired
                .as_ref()
                .ok_or(Failure::Unavailable)?
                .clone();
            for assignment in &desired.assignments {
                if assignment.desired_presence {
                    let grant = self
                        .control
                        .artifact_grant(
                            identity.clone(),
                            FleetArtifactGrantRequest {
                                generation: desired.generation,
                                manifest_digest: assignment.release.manifest_digest.clone(),
                            },
                        )
                        .await?;
                    let (claims, _): (FleetArtifactGrantClaims, _) = settings.verify(
                        &grant,
                        "toolgate-fleet-artifact+jws",
                        "FleetArtifactGrantClaims",
                    )?;
                    let now = self.clock()?;
                    if claims.device_id != identity.device_id
                        || claims.tenant_id != identity.tenant_id
                        || claims.server_id != server
                        || claims.generation != desired.generation
                        || claims.manifest_digest != assignment.release.manifest_digest
                        || claims.size_bytes != assignment.release.size_bytes
                        || claims.issued_at_unix_ms > now
                        || claims.expires_at_unix_ms <= now
                        || claims
                            .expires_at_unix_ms
                            .saturating_sub(claims.issued_at_unix_ms)
                            > 60000
                    {
                        return Err(Failure::Unauthorized);
                    }
                    let bytes = self.control.artifact(identity.clone(), grant).await?;
                    if bytes.len() as u64 != assignment.release.size_bytes
                        || crate::digest(&bytes) != assignment.release.manifest_digest
                    {
                        return Err(Failure::Unauthorized);
                    }
                    let (_, signed_bytes): (FleetPackageDocument, _) = settings.verify(
                        &assignment.release.release,
                        "toolgate-package-release+jws",
                        "FleetPackageDocument",
                    )?;
                    if bytes != signed_bytes {
                        return Err(Failure::Unauthorized);
                    }
                }
            }
            let mut candidate = crate::execution::Manager::new(candidate, authorization)?;
            if !documents.is_empty() && !candidate.prepare().await?.ready {
                return Err(Failure::Unavailable);
            }
            for document in documents {
                for test in document.self_tests {
                    candidate.probe(&test).await?;
                }
            }
            self.clock()?;
            self.deployment.activate(&self.store)?;
            Ok(candidate)
        }
        .await;
        match result {
            Ok(candidate) => {
                self.execution = Some(candidate);
                tracing::info!(
                    event = "fleet_reconcile",
                    generation = self.deployment.status.generation,
                    result = "ready"
                );
                Ok(())
            }
            Err(failure) => {
                self.deployment.failed(failure);
                tracing::warn!(event="fleet_reconcile",generation=self.deployment.status.generation,result="failed",error=?failure);
                Err(failure)
            }
        }
    }
    fn device(&self) -> String {
        format!("device-{}", &self.key.fingerprint()[..32])
    }
    async fn collect_remote(&mut self, identity: &DeviceIdentity) -> Result<()> {
        if let Some(job) = &mut self.remote_job {
            if let Some(result) = job.completed().await {
                let journal = RemoteJournal {
                    server_url: self.config.server_url.clone(),
                    request_id: result.request_id.clone(),
                    lease_id: result.lease_id.clone(),
                    result: Some(result.clone()),
                };
                self.store.write(
                    "remote-journal.json",
                    &serde_json::to_vec(&journal).map_err(|_| Failure::Validation)?,
                )?;
                self.remote_job = None;
                self.remote_result = Some(result);
            }
        }
        if let Some(result) = self.remote_result.clone() {
            let response = self
                .control
                .remote_result(identity.clone(), result.clone())
                .await;
            if matches!(response, Err(Failure::Conflict | Failure::Expired)) {
                self.remote_result = None;
            }
            let response = response?;
            if response.request_id != result.request_id || response.device_id != identity.device_id
            {
                return Err(Failure::Unauthorized);
            }
            self.remote_result = None;
        }
        Ok(())
    }
    fn receive_remote(&mut self, identity: &DeviceIdentity, task: RemoteToolTask) -> Result<()> {
        let context = &task.input.context;
        if context.tenant_id != identity.tenant_id
            || context.user_id != identity.user_id
            || context.device_id.as_ref() != Some(&identity.device_id)
            || task.request_id != context.request_id
            || task.request.tool_id != task.input.tool_id
            || task.request.action != task.input.action
            || task.expires_at_unix_ms <= crate::now()
            || task.expires_at_unix_ms > crate::now().saturating_add(30000)
        {
            return Err(Failure::Unauthorized);
        }
        if let Some(job) = &self.remote_job {
            if job.task != task {
                return Err(Failure::Conflict);
            }
            return Ok(());
        }
        if let Some(result) = &self.remote_result {
            if result.request_id != task.request_id || result.lease_id != task.lease_id {
                return Err(Failure::Conflict);
            }
            return Ok(());
        }
        if let Some(bytes) = self.store.read("remote-journal.json")? {
            let journal: RemoteJournal =
                serde_json::from_slice(&bytes).map_err(|_| Failure::Validation)?;
            if journal.server_url == self.config.server_url
                && journal.request_id == task.request_id
                && journal.lease_id == task.lease_id
            {
                // An interrupted effect is never replayed. Return the durable result or a bounded failure.
                self.remote_result = Some(journal.result.unwrap_or(RemoteToolResult {
                    request_id: task.request_id,
                    lease_id: task.lease_id,
                    output: None,
                    error: Some(ErrorCode::Conflict),
                }));
                return Ok(());
            }
        }
        let configuration = self.permissions.as_ref().ok_or(Failure::Unauthorized)?;
        if !crate::permissions::visible(
            configuration,
            Some(&context.agent_id),
            &task.request.tool_id,
            &task.request.action,
        ) {
            return Err(Failure::Unauthorized);
        }
        let journal = RemoteJournal {
            server_url: self.config.server_url.clone(),
            request_id: task.request_id.clone(),
            lease_id: task.lease_id.clone(),
            result: None,
        };
        self.store.write(
            "remote-journal.json",
            &serde_json::to_vec(&journal).map_err(|_| Failure::Validation)?,
        )?;
        self.remote_job = Some(crate::remote::Job::start(
            self.config.clone(),
            self.execution.as_ref().map(|m| m.settings()),
            self.control.clone(),
            identity.clone(),
            task,
        ));
        Ok(())
    }
    async fn builder_test(&mut self, identity: &DeviceIdentity) -> Result<()> {
        let Some(settings) = self.config.deployment.clone() else {
            return Ok(());
        };
        if let Some(job) = &mut self.builder_job {
            let Some(result) = job.completed().await else {
                return Ok(());
            };
            self.builder_job = None;
            self.builder_result = Some(result);
        }
        if let Some(result) = self.builder_result.clone() {
            let response = self
                .control
                .builder_result(identity.clone(), result.clone())
                .await;
            if matches!(response, Err(Failure::Conflict | Failure::Expired)) {
                // An expired/replaced lease must not fence the next queued job forever.
                self.builder_result = None;
            }
            let response = response?;
            if response.id != result.job_id
                || response.definition_digest != result.definition_digest
            {
                return Err(Failure::Unauthorized);
            }
            tracing::info!(event="builder_test",job_id=%result.job_id,result=if result.success {"passed"} else {"failed"});
            self.builder_result = None;
        }
        if self.config.execution.is_none() || self.config.tools.is_none() {
            return Ok(());
        }
        let poll = self.control.builder_poll(identity.clone()).await?;
        let Some(signed) = poll.task else {
            return Ok(());
        };
        let server = self
            .journal
            .manifest
            .as_ref()
            .ok_or(Failure::Unauthorized)?
            .server_id
            .clone();
        let task = crate::builder::verify(&settings, &signed, identity, &server, self.clock()?)?;
        self.builder_job = Some(crate::builder::Job::start(self.config.clone(), task));
        Ok(())
    }
    pub fn health(&self) -> ClientHealth {
        let now = crate::now().max(
            self.boot_time.saturating_add(
                self.started
                    .elapsed()
                    .as_millis()
                    .try_into()
                    .unwrap_or(u64::MAX),
            ),
        );
        let fresh = now >= self.observed
            && self
                .journal
                .identity
                .as_ref()
                .is_some_and(|i| i.expires_at_unix_ms > now)
            && self
                .journal
                .last_success
                .is_some_and(|t| now.saturating_sub(t) <= 120000);
        ClientHealth {
            state: if self.state == EndpointState::Active && !fresh {
                EndpointState::Offline
            } else {
                self.state.clone()
            },
            ready: self.state == EndpointState::Active && fresh,
            uptime_seconds: self.started.elapsed().as_secs(),
            successful_check_ins: self.successes,
            failed_check_ins: self.failures,
            report_sequence: self.journal.sequence,
            last_success_unix_ms: self.journal.last_success,
        }
    }
    fn prompt(&self, challenge: &EndpointEnrollmentChallenge) -> EndpointEnrollmentPrompt {
        EndpointEnrollmentPrompt {
            enrollment_id: challenge.enrollment_id.clone(),
            user_code: challenge.user_code.clone(),
            verification_uri: challenge.verification_uri.clone(),
            expires_at_unix_ms: challenge.expires_at_unix_ms,
            poll_interval_seconds: challenge.poll_interval_seconds,
            key_fingerprint: self.key.fingerprint(),
        }
    }
    pub async fn enroll(&mut self) -> Result<EndpointEnrollmentPrompt> {
        if self.journal.revoked || self.journal.identity.is_some() {
            return Err(Failure::Conflict);
        }
        let now = self.clock()?;
        if let Some(challenge) = &self.journal.challenge {
            if challenge.expires_at_unix_ms > now {
                return Ok(self.prompt(challenge));
            }
        }
        let envelope = self.control.discovery().await?;
        // Signed issuance occurs after the outgoing request. Validate against receipt time.
        let now = self.clock()?;
        let pin = self
            .journal
            .manifest
            .as_ref()
            .map(|m| {
                crate::identity::certificate_der(&m.issuer_certificate_pem)
                    .map(|b| crate::digest(&b))
            })
            .transpose()?;
        let manifest = verify_discovery(
            &envelope,
            &crate::config::origin(&self.config.server_url)?,
            now,
            pin.as_deref(),
        )?;
        let request = EndpointEnrollmentStart {
            device_id: self.device(),
            client_version: env!("CARGO_PKG_VERSION").into(),
            platform: crate::platform::current(),
            csr_pem: self.key.csr(&self.device())?,
            capabilities: vec!["endpoint.identity.v1".into(), "endpoint.check-in.v1".into()],
        };
        let challenge = self.control.start(request).await?;
        let now = self.clock()?;
        if challenge.verification_uri != manifest.verification_uri
            || challenge.expires_at_unix_ms <= now
            || challenge.expires_at_unix_ms - now > 600000
        {
            return Err(Failure::Unauthorized);
        }
        self.journal.manifest = Some(manifest);
        self.journal.challenge = Some(challenge.clone());
        self.state = EndpointState::Pending;
        self.save()?;
        Ok(self.prompt(&challenge))
    }
    pub async fn tick(&mut self) -> Result<()> {
        let result = self.tick_inner().await;
        if let Err(failure) = result {
            if let Some(job) = &self.remote_job {
                job.cancel();
            }
            if let Some(job) = &self.builder_job {
                job.cancel();
            }
            self.failures = self.failures.saturating_add(1);
            if failure == Failure::Revoked {
                self.state = EndpointState::Revoked;
                self.journal.revoked = true;
                self.save()?;
            } else if self.journal.identity.is_some() {
                self.state = EndpointState::Offline;
            }
            tracing::warn!(event="client_check_in",result="rejected",error=?failure);
        }
        result
    }
    async fn tick_inner(&mut self) -> Result<()> {
        let now = self.clock()?;
        if self.journal.revoked {
            return Err(Failure::Revoked);
        }
        if self.journal.identity.is_none() {
            let Some(challenge) = &self.journal.challenge else {
                return Ok(());
            };
            if challenge.expires_at_unix_ms <= now {
                self.journal.challenge = None;
                self.state = EndpointState::Unenrolled;
                self.save()?;
                return Err(Failure::Expired);
            }
            let result = self
                .control
                .poll(EndpointEnrollmentPoll {
                    enrollment_id: challenge.enrollment_id.clone(),
                    device_code: challenge.device_code.clone(),
                })
                .await?;
            match result.state {
                EnrollmentState::Consumed => {
                    let identity = result.identity.ok_or(Failure::Unauthorized)?;
                    let manifest = self
                        .journal
                        .manifest
                        .as_ref()
                        .ok_or(Failure::Unauthorized)?;
                    verify_identity(&identity, manifest, &self.key, &self.device(), now)?;
                    self.journal.identity = Some(identity);
                    self.journal.challenge = None;
                    self.save()?;
                }
                EnrollmentState::Pending => return Ok(()),
                EnrollmentState::Denied | EnrollmentState::Expired => {
                    self.journal.challenge = None;
                    self.state = EndpointState::Unenrolled;
                    self.save()?;
                    return Err(Failure::Unauthorized);
                }
                _ => return Err(Failure::Unauthorized),
            }
        }
        let identity = self.journal.identity.clone().ok_or(Failure::Unauthorized)?;
        if identity.expires_at_unix_ms <= now {
            return Err(Failure::Expired);
        }
        if self.journal.pending_report.is_none()
            && self
                .journal
                .last_success
                .is_some_and(|last| now.saturating_sub(last) < 2000)
        {
            return Ok(());
        }
        if self.journal.pending_report.is_none() {
            if let Err(failure) = self.collect_remote(&identity).await {
                tracing::warn!(event="remote_result",error=?failure);
                if failure == Failure::Revoked {
                    return Err(failure);
                }
            }
            if serde_json::to_vec(&self.local_catalog())
                .map_err(|_| Failure::Validation)?
                .len()
                > 49152
            {
                return Err(Failure::Validation);
            }
            let request = EndpointCheckIn {
                sequence: self
                    .journal
                    .sequence
                    .checked_add(1)
                    .ok_or(Failure::Conflict)?,
                report: ClientReport {
                    device_id: self.device(),
                    client_version: env!("CARGO_PKG_VERSION").into(),
                    applied_revision: self.deployment.status.generation,
                    packages: self.deployment.status.packages.clone(),
                },
                configuration_digest: self
                    .permissions
                    .as_ref()
                    .filter(|p| {
                        p.device_id == identity.device_id
                            && p.user_id == identity.user_id
                            && self
                                .journal
                                .manifest
                                .as_ref()
                                .is_some_and(|m| m.server_id == p.server_id)
                    })
                    .map(|p| p.digest.clone()),
                local_tools: Some(self.local_catalog()),
            };
            self.journal.pending_report = Some(request);
            self.save()?;
        }
        let request = self
            .journal
            .pending_report
            .clone()
            .ok_or(Failure::Conflict)?;
        let ack = self.control.check_in(identity, request.clone()).await?;
        if ack.device_id != self.device()
            || ack.sequence != request.sequence
            || ack.server_time_unix_ms.abs_diff(now) > 300000
        {
            return Err(Failure::Unauthorized);
        }
        if let Some(identity) = ack.identity {
            verify_identity(
                &identity,
                self.journal
                    .manifest
                    .as_ref()
                    .ok_or(Failure::Unauthorized)?,
                &self.key,
                &self.device(),
                now,
            )?;
            self.journal.identity = Some(identity);
        }
        if let Some(configuration) = ack.configuration {
            let identity = self
                .journal
                .identity
                .as_ref()
                .ok_or(Failure::Unauthorized)?;
            let server = &self
                .journal
                .manifest
                .as_ref()
                .ok_or(Failure::Unauthorized)?
                .server_id;
            if configuration.device_id != identity.device_id
                || configuration.user_id != identity.user_id
                || &configuration.server_id != server
                || self.permissions.as_ref().is_some_and(|old| {
                    old.server_id == configuration.server_id
                        && old.revision > configuration.revision
                })
            {
                return Err(Failure::Unauthorized);
            }
            let bytes = crate::contracts::Contracts::new()?
                .encode("EndpointPermissionConfiguration", &configuration)?;
            self.store.write("permissions.json", &bytes)?;
            self.permissions = Some(configuration);
            if let Some(job) = &self.remote_job {
                if !crate::permissions::visible(
                    self.permissions.as_ref().ok_or(Failure::Unauthorized)?,
                    Some(&job.task.input.context.agent_id),
                    &job.task.request.tool_id,
                    &job.task.request.action,
                ) {
                    job.cancel();
                }
            }
        }
        self.journal.sequence = request.sequence;
        self.journal.pending_report = None;
        self.journal.last_success = Some(now);
        self.save()?;
        self.successes = self.successes.saturating_add(1);
        self.state = EndpointState::Active;
        tracing::info!(
            event = "client_check_in",
            result = "accepted",
            sequence = self.journal.sequence
        );
        // Check authority first on every cycle, including while a leased job is running.
        let identity = self.journal.identity.clone().ok_or(Failure::Unauthorized)?;
        if let Some(task) = ack.task {
            self.receive_remote(&identity, task)?;
        }
        if let Err(failure) = self.reconcile(&identity).await {
            if self
                .deployment
                .desired
                .as_ref()
                .is_some_and(|d| d.expires_at_unix_ms <= crate::now())
            {
                self.deployment.failed(Failure::Expired);
            }
            if failure == Failure::Revoked {
                return Err(failure);
            }
        }
        if let Err(failure) = self.builder_test(&identity).await {
            tracing::warn!(event="builder_test",result="rejected",error=?failure);
            if failure == Failure::Revoked {
                return Err(failure);
            }
        }
        Ok(())
    }
    pub fn next_delay_seconds(&self) -> u64 {
        if self.journal.challenge.is_some() {
            return 5;
        }
        if self.state == EndpointState::Active {
            return 2;
        }
        (5_u64.saturating_mul(1_u64 << self.failures.min(6))).min(300)
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::transport::Call;
    use std::sync::atomic::{AtomicBool, AtomicU64, Ordering};

    struct Gateway {
        polls: AtomicU64,
        desired: AtomicU64,
        revoked: AtomicBool,
        authorizations: AtomicU64,
        deadline: AtomicU64,
    }
    impl ControlPort for Gateway {
        fn discovery(&self) -> Call<'_, SignedClientDiscovery> {
            Box::pin(async { Err(Failure::Unsupported) })
        }
        fn start(&self, _: EndpointEnrollmentStart) -> Call<'_, EndpointEnrollmentChallenge> {
            Box::pin(async { Err(Failure::Unsupported) })
        }
        fn poll(&self, _: EndpointEnrollmentPoll) -> Call<'_, EndpointEnrollmentResult> {
            Box::pin(async { Err(Failure::Unsupported) })
        }
        fn desired(&self, _: DeviceIdentity) -> Call<'_, FleetSignedDocument> {
            Box::pin(async {
                self.desired.fetch_add(1, Ordering::SeqCst);
                Err(Failure::Unsupported)
            })
        }
        fn check_in(
            &self,
            _: DeviceIdentity,
            report: EndpointCheckIn,
        ) -> Call<'_, EndpointCheckInAck> {
            Box::pin(async move {
                self.polls.fetch_add(1, Ordering::SeqCst);
                if self.revoked.load(Ordering::SeqCst) {
                    return Err(Failure::Revoked);
                }
                Ok(EndpointCheckInAck {
                    device_id: report.report.device_id,
                    sequence: report.sequence,
                    server_time_unix_ms: crate::now(),
                    next_interval_seconds: 2,
                    identity: None,
                    configuration: None,
                    task: None,
                })
            })
        }
        fn remote_authorize(
            &self,
            _: DeviceIdentity,
            request: RemoteToolAuthorization,
        ) -> Call<'_, RemoteToolAuthorizationAck> {
            Box::pin(async move {
                self.authorizations.fetch_add(1, Ordering::SeqCst);
                assert!(["hotfolder.write_text", "hotfolder.read_text"]
                    .contains(&request.request.tool_id.as_str()));
                assert_eq!(
                    request.request.arguments.get("path"),
                    Some(&serde_json::json!("test.txt"))
                );
                if self.revoked.load(Ordering::SeqCst) {
                    return Err(Failure::Revoked);
                }
                Ok(RemoteToolAuthorizationAck {
                    expires_at_unix_ms: self.deadline.load(Ordering::SeqCst),
                })
            })
        }
    }

    fn test_service() -> (ClientService, Arc<Gateway>, std::path::PathBuf) {
        #[cfg(windows)]
        let base = std::path::PathBuf::from(std::env::var_os("ProgramData").unwrap());
        #[cfg(target_os = "macos")]
        let base = std::path::PathBuf::from("/private/tmp");
        #[cfg(all(unix, not(target_os = "macos")))]
        let base = std::env::temp_dir();
        let directory = base.join(format!(
            "toolgate-poll-{}",
            crate::identity::nonce().unwrap()
        ));
        let store = ProtectedStore::open(directory.clone()).unwrap();
        let key = Arc::new(DeviceKey::load_or_create(&store).unwrap());
        let config = Config {
            deployment: None,
            execution: None,
            tools: None,
            server_url: "https://control.example.test".into(),
            state_directory: directory.clone(),
            ipc_endpoint: {
                #[cfg(unix)]
                {
                    directory.join("client.sock").to_string_lossy().into_owned()
                }
                #[cfg(windows)]
                {
                    r"\\.\pipe\olo-toolgate-client".into()
                }
            },
            authorized_peers: vec![{
                #[cfg(unix)]
                {
                    unsafe { libc::geteuid() }.to_string()
                }
                #[cfg(windows)]
                {
                    crate::platform::windows::current_sid().unwrap()
                }
            }],
            ca_certificate_path: None,
            request_timeout_seconds: 5,
        };
        let gateway = Arc::new(Gateway {
            polls: AtomicU64::new(0),
            desired: AtomicU64::new(0),
            revoked: AtomicBool::new(false),
            authorizations: AtomicU64::new(0),
            deadline: AtomicU64::new(crate::now() + 29000),
        });
        let service = ClientService::open(config, store, key, gateway.clone()).unwrap();
        (service, gateway, directory)
    }
    fn fixtures() -> serde_json::Value {
        serde_json::from_str(include_str!(
            "../../../tests/fixtures/contracts/v1/valid.json"
        ))
        .unwrap()
    }
    fn remote_task(identity: &DeviceIdentity, deadline: u64) -> RemoteToolTask {
        let mut task: RemoteToolTask =
            serde_json::from_value(fixtures()["RemoteToolTask"].clone()).unwrap();
        task.input.context.tenant_id = identity.tenant_id.clone();
        task.input.context.user_id = identity.user_id.clone();
        task.input.context.device_id = Some(identity.device_id.clone());
        task.input.context.request_id = task.request_id.clone();
        task.input.tool_id = task.request.tool_id.clone();
        task.input.action = task.request.action.clone();
        task.expires_at_unix_ms = deadline;
        task
    }
    #[tokio::test]
    async fn interrupted_effect_is_not_replayed_and_completed_result_is_recovered() {
        let (mut service, _, directory) = test_service();
        let identity: DeviceIdentity =
            serde_json::from_value(fixtures()["DeviceIdentity"].clone()).unwrap();
        let task = remote_task(&identity, crate::now() + 29000);
        let mut journal = RemoteJournal {
            server_url: service.config.server_url.clone(),
            request_id: task.request_id.clone(),
            lease_id: task.lease_id.clone(),
            result: None,
        };
        service
            .store
            .write(
                "remote-journal.json",
                &serde_json::to_vec(&journal).unwrap(),
            )
            .unwrap();
        service.receive_remote(&identity, task.clone()).unwrap();
        assert!(service.remote_job.is_none());
        assert_eq!(
            service.remote_result.as_ref().unwrap().error,
            Some(ErrorCode::Conflict)
        );
        let result = RemoteToolResult {
            request_id: task.request_id.clone(),
            lease_id: task.lease_id.clone(),
            output: Some(std::collections::BTreeMap::new()),
            error: None,
        };
        journal.result = Some(result.clone());
        service
            .store
            .write(
                "remote-journal.json",
                &serde_json::to_vec(&journal).unwrap(),
            )
            .unwrap();
        service.remote_result = None;
        service.receive_remote(&identity, task).unwrap();
        assert!(service.remote_job.is_none());
        assert_eq!(service.remote_result, Some(result));
        drop(service);
        std::fs::remove_dir_all(directory).unwrap();
    }
    #[tokio::test]
    async fn relayed_builtin_requires_current_authorization_before_effect() {
        let (service, gateway, directory) = test_service();
        let identity: DeviceIdentity =
            serde_json::from_value(fixtures()["DeviceIdentity"].clone()).unwrap();
        let mut task = remote_task(&identity, gateway.deadline.load(Ordering::SeqCst));
        task.request.tool_id = "hotfolder.write_text".into();
        task.request.action = "write".into();
        task.request.arguments =
            serde_json::from_value(serde_json::json!({"path":"test.txt","text":"allowed"}))
                .unwrap();
        let mut job = crate::remote::Job::start(
            service.config.clone(),
            None,
            gateway.clone(),
            identity.clone(),
            task.clone(),
        );
        let completed = async |job: &mut crate::remote::Job| {
            tokio::time::timeout(std::time::Duration::from_secs(2), async {
                loop {
                    if let Some(result) = job.completed().await {
                        break result;
                    }
                    tokio::task::yield_now().await;
                }
            })
            .await
            .unwrap()
        };
        assert!(completed(&mut job).await.output.is_some());
        assert_eq!(
            std::fs::read_to_string(directory.join("hotfolder/test.txt")).unwrap(),
            "allowed"
        );
        gateway.revoked.store(true, Ordering::SeqCst);
        task.request
            .arguments
            .insert("text".into(), serde_json::json!("revoked"));
        let mut denied = crate::remote::Job::start(
            service.config.clone(),
            None,
            gateway.clone(),
            identity,
            task.clone(),
        );
        assert_eq!(
            completed(&mut denied).await.error,
            Some(ErrorCode::Forbidden)
        );
        assert_eq!(gateway.authorizations.load(Ordering::SeqCst), 2);
        assert_eq!(
            std::fs::read_to_string(directory.join("hotfolder/test.txt")).unwrap(),
            "allowed"
        );
        gateway.revoked.store(false, Ordering::SeqCst);
        task.request
            .arguments
            .insert("text".into(), serde_json::json!("a".repeat(65536)));
        let mut large_write = crate::remote::Job::start(
            service.config.clone(),
            None,
            gateway.clone(),
            serde_json::from_value(fixtures()["DeviceIdentity"].clone()).unwrap(),
            task.clone(),
        );
        assert!(completed(&mut large_write).await.output.is_some());
        task.request.tool_id = "hotfolder.read_text".into();
        task.request.action = "read".into();
        task.request.arguments.remove("text");
        let mut large_read = crate::remote::Job::start(
            service.config.clone(),
            None,
            gateway.clone(),
            serde_json::from_value(fixtures()["DeviceIdentity"].clone()).unwrap(),
            task,
        );
        let oversized = completed(&mut large_read).await;
        assert!(oversized.output.is_none());
        assert_eq!(oversized.error, Some(ErrorCode::Validation));
        drop(large_write);
        drop(large_read);
        drop(job);
        drop(denied);
        drop(service);
        std::fs::remove_dir_all(directory).unwrap();
    }
    #[tokio::test]
    async fn polls_continue_during_a_job_and_revocation_cancels_it() {
        let (mut service, gateway, directory) = test_service();
        let fixtures: serde_json::Value = serde_json::from_str(include_str!(
            "../../../tests/fixtures/contracts/v1/valid.json"
        ))
        .unwrap();
        let mut identity: DeviceIdentity =
            serde_json::from_value(fixtures["DeviceIdentity"].clone()).unwrap();
        identity.expires_at_unix_ms = crate::now() + 60000;
        service.journal.identity = Some(identity);
        service.config.deployment = Some(crate::deployment::Settings {
            organization_keys: vec![],
            release_keys: vec![],
        });
        service.builder_job = Some(crate::builder::Job::waiting(
            serde_json::from_value(fixtures["BuilderTestTask"].clone()).unwrap(),
        ));
        tokio::time::timeout(std::time::Duration::from_secs(1), service.tick())
            .await
            .unwrap()
            .unwrap();
        assert!(service.health().ready);
        assert_eq!(service.next_delay_seconds(), 2);
        tokio::time::sleep(std::time::Duration::from_millis(2050)).await;
        tokio::time::timeout(std::time::Duration::from_secs(1), service.tick())
            .await
            .unwrap()
            .unwrap();
        assert_eq!(gateway.polls.load(Ordering::SeqCst), 2);
        assert_eq!(gateway.desired.load(Ordering::SeqCst), 2);
        assert_eq!(service.health().successful_check_ins, 2);
        gateway.revoked.store(true, Ordering::SeqCst);
        tokio::time::sleep(std::time::Duration::from_millis(2050)).await;
        assert_eq!(service.tick().await, Err(Failure::Revoked));
        assert_eq!(service.health().state, EndpointState::Revoked);
        let mut job = service.builder_job.take().unwrap();
        let result = tokio::time::timeout(std::time::Duration::from_secs(1), async {
            loop {
                if let Some(result) = job.completed().await {
                    break result;
                }
                tokio::task::yield_now().await;
            }
        })
        .await
        .unwrap();
        assert!(!result.success);
        assert_eq!(result.error, Some(ErrorCode::Forbidden));
        drop(service);
        std::fs::remove_dir_all(directory).unwrap();
    }
}
