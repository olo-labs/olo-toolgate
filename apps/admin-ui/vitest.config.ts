// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
import { defineConfig } from 'vitest/config';
import {readFileSync} from 'node:fs';

export default defineConfig({
  define: { __APP_VERSION__: JSON.stringify(readFileSync(new URL('../../VERSION',import.meta.url),'utf-8').trim()), __APP_BUILD__: JSON.stringify('local') },
  test: {
    environment: 'jsdom', include: ['tests/*.test.ts', 'tests/*.test.tsx'],
    coverage: { provider: 'v8', include: ['src/api.ts'], thresholds: { statements: 80, branches: 80, functions: 80, lines: 80 } },
  },
});
