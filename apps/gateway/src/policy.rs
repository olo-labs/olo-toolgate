// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
//! Pure deterministic static policy, independently replaceable by verified bundles.
use crate::{extraction::identifier, validation::Contracts};
use olo_toolgate_contracts::{
    Decision, DecisionReason, PolicyDecision, PolicyInput, ResourceDescriptor,
};
use serde::{Deserialize, Serialize};
use std::{future::Future, pin::Pin};

/// Async boundary permits later adapters without hidden blocking I/O.
pub type PortFuture<'a, T> = Pin<Box<dyn Future<Output = T> + Send + 'a>>;

/// Evaluator failures and unsupported ASK must never become ALLOW.
pub trait PolicyEvaluator: Send + Sync {
    /// Return an explicit decision for this exact normalized input.
    fn evaluate<'a>(
        &'a self,
        input: &'a PolicyInput,
        now: u64,
    ) -> PortFuture<'a, Result<PolicyDecision, &'static str>>;
    /// Readiness means policy is safe for new traffic at this time.
    fn ready(&self, now: u64) -> bool;
}

/// Static inputs cannot express ASK before the approvals module exists.
#[derive(Clone, Deserialize, Serialize, PartialEq)]
#[serde(rename_all = "UPPERCASE")]
pub enum Effect {
    Allow,
    Block,
}

/// Exact matches; tenant is mandatory so an ALLOW cannot span organizations.
#[derive(Clone, Deserialize, Serialize)]
#[serde(rename_all = "camelCase", deny_unknown_fields)]
pub struct Rule {
    pub tenant_id: String,
    pub user_id: Option<String>,
    pub agent_id: Option<String>,
    pub device_id: Option<String>,
    pub tool_id: String,
    pub action: String,
    pub resource: ResourceDescriptor,
    pub effect: Effect,
}

impl Rule {
    fn matches(&self, input: &PolicyInput) -> bool {
        self.tenant_id == input.context.tenant_id
            && self.tool_id == input.tool_id
            && self.action == input.action
            && self.resource == input.resource
            && self
                .user_id
                .as_ref()
                .is_none_or(|v| v == &input.context.user_id)
            && self
                .agent_id
                .as_ref()
                .is_none_or(|v| v == &input.context.agent_id)
            && self
                .device_id
                .as_ref()
                .is_none_or(|v| Some(v) == input.context.device_id.as_ref())
    }
}

/// Trusted immutable administrator file, not an unsigned distributed bundle.
#[derive(Clone, Deserialize, Serialize)]
#[serde(rename_all = "camelCase", deny_unknown_fields)]
pub struct StaticPolicy {
    pub version: String,
    pub expires_at_unix_ms: u64,
    pub emergency_block: bool,
    pub rules: Vec<Rule>,
}

impl StaticPolicy {
    /// Validate bounded rule data and canonical version/resource constraints.
    pub fn validate(&self, contracts: &Contracts, now: u64) -> Result<(), &'static str> {
        let decision = self.block("validation", DecisionReason::NoMatch);
        if !contracts.valid(
            "PolicyDecision",
            &serde_json::to_value(decision).map_err(|_| "invalid policy")?,
        ) || self.expires_at_unix_ms <= now
            || self.rules.len() > 4096
        {
            return Err("invalid policy version, expiry or rule count");
        }
        for rule in &self.rules {
            if !identifier(&rule.tenant_id)
                || !identifier(&rule.tool_id)
                || !identifier(&rule.action)
                || [&rule.user_id, &rule.agent_id, &rule.device_id]
                    .iter()
                    .any(|v| v.as_ref().is_some_and(|v| !identifier(v)))
                || rule.resource.locator.is_empty()
                || rule.resource.locator.len() > 2048
            {
                return Err("invalid policy rule");
            }
        }
        Ok(())
    }

    fn block(&self, request_id: &str, reason: DecisionReason) -> PolicyDecision {
        PolicyDecision {
            decision: Decision::Block,
            reason,
            policy_version: self.version.clone(),
            request_id: request_id.into(),
        }
    }

    /// Emergency deny, explicit block, explicit allow, default block. Order of
    /// rule declaration cannot allow a caller to escape a matching block.
    pub fn decide(&self, input: &PolicyInput, now: u64) -> PolicyDecision {
        if !self.ready(now) {
            return self.block(&input.context.request_id, DecisionReason::PolicyUnavailable);
        }
        if self.emergency_block {
            return self.block(&input.context.request_id, DecisionReason::Matched);
        }
        let mut allow = false;
        for rule in &self.rules {
            if rule.matches(input) {
                if rule.effect == Effect::Block {
                    return self.block(&input.context.request_id, DecisionReason::Matched);
                }
                allow = true;
            }
        }
        if allow {
            PolicyDecision {
                decision: Decision::Allow,
                reason: DecisionReason::Matched,
                policy_version: self.version.clone(),
                request_id: input.context.request_id.clone(),
            }
        } else {
            self.block(&input.context.request_id, DecisionReason::NoMatch)
        }
    }
}

impl PolicyEvaluator for StaticPolicy {
    fn evaluate<'a>(
        &'a self,
        input: &'a PolicyInput,
        now: u64,
    ) -> PortFuture<'a, Result<PolicyDecision, &'static str>> {
        Box::pin(async move { Ok(self.decide(input, now)) })
    }
    fn ready(&self, now: u64) -> bool {
        self.expires_at_unix_ms > now
    }
}
