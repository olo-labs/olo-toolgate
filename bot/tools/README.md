<!-- Copyright 2026 OLO Labs; SPDX-License-Identifier: Apache-2.0 -->
# Business workflow tool packages

These modules use the current ToolGate Java SDK and `.olotool` format. RAG and
memory have distinct package identities; archive suffixes do not define a new
runtime, permission model or wire contract.

| Module | Package ID | Operations |
|---|---|---|
| `business-workflows` | `olo.business` | Bounded document processing through an approved HTTPS route |
| `rag` | `olo.rag` | Governed query, ingest, update, delete and sharing |
| `memory` | `olo.memory` | Governed read, append, delete, share and export |
| `sandbox` | `olo.sandbox` | User-bound sandbox restart, provisioning and inspection |

The memory/RAG adapters derive from the Apache-2.0 OLO Knowledge package sources
in `olo-mono` revision `59c53d7`. They are rebuilt against this repository's exact
SDK version and current schema. They do not contain the service databases,
embedding model, vector store, credentials or a private workflow executor.

## Build

From the repository root, publish the current Java SDK, install the matching
ToolGate CLI and expose its absolute path through `TOOLGATE_CLI`, then run:

```powershell
.\gradlew.bat --no-daemon -p bot/tools -PtoolgateSdkRepo=D:/git/olo-toolgate/build/sdk-repo -PtoolgateJvmImageDigest=sha256:YOUR_QUALIFIED_JAVA_WORKER_DIGEST build toolgatePackage
```

Each module writes its `.olotool` to `build/toolgate/` below that module. Use the
current product builder/CLI; renaming an older archive is not a build. Generated
descriptors declare exact SDK, protocol, descriptor generation and runtime
requirements. Hashes identify the actual emitted archive, not an assumed ID.
Packaging requires an explicit Java OCI worker digest. Distribute that exact
worker with the managed host and qualify its host/pool/image tuple independently
in Control. A digest parameter or a successful package build is not execution
qualification. Do not publish these starters with a native JVM fallback.

## Runtime boundaries

`olo.rag` and `olo.memory` are user-bound platform operations. They require the
existing ToolGate user credential, current root-resource grants and service
ACLs. Their adapters reject agent delegation; SERVICE is limited to the
existing scheduled `rag.update` contract. The broker owns mTLS and service
permit injection. Tools do not receive private certificate keys or service
permits. See [service authorization](../../docs/control-plane/olo-service-permits.md).

Deploy compatible memory and RAG listeners separately, with current transport
bindings, trusted profiles, fresh schemas, Control consumption, PostgreSQL and
the RAG vector/embedding dependencies. A successful SDK build or package import
does not establish those downstream services are compatible or qualified.

`olo.business/process-intake` records fenced document-job submission and
result-access receipts through the [document-job target](../document-target/README.md).
Its approved service profile fixes the destination, CA trust, target identity
and exact request intent; `businessprocessorapikey` remains broker-owned.
The extractor checks its installation-owned tenant and source hashes. It
executes no model, workflow, approval, account change or payment. Generic
unfenced HTTP processing cannot supply the target confirmation these graphs
require. Packaging does not qualify an isolated Java runtime.

Live import, authorization, target execution, denial and unknown-outcome checks
are required before advertising any of these integrations as accepted bots.
