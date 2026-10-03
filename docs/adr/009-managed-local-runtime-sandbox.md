# ADR 009: Managed runtimes execute in a disposable OCI sandbox

Status: accepted for Module 08, 2026-10-03.

## Context

The endpoint identity service owns privileged device credentials. Python, Node,
native binaries and shell tools must never execute in that process/host namespace.
Missing host interpreters must not require users to install them manually or
permit a fallback that bypasses isolation. Module 09 organization distribution
does not yet exist; local registration must itself remain an explicit protected
administrator trust decision, separate from online runtime authorization.

## Decision

Add a Docker Engine adapter behind an execution port. The administrator installs
and configures a local Linux OCI engine. An explicit opt-in managed runtime setting
allows first-use pulling of administrator-pinned images, with a bounded deadline;
install-time preparation uses the same manager. Image identity is an immutable
SHA-256 reference, never a mutable tag. Host PATH/interpreter availability is ignored.
The runtime image provides the interpreter plus dependencies; no pip/npm/download
command runs inside a tool invocation. Engine installation is an external
privileged operation and is never silently performed by a script from the internet.

Every tool needs protected local registration, exact tool-image digest, validated
input/output schemas and a fresh device-bound Gateway grant. No Marketplace
metadata or downloaded client alone grants execution. Inputs travel solely as
one canonical JSON stdin document; launch arguments are fixed adapter vectors.
An invocation has no host mounts, network, engine socket or inherited credentials.
Its root is read-only; writable work/tmp are bounded tmpfs; UID is non-root;
capabilities are dropped; memory/swap/PIDs/CPU/output/time are bounded. A custom
seccomp profile allows runtime threads but denies process creation and dangerous
namespace/kernel operations. Reviewed source and dependencies are baked into the
immutable tool image under `/opt/tool`, not copied or mounted from the privileged
service filesystem. Each image includes its managed interpreter. The image digest
binds both tool and runtime; a caller cannot choose an image, executable or path.

Linux-container native/Python/Node/PowerShell/Shell/Java/.NET adapters are supported
where their pinned images pass version and sandbox self-tests. Batch/CMD and WASM
have explicit capability interfaces and return UNSUPPORTED until a backend can
enforce the same boundary; Windows host execution is not a fallback. Windows/macOS
clients can use an administrator-provisioned system-accessible Linux engine;
user-session Docker Desktop availability alone does not guarantee logout operation.

Preparation occurs before requesting an execution grant so a slow initial image
pull cannot age a permit. Execution rechecks identity freshness after preparation.
Cleanup removes only randomly named owned containers; uncertainty blocks further
execution and is surfaced in runtime health. No shared/global daemon cleanup.

## Consequences

Docker is optional for identity/HotFolder; required only for managed execution.
This is the additional infrastructure decision for this module. Operators must
keep the trusted engine/kernel patched, ensure cgroup/seccomp support and constrain
daemon access. Rootless Linux engines are preferred where resource enforcement
is available. OCI isolation shares the engine kernel; high-risk code may require
a stronger VM backend. Unsupported platforms/adapters fail closed. No automatic
system-wide Python installation, host dependency pollution or Module 09 work occurs.

References: [Docker run controls](https://docs.docker.com/reference/cli/docker/container/run/),
[seccomp](https://docs.docker.com/engine/security/seccomp/),
[engine trust](https://docs.docker.com/engine/security/).
