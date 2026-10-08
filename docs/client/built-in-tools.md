# Built-In Tools

## Purpose

Quickstart ships at least 10 safe tools: HotFolder operations, calculator, text transform, JSON validation, SHA-256, system info, plus optional configured web search.

Server tools use [registered device controls](../control-plane/device-registry.md).
`local-builtins` gates the fixed executor, including its HotFolder adapter.
HotFolder additionally checks `local-hotfolder` and authorizes with that device's
own runtime credential, allowing independent device filters. Disabling HotFolder
leaves compute available. Every effect still needs fresh Gateway authorization
and any required ASK permit; green readiness and registration do not grant access.
