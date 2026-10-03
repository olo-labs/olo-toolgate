// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
//! Administrator-owned immutable configuration; local callers cannot change destinations.
use crate::{Failure, Result};
use serde::{Deserialize, Serialize};
use std::path::PathBuf;

#[derive(Clone, Deserialize, Serialize)]
#[serde(rename_all = "camelCase", deny_unknown_fields)]
pub struct Config {
    pub server_url: String,
    pub state_directory: PathBuf,
    pub ipc_endpoint: String,
    pub authorized_peers: Vec<String>,
    pub ca_certificate_path: Option<PathBuf>,
    pub request_timeout_seconds: u64,
}
impl Config {
    pub fn validate(&self) -> Result<()> {
        origin(&self.server_url)?;
        if !self.state_directory.is_absolute()
            || self.authorized_peers.is_empty()
            || self.authorized_peers.len() > 32
            || !(1..=15).contains(&self.request_timeout_seconds)
            || self
                .ca_certificate_path
                .as_ref()
                .is_some_and(|p| !p.is_absolute())
        {
            return Err(Failure::Validation);
        }
        #[cfg(unix)]
        {
            if !std::path::Path::new(&self.ipc_endpoint).is_absolute()
                || self
                    .authorized_peers
                    .iter()
                    .any(|p| p.parse::<u32>().is_err())
            {
                return Err(Failure::Validation);
            }
        }
        #[cfg(windows)]
        {
            if self.ipc_endpoint != r"\\.\pipe\olo-toolgate-client"
                || self.authorized_peers.iter().any(|p| {
                    !p.starts_with("S-1-")
                        || !p
                            .bytes()
                            .all(|b| b.is_ascii_digit() || b == b'S' || b == b'-')
                })
            {
                return Err(Failure::Validation);
            }
        }
        Ok(())
    }
    pub fn load(path: &std::path::Path) -> Result<Self> {
        let bytes = crate::storage::read_owned(path, 65536, false)?;
        let config: Self = serde_json::from_slice(&bytes).map_err(|_| Failure::Validation)?;
        config.validate()?;
        Ok(config)
    }
}
/// Preserve exact HTTPS authority. Do not follow URLs learned from local IPC.
pub fn origin(value: &str) -> Result<String> {
    let url = reqwest::Url::parse(value).map_err(|_| Failure::Validation)?;
    if url.scheme() != "https"
        || url.host_str().is_none()
        || !url.username().is_empty()
        || url.password().is_some()
        || url.query().is_some()
        || url.fragment().is_some()
        || url.path() != "/"
    {
        return Err(Failure::Validation);
    }
    let host = url.host_str().ok_or(Failure::Validation)?;
    if host == "169.254.169.254"
        || host == "metadata.google.internal"
        || host == "0.0.0.0"
        || host == "::"
    {
        return Err(Failure::Validation);
    }
    Ok(url.as_str().trim_end_matches('/').to_owned())
}
