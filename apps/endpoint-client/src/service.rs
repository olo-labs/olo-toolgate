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
/// One instance per machine, protected by OS lease and serialized command handling.
pub struct ClientService {
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
            Some(crate::builtins::Executor::new(
                settings,
                Arc::new(crate::tool_gateway::HttpsGateway::new(settings, contracts)?),
            )?)
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
    pub fn tool_catalog(&self) -> Vec<BuiltinToolInfo> {
        self.tools.as_ref().map(|e| e.catalog()).unwrap_or_default()
    }
    pub async fn execute_tool(
        &mut self,
        invocation: BuiltinInvocation,
    ) -> Result<std::collections::BTreeMap<String, serde_json::Value>> {
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
        self.execution
            .as_mut()
            .ok_or(Failure::Unsupported)?
            .prepare()
            .await
    }
    pub async fn shutdown_runtimes(&self) -> Result<()> {
        if let Some(manager) = &self.execution {
            manager.shutdown().await?;
        }
        Ok(())
    }
    pub async fn invoke_runtime(&mut self, input: LocalToolInput) -> Result<LocalToolOutput> {
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
    async fn builder_test(&mut self, identity: &DeviceIdentity) -> Result<()> {
        let Some(settings) = self.config.deployment.clone() else {
            return Ok(());
        };
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
        let outcome = crate::builder::run(&self.config, &task).await;
        let error = outcome.as_ref().err().map(|failure| match failure {
            Failure::Unsupported => ErrorCode::Unsupported,
            Failure::Expired => ErrorCode::Timeout,
            Failure::Unauthorized | Failure::Revoked => ErrorCode::Forbidden,
            _ => ErrorCode::Validation,
        });
        let response = self
            .control
            .builder_result(
                identity.clone(),
                BuilderTestResult {
                    job_id: task.job.id.clone(),
                    lease_id: task.job.lease_id.clone(),
                    definition_digest: task.job.definition_digest.clone(),
                    success: outcome.is_ok(),
                    error,
                },
            )
            .await?;
        if response.id != task.job.id || response.definition_digest != task.job.definition_digest {
            return Err(Failure::Unauthorized);
        }
        tracing::info!(event="builder_test",job_id=%task.job.id,result=if outcome.is_ok(){"passed"}else{"failed"});
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
            if let Err(failure) = self.builder_test(&identity).await {
                tracing::warn!(event="builder_test",result="rejected",error=?failure);
                if failure == Failure::Revoked {
                    return Err(failure);
                }
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
