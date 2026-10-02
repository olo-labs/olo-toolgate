// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
import { defineConfig } from '@playwright/test';
export default defineConfig({
  testDir: './tests/e2e', fullyParallel: false, workers: 1, retries: 0,
  timeout: 30000, reporter: [['line'],['json',{outputFile:'../../build/ui/browser-results.json'}]],
  outputDir: '../../build/ui/browser-output',
  use: { baseURL: process.env.UI_TEST_ORIGIN, browserName:'chromium', trace:'off', screenshot:'off', video:'off' },
});
