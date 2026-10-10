# Official MCP SDK compatibility records

## Purpose

Records which official MCP SDK versions the standalone runtimes depend on, what was tested, and which gaps the ToolGate adapters own. One record per pinned set; a record is never edited after its PR merges, and a version change adds a new record.

## Current record: 2026-10-10

[2026-10-10/results.md](2026-10-10/results.md), raw outputs in [2026-10-10/raw/](2026-10-10/raw/). Suite `@modelcontextprotocol/conformance@0.2.0-alpha.12`.

Lockfiles for the pinned sets are in [2026-10-10/pins/](2026-10-10/pins/): npm `package-lock.json` (TypeScript), `uv pip compile --generate-hashes` output (Python 3.10+), NuGet `packages.lock.json` (.NET 8), and the Maven POM with the sha256 of every resolved jar (Java, which has no native lockfile). Each SDK build **must** resolve exactly these artifacts.

| Language | Pinned coordinate (commit) | Tier | 2026-07-28 required server scenarios | Adapters used |
|---|---|---|---|---|
| TypeScript | `@modelcontextprotocol/server@2.3.1`, `@modelcontextprotocol/client@2.3.1` (fcef852f) | 1 | 37 / 37 | A2, A3, A4 |
| Python | `mcp==2.3.0` (2118f14f) | 1 | 37 / 37 | A2, A3, A4 |
| .NET | `ModelContextProtocol` 2.2.0 (6fa38259), stateless or hybrid HTTP mode only | 1 | 37 / 37 (stateless endpoint) | A2, A3, A4, A5 |
| Java | `io.modelcontextprotocol.sdk:mcp:2.0.1`, BOM `mcp-bom:2.0.1` (c7e1cfe9) | 2 | 0 / 37 (2025-11-25: 30 / 30) | A1, A2, A3, A4, A5 |

## Feature ownership

| Feature | Result | Owner |
|---|---|---|
| Stateless core, `server/discover`, per-request `_meta` | PASS in TS, Python, .NET; MISSING in Java | Official SDK; A1 in Java |
| MRTR transport | PASS in TS, Python, .NET (14 scenarios each); MISSING in Java | Official SDK plus A3; A1 in Java |
| Tasks extension | PASS only in .NET (without status push); MISSING elsewhere | A2 in all four |
| `subscriptions/listen` | PASS in TS, Python, .NET; MISSING in Java | Official SDK; A1 in Java |
| Legacy 2025-11-25 | PASS in all four | Official SDK |
| stdio and Streamable HTTP | HTTP PASS; stdio by probe | Official SDK; A1 for 2026-07-28 in Java |
| Custom-method hook | Official in TS and Python; experimental in .NET; missing in Java | A5 in .NET and Java |
| Bearer verification | Not covered by the suite | A4 in all four |

## D0 acceptance rule

Every feature the standalone runtime relies on is PASS in the official SDK or owned by an adapter in [protocol-layer.md](../protocol-layer.md). SUPPORTED-BY-SOURCE and UNKNOWN do not pass. The table above meets the rule.

## Adding a record

1. Run the wrapper in [2026-10-10/raw/probe-sources/](2026-10-10/raw/probe-sources/) against the new tags with the declared suite version.
2. Commit `results.md` and raw text outputs under a new dated directory; keep binary archives out of the repository.
3. Update the tables here and in [protocol-layer.md](../protocol-layer.md) if ownership changes.
