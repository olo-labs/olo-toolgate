// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
//! Transport-independent authorization coordination over explicit security ports.
use crate::{
    audit::AuditSink, digest, extraction::Registry, policy::PolicyEvaluator, validation::Contracts,
};
use olo_toolgate_contracts::{
    AuthorizationRequest, Decision, DecisionReason, ErrorCode, PolicyDecision, PolicyInput,
    RequestContext, RuntimeAuditEvent,
};
use std::sync::Arc;

/// No database, network client, execution engine or credential resolver exists here.
pub struct Gateway {
    pub contracts: Contracts,
    pub extractors: Registry,
    pub policy: Arc<dyn PolicyEvaluator>,
    pub audit: Arc<dyn AuditSink>,
}

impl Gateway {
    /// Validate, extract, evaluate and acknowledge sanitized audit before responding.
    /// The HTTP adapter bounds this entire future, including dependency waits.
    pub async fn authorize(
        &self,
        request: AuthorizationRequest,
        context: RequestContext,
        trace_id: String,
        now: u64,
    ) -> Result<PolicyDecision, ErrorCode> {
        let valid = serde_json::to_value(&request)
            .ok()
            .is_some_and(|v| self.contracts.valid("AuthorizationRequest", &v));
        if !valid {
            return Err(ErrorCode::Validation);
        }
        let resource = self
            .extractors
            .extract(&request)
            .map_err(|_| ErrorCode::Validation)?;
        let arguments_digest =
            digest(&serde_json::to_vec(&request.arguments).map_err(|_| ErrorCode::Validation)?);
        let input = PolicyInput {
            context,
            tool_id: request.tool_id,
            action: request.action,
            resource,
            arguments_digest,
        };
        if !self.contracts.valid(
            "PolicyInput",
            &serde_json::to_value(&input).map_err(|_| ErrorCode::Validation)?,
        ) {
            return Err(ErrorCode::Validation);
        }
        let mut decision = self
            .policy
            .evaluate(&input, now)
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
        if decision.decision == Decision::Ask {
            decision.decision = Decision::Block;
            decision.reason = DecisionReason::ApprovalRequired;
        }
        let event = RuntimeAuditEvent {
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
        };
        self.audit
            .record(event)
            .await
            .map_err(|_| ErrorCode::DependencyUnavailable)?;
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

    /// Policy freshness and local audit transport determine safety for new work.
    pub fn ready(&self, now: u64) -> bool {
        self.policy.ready(now) && self.audit.ready()
    }
}
