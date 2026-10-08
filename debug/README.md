# Local debug stack

The current Clients table combines registered devices and pending requests, with device names, registered users and detail tooltips. Enable/Disable and Approve/Deapprove are independent; approval may be timed or explicitly unlimited. Quickstart separately registers the fixed executor, HotFolder and REST forwarding. Redeploy without deleting toolgate-debug_data. Download the rebuilt Windows installer for the current certificate recovery behavior; existing device approval deadlines are not automatically extended.

See [Device registry and tool-call controls](../docs/control-plane/device-registry.md).

Double-click `start.bat` to test the working tree, build Control and Quickstart
with the production Dockerfiles, smoke-test the new image, then start the console
at http://127.0.0.1:18090/console/. `restart.bat` repeats the checks and build
before replacing the container. `stop.bat` stops this stack and preserves data.
Scripts work from any current directory and never commit or push.

Requires Docker Desktop running Linux containers, Git, Node.js/npm, Python 3.11+
and internet access for dependencies and base images. A Python environment is
created under `.dev/debug/venv`. The matching Playwright Chromium browser is
installed automatically after npm dependencies. Native archives and one-click
installers are fetched from the latest published GitHub release matching VERSION,
verified by the canonical manifest tool, and included in the local image. The
Chrome extension is packaged from the working tree. Compatible Windows native
inputs can be supplied from the client CI `public-client-bundle` artifact in
`.dev/debug/client-assets`; downloads never overwrite those inputs. Older Windows
archives without the Chrome native host are excluded. If a compatible x64 Windows
archive is unavailable, Docker builds the current client and native host together
for `x86_64-pc-windows-gnu`, then creates the combined EXE with Inno Setup. The first
build installs the pinned Rust toolchain and MinGW compiler in a cached Docker
image; subsequent builds reuse Cargo's cache. GNU debug builds retain their actual
target name. Windows ARM64 downloads require compatible native CI inputs.
Windows/Chrome ZIPs are internal build inputs only; the server offers the EXE.
Errors stop deployment and remain visible in the console; failed checks leave
the running debug stack untouched.

The default gates cover generated contracts, CI regression tests, headers,
contract validation, UI types/unit tests, Chrome extension tests, Windows and
Linux download browser tests, and real Quickstart boot/ASK/enrollment/persistence
smoke tests. Build uses current files including uncommitted changes.

For the broader foundation and security scan gates run `start.bat -FullCheck`
(or `restart.bat -FullCheck`). Install the toolchains used by `tools/check.py`:
Java 21, Gradle, Rust 1.94.1, PHP and Helm, and make them available on PATH.
Cross-platform installer builds, signing, release publication, hosted CI and
Kubernetes jobs still run in their respective CI environments; these scripts
do not replace every workflow job.

This is a separate Compose project (`toolgate-debug`) using the local
`olo-toolgate-quickstart:debug` image and its own persistent volume. It does not
pull published ToolGate images or stop the stack on port 18089. Admin password
login is disabled by default for this local debug stack, using the default admin
identity so user-specific flows continue to work. To test password login, set
`TOOLGATE_DISABLE_ADMIN_PASSWORD=false` in your environment before launching a
script, then read bootstrap credentials using
`docker compose -p toolgate-debug -f debug/compose.yaml logs quickstart`.
Ports bind only to localhost.
The public endpoint configuration advertises `https://localhost:18450`, matching
the debug TLS port mapping. Enroll Device displays this address beside the downloads
with a copy button. The Windows installer's optional Gateway URL defaults to
`https://localhost:18450` for a new installation; an existing installation keeps its current gateway
unless changed. Client enrollment still needs trust for Quickstart's
public CA, as described in the Quickstart guide; certificate validation stays on.
For an unpublished extension, click Connect and follow its Load unpacked steps.

Validate Docker/Compose configuration without building:
`powershell -NoProfile -File debug/manage.ps1 start -ValidateOnly`.
Inspect failures with `docker compose -p toolgate-debug -f debug/compose.yaml logs`.
No script deletes volumes; back up `/data` before testing migrations.

For first-time Windows setup, run `setup-agent-mimic.bat` and approve the Windows
administrator prompt. It exports this debug stack's public CA, repairs the native
service, enrolls the device after matching its real key fingerprint, installs the
two exact tool grants, waits for Gateway readiness, and runs the mimic requests.
Only this local password-free debug stack supports the automatic approval helper.
The repair waits for the installer process to finish. Retrying resumes an already
completed repair when the installed binaries and Gateway CA match this build.

After setup, run `agent-mimic.bat` to mimic an agent through `https://localhost:18450/mcp`.
Both setup and mimic commands also save their visible console output, including
errors, in timestamped files under `.dev/debug/logs/`. Each run prints its log path.
After a Gateway restart, setup waits for a new authenticated client check-in and
prints progress; the client's outage retry interval can reach five minutes.
It calls `server/discover`, then `tools/list`, then waits for each client tool to
finish before sending the next call. `hotfolder.write_text` creates
`rahul-nigam.txt` containing exactly `My Name is Rahul Nigam` in the installed
client's protected HotFolder. `client.read_log_entry` reads exactly one latest
entry from that client's real `packets.jsonl` and prints it in the console.
An empty catalog or failed write stops the script before the next effect.

The local setup uses a device-bound agent bearer token in
`.dev/debug/client-agent-token`, readable only by the current Windows account
and SYSTEM. The server stores its hash in `/data/client-runtime-credentials.json`;
`TOOLGATE_QUICKSTART_CLIENT_CREDENTIALS=true` enables those explicitly installed
credentials for this debug stack. Credentials expire after 24 hours. Refresh with
`.dev/debug/venv/Scripts/python.exe debug/enroll-mimic-client.py`; it preserves the
existing enrollment using `.dev/debug/mimic-client.json`, installs only the exact
file and diagnostic tool grants for `debug-mimic-agent`, publishes them, and
restarts only this Compose project. This helper requires the debug stack's
configured password-free administrator login. The mimic script itself uses the
agent credential and never an administrator credential.

Fresh client installation needs only the EXE downloaded from Enroll Device while
this container is running. Setup obtains the public local CA, validates HTTPS,
stores the certificate in the client's protected configuration directory, installs
the service/native bridge/tray, and starts enrollment. Approve the device in the
console; Chrome is optional when enrolling from setup or the tray menu.
`prepare-local-client.ps1` now invokes this same installer and does not export,
copy or configure a CA. The mimic helper still saves a public CA for its own agent
HTTPS connection; it does not supply client installation configuration.
The Gateway default remains `https://localhost:18450`; the mimic PowerShell wrapper
also accepts `-Gateway`, `-TokenFile`, `-CaFile`, and `-File` for an already configured
deployment.

Right-click the tray icon and choose **View messages sent / received** for the
live client log (Windows administrator access is required for the protected
state folder). It retains at most 100 entries / 64 KiB. Server packet diagnostics
are available with `docker compose -p toolgate-debug -f debug/compose.yaml logs -f quickstart`;
look for `protocol_packet`, `SEND`, and `RECEIVE`. Messages include the route,
request IDs, status, poll sequence, configuration digest/counts and tool/job state.
Enrollment codes, certificates, leases, credentials, tool arguments and outputs
are redacted. Client `peerRequestId` matches the Control server's `requestId`.

Current clients poll task and configuration changes every 500 ms while healthy.
Receiving a job opens a direct mTLS WebSocket; job authorization, results and
polls use that channel until 30 seconds after the last job finishes. Ping/pong
frames run every five seconds. Packet logs include `CONNECTED`, `PING`, `PONG`,
`PEER_PING`, `IDLE_DISCONNECTED` and `HTTP_FALLBACK`; server socket packets have
`transport: WEBSOCKET`. A lost socket falls back to the durable HTTPS routes.

The automated native Linux regression runs with
`python tools/client/mcp_smoke.py --image olo-toolgate-quickstart:debug --binary <current-linux-client>`.
It owns only disposable containers and volumes, enrolls with a real CSR and mTLS
identity, invokes this same mimic script through the HTTPS MCP route, verifies
the file on the client, checks both requests reached DONE, and verifies packet
logs contain SEND/RECEIVE without the credential, enrollment code or file text.
It also verifies a fresh client install obtains its own CA, measures the 500 ms
poll cadence, rejects sockets without device identity, and checks bidirectional
heartbeats and idle disconnect. Results are written to `build/quickstart/mcp-smoke.json`.
