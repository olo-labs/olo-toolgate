# Component Map

| Component | Language | Owns | Must Not Own |
|---|---|---|---|
| Gateway | Rust | Runtime auth, routing, permits, runtime audit | Admin UI, package execution |
| Control Plane | Java | Org config, policies, fleet, approvals, imports | Arbitrary custom-code execution |
| Admin UI | React/TS | Human management experience | Security decisions |
| Endpoint Client | Rust | Package install, local runtime, HotFolder, protected execution | Org policy authoring |
| Quickstart | Combined release | Single-user all-in-one experience | HA guarantees |
| Drupal | PHP/Drupal | Public/community web UX | Package authority, signing |
| Marketplace API | Java | Package/publisher/version/submission domain | Untrusted code execution |
| Marketplace Worker | Java + sandbox | Scan/build/package/publish workflow | Public CRUD API |
| Signing Service/KMS | External | Marketplace signing key operations | Package business logic |

## Contributor Rule

If a change moves responsibility from one row to another, create an ADR before coding.
