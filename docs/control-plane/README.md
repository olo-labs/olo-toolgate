# Control Plane

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
