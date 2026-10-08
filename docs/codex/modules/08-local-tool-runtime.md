# Module 08 — Local Tool Runtime

## Dependencies

Modules 06–07.

## Objective

Implement standard local execution protocol and runtime adapters.

Follow the current [device registry execution flow](../../control-plane/device-registry.md).
Resolve device identity from authenticated service context, check current owner,
enablement and installed-client approval, then enforce Gateway filters and permits
before effects. Registry approval never relaxes sandbox limits. Test disabled,
deapproved and expired devices against actual execution, including restart/recovery.

## Mandatory Preparation

Read the master prompt and requirements traceability document.

Before coding produce the module requirement coverage table.

## Deliverables


- JSON stdin/stdout invocation;
- native;
- Python;
- Node;
- PowerShell;
- Batch/CMD;
- Shell;
- Java JAR;
- .NET abstraction where supported;
- future WASM interface;
- runtime discovery;
- managed runtime strategy;
- timeout/memory/output limits;
- sanitized environment;
- working directory isolation;
- no unsafe string shell concatenation;
- logs/redaction;
- health/self-test.


## Mandatory Tests


- argument injection;
- timeout;
- output overflow;
- missing runtime;
- wrong version;
- child-process restriction;
- environment leakage;
- malformed output.


## Deployment / Release


- client release packaging/runtime dependencies documented.


## Completion

Apply `docs/codex/08-DEFINITION-OF-DONE.md`.

Do not start another module.

## Suggested Commit

```text
feat(client): implement managed local tool runtimes
```
