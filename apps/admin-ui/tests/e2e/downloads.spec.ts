// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
import { test, expect } from '@playwright/test';
import AxeBuilder from '@axe-core/playwright';
import { createHash } from 'node:crypto';
import { readFile } from 'node:fs/promises';
import type { ClientDownloadManifest, ClientInstallerManifest } from '@olo-labs/toolgate-contracts';
test.skip(process.env.UI_TEST_DOWNLOADS_EXPECTED !== 'true', 'Requires a real three-platform release image');
test('anonymous home page downloads all native clients with matching checksums and accessible states', async ({ page, request }) => {
  await page.goto('/');
  await expect(page.getByRole('heading',{name:'Install ToolGate on your computer'})).toBeVisible();
  await expect(page.getByLabel('Access token')).toBeVisible();
  const response = await request.get('/api/public/v1/clients'); expect(response.status()).toBe(200);
  const manifest = await response.json() as ClientDownloadManifest;
  const labels = {WINDOWS:'Windows',MACOS:'macOS',LINUX:'Linux'};
  for (const artifact of manifest.artifacts) {
    const architecture=artifact.target.startsWith('aarch64')?'ARM64':'x64';
    const pending = page.waitForEvent('download'); await page.getByRole('link',{name:`Download ${labels[artifact.platform]} ${architecture}`,exact:true}).click();
    const download = await pending; expect(download.suggestedFilename()).toBe(artifact.filename);
    const path = await download.path(); const data = await readFile(path!);
    expect(createHash('sha256').update(data).digest('hex')).toBe(artifact.sha256);
    expect(data.length).toBe(artifact.bytes);
  }
  const installerResponse = await request.get('/api/public/v1/installers'); expect(installerResponse.status()).toBe(200);
  const installers = await installerResponse.json() as ClientInstallerManifest;
  for (const installer of installers.artifacts) {
    const suffix={WINDOWS:'setup.exe',MACOS:'dmg',LINUX:'run'}[installer.platform];
    const stable=`olo-toolgate-client-${installer.target}.${suffix}`;
    const architecture=installer.target.startsWith('aarch64')?'ARM64':'x64';
    const link=page.getByRole('link',{name:`Install ${labels[installer.platform]} ${architecture}`,exact:true});
    await expect(link).toHaveAttribute('href',`/api/public/v1/clients/${stable}`);
    const stableResponse=await request.get(`/api/public/v1/clients/${stable}`);
    expect(stableResponse.status()).toBe(200);
    expect(stableResponse.headers()['cache-control']).toBe('no-store');
    expect(stableResponse.headers()['content-disposition']).toBe(`attachment; filename="${stable}"`);
    expect(createHash('sha256').update(await stableResponse.body()).digest('hex')).toBe(installer.sha256);
    const pending = page.waitForEvent('download'); await link.click();
    const download = await pending; expect(download.suggestedFilename()).toBe(stable);
    const data = await readFile((await download.path())!);
    expect(createHash('sha256').update(data).digest('hex')).toBe(installer.sha256);
    expect(data.length).toBe(installer.bytes);
    const versioned=await request.get(`/api/public/v1/clients/${installer.filename}`);
    expect(versioned.status()).toBe(200);
    expect(versioned.headers()['cache-control']).toContain('immutable');
    expect(await versioned.body()).toEqual(data);
  }
  expect((await request.get('/api/public/v1/clients/olo-toolgate-client-unknown.setup.exe')).status()).toBe(404);
  await expect(page.getByText(/These development installers are unsigned/)).toBeVisible();
  expect((await new AxeBuilder({page}).withTags(['wcag2a','wcag2aa','wcag21aa']).analyze()).violations).toEqual([]);
  await page.setViewportSize({width:390,height:844});
  await expect(page.getByRole('link',{name:'Download Linux x64'})).toBeVisible();
  expect(await page.evaluate(()=>document.documentElement.scrollWidth<=window.innerWidth)).toBe(true);
});
