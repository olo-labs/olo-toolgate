# Codex Runbook

## Copy/Paste Prompt for a Module

```text
You are working in the OLO ToolGate repository.

Implement ONLY:

docs/codex/modules/<MODULE>.md

Mandatory governing documents:

docs/codex/00-MASTER-IMPLEMENTATION-PROMPT.md
docs/codex/09-REQUIREMENTS-TRACEABILITY.md
docs/codex/08-DEFINITION-OF-DONE.md

First:
1. read all documents referenced by the master prompt;
2. inspect the current repository state;
3. produce a requirement coverage table for this module;
4. identify any missing prerequisite;
5. if prerequisites are satisfied, implement.

Requirements:
- production-quality code;
- complete unit/integration/security/contract/E2E coverage applicable to the module;
- maintainable low-coupling design;
- copyright/SPDX headers;
- comments explaining non-obvious/security behavior;
- Docker updates where deployable;
- Kubernetes/Helm updates where deployable;
- observability;
- CI;
- shared contract updates/publication compatibility;
- release impact;
- documentation.

Do not skip requirements because another future module might implement them.
Do not start the next module.

At the end, produce:
- requirement traceability with evidence;
- commands run and results;
- files changed;
- suggested commit message.
```

## Final GA Audit Prompt

```text
Audit the entire OLO ToolGate repository against:

- all architecture docs
- all security invariants
- docs/codex/09-REQUIREMENTS-TRACEABILITY.md
- docs/codex/08-DEFINITION-OF-DONE.md
- every module prompt 00–18

Do not implement unrelated new product features.

Find every:
- missing requirement
- stub
- unsafe fallback
- missing test
- missing Helm artifact
- missing release artifact
- missing Maven publication path
- missing Docker image workflow
- missing docs
- duplicated contract
- cross-component coupling
- insecure default
- non-HA state assumption
- secret leakage risk
- upgrade/rollback gap

Produce a blocking checklist.

Then fix blockers in dependency order.

Run the full release gate and update the requirements matrix with evidence.
```
