# Unsigned client installers

Downloads are available on the Control home page without logging in. Choose your
OS and CPU architecture. Installer and archive SHA-256 values are shown beside
their links. These development installers are unsigned; signing and notarization
are separate release steps. An installer cannot remove SmartScreen or Gatekeeper
warnings. Do not disable machine-wide security protections.

Windows: download the `.setup.exe` from your ToolGate page and approve UAC.
Setup has an optional **Gateway URL** field. For a new installation it defaults to
the gateway carried by the console download link, falling back to `https://localhost:18450`; leave it unchanged for a gateway running locally, or paste
the current **Gateway URL** shown alongside the downloads on Enroll Device. No
credentials are required. Setup installs the Windows LocalSystem service with
automatic startup and registers an entry in Installed Apps for uninstallation.
Setup configures local quickstart CA trust automatically in `C:\ProgramData\OLO\ToolGate\`, verifies HTTPS, and starts enrollment in the console. Approve the device code and fingerprint there. The tray also has **Enroll this device**, so Chrome is optional for enrollment. No debug helper or manual CA export is needed for client installation.
It also installs the Chrome extension files, protected native messaging bridge and a tray icon. The tray starts at login and shows connection and enrollment status. Use Enroll device Ã¢â€ â€™
Connect in the Admin console and follow the [Chrome Connect guide](connect.md).

Installer downloads use stable names, such as
`olo-toolgate-client-x86_64-pc-windows-msvc.setup.exe`, across releases. The public
download API resolves that name to the current validated installer and sends
`Cache-Control: no-store`; versioned files and manifests remain immutable for
checksum verification. GitHub releases also include these stable installer names.
In Connect, **Install Chrome extension and client** downloads the same Windows setup EXE, which
contains the client, native bridge and unpacked Chrome extension. There is no
separate ZIP download. Open chrome://extensions, enable Developer mode and choose
Load unpacked from `Program Files\OLO\ToolGateSetup\chrome-extension`.
Chrome still requires the user's extension approval. Windows client ZIP archives
are internal CI build inputs and are not public installation downloads.

The configured Windows download endpoint is `/api/public/v1/clients/setup/<target>`.
It uses the server-owned `toolgate.control.endpoint.control-url` setting and carries
that public HTTPS origin as a hex suffix in the suggested EXE filename for interactive and unattended
installation. The hint is limited to 90 ASCII characters to
fit Windows filename limits. The EXE bytes and checksums remain identical to the
versioned release. Plain release EXEs can use `/SERVER=<HTTPS origin>` for managed
silent installation. Interactive setup uses the optional field; the configured download prefills it, then repair falls back to
the existing gateway. A blank field uses `https://localhost:18450`. Both setup modes use
`/SERVER=`, then the download hint, then the existing gateway, then the localhost
default. A loopback HTTP console address is resolved to its published HTTPS gateway
before changing the service. Client communication remains HTTPS. The local console
must be running for this resolution; setup offers a retry if it is unavailable.

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

Windows setup detects an existing client and offers repair/reinstall or uninstall.
Repair retains configuration and enrollment for the same gateway. Connect from another
ToolGate page switches gateways and starts a new enrollment. Linux/macOS installs
remain initial-install only. Enrollment keys are retained on uninstall by default. Do not use `--purge` unless you intend to remove enrollment state.
If a fresh debug data volume replaces the local gateway CA, Windows setup or Retry Connect
verifies the new gateway and clears the old enrollment and gateway-specific settings,
even if the public config already contains the new CA. It preserves the device key and
starts a new enrollment. Removing a container while retaining its data volume preserves
the gateway CA and enrollment. The Clients list refreshes automatically after enrollment.
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
