<!-- Copyright 2026 OLO Labs; SPDX-License-Identifier: Apache-2.0 -->
# Business document processor

This service decodes bounded source documents and produces deterministic,
source-linked findings for the starter bots. ToolGate executes the workflow and
authorizes its tools. The processor contains no model, workflow runner, approval
engine, payment operation or account-management operation. The agents' drafts
remain separate from the processor's findings.

The current supported inputs are UTF-8 text/Markdown, PDF, single-frame PNG and
JPEG. PDFium extracts PDF text layers; raster pages and images use Tesseract's
English model. Four right-angle orientations are compared using OCR confidence
only to select orientation. That score is not a calibrated field-confidence or
accuracy measure. An OCR character can be wrong even when the template parses;
review original pages before relying on values.

## Deploy a confined listener

Build the service from the same source as the bot packages:

```powershell
docker build -f bot/business/Dockerfile -t olo-business-processor:local .
```

Provision a private deployment network, a TLS certificate whose SAN includes
`business-processor`, its private key, and a generated private API key of 32–512
ASCII characters from `A–Z`, `a–z`, `0–9`, `_` and `-`. Mount those files read-only
and make them readable only by the service's UID 65532 and the operator. Never
place credentials, customer documents or private keys in a bot source kit or
release archive. Mount a separate read-only intake directory. For example,
after replacing the deployment-specific names and absolute paths:

```powershell
docker run --name business-processor --network YOUR_PRIVATE_NETWORK --network-alias business-processor --read-only --tmpfs /tmp:rw,noexec,nosuid,size=128m --cpus 4 --memory 768m --pids-limit 64 -v D:/private/business-tls:/run/business:ro -v D:/private/business-intakes:/intakes:ro olo-business-processor:local --certificate /run/business/server.crt --key /run/business/server.key --api-key-file /run/business/api-key --tenant YOUR_VERIFIED_TENANT --intakes /intakes
```

The listener serves HTTPS on port 8443 with TLS 1.2 or later. Its outbound
document processing uses no network calls. Keep network ingress limited to the
reviewed ToolGate broker path. The API key authenticates that route; it does not
replace Control authorization or grant the caller an intake resource.

Create destination-local connection JSON and set `OLO_BUSINESS_CONNECTION` to
its absolute path before running the existing bot configuration command:

```json
{
  "schemaVersion": 1,
  "host": "business-processor",
  "port": 8443,
  "caFile": "D:/private/business-tls/ca.crt",
  "apiKeyFile": "D:/private/business-tls/api-key",
  "privateCidr": "172.30.0.0/24",
  "toolHostEvidenceFile": "D:/private/business-host/public-identity.json"
}
```

Use the actual approved private deployment subnet, not an assumed Docker
range. The loader rejects loopback/link-local ranges, unsupported destinations
and non-current connection formats. The bot setup installs the matching
destination CA/private-range profile and stores `businessprocessorapikey` in
ToolGate's vault. The broker injects it; tool code and model inputs do not
receive the credential. The listener binds to its installation-owned tenant.

The starter placement is `business-tool-host`. Provision a managed rootless
OCI Tool Host using the [runtime guide](../../docs/operations/rootless-tool-host.md)
and the exact Java worker digest embedded in its tool packages. Approve the
executor/image pair in Control only after the applicable containment checks.
Native JVM execution does not provide finalized broker-only outcomes for these
workflows. Before configuration, export the host's public enrollment status to
the evidence file above. Its exact fields are `hostId`, `tenantId`, `userCode`
(null after enrollment) and `keyFingerprint` (64 lowercase hex characters).
The launcher checks tenant, placement and Control's enrolled key before approval
and group allocation. This file contains no private key and does not itself
confer runtime authority. Preserve denied/revoked enrollments for operator repair.
Allocate the host to the administered Device Group named by its configured
`enrollment.poolId` before registration. Starter setup separately allocates its
execution group `business-processor-hosts`; enrollment approval alone does not
grant membership in a runtime pool.

## Intake repository and authority

Each intake is a directory named by an opaque identifier. Its `manifest.json`
has exactly `schemaVersion: 1`, `useCase`, `documents` and `request`. Each document
declaration has exactly `id`, `file`, `mediaType`, `bytes` and `sha256`. `request`
contains the domain rules/records and must not embed `documents`. Source files
are regular files directly inside that intake directory. Current manifest
examples are shipped beside each starter configuration.

Stage authored synthetic samples into a **fresh** repository location using the
existing hash-checking helper; it refuses to replace an existing intake:

```python
from pathlib import Path
from bot.business.installation import stage_intake

folder = Path("bot/configurations/starter-ocr").resolve()
for case in ("sample", "exception"):
    stage_intake(folder, case, Path("D:/private/business-intakes"))
```

Grant the selected runtime agent current `document.process` authority on the
exact resource `documents://<tenant>/intake/<intakeId>` through Control. Device
approval, deployment and possession of a manifest do not supply that authority.
The starter setup scopes its grants to its two authored synthetic intake IDs;
real intake IDs require separately reviewed grants. Secret access also needs
the current scoped SECRET grant. See
[service authorization](../../docs/control-plane/olo-service-permits.md) for the
separate memory/RAG target authority contracts.

`olo.business/process-intake` uses the [fenced document-job adapter](../document-target/README.md).
It submits the verified tenant, use case and intake ID, then waits for confirmed
result access with the same manifest digest. Each target write records a durable
receipt through current Control authorization. The extractor reads its confined
repository and checks each observed size and SHA-256 before extraction. The
repository's Linux directory descriptors and no-follow
opens reject symlink/traversal escape and non-regular files. Source content
changed after a manifest read is refused; the adapter also checks against the
manifest it actually observed. Keep the mounted repository read-only throughout
execution. No upload, listing, write or deletion endpoint is exposed.

## Rules and limits

Configured labelled fields match explicit `Label: value` lines. Missing and
unreadable placeholders remain unresolved; conflicting nonempty values are
preserved as failures. Email syntax and ISO calendar-date checks do not establish
identity or authenticity. Medical chronology includes only explicit valid dates
from source lines. Loan processing inventories supplied evidence and consistency;
it does not make a credit decision. Support, service-desk and joining-document
processing inventory sources; governed retrieval, memory, approval and actual
business effects must be verified by their own platform integrations.

The `structural-verilog-1` checklist accepts a deliberately narrow scalar ANSI
Verilog dialect: one module, explicit input/output/inout ports, scalar wires and
simple assignments. Unsupported syntax is rejected. Findings do not establish
timing closure, synthesis correctness, functional verification or silicon signoff.

Invoice requests supply exactly one of a structured `invoice` or
`invoiceDocumentId`, plus structured `purchaseOrder`, `receipts` and optional
`quantityTolerance`. A source invoice must use this explicit template:

```text
Invoice format: invoice-lines-1
Invoice ID: INV-0042
Purchase Order ID: PO-0042
Currency: USD
Line count: 2
Line: A | 2 | 0.10
Line: B | 5 | 20.00
```

The source template supports 1–99 declared rows. The declared count detects missing, duplicated or unreadable rows; incomplete
extraction does not release a partial financial match. Other layouts need review
or a separately implemented parser. Decimal strings permit up to twelve integer
digits and four fractional digits; negative values, exponents, NaN and Infinity
are rejected. Invoice and PO matching IDs and currencies must agree. Receipt
quantities are summed per SKU, missing receipts stay `needs review`, and quantity
or unit-price differences fail. Quantity tolerance is explicit and never relaxes
the exact unit-price comparison. No currency conversion, tax calculation,
payment, booking or approval occurs. Source invoice citations retain page, line,
exact extracted quote and original document SHA-256; structured PO/receipt
citations use JSON pointers.

| Boundary | Current limit |
|---|---|
| One document / total repository intake | 2 MiB / 2 MiB |
| Documents / PDF pages per document / extracted pages per intake | 16 / 10 / 32 |
| Decoded image or rendered page | 8 million pixels |
| Extracted text across an intake | 64,000 characters |
| Field checks / aliases per field / required document IDs | 32 / 8 / 16 |
| Structured invoice or PO lines / receipt documents / lines per receipt | 100 / 100 / 100 |
| Processor JSON request / processed JSON response | 3 MiB / 1 MiB |
| Repository manifest / source download | 16 KiB / 2 MiB |
| Inline tool request JSON string | 60,000 characters |
| Intake OCR deadline / each Tesseract orientation | 45 seconds / at most 10 seconds within remaining deadline |
| Concurrent processing requests | 2; additional processing requests receive HTTP 429 |

The tool has a 60-second invocation deadline and a 55-second processing route
timeout. Connection/header/body reads have a 15-second socket timeout. Resource
limits remain necessary for native PDF/image parsing. Output-size failure returns
an error rather than a truncated report. Denial, unavailable sources and changed
source bytes remain errors; no effect retry is implemented here.

## Component verification

Standard handler/rule tests require only Python. Repository tests intentionally
require Linux. Real scan acceptance requires the processor image's PDFium,
Pillow, Tesseract and English model; run the actual component gate explicitly:

```powershell
docker run --rm --network none --read-only --tmpfs /tmp:rw,noexec,nosuid,size=128m --cpus 4 --memory 768m --pids-limit 64 -e OLO_BUSINESS_SCAN_ACCEPTANCE=1 -e PYTHONDONTWRITEBYTECODE=1 -v D:/git/olo-toolgate:/workspace:ro -w /workspace --entrypoint python olo-business-processor:local -m unittest discover -s tests/ci -p "test_business*.py" -v
```

The scan tests generate synthetic raster-only PDFs, compare results with authored
ground truth, verify rotated-page handling and an unreadable field, and exercise
an invoice match, discrepancy and missing receipt. Handler tests cover malformed
framing/JSON, cross-tenant and duplicate-key denial, concurrency and response
limits. Linux tests exercise symlinks, changed content, FIFO rejection and an
intake directory replaced after opening. These component tests do not qualify
live Control grants, TLS deployment, OLO import/publish/Run, model quality,
memory/RAG services, trace backends or an end-to-end industry workflow. Execute
and retain those separate release acceptance results before publication claims.
