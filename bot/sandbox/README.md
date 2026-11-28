<!-- Copyright 2026 OLO Labs; SPDX-License-Identifier: Apache-2.0 -->
# Governed business sandbox

`olo.sandbox` provides an explicit human APPLY and status handoff after the ITSM
or onboarding starter's preparation/review run. The package calls this separate
Java 21 target through ToolGate's broker. Restart increments a seeded sandbox
service's stored generation and marks it running. Provisioning creates three
stored sandbox tasks: identity, equipment and payroll. It does not contact
operating systems, identity providers, payroll or equipment services.

The tools reject agent and delegated contexts. The current runtime does not
resume an approved suspended agent run into this APPLY phase. A human uses the
public OLO MCP tools after reviewing the proposal. Proposal, runtime state,
business result and target finality remain separate. No input field asserts
approval, chooses a tenant or supplies the acting user.

## Authority and trust

Deploy the `olo.sandbox` `.olotool` produced by `bot/tools/sandbox`. Its
`SandboxHost` global defaults to `business-sandbox`, port 8443. Store a private
generated credential under vault name `businesssandboxapikey`; the broker
injects `X-Api-Key`, and package code cannot read it. The current SDK and vault
name validators both accept this lower-case alphanumeric name.

Grant only selected human users the current actions and exact resources:

| Tool | Action | Resource |
| --- | --- | --- |
| `olo.sandbox/restart` | `sandbox.restart` | `sandbox://TENANT/service/SERVICE` |
| `olo.sandbox/provision` | `sandbox.provision` | `sandbox://TENANT/joiner/JOINER` |
| `olo.sandbox/inspect` | `sandbox.inspect` | `sandbox://TENANT/KIND/SUBJECT` |

Bindings, grants, tool placement and any approval policy still apply. Current
`package.deploy` authority must independently review each exact route intent.
Declare `restart` and `provision` as WRITE with fenced target
`{"targetId":"business-sandbox","targetClass":"SHARED_SQL"}`. Classify
`inspect` as READ, without a fenced target. Use the current
[route-intent](../../docs/operations/general-effect-intents.md) and
[target-fencing](../../docs/operations/generalized-target-fencing.md) contracts.

Every route sends canonical JSON with five members: `tenantId`, `actorId`,
`operation`, `subject`, `reference`. Pin the verified installation tenant and
selected human's actual user ID as reviewed constants. For writes, operation is
the constant `restart` or `provision`, subject resolves from `/serviceId` or
`/joinerId`, and reference resolves from `/reference`. For inspect, operation
resolves from `/kind`, subject from `/subject`, reference from `/reference`.
The package derives tenant and actor from verified invocation context. Current
intent templates cannot reconstruct invocation-principal expressions, so this
deployment deliberately selects one reviewed human operator. Do not add
caller-supplied actor arguments to generalize it. Deploy another appropriately
reviewed target/profile for a different operator.

The target additionally requires its private API credential and native seeded
subject ACL. Every write verifies an actual accepted executor mTLS session,
the exact request bytes/headers and the current signed effect grant using
`ControlTargetAuthorization`. The target's private machine credential has only
the exact role `toolgate-target-business-sandbox` for this tenant. It cannot be
replaced by an administrator, agent or executor credential. Reads use the
broker-injected API credential and native ACL; generic fenced reads are not
supported. The TLS listener requests a client certificate, and the write
verifier refuses writes without the accepted bound executor certificate.

## Fresh initialization and deployment

Build current SDK artifacts, then run:

```powershell
.\gradlew.bat --no-daemon -p bot/tools -PtoolgateJvmImageDigest=sha256:YOUR_QUALIFIED_JAVA_WORKER_DIGEST :sandbox:toolgatePackage
.\gradlew.bat --no-daemon -p bot/sandbox installDist
docker build -f bot/sandbox/Dockerfile -t olo-business-sandbox:local bot/sandbox/build/install/olo-business-sandbox
```

Use an empty dedicated PostgreSQL database. Run the installed application once
with schema-owner credentials and `--initialize /private/seed.json`. The seed
has the shape of [seed-example.json](seed-example.json), with the actual tenant,
user IDs and reviewed subject IDs. Initialization rejects every existing
nonempty public schema without modifying it. Runtime startup validates the
current markers and columns; it does not initialize, migrate or repair stores.

Then start the target with a separate `NOSUPERUSER NOBYPASSRLS` runtime role,
schema USAGE and SELECT/INSERT/UPDATE/DELETE on the initialized tables. Do not
grant CREATE, schema ownership or access to another installation's database.
All business, claim and outcome rows include tenant IDs, and forced PostgreSQL
RLS uses the transaction-local `olo.tenant` setting. All replicas for this target
share that same database. Retain claim, receipt and task tombstones.

Required environment settings:

| Setting | Meaning |
| --- | --- |
| `TENANT_ID` | Verified installation tenant |
| `DATABASE_URL`, `DATABASE_USER`, `DATABASE_PASSWORD` | Private PostgreSQL connection and runtime role |
| `TLS_KEYSTORE`, `TLS_TRUSTSTORE`, `TLS_STORE_PASSWORD` | PKCS12 server identity and installation CA/executor trust |
| `CONTROL_ORIGIN`, `CONTROL_ISSUER` | CA-verified HTTPS Control origin and its actual signed-grant issuer |
| `CONTROL_CREDENTIAL_FILE` | Mounted private target-role bearer credential; re-read for each exchange |
| `API_KEY_FILE` | Mounted private generated API key, 32–512 UTF-8 bytes, matching the vault secret |
| `TRANSPORT_BINDINGS_FILE` | Read-only installation-owned current service-profile binding |

After deployment review, obtain the actual immutable profile ID/digest from
Control and install a binding file:

```json
{
  "schemaVersion": 1,
  "origin": "https://business-sandbox:8443",
  "routes": {
    "restart": {"profileId": "ACTUAL_PROFILE_ID", "profileDigest": "sha256:ACTUAL_PROFILE_DIGEST", "routeId": "restart"},
    "provision": {"profileId": "ACTUAL_PROFILE_ID", "profileDigest": "sha256:ACTUAL_PROFILE_DIGEST", "routeId": "provision"}
  }
}
```

Never accept the binding from request headers or package arguments. Reinstall
the trusted binding when the reviewed deployment changes. Mount configuration
read-only, run the image as its nonroot user with a read-only filesystem,
bounded CPU/memory/PIDs and a small `/tmp` tmpfs, and keep the listener private.
The example image tag is a local build input, not a published or qualified
release identity. Production integration must record immutable image and JAR
digests and execute its own actual Control/target checks.

## Outcomes and recovery

The write target first durably acquires one shared claim, consumes it online at
Control, and wins `beginApply`. It then commits the business change, exact
result receipt, DONE state and pending outcome in one SQL transaction within the
original grant deadline. It retries only outcome-evidence delivery. An uncertain
commit or lost reply never invokes the business operation again. Confirmed
rollback records FAILED. Expired CLAIMED records may be abandoned by CAS;
APPLYING records are never automatically abandoned or replayed.

The same business reference and exact input returns the stored receipt after a
freshly authorized call; a different body under that reference is refused. A
second joiner reference still creates no duplicate tasks. Operator-owned
`prerequisites_complete=false` returns PENDING with no task creation. This is a
sandbox prerequisite state, not a document or policy approval inferred by a
model. Change that prerequisite only through the operator's independently
verified intake process, then submit a fresh reviewed operation reference.

`inspect` returns the stored reference result if present, or the actual current
subject state. A missing non-`status` reference returns UNKNOWN: absence of a
receipt does not establish that an uncertain effect can never apply. A positive
target-owned receipt can support independent reconciliation using the current
claim adapter; status reads never issue new write authority.

## Verification

Run `python bot/sandbox/test.py` to create an owned disposable PostgreSQL
container and execute the target storage tests, with UTC Java test time. These
exercise actual persisted service generations, concurrent joiner deduplication,
pending/unknown outcomes, native ACL refusal, tenant RLS, rollback and fresh-store
rejection. Run the `bot/tools` sandbox tests for wrapper authority and no-retry
behavior. These component checks do not establish actual Control approval,
broker transport, target TLS acceptance, finality delivery or end-to-end starter
qualification. Execute those against the exact reviewed deployed artifacts
before publishing this as an end-to-end qualified starter.
