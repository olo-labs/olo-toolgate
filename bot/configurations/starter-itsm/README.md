<!-- Copyright 2026 OLO Labs; SPDX-License-Identifier: Apache-2.0 -->
# IT service desk starter

Import the versioned `.olo`, provision the approved processor and intake repository,
then run `starter-itsm-sample` in the ordinary OLO Run UI. The exception input is
`starter-itsm-exception`. The corresponding `*-intake.json` manifests pin every source
by SHA-256; intake identifiers carry no authorization.

The graph executes `olo.business/process-intake`, then an analyst and evidence
reviewer. Its `report` and source locations come directly from the processor;
model commentary is retained separately in `analysis` and `review`. Neither
agent can alter the processor findings. Drafts require review.

See [starter operations](../../starters/README.md) for deployment, grants,
limits, tuning and the outstanding end-to-end acceptance requirements.
