# Key Rotation

## Purpose

Rotate Marketplace, deployment, bundle, permit and device keys separately with overlap and `kid`.

Module 04 implements dedicated RSA policy keys with explicit local key IDs. Deploy
old/new public overlap before switching the Control signing key, republish and
observe every Gateway sequence, then remove the old key with a restart. Follow
[the concrete rotation and compromise runbook](../control-plane/policy-bundles.md).

For Module 05, rotate Gateway permit keys independently from Control policy keys.
Use `gateway.approval.keyRevision` to trigger rollout; drain the maximum ten-second
outstanding permits before removing old verification capability. Restart can
conservatively invalidate permits. [Approval failures/operations](../control-plane/approvals.md).
