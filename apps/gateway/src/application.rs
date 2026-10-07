// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
//! Transport-independent authorization coordination over explicit security ports.
use crate::{
    approvals::{ApprovalCoordinator, PermitSigner},
    audit::AuditSink,
    digest,
    extraction::Registry,
    policy::PolicyEvaluator,
    validation::Contracts,
};
use olo_toolgate_contracts::{
    ApprovalPermitUse, ApprovalResolution, ApprovalState, ApprovalSubmission, AuthorizationOutcome,
    AuthorizationRequest, Decision, DecisionReason, ErrorCode, ExecutionPermitUseRequest,
    PolicyDecision, PolicyInput, RequestContext, RuntimeAuditEvent,
};
use std::sync::Arc;

/// Remote work occurs only for ASK and explicit permit consumption, through a port.
pub struct Gateway {
    pub contracts: Contracts,
    pub extractors: Registry,
    pub policy: Arc<dyn PolicyEvaluator>,
    pub audit: Arc<dyn AuditSink>,
    pub approval: Option<Arc<dyn ApprovalCoordinator>>,
    pub permit_signer: Option<Arc<PermitSigner>>,
}

impl Gateway {
    pub(crate) fn normalize(
        &self,
        request: AuthorizationRequest,
        context: RequestContext,
    ) -> Result<PolicyInput, ErrorCode> {
        if !self.contracts.valid(
            "AuthorizationRequest",
            &serde_json::to_value(&request).map_err(|_| ErrorCode::Validation)?,
        ) {
            return Err(ErrorCode::Validation);
        }
        let resource = self
            .extractors
            .extract(&request)
            .map_err(|_| ErrorCode::Validation)?;
        let input = PolicyInput {
            context,
            tool_id: request.tool_id,
            action: request.action,
            resource,
            arguments_digest: digest(
                &serde_json::to_vec(&request.arguments).map_err(|_| ErrorCode::Validation)?,
            ),
        };
        if !self.contracts.valid(
            "PolicyInput",
            &serde_json::to_value(&input).map_err(|_| ErrorCode::Validation)?,
        ) {
            return Err(ErrorCode::Validation);
        }
        Ok(input)
    }
    async fn evaluate(&self, input: &PolicyInput, now: u64) -> Result<PolicyDecision, ErrorCode> {
        let decision = self
            .policy
            .evaluate(input, now)
            .await
            .map_err(|_| ErrorCode::DependencyUnavailable)?;
        if decision.request_id != input.context.request_id
            || !self.contracts.valid(
                "PolicyDecision",
                &serde_json::to_value(&decision).map_err(|_| ErrorCode::Internal)?,
            )
        {
            return Err(ErrorCode::DependencyUnavailable);
        }
        Ok(decision)
    }
    async fn audit(
        &self,
        input: &PolicyInput,
        decision: &PolicyDecision,
        trace_id: String,
        now: u64,
    ) -> Result<(), ErrorCode> {
        self.audit
            .record(RuntimeAuditEvent {
                timestamp_unix_ms: now,
                context: input.context.clone(),
                tool_id: input.tool_id.clone(),
                action: input.action.clone(),
                resource_digest: digest(
                    &serde_json::to_vec(&input.resource).map_err(|_| ErrorCode::Internal)?,
                ),
                arguments_digest: input.arguments_digest.clone(),
                decision: decision.clone(),
                trace_id,
            })
            .await
            .map_err(|_| ErrorCode::DependencyUnavailable)
    }
    fn block(decision: &mut PolicyDecision, reason: DecisionReason) {
        decision.decision = Decision::Block;
        decision.reason = reason;
    }
    fn resolution_valid(
        &self,
        resolution: &ApprovalResolution,
        input: &PolicyInput,
        version: &str,
    ) -> bool {
        resolution.input == *input
            && resolution.policy_version == version
            && self.contracts.valid(
                "ApprovalResolution",
                &serde_json::to_value(resolution).unwrap_or_default(),
            )
    }

    /// Legacy interfaces never discard a permit or turn ASK into an implicit grant.
    pub async fn authorize(
        &self,
        request: AuthorizationRequest,
        context: RequestContext,
        trace_id: String,
        now: u64,
    ) -> Result<PolicyDecision, ErrorCode> {
        let input = self.normalize(request, context)?;
        self.authorize_input(input, trace_id, now).await
    }

    /// MCP runtime inputs use a fixed CUSTOM locator after device-scoped discovery.
    /// This entry point is internal; callers cannot supply a normalized policy input.
    pub(crate) async fn authorize_input(
        &self,
        input: PolicyInput,
        trace_id: String,
        now: u64,
    ) -> Result<PolicyDecision, ErrorCode> {
        if !self.contracts.valid(
            "PolicyInput",
            &serde_json::to_value(&input).map_err(|_| ErrorCode::Validation)?,
        ) {
            return Err(ErrorCode::Validation);
        }
        let mut decision = self.evaluate(&input, now).await?;
        if decision.decision == Decision::Ask {
            Self::block(&mut decision, DecisionReason::ApprovalRequired);
        }
        self.audit(&input, &decision, trace_id, now).await?;
        if decision.decision == Decision::Allow
            && !self.policy.decision_valid(
                &decision,
                &input,
                crate::unix_ms().ok_or(ErrorCode::DependencyUnavailable)?,
            )
        {
            return Err(ErrorCode::DependencyUnavailable);
        }
        Ok(decision)
    }

    /// A human never holds this connection open. Retrying after an approval asks
    /// Control for a fresh single-use lease for this exact current policy/input.
    pub async fn authorize_v2(
        &self,
        request: AuthorizationRequest,
        context: RequestContext,
        trace_id: String,
        now: u64,
        credential_deadline: u64,
    ) -> Result<AuthorizationOutcome, ErrorCode> {
        let input = self.normalize(request, context)?;
        let initial = self.evaluate(&input, now).await?;
        let mut outcome = AuthorizationOutcome {
            decision: initial.clone(),
            approval_id: None,
            permit: None,
        };
        if initial.decision == Decision::Ask {
            let submission = ApprovalSubmission {
                input: input.clone(),
                policy_version: initial.policy_version.clone(),
            };
            let resolved = match &self.approval {
                Some(coordinator) => coordinator.resolve(&submission, &trace_id).await,
                None => Err("approval coordinator disabled"),
            };
            match resolved {
                Ok(resolution)
                    if self.resolution_valid(&resolution, &input, &initial.policy_version) =>
                {
                    outcome.approval_id = Some(resolution.approval_id.clone());
                    let current = crate::unix_ms().ok_or(ErrorCode::DependencyUnavailable)?;
                    let has_lease = resolution.permit_id.is_some()
                        && resolution.permit_expires_at_unix_ms.is_some();
                    let grant = matches!(
                        resolution.state,
                        ApprovalState::Consumed | ApprovalState::ApprovedTemporary
                    ) && has_lease
                        && self.policy.decision_valid(&initial, &input, current);
                    if grant {
                        let issued = self
                            .policy
                            .approval_deadline(&input, current)
                            .zip(self.permit_signer.as_ref())
                            .ok_or("fresh ASK permit evidence unavailable")
                            .and_then(|(deadline, signer)| {
                                signer.issue(
                                    &input,
                                    &resolution,
                                    current,
                                    deadline.min(credential_deadline),
                                )
                            });
                        match issued {
                            Ok(permit) => {
                                outcome.permit = Some(permit);
                                outcome.decision.decision = Decision::Allow;
                                outcome.decision.reason = DecisionReason::Matched;
                            }
                            Err(_) => Self::block(
                                &mut outcome.decision,
                                DecisionReason::PolicyUnavailable,
                            ),
                        }
                    } else if resolution.state == ApprovalState::Pending
                        && !has_lease
                        && resolution.permit_id.is_none()
                        && resolution.permit_expires_at_unix_ms.is_none()
                        && self.policy.decision_valid(&initial, &input, current)
                    {
                        // ASK carries only a reference; it grants no runtime permission.
                    } else {
                        Self::block(&mut outcome.decision, DecisionReason::ApprovalRequired);
                    }
                }
                _ => Self::block(&mut outcome.decision, DecisionReason::PolicyUnavailable),
            }
        }
        self.audit(
            &input,
            &outcome.decision,
            trace_id.clone(),
            crate::unix_ms().ok_or(ErrorCode::DependencyUnavailable)?,
        )
        .await?;
        let current = crate::unix_ms().ok_or(ErrorCode::DependencyUnavailable)?;
        let still_valid = if initial.decision == Decision::Ask {
            self.policy.decision_valid(&initial, &input, current)
                && outcome.permit.as_ref().is_none_or(|p| {
                    self.permit_signer
                        .as_ref()
                        .is_some_and(|s| s.verify(p, &input, current).is_ok())
                })
        } else {
            self.policy
                .decision_valid(&outcome.decision, &input, current)
        };
        if matches!(outcome.decision.decision, Decision::Allow | Decision::Ask)
            && (!still_valid || current >= credential_deadline)
        {
            outcome.permit = None;
            Self::block(&mut outcome.decision, DecisionReason::PolicyUnavailable);
            self.audit(&input, &outcome.decision, trace_id, current)
                .await?;
        }
        if !self.contracts.valid(
            "AuthorizationOutcome",
            &serde_json::to_value(&outcome).map_err(|_| ErrorCode::Internal)?,
        ) {
            return Err(ErrorCode::DependencyUnavailable);
        }
        Ok(outcome)
    }

    /// A signature alone is insufficient: Control must atomically spend the lease.
    /// The authenticated consume ingress ID stays separate from the permit's
    /// original request ID when reproducing its durable Control lease binding.
    pub async fn consume_permit(
        &self,
        request: ExecutionPermitUseRequest,
        context: RequestContext,
        trace_id: String,
        now: u64,
        credential_deadline: u64,
    ) -> Result<PolicyDecision, ErrorCode> {
        let input = self.normalize(request.request, context)?;
        let initial = self.evaluate(&input, now).await?;
        let mut decision = initial.clone();
        Self::block(&mut decision, DecisionReason::ApprovalRequired);
        if initial.decision == Decision::Ask {
            let verified = self
                .permit_signer
                .as_ref()
                .ok_or("permit signer disabled")
                .and_then(|signer| signer.verify(&request.permit, &input, now));
            if let Ok(claims) = verified {
                if claims.policy_version == initial.policy_version {
                    let mut lease_input = input.clone();
                    lease_input.context.request_id = claims.request_id.clone();
                    let use_request = ApprovalPermitUse {
                        approval_id: claims.approval_id.clone(),
                        permit_id: claims.jti.clone(),
                        input: lease_input,
                        policy_version: claims.policy_version.clone(),
                    };
                    let consumed = match &self.approval {
                        Some(coordinator) => coordinator.consume(&use_request, &trace_id).await,
                        None => Err("approval coordinator disabled"),
                    };
                    let current = crate::unix_ms().ok_or(ErrorCode::DependencyUnavailable)?;
                    if consumed.is_ok_and(|r| {
                        self.resolution_valid(&r, &use_request.input, &use_request.policy_version)
                            && r.approval_id == claims.approval_id
                            && r.permit_id.as_ref() == Some(&claims.jti)
                            && r.permit_expires_at_unix_ms.is_some_and(|expiry| {
                                expiry >= claims.expires_at_unix_ms && expiry > current
                            })
                            && matches!(
                                r.state,
                                ApprovalState::Consumed | ApprovalState::ApprovedTemporary
                            )
                    }) && self.policy.decision_valid(&initial, &input, current)
                        && self
                            .permit_signer
                            .as_ref()
                            .is_some_and(|s| s.verify(&request.permit, &input, current).is_ok())
                        && current < credential_deadline
                    {
                        decision.decision = Decision::Allow;
                        decision.reason = DecisionReason::Matched;
                    } else {
                        Self::block(&mut decision, DecisionReason::PolicyUnavailable);
                    }
                }
            }
        }
        self.audit(
            &input,
            &decision,
            trace_id.clone(),
            crate::unix_ms().ok_or(ErrorCode::DependencyUnavailable)?,
        )
        .await?;
        let current = crate::unix_ms().ok_or(ErrorCode::DependencyUnavailable)?;
        if decision.decision == Decision::Allow
            && (!self.policy.decision_valid(&initial, &input, current)
                || self
                    .permit_signer
                    .as_ref()
                    .is_none_or(|s| s.verify(&request.permit, &input, current).is_err())
                || current >= credential_deadline)
        {
            Self::block(&mut decision, DecisionReason::PolicyUnavailable);
            self.audit(&input, &decision, trace_id, current).await?;
        }
        Ok(decision)
    }

    /// Approval availability cannot make ordinary in-memory policy paths unsafe.
    pub fn ready(&self, now: u64) -> bool {
        self.policy.ready(now) && self.audit.ready()
    }
}
