// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
import { test, expect } from '@playwright/test';

test('explicit local password-free mode enters the console and still uses protected tools', async ({page}) => {
  test.skip(process.env.UI_PASSWORDLESS_TEST !== '1', 'Requires explicit password-free Quickstart');
  await page.goto('/console/');
  await expect(page.getByRole('heading',{name:'Your organization, at a glance'})).toBeVisible();
  await expect(page.getByLabel('Password',{exact:true})).toHaveCount(0);
  await page.getByRole('link',{name:'Built-in tools and vault'}).click();
  await expect(page.getByRole('button',{name:'Run protected tool'})).toBeEnabled();
  await page.getByLabel('Arguments (JSON object)').fill('{"expression":"2+3*4"}');
  await page.getByRole('button',{name:'Run protected tool'}).click();
  await expect(page.getByRole('region',{name:'Tool result'})).toContainText('14');
});
