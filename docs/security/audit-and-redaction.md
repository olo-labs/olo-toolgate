# Audit and Redaction

Device approve/deapprove, enable/disable and exact-key certificate recovery are transactional audit events; PostgreSQL migration V11 permits them. Administrative retries are idempotent. Public device details exclude private keys, device codes and runtime credentials.

See [Device registry and tool-call controls](../control-plane/device-registry.md).

## Purpose

Audit metadata first; redact tokens/passwords/private keys/cookies and configured sensitive fields.
