# Health and Inventory

## Purpose

Report client version, OS/arch, runtime availability, installed package versions and health—never user file contents.

The Clients table joins these reports with the [device registry](../control-plane/device-registry.md),
including names, registered users and pending requests. Connected requires an
authenticated report within two minutes and current registry access; approval
alone is insufficient. Server-managed Quickstart devices use composition readiness.
Unknown availability is not green. Check-ins update report revision independently
of approval revision and cannot override disablement, deapproval or expiry.
