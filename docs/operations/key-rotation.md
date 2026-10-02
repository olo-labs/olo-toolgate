# Key Rotation

## Purpose

Rotate Marketplace, deployment, bundle, permit and device keys separately with overlap and `kid`.

Module 04 implements dedicated RSA policy keys with explicit local key IDs. Deploy
old/new public overlap before switching the Control signing key, republish and
observe every Gateway sequence, then remove the old key with a restart. Follow
[the concrete rotation and compromise runbook](../control-plane/policy-bundles.md).
