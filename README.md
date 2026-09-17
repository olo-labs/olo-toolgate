# OLO ToolGate

> **Control what AI can do.**

OLO ToolGate is an open-source control plane for **building, distributing, authorizing, and governing AI tools** across users, agents, devices, and resources.

MCP is part of the story — not the limit.

ToolGate is designed to govern:

```text
MCP servers
REST / OpenAPI tools
Python / JavaScript
PowerShell / Batch / Shell
Java / .NET
Native binaries
Local device tools
Privileged remote operations
```

all through one security model:

```text
WHO + AGENT + DEVICE + TOOL + ACTION + RESOURCE
                        |
                        v
                ALLOW / ASK / BLOCK
```

## Why ToolGate?

AI can already call tools.

The missing layer is:

> **Who allowed this agent to do this action, on this device, against this resource — and can we stop, approve, audit, update, or revoke it?**

ToolGate aims to make that answer simple.

### What we are building

```text
                    COMMUNITY
                  Tool Marketplace
                        |
                        v
               Organization Admin
                        |
              review / configure
                        |
                        v
                 ToolGate Control
                        |
              deploy / authorize
                        |
         +--------------+--------------+
         |              |              |
       Windows         Linux          macOS
         |              |              |
      Python         Binary         PowerShell
      Node.js        Java           Shell
         |              |              |
         +--------------+--------------+
                        |
                        v
               EVERY TOOL CALL
                        |
                        v
                ToolGate Gateway
                        |
                ALLOW / ASK / BLOCK
```

## The 30-second version

ToolGate is trying to become:

- **Cloudflare Access for AI tools**
- **an App Store for governed AI capabilities**
- **a fleet manager for local AI tools**
- **a policy gateway for every protected action**

without forcing teams to understand MCP internals, policy languages, or complex infrastructure.

## Quickstart vision

The goal is a one-command personal/community experience:

```bash
docker run --rm \
  --name olo-toolgate \
  -p 8080:8080 \
  -v olo-toolgate-data:/data \
  ghcr.io/olo-labs/olo-toolgate-quickstart:latest
```

Then open:

```text
http://localhost:8080
```

and immediately get:

- a single-user admin workspace
- safe built-in tools
- an encrypted local credential vault
- a downloadable endpoint client
- a managed `HotFolder`
- `ALLOW / ASK / BLOCK`
- audit history
- JSON/YAML import/export

> **Project status:** early-stage / pre-alpha. The architecture is defined and implementation is being built. The quickstart command above represents the target release experience and may not be available until the first packaged release.

## Built-in safe tools planned for first start

Examples:

```text
hotfolder.list
hotfolder.read_text
hotfolder.write_text
hotfolder.append_text
hotfolder.mkdir
hotfolder.search_text
hotfolder.watch_events
hotfolder.hash
hotfolder.move
hotfolder.copy

calculator.evaluate
text.transform
json.validate
hash.sha256
system.info
web.search        # after user configures a provider
```

## A different kind of Marketplace

Community members can publish:

```text
schemas
API -> MCP integrations
Python tools
JavaScript tools
PowerShell tools
Java tools
native binaries
existing CLI definitions
policy templates
```

But the community does **not** decide what runs in your company.

The flow is:

```text
Community publishes capability
        |
        v
Admin reviews it
        |
        v
Organization configures credentials + permissions
        |
        v
ToolGate deploys it to selected clients
        |
        v
Gateway authorizes each protected execution
```

## Security model

A Marketplace signature is not enough to run software.

A managed client requires:

```text
Marketplace release trust
        +
Organization deployment trust
        +
Runtime Gateway authorization
```

for protected execution.

Credentials stay in the organization's configured Vault and are referenced as:

```text
secret://github/team-token
```

not embedded into packages or prompts.

## Architecture

Customer runtime:

```text
AI Clients
    |
    v
ToolGate Gateway  x N
    |
    +---- Remote MCP / API
    |
    +---- short-lived permit ----> Endpoint Clients
                                    |
                                    v
                                Local Tools

ToolGate Control  x N
    |
    +---- PostgreSQL
    +---- Vault
    +---- Artifact Store
```

Community Marketplace:

```text
Drupal
   |
Marketplace API
   |
PostgreSQL / Object Storage / Queue
   |
Marketplace Worker
   |
Isolated Sandbox
   |
Signing Service / KMS
```

Read the architecture: [`docs/architecture/overview.md`](docs/architecture/overview.md)

## Monorepo

```text
apps/
├── gateway/               # Rust
├── control-plane/         # Java
├── admin-ui/              # React / TypeScript
├── endpoint-client/       # Rust
├── marketplace-api/       # Java
├── marketplace-worker/    # Java
└── marketplace-drupal/    # Drupal / PHP
```

## Want to contribute?

You should be able to get from clone to a useful developer environment with:

```bash
git clone https://github.com/olo-labs/olo-toolgate.git
cd olo-toolgate
make dev
```

The target `make dev` experience starts the local stack, mock MCP/API services, seed data, and a development endpoint client.

Start here:

- [Contributing](CONTRIBUTING.md)
- [Development](DEVELOPMENT.md)
- [Architecture](ARCHITECTURE.md)
- [Roadmap](ROADMAP.md)
- [Good first contribution ideas](docs/contributors/good-first-contributions.md)

## Where help is most useful

We especially welcome contributors interested in:

- Rust networking/runtime security
- Java control-plane/backend systems
- React/TypeScript UX
- Windows/Linux/macOS endpoint management
- MCP / OAuth / OIDC
- sandboxing
- package supply-chain security
- Drupal
- developer tooling
- documentation
- security review

You do **not** need to understand the entire platform before contributing.

## The vision

We want organizations to be able to say:

> "These are the capabilities our AI agents can use. These users can use them. These devices may run them. These resources are allowed. Sensitive actions require approval. Everything is visible and reversible."

And make setting that up feel simple.

## If this direction is useful to you

⭐ **Star the repository** so you can follow the build.

💬 **Start a Discussion** and tell us the first tool or workflow you would want ToolGate to control.

🧩 **Pick an issue** if you want to help build it.

🔐 **Security researchers:** please read [`SECURITY.md`](SECURITY.md) before reporting vulnerabilities.

---

**Repository:** https://github.com/olo-labs/olo-toolgate  
**Organization:** https://github.com/olo-labs
