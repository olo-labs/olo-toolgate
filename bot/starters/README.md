<!-- Copyright 2026 OLO Labs; SPDX-License-Identifier: Apache-2.0 -->
# Business use-case starter bots

Eight editable bots use the ordinary OLO import, review, preparation, publication
and Run interfaces. Each graph first calls the pinned `olo.business/process-intake`
tool, then runs an analyst and a reviewer. Its `report`, source pages and manifest
digest come directly from the processor. Models produce separate `analysis` and
`review` commentary and cannot replace the deterministic findings.

| Bot | Sample result |
|---|---|
| [OCR](../configurations/starter-ocr/README.md) | PDF/image OCR, right-angle orientation, supplier fields and unreadable email |
| [Medical](../configurations/starter-medical/README.md) | Chronology, missing department and conflicting dates |
| [VLSIC](../configurations/starter-vlsic/README.md) | Missing signal and unsupported timing check |
| [Loan](../configurations/starter-loan/README.md) | Completeness and conflicting income values |
| [Invoice](../configurations/starter-invoice/README.md) | Quantity mismatch and missing receipt |
| [Support](../configurations/starter-support/README.md) | Source inventory, response draft and handoff requirements |
| [IT service desk](../configurations/starter-itsm/README.md) | Triage, runbook and proposed action awaiting approval |
| [Onboarding](../configurations/starter-onboarding/README.md) | Checklist, policy answer and pending access |

## Release assets and setup

Provision the [processor and read-only intake repository](../business/README.md).
Set `OLO_BUSINESS_CONNECTION` to its installation-local connection JSON before
selecting these bots in the setup menu. Setup checks the managed OCI Tool Host's
operator-observed public key before enrollment approval, allocates its group, imports the
scoped processor secret and creates exact grants for the selected starter's two
synthetic intake IDs. Production intakes need separate reviewed grants. Current
grants, execution bindings and broker profiles remain necessary at each call.

Stage `sample-intake.json` and its declared files under the directory named by
`sample-input.txt`, with the manifest named `manifest.json`. Stage the exception
the same way. `bot.business.installation.stage_intake` verifies source sizes and
hashes before copying into a fresh directory. Mount the repository read-only.
In OLO Run, paste `sample-input.txt` or `exception-input.txt`; the message is an
intake ID, not a PDF or JSON blob. The [document-job target](../document-target/README.md)
records fenced submission and result-access receipts for that intake. The
extractor verifies every file against its observed manifest. Changed or unavailable files fail the
tool call. Denied or unknown tool outcomes stop before model commentary.

The graphs use APPLY mode for these declared job/receipt writes. Their agent
catalog contains only the document-processing tool. This does not add sandbox
action tools, grant business-action authority or approve a proposed remediation;
those remain separate user-bound handoffs under current permissions.

The ordinary profile builder includes all eight. Build only these starters with:

```powershell
python bot/build.py --configuration starter-ocr --configuration starter-medical --configuration starter-vlsic --configuration starter-loan --configuration starter-invoice --configuration starter-support --configuration starter-itsm --configuration starter-onboarding --output bot/bin/starters
```

`olo-profiles-<version>.json` identifies exact `.olo` filenames, SHA-256 digests,
sizes, source commit and dependencies. Its `starterKit` entry identifies the
companion `olo-starters-<version>.zip`. The kit pins those same profile rows and
contains editable configuration, guides, synthetic inputs, authored expected
reports and license notices. The release collector verifies and retains its
bytes. Checksums and authored expected output do not prove a live run.

Download the bot `.olo` and companion kit from the same ToolGate release. Upload
only `.olo` through **Bots > Import bot .olo**. Extract the ZIP separately for
samples; it is not a bot/tool upload. Each `starter-<key>/` directory contains
the guide and samples. Guide links are repository-relative and are intended
for the matching checkout, which also provides the evaluator and quickstart
wrappers. Follow the [installation guide](../README.md) for model custody,
readiness and publication, or choose a starter in `bot/start.bat` / `bot/start.sh`.
Local builds need `-BundlesDir` / `--bundles-dir` until those bytes are published.

Selected-bot setup installs/preloads `qwen3:4b` once in the existing managed
Ollama companion before publication. Allow roughly 2.5 GB for its download plus
inference memory; CPU execution is slower than a supported GPU. This model is
separate from the smaller general model used by the other examples. Manual
imports require the operator to provision that exact model separately.

## Evaluate and tune

Run both the normal sample and missing-source exception through the published
bot. Save the actual result object, without its execution envelope, as private
JSON. From a matching checkout with repository Python requirements installed:

```powershell
python bot/starters.py --configuration starter-loan --result build/loan-result.json
python bot/starters.py --configuration starter-loan --exception --result build/loan-exception-result.json
```

The evaluator checks schema, input identity, expected finding statuses/values,
source hashes, coverage, quotes, line positions and commentary citations. It rejects duplicate findings,
fabricated quotes, lost conflicts and missing evidence. It does not validate
arbitrary prose semantics, authorize actions, execute a model or measure quality
beyond these synthetic checks. Review the summary, draft, chronology and next
actions yourself. Record actual run ID, terminal state, revision, provider/model,
latency and assessment privately. Failed, denied and unknown executions are not
successful sample results.

Modify prompts, checks and knowledge using the [tuning guide](../../docs/help/tune-bots.md);
keep held-out examples. Weight training and provisioning remain separate.
Bundles contain neither training jobs nor credentials. The hotfolder QLoRA model
is not domain training; these starters use the general local model group.

## Boundaries

The confined processor extracts PDF/PNG/JPEG/text, checks explicit labelled
fields and versioned scalar structural Verilog, and compares financial records
using exact decimal arithmetic. It performs no model calls or business effects.
Medical output is document administration for professional review; loan output
does not approve credit or authenticate identities. Structural checks do not
prove synthesis, timing closure or chip signoff. Reconciliation executes no payment.

Support, IT and onboarding retain a `knowledgeProvenance` review finding until
actual user-bound retrieval is connected. A source claiming to be approved is
not a governed retrieval receipt. Drafts, proposed actions and pending access
records do not claim that a message was sent or an account provisioned.

Before advertising accepted public demos, qualify real release download/import/
setup/sample/exception runs, source correctness, applicable governed effects,
idempotency, integration versions and each observability provider separately.
Capture actual results after those runs. The kit records `qualification: not-qualified`.
Schema, bundle and scripted-provider graph tests do not establish release acceptance.
See the [remaining acceptance plan](../../docs/plans/business-starters.md).

## User-bound knowledge and action handoff

The current RAG/memory and sandbox tools require a verified human context.
Agents cannot call them by acquiring SERVICE authority or copying a credential
into their prompt. The operator uses the existing public Control human-tool
invocation API after reviewing the proposed work. The supplied thin client is
available from the matching checkout:

```powershell
python -m bot.business.user_tools --origin https://YOUR_CONTROL --ca-file D:/private/control-ca.crt --token-file D:/private/user.token --binding-id YOUR_REVIEWED_BINDING --device-id business-tool-host --request-key policy-review-42 --output D:/private/policy-review-42.json query --kb-id YOUR_KNOWLEDGE_BASE_UUID --query "Applicable approved policy"
python -m bot.business.user_tools --origin https://YOUR_CONTROL --ca-file D:/private/control-ca.crt --token-file D:/private/user.token --binding-id YOUR_REVIEWED_BINDING --device-id business-tool-host --request-key service-apply-42 --output D:/private/service-apply-42.json apply --operation restart --subject YOUR_SANDBOX_SERVICE --reference business-request-42
```

Deploy and grant the [knowledge adapters](../tools/README.md) and
[sandbox target](../sandbox/README.md) first. UUIDs, bindings, resources and
credentials above are installation-owned values. The client records its explicit
request key before admission and the authoritative invocation ID before polling,
including for retrieval. It exposes output only for a matching finalized success.
Pending approval remains pending. An existing journal is never overwritten by a
new admission; transport failure or an unknown effect requires investigation of
the saved request/invocation, without automatic repetition. Journals may contain
private query results and belong outside release assets.
