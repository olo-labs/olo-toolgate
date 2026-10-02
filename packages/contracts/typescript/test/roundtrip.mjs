// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import test from 'node:test';
import { CONTRACT_SET_VERSION, CONTRACT_SET_NAME } from '../dist/index.js';

test('shared JSON corpus survives the JavaScript wire transport', () => {
  const fixtures = JSON.parse(readFileSync(new URL('../../../../tests/fixtures/contracts/v1/valid.json', import.meta.url)));
  for (const [name, fixture] of Object.entries(fixtures)) {
    assert.deepEqual(JSON.parse(JSON.stringify(fixture)), fixture, name);
  }
  assert.equal(readFileSync(new URL('../../VERSION', import.meta.url), 'utf8').trim(), CONTRACT_SET_VERSION);
  assert.equal(fixtures.ContractSet.name, CONTRACT_SET_NAME);
});
