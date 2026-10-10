// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
//! Explicit OS service installation commands; fixed paths/arguments and no shell evaluation.
use crate::{config::Config, Failure, Result};
use std::{
    path::{Path, PathBuf},
    process::Command,
};
pub fn config_path() -> PathBuf {
    #[cfg(target_os = "linux")]
    {
        PathBuf::from("/etc/olo-toolgate/client.json")
    }
    #[cfg(target_os = "macos")]
    {
        PathBuf::from("/Library/Application Support/OLO/ToolGate/client.json")
    }
    #[cfg(windows)]
    {
        PathBuf::from(r"C:\ProgramData\OLO\ToolGate\client.json")
    }
}
pub fn ipc_endpoint() -> String {
    #[cfg(target_os = "linux")]
    {
        "/run/olo-toolgate/client.sock".into()
    }
    #[cfg(target_os = "macos")]
    {
        "/Library/Application Support/OLO/ToolGate/run/client.sock".into()
    }
    #[cfg(windows)]
    {
        r"\\.\pipe\olo-toolgate-client".into()
    }
}
pub fn binary_path() -> PathBuf {
    #[cfg(target_os = "linux")]
    {
        PathBuf::from("/usr/local/lib/olo-toolgate/olo-toolgate-client")
    }
    #[cfg(target_os = "macos")]
    {
        PathBuf::from("/Library/Application Support/OLO/ToolGate/bin/olo-toolgate-client")
    }
    #[cfg(windows)]
    {
        PathBuf::from(r"C:\Program Files\OLO\ToolGate\olo-toolgate-client.exe")
    }
}
fn directory(path: &Path, private: bool) -> Result<()> {
    if !path.exists() {
        let parent = path.parent().ok_or(Failure::Validation)?;
        if !parent.exists() {
            directory(parent, false)?;
        }
        check_directory(parent, false)?;
        let builder = std::fs::DirBuilder::new();
        #[cfg(unix)]
        let mut builder = builder;
        #[cfg(unix)]
        {
            use std::os::unix::fs::DirBuilderExt;
            builder.mode(if private { 0o700 } else { 0o755 });
        }
        builder.create(path).map_err(|_| Failure::Unavailable)?;
        #[cfg(windows)]
        crate::platform::windows::protect_install_acl(path, !private)?;
    }
    check_directory(path, private)
}
fn check_directory(path: &Path, private: bool) -> Result<()> {
    crate::storage::check_owned(path, private)?;
    #[cfg(windows)]
    {
        crate::platform::windows::check_install_acl(path, private)?;
        for ancestor in path.ancestors().skip(1) {
            crate::storage::check_owned(ancestor, false)?;
            crate::platform::windows::check_install_acl(ancestor, false)?;
        }
    }
    Ok(())
}
fn admin() -> Result<()> {
    #[cfg(unix)]
    {
        crate::platform::require_service_identity()
    }
    #[cfg(windows)]
    {
        crate::platform::windows::require_administrator()
    }
}
fn command(program: &str, args: &[&str]) -> Result<()> {
    let mut command = Command::new(program);
    command.args(args);
    #[cfg(windows)]
    {
        use std::os::windows::process::CommandExt;
        command.creation_flags(0x08000000);
    }
    if command
        .status()
        .map_err(|_| Failure::Unavailable)?
        .success()
    {
        Ok(())
    } else {
        Err(Failure::Unavailable)
    }
}
pub fn install(server: &str) -> Result<()> {
    install_for_peer(server, None)
}
pub fn install_for_peer(server: &str, peer: Option<&str>) -> Result<()> {
    install_for_peer_with_ca(server, peer, None, None)
}
pub fn install_for_peer_with_ca(
    server: &str,
    peer: Option<&str>,
    ca: Option<&str>,
    console: Option<&str>,
) -> Result<()> {
    install_config(server, peer, None, ca, console)
}
pub fn reinstall(server: &str, peer: Option<&str>) -> Result<()> {
    reinstall_with_ca(server, peer, None, None)
}
pub fn reinstall_with_ca(
    server: &str,
    peer: Option<&str>,
    ca: Option<&str>,
    console: Option<&str>,
) -> Result<()> {
    admin()?;
    validate_peer(peer)?;
    #[cfg(windows)]
    if ca.is_some() {
        // Reconcile persisted enrollment too: the config may already contain a replacement CA.
        configure_with_ca(server, peer, ca, console)?;
    }
    let previous = Config::load(&config_path())?;
    if crate::config::origin(server)? != previous.server_url {
        return Err(Failure::Conflict);
    }
    uninstall(false)?;
    install_config(server, peer, Some(previous), ca, console)
}
#[cfg(not(windows))]
pub fn configure(_: &str, _: Option<&str>) -> Result<()> {
    Err(Failure::Unsupported)
}
#[cfg(not(windows))]
pub fn configure_with_ca(_: &str, _: Option<&str>, _: Option<&str>, _: Option<&str>) -> Result<()> {
    Err(Failure::Unsupported)
}
#[cfg(windows)]
pub fn configure(server: &str, peer: Option<&str>) -> Result<()> {
    configure_with_ca(server, peer, None, None)
}
/// Switches focus or repairs the gateway. The leaving gateway's enrollment is parked in a
/// profile that stays connected in the background, so switching back resumes it unless
/// that gateway was since recreated.
#[cfg(windows)]
pub fn configure_with_ca(
    server: &str,
    peer: Option<&str>,
    ca: Option<&str>,
    console: Option<&str>,
) -> Result<()> {
    admin()?;
    validate_peer(peer)?;
    let server = crate::config::origin(server)?;
    let console = console.map(crate::config::local_console).transpose()?;
    let previous = Config::load(&config_path())?;
    let switching = previous.server_url != server;
    let trust_changed = !switching && local_gateway_trust_changed(&previous, ca)?;
    let mut settings = previous.clone();
    let path = config_path();
    let mut moves = Vec::new();
    let mut retired = Vec::new();
    let update = (|| {
        stop_windows_service()?;
        if switching {
            archive_profile(&previous, &mut moves)?;
            settings.server_url = server.clone();
            match restore_profile(&previous.state_directory, &server, ca, &mut moves)? {
                Some((saved, snapshot)) => {
                    settings.tools = saved.tools;
                    settings.execution = saved.execution;
                    settings.deployment = saved.deployment;
                    settings.ca_certificate_path = saved.ca_certificate_path;
                    settings.local_console_url = saved.local_console_url;
                    retired.push(snapshot);
                }
                None => clear_gateway_settings(&mut settings),
            }
        } else if trust_changed {
            // The same URL now serves a recreated gateway; its old enrollment is unusable.
            clear_gateway_settings(&mut settings);
            for name in GATEWAY_STATE {
                let source = settings.state_directory.join(name);
                if source.try_exists().map_err(|_| Failure::Unavailable)? {
                    crate::storage::check_owned(&source, true)?;
                    let backup =
                        source.with_extension(format!("{}.json", crate::identity::nonce()?));
                    std::fs::rename(&source, &backup).map_err(|_| Failure::Unavailable)?;
                    moves.push((source, backup.clone()));
                    retired.push(backup);
                }
            }
        }
        if let Some(console) = &console {
            settings.local_console_url = Some(console.clone());
        }
        if let Some(peer) = peer {
            if !settings.authorized_peers.iter().any(|p| p == peer) {
                settings.authorized_peers.push(peer.to_owned());
            }
        }
        if let Some(ca) = ca {
            settings.ca_certificate_path = Some(save_ca(ca)?);
        }
        settings.validate()?;
        write_config(&path, &settings)
    })();
    if let Err(failure) = update {
        for (source, destination) in moves.iter().rev() {
            let _ = std::fs::rename(destination, source);
        }
        let _ = command(
            r"C:\Windows\System32\sc.exe",
            &["start", "OloToolGateClient"],
        );
        return Err(failure);
    }
    for file in retired {
        std::fs::remove_file(file).map_err(|_| Failure::Unavailable)?;
    }
    if let Err(failure) = record_gateway(&settings, switching.then_some(&previous)) {
        tracing::warn!(event="gateway_index",error=?failure);
    }
    let registry = command(
        r"C:\Windows\System32\reg.exe",
        &[
            "add",
            r"HKLM\Software\OLO\ToolGate",
            "/v",
            "ServerUrl",
            "/t",
            "REG_SZ",
            "/d",
            &server,
            "/f",
        ],
    );
    let start = command(
        r"C:\Windows\System32\sc.exe",
        &["start", "OloToolGateClient"],
    );
    start.and(registry)
}
#[cfg(windows)]
fn stop_windows_service() -> Result<()> {
    use windows_service::{
        service::{ServiceAccess, ServiceState},
        service_manager::{ServiceManager, ServiceManagerAccess},
    };
    let manager = ServiceManager::local_computer(None::<&str>, ServiceManagerAccess::CONNECT)
        .map_err(|_| Failure::Unavailable)?;
    let service = manager
        .open_service(
            "OloToolGateClient",
            ServiceAccess::QUERY_STATUS | ServiceAccess::STOP,
        )
        .map_err(|_| Failure::Unavailable)?;
    if service
        .query_status()
        .map_err(|_| Failure::Unavailable)?
        .current_state
        == ServiceState::Running
    {
        service.stop().map_err(|_| Failure::Unavailable)?;
    }
    let deadline = std::time::Instant::now() + std::time::Duration::from_secs(30);
    while service
        .query_status()
        .map_err(|_| Failure::Unavailable)?
        .current_state
        != ServiceState::Stopped
    {
        if std::time::Instant::now() >= deadline {
            return Err(Failure::Unavailable);
        }
        std::thread::sleep(std::time::Duration::from_millis(100));
    }
    Ok(())
}
fn validate_peer(peer: Option<&str>) -> Result<()> {
    #[cfg(windows)]
    if peer.is_some_and(|sid| {
        !sid.starts_with("S-1-")
            || sid.len() > 184
            || !sid
                .bytes()
                .all(|b| b.is_ascii_digit() || b == b'S' || b == b'-')
    }) {
        return Err(Failure::Validation);
    }
    #[cfg(unix)]
    if peer.is_some() {
        return Err(Failure::Unsupported);
    }
    Ok(())
}
/// Only installer-verified local quickstart trust is supplied here. Its CA is also
/// the device issuer, so a recreated gateway invalidates enrollment even when its URL is unchanged.
#[cfg(any(windows, test))]
fn local_gateway_trust_changed(settings: &Config, ca: Option<&str>) -> Result<bool> {
    let Some(ca) = ca else { return Ok(false) };
    let expected = crate::identity::certificate_der(ca)?;
    if let Some(path) = &settings.ca_certificate_path {
        let current = crate::storage::read_owned(path, 16384, false)?;
        let current = std::str::from_utf8(&current).map_err(|_| Failure::Validation)?;
        if crate::identity::certificate_der(current)? != expected {
            return Ok(true);
        }
    }
    let journal_path = settings.state_directory.join("journal.json");
    if journal_path
        .try_exists()
        .map_err(|_| Failure::Unavailable)?
    {
        let bytes = crate::storage::read_owned(&journal_path, 131072, true)?;
        let journal: serde_json::Value =
            serde_json::from_slice(&bytes).map_err(|_| Failure::Validation)?;
        for field in ["manifest", "identity"] {
            if let Some(saved) = journal.get(field).filter(|value| !value.is_null()) {
                let issuer = saved
                    .get("issuerCertificatePem")
                    .and_then(|value| value.as_str())
                    .ok_or(Failure::Validation)?;
                if crate::identity::certificate_der(issuer)? != expected {
                    return Ok(true);
                }
            }
        }
    }
    Ok(false)
}
fn save_ca(ca: &str) -> Result<PathBuf> {
    if ca.len() > 16384 {
        return Err(Failure::Validation);
    }
    reqwest::Certificate::from_pem(ca.as_bytes()).map_err(|_| Failure::Validation)?;
    use sha2::Digest;
    let path = config_path()
        .parent()
        .ok_or(Failure::Validation)?
        .join(format!(
            "gateway-ca-{}.crt",
            sha2::Sha256::digest(ca.as_bytes())
                .iter()
                .map(|byte| format!("{byte:02x}"))
                .collect::<String>()
        ));
    crate::storage::check_parents(&path)?;
    if path.exists() {
        if crate::storage::read_owned(&path, 16384, false)? != ca.as_bytes() {
            return Err(Failure::Conflict);
        }
        return Ok(path);
    }
    let temporary = path.with_extension(format!("{}.crt", crate::identity::nonce()?));
    use std::io::Write;
    let mut options = std::fs::OpenOptions::new();
    options.write(true).create_new(true);
    #[cfg(unix)]
    {
        use std::os::unix::fs::OpenOptionsExt;
        options.mode(0o644).custom_flags(libc::O_NOFOLLOW);
    }
    let mut file = options.open(&temporary).map_err(|_| Failure::Unavailable)?;
    file.write_all(ca.as_bytes())
        .and_then(|_| file.sync_all())
        .map_err(|_| Failure::Unavailable)?;
    drop(file);
    #[cfg(windows)]
    crate::platform::windows::protect_install_acl(&temporary, true)?;
    std::fs::rename(&temporary, &path).map_err(|_| Failure::Unavailable)?;
    Ok(path)
}
/// Gateway-scoped protected state. The device key, activity and packet logs belong to the device.
#[cfg(any(windows, test))]
const GATEWAY_STATE: [&str; 7] = [
    "journal.json",
    "permissions.json",
    "remote-journal.json",
    "effect-journal.json",
    "adoption.json",
    "fleet-intent.json",
    "fleet-active.json",
];
/// These settings contain gateway destinations, pins and credentials from the old site.
#[cfg(windows)]
fn clear_gateway_settings(settings: &mut Config) {
    settings.tools = None;
    settings.execution = None;
    settings.deployment = None;
    settings.ca_certificate_path = None;
    settings.local_console_url = None;
}
fn write_new(path: &Path, bytes: &[u8], public: bool) -> Result<()> {
    use std::io::Write;
    let mut options = std::fs::OpenOptions::new();
    options.write(true).create_new(true);
    #[cfg(unix)]
    {
        use std::os::unix::fs::OpenOptionsExt;
        options
            .mode(if public { 0o644 } else { 0o600 })
            .custom_flags(libc::O_NOFOLLOW);
    }
    let mut file = options.open(path).map_err(|_| Failure::Conflict)?;
    let written = file
        .write_all(bytes)
        .and_then(|_| file.sync_all())
        .map_err(|_| Failure::Unavailable);
    drop(file);
    #[cfg(windows)]
    let written = written.and_then(|_| crate::platform::windows::protect_install_acl(path, public));
    if written.is_err() {
        let _ = std::fs::remove_file(path);
    }
    written
}
/// Atomically replaces a public, administrator-owned document beside the configuration.
fn replace_public(path: &Path, bytes: &[u8]) -> Result<()> {
    let temporary = path.with_extension(format!("{}.json", crate::identity::nonce()?));
    write_new(&temporary, bytes, true)?;
    std::fs::rename(&temporary, path).map_err(|_| {
        let _ = std::fs::remove_file(&temporary);
        Failure::Unavailable
    })
}
#[cfg(windows)]
fn write_config(path: &Path, settings: &Config) -> Result<()> {
    replace_public(
        path,
        &serde_json::to_vec_pretty(settings).map_err(|_| Failure::Validation)?,
    )
}
#[cfg(any(windows, test))]
fn profile_directory(state: &Path, server: &str) -> PathBuf {
    state
        .join("profiles")
        .join(crate::connections::profile_name(server))
}
#[cfg(any(windows, test))]
fn move_owned(
    source: &Path,
    destination: &Path,
    moves: &mut Vec<(PathBuf, PathBuf)>,
) -> Result<()> {
    if source.try_exists().map_err(|_| Failure::Unavailable)? {
        crate::storage::check_owned(source, true)?;
        std::fs::rename(source, destination).map_err(|_| Failure::Unavailable)?;
        moves.push((source.to_owned(), destination.to_owned()));
    }
    Ok(())
}
#[cfg(any(windows, test))]
fn discard_profile(profile: &Path) -> Result<()> {
    // The background connection also keeps its own lease, activity and packet logs here.
    for name in GATEWAY_STATE.into_iter().chain([
        "client.json",
        "service.lock",
        "activity.json",
        "packets.jsonl",
    ]) {
        let path = profile.join(name);
        if path.try_exists().map_err(|_| Failure::Unavailable)? {
            crate::storage::check_owned(&path, true)?;
            std::fs::remove_file(path).map_err(|_| Failure::Unavailable)?;
        }
    }
    match std::fs::remove_dir(profile) {
        Err(error) if error.kind() != std::io::ErrorKind::NotFound => Err(Failure::Unavailable),
        _ => Ok(()),
    }
}
/// Parks the active gateway's settings and enrollment so a later switch back resumes it.
#[cfg(any(windows, test))]
fn archive_profile(settings: &Config, moves: &mut Vec<(PathBuf, PathBuf)>) -> Result<()> {
    let profile = profile_directory(&settings.state_directory, &settings.server_url);
    directory(profile.parent().ok_or(Failure::Validation)?, true)?;
    // Any older parked copy of this gateway is superseded by the active state.
    discard_profile(&profile)?;
    directory(&profile, true)?;
    write_new(
        &profile.join("client.json"),
        &serde_json::to_vec_pretty(settings).map_err(|_| Failure::Validation)?,
        false,
    )?;
    for name in GATEWAY_STATE {
        move_owned(
            &settings.state_directory.join(name),
            &profile.join(name),
            moves,
        )?;
    }
    Ok(())
}
/// Resumes a parked gateway unless the gateway was recreated with another CA since then.
/// Returns its saved settings and the snapshot file to retire once the switch commits.
#[cfg(any(windows, test))]
fn restore_profile(
    state: &Path,
    server: &str,
    ca: Option<&str>,
    moves: &mut Vec<(PathBuf, PathBuf)>,
) -> Result<Option<(Config, PathBuf)>> {
    let profile = profile_directory(state, server);
    let snapshot = profile.join("client.json");
    if !snapshot.try_exists().map_err(|_| Failure::Unavailable)? {
        return Ok(None);
    }
    let saved = Config::load(&snapshot)
        .ok()
        .filter(|saved| saved.server_url == server && saved.state_directory == state)
        .filter(|saved| {
            let parked = Config {
                state_directory: profile.clone(),
                ..saved.clone()
            };
            !local_gateway_trust_changed(&parked, ca).unwrap_or(true)
        });
    let Some(saved) = saved else {
        discard_profile(&profile)?;
        return Ok(None);
    };
    for name in GATEWAY_STATE {
        move_owned(&profile.join(name), &state.join(name), moves)?;
    }
    Ok(Some((saved, snapshot)))
}
/// Public list of gateways this device has used, so the tray can offer one-click switching.
#[derive(Clone, serde::Deserialize, serde::Serialize)]
#[serde(rename_all = "camelCase", deny_unknown_fields)]
struct KnownGateway {
    server_url: String,
    #[serde(default)]
    console_url: Option<String>,
}
fn gateway_index_path() -> PathBuf {
    config_path().with_file_name("gateways.json")
}
fn known_gateways() -> Vec<KnownGateway> {
    crate::storage::read_owned(&gateway_index_path(), 16384, false)
        .ok()
        .and_then(|bytes| serde_json::from_slice::<Vec<KnownGateway>>(&bytes).ok())
        .unwrap_or_default()
        .into_iter()
        .filter(|gateway| {
            crate::config::origin(&gateway.server_url).ok().as_ref() == Some(&gateway.server_url)
                && gateway.console_url.as_ref().is_none_or(|console| {
                    crate::config::local_console(console).ok().as_ref() == Some(console)
                        && crate::config::loopback(&gateway.server_url)
                })
        })
        .collect()
}
/// The loopback console recorded for a local gateway, used to refresh its CA on repair.
pub fn remembered_console(server: &str) -> Option<String> {
    if let Ok(active) = Config::load(&config_path()) {
        if active.server_url == server && active.local_console_url.is_some() {
            return active.local_console_url;
        }
    }
    known_gateways()
        .into_iter()
        .find(|gateway| gateway.server_url == server)
        .and_then(|gateway| gateway.console_url)
}
fn record_gateway(current: &Config, previous: Option<&Config>) -> Result<()> {
    let mut gateways = known_gateways();
    for config in previous.into_iter().chain([current]) {
        let console = config.local_console_url.clone().or_else(|| {
            gateways
                .iter()
                .find(|gateway| gateway.server_url == config.server_url)
                .and_then(|gateway| gateway.console_url.clone())
        });
        gateways.retain(|gateway| gateway.server_url != config.server_url);
        gateways.insert(
            0,
            KnownGateway {
                server_url: config.server_url.clone(),
                console_url: console,
            },
        );
    }
    gateways.truncate(16);
    replace_public(
        &gateway_index_path(),
        &serde_json::to_vec_pretty(&gateways).map_err(|_| Failure::Validation)?,
    )
}
fn install_config(
    server: &str,
    peer: Option<&str>,
    previous: Option<Config>,
    ca: Option<&str>,
    console: Option<&str>,
) -> Result<()> {
    admin()?;
    let origin = crate::config::origin(server)?;
    validate_peer(peer)?;
    let binary = binary_path();
    let config = config_path();
    if config.exists() {
        return Err(Failure::Conflict);
    }
    directory(binary.parent().ok_or(Failure::Validation)?, false)?;
    directory(config.parent().ok_or(Failure::Validation)?, false)?;
    #[cfg(target_os = "linux")]
    let (state, mut peers) = (PathBuf::from("/var/lib/olo-toolgate"), vec!["0".to_owned()]);
    #[cfg(target_os = "macos")]
    let (state, mut peers) = (
        PathBuf::from("/Library/Application Support/OLO/ToolGate/state"),
        vec!["0".to_owned()],
    );
    #[cfg(unix)]
    if let Ok(uid) = std::env::var("SUDO_UID") {
        if uid.parse::<u32>().is_ok() && !peers.contains(&uid) {
            peers.push(uid);
        }
    }
    #[cfg(windows)]
    let (state, peers) = (
        PathBuf::from(r"C:\ProgramData\OLO\ToolGate\state"),
        vec![
            "S-1-5-18".to_owned(),
            crate::platform::windows::current_sid()?,
        ],
    );
    directory(&state, true)?;
    #[cfg(unix)]
    directory(
        Path::new(&ipc_endpoint())
            .parent()
            .ok_or(Failure::Validation)?,
        false,
    )?;
    let mut settings = previous.unwrap_or(Config {
        deployment: None,
        execution: None,
        tools: None,
        server_url: origin,
        state_directory: state,
        ipc_endpoint: ipc_endpoint(),
        authorized_peers: peers,
        ca_certificate_path: None,
        request_timeout_seconds: 10,
        local_console_url: None,
    });
    if let Some(peer) = peer {
        if !settings.authorized_peers.iter().any(|value| value == peer) {
            settings.authorized_peers.push(peer.to_owned());
        }
    }
    if let Some(ca) = ca {
        settings.ca_certificate_path = Some(save_ca(ca)?);
    }
    if let Some(console) = console {
        settings.local_console_url = Some(crate::config::local_console(console)?);
    }
    settings.validate()?;
    let executable = std::env::current_exe().map_err(|_| Failure::Unavailable)?;
    let bytes = std::fs::read(executable).map_err(|_| Failure::Unavailable)?;
    let mut options = std::fs::OpenOptions::new();
    options.write(true).create_new(true);
    #[cfg(unix)]
    {
        use std::os::unix::fs::OpenOptionsExt;
        options.mode(0o755).custom_flags(libc::O_NOFOLLOW);
    }
    use std::io::Write;
    let mut file = options.open(&binary).map_err(|_| Failure::Conflict)?;
    file.write_all(&bytes)
        .and_then(|_| file.sync_all())
        .map_err(|_| Failure::Unavailable)?;
    drop(file);
    #[cfg(windows)]
    crate::platform::windows::protect_install_acl(&binary, true)?;
    let mut options = std::fs::OpenOptions::new();
    options.write(true).create_new(true);
    #[cfg(unix)]
    {
        use std::os::unix::fs::OpenOptionsExt;
        options.mode(0o644).custom_flags(libc::O_NOFOLLOW);
    }
    options
        .open(&config)
        .and_then(|mut f| {
            f.write_all(&serde_json::to_vec_pretty(&settings).map_err(std::io::Error::other)?)
        })
        .map_err(|_| Failure::Unavailable)?;
    #[cfg(windows)]
    crate::platform::windows::protect_install_acl(&config, true)?;
    #[cfg(target_os = "linux")]
    {
        let unit = include_str!("../packaging/olo-toolgate-client.service");
        write_definition(
            Path::new("/etc/systemd/system/olo-toolgate-client.service"),
            unit.as_bytes(),
        )?;
        command("/usr/bin/systemctl", &["daemon-reload"])?;
        command(
            "/usr/bin/systemctl",
            &["enable", "--now", "olo-toolgate-client.service"],
        )?;
    }
    #[cfg(target_os = "macos")]
    {
        write_definition(
            Path::new("/Library/LaunchDaemons/io.ololabs.toolgate.client.plist"),
            include_bytes!("../packaging/io.ololabs.toolgate.client.plist"),
        )?;
        command(
            "/bin/launchctl",
            &[
                "bootstrap",
                "system",
                "/Library/LaunchDaemons/io.ololabs.toolgate.client.plist",
            ],
        )?;
    }
    #[cfg(windows)]
    {
        // Installer paths are fixed, so binPath quoting cannot contain caller-controlled switches.
        let path = format!("\"{}\" service", binary.to_string_lossy());
        command(
            r"C:\Windows\System32\sc.exe",
            &[
                "create",
                "OloToolGateClient",
                "binPath=",
                &path,
                "start=",
                "auto",
                "obj=",
                "LocalSystem",
            ],
        )?;
        command(
            r"C:\Windows\System32\sc.exe",
            &["start", "OloToolGateClient"],
        )?;
    }
    if let Err(failure) = record_gateway(&settings, None) {
        tracing::warn!(event="gateway_index",error=?failure);
    }
    Ok(())
}
#[cfg(unix)]
fn write_definition(path: &Path, bytes: &[u8]) -> Result<()> {
    crate::storage::check_parents(path)?;
    use std::io::Write;
    let mut options = std::fs::OpenOptions::new();
    options.write(true).create_new(true);
    #[cfg(unix)]
    {
        use std::os::unix::fs::OpenOptionsExt;
        options.mode(0o644).custom_flags(libc::O_NOFOLLOW);
    }
    options
        .open(path)
        .and_then(|mut f| f.write_all(bytes))
        .map_err(|_| Failure::Conflict)
}
/// Uninstall retains enrollment custody by default; purge deletes only the fixed protected state directory.
pub fn uninstall(purge: bool) -> Result<()> {
    admin()?;
    let config = Config::load(&config_path())?;
    #[cfg(target_os = "linux")]
    {
        command(
            "/usr/bin/systemctl",
            &["disable", "--now", "olo-toolgate-client.service"],
        )?;
        std::fs::remove_file("/etc/systemd/system/olo-toolgate-client.service")
            .map_err(|_| Failure::Unavailable)?;
        command("/usr/bin/systemctl", &["daemon-reload"])?;
    }
    #[cfg(target_os = "macos")]
    {
        command(
            "/bin/launchctl",
            &["bootout", "system/io.ololabs.toolgate.client"],
        )?;
        std::fs::remove_file("/Library/LaunchDaemons/io.ololabs.toolgate.client.plist")
            .map_err(|_| Failure::Unavailable)?;
    }
    #[cfg(windows)]
    {
        stop_windows_service()?;
        command(
            r"C:\Windows\System32\sc.exe",
            &["delete", "OloToolGateClient"],
        )?;
    }
    if purge {
        #[cfg(target_os = "linux")]
        let expected = PathBuf::from("/var/lib/olo-toolgate");
        #[cfg(target_os = "macos")]
        let expected = PathBuf::from("/Library/Application Support/OLO/ToolGate/state");
        #[cfg(windows)]
        let expected = PathBuf::from(r"C:\ProgramData\OLO\ToolGate\state");
        if config.state_directory != expected {
            return Err(Failure::Unauthorized);
        }
        crate::storage::check_owned(&expected, true)?;
        // A nonrecursive known-file purge cannot follow directory links or erase unrelated files.
        for name in [
            "journal.json",
            "permissions.json",
            "remote-journal.json",
            "fleet-intent.json",
            "fleet-active.json",
            "device-key",
            "service.lock",
        ] {
            let path = expected.join(name);
            if path.exists() {
                crate::storage::check_owned(&path, true)?;
                std::fs::remove_file(path).map_err(|_| Failure::Unavailable)?;
            }
        }
        match std::fs::remove_dir(expected) {
            Ok(()) => {}
            Err(error) if error.kind() == std::io::ErrorKind::DirectoryNotEmpty => {}
            Err(_) => return Err(Failure::Unavailable),
        }
    }
    std::fs::remove_file(config_path()).map_err(|_| Failure::Unavailable)?;
    std::fs::remove_file(binary_path()).map_err(|_| Failure::Unavailable)?;
    Ok(())
}

#[cfg(test)]
mod tests {
    use super::*;
    #[test]
    fn repair_detects_stale_enrollment_after_config_ca_was_already_updated() {
        #[cfg(windows)]
        let base = PathBuf::from(std::env::var_os("ProgramData").unwrap());
        #[cfg(target_os = "macos")]
        let base = PathBuf::from("/private/tmp");
        #[cfg(all(unix, not(target_os = "macos")))]
        let base = std::env::temp_dir();
        let directory = base.join(format!(
            "toolgate-trust-test-{}",
            crate::identity::nonce().unwrap()
        ));
        let store = crate::storage::ProtectedStore::open(directory.clone()).unwrap();
        let old = rcgen::generate_simple_self_signed(vec!["localhost".into()])
            .unwrap()
            .cert
            .pem();
        let new = rcgen::generate_simple_self_signed(vec!["localhost".into()])
            .unwrap()
            .cert
            .pem();
        let path = directory.join("ca.crt");
        std::fs::write(&path, &new).unwrap();
        #[cfg(windows)]
        crate::platform::windows::protect_acl(&path).unwrap();
        let settings = Config {
            deployment: None,
            execution: None,
            tools: None,
            server_url: "https://localhost:18450".into(),
            state_directory: directory.clone(),
            ipc_endpoint: ipc_endpoint(),
            authorized_peers: vec!["test".into()],
            ca_certificate_path: Some(path.clone()),
            request_timeout_seconds: 10,
            local_console_url: None,
        };
        assert!(!local_gateway_trust_changed(&settings, Some(&new)).unwrap());
        let journal = |issuer: &str| {
            serde_json::to_vec(
                &serde_json::json!({"identity":{"issuerCertificatePem":issuer},"manifest":null}),
            )
            .unwrap()
        };
        store.write("journal.json", &journal(&old)).unwrap();
        // This was missed when setup compared only its already-updated config CA.
        assert!(local_gateway_trust_changed(&settings, Some(&new)).unwrap());
        assert!(!local_gateway_trust_changed(&settings, None).unwrap());
        store.write("journal.json", &journal(&new)).unwrap();
        assert!(!local_gateway_trust_changed(&settings, Some(&new.replace('\n', "\r\n"))).unwrap());
        std::fs::write(&path, &old).unwrap();
        assert!(local_gateway_trust_changed(&settings, Some(&new)).unwrap());
        std::fs::remove_file(path).unwrap();
        std::fs::remove_file(directory.join("journal.json")).unwrap();
        std::fs::remove_dir(directory).unwrap();
    }
    // Windows profile directories require installer (SYSTEM/Administrators-only) custody,
    // which an ordinary test process cannot create; the file logic is platform-neutral.
    #[cfg(unix)]
    #[test]
    fn switching_gateways_parks_and_resumes_enrollment_until_the_gateway_is_recreated() {
        #[cfg(target_os = "macos")]
        let base = PathBuf::from("/private/tmp");
        #[cfg(all(unix, not(target_os = "macos")))]
        let base = std::env::temp_dir();
        let directory = base.join(format!(
            "toolgate-profile-test-{}",
            crate::identity::nonce().unwrap()
        ));
        let store = crate::storage::ProtectedStore::open(directory.clone()).unwrap();
        let ca = |name: &str| {
            let pem = rcgen::generate_simple_self_signed(vec!["localhost".into()])
                .unwrap()
                .cert
                .pem();
            let path = directory.join(name);
            std::fs::write(&path, &pem).unwrap();
            (pem, path)
        };
        let (first_ca, first_path) = ca("first.crt");
        let first = Config {
            deployment: None,
            execution: None,
            tools: None,
            server_url: "https://localhost:18450".into(),
            state_directory: directory.clone(),
            ipc_endpoint: ipc_endpoint(),
            authorized_peers: vec!["0".into()],
            ca_certificate_path: Some(first_path),
            request_timeout_seconds: 10,
            local_console_url: Some("http://127.0.0.1:18090".into()),
        };
        let journal = |issuer: &str| {
            serde_json::to_vec(
                &serde_json::json!({"identity":{"issuerCertificatePem":issuer},"manifest":null}),
            )
            .unwrap()
        };
        store.write("journal.json", &journal(&first_ca)).unwrap();
        store.write("adoption.json", b"first").unwrap();
        store.write("device-key", b"device").unwrap();
        let second = "https://localhost:18451";
        let mut moves = Vec::new();
        archive_profile(&first, &mut moves).unwrap();
        assert!(restore_profile(&directory, second, None, &mut moves)
            .unwrap()
            .is_none());
        assert_eq!(store.read("journal.json").unwrap(), None);
        assert_eq!(store.read("adoption.json").unwrap(), None);
        // The device key is not gateway state and never moves.
        assert_eq!(store.read("device-key").unwrap().unwrap(), b"device");
        // Rolling back a failed switch restores the active gateway exactly.
        for (source, destination) in moves.iter().rev() {
            std::fs::rename(destination, source).unwrap();
        }
        assert_eq!(store.read("adoption.json").unwrap().unwrap(), b"first");
        let mut moves = Vec::new();
        archive_profile(&first, &mut moves).unwrap();
        // Switching back with the same gateway CA resumes enrollment and settings.
        let (saved, snapshot) =
            restore_profile(&directory, &first.server_url, Some(&first_ca), &mut moves)
                .unwrap()
                .unwrap();
        assert_eq!(saved.local_console_url, first.local_console_url);
        assert_eq!(saved.ca_certificate_path, first.ca_certificate_path);
        assert_eq!(store.read("adoption.json").unwrap().unwrap(), b"first");
        std::fs::remove_file(snapshot).unwrap();
        // A recreated gateway (new CA) invalidates the parked enrollment.
        archive_profile(&first, &mut Vec::new()).unwrap();
        let (recreated, _) = ca("recreated.crt");
        assert!(restore_profile(
            &directory,
            &first.server_url,
            Some(&recreated),
            &mut Vec::new()
        )
        .unwrap()
        .is_none());
        assert!(!profile_directory(&directory, &first.server_url).exists());
        assert_eq!(store.read("journal.json").unwrap(), None);
        for name in ["first.crt", "recreated.crt", "device-key"] {
            std::fs::remove_file(directory.join(name)).unwrap();
        }
        std::fs::remove_dir(directory.join("profiles")).unwrap();
        std::fs::remove_dir(directory).unwrap();
    }
}
