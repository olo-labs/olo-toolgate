// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
import type { PolicyDecision, DeploymentAssignment, MarketplaceRelease } from '../src/index.js';

const blocked = { decision: 'BLOCK', reason: 'NO_MATCH', policyVersion: '0.1.0-dev', requestId: 'r' } satisfies PolicyDecision;
void blocked;
// @ts-expect-error Unknown decisions cannot become an authorization result.
const unknown: PolicyDecision['decision'] = 'UNKNOWN';
void unknown;
declare const release: MarketplaceRelease;
// @ts-expect-error Marketplace release evidence lacks organization deployment authorization.
const assignment: DeploymentAssignment = release;
void assignment;
