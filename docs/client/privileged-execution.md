# Privileged Execution

## Purpose

Privileged credentials remain outside AI/user process. Each protected operation requires a short-lived Gateway permit.

Protected operations also require a trusted [registered device identity](../control-plane/device-registry.md),
current owner/device enablement and, for installed clients, connection approval.
Recheck these gates at the effect boundary. An earlier permit or unlimited
connection approval cannot override disablement, deapproval, expiry or resource filters.
