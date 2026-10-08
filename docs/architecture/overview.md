# Architecture Overview

Managed tool execution uses a trusted registered device, current owner/device enablement, installed-client connection approval, and the existing agent/tool/action/resource filters before every protected effect. Registration, green availability or deployment never grants execution.

See [Device registry and tool-call controls](../control-plane/device-registry.md).

## Purpose

Describe the complete OLO ToolGate system in one page.

## Customer Runtime

```text
AI Clients
   |
   v
Gateway (stateless, xN)
   |
   +---- remote MCP/API targets
   |
   +---- authorization permits ----> Endpoint Clients
                                      |
                                      v
                                  Local Tools

Control Plane (stateless, xN)
   |
   +---- PostgreSQL
   +---- Vault
   +---- Artifact Mirror
   +---- optional Redis/queue/object storage
```

## Quickstart

```text
olo-toolgate-quickstart
  ├─ Gateway
  ├─ Control Plane
  ├─ Admin UI
  ├─ embedded DB/cache
  ├─ encrypted built-in vault
  └─ built-in safe tools
```

## Community Marketplace

```text
Drupal
  |
Marketplace API
  |
PostgreSQL / Object Storage / Queue
  |
Marketplace Worker
  |
Sandbox + Signing Service
```

## Runtime Decision

```text
User + Team + Agent + Device + Tool + Resource + Operation + Context
                                      |
                                      v
                              ALLOW / ASK / BLOCK
```
