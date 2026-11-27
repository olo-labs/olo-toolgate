<!-- Copyright 2026 OLO Labs; SPDX-License-Identifier: Apache-2.0 -->
# Remaining Marketplace Demo 3 Java delivery

Tracking: [issue 339](https://github.com/olo-labs/olo-toolgate/issues/339).
Demo 3 is unavailable until the published-artifact acceptance below passes.

- Qualify the actual Compose runtime container, including delegated CPU, memory,
  I/O and PID controllers, separate rootless identities, worker/broker authority,
  deadlines, cancellation, crash recovery and confirmed cleanup. Document its
  required host capabilities, durable volumes and unavoidable operator setup.
- Exercise real Java SDK packages under that runtime, including worker/broker
  failures, denied effects, timeouts, cancellation and no automatic uncertain replay.
- Include the runtime container, immutable JVM and broker images in the established
  release workflow. Collect source-bound evidence, compatibility metadata,
  checksums and tested archives. Promotion must reuse their exact bytes.
- Build new versions of all seven mocked packages with the same JVM image pin and
  compatible SDK/contracts. Supply Marketplace startup with released binaries and
  archives through idempotent installation; no ToolGate checkout or compilation.
- From a clean environment using the exact publication candidates, invoke
  `examples.loans/evaluate` and all configured Demo 3 readiness calls through
  Gateway/MCP. Successful calls must have Control `operationOutcome.state=SUCCEEDED`
  and `finalized=true`; run the documented samples from care, incidents, support,
  HR, fraud and claims. Expected-output fixtures cannot replace execution.
- Repeat startup without data reset; retain Demos 1/2 regression evidence. Preserve
  existing unknown outcomes and do not replay uncertain or state-changing calls.
- Pass component, repository and release gates; publish through the release line.
  Repeat acceptance with the downloaded published artifacts before announcing
  availability. Provide exact versions/digests, Compose configuration, installation,
  qualification, readiness commands, evidence and remaining deployment limits.
