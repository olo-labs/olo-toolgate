// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
import { defineConfig } from 'vite';
import { readFileSync } from 'node:fs';

export default defineConfig({
  base: '/console/',
  define: {
    __APP_VERSION__: JSON.stringify(readFileSync(new URL('../../VERSION', import.meta.url), 'utf8').trim()),
    // Release images pass the CI run number and commit; local builds say so.
    __APP_BUILD__: JSON.stringify(process.env.TOOLGATE_BUILD || 'local'),
  },
  build: { outDir: 'dist', sourcemap: false, manifest: true },
  // Development only. Production is embedded at the Control origin.
  server: { proxy: { '/api/control/v1': 'http://127.0.0.1:8082' } },
});
