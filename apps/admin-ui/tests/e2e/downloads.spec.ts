// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
import { test, expect } from '@playwright/test';
import AxeBuilder from '@axe-core/playwright';
import { createHash } from 'node:crypto';
import { readFile } from 'node:fs/promises';
import type { ClientDownloadManifest } from '@olo-labs/toolgate-contracts';
test.skip(process.env.UI_TEST_DOWNLOADS_EXPECTED !== 'true', 'Requires a real three-platform release image');
test('anonymous home page downloads all native clients with matching checksums and accessible states', async ({ page, request }) => {
  await page.goto('/');
  await expect(page.getByRole('heading',{name:'Install ToolGate on your computer'})).toBeVisible();
  await expect(page.getByLabel('Access token')).toBeVisible();
  const response = await request.get('/api/public/v1/clients'); expect(response.status()).toBe(200);
  const manifest = await response.json() as ClientDownloadManifest;
  for (const [platform,label] of [['WINDOWS','Windows'],['MACOS','macOS'],['LINUX','Linux']] as const) {
    const artifact = manifest.artifacts.find(a=>a.platform===platform && a.target.startsWith('x86_64'))!;
    const pending = page.waitForEvent('download'); await page.getByRole('link',{name:`Download ${label} x64`}).click();
    const download = await pending; expect(download.suggestedFilename()).toBe(artifact.filename);
    const path = await download.path(); const data = await readFile(path!);
    expect(createHash('sha256').update(data).digest('hex')).toBe(artifact.sha256);
    expect(data.length).toBe(artifact.bytes);
  }
  expect((await new AxeBuilder({page}).withTags(['wcag2a','wcag2aa','wcag21aa']).analyze()).violations).toEqual([]);
  await page.setViewportSize({width:390,height:844});
  await expect(page.getByRole('link',{name:'Download Linux x64'})).toBeVisible();
  expect(await page.evaluate(()=>document.documentElement.scrollWidth<=window.innerWidth)).toBe(true);
});
