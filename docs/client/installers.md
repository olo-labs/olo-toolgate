# Unsigned client installers

Downloads are available on the Control home page without logging in. Choose your
OS and CPU architecture. Installer and archive SHA-256 values are shown beside
their links. These development installers are unsigned; signing and notarization
are separate release steps. An installer cannot remove SmartScreen or Gatekeeper
warnings. Do not disable machine-wide security protections.

Windows: open the `.setup.exe`, approve UAC, and enter your organization's HTTPS
Control server address. Setup installs the Windows LocalSystem service with
automatic startup and registers an entry in Installed Apps for uninstallation.

macOS: open the `.dmg`, then `Install ToolGate.app`. Enter your HTTPS Control server
and approve administrator authentication. A system LaunchDaemon runs independently
of user login. The app includes the client, licenses, dependency notices and guides.
Gatekeeper may block unsigned builds; managed administrators must approve their use
under their organization's policy. Installer signing/notarization is deferred.

Linux: the self-contained `.run` uses the native systemd service installer. On a
desktop with Zenity and PolicyKit it asks for the server and administrator approval.
Without a graphical frontend it prompts in a terminal and uses sudo. Browser
downloads do not preserve executable permission: use file properties to mark it
executable, or run `chmod +x ./olo-toolgate-client-*.run`, then open/run it. This
permission step depends on the desktop; universal unattended browser-to-install
behavior is not supported. Linux requires systemd; this is not a .deb/.rpm package.

Installation is initial-install only and rejects an existing configured client.
To replace an existing installation, uninstall first; enrollment custody is retained
by default. Do not use `--purge` unless you intend to remove enrollment state.
Use the installed client `uninstall` command as administrator on Linux/macOS;
Windows Installed Apps invokes this command. Installation does not authorize tools
or enroll a device. Complete enrollment using the [endpoint guide](../../apps/endpoint-client/README.md).
The service continues after locking the screen or logging out, and protected calls
block if Gateway authorization is unavailable.

Diagnostics: inspect Windows Services/Event Viewer, `journalctl -u
olo-toolgate-client` on Linux, or `sudo launchctl print
system/io.ololabs.toolgate.client` on macOS. Run the installed client `health`.
See [production/debugging](../operations/debugging.md) for TLS and enrollment errors.

Build with the existing tested native archive in `build/client/release`:
`python tools/client/installer.py --target <native-target>`. Windows requires Inno
Setup 6.7.1 (`CLIENT_ISCC_PATH` can select its compiler); macOS uses osacompile and
hdiutil; Linux uses Python standard libraries. Native CI builds all six target
installers after service tests. The public bundle includes `installers.json` and
per-installer checksums, and development releases reuse those exact artifacts.
