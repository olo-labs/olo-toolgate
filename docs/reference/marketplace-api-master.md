# Marketplace Backend API & Drupal Publishing Integration
## Admin Panel Communication, Community Publishing, Artifact Distribution, Moderation, and Security Specification

**Status:** Build specification  
**Companion document:** `MCP_TEAM_ACCESS_CONTROL_ARCHITECTURE.md`  
**Audience:** Drupal engineers, Java/control-plane engineers, frontend engineers, security engineers, DevOps/SRE, endpoint-client engineers  
**Version:** 0.1  
**Date:** 2026-09-17

---

# 1. Purpose

This document specifies the backend contract between:

```text
Drupal Community Marketplace
        ↕
MCP Access Control/Admin Panel
        ↕
Organization Artifact Mirror
        ↕
Managed Endpoint Clients
```

and the reverse publishing flow:

```text
Custom/Internal Tool
        ↓
MCP Access Control/Admin Panel
        ↓
Sanitize + Package
        ↓
Drupal Marketplace API
        ↓
Moderation
        ↓
Signed Public Release
```

The design must support both:

1. **Pull/import**
   - An organization administrator discovers a community package.
   - Imports its signed schema/artifacts.
   - Reviews and configures it locally.
   - Mirrors approved artifacts.
   - Deploys it to managed clients.

2. **Create/publish**
   - A user/tool author creates or uploads a tool in the Admin Panel.
   - The organization tests it.
   - The publishing workflow converts it into a portable public package.
   - The author authenticates to the Drupal Marketplace.
   - Package and artifacts are uploaded.
   - Marketplace validates/moderates/signs/publishes it.

The Marketplace is a **registry and publication system**.

It is not:

- the organization authorization engine.
- the organization credential vault.
- the client deployment authority.
- a runtime dependency for MCP execution.

---

# 2. Architectural Principles

## 2.1 Organization Admin Decides Authority

Marketplace can say:

```text
Package community/pdf-tools@1.4.0 is published.
```

Only the organization can say:

```text
Finance clients may install it.
Finance AI agents may execute pdf.extract_text.
```

## 2.2 Credentials Never Cross into Marketplace

Marketplace packages define credential requirements.

Organization credentials remain in the organization Vault.

## 2.3 Public Package and Internal Package Are Different Artifacts

Publishing performs sanitization/transformation.

Never upload internal configuration directly.

## 2.4 Immutable Published Versions

Once:

```text
publisher/package@1.2.3
```

is published, its manifest/artifact digest set cannot be changed.

## 2.5 Asynchronous Large Operations

Binary upload, malware scan, static analysis, moderation and signing use asynchronous jobs.

## 2.6 Drupal Does Not Execute Submitted Code

Dynamic code analysis/testing runs in isolated workers outside normal Drupal PHP processes.

## 2.7 Registry Is Replaceable

Container 2 talks through a registry abstraction so private registries can be added later.

---

# 3. Components

## 3.1 Drupal Marketplace

Responsibilities:

- Accounts.
- Publisher namespaces.
- Package metadata.
- Version metadata.
- Search.
- Moderation.
- Reviews.
- documentation.
- API authorization.
- submission state.
- artifact metadata.
- signing metadata.
- advisories.
- webhooks.

## 3.2 Object Storage

Stores immutable uploaded artifacts:

```text
source archives
native binaries
scripts
JARs
package archives
SBOMs
scan reports where appropriate
```

Examples:

- S3.
- S3-compatible.
- cloud object storage.

Use versioned immutable object keys.

## 3.3 Marketplace Scan Workers

Responsibilities:

- archive safety.
- antivirus/malware scan.
- secret scanning.
- static analysis.
- SBOM validation/generation where appropriate.
- manifest/artifact consistency.
- binary metadata extraction.
- package permission checks.

Workers run isolated from Drupal.

## 3.4 Marketplace Signing Service

Responsibilities:

- sign approved package release envelope.
- expose public key set.
- rotate keys.
- protect private key in KMS/HSM when production maturity requires.

## 3.5 Container 2 Marketplace Client

Java service/library:

```text
backend-libs/marketplace-client
```

Responsibilities:

- OAuth.
- catalog.
- import.
- artifact download.
- signature verification.
- submission.
- artifact upload.
- submission status.
- advisory sync.
- webhook registration.

## 3.6 Organization Artifact Mirror

Recommended enterprise dependency:

- S3-compatible object store or artifact repository.

Approved package artifacts are copied here before fleet deployment.

## 3.7 Endpoint Client

Does not communicate with public Marketplace directly for managed package installation.

It receives desired state from the organization and downloads organization-approved artifacts.

---

# 4. Drupal Implementation Model

Use custom modules:

```text
mcp_marketplace_core
mcp_marketplace_package
mcp_marketplace_builder
mcp_marketplace_publisher
mcp_marketplace_review
mcp_marketplace_api
mcp_marketplace_security
mcp_marketplace_signing
mcp_marketplace_advisory
mcp_marketplace_webhook
```

Drupal core JSON:API may be used for ordinary entity-oriented read operations, but the package-registry workflow should use explicit custom versioned endpoints where transactional semantics, idempotency, upload sessions, or domain-specific error contracts are required.

---

# 5. Drupal Entity Model

Recommended custom content/entity structures.

## 5.1 Publisher

```text
id
namespace
display_name
owner_user_id
organization_name optional
website
source_profile
security_contact
verification_state
created_at
status
```

## 5.2 Package

```text
id
publisher_id
slug
display_name
summary
description
category
tags
license
source_repository
documentation_url
support_url
visibility
status
created_at
updated_at
```

Unique:

```text
publisher namespace + package slug
```

## 5.3 PackageVersion

```text
id
package_id
version
schema_version
package_type
manifest_object_key
manifest_sha256
release_envelope_object_key
release_sha256
moderation_state
compatibility
published_at
deprecated
revoked
created_by
created_at
```

Unique:

```text
package_id + version
```

Immutable after `PUBLISHED`.

## 5.4 PackageArtifact

```text
id
package_version_id
artifact_role
runtime_type
os
architecture
filename
object_key
sha256
size
media_type
scan_state
signature_metadata
sbom_object_key optional
created_at
```

## 5.5 Submission

```text
id
publisher_id
package_id optional
requested_version
status
idempotency_key
created_by
created_at
updated_at
failure_code
failure_message_safe
```

## 5.6 SubmissionArtifact

Tracks upload sessions and uploaded artifacts.

## 5.7 SecurityReview

```text
submission_id
automated_results
manual_reviewer
decision
comments
created_at
```

## 5.8 Advisory

```text
id
package_id
affected_versions
severity
status
summary
details
fixed_versions
published_at
signature
```

## 5.9 WebhookRegistration

```text
id
owner_user_id
target_url
event_types
secret_ref/internal encrypted secret
status
created_at
last_delivery
```

## 5.10 WebhookDelivery

```text
event_id
registration_id
attempt
status
http_status
next_attempt_at
created_at
```

---

# 6. API Base and Versioning

Base:

```text
https://marketplace.example.com/api/marketplace/v1
```

Versioning rules:

- Breaking changes require `/v2`.
- Additive optional fields may remain in `/v1`.
- Security-sensitive semantic changes require release notes and compatibility tests.
- Response contains schema/API version metadata where useful.

---

# 7. Authentication

Use OAuth 2.x/OIDC-compatible access tokens for Control Plane ↔ Marketplace operations.

The exact Drupal OAuth module/version must be validated at implementation time.

Recommended flows:

## 7.1 Interactive Account Link

Authorization Code + PKCE:

```text
Admin Panel
   |
   | open browser
   v
Drupal Marketplace
   |
   | login/consent
   v
callback to Control Plane
```

Token stored in organization Credential Vault.

## 7.2 Headless Automation

Use explicitly provisioned machine credentials with narrowly scoped permissions.

Do not reuse interactive user tokens for unattended publication pipelines unless intended.

---

# 8. OAuth Scopes

Recommended scopes:

```text
catalog.read
package.read
package.import

submission.create
submission.write
submission.read
submission.cancel

publisher.read
publisher.manage

advisory.read
webhook.manage
```

Publishing may require:

```text
submission.create
submission.write
```

Final public release remains controlled by Marketplace moderation; a publisher token does not grant reviewer approval.

---

# 9. OAuth Account-Link API Expectations

Control Plane stores:

```text
Marketplace account identity
publisher namespaces
access token reference
refresh token reference if used
granted scopes
expiry
```

Tokens are stored only in Credential Vault.

Admin UI:

```text
Marketplace Account

Connected as: rahul
Publishers:
  rahul
  example-org

Scopes:
  catalog.read
  package.import
  submission.create
  submission.write
```

---

# 10. Common API Headers

Requests:

```text
Authorization: Bearer <token>
Accept: application/json
Content-Type: application/json
X-Request-ID: <uuid/ulid>
Idempotency-Key: <uuid/ulid>      # mutating operation
User-Agent: mcp-access-control/<version>
```

Responses:

```text
X-Request-ID
X-Marketplace-API-Version
ETag where applicable
```

---

# 11. Common Error Envelope

```json
{
  "error": {
    "code": "PACKAGE_VERSION_NOT_FOUND",
    "message": "The requested package version does not exist.",
    "requestId": "01J...",
    "retryable": false,
    "details": {}
  }
}
```

Never expose:

- stack traces.
- SQL errors.
- filesystem paths.
- tokens.
- object-storage credentials.

---

# 12. Error Codes

Common:

```text
AUTHENTICATION_REQUIRED
INSUFFICIENT_SCOPE
PUBLISHER_NOT_ALLOWED
PACKAGE_NOT_FOUND
PACKAGE_VERSION_NOT_FOUND
PACKAGE_VERSION_EXISTS
SUBMISSION_NOT_FOUND
SUBMISSION_STATE_CONFLICT
INVALID_MANIFEST
INVALID_ARTIFACT
ARTIFACT_TOO_LARGE
HASH_MISMATCH
SECRET_DETECTED
SECURITY_VALIDATION_FAILED
VERSION_IMMUTABLE
RATE_LIMITED
TEMPORARILY_UNAVAILABLE
IDEMPOTENCY_CONFLICT
```

---

# 13. Catalog Search API

```text
GET /packages
```

Query parameters:

```text
q
publisher
type
runtime
os
architecture
risk
license
verifiedPublisher
securityReviewed
page[cursor]
page[size]
sort
```

Response:

```json
{
  "items": [
    {
      "id": "community/pdf-tools",
      "displayName": "PDF Tools",
      "publisher": {
        "namespace": "community",
        "verified": true
      },
      "latestVersion": "1.4.0",
      "packageType": "local-tool",
      "runtimeTypes": ["python"],
      "riskSummary": {
        "read": 2,
        "write": 0,
        "destructive": 0
      }
    }
  ],
  "nextCursor": "..."
}
```

---

# 14. Package Detail API

```text
GET /packages/{publisher}/{package}
```

Contains:

- public metadata.
- latest stable version.
- versions.
- source URL.
- trust metadata.
- aggregate permission/risk.
- advisory summary.

---

# 15. Package Version API

```text
GET /packages/{publisher}/{package}/versions/{version}
```

Response includes:

```text
manifest URL/reference
manifest hash
release envelope
artifact list
artifact hashes
Marketplace signature
signature key ID
compatibility
moderation/security metadata
```

Do not embed large binaries in JSON.

---

# 16. Marketplace Signing Key API

```text
GET /.well-known/mcp-access-marketplace-keys.json
```

or a versioned equivalent.

Expose public keys:

```json
{
  "keys": [
    {
      "kid": "marketplace-2026-01",
      "kty": "...",
      "alg": "...",
      "use": "package-signing",
      "publicKey": "..."
    }
  ]
}
```

Control Plane pins/trusts configured Marketplace root/key set and supports rotation.

---

# 17. Artifact Download

Package-version response contains artifact download descriptors.

Preferred:

```text
GET /packages/.../artifacts/{artifactId}/download
```

Marketplace returns a short-lived immutable object-storage URL or streams only small metadata artifacts.

Control Plane:

1. downloads.
2. enforces maximum size.
3. calculates SHA-256.
4. compares expected hash.
5. stores into quarantine.
6. runs organization scanning.
7. mirrors approved artifact.

---

# 18. Import Flow Sequence

```text
Admin UI
   |
   | Import
   v
Container 2
   |
   | GET version metadata
   v
Drupal Marketplace
   |
   | signed release metadata
   v
Container 2
   |
   | verify Marketplace signature
   |
   | download artifacts
   |
   | verify hashes
   |
   | organization scan
   |
   | mirror artifacts
   |
   | create local package version = UNDER_REVIEW
   v
Admin UI
```

Marketplace is never asked to activate the tool.

---

# 19. Local Import Record

Store:

```text
registry ID
Marketplace package ID
version
Marketplace manifest hash
Marketplace signature key ID
Marketplace release hash
local mirror artifact IDs
organization scan result
imported by
imported at
local state
```

---

# 20. Import Idempotency

Container 2 uses an operation key:

```text
registry + package + version
```

Repeated import requests should return the same logical import job if one is already active/completed.

---

# 21. Import Job States

```text
REQUESTED
FETCHING_METADATA
VERIFYING_MARKETPLACE_SIGNATURE
DOWNLOADING_ARTIFACTS
VERIFYING_ARTIFACTS
ORGANIZATION_SCANNING
MIRRORING
CREATING_LOCAL_PACKAGE
UNDER_REVIEW
COMPLETED
FAILED
```

UI displays safe progress.

---

# 22. Marketplace Publication Model

Publishing is submission-based.

A publisher cannot directly create a `PUBLISHED` PackageVersion.

Flow:

```text
Create Submission
Upload Manifest
Upload Artifacts
Complete Upload
Marketplace Validation
Automated Scans
Manual Review if required
Approval
Marketplace Signing
Publish Immutable Version
```

---

# 23. Create Submission

```text
POST /submissions
```

Request:

```json
{
  "publisher": "rahul",
  "package": "log-parser",
  "version": "1.0.0",
  "packageType": "local-tool",
  "schemaVersion": "1",
  "releaseNotes": "Initial release."
}
```

Headers:

```text
Idempotency-Key: ...
```

Response:

```json
{
  "submissionId": "sub_01J...",
  "status": "CREATED",
  "upload": {
    "mode": "presigned"
  }
}
```

---

# 24. Submission Ownership

Drupal verifies:

- token has `submission.create`.
- authenticated user may publish under requested namespace.
- package/version constraints.
- version does not already exist.

---

# 25. Upload Manifest

Preferred:

```text
PUT /submissions/{id}/manifest
```

For small manifests, JSON/multipart is acceptable.

Marketplace records hash.

Manifest is not final until submission is completed.

---

# 26. Declare Artifact

```text
POST /submissions/{id}/artifacts
```

Request:

```json
{
  "filename": "pdf-tools-linux-x64.tar.zst",
  "size": 3421021,
  "sha256": "...",
  "runtime": "native-binary",
  "os": "linux",
  "architecture": "x86_64",
  "mediaType": "application/zstd"
}
```

Response:

```json
{
  "artifactId": "art_...",
  "uploadUrl": "https://object-store/...signed...",
  "expiresAt": "..."
}
```

The Marketplace controls object key; client cannot choose arbitrary bucket path.

---

# 27. Artifact Upload

Control Plane uploads directly to object storage using short-lived presigned upload URL.

Requirements:

- exact/content-length limit.
- checksum metadata when provider supports it.
- no public ACL.
- upload expiry.
- quarantine prefix.

---

# 28. Complete Artifact

```text
POST /submissions/{id}/artifacts/{artifactId}/complete
```

Marketplace verifies object exists, size and digest.

Status becomes:

```text
UPLOADED
```

Artifact cannot be replaced after submission finalization.

---

# 29. Complete Submission

```text
POST /submissions/{id}/complete
```

Marketplace verifies:

- manifest present.
- all referenced artifacts present.
- checksums match.
- package identity/version match.
- required metadata complete.

Then transitions asynchronously to:

```text
VALIDATING
```

---

# 30. Submission Status

```text
GET /submissions/{id}
```

Response example:

```json
{
  "submissionId": "sub_...",
  "status": "SCANNING",
  "progress": {
    "phase": "malware-scan",
    "completed": 3,
    "total": 5
  },
  "issues": []
}
```

---

# 31. Submission States

Canonical:

```text
CREATED
UPLOADING
VALIDATING
SCANNING
CHANGES_REQUESTED
AWAITING_REVIEW
APPROVED
SIGNING
PUBLISHED
REJECTED
WITHDRAWN
FAILED
```

State transitions must be validated server-side.

---

# 32. Changes Requested

Reviewer may request:

```text
missing license
undeclared network destination
missing resource mapping
risk classification incorrect
embedded secret detected
AI description ambiguous
```

Publisher creates corrected submission content before final approval according to Marketplace workflow.

Published bytes are never edited.

---

# 33. Cancel/Withdraw Submission

```text
POST /submissions/{id}/withdraw
```

Allowed only in appropriate pre-publish states.

Published package version uses deprecation/advisory/revocation workflow instead.

---

# 34. Publication from Admin Panel

Admin UI:

```text
Tools
 -> My Tools
   -> Tool
     -> Publish to Marketplace
```

Wizard steps:

```text
1. Readiness
2. Publisher
3. Package identity
4. Version
5. Portability/Sanitization
6. Permissions
7. AI metadata
8. Artifacts
9. License/Source
10. Preview
11. Submit
12. Track review
```

---

# 35. Readiness API — Internal Control Plane

Before contacting Marketplace, Container 2 performs:

```text
schema validation
secret scan
organization-specific-value scan
runtime validation
permission declaration check
resource mapping check
license metadata check
artifact hash
SBOM presence
```

Result shown locally.

---

# 36. Sanitization Rules

Convert:

```text
secret://internal/foo
```

into:

```text
credentialRequirement:
  id: apiCredential
  type: bearer
```

Convert:

```text
https://jira.company.local
```

into:

```text
installSetting:
  id: jiraBaseUrl
  type: url
```

Convert absolute path:

```text
C:\Company\Exports
```

into logical:

```text
hotfolder://Finance
```

or a required install setting.

---

# 37. Sanitization Must Be Reviewable

Do not silently transform security-relevant values.

Show:

```text
Before
After
Reason
```

User approves transformations.

---

# 38. AI Metadata Publication

Marketplace receives portable default:

```text
description
useWhen
doNotUseWhen
examples
keywords
```

Organization-specific AI description override is not automatically published.

Wizard may offer:

```text
Use Marketplace Base Description
Use Organization Override
Write New Public Description
```

---

# 39. Source Publication

Options:

```text
source repository URL
source archive
binary only
source + binary
schema only
```

Binary-only packages receive stricter trust presentation/review.

---

# 40. Organization Approval Before External Publication

Enterprise option:

```text
requireInternalPublicationApproval = true
```

State:

```text
LOCAL_DRAFT
PUBLICATION_REVIEW
APPROVED_FOR_EXTERNAL_SUBMISSION
SUBMITTED_TO_MARKETPLACE
```

Internal reviewers may include:

- Security.
- Legal.
- Tool Admin.

---

# 41. Marketplace Webhook Registration

Control Plane can register:

```text
POST /webhooks
```

Request:

```json
{
  "url": "https://mcp-admin.example.com/api/v1/marketplace/webhooks/community",
  "events": [
    "submission.status.changed",
    "package.version.published",
    "package.version.revoked",
    "package.advisory.created"
  ]
}
```

Marketplace generates/signing secret securely and returns once or uses asymmetric signatures.

Prefer asymmetric webhook signing where operationally feasible.

---

# 42. Webhook Event Envelope

```json
{
  "eventId": "evt_01J...",
  "type": "submission.status.changed",
  "createdAt": "2026-09-17T...",
  "data": {
    "submissionId": "sub_...",
    "status": "AWAITING_REVIEW"
  }
}
```

Headers:

```text
X-Marketplace-Event-ID
X-Marketplace-Timestamp
X-Marketplace-Signature
```

---

# 43. Webhook Verification

Container 2 verifies:

- signature.
- timestamp tolerance.
- event ID replay.
- registered Marketplace identity.

Then stores event idempotently.

Never trust webhook body before verification.

---

# 44. Webhook Retry Policy

Marketplace retries on:

- timeout.
- 408.
- 429.
- 5xx.

Backoff with jitter.

Do not retry forever.

Dead-letter state visible to Marketplace operator.

---

# 45. Polling Fallback

Container 2 polls:

```text
GET /submissions/{id}
GET /advisories?since=...
```

when webhook is unavailable.

Webhooks are optimization, not correctness dependency.

---

# 46. Advisory API

```text
GET /advisories
GET /advisories/{id}
```

Filters:

```text
since
package
severity
cursor
```

Advisories are signed/covered by Marketplace trust.

---

# 47. Revocation API Representation

Package version detail contains:

```text
status: PUBLISHED | DEPRECATED | QUARANTINED | REVOKED
```

Control Plane never treats `REVOKED` as recommended/current.

---

# 48. Package Update Discovery

```text
GET /packages/{publisher}/{package}/versions?after=<installed>
```

Container 2 computes local security diff.

Marketplace may provide changelog, but local product determines local impact.

---

# 49. Security Diff Contract

The canonical package diff returns:

```json
{
  "toolsAdded": [],
  "toolsRemoved": [],
  "permissionsAdded": [],
  "permissionsRemoved": [],
  "networkDestinationsAdded": [],
  "filesystemScopesAdded": [],
  "credentialsAdded": [],
  "riskIncreased": [],
  "runtimeChanged": false,
  "schemaBreakingChanges": []
}
```

Same schema used by Marketplace reviewer and organization UI where possible.

---

# 50. Admin Panel Marketplace Pages

## 50.1 Marketplace Browse

Admin only by default.

## 50.2 Imported Packages

Shows organization copy/status.

## 50.3 Updates

Shows available versions and security diff.

## 50.4 Publishing

Shows local tools eligible for Marketplace.

## 50.5 Submissions

Shows:

```text
Draft
Uploading
Scanning
Review
Changes Requested
Published
Rejected
```

## 50.6 Marketplace Connection

Account, scopes, publisher namespaces, webhook health.

---

# 51. Admin Import UI

Steps:

```text
Package Overview
Trust & Source
Permissions
Artifacts
Compatibility
Import
Organization Scan
Configuration
Approval
Deployment
```

---

# 52. Organization Scan Interface

Container 2 should support pluggable organization scanners:

```text
antivirus
binary allowlist
license policy
SBOM vulnerability scan
internal policy engine
```

Scanning may be implemented by external service/job infrastructure.

Container 2 orchestrates and stores result, not necessarily scans large binaries itself.

---

# 53. Artifact Mirror Interface

Internal abstraction:

```text
putArtifact()
getArtifact()
createDownloadGrant()
deleteArtifactIfUnreferenced()
verifyArtifact()
```

Backends:

- S3.
- MinIO.
- Azure Blob.
- GCS.
- artifact repository later.

---

# 54. Endpoint Artifact Download API

Clients should not receive public Marketplace credentials.

Container 2 desired state references organization artifact IDs.

Client obtains:

```text
short-lived organization download grant
```

or accesses internal authenticated artifact repository.

---

# 55. Desired State API for Endpoint Client

Conceptual:

```text
GET /api/v1/client/desired-state
```

Authenticated using device identity.

Response:

```json
{
  "generation": 1842,
  "generatedAt": "...",
  "packages": [
    {
      "package": "community/pdf-tools",
      "version": "1.4.0",
      "organizationArtifactId": "oa_...",
      "artifactSha256": "...",
      "installPolicyId": "...",
      "deploymentId": "..."
    }
  ],
  "signature": "..."
}
```

---

# 56. Client Reported State API

```text
POST /api/v1/client/reported-state
```

Contains safe package status.

Batch state updates to reduce load.

---

# 57. Fleet Status Aggregation

Container 2 aggregates reported state:

```text
package/version
device group
deployment
```

UI provides:

```text
assigned
downloaded
installed
ready
failed
offline
outdated
```

---

# 58. Publication Package Generation API — Internal

Container 2 internal service:

```text
POST /api/v1/tools/{toolId}/publication-package
```

Creates a local draft artifact after sanitization review.

This is organization API, not Marketplace API.

---

# 59. Marketplace Publish Job — Internal

```text
POST /api/v1/marketplace/publications
```

Request:

```json
{
  "toolVersionId": "...",
  "registryId": "community",
  "publisher": "rahul",
  "package": "log-parser",
  "version": "1.0.0"
}
```

Returns organization job ID.

Job persists across Container 2 replicas.

---

# 60. Publication Job States — Control Plane

```text
DRAFT
READINESS_CHECK
WAITING_FOR_LOCAL_APPROVAL
CREATING_MARKETPLACE_SUBMISSION
UPLOADING
WAITING_FOR_MARKETPLACE
CHANGES_REQUESTED
PUBLISHED
REJECTED
FAILED
```

---

# 61. Multi-Replica Safety

Container 2 publication/import workers must use:

- database row locking/advisory locks.
- job lease.
- idempotency keys.

Two replicas must not publish the same version twice.

---

# 62. Retry Policy

Retry only retryable failures.

Examples retryable:

```text
network timeout
429
Marketplace 5xx
temporary object-store failure
```

Non-retryable:

```text
invalid manifest
insufficient scope
version already exists with different idempotency payload
security validation rejection
```

---

# 63. Rate Limiting

Marketplace should rate-limit:

- search abuse.
- auth.
- submission creation.
- uploads.
- review endpoints.
- webhook registration.

Rate limits can vary by role/publisher reputation.

---

# 64. Artifact Limits

Set explicit per-artifact and per-package limits.

Configurable examples:

```text
manifest <= 2 MB
individual artifact <= configured maximum
package total <= configured maximum
file count <= configured maximum
archive expansion ratio <= configured maximum
```

Do not leave unlimited defaults.

---

# 65. Archive Safety

Reject:

- absolute paths.
- `..` traversal.
- device files.
- symlinks escaping package root.
- excessive nesting.
- decompression bombs.
- duplicate/conflicting paths.

---

# 66. Secret Scanning

Scan:

- manifest.
- text source.
- scripts.
- documentation.
- config examples.

Flag likely:

- private keys.
- cloud access keys.
- API tokens.
- passwords.
- bearer tokens.
- internal credentials.

Binary entropy alone is not sufficient to label something a secret.

---

# 67. Internal Information Scanning

Before external publication, organization also scans for:

```text
private IPs
internal DNS
employee emails
tenant IDs
internal URLs
absolute user paths
internal project names configurable
```

This is separate from Marketplace's public submission scan.

---

# 68. Marketplace Moderation UI

Reviewer sees:

```text
Publisher
Package
Version
Diff from previous
Source
License
AI metadata
Permissions
Network
Filesystem
Credential requirements
Artifact types
Automated scan results
Warnings
```

Actions:

```text
Approve
Request Changes
Reject
Escalate Security Review
```

---

# 69. Marketplace Signing Flow

```text
Reviewer Approval
     |
     v
freeze canonical release content
     |
     v
calculate release digest
     |
     v
signing service
     |
     v
signature/release envelope
     |
     v
PackageVersion = PUBLISHED
```

No modifications after digest freeze.

---

# 70. Release Envelope

Conceptual:

```json
{
  "package": "community/pdf-tools",
  "version": "1.4.0",
  "schemaVersion": "1",
  "manifestSha256": "...",
  "artifacts": [
    {
      "id": "linux-x64",
      "sha256": "..."
    }
  ],
  "publishedAt": "...",
  "publisher": "community",
  "marketplace": "community-marketplace",
  "kid": "marketplace-2026-01",
  "signature": "..."
}
```

---

# 71. Organization Verification

Container 2 must verify release envelope locally using trusted Marketplace public key.

Do not delegate verification to Drupal response alone.

---

# 72. Package Trust Configuration

Registry configuration:

```text
registry ID
base URL
trusted signing keys/root
OAuth credential reference
allow publishers
require security review
allow binary-only
maximum risk
```

---

# 73. Multiple Registries

Same internal API supports:

```text
Community Drupal Marketplace
Private Enterprise Registry
Partner Registry
Air-Gapped Upload
```

The UI shows package source clearly.

---

# 74. Private Drupal Registry

Organizations may deploy a private Drupal Marketplace using the same modules/API later.

No code in the Control Plane should assume the public Marketplace hostname.

---

# 75. Drupal JSON:API Usage

Use core JSON:API primarily for public/read-oriented Drupal entities where its entity/bundle model is a good fit.

Examples:

```text
categories
public publisher profile
public documentation metadata
reviews
```

Use custom Marketplace REST endpoints for:

```text
submission creation
artifact upload sessions
finalize submission
signing workflow
webhook registration
security advisories
immutable package version retrieval contract
```

Reason:

- explicit domain semantics.
- idempotency.
- stable registry API independent of Drupal entity URL structure.
- better security review.
- easier future non-Drupal registry compatibility.

---

# 76. Drupal Authentication/Authorization

Drupal permissions protect administration/review routes.

Marketplace API authorization validates OAuth scopes plus Drupal user/package ownership.

Authorization checks include both:

```text
token scope
+
Drupal permission/entity ownership
```

---

# 77. CSRF

Browser Drupal administrative forms use Drupal's normal CSRF/session protections.

OAuth bearer API endpoints do not rely on browser cookie authentication for machine calls.

Do not mix cookie-authenticated mutating APIs with bearer APIs without clear CSRF design.

---

# 78. CORS

Marketplace API should not broadly allow `*` origins for credentialed requests.

Control Panel generally performs server-to-server calls, reducing browser CORS requirements.

Browser OAuth redirects use standard redirect URI allowlists.

---

# 79. Redirect URI Registration

Linked Control Plane instance supplies/registers exact callback origins according to Marketplace client-registration design.

Prevent arbitrary wildcard redirects.

---

# 80. Marketplace Client Registration

Options:

1. Predefined public client for standard MCP Access product with PKCE.
2. Dynamic client registration later.
3. Admin-created OAuth clients for enterprise automation.

Start with the simplest secure model compatible with deployments.

---

# 81. Publisher Namespace Ownership

Creating a namespace requires:

```text
unique slug
owner
terms acceptance
optional verification
```

Namespace transfer must be audited and protected.

---

# 82. Package Ownership

Package maintainers are explicit.

Avoid granting all publisher-namespace members irreversible full rights by default.

---

# 83. Publisher MFA

Require or strongly encourage MFA for:

- verified publishers.
- high-download publishers.
- Marketplace reviewers.
- administrators.

---

# 84. Security Contact

Every package publisher should provide a security contact or inherit from namespace.

Marketplace security advisories can be coordinated through it.

---

# 85. Package Deletion

Pre-publication draft/submission can be deleted according to retention.

Published package versions are immutable records and transition to status instead of silently disappearing.

---

# 86. Audit — Marketplace

Record:

```text
login/security events
publisher changes
namespace ownership
package creation
submission
artifact completion
review decisions
signing
publish
deprecate
revoke
advisory
webhook change
OAuth client changes
```

---

# 87. Audit — Organization

Record:

```text
Marketplace account linking
package search/import
signature result
organization scan
approval
artifact mirror
deployment
update
rollback
publish request
sanitization choices
Marketplace submission ID
Marketplace final version
```

---

# 88. Observability

Marketplace:

```text
API request rate
API latency
OAuth failures
submission count
scan queue depth
scan duration
signing duration
object-store errors
webhook delivery failures
publication failures
```

Control Plane:

```text
Marketplace call latency
import job state
publication job state
signature failures
download failures
advisory sync age
webhook verification failures
```

---

# 89. Marketplace Health

Control Panel shows:

```text
Community Marketplace
  API: Healthy
  Authentication: Connected
  Advisory Sync: 2m ago
  Webhook: Healthy
```

Marketplace failure does not mark Gateway unhealthy.

---

# 90. Caching

Container 2 may cache:

```text
search results
package metadata
public signing keys
advisory cursor
```

with TTL.

Never use stale cached data to bypass a known revocation fetched later.

---

# 91. Search Offline Behavior

If Marketplace unavailable:

```text
Marketplace browsing unavailable
```

Imported packages remain visible from local DB.

---

# 92. Publication Offline Behavior

Local custom tool can remain:

```text
READY_TO_PUBLISH
```

until Marketplace is available.

Do not lose the sanitized publication draft.

---

# 93. Package Preview API

Marketplace may expose a safe rendered preview, but local Control Plane should render canonical manifest itself too.

Never trust Marketplace HTML inside Admin Panel without sanitization.

---

# 94. Markdown Documentation

Marketplace package docs may use Markdown.

Drupal sanitizes rendering.

Control Panel must sanitize rendered remote Markdown/HTML.

No arbitrary script/embed execution.

---

# 95. Images and Assets

Package icons/screenshots:

- size limited.
- type validated.
- image processing/sanitization.
- no SVG script unless sanitized according to strict policy.
- object storage/CDN.

---

# 96. Build Metadata

Package may declare:

```text
build system
source commit
compiler/runtime version
build timestamp
provenance
```

Future reproducible build verification can use this.

---

# 97. SBOM

For executable packages, encourage/require CycloneDX or SPDX SBOM according to policy.

Marketplace displays:

```text
SBOM Available
```

not a generic safety judgment.

---

# 98. Vulnerability Scanning

Marketplace may scan SBOM/artifacts.

Results can become stale.

Display:

```text
Last scanned at
scanner version
known findings
```

Organization may re-scan after import.

---

# 99. Legal/License Metadata

API returns:

```text
declared license
source URL
redistribution notes from publisher
```

The product does not make legal determinations automatically.

---

# 100. Example — Publish Python Tool

Internal tool:

```text
finance.validate_csv
```

Author clicks:

```text
Publish to Marketplace
```

Control Plane:

1. Finds internal `Finance` HotFolder override.
2. Suggests converting it to generic `hotfolder://`.
3. Finds `secret://finance/api`.
4. Converts it to credential requirement `financeApi`.
5. Removes internal base URL.
6. Creates install setting `apiBaseUrl`.
7. Uses public AI description.
8. Packages Python source + lockfile.
9. Generates hashes/SBOM.
10. Runs readiness scan.
11. Creates Marketplace submission.
12. Uploads manifest/artifacts.
13. Shows Marketplace status.

---

# 101. Example — Import Python Tool

Admin selects:

```text
community/finance-validator@1.0.0
```

Control Plane:

1. Fetches signed release envelope.
2. Verifies Marketplace key.
3. Downloads source/artifact.
4. Verifies SHA-256.
5. Runs organization scan.
6. Mirrors package.
7. Creates `UNDER_REVIEW`.
8. Admin configures:
   - API base URL.
   - Vault credential.
   - Finance device group.
   - HotFolder path.
   - ALLOW policy.
9. Security admin approves.
10. Deployment begins.
11. Clients install isolated Python runtime/env.
12. Clients report `READY`.
13. Calls are authorized by Container 1.

---

# 102. Example — Import Native Binary

Same workflow, but:

- binary scan.
- architecture selection.
- code-signature metadata.
- no interpreter.
- exact artifact hash.

---

# 103. Example — Publish API-to-MCP

No executable required.

Control Plane converts internal API configuration into:

```text
portable endpoint template
install settings
credential requirement
input/output mapping
resource mapping
AI metadata
```

Marketplace publication is lightweight.

---

# 104. Example — Changes Requested

Marketplace response:

```json
{
  "status": "CHANGES_REQUESTED",
  "issues": [
    {
      "code": "UNDECLARED_NETWORK_DESTINATION",
      "path": "spec.permissions.network",
      "message": "The source references api.example.com but it is not declared."
    }
  ]
}
```

Admin Panel highlights issue and permits author to create corrected submission revision.

---

# 105. Publication Revision Model

Before publish, a submission may have revisions.

Once package version is published, fixing content requires a new package version.

---

# 106. Concurrency

Use optimistic version/ETag for editable Marketplace metadata.

Immutable published versions need no edit concurrency.

---

# 107. Idempotency Storage

Drupal stores:

```text
authenticated principal
endpoint
idempotency key
request hash
response reference
expiry
```

Same idempotency key with different request hash returns:

```text
IDEMPOTENCY_CONFLICT
```

---

# 108. Pagination

Use cursor pagination for package/search/audit/submission lists.

Avoid unstable page-number behavior at large scale.

---

# 109. Date/Time

All API timestamps:

```text
RFC 3339 / ISO-8601 UTC
```

Example:

```text
2026-09-17T17:00:00Z
```

---

# 110. Identifiers

Prefer opaque UUID/ULID IDs internally.

Public package identity remains human-readable:

```text
publisher/package@version
```

---

# 111. Content Hashing

Baseline:

```text
SHA-256
```

Define canonical serialization for manifest hashes.

Never hash pretty-printed ad hoc YAML and assume stable identity.

---

# 112. Canonical Manifest

Convert source YAML/JSON into canonical semantic representation before release hashing/signing.

Keep original author source separately if desired.

---

# 113. Marketplace Signature Algorithm

Choose through security ADR.

Requirements:

- modern.
- widely available in Rust/Java/PHP verification stack.
- supports key rotation.
- deterministic canonical payload.

Do not invent custom cryptography.

---

# 114. Drupal Queue/Worker Architecture

Drupal may enqueue:

```text
validation job
scan job
webhook delivery
search indexing
advisory processing
```

Heavy/untrusted analysis should be delegated to isolated workers rather than PHP request workers.

---

# 115. Malware Scanner Adapter

Define provider interface.

Potential providers:

- ClamAV.
- commercial scanner.
- cloud malware scanning service.

Do not hard-code one scanner into package schema.

---

# 116. Static Analysis Adapter

Runtime-specific scanners may be added:

```text
Python
JavaScript
PowerShell
native binaries
Java
```

Scan output is evidence, not a guarantee.

---

# 117. Marketplace Review Policy

Configurable thresholds:

```text
schema-only -> automated publish possible after checks
source script -> manual review optional/required by reputation
native binary -> stricter review
elevated tool -> security reviewer mandatory
```

Initial public Marketplace should prefer conservative manual review.

---

# 118. Publisher Reputation

May influence moderation queue priority, but must not bypass critical validation.

---

# 119. Marketplace Package Statistics

Do not expose organization-specific install identities.

Aggregate:

```text
downloads
favorites
ratings
```

Active-install telemetry is opt-in.

---

# 120. Webhook Privacy

Webhook payloads contain Marketplace metadata only.

Never include organization runtime tool calls or credentials.

---

# 121. Admin Panel UX — Import

Button wording:

```text
Import to Organization
```

not:

```text
Install Everywhere
```

Deployment is a separate deliberate step.

---

# 122. Admin Panel UX — Publish

Button:

```text
Publish to Community Marketplace
```

Then show:

```text
This will make the sanitized package publicly available if approved.
No organization credentials will be included.
```

---

# 123. Admin Panel UX — Marketplace Status

Three columns:

```text
COMMUNITY      ORGANIZATION       FLEET

Published      Approved           Ready 96%
1.4.0          1.3.0              128/133
```

---

# 124. Admin Panel UX — Submission Tracking

```text
community/log-parser 1.0.0

Marketplace status:
  Security Review

Uploaded:
  manifest     ✓
  source       ✓
  windows-x64  ✓

Checks:
  secret scan       ✓
  malware           ✓
  schema            ✓
  network review    ⚠

Reviewer:
  Waiting
```

---

# 125. Drupal UI — Package Creation

The Drupal site itself supports:

```text
Create Package
```

for authors who do not use the local Admin Panel.

It can build:

- schema-only MCP.
- API-to-MCP.
- local tool package metadata.
- upload executable/source artifacts.

The same canonical schema is generated.

---

# 126. Drupal UI — API-to-MCP Builder

Fields:

```text
Base URL/install setting
Authentication requirement
Operations
Inputs
Outputs
Resource mapping
Risk
AI description
Network destinations
```

No real organization credential is entered.

---

# 127. Drupal UI — Local Tool Upload

Fields:

```text
runtime
OS
architecture
entrypoint
artifact upload
input/output schema
permissions
resource mapping
AI description
runtime limits
```

---

# 128. Drupal UI — Source Code Editor

Optional future feature.

If offered, it stores source as part of submission but must not execute it inside Drupal.

For initial Marketplace, Admin Panel code builder can be the stronger authoring experience.

---

# 129. Drupal UI — Review

Reviewer sees semantic permission diff and scan results.

---

# 130. Marketplace API Compatibility Tests

CI should maintain contract tests between:

```text
Drupal Marketplace
Control Plane marketplace-client
canonical schema
```

Use test fixtures for:

- search.
- import.
- publication.
- changes requested.
- publish.
- advisory.
- revoked package.
- key rotation.

---

# 131. Local Development

Provide:

```text
mock-marketplace
```

or Drupal development profile with seeded:

```text
publisher
package
signed test package
advisory
```

Control Plane developers should not require the production public website.

---

# 132. Test Signing Keys

Development/test keys are separate and clearly marked.

Production client/control plane must not trust test key by default.

---

# 133. End-to-End Test — Import

CI:

1. Seed Drupal package.
2. Publish/sign.
3. Control Plane searches.
4. Import.
5. Verify.
6. mirror.
7. configure.
8. deploy to test client.
9. client ready.
10. execute authorized tool.

---

# 134. End-to-End Test — Publish

CI:

1. Create custom tool in Control Plane fixture.
2. Sanitize.
3. create submission.
4. upload artifacts.
5. Marketplace validates.
6. test reviewer approves.
7. package signs/publishes.
8. second organization imports.
9. deployment succeeds.

This test proves the community loop.

---

# 135. Security Tests

Test:

- forged Marketplace signature.
- replaced artifact.
- artifact hash mismatch.
- expired OAuth.
- insufficient publish scope.
- namespace takeover.
- webhook replay.
- idempotency reuse with altered payload.
- archive traversal.
- decompression bomb.
- embedded secret.
- private-IP leak.
- executable in wrong platform artifact.
- malicious Markdown.
- revoked signing key.
- stale advisory.
- Drupal upload RCE attempt.
- scan-worker escape attempt.

---

# 136. Disaster Recovery

Marketplace backup:

- Drupal DB.
- object storage.
- signing key metadata.
- HSM/KMS configuration.
- audit.
- publisher ownership.
- advisories.

Signing private key recovery follows separate secure key-management process.

---

# 137. Marketplace Key Rotation

1. Publish new public key.
2. Overlap trust window.
3. Begin signing new releases with new `kid`.
4. Keep old key available for verifying historical releases.
5. Revoke compromised key explicitly if necessary.

Do not require resigning immutable historical packages merely for routine rotation.

---

# 138. Compromised Signing Key

Emergency:

- publish trust advisory through independent configured channel where possible.
- mark affected signing key compromised.
- Control Planes refuse new imports signed only by compromised key.
- determine affected releases by signing key/time.
- require organization re-review/re-sign process.

---

# 139. Control Plane Registry Interface

Internal Java interface concept:

```java
interface MarketplaceRegistry {
    SearchResult search(SearchRequest request);
    PackageMetadata getPackage(PackageId id);
    PackageVersionMetadata getVersion(PackageVersionId id);

    ImportDescriptor prepareImport(PackageVersionId id);

    SubmissionRef createSubmission(PublicationMetadata metadata, String idempotencyKey);
    ArtifactUploadRef createArtifactUpload(SubmissionRef submission, ArtifactMetadata artifact);
    void completeArtifact(SubmissionRef submission, ArtifactRef artifact);
    void completeSubmission(SubmissionRef submission);
    SubmissionStatus getSubmissionStatus(SubmissionRef submission);

    AdvisoryPage getAdvisories(AdvisoryCursor cursor);
}
```

Do not leak Drupal entity IDs into domain logic.

---

# 140. Marketplace Client Resilience

Implement:

- connect timeout.
- request timeout.
- retry with jitter.
- circuit breaker.
- rate-limit backoff.
- token refresh.
- request IDs.
- metrics.

No infinite retries.

---

# 141. Marketplace Client Credential Handling

OAuth access/refresh token is a Vault credential reference.

The Java Marketplace adapter asks Credential Broker for token when making call.

Never persist token in logs or package records.

---

# 142. API Documentation

Drupal publishes:

```text
OpenAPI 3.x
```

for custom Marketplace API.

The canonical OpenAPI document is versioned and used to generate/test Java client where practical.

---

# 143. SDK Generation

A generated Java low-level HTTP client may be used, but wrap it in hand-written domain adapter.

Do not couple application services directly to generated classes.

---

# 144. Public API Stability

Marketplace API is a public product contract.

Document:

- deprecation window.
- version policy.
- error codes.
- rate limits.
- required scopes.
- maximum upload sizes.

---

# 145. Marketplace Backend Deployment

Recommended:

```text
WAF/CDN
   |
Drupal Web x N
   |
   +--> Drupal DB
   +--> Redis optional
   +--> Object Storage
   +--> Search
   +--> Job Queue
   +--> Isolated Scan Workers
   +--> Signing Service / KMS/HSM
```

Drupal web replicas should remain stateless.

---

# 146. Publication Artifact Lifecycle

Quarantine:

```text
submissions/<submission-id>/...
```

Published immutable:

```text
packages/<publisher>/<package>/<version>/...
```

Never overwrite published object keys.

---

# 147. Retention

Failed/rejected submission artifacts can expire after configured period.

Published artifacts retained according to Marketplace policy.

Revocation does not imply immediate byte deletion unless legally/security required.

---

# 148. Search Index Updates

Only `PUBLISHED` public package versions are searchable by default.

Quarantined/revoked status must propagate quickly.

---

# 149. Advisory Delivery

Use:

- API polling.
- webhook.
- optional RSS/security feed for humans.

Signed machine-readable API remains authoritative for Control Plane.

---

# 150. Final API Boundary

The complete system boundary is:

```text
COMMUNITY AUTHOR
     |
     | browser or MCP Access Admin
     v
DRUPAL MARKETPLACE
     |
     | immutable signed package API
     v
ORGANIZATION CONTROL PLANE
     |
     | local review / credentials / policy / mirror
     v
ENDPOINT CLIENT FLEET
     |
     | runtime authorization
     v
ACCESS GATEWAY
```

The design succeeds only if these responsibilities remain separated.

Drupal answers:

> What community package exists, who published it, what does it contain, and is this exact version an approved signed Marketplace release?

Container 2 answers:

> Does my organization trust/import it, how should it be configured, who gets it, which credentials may it use, and which devices should install it?

Endpoint Client answers:

> Is this organization-assigned artifact valid, installed, healthy, and runnable under the declared local restrictions?

Container 1 answers:

> Is this exact runtime action allowed right now for this user, agent, device, tool, operation and resource?

That separation should remain the implementation invariant.

---

# 151. Drupal Implementation Notes to Revalidate at Build Time

The Marketplace should rely on Drupal capabilities where they fit naturally, but the registry contract must stay independent from Drupal internals.

Current implementation notes as of 2026-09-17:

- Drupal core JSON:API provides RESTful CRUD around Drupal entities/bundles and is appropriate for selected entity-oriented/public catalog use cases.
- Drupal core Workflows and Content Moderation support configurable workflow states/transitions and can be reused for human-facing editorial/moderation experiences.
- The current stable Simple OAuth 6.1 line supports Drupal 10.3/11 and is covered by Drupal's security advisory policy.

However:

- Do not expose raw Drupal entity URLs as the permanent external registry contract for package publication.
- Keep `/api/marketplace/v1/...` as the stable domain API.
- Revalidate supported Drupal/PHP/module versions immediately before implementation and each major upgrade.
- Security-critical authentication/module versions must be pinned through Composer and monitored for advisories.

Authoritative references:

```text
https://www.drupal.org/docs/core-modules-and-themes/core-modules/jsonapi-module
https://www.drupal.org/docs/8/core/modules/workflows
https://www.drupal.org/docs/8/core/modules/content-moderation/overview
https://www.drupal.org/project/simple_oauth
```

---

# 152. Finalized Backend Topology

The Marketplace backend is no longer modeled as Drupal plus helper APIs.

The authoritative backend is a **stateless Java Marketplace API** with a separate **Marketplace Worker**.

Drupal is a first-party web client/CMS consuming the same API used by Control Plane and future clients.

Final topology:

```text
                         PUBLIC / COMMUNITY

                             CDN / WAF
                                |
                                v
                              Drupal
                                |
                                | OAuth 2.x / REST
                                v
                       +----------------------+
                       | Marketplace API      |
                       | Java / stateless     |
                       +-----+----------+-----+
                             |          |
                             |          +---------------------+
                             |                                |
                             v                                v
                       PostgreSQL                       Object Storage
                             |
                             v
                         Job Queue
                             |
                             v
                       +----------------------+
                       | Marketplace Worker   |
                       | Java / stateless     |
                       +----------+-----------+
                                  |
                                  v
                         Isolated Scan/Build
                                  |
                                  v
                         Signing Service/KMS
```

Consumers:

```text
Drupal --------------+
Control Plane --------+--> Marketplace API
CLI ------------------+
CI/CD ----------------+
Future Private Sync --+
```

No consumer should rely on Drupal entity endpoints as the core registry contract.

---

# 153. Marketplace API Owns the Domain Model

Marketplace API is authoritative for:

```text
publishers
namespaces
packages
draft metadata
package versions
submissions
artifacts
moderation state
publication jobs
immutable release records
advisories
webhooks
search/catalog metadata
```

Drupal stores only Drupal-specific presentation/community content when it does not belong in the Marketplace domain.

Examples that may remain Drupal-native:

```text
marketing landing pages
community articles
generic forum/community pages
site navigation
SEO content
```

Package authority remains Marketplace API.

---

# 154. Drupal Package UI Must Use Marketplace API

Drupal actions:

```text
List
Search
Create draft
Edit draft
Delete draft
Create version
Submit version
Request publication
View job state
Deprecate
Withdraw
View advisories
Manage publisher namespace
```

must call Marketplace API.

Do not perform direct Marketplace table mutation from Drupal PHP.

---

# 155. Marketplace API Application

Recommended:

```text
apps/marketplace-api
```

Technology:

- Java 21+.
- Quarkus.
- PostgreSQL.
- OpenAPI 3.x.
- OAuth/OIDC resource server.
- object-store client.
- queue/workflow client.
- search adapter.
- OpenTelemetry.
- Prometheus.
- Flyway.
- jOOQ or ORM according to implementation ADR.

The service is stateless and horizontally scalable.

---

# 156. Marketplace Worker Application

Recommended:

```text
apps/marketplace-worker
```

Technology:

- Java 21+ for workflow/orchestration consistency.
- Isolated external scan/build sandboxes for untrusted code.
- queue-driven.
- stateless worker replicas.

Worker responsibilities:

```text
validate
scan
build when applicable
generate/validate SBOM
create semantic permission diff
create canonical release
call signing service
publish immutable artifacts
```

Worker never exposes publisher-facing CRUD APIs.

---

# 157. Worker Trust Model

Assume every uploaded artifact is malicious.

Worker orchestration process itself must not directly execute uploaded code in its host namespace.

Dynamic execution:

```text
Marketplace Worker
    |
    v
Disposable Sandbox
```

Sandbox requirements:

```text
no production secrets
no customer secrets
no host Docker socket
no privileged capabilities
no internal network by default
no host mounts
ephemeral filesystem
resource limits
time limits
controlled build egress
```

---

# 158. Signing Service Boundary

Signing must not be performed by loading the Marketplace private key into Drupal or generic Worker process memory.

Preferred:

```text
release digest
     |
     v
KMS/HSM/signing service
     |
     v
signature
```

Marketplace API/Worker stores:

```text
kid
algorithm
signature
signed release digest
```

---

# 159. CRUD Rules

## Draft

```text
POST    create
GET     read
PATCH   edit
DELETE  delete
```

## Published Version

```text
GET     read
POST    deprecate
POST    withdraw
POST    quarantine
POST    revoke
```

No PATCH/DELETE of immutable published content.

Functional changes require a new semantic version.

---

# 160. Marketplace API Endpoints — Final Shape

## Catalog

```text
GET /api/marketplace/v1/packages
GET /api/marketplace/v1/packages/{publisher}/{package}
GET /api/marketplace/v1/packages/{publisher}/{package}/versions
GET /api/marketplace/v1/packages/{publisher}/{package}/versions/{version}
```

## Draft packages

```text
POST   /api/marketplace/v1/packages
PATCH  /api/marketplace/v1/packages/{publisher}/{package}
DELETE /api/marketplace/v1/packages/{publisher}/{package}
```

DELETE is only allowed for eligible draft/unpublished package state.

## Versions

```text
POST /api/marketplace/v1/packages/{publisher}/{package}/versions
```

## Publication

```text
POST /api/marketplace/v1/packages/{publisher}/{package}/versions/{version}/publish
```

Returns job.

## Jobs

```text
GET /api/marketplace/v1/jobs/{jobId}
```

## Submissions

```text
POST /api/marketplace/v1/submissions
GET  /api/marketplace/v1/submissions/{id}
POST /api/marketplace/v1/submissions/{id}/withdraw
```

## Artifacts

```text
POST /api/marketplace/v1/submissions/{id}/artifacts
POST /api/marketplace/v1/submissions/{id}/artifacts/{artifactId}/complete
```

## Advisories

```text
GET /api/marketplace/v1/advisories
GET /api/marketplace/v1/advisories/{id}
```

## Publishers

```text
GET  /api/marketplace/v1/publishers/{namespace}
POST /api/marketplace/v1/publishers
PATCH /api/marketplace/v1/publishers/{namespace}
```

## Webhooks

```text
POST   /api/marketplace/v1/webhooks
GET    /api/marketplace/v1/webhooks
DELETE /api/marketplace/v1/webhooks/{id}
```

---

# 161. Publication Job Contract

Request:

```text
POST /api/marketplace/v1/packages/{publisher}/{package}/versions/{version}/publish
```

Response:

```json
{
  "jobId": "pub_01K...",
  "status": "QUEUED"
}
```

Drupal then uses:

```text
GET /api/marketplace/v1/jobs/pub_01K...
```

Response:

```json
{
  "jobId": "pub_01K...",
  "type": "PACKAGE_PUBLICATION",
  "status": "SCANNING",
  "progress": 52,
  "currentStep": "malware-scan",
  "createdAt": "...",
  "updatedAt": "..."
}
```

---

# 162. Canonical Publication State Machine

```text
QUEUED
  |
  v
VALIDATING
  |
  v
SCANNING
  |
  v
BUILDING             # only if package requires build
  |
  v
GENERATING_SBOM
  |
  v
SECURITY_CHECK
  |
  v
AWAITING_REVIEW      # according to moderation policy
  |
  v
APPROVED
  |
  v
SIGNING
  |
  v
PUBLISHING
  |
  v
PUBLISHED
```

Failure/alternate:

```text
CHANGES_REQUESTED
REJECTED
FAILED
WITHDRAWN
QUARANTINED
```

State transitions are enforced in Marketplace API/domain layer.

---

# 163. Queue and Workflow

V1:

- PostgreSQL-backed durable job/workflow table acceptable.
- worker claims job with lease.
- heartbeat/lease expiry.
- retry count.
- next-attempt time.
- state machine validation.
- idempotency.

Scale options:

- NATS JetStream.
- RabbitMQ.
- Kafka.
- SQS.

The workflow state must remain reconstructable even if queue messages are duplicated.

---

# 164. Artifact Upload — Presigned

Large package bytes bypass Drupal and Java API body handling.

Flow:

```text
Drupal browser or Control Plane
       |
       | POST artifact metadata
       v
Marketplace API
       |
       | presigned URL
       v
Uploader ------------------> Object Storage
       |
       | complete
       v
Marketplace API
```

Marketplace API controls:

```text
bucket/prefix
object key
maximum size
upload TTL
expected content type
expected digest
```

---

# 165. Artifact Quarantine

New uploads land under:

```text
quarantine/submissions/{submissionId}/...
```

Only Marketplace Worker may promote an approved artifact into immutable release namespace.

Published:

```text
packages/{publisher}/{package}/{version}/...
```

Published keys are immutable.

---

# 166. Build/Scan Worker Types

Worker pipeline selects handlers by package class:

```text
DECLARATIVE
SOURCE_SCRIPT
SOURCE_BUILD
PUBLISHER_BINARY
```

### Declarative

No arbitrary execution.

### Source Script

Scan, package, optionally isolated test.

### Source Build

Build in isolated sandbox.

### Publisher Binary

Scan/provenance/SBOM handling without claiming Marketplace-built provenance.

---

# 167. Drupal UI Does Not Need to Know Worker Internals

Drupal displays state returned from Marketplace API.

Drupal should not enqueue scanner/build jobs directly.

Correct:

```text
Drupal -> Marketplace API -> durable workflow -> Worker
```

Incorrect:

```text
Drupal -> scanner queue
Drupal -> signer
Drupal -> object-store promotion
```

---

# 168. Package Listing/Editing from Drupal

Example:

```text
GET /packages?publisher=rahul
```

Create:

```text
POST /packages
```

Edit draft:

```text
PATCH /packages/rahul/log-parser
```

Delete draft:

```text
DELETE /packages/rahul/log-parser
```

For published versions:

```text
PATCH bytes = forbidden
DELETE immutable version = forbidden
```

---

# 169. Organization Control Plane Uses Same Catalog API

Control Plane does not scrape Drupal pages.

It calls:

```text
GET /api/marketplace/v1/packages
GET /api/marketplace/v1/packages/.../versions/...
```

and verifies Marketplace signatures locally.

---

# 170. Marketplace API Database Ownership

Only Marketplace API/domain/worker components should mutate Marketplace domain tables.

Drupal has its own Drupal database tables.

They may use the same physical PostgreSQL cluster if desired operationally, but should use separate schemas/databases and credentials.

Recommended:

```text
drupal DB/schema
marketplace DB/schema
```

with separate DB users.

---

# 171. Search

Marketplace API owns search semantics.

Backend options:

```text
PostgreSQL full text initially
OpenSearch/Solr later
```

Drupal search form calls Marketplace API.

Do not index unpublished/quarantined package versions as public catalog entries.

---

# 172. Moderation

Drupal provides moderation UI, but moderation actions call Marketplace API.

Example:

```text
POST /api/marketplace/v1/reviews/{submissionId}/approve
POST /api/marketplace/v1/reviews/{submissionId}/request-changes
POST /api/marketplace/v1/reviews/{submissionId}/reject
```

These endpoints require reviewer scopes/roles.

---

# 173. Marketplace Roles vs OAuth Scopes

Drupal human role and API token scope both apply.

Example:

```text
Drupal user role: Marketplace Reviewer
OAuth token scope: submission.review
```

Both must permit the action.

---

# 174. Marketplace API Security Boundary

Marketplace API is Internet-facing and must enforce:

- strict authentication.
- rate limits.
- payload limits.
- schema validation.
- idempotency.
- authorization.
- publisher ownership.
- content-type checks.
- audit.
- CSRF-independent bearer API.
- no trust based on Drupal headers unless cryptographically authenticated.

---

# 175. Drupal Authentication to Marketplace API

For logged-in community users, prefer delegated OAuth access where Marketplace API can identify the human actor.

Do not use one Drupal superuser service token for every user mutation.

Machine/background Drupal tasks may use a narrowly scoped service identity.

---

# 176. Control Plane Authentication to Marketplace API

When user links Marketplace account:

```text
Control Plane
  -> Authorization Code + PKCE
  -> Marketplace identity
```

Tokens stored in customer Credential Vault.

This permits:

```text
browse/import
publish under authorized namespace
track submissions
```

according to scopes.

---

# 177. Marketplace API Never Receives Customer Tool Credentials

A publication submission contains:

```text
credential requirement
```

not:

```text
credential value
```

Import/setup occurs locally in customer Vault.

---

# 178. Webhooks

Marketplace API owns webhook registration/delivery.

Drupal UI may expose settings but does not sign/deliver package webhooks itself.

Events:

```text
submission.status.changed
package.version.published
package.version.revoked
package.advisory.created
```

---

# 179. Idempotent Drupal UI

Drupal form submission should generate one operation/idempotency ID and reuse it on retry.

This avoids duplicate packages/versions/jobs after browser retry/timeouts.

---

# 180. API/Worker Observability

Marketplace API metrics:

```text
http_requests
oauth_failures
rate_limit
idempotency_conflict
package_create
publication_create
job_query
artifact_session
```

Marketplace Worker metrics:

```text
jobs_claimed
jobs_failed
job_duration
scan_duration
build_duration
sign_duration
sandbox_failure
artifact_promotion_failure
```

---

# 181. Failure Recovery

If Marketplace Worker crashes:

- job lease expires.
- another worker can resume/retry from safe stage.
- immutable/published objects are never overwritten.
- stage operations must be idempotent.

If Marketplace API restarts:

- jobs remain durable.
- upload sessions remain valid until expiry.
- Drupal can continue polling.

---

# 182. Signing Failure

If signing fails:

```text
state = FAILED or retryable SIGNING
```

Never mark release PUBLISHED without valid signature.

---

# 183. Object-Store Failure

Publication must not enter PUBLISHED until all immutable release objects are confirmed.

---

# 184. Scan Failure

Security scan failure blocks publication.

Scanner-unavailable behavior:

- retry.
- remain pending.
- do not assume pass.

---

# 185. Build Failure

Build-required package cannot publish built artifact if build failed.

Publisher may upload publisher-binary form only through a different explicitly declared release model if Marketplace policy permits it.

---

# 186. API OpenAPI Contract

Marketplace API must publish:

```text
/openapi/marketplace-v1.yaml
```

or equivalent.

Use it for:

- Java client generation.
- Drupal client integration tests.
- CLI SDK.
- contract tests.

The OpenAPI document is authoritative for HTTP shape; canonical JSON Schemas remain authoritative for package content.

---

# 187. Drupal Client Module

Add:

```text
mcp_marketplace_api_client/
```

Responsibilities:

- OAuth token use.
- Marketplace API HTTP client.
- request IDs.
- idempotency keys.
- retry on safe operations.
- form error mapping.
- job polling.
- response caching.

No domain database writes.

---

# 188. Drupal Publisher UI Module

`mcp_marketplace_publisher` uses the API client to:

- create/edit drafts.
- create versions.
- upload metadata.
- initiate publication.
- display status.
- handle changes requested.

---

# 189. Drupal Moderation UI Module

`mcp_marketplace_review` uses Marketplace API reviewer endpoints.

Marketplace API remains the state-machine authority.

---

# 190. Marketplace Worker Deployment

Run multiple replicas.

Example:

```text
marketplace-worker x 4
```

No public ingress.

Network allowlist:

```text
PostgreSQL
Queue
Object Store
Sandbox service
Signing service
approved scanner services
```

---

# 191. Marketplace API Deployment

Run multiple replicas.

Example:

```text
marketplace-api x 3
```

Behind:

```text
load balancer / ingress / WAF
```

No local persistent volume.

---

# 192. Separate Release Lifecycle

Marketplace platform deployment artifacts:

```text
marketplace-drupal
marketplace-api
marketplace-worker
```

are separate from customer product images:

```text
mcp-access-quickstart
mcp-access-gateway
mcp-access-control
```

Compatibility is tested but release cadence may differ.

---

# 193. Marketplace API Compatibility with Older Control Planes

API should remain backward compatible within documented support window.

Package manifests use explicit compatibility so an older Control Plane can browse but refuse unsupported package installation safely.

---

# 194. Sequence — Drupal Publishes a Package

```text
Publisher Browser
      |
      v
Drupal
      |
      | create/edit draft
      v
Marketplace API
      |
      | metadata stored
      v
PostgreSQL

Publisher uploads artifact:

Browser
  |
  | request upload
  v
Drupal
  |
  v
Marketplace API
  |
  | presigned URL
  v
Browser --------------------> Object Storage

Publisher clicks Publish:

Drupal
  |
  | POST publish
  v
Marketplace API
  |
  | create durable job
  v
Queue/Workflow
  |
  v
Marketplace Worker
  |
  +--> validate
  +--> scan
  +--> build if needed
  +--> SBOM
  +--> review state
  +--> signing service
  +--> immutable object promotion
  |
  v
Marketplace API state = PUBLISHED

Drupal polls/receives status and renders result.
```

---

# 195. Sequence — Control Plane Imports Package

```text
Organization Admin
      |
      v
mcp-access-control
      |
      | Marketplace OAuth
      v
Marketplace API
      |
      | signed version metadata
      v
Control Plane
      |
      | verify signature
      | download artifacts
      | verify hashes
      | organization scan
      | mirror internally
      v
Organization package = UNDER_REVIEW
```

---

# 196. Sequence — Control Plane Publishes Custom Tool

```text
Tool Author
      |
      v
mcp-access-control
      |
      | sanitize/publication transform
      v
Local Public Draft
      |
      | create Marketplace submission
      v
Marketplace API
      |
      | upload sessions
      v
Organization Control Plane ----------> Marketplace Object Storage
      |
      | complete
      v
Marketplace API
      |
      | publication workflow
      v
Marketplace Worker
      |
      v
Marketplace published version
```

Drupal is optional in this direct publication path; it can still show the user's published package afterward.

---

# 197. Sequence — Drupal Deletes a Draft

```text
Drupal
   |
   | DELETE draft
   v
Marketplace API
   |
   | verify ownership/state
   v
Delete/soft-delete eligible draft metadata
```

Published versions are not deleted through this route.

---

# 198. Sequence — Published Version Update

Not supported in place.

Instead:

```text
1.4.0 published
   |
   | create new version
   v
1.4.1 draft
   |
   v
publication workflow
```

---

# 199. Sequence — Revocation

Reviewer/security admin:

```text
Drupal security UI
   |
   v
Marketplace API
   |
   | create signed advisory/revocation state
   v
Catalog/API/Webhooks
   |
   v
Customer Control Planes
```

---

# 200. Final Responsibility Matrix

| Capability | Drupal | Marketplace API | Marketplace Worker | Control Plane | Endpoint Client | Gateway |
|---|---:|---:|---:|---:|---:|---:|
| Public pages | Yes | No | No | No | No | No |
| Community login UI | Yes | API support | No | No | No | No |
| Package metadata source of truth | No | Yes | Reads/updates workflow | Local imported copy | No | No |
| Draft package CRUD | UI | Yes | No | May call API | No | No |
| Artifact upload orchestration | UI | Yes | No | May call API | No | No |
| Binary storage | No | Metadata | Reads/promotes | Mirrors | Downloads org copy | No |
| Scan/build | No | Orchestrates | Yes via sandbox | Org scan only | No | No |
| Marketplace signing | No | Records | Requests | Verifies | Verifies via org package | No |
| Organization permissions | No | No | No | Yes | Enforces local assignment | Yes runtime |
| Organization credentials | No | No | No | Vault/bindings | Secure use | Broker/use |
| Fleet deployment | No | No | No | Yes | Yes | No |
| Runtime authorization | No | No | No | Policy management | Requests | Yes |

This matrix is the implementation boundary.
