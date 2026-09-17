# Architecture Overview

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
