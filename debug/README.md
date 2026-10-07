# Local debug stack

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
separately packaged Chrome extension in `deploy/client-assets/release` is retained.
Windows/Chrome ZIPs are internal build inputs only. After installer-only releases
are published, a clean machine must obtain their internal native bundle from the
client CI `public-client-bundle` artifact in `.dev/debug/client-assets`; existing
verified build inputs are retained there. The server never offers ZIP downloads.
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

Validate Docker/Compose configuration without building:
`powershell -NoProfile -File debug/manage.ps1 start -ValidateOnly`.
Inspect failures with `docker compose -p toolgate-debug -f debug/compose.yaml logs`.
No script deletes volumes; back up `/data` before testing migrations.
