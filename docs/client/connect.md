<!-- Copyright 2026 OLO Labs; SPDX-License-Identifier: Apache-2.0 -->
# Chrome Connect

In the Admin console, open Devices → Enroll device and click **Connect**. Chrome on Windows is currently supported. Other browsers and operating systems use the existing client downloads.

1. Install or update ToolGate Connect through the configured Chrome Web Store link. Chrome requires your approval. Until a listing exists, development builds provide a ZIP: extract it, open `chrome://extensions`, enable Developer mode and Load unpacked. Reload the console after installing/upgrading.
2. Open ToolGate Connect from Chrome's Extensions menu. Click **Approve this site and connect**. Permission is restricted to this console origin. The extension first tries the installed client's native messaging host.
3. Select x64 or ARM64 before approving. If the client is unavailable, the extension automatically downloads the verified installer. Keep the popup open during checksum verification. Use **Download verified installer** to retry. After completion click **Open installer**. Chrome/Windows approval, the license and UAC are required; the browser cannot silently elevate a new installer.
4. Enter your organization's **HTTPS Control server address** in the installer. The console HTTP address can differ from the enrollment HTTPS address. Install only on a trusted computer. Unsigned development builds may trigger SmartScreen.
5. Open the extension and click **Start enrollment**. Review the code and fingerprint in the console and explicitly approve enrollment. Connect never auto-approves a device. The console waits up to ten minutes for that exact device and fingerprint to have a fresh authenticated server check-in.

**Cancel Connect** stops console polling and cancels this extension's active download. Close an already launched Windows installer separately. Retry restarts the bounded checks. Navigating away stops console polling. Installation runs a system service, which continues after logout; Chrome is only needed for setup.

## Publishing and configuration

The endpoint-client workflow tests and packages a deterministic Manifest V3 ZIP, checksum, license, notices, release notes and this guide in `public-client-bundle`. Development release preparation validates and carries these files into GitHub release assets and the client asset directory used by containers. `extension.json` selects the exact supported protocol, Chrome version and package checksum. The public endpoint is `/api/public/v1/clients/extension`.

Chrome's numeric version is the repository version's major/minor/patch plus the full-history Git commit count (0..65535); `version_name` retains the full repository version. This gives the same commit the same extension version across client and Quickstart workflows, with increasing versions on main. The public key in `apps/browser-extension/identity.json` fixes the extension ID; Windows' native host permits only that ID. It is public material, not a signing credential. This is currently a development identity. Before enabling Web Store distribution, obtain the public key and item ID from the Chrome Developer Dashboard, update identity.json and the native-host, installer, console and Control allowlists together, then rebuild the client and extension. The identity tests check these agree. After adopting the store identity, set repository Actions variable `TOOLGATE_CHROME_STORE_URL` to `https://chromewebstore.google.com/detail/<extension-id>` **only after that listing is published**. The workflow publishes a ZIP to release assets; it does not publish to the Web Store. Store submission credentials and review are a separate deployment prerequisite.

The Windows installer installs and registers the native messaging host in HKLM. Its only operations are health and enrollment, through the existing OS-authenticated IPC. It cannot execute tools, change the server, read secrets, install software or elevate itself. A local health response confirms service presence, not authorization to this organization. The existing fingerprint confirmation and Control check-in provide that binding.

## Debugging

- No extension detected: confirm Chrome on Windows, reload both the extension and console, then Retry Connect. Check the exact Chrome version shown in the console.
- Permission denied: approve this specific console origin from the extension popup. No administrator session token is sent to the extension.
- Missing native host: install the latest Windows installer; archive-only installations do not register Chrome. Inspect `HKLM\Software\Google\Chrome\NativeMessagingHosts\io.ololabs.toolgate.connect` and the registered manifest's allowed origin. Do not broaden it to other extensions.
- Service unavailable: inspect the ToolGate Windows service and existing client logs. The browser account must be in the client's OS peer allowlist. Installing using another administrator's credentials does not grant the browser account IPC access. Have the administrator provision the correct Windows SID; do not make the pipe public.
- Download or checksum failure: verify public installer assets are mounted and belong to the same release. A failure never opens an installer. Browser downloads retain normal security warnings.
- Waiting for check-in: verify the HTTPS Control address, certificate trust, enrollment confirmation and network reachability. A revoked device, stale heartbeat or Control outage never produces a Connected result.

The native host's IPC deadline is 20 seconds, page bridge deadline 25 seconds, and console flow ten minutes. Extension progress is stored per console origin and survives service-worker restarts. No secrets are stored in browser storage.
