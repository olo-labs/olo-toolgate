# One-Minute Quickstart

## Purpose

Run a single-user OLO ToolGate environment with no external database, Redis, Vault, or Kubernetes.

## Requirements

- Docker 24+ or compatible container runtime.
- One free local port.

## Run

```bash
docker volume create olo-toolgate-data

docker run --rm   --name olo-toolgate   -p 8080:8080   -v olo-toolgate-data:/data   ghcr.io/olo-labs/olo-toolgate-quickstart:latest
```

Open:

```text
http://localhost:8080
```

On first boot:

- ToolGate creates embedded state.
- ToolGate creates the built-in encrypted vault.
- ToolGate creates the default single-user workspace.
- ToolGate enables the safe built-in tool pack.
- A one-time admin bootstrap credential is generated unless explicitly provided.

## Verify

The dashboard should show:

```text
Mode: Quickstart
Gateway: Ready
Control Plane: Ready
Built-in Tools: Ready
Client Downloads: Available
```

## Next

1. Change the bootstrap password.
2. Open **Tools → Built-In**.
3. Download the endpoint client.
4. Install it using only the server URL.
5. Confirm `HotFolder` is created.
6. Test `hotfolder.write_text` and `hotfolder.read_text`.

## Important

Quickstart is:

- single-node.
- non-HA.
- intended for personal/community evaluation.

Production uses separate stateless Gateway and Control Plane containers with external state.
