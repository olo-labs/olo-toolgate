<!-- Copyright 2026 OLO Labs; SPDX-License-Identifier: Apache-2.0 -->
# Governed document jobs

This target records source-pinned document-job submission and result-access
receipts using ToolGate's current generic fenced-target contract. Each write
requires the accepted executor mTLS session, current Control authorization,
exclusive PostgreSQL claim ownership and a commit before the original grant
deadline. Job/receipt changes, claim completion and the outcome outbox commit
together. An uncertain commit remains APPLYING and is never replayed.

Document extraction is asynchronous bounded computation over read-only sources.
It is separate from the short authorization window for inserting a job or
recording access to its result. The native worker performs no payment,
communications, provisioning, model call or other business action. A successful
submission receipt confirms only job acceptance. The tool returns a report only
after a later fenced access observes READY and the same source-manifest digest.

## Components and input

Deploy this Java 21 target as `business-processor:8443` and the confined
[Python extractor](../business/README.md) as `business-extractor:8443`.
Only the target calls the extractor. Both source inspection and processing
recheck every declared byte count and SHA-256. The extractor has no write
endpoint. Its inspection capacity is independent of its two OCR slots.

`POST /v1/submit` and `/v1/result` accept exactly `tenantId`, `useCase`, `intakeId`.
The tenant is installation-owned; Control reconstructs it as a reviewed constant
and the other two fields from the admitted arguments. Both routes are WRITE
with fenced target `{"targetId":"document-jobs","targetClass":"SHARED_SQL"}`.
The package uses broker-owned `processor-key` API-key authentication and receives
no credential or signing material. Keep both listeners on a private deployment
network and mount credentials read-only.

Submission deduplicates by tenant/use case/intake ID. Intakes are immutable;
changed source versions cannot reuse an existing job identity. Each submitted
or observed response also creates a receipt under the unique consumed
authorization nonce. Only successful current target claims can create receipts.
The SDK never retries submission or an uncertain observation. Its bounded result
wait uses distinct effect keys, and remains subject to the original invocation
deadline and current grants.

## Installation

Build current SDK artifacts, then:

```powershell
.\gradlew.bat --no-daemon -p bot/document-target installDist
docker build -f bot/document-target/Dockerfile -t olo-document-target:local bot/document-target/build/install/olo-document-target
```

Use a fresh dedicated PostgreSQL database. Run the installed application with
`--initialize` once as its schema owner. Initialization refuses nonempty public
schemas; startup validates the current job and claim markers/columns. No older
store is converted or removed. Run the listener with a separate NOSUPERUSER
NOBYPASSRLS role with schema USAGE and SELECT/INSERT/UPDATE/DELETE on these
tables, without CREATE or ownership. Forced RLS uses transaction-local
`olo.tenant`; each database belongs to one configured installation tenant.

Required environment settings follow the
[sandbox target's TLS and Control custody](../sandbox/README.md): `TENANT_ID`,
`DATABASE_URL`, `DATABASE_USER`, `DATABASE_PASSWORD`, `TLS_KEYSTORE`,
`TLS_TRUSTSTORE`, `TLS_STORE_PASSWORD`, `CONTROL_ORIGIN`, `CONTROL_ISSUER`,
`CONTROL_CREDENTIAL_FILE`, `API_KEY_FILE`, `TRANSPORT_BINDINGS_FILE`.
The machine credential requires only `toolgate-target-document-jobs` for this
tenant. It is neither a human, administrator nor executor credential.
Also supply `PROCESSOR_ORIGIN=https://business-extractor:8443` and
`PROCESSOR_API_KEY_FILE` for the extractor. The truststore must contain the
installation's Control TLS CA, its tenant DEVICE_CA publication for executor
certificates, and the independently reviewed extractor TLS CA. The TLS-server
CA and tenant device-signing CA are separate trust domains; obtain each from
the verified installation publication/custody.
HTTP redirects, plaintext fallback and caller-selected extractor origins are refused.

After deployment review, install the actual immutable profile bindings:

```json
{
  "schemaVersion": 1,
  "origin": "https://business-processor:8443",
  "routes": {
    "submit": {"profileId": "ACTUAL_PROFILE_ID", "profileDigest": "sha256:ACTUAL_PROFILE_DIGEST", "routeId": "submit"},
    "result": {"profileId": "ACTUAL_PROFILE_ID", "profileDigest": "sha256:ACTUAL_PROFILE_DIGEST", "routeId": "result"}
  }
}
```

## Limits, recovery and verification

There are at most 1,024 retained jobs per installation database, two computation
workers and a 120-second computation lease. Extractor calls have a 55-second
timeout and one-MiB response limit. Source inspection has a one-second timeout;
SQL lock and statement timeouts are bounded independently. PostgreSQL tables
retain job identities and receipt tombstones. Capacity exhaustion requires an
explicit operator retention decision; the target never deletes jobs to retry.

A lost computation becomes INTERRUPTED after its lease expires. Failed or
interrupted jobs remain terminal and cannot be resubmitted automatically.
Queued jobs are already accepted native computation tasks. Recovery retries
outcome delivery only and abandons expired CLAIMED target grants through CAS;
it cannot abandon or reapply an uncertain APPLYING effect. This implementation
does not establish distributed worker availability or production SLOs.

Run `python bot/document-target/test.py` for actual PostgreSQL, forced RLS,
source-version refusal, duplicate/concurrent submission, expiry and atomic
claim/job/outcome checks. These storage checks do not establish live Control
acceptance. Qualify real mTLS, current grant denial, receipts, source changes,
unknown outcomes and ordinary workflow Run before publishing runnable starters.
