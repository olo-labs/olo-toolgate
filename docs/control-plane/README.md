# Control Plane

Module 02 implements tenant-scoped users, teams, agents, tools, policies and device
records, versioned CRUD, append-only mutation audit and bounded JSON/YAML configuration
import/export. See [configuration](configuration.md), [API](../api/control-plane-api.md),
[deployment/upgrades](upgrades.md) and [ADR 003](../adr/003-control-plane-transactions.md).
The remaining ownership list below describes the wider architecture; approvals,
fleet deployment, vault bindings and Marketplace workflows are later modules.

The Control Plane is the organization administration service and serves the Admin UI.

## Owns

- users/teams/roles.
- tools/packages.
- policies.
- approvals.
- clients/device groups.
- desired state.
- vault bindings.
- Marketplace imports/publications.
- configuration import/export.
- audit search.

## Important

It must **not execute arbitrary custom tool code inside the Java process**. Testing runs on a designated enrolled test client or isolated sandbox.

Module 09 signed fleet lifecycle: [production usage, configuration and debugging](../client/package-deployment.md).
