// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
//! Immutable administrator configuration. Unknown settings are errors.
use crate::{extraction::ExtractorBinding, policy::StaticPolicy, validation::Contracts};
use serde::{Deserialize, Serialize};
use std::{io::Read, net::SocketAddr, path::Path};

/// Capacity protection is per replica, independent of authorization correctness.
#[derive(Clone, Deserialize, Serialize)]
#[serde(rename_all = "camelCase", deny_unknown_fields)]
pub struct Limits {
    pub max_body_bytes: usize,
    pub max_header_bytes: usize,
    pub max_concurrent_requests: usize,
    pub max_connections: usize,
    pub requests_per_second: u32,
    pub request_timeout_ms: u64,
    pub connection_timeout_ms: u64,
    pub shutdown_timeout_ms: u64,
    pub audit_queue_capacity: usize,
}

impl Default for Limits {
    fn default() -> Self {
        Self {
            max_body_bytes: 65536,
            max_header_bytes: 8192,
            max_concurrent_requests: 128,
            max_connections: 256,
            requests_per_second: 1000,
            request_timeout_ms: 2000,
            connection_timeout_ms: 10000,
            shutdown_timeout_ms: 15000,
            audit_queue_capacity: 256,
        }
    }
}

/// Trusted config file has precedence over compiled defaults. Environment only
/// selects its path and the separate credential file; no policy overrides exist.
#[derive(Clone, Deserialize, Serialize)]
#[serde(rename_all = "camelCase", deny_unknown_fields)]
pub struct Config {
    pub listen: SocketAddr,
    pub management_listen: SocketAddr,
    pub trusted_tls_proxy: bool,
    pub allowed_origins: Vec<String>,
    #[serde(default)]
    pub limits: Limits,
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub policy: Option<StaticPolicy>,
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub bundle_source: Option<crate::bundles::BundleSourceConfig>,
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub approval: Option<crate::approvals::ApprovalConfig>,
    pub extractors: Vec<ExtractorBinding>,
}

impl Config {
    /// Invalid or unbounded configuration prevents startup.
    pub fn validate(&self, contracts: &Contracts, now: u64) -> Result<(), &'static str> {
        if self.listen.port() == 0
            || self.management_listen.port() == 0
            || self.listen == self.management_listen
            || (!self.listen.ip().is_loopback() && !self.trusted_tls_proxy)
        {
            return Err("separate listeners and trusted TLS termination required");
        }
        let l = &self.limits;
        if !(256..=1_048_576).contains(&l.max_body_bytes)
            || !(1024..=32768).contains(&l.max_header_bytes)
            || !(1..=1024).contains(&l.max_concurrent_requests)
            || !(1..=4096).contains(&l.max_connections)
            || l.max_connections < l.max_concurrent_requests
            || !(1..=100_000).contains(&l.requests_per_second)
            || !(10..=30000).contains(&l.request_timeout_ms)
            || !(100..=60000).contains(&l.connection_timeout_ms)
            || l.connection_timeout_ms < l.request_timeout_ms
            || !(100..=120000).contains(&l.shutdown_timeout_ms)
            || l.shutdown_timeout_ms < l.request_timeout_ms
            || !(1..=4096).contains(&l.audit_queue_capacity)
        {
            return Err("invalid capacity limits");
        }
        if self.allowed_origins.len() > 32
            || self.allowed_origins.iter().any(|o| {
                o.len() > 256
                    || !o.starts_with("https://")
                    || o.contains(['\r', '\n', '*'])
                    || o.ends_with('/')
                    || o.parse::<axum::http::Uri>().ok().is_none_or(|u| {
                        u.scheme_str() != Some("https")
                            || u.authority().is_none_or(|a| a.as_str().contains('@'))
                            || u.path() != "/"
                            || u.query().is_some()
                    })
            })
        {
            return Err("origins must be exact HTTPS origins without a trailing slash");
        }
        match (&self.policy, &self.bundle_source) {
            (Some(policy), None) => policy.validate(contracts, now)?,
            (None, Some(source)) => source.validate()?,
            _ => return Err("exactly one static or signed policy source is required"),
        }
        if let Some(approval) = &self.approval {
            approval.validate()?;
            let source = self
                .bundle_source
                .as_ref()
                .ok_or("approvals require signed policy")?;
            if approval.private_key_path == source.keyring_path
                || approval.private_key_path == source.token_path
                || approval.private_key_path == approval.token_path
                || approval.request_timeout_ms >= l.request_timeout_ms
            {
                return Err("approval key domains or timeout invalid");
            }
        }
        crate::extraction::Registry::new(self.extractors.clone())?;
        Ok(())
    }
}

/// Bound startup file reads, including files whose metadata size is misleading.
pub fn read_json<T: serde::de::DeserializeOwned>(path: &Path) -> Result<T, &'static str> {
    let file = std::fs::File::open(path).map_err(|_| "configuration file unavailable")?;
    let mut bytes = Vec::new();
    file.take(1_048_577)
        .read_to_end(&mut bytes)
        .map_err(|_| "configuration read failed")?;
    if bytes.len() > 1_048_576 {
        return Err("configuration file too large");
    }
    serde_json::from_slice(&bytes).map_err(|_| "invalid configuration JSON")
}
