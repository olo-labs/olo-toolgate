# Contract set v2: binding generation design

## Purpose

How milestone M1 generates Rust, Java, TypeScript and PHP bindings for the frozen v2 schemas in [packages/contracts/schemas/v2](../../packages/contracts/schemas/v2/README.md), alongside the existing v1 bindings, and how the contract-set version moves to 1.0. This is a design note (gate D1); `tools/contracts/generate.py` is unchanged until M1.

## Current state (v1)

- `tools/contracts/generate.py` reads only `schemas/v1/*.json` and emits one flat namespace per language: Rust crate `olo-toolgate-contracts`, Java package `io.ololabs.toolgate.contracts`, TypeScript `@olo-labs/toolgate-contracts`, PHP `OloLabs\ToolGate\Contracts`.
- It refuses `oneOf`, `anyOf`, `allOf`, `if`/`then`/`else`, `not`, `patternProperties`, `dependentSchemas` and `unevaluatedProperties`, inline enums and inline object models.
- It copies each schema into `packages/contracts/rust/schemas/v1/` for `include_str!`. `rust/schemas/v1/approval.schema.json` has no canonical source in `schemas/v1/` (it is a stale copy), and nothing detects that today.
- `tools/contracts/fixtures.py` builds the v1 round-trip corpus; `generated-manifest.json` records source digests.

## Decisions

1. **Namespaced, side by side.** v2 models are generated into a separate namespace in every language, so v1 and v2 types with the same name (`RequestContext`, `PackageManifest`, `ToolDefinition`) coexist during migration:

   | Language | v1 (unchanged) | v2 |
   |---|---|---|
   | Rust | crate root | module `olo_toolgate_contracts::v2` |
   | Java | `io.ololabs.toolgate.contracts` | `io.ololabs.toolgate.contracts.v2` |
   | TypeScript | package root | subpath export `@olo-labs/toolgate-contracts/v2` |
   | PHP | `OloLabs\ToolGate\Contracts` | `OloLabs\ToolGate\Contracts\V2` |

   Same package identities and one shared SemVer; no new artifacts.

2. **Same structural subset, two additions.** v2 schemas stay inside the v1 generator's subset (enforced now by `test_definitions_stay_in_the_generator_subset`). The generator gains exactly:
   - `type: number` → `f64` / `Double` / `number` / `float`;
   - a property schema with no `type` (only a description, used for typed defaults and effective values) → `serde_json::Value` / `JsonNode` / `unknown` / `mixed`.
   Everything else that the v1 generator refuses stays refused.

3. **`const` and integer-const fields** (`schemaVersion: 2`, `descriptorGeneration: 1`, `formatVersion: 2`) are emitted as the literal type where the language has one (TypeScript) and validated on construction elsewhere.

4. **Stale-copy check.** The generator computes the full output set for both versions and fails (in `--check`) when `packages/contracts/rust/schemas/` contains any file without a canonical source. M1 removes `rust/schemas/v1/approval.schema.json`, or restores its canonical source if something still needs it, in the same PR that adds the check.

5. **Manifest.** `generated-manifest.json` gains `schemaSha256` entries for `schemas/v2/*.json`; the generator digest covers both versions.

6. **Fixtures.** `tests/fixtures/contracts/v2/valid.json` is the v2 round-trip corpus. The per-language round-trip tests (`rust/tests/roundtrip.rs`, `ContractRoundTripTest.java`, `php/tests/roundtrip.php`, `typescript/test/roundtrip.mjs`) gain a v2 section that decodes and re-encodes every v2 fixture byte-equivalently (after JSON normalisation). `invalid.json` stays a schema-validation corpus; bindings are structural and are not expected to reject every invalid case.

7. **Version.** The contract set moves from `0.10.0-dev` to `1.0.0` when M1 ships the v2 bindings, because v2 is the major break (no device in `RequestContext`, Control-issued Tool Host permits). v1 bindings stay published in the same artifacts for at least the legacy window in ADR 018.

8. **Descriptor digests are not computed by the bindings.** Canonicalisation and digests live in the SDK packagers and Control, implemented against [tool-sdk/spec/descriptor](../../tool-sdk/spec/descriptor/README.md) and its vectors, not in generated code.

## Acceptance for M1

- `python tools/contracts/generate.py --check` covers v1 and v2 and the stale-copy rule.
- Round-trip tests pass for every v2 fixture in all four languages.
- No v2 schema file changes in the M1 PRs.
