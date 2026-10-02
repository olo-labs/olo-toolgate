# ADR 004: Embedded administration console

Status: accepted, 2026-10-02.

## Context

Module 02 exposes a versioned, signed-token administrative API. Module 03 needs
a simple management console without taking over authorization, provisioning
identities, distributing policies or enrolling clients.

## Decision

Build React/TypeScript with Vite and embed its immutable assets under `/console/`
in the existing Control image. Hash navigation avoids server fallback routes.
The API stays at `/api/control/v1/` on the same origin. No CORS or selectable
remote API URL is added. Existing Control ingress, replicas, readiness and
NetworkPolicy apply. A pinned Node stage builds assets; the runtime remains JRE.

The authentication shell accepts an externally issued access token over HTTPS
(loopback HTTP for development). It retains the token only in memory, clears it
on disconnect or a backend 401, aborts requests when sessions change, and never
decodes claims to decide authorization. A reader can browse; the backend rejects
unauthorized mutation attempts. No token enters storage, URLs, telemetry or
exported configuration. Interactive enterprise OIDC and local Quickstart
bootstrap need their own identity integration; this module does not claim them.

Generate typed operation routes from canonical OpenAPI and import models using
`@olo-labs/toolgate-contracts`. Backend validation, revisions, references,
idempotency and policy semantics remain authoritative. Bounded page counts are
labeled as page counts; no fictitious operational dashboard is introduced.

## Consequences

Control builds require an explicit production UI artifact with matching version.
The console and backend release atomically. Future Quickstart packaging reuses
the same assets behind its origin and supplies its own identity adapter; no
Quickstart service or bootstrap is implemented here. A separated UI repository
can consume the same published contract identity and OpenAPI bundle.
