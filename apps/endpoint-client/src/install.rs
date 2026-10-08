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
    install_for_peer_with_ca(server, peer, None)
}
pub fn install_for_peer_with_ca(server: &str, peer: Option<&str>, ca: Option<&str>) -> Result<()> {
    install_config(server, peer, None, ca)
}
pub fn reinstall(server: &str, peer: Option<&str>) -> Result<()> {
    reinstall_with_ca(server, peer, None)
}
pub fn reinstall_with_ca(server: &str, peer: Option<&str>, ca: Option<&str>) -> Result<()> {
    admin()?;
    validate_peer(peer)?;
    #[cfg(windows)]
    if ca.is_some() {
        // Reconcile persisted enrollment too: the config may already contain a replacement CA.
        configure_with_ca(server, peer, ca)?;
    }
    let previous = Config::load(&config_path())?;
    if crate::config::origin(server)? != previous.server_url {
        return Err(Failure::Conflict);
    }
    uninstall(false)?;
    install_config(server, peer, Some(previous), ca)
}
#[cfg(not(windows))]
pub fn configure(_: &str, _: Option<&str>) -> Result<()> {
    Err(Failure::Unsupported)
}
#[cfg(not(windows))]
pub fn configure_with_ca(_: &str, _: Option<&str>, _: Option<&str>) -> Result<()> {
    Err(Failure::Unsupported)
}
#[cfg(windows)]
pub fn configure(server: &str, peer: Option<&str>) -> Result<()> {
    configure_with_ca(server, peer, None)
}
#[cfg(windows)]
pub fn configure_with_ca(server: &str, peer: Option<&str>, ca: Option<&str>) -> Result<()> {
    admin()?;
    validate_peer(peer)?;
    let server = crate::config::origin(server)?;
    let mut settings = Config::load(&config_path())?;
    let trust_changed = local_gateway_trust_changed(&settings, ca)?;
    let changed = settings.server_url != server || trust_changed;
    if changed {
        settings.server_url = server.clone();
        // These settings contain gateway destinations, pins and credentials from the old site.
        settings.tools = None;
        settings.execution = None;
        settings.deployment = None;
        settings.ca_certificate_path = None;
    }
    if let Some(peer) = peer {
        if !settings.authorized_peers.iter().any(|p| p == peer) {
            settings.authorized_peers.push(peer.to_owned());
        }
    }
    settings.validate()?;
    let path = config_path();
    let temporary = path.with_extension(format!("{}.json", crate::identity::nonce()?));
    use std::io::Write;
    let mut file = std::fs::OpenOptions::new()
        .write(true)
        .create_new(true)
        .open(&temporary)
        .map_err(|_| Failure::Conflict)?;
    file.write_all(&serde_json::to_vec_pretty(&settings).map_err(|_| Failure::Validation)?)
        .and_then(|_| file.sync_all())
        .map_err(|_| Failure::Unavailable)?;
    drop(file);
    crate::platform::windows::protect_install_acl(&temporary, true)?;
    let mut backups = Vec::new();
    let update = (|| {
        stop_windows_service()?;
        if let Some(ca) = ca {
            settings.ca_certificate_path = Some(save_ca(ca)?);
            std::fs::write(
                &temporary,
                serde_json::to_vec_pretty(&settings).map_err(|_| Failure::Validation)?,
            )
            .map_err(|_| Failure::Unavailable)?;
        }
        if changed {
            for name in [
                "journal.json",
                "permissions.json",
                "remote-journal.json",
                "fleet-intent.json",
                "fleet-active.json",
            ] {
                let source = settings.state_directory.join(name);
                if source.try_exists().map_err(|_| Failure::Unavailable)? {
                    crate::storage::check_owned(&source, true)?;
                    let backup =
                        source.with_extension(format!("{}.json", crate::identity::nonce()?));
                    std::fs::rename(&source, &backup).map_err(|_| Failure::Unavailable)?;
                    backups.push((source, backup));
                }
            }
        }
        std::fs::rename(&temporary, &path).map_err(|_| Failure::Unavailable)
    })();
    if let Err(failure) = update {
        for (source, backup) in backups.iter().rev() {
            let _ = std::fs::rename(backup, source);
        }
        let _ = std::fs::remove_file(&temporary);
        let _ = command(
            r"C:\Windows\System32\sc.exe",
            &["start", "OloToolGateClient"],
        );
        return Err(failure);
    }
    for (_, backup) in backups {
        std::fs::remove_file(backup).map_err(|_| Failure::Unavailable)?;
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
fn install_config(
    server: &str,
    peer: Option<&str>,
    previous: Option<Config>,
    ca: Option<&str>,
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
    });
    if let Some(peer) = peer {
        if !settings.authorized_peers.iter().any(|value| value == peer) {
            settings.authorized_peers.push(peer.to_owned());
        }
    }
    if let Some(ca) = ca {
        settings.ca_certificate_path = Some(save_ca(ca)?);
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
}
