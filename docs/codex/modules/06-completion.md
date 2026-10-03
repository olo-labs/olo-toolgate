# Module 06 verification report

Status: **IN PROGRESS, NOT DONE**, 2026-10-03. The user requested a commit of the
current changes. This report records the implemented foundation and outstanding
gates rather than claiming completion under
[the definition of done](../08-DEFINITION-OF-DONE.md).
Preparation preceded implementation; see [the coverage plan](06-coverage.md) and
[ADR 007](../../adr/007-endpoint-enrollment.md). No later module was started.

## Implemented

The Rust client provides protected key generation/storage, OS service abstractions,
fixed HTTPS discovery, CSR enrollment, certificate verification, ordered durable
check-in, offline/revoked readiness, structured logs, bounded authenticated IPC,
graceful shutdown and an install/uninstall framework. Control adds real PKCS#10
verification, a dedicated external RSA device CA, human browser decisions,
actual TLS peer authentication, active-owner/device checks, renewal, revocation,
transactional audit and migration V5. Shared schemas generate Java/Rust/TypeScript/
PHP models. The UI requires explicit comparison of the public code and key hash.
Helm references external CA/TLS/trust-store Secrets; CI defines six native targets
and deterministic packaging with dependency SBOMs and checksums.

There is no production mock, execution capability, package installation, or
Gateway runtime credential issuance. Existing Gateway identity interfaces are
retained; enrollment does not authorize runtime calls by itself.

## Executed evidence

- `python tools/control/check.py`: Control build, 19 Java tests against real
  PostgreSQL, migration/renewal/revocation/concurrent decision/audit rollback
  coverage, and existing authenticated HTTP/telemetry smoke passed.
- `cargo test -p olo-toolgate-client --locked`: storage/key persistence, strict
  origin/IPC contracts, unauthorized peers, exclusive lease and offline paths
  passed. A public signed-discovery regression fixture also checks wrong origin,
  expiry, changed CA pin and corrupted signature.
  All eight client tests passed.
- `cargo clippy -p olo-toolgate-client --all-targets --locked -- -D warnings`:
  Linux passed; formatting is enforced by the repository check.
- `cargo check -p olo-toolgate-client --all-targets --locked --target
  x86_64-pc-windows-gnu`: Windows x64 code and tests compile. This is cross-target
  compilation, not execution of Windows DPAPI, ACL, pipe or service tests.
- `npm --workspace @olo-labs/toolgate-admin-ui run test`: UI component and transport
  tests passed, including explicit enrollment confirmation and competing decisions.
  All 38 UI tests passed.
- `python -m unittest discover -s tests/contracts -v`: 43 tests passed, including
  closed endpoint contracts, external chart keys, compatibility and drift checks.
- `python tools/check.py --scans`: workflow, npm, Python, resolved dependency
  license (782 entries), Gitleaks, adversarial secret checks and source scans passed.
- Existing Helm install/upgrade/rotation/negative render checks passed.
- Local Java artifact publication and all three artifact-mode service builds passed.
- Client archive/checksum/SBOM packaging was run twice over the same tested Linux
  debug executable; checksums matched byte for byte. This checks deterministic
  packaging, not a signed production release build.
- Existing browser regression E2E passed: five tests against real Control/PostgreSQL.
  This does not exercise the new TLS enrollment browser flow.

`python tools/check.py` passed: source and local-artifact Java builds, schema/
drift/compatibility gates, workspace Rust tests/Clippy, TypeScript/UI tests,
five existing browser E2E cases, 90 PHP round trips and Helm validation. The
final multi-part CA-name regression was followed by a successful Control build,
all 19 Java tests and authenticated HTTP/telemetry smoke. The separate
`python tools/client/helm.py` check passed seven rendered Kubernetes resources
and four negative cases; it is wired into subsequent repository checks.
Logs are retained locally under `.dev/client-*.log`.

## Open mandatory gates and implementation limitations

Module 06 cannot be declared complete until all of these are resolved:

1. Real direct-HTTPS/mTLS client-to-Control enrollment, lost-response reconnect,
   outage/revocation and browser E2E; dedicated enroller authorization at the HTTP
   boundary must be proved. Unit/domain tests do not replace this evidence.
2. Native Windows/macOS install/uninstall, DPAPI/ACL/pipe and service-manager tests;
   Linux arm64, macOS x64/arm64 and Windows arm64 compilation/execution evidence.
   Configured CI jobs have not been run remotely in this task.
3. Protected signing hooks and GitHub Release publication wiring, native production
   package validation and final signed binary SBOM/provenance proof.
4. Enrollment-enabled production Control image build/scan and two-replica TLS
   cluster smoke, including chart trust-store and external Secret behavior.
5. Administrator recovery for partial installation and expired-certificate
   re-enrollment. Current uninstall retains identity; revocation is durable.
6. Complete the security review of native installation/ACL paths, certificate
   identity extension binding, CA rotation/recovery and fleet limits. The
   configured discovery floor currently requires matching client/contract versions.

These are unresolved gates, not deferred deliverables for another module. The
requirement matrix deliberately leaves applicable IDs BLOCKED pending integrated
proof and review, even where implementation and unit evidence already exist.

## Requirement status

All 104 IDs are listed individually below. Three are outside Module 06's scope;
the other 101 remain blocked by the module-wide gates above. BLOCKED here is a
requirements-report status, not a claim that no further engineering is possible.

| Requirement | Status | Evidence or outstanding gate |
|---|---|---|
| ARC-001 | BLOCKED | ADR 007 implemented; integrated native/TLS architecture review remains. |
| ARC-002 | BLOCKED | ADR 007 implemented; integrated native/TLS architecture review remains. |
| ARC-003 | BLOCKED | ADR 007 implemented; integrated native/TLS architecture review remains. |
| ARC-004 | BLOCKED | ADR 007 implemented; integrated native/TLS architecture review remains. |
| ARC-005 | BLOCKED | ADR 007 implemented; integrated native/TLS architecture review remains. |
| ARC-006 | BLOCKED | ADR 007 implemented; integrated native/TLS architecture review remains. |
| ARC-007 | BLOCKED | ADR 007 implemented; integrated native/TLS architecture review remains. |
| CON-001 | BLOCKED | 90 generated models and schema/round-trip gates; real TLS compatibility remains. |
| CON-002 | BLOCKED | 90 generated models and schema/round-trip gates; real TLS compatibility remains. |
| CON-003 | BLOCKED | 90 generated models and schema/round-trip gates; real TLS compatibility remains. |
| CON-004 | BLOCKED | 90 generated models and schema/round-trip gates; real TLS compatibility remains. |
| CON-005 | BLOCKED | 90 generated models and schema/round-trip gates; real TLS compatibility remains. |
| CON-006 | BLOCKED | 90 generated models and schema/round-trip gates; real TLS compatibility remains. |
| SEC-001 | BLOCKED | Negative custody/CSR/identity tests exist; native OS and production TLS review remains. |
| SEC-002 | BLOCKED | Negative custody/CSR/identity tests exist; native OS and production TLS review remains. |
| SEC-003 | BLOCKED | Negative custody/CSR/identity tests exist; native OS and production TLS review remains. |
| SEC-004 | BLOCKED | Negative custody/CSR/identity tests exist; native OS and production TLS review remains. |
| SEC-005 | BLOCKED | Negative custody/CSR/identity tests exist; native OS and production TLS review remains. |
| SEC-006 | NOT_APPLICABLE | No untrusted execution or sandbox in Module 06. |
| SEC-007 | BLOCKED | Negative custody/CSR/identity tests exist; native OS and production TLS review remains. |
| SEC-008 | BLOCKED | Negative custody/CSR/identity tests exist; native OS and production TLS review remains. |
| SEC-009 | BLOCKED | Negative custody/CSR/identity tests exist; native OS and production TLS review remains. |
| SEC-010 | BLOCKED | Negative custody/CSR/identity tests exist; native OS and production TLS review remains. |
| TST-001 | BLOCKED | Local tests and native CI matrix exist; platform and TLS/browser E2E gates remain. |
| TST-002 | BLOCKED | Local tests and native CI matrix exist; platform and TLS/browser E2E gates remain. |
| TST-003 | BLOCKED | Local tests and native CI matrix exist; platform and TLS/browser E2E gates remain. |
| TST-004 | BLOCKED | Local tests and native CI matrix exist; platform and TLS/browser E2E gates remain. |
| TST-005 | BLOCKED | Local tests and native CI matrix exist; platform and TLS/browser E2E gates remain. |
| TST-006 | BLOCKED | Local tests and native CI matrix exist; platform and TLS/browser E2E gates remain. |
| TST-007 | NOT_APPLICABLE | No execution performance path added; existing Gateway benchmarks remain. |
| TST-008 | BLOCKED | Local tests and native CI matrix exist; platform and TLS/browser E2E gates remain. |
| QLT-001 | BLOCKED | Local formatting/static/scan gates exist; final integrated review remains. |
| QLT-002 | BLOCKED | Local formatting/static/scan gates exist; final integrated review remains. |
| QLT-003 | BLOCKED | Local formatting/static/scan gates exist; final integrated review remains. |
| QLT-004 | BLOCKED | Local formatting/static/scan gates exist; final integrated review remains. |
| QLT-005 | BLOCKED | Local formatting/static/scan gates exist; final integrated review remains. |
| QLT-006 | BLOCKED | Local formatting/static/scan gates exist; final integrated review remains. |
| API-001 | BLOCKED | Canonical OpenAPI and use cases exist; dedicated human/mTLS HTTP E2E remains. |
| API-002 | BLOCKED | Canonical OpenAPI and use cases exist; dedicated human/mTLS HTTP E2E remains. |
| API-003 | BLOCKED | Canonical OpenAPI and use cases exist; dedicated human/mTLS HTTP E2E remains. |
| API-004 | BLOCKED | Canonical OpenAPI and use cases exist; dedicated human/mTLS HTTP E2E remains. |
| API-005 | BLOCKED | Canonical OpenAPI and use cases exist; dedicated human/mTLS HTTP E2E remains. |
| API-006 | BLOCKED | Canonical OpenAPI and use cases exist; dedicated human/mTLS HTTP E2E remains. |
| API-007 | BLOCKED | Canonical OpenAPI and use cases exist; dedicated human/mTLS HTTP E2E remains. |
| DAT-001 | BLOCKED | PostgreSQL V5 race/replay/audit rollback tested; two-replica TLS proof remains. |
| DAT-002 | BLOCKED | PostgreSQL V5 race/replay/audit rollback tested; two-replica TLS proof remains. |
| DAT-003 | BLOCKED | PostgreSQL V5 race/replay/audit rollback tested; two-replica TLS proof remains. |
| DAT-004 | BLOCKED | PostgreSQL V5 race/replay/audit rollback tested; two-replica TLS proof remains. |
| DAT-005 | BLOCKED | PostgreSQL V5 race/replay/audit rollback tested; two-replica TLS proof remains. |
| OBS-001 | BLOCKED | Health/logs/Control counters exist; production outage/telemetry proof remains. |
| OBS-002 | BLOCKED | Health/logs/Control counters exist; production outage/telemetry proof remains. |
| OBS-003 | BLOCKED | Health/logs/Control counters exist; production outage/telemetry proof remains. |
| OBS-004 | BLOCKED | Health/logs/Control counters exist; production outage/telemetry proof remains. |
| OBS-005 | BLOCKED | Health/logs/Control counters exist; production outage/telemetry proof remains. |
| OBS-006 | BLOCKED | Health/logs/Control counters exist; production outage/telemetry proof remains. |
| OBS-007 | BLOCKED | Health/logs/Control counters exist; production outage/telemetry proof remains. |
| DKR-001 | BLOCKED | Existing Control runtime retained; endpoint-enabled image build/scan remain. |
| DKR-002 | BLOCKED | Existing Control runtime retained; endpoint-enabled image build/scan remain. |
| DKR-003 | BLOCKED | Existing Control runtime retained; endpoint-enabled image build/scan remain. |
| DKR-004 | BLOCKED | Existing Control runtime retained; endpoint-enabled image build/scan remain. |
| DKR-005 | BLOCKED | Existing Control runtime retained; endpoint-enabled image build/scan remain. |
| DKR-006 | BLOCKED | Existing Control runtime retained; endpoint-enabled image build/scan remain. |
| DKR-007 | BLOCKED | Existing Control runtime retained; endpoint-enabled image build/scan remain. |
| DKR-008 | BLOCKED | Existing Control runtime retained; endpoint-enabled image build/scan remain. |
| K8S-001 | BLOCKED | External Secret references added; production TLS chart/cluster proof remains. |
| K8S-002 | BLOCKED | External Secret references added; production TLS chart/cluster proof remains. |
| K8S-003 | BLOCKED | External Secret references added; production TLS chart/cluster proof remains. |
| K8S-004 | BLOCKED | External Secret references added; production TLS chart/cluster proof remains. |
| K8S-005 | BLOCKED | External Secret references added; production TLS chart/cluster proof remains. |
| K8S-006 | BLOCKED | External Secret references added; production TLS chart/cluster proof remains. |
| K8S-007 | BLOCKED | External Secret references added; production TLS chart/cluster proof remains. |
| K8S-008 | BLOCKED | External Secret references added; production TLS chart/cluster proof remains. |
| K8S-009 | BLOCKED | External Secret references added; production TLS chart/cluster proof remains. |
| K8S-010 | BLOCKED | External Secret references added; production TLS chart/cluster proof remains. |
| K8S-011 | BLOCKED | External Secret references added; production TLS chart/cluster proof remains. |
| K8S-012 | BLOCKED | External Secret references added; production TLS chart/cluster proof remains. |
| K8S-013 | BLOCKED | External Secret references added; production TLS chart/cluster proof remains. |
| K8S-014 | BLOCKED | External Secret references added; production TLS chart/cluster proof remains. |
| K8S-015 | BLOCKED | External Secret references added; production TLS chart/cluster proof remains. |
| K8S-016 | BLOCKED | External Secret references added; production TLS chart/cluster proof remains. |
| REL-001 | BLOCKED | Version/Maven/package skeleton exist; signing/publication/native evidence remain. |
| REL-002 | BLOCKED | Version/Maven/package skeleton exist; signing/publication/native evidence remain. |
| REL-003 | BLOCKED | Version/Maven/package skeleton exist; signing/publication/native evidence remain. |
| REL-004 | BLOCKED | Version/Maven/package skeleton exist; signing/publication/native evidence remain. |
| REL-005 | BLOCKED | Version/Maven/package skeleton exist; signing/publication/native evidence remain. |
| REL-006 | BLOCKED | Version/Maven/package skeleton exist; signing/publication/native evidence remain. |
| REL-007 | BLOCKED | Version/Maven/package skeleton exist; signing/publication/native evidence remain. |
| REL-008 | BLOCKED | Version/Maven/package skeleton exist; signing/publication/native evidence remain. |
| REL-009 | BLOCKED | Version/Maven/package skeleton exist; signing/publication/native evidence remain. |
| REL-010 | BLOCKED | Version/Maven/package skeleton exist; signing/publication/native evidence remain. |
| REL-011 | BLOCKED | Version/Maven/package skeleton exist; signing/publication/native evidence remain. |
| DOC-001 | BLOCKED | Coverage/ADR/component docs/report exist; finalize after remaining gates. |
| DOC-002 | BLOCKED | Coverage/ADR/component docs/report exist; finalize after remaining gates. |
| DOC-003 | BLOCKED | Coverage/ADR/component docs/report exist; finalize after remaining gates. |
| DOC-004 | BLOCKED | Coverage/ADR/component docs/report exist; finalize after remaining gates. |
| DOC-005 | BLOCKED | Coverage/ADR/component docs/report exist; finalize after remaining gates. |
| DOC-006 | BLOCKED | Coverage/ADR/component docs/report exist; finalize after remaining gates. |
| DOC-007 | BLOCKED | Coverage/ADR/component docs/report exist; finalize after remaining gates. |
| PRF-001 | BLOCKED | Bounded enrollment/reports/IPC exist; fleet/native outage validation remains. |
| PRF-002 | BLOCKED | Bounded enrollment/reports/IPC exist; fleet/native outage validation remains. |
| PRF-003 | BLOCKED | Bounded enrollment/reports/IPC exist; fleet/native outage validation remains. |
| PRF-004 | BLOCKED | Bounded enrollment/reports/IPC exist; fleet/native outage validation remains. |
| PRF-005 | NOT_APPLICABLE | No new performance-critical runtime evaluator. |
| PRF-006 | BLOCKED | Bounded enrollment/reports/IPC exist; fleet/native outage validation remains. |
