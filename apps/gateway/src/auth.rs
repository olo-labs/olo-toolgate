// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
//! Static runtime credentials bind all identity fields; client assertions never do.
use crate::{digest, validation::Contracts};
use olo_toolgate_contracts::RequestContext;
use serde::{Deserialize, Serialize};
use std::collections::BTreeSet;
use subtle::ConstantTimeEq;

/// No Debug implementation: credentials cannot accidentally enter telemetry.
#[derive(Clone, Deserialize, Serialize)]
#[serde(rename_all = "camelCase", deny_unknown_fields)]
pub struct Credential {
    pub token_sha256: String,
    pub tenant_id: String,
    pub user_id: String,
    pub agent_id: String,
    pub device_id: Option<String>,
    pub expires_at_unix_ms: u64,
}

/// Credential adapter for administrator-mounted high-entropy tokens.
pub struct Authenticator {
    credentials: Vec<Credential>,
}

impl Authenticator {
    /// Empty, duplicate, malformed or expired initial credentials reject startup.
    pub fn new(
        credentials: Vec<Credential>,
        contracts: &Contracts,
        now: u64,
    ) -> Result<Self, &'static str> {
        if credentials.is_empty() || credentials.len() > 256 {
            return Err("invalid credential count");
        }
        let mut hashes = BTreeSet::new();
        for c in &credentials {
            let context = RequestContext {
                request_id: "validation".into(),
                tenant_id: c.tenant_id.clone(),
                user_id: c.user_id.clone(),
                agent_id: c.agent_id.clone(),
                device_id: c.device_id.clone(),
            };
            if c.token_sha256.len() != 64
                || !c
                    .token_sha256
                    .bytes()
                    .all(|b| b.is_ascii_digit() || (b'a'..=b'f').contains(&b))
                || !hashes.insert(&c.token_sha256)
                || c.expires_at_unix_ms <= now
                || !contracts.valid(
                    "RequestContext",
                    &serde_json::to_value(context).map_err(|_| "invalid identity")?,
                )
            {
                return Err("invalid runtime credential");
            }
        }
        Ok(Self { credentials })
    }

    /// Compare digests without data-dependent equality and enforce runtime expiry.
    pub fn authenticate(
        &self,
        bearer: &str,
        request_id: String,
        now: u64,
    ) -> Option<(RequestContext, u64)> {
        if !(32..=256).contains(&bearer.len())
            || !bearer
                .bytes()
                .all(|b| b.is_ascii_alphanumeric() || b"-_.~".contains(&b))
        {
            return None;
        }
        let hash = digest(bearer.as_bytes());
        let mut matched = None;
        for credential in &self.credentials {
            if bool::from(hash.as_bytes().ct_eq(credential.token_sha256.as_bytes()))
                && credential.expires_at_unix_ms > now
            {
                matched = Some(credential);
            }
        }
        matched.map(|c| {
            (
                RequestContext {
                    request_id,
                    tenant_id: c.tenant_id.clone(),
                    user_id: c.user_id.clone(),
                    agent_id: c.agent_id.clone(),
                    device_id: c.device_id.clone(),
                },
                c.expires_at_unix_ms,
            )
        })
    }

    /// Readiness requires at least one usable credential.
    pub fn ready(&self, now: u64) -> bool {
        self.credentials.iter().any(|c| c.expires_at_unix_ms > now)
    }
}
