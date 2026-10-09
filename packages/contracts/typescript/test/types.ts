// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
import type { EnterpriseDecision, DeploymentAssignment, MarketplaceRelease } from '../src/index.js';

const blocked = { decision: 'BLOCK', reason: 'NO_GRANT', obligations: [], witnesses: [], revision: 1, authorizationEpoch: 1, validUntilUnixMs: 1, diagnosticId: 'request' } satisfies EnterpriseDecision;
void blocked;
// @ts-expect-error Unknown decisions cannot become an authorization result.
const unknown: EnterpriseDecision['decision'] = 'UNKNOWN';
void unknown;
declare const release: MarketplaceRelease;
// @ts-expect-error Marketplace release evidence lacks organization deployment authorization.
const assignment: DeploymentAssignment = release;
void assignment;
