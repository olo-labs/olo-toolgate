// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
//! Trusted resource bindings select a bounded extractor before policy evaluation.
use olo_toolgate_contracts::{AuthorizationRequest, ResourceDescriptor, ResourceKind};
use serde::{Deserialize, Serialize};
use serde_json::Value;
use std::{collections::BTreeMap, sync::Arc};

/// Real variation point; implementations perform no I/O or untrusted execution.
pub trait ResourceExtractor: Send + Sync {
    /// Missing, ambiguous or unsafe resource values reject the request.
    fn extract(&self, arguments: &Value) -> Result<ResourceDescriptor, &'static str>;
}

/// Administrator-selected binding, never supplied by the authorization caller.
#[derive(Clone, Deserialize, Serialize)]
#[serde(rename_all = "camelCase", deny_unknown_fields)]
pub struct ExtractorBinding {
    pub tool_id: String,
    pub action: String,
    pub pointer: String,
    pub kind: ResourceKind,
}

impl ResourceExtractor for ExtractorBinding {
    fn extract(&self, arguments: &Value) -> Result<ResourceDescriptor, &'static str> {
        let raw = arguments
            .pointer(&self.pointer)
            .and_then(Value::as_str)
            .ok_or("resource missing")?;
        if raw.is_empty()
            || raw.len() > 2048
            || raw.trim() != raw
            || raw.chars().any(char::is_control)
            || raw.contains(['\\', '%', '?', '#'])
        {
            return Err("ambiguous resource");
        }
        // Reject aliases rather than guessing a platform-specific normalization.
        if raw
            .split('/')
            .any(|s| s == "." || s == ".." || s.is_empty())
            || raw.contains(':')
        {
            return Err("unsafe resource");
        }
        Ok(ResourceDescriptor {
            kind: self.kind.clone(),
            locator: raw.into(),
        })
    }
}

/// Immutable registry keyed by exact tool/action pairs. Unknown pairs fail closed.
pub struct Registry {
    bindings: BTreeMap<(String, String), Arc<dyn ResourceExtractor>>,
}

impl Registry {
    /// Only CUSTOM and relative FILE locators are supported in Module 01.
    pub fn new(bindings: Vec<ExtractorBinding>) -> Result<Self, &'static str> {
        if bindings.is_empty() || bindings.len() > 1024 {
            return Err("invalid extractor count");
        }
        let mut registry = Self {
            bindings: BTreeMap::new(),
        };
        for binding in bindings {
            if !identifier(&binding.tool_id)
                || !identifier(&binding.action)
                || !binding.pointer.starts_with('/')
                || binding.pointer.len() > 256
                || !matches!(binding.kind, ResourceKind::Custom | ResourceKind::File)
            {
                return Err("invalid extractor binding");
            }
            let key = (binding.tool_id.clone(), binding.action.clone());
            if registry.bindings.insert(key, Arc::new(binding)).is_some() {
                return Err("duplicate extractor binding");
            }
        }
        Ok(registry)
    }

    /// Dependency-injected extractor failures have the same fail-closed behavior.
    pub fn insert(&mut self, tool: String, action: String, extractor: Arc<dyn ResourceExtractor>) {
        self.bindings.insert((tool, action), extractor);
    }

    /// Resolve the exact registered action without interpreting caller URLs/paths.
    pub fn extract(
        &self,
        request: &AuthorizationRequest,
    ) -> Result<ResourceDescriptor, &'static str> {
        self.bindings
            .get(&(request.tool_id.clone(), request.action.clone()))
            .ok_or("unknown extractor")?
            .extract(&serde_json::to_value(&request.arguments).map_err(|_| "invalid arguments")?)
    }

    /// MCP names map to a single action; ambiguous multi-action tools reject.
    pub fn mcp_action(&self, tool: &str) -> Option<&str> {
        let mut actions = self
            .bindings
            .keys()
            .filter(|(t, _)| t == tool)
            .map(|(_, a)| a.as_str());
        let first = actions.next()?;
        actions.next().is_none().then_some(first)
    }
}

/// Contract identifier constraint, used for administrator configuration only.
pub fn identifier(value: &str) -> bool {
    !value.is_empty()
        && value.len() <= 128
        && value.as_bytes()[0].is_ascii_alphanumeric()
        && value
            .bytes()
            .all(|b| b.is_ascii_alphanumeric() || b"._:/-".contains(&b))
}
