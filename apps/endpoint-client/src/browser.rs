// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
//! Browser setup invokes only the protected client shipped beside the native host.
use crate::{Failure, Result};
use serde::Deserialize;
use std::path::Path;

#[derive(Deserialize)]
#[serde(rename_all = "camelCase", deny_unknown_fields)]
struct SiteConfiguration {
    server_url: String,
}
pub fn console_origin(value: &str) -> Result<String> {
    let mut url = reqwest::Url::parse(value).map_err(|_| Failure::Validation)?;
    if !url.path().starts_with("/console/") || value.len() > 2048 {
        return Err(Failure::Validation);
    }
    url.set_path("/");
    url.set_query(None);
    url.set_fragment(None);
    if url.scheme() == "http"
        && matches!(url.host_str(), Some("localhost" | "127.0.0.1"))
        && url.username().is_empty()
        && url.password().is_none()
    {
        return Ok(url.as_str().trim_end_matches('/').to_owned());
    }
    crate::config::origin(url.as_str())
}
async fn fetch(client: &reqwest::Client, url: &str, limit: u64) -> Result<Vec<u8>> {
    let mut response = client
        .get(url)
        .send()
        .await
        .map_err(|_| Failure::Unavailable)?;
    if !response.status().is_success() || response.content_length().is_some_and(|n| n > limit) {
        return Err(Failure::Validation);
    }
    let mut bytes = Vec::new();
    while let Some(chunk) = response.chunk().await.map_err(|_| Failure::Unavailable)? {
        if bytes.len() as u64 + chunk.len() as u64 > limit {
            return Err(Failure::Validation);
        }
        bytes.extend_from_slice(&chunk);
    }
    Ok(bytes)
}
pub async fn connect(console_url: &str) -> Result<String> {
    let origin = console_origin(console_url)?;
    let client = reqwest::Client::builder()
        .redirect(reqwest::redirect::Policy::none())
        .timeout(std::time::Duration::from_secs(120))
        .build()
        .map_err(|_| Failure::Unavailable)?;
    let document = fetch(
        &client,
        &format!("{origin}/api/public/v1/clients/configuration"),
        4096,
    )
    .await?;
    let config: SiteConfiguration =
        serde_json::from_slice(&document).map_err(|_| Failure::Validation)?;
    let server = crate::config::origin(&config.server_url)?;
    let executable = std::env::current_exe().map_err(|_| Failure::Unavailable)?;
    let cli = executable
        .parent()
        .ok_or(Failure::Validation)?
        .join("olo-toolgate-client.exe");
    crate::storage::check_parents(&cli)?;
    crate::storage::check_owned(&cli, false)?;
    let peer = current_peer()?;
    let installed = crate::install::config_path();
    let operation = if installed.exists() {
        let settings = crate::config::Config::load(&installed)?;
        if settings.server_url == server && settings.authorized_peers.contains(&peer) {
            return Ok(server);
        }
        "configure"
    } else {
        "install"
    };
    let parameters = format!("{operation} --server \"{server}\" --peer \"{peer}\"");
    elevate(&cli, &parameters)?;
    Ok(server)
}
#[cfg(windows)]
fn current_peer() -> Result<String> {
    crate::platform::windows::current_sid()
}
#[cfg(not(windows))]
fn current_peer() -> Result<String> {
    Err(Failure::Unsupported)
}
#[cfg(not(windows))]
fn elevate(_: &Path, _: &str) -> Result<()> {
    Err(Failure::Unsupported)
}
#[cfg(windows)]
fn elevate(path: &Path, parameters: &str) -> Result<()> {
    use windows_sys::Win32::{
        Foundation::CloseHandle,
        System::Threading::{GetExitCodeProcess, WaitForSingleObject},
        UI::Shell::{ShellExecuteExW, SEE_MASK_NOCLOSEPROCESS, SHELLEXECUTEINFOW},
    };
    let wide = |s: &str| s.encode_utf16().chain(Some(0)).collect::<Vec<_>>();
    let path = wide(&path.to_string_lossy());
    let verb = wide("runas");
    let parameters = wide(parameters);
    unsafe {
        let mut info: SHELLEXECUTEINFOW = std::mem::zeroed();
        info.cbSize = std::mem::size_of::<SHELLEXECUTEINFOW>() as u32;
        info.fMask = SEE_MASK_NOCLOSEPROCESS;
        info.lpVerb = verb.as_ptr();
        info.lpFile = path.as_ptr();
        info.lpParameters = parameters.as_ptr();
        info.nShow = 0;
        if ShellExecuteExW(&mut info) == 0 || info.hProcess.is_null() {
            return Err(Failure::Unavailable);
        }
        let waited = WaitForSingleObject(info.hProcess, 600000);
        let mut code = 1;
        let success = waited == 0 && GetExitCodeProcess(info.hProcess, &mut code) != 0 && code == 0;
        CloseHandle(info.hProcess);
        if !success {
            return Err(Failure::Unavailable);
        }
    }
    Ok(())
}
#[cfg(test)]
mod tests {
    use super::*;
    #[test]
    fn only_https_console_origins_are_accepted() {
        assert_eq!(
            console_origin("https://gate.example/console/#enroll").unwrap(),
            "https://gate.example"
        );
        for url in [
            "http://gate.example/console/",
            "https://user:password@gate.example/console/",
            "https://gate.example/other",
        ] {
            assert!(console_origin(url).is_err());
        }
    }
}
