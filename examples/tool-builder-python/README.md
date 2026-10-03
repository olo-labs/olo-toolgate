# Builder example: trim text

Paste [tool.py](tool.py) into the Python editor. Use package `text-trim`, tool
`custom.trim`, version `1.0.0`, `COMPUTE` and `runtime/custom.trim`.
Select your reviewed digest-pinned Python image and its exact runtime version;
there is no placeholder executable or bundled release key.

Use the same input and output schema:

```json
{"type":"object","additionalProperties":false,"properties":{"text":{"type":"string","maxLength":256}},"required":["text"]}
```

Examples:

```json
[{"arguments":{"text":"  hello  "},"expectedOutput":{"text":"hello"}},{"arguments":{"text":"already trimmed"},"expectedOutput":{"text":"already trimmed"}}]
```

Description: “Trim surrounding whitespace from a bounded text value.” Use when:
“Normalizing user-provided text without accessing files.” Do not use when:
“Reading files, retrieving credentials or accessing external services.”
Leave credential requirements empty. Test both examples on an enrolled client,
seal, independently review/sign/mirror the descriptor, publish and deploy as
described in [the production/debug guide](../../docs/client/tool-builder.md).
