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
        #[cfg(windows)]
        let builder = builder;
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
    admin()?;
    let origin = crate::config::origin(server)?;
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
    let settings = Config {
        deployment: None,
        execution: None,
        tools: None,
        server_url: origin,
        state_directory: state,
        ipc_endpoint: ipc_endpoint(),
        authorized_peers: peers,
        ca_certificate_path: None,
        request_timeout_seconds: 10,
    };
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
        command(
            r"C:\Windows\System32\sc.exe",
            &["stop", "OloToolGateClient"],
        )?;
        let manager = windows_service::service_manager::ServiceManager::local_computer(
            None::<&str>,
            windows_service::service_manager::ServiceManagerAccess::CONNECT,
        )
        .map_err(|_| Failure::Unavailable)?;
        let service = manager
            .open_service(
                "OloToolGateClient",
                windows_service::service::ServiceAccess::QUERY_STATUS,
            )
            .map_err(|_| Failure::Unavailable)?;
        let deadline = std::time::Instant::now() + std::time::Duration::from_secs(30);
        while service
            .query_status()
            .map_err(|_| Failure::Unavailable)?
            .current_state
            != windows_service::service::ServiceState::Stopped
        {
            if std::time::Instant::now() >= deadline {
                return Err(Failure::Unavailable);
            }
            std::thread::sleep(std::time::Duration::from_millis(100));
        }
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
        for name in ["journal.json", "device-key", "service.lock"] {
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
