<!-- Copyright 2026 OLO Labs -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# LangChain agents using real ToolGate devices

This example starts the published `ololab/olo-toolgate-quickstart` container and a
real Linux endpoint client. A LangChain AI agent discovers its permitted tools,
reads a file **on the selected device through ToolGate**, analyzes it with a model,
and writes a Markdown report back to that device's HotFolder. Linux and prepared,
approved Windows devices share `ReadAndWriteDeviceGroup`. Each startup selects
one available member; `-Win` restricts selection to Windows.

The AI container has no device filesystem mount, Docker socket or shell tool.
Its file reads and writes use Gateway MCP calls. The endpoint authenticates with
its enrolled mTLS identity and consumes the current authorization before effects.
The console records the requests; the endpoint records its actual activity.

## What it does

| Use case | Device input | AI work | Generated device file |
| --- | --- | --- | --- |
| `tickets` (default) | `support-tickets.json` | Prioritize a support queue by customer impact and propose next steps | `support-triage.md` |
| `operations` | `service-events.json` | Produce an incident timeline, recovery evidence and handoff questions | `operations-summary.md` |
| `inventory` | `asset-inventory.csv` | Flag old patches and failed/unknown backups; prioritize investigation | `inventory-review.md` |
| `-Smoke` / `--smoke` | `support-tickets.json` | Test real LangChain tool discovery, reads, native write/readback and activity; **no AI model call** | `langchain-smoke.txt` |

The bundled business inputs are **fictional demonstration data**. They show real
workflows, rather than claiming to be your machine's service logs or asset inventory.
The read/write operations and device activity are real. Startup seeds missing inputs;
it preserves existing inputs and reports. Each AI run replaces its selected report.

The runner lists the HotFolder, reads the selected input, asks the model to write
its report, and reads the report back. It reports success only if a write occurred
in this run and the actual device readback matches. The runner restricts reads
to the selected input/report and writes to that report. The model sees those exact
relative paths in its tool schemas and can correct a rejected path; a rejected
path never reaches the Gateway. ToolGate also evaluates
current group grants, policy, device approval and installed package evidence.

## Requirements

- Docker with Linux containers and Docker Compose v2.24 or newer.
- Python 3.11+ on the host for the setup helper. Host Python uses only the standard
  library; LangChain and its dependencies are installed inside the AI image.
- Internet for pulling images and Python dependencies.
- An OpenRouter API key for an AI run, including the free model. `-Smoke` needs no
  model key or API spend.
- For Windows targeting, an installed client enrolled and approved on this
  example's Gateway, on the machine running Docker.

The checked-in image tag is a specific published development release. Both the
Quickstart and Linux client are taken from that image; the native archive checksum
and architecture are verified before extraction. No Rust compilation is required.
To use another compatible release, change `TOOLGATE_QUICKSTART_IMAGE` in `.env`.
An image without its Linux client archive fails the build.

## Windows: start everything and run the example

From a terminal:

```bat
cd D:\git\olo-toolgate\examples\langchain
copy .env.example .env
```

Create a key at [OpenRouter API keys](https://openrouter.ai/settings/keys), then
edit `.env` locally:

```dotenv
OPENROUTER_API_KEY=your-openrouter-key
OPENROUTER_MODEL=openrouter/free
```

The runner uses the dedicated `ChatOpenRouter` integration at
`https://openrouter.ai/api/v1`. Only the AI container receives the model key.
The default [free models router](https://openrouter.ai/openrouter/free) selects
an available free model that supports the requested tools. Free models still
require an OpenRouter account/key and are subject to rate limits and availability.
The selected device input and generated report are sent through OpenRouter to
the selected model provider during an AI run. These fixtures are fictional.
For a specific free model, set `OPENROUTER_MODEL` to its full `author/model:free`
ID from the [tool-capable model catalog](https://openrouter.ai/models?supported_parameters=tools).
An existing `.env` is preserved; add these two settings when upgrading this example.

Run:

```bat
start.bat
start.bat -UseCase operations
start.bat -UseCase inventory
start.bat -Win
start.bat -Target linux
```

Without a model key, test the actual ToolGate/device path first:

```bat
start.bat -Smoke
```

Image builds and pulls retry up to three times. A Docker Hub authentication
`500`/`504` occurs before ToolGate starts; wait and retry if the registry remains
unavailable. After the images have built successfully, `start.bat -Smoke -SkipBuild`
uses the existing local images without rebuilding. Omit `-SkipBuild` after code changes.

`start.bat` works from another directory too. It calls `start.ps1`, which:

1. Loads `.env` (copies `.env.example` if absent) and checks the required settings.
2. Builds the Linux client wrapper and LangChain runner images.
3. Starts Quickstart and the Linux device, waiting for readiness.
4. Logs in through the normal admin and two independent reviewer APIs. On a fresh
   isolated stack it uses the one-time installation passwords, changes them to
   unique values, and saves them privately in `.state/console-passwords.json`.
5. Checks the Compose Linux device's actual enrollment code/fingerprint and approves
   it for 24 hours. Adds Linux and the registered, approved Windows devices to
   `ReadAndWriteDeviceGroup`, preserving their other memberships. Selects an
   available group member (Windows only with `-Win`, Linux only with `-Target linux`),
   registers its real tool profiles/package digest, and maps tools/agent to the
   standard ReadAndWrite groups through independent reviews.
6. Issues a 24-hour opaque agent credential for the selected device, restarts only this
   example's Quickstart to load it, then runs the selected LangChain task.

The bootstrap/reviewer automation is an **explicit local evaluation fixture**.
The configuration still uses distinct requester/reviewer identities and the real
review transitions. It is not a substitute for separate human reviewers in an
organization. No private signing keys are read, default groups gain no rights,
and direct entity permissions are not introduced.

For an existing installation whose passwords were changed elsewhere, put its
admin/reviewer credentials in your private `.env`. For manual independent review,
run `python manage.py setup --approve-linux-device` without `--bootstrap-local`;
provide the existing admin password, leave reviewer password variables unset,
and approve the submitted drafts using independent console sessions.

## Linux/macOS host: run the same Compose stack

```bash
cd examples/langchain
cp .env.example .env
# Edit .env. Set LOCAL_UID and LOCAL_GID to `id -u` and `id -g` for private file access.
docker compose build linux-device agent
docker compose up -d --wait --wait-timeout 180 quickstart linux-device
python3 manage.py setup --bootstrap-local --approve-linux-device
docker compose run --rm -T agent --smoke
# With OPENROUTER_API_KEY configured in .env:
docker compose run --rm -T agent --use-case tickets
```

Changing host ports is supported: update `TOOLGATE_HTTP_PORT` and
`TOOLGATE_TLS_PORT` in `.env`. All published ports bind to loopback. The endpoint
and runner use byte-preserving internal loopback relays so the Quickstart's
`localhost` certificate is validated; TLS verification stays enabled.

## Where to open it and find the generated files

Default console: [http://127.0.0.1:18091/console/](http://127.0.0.1:18091/console/).
Gateway: `https://localhost:18451`. This is a separate Compose project and data
volume from `debug/start.bat` (18090/18450).

The first-run console passwords are in your private `.state/console-passwords.json`.
Open that local file when you need to sign in; it is excluded from Git and the
AI container. In the console, open **Devices → Clients** for connection state
and **Tools → Client tool requests** to inspect real requests/responses.

| Target | Report location |
| --- | --- |
| Compose Linux device | `/var/lib/olo-toolgate/hotfolder/<report-name>` **inside `linux-device`** |
| Linux persistence | Named volume `toolgate-langchain_device-state` holds that HotFolder across restarts/container recreation |
| Windows, prepared example with default state directory | `C:\ProgramData\OLO\ToolGate\state\hotfolder\<report-name>` on the **selected Windows device** |
| Windows with a custom HotFolder | The `tools.hotfolder.root` value in that device's protected `client.json`; `prepare-windows.ps1` prints the actual path |

The reports are **not written into the AI container or automatically into your
repository**. To inspect/copy a Linux report from this example directory:

```bash
docker compose exec linux-device cat /var/lib/olo-toolgate/hotfolder/support-triage.md
docker compose cp linux-device:/var/lib/olo-toolgate/hotfolder/support-triage.md ./.state/support-triage.md
```

Substitute `operations-summary.md`, `inventory-review.md`, or `langchain-smoke.txt`
for the other runs. The copied file appears in
`examples/langchain/.state/` on your host. That directory is ignored by Git.
For Windows, read the report directly from its HotFolder using an authorized
account, or inspect the report-read response in Client tool requests.

Device activity is persisted in `activity.json` and `packets.jsonl` in the device's
state directory (`/var/lib/olo-toolgate` on Linux; normally
`C:\ProgramData\OLO\ToolGate\state` on Windows). The `client.read_log_entry` tool reads
one latest redacted packet entry. Windows also has tray **Show status**, **Activity
log**, and **About** for command progress, recent activity and version information.

## Add/select a Windows device

1. Start this stack first (`start.bat -Smoke` is sufficient).
2. Open its console and click **Connect** (or download the Windows setup from
   it). For a local stack, setup is pointed at the console address
   `http://127.0.0.1:18091`, which publishes the gateway's certificate, and it
   connects the client to `https://localhost:18451`. If you type an address in
   setup, enter that console address, not the HTTPS gateway URL. Approve the
   code/fingerprint and choose `ReadAndWriteDeviceGroup`. Confirm the device is
   enabled and connected.

   Already enrolled to another gateway, such as the debug stack? Use the tray's
   **Switch gateway** menu. Each gateway keeps its own enrollment, so switching
   back needs no new approval. If this stack is recreated with a fresh volume,
   the tray offers **Repair gateway connection**, and the Linux device container
   re-enrolls by itself.
3. In an Administrator PowerShell on that Windows device, run:

   ```powershell
   cd D:\git\olo-toolgate\examples\langchain
   .\prepare-windows.ps1
   ```

   This explicitly enables the four **already compiled** example tools in the
   protected client configuration, restarts its service, seeds missing fictional
   input files, and exports public installed profiles to
   `.state/windows-profiles.json` and a public selection registration in
   `.state/devices/<device-id>.json`. It preserves enrollment and other tool profiles;
   it does not install/replace the executable or switch gateways.

4. Run:

   ```bat
   start.bat -Win -Smoke
   start.bat -Win -UseCase tickets
   ```

   Setup adds both registered devices to `ReadAndWriteDeviceGroup` through review.
   No device ID is needed for a run. For multiple Windows devices, copy each
   `.state/devices/<device-id>.json` export into this host's `.state/devices/`
   directory. These exports contain the ID, OS, Gateway URL and actual public
   profiles, never enrollment keys or agent credentials. Alternatively import
   one with `-Profiles <registration.json>` or `TOOLGATE_WINDOWS_PROFILES` in `.env`.
   An export from a different Gateway is rejected.

   `start.bat` picks any eligible Linux or Windows member; `-Win` / `-Target windows`
   filters to Windows, and `-Target linux` filters to Linux. `-DeviceId <id>`
   optionally pins a particular eligible member. Membership alone is insufficient:
   the device must be enabled, approved, have unexpired connection approval, and
   have checked in within 120 seconds. Missing public registrations are excluded.
   If no eligible device matches, startup fails with a clear explanation.

`-Win` executes tools on the selected Windows device. The Linux container still
runs as the example's local TLS relay for the agent container.

Selection occurs once at startup, before the AI runs; reads and writes in that
run stay on the same device, with the selected ID/group printed in the console.
If it goes offline during execution, the run fails; writes are not replayed on
another device. Rerun startup to select an available member again.
After its Gateway restart, setup waits for a fresh check-in before starting the
agent. Existing clients can take up to five minutes to reconnect because of retry
backoff; setup allows six minutes and preserves their service/enrollment.

The Gateway credential fixes the target device; the AI cannot supply another
actor or device identity in its tool arguments. Setup selects the real Windows
profiles and pins their package bytes through reviewed changes.

**Current product limitation:** a built-in tool ID has one current registered
package/profile in this tenant. Windows and Linux executable hashes differ, so
this example selects **one active device build at a time**. Selecting Windows can
remove the matching Linux catalog until you run `start.bat -Target linux -Smoke` (or Linux setup)
again. Credentials remain device-bound; they do not bypass package matching. This
example does not claim simultaneous mixed-build dispatch under the same tool IDs.

## Customize the inputs

Replace the three fictional inputs in the selected device's HotFolder with your
own matching JSON/CSV structure, then rerun its use case. The AI reads them through
ToolGate; startup does not overwrite existing files. Keep inputs under the client's
64 KiB default file limit. The smoke check expects the bundled support fixture.

Agent and device credentials expire after 24 hours. Rerun setup to renew the agent
credential through review; extend expired device approval in the console. The
application does not auto-approve unrelated or Windows devices.

## Stop, troubleshoot and verify

```bash
docker compose stop
docker compose logs --tail 50 quickstart linux-device
python manage.py status
```

`stop` retains both the device identity and files. `docker compose down` removes
containers/network but also retains named volumes. Keep the volumes to reuse
enrollment and generated reports.

- **No model key:** use `start.bat -Smoke`, or configure `.env` before an AI run.
- **OpenRouter HTTP 401:** check the OpenRouter key in `OPENROUTER_API_KEY`.
- **OpenRouter HTTP 429:** the free-model quota/rate limit may be exhausted;
  wait for the limit to reset before retrying. `-Smoke` still tests real device
  operations without model calls.
- **OpenRouter HTTP 402:** check that `OPENROUTER_MODEL` selects `openrouter/free`
  or a free variant. Paid models require OpenRouter credits.
- **No tool-capable provider:** select an available model that supports tool calls
  or retry when a free provider becomes available. The runner requires providers
  to support its requested parameters.
- **HTTP 401:** the target agent token is missing, expired, or not loaded; rerun setup.
- **Empty catalog / HTTP 403:** check approval, enablement, current group mappings,
  selected installed profiles and package pins. Device installation alone grants no access.
- **Approval pending:** log in as the independent reviewers named by setup and
  review the draft. The requester cannot approve its own change.
- **TLS failure:** retain the stack's data volume and its current exported public CA.
  Never disable certificate validation.
- **Windows report absent:** verify you selected the correct device and actual
  configured HotFolder. The runner refuses to claim success without matching readback.

The [LangChain agent API](https://docs.langchain.com/oss/python/langchain/agents)
provides the model/tool loop; the [OpenRouter integration](https://docs.langchain.com/oss/python/integrations/chat/openrouter)
provides model access. The [ToolGate initial configuration guide](../../config/initial/README.md)
explains standard groups, grants and independent review; the
[client guide](../../docs/client/README.md) covers endpoint enforcement.
