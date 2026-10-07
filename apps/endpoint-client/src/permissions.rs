// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
//! Discovery is narrowed by the latest authenticated permission replacement.
use olo_toolgate_contracts::{Decision, EndpointPermissionConfiguration};

pub fn visible(
    configuration: &EndpointPermissionConfiguration,
    agent: Option<&str>,
    tool: &str,
    action: &str,
) -> bool {
    let applicable: Vec<_> = configuration
        .permissions
        .iter()
        .filter(|rule| {
            rule.tool_id == tool
                && rule.action == action
                && (rule.agent_ids.is_empty()
                    || agent.is_some_and(|id| rule.agent_ids.iter().any(|a| a == id)))
        })
        .collect();
    // An argument-free catalog cannot prove that a resource-specific denial does not apply.
    !applicable
        .iter()
        .any(|rule| rule.decision == Decision::Block)
        && applicable
            .iter()
            .any(|rule| matches!(rule.decision, Decision::Allow | Decision::Ask))
}

#[cfg(test)]
mod tests {
    use super::*;
    use olo_toolgate_contracts::{EndpointPermissionRule, ResourceDescriptor, ResourceKind};
    #[test]
    fn agent_scopes_deny_precedence_and_replacement_filter_discovery() {
        let rule = EndpointPermissionRule {
            tool_id: "local.tool".into(),
            action: "invoke".into(),
            agent_ids: vec!["agent-one".into()],
            resource: ResourceDescriptor {
                kind: ResourceKind::Custom,
                locator: "runtime/local.tool".into(),
            },
            decision: Decision::Ask,
        };
        let mut configuration = EndpointPermissionConfiguration {
            server_id: "server".into(),
            device_id: "device".into(),
            user_id: "owner".into(),
            revision: 1,
            digest: "0".repeat(64),
            permissions: vec![rule.clone()],
        };
        assert!(visible(
            &configuration,
            Some("agent-one"),
            "local.tool",
            "invoke"
        ));
        assert!(!visible(&configuration, None, "local.tool", "invoke"));
        assert!(!visible(
            &configuration,
            Some("agent-two"),
            "local.tool",
            "invoke"
        ));
        assert!(!visible(
            &configuration,
            Some("agent-one"),
            "another.tool",
            "invoke"
        ));
        let mut blocked = rule;
        blocked.decision = Decision::Block;
        configuration.permissions.push(blocked);
        assert!(!visible(
            &configuration,
            Some("agent-one"),
            "local.tool",
            "invoke"
        ));
        configuration.permissions.clear();
        assert!(!visible(
            &configuration,
            Some("agent-one"),
            "local.tool",
            "invoke"
        ));
    }
}
