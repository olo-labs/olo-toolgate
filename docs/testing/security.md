# Security Tests

## Purpose

Auth bypass, JWT confusion, replay, SSRF, traversal, archive bombs, shell injection, secret leakage, invalid signatures and sandbox escape attempts.

Include [device registry](../control-plane/device-registry.md) negatives: forged
device context, owner/tenant mismatch, pending or suspended access, stale approval
CAS, expired approval, retired keys and recovery with a different key. Prove actual
fixed-executor/HotFolder/REST-relay denial, retain resource confinement, and verify
temporary HTTP 423 responses never become permanent identity loss or permission.
