// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
import { defineConfig } from 'vitest/config';

export default defineConfig({
  define: { __APP_VERSION__: JSON.stringify('test') },
  test: {
    environment: 'jsdom', include: ['tests/*.test.ts', 'tests/*.test.tsx'],
    coverage: { provider: 'v8', include: ['src/api.ts'], thresholds: { statements: 80, branches: 80, functions: 80, lines: 80 } },
  },
});
