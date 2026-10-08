# Local debug stack

The current Clients table combines registered devices and pending requests, with device names, registered users and detail tooltips. Enable/Disable and Approve/Deapprove are independent; approval may be timed or explicitly unlimited. Quickstart separately registers the fixed executor, HotFolder and REST forwarding. Redeploy without deleting toolgate-debug_data. Download the rebuilt Windows installer for the current certificate recovery behavior; existing device approval deadlines are not automatically extended.

See [Device registry and tool-call controls](../docs/control-plane/device-registry.md).

Double-click `start.bat` to test the working tree, build Control and Quickstart
with the production Dockerfiles, smoke-test the new image, then start the console
at http://127.0.0.1:18090/console/. `restart.bat` repeats the checks and build
before replacing the container. `stop.bat` stops this stack and preserves data.
Scripts work from any current directory and never commit or push.

Run `create-devices.bat` from the repository root to create Linux Docker devices
with the current endpoint client. It prompts for a Linux device count, defaulting
to **1**; enter **0** to do nothing. Windows devices are deferred. The debug stack
must already be running and healthy. No host Python or Rust installation is
needed: Docker builds the client and its container image from the working tree.

For a noninteractive run use `create-devices.bat -Count 1`. The helper also accepts
`-LinuxCount`, `-DockerContext <name>` and `-ValidateOnly`. Validation checks the
Linux engine, debug server and container ownership without building or deploying.
The count is the number of numbered device slots to ensure, up to 32: existing
`toolgate-device-linux-001`, `002`, etc. are reused; a smaller count leaves extra
devices alone. Re-running does not revoke, approve, or replace existing identities.
Existing containers keep their image; new slots use the latest source build.

Open http://127.0.0.1:18090/console/#devices after deployment. Each new Linux client
appears in **Clients** as **Needs approval**. Compare its enrollment code and key
fingerprint with the script output, then approve it for the desired duration. The
client connects automatically after approval; the script never makes approval or
permission decisions. Pending requests expire after ten minutes and refresh
automatically while the container stays running. Explicit denial stops that
automatic refresh until the container is restarted. The server allows at most
32 simultaneous pending requests across all clients.

Each device has its own hostname, protected key and named config/state volumes.
No device ports or Docker socket are exposed. Its loopback relay forwards bytes
to Quickstart on the Docker network, preserving the advertised local HTTPS origin,
certificate validation, mTLS and WebSockets. The real client installer obtains
the CA itself. Docker supervises the foreground protected service; a narrowly
scoped `systemctl` shim handles the installer's service registration commands.
The installed client supports its normal fixed tools after the administrator
configures their permissions; these containers do not provision an OCI tool
execution engine.

Inspect a device with `docker logs toolgate-device-linux-001` or
`docker exec toolgate-device-linux-001 /usr/local/lib/olo-toolgate/olo-toolgate-client health`.
Stop it with `docker stop toolgate-device-linux-001`; `create-devices.bat -Count 1`
starts it again. `debug/stop.bat` stops the server stack only. Device identities
survive container restarts and removal while their named volumes are retained.

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

Run `setup-agent-mimic.bat` or `agent-mimic.bat` to call an already configured,
approved and connected client through `https://localhost:18450/mcp`. The setup
filename is retained as an alias for the call-and-log command. Configure the
device, agent credential and intended grants beforehand. The commands only
send the two tool calls and record their responses. Denied or unavailable calls
fail visibly.

Both commands save visible console output and errors in timestamped files under
`.dev/debug/logs/`; each run prints its log path. The **Client tool requests** page
fills the parent canvas and logs progress. Use **View response** for the completed
JSON output. Both wrappers accept `-Gateway`, `-TokenFile`, `-CaFile` and `-File`.
It calls `hotfolder.write_text`, then waits for its response before calling
`client.read_log_entry`. `hotfolder.write_text` creates
`rahul-nigam.txt` containing exactly `My Name is Rahul Nigam` in the installed
client's protected HotFolder. `client.read_log_entry` reads exactly one latest
entry from that client's real `packets.jsonl`. Both complete tool responses are
printed in the console. A denied or failed write stops the script before the
second call.

The calls use an existing device-bound agent bearer token in
`.dev/debug/client-agent-token`, readable only by the current Windows account
and SYSTEM. The server stores its hash in `/data/client-runtime-credentials.json`;
`TOOLGATE_QUICKSTART_CLIENT_CREDENTIALS=true` enables those explicitly installed
credentials for this debug stack. Credentials expire after 24 hours. The mimic
commands use the existing agent credential; enrollment, approval, permissions
and credential administration belong to the deployment's configuration.

Fresh client installation needs only the EXE downloaded from Enroll Device while
this container is running. Setup obtains the public local CA, validates HTTPS,
stores the certificate in the client's protected configuration directory, installs
the service/native bridge/tray, and starts enrollment. Approve the device in the
console; Chrome is optional when enrolling from setup or the tray menu.
`prepare-local-client.ps1` now invokes this same installer and does not export,
copy or configure a CA. The call scripts use the installed client's configured CA
for the same gateway, an explicit `-CaFile`, or system TLS trust for their agent
HTTPS connection.
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
