// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
import { test, expect, type Page } from '@playwright/test';
import AxeBuilder from '@axe-core/playwright';
import { mkdirSync, readFileSync } from 'node:fs';
const credentials = JSON.parse(readFileSync(process.env.UI_TEST_CREDENTIALS!, 'utf8')) as Record<string,string>;
const origin = process.env.UI_TEST_ORIGIN!;
async function login(page:Page, kind = 'admin') {
  await page.goto('/console/');
  await page.getByLabel('Access token').fill(credentials[kind]);
  await page.getByRole('button',{name:'Connect to workspace'}).click();
}
async function navigate(page:Page, label:string) { await page.getByRole('navigation').getByRole('link',{name:label,exact:true}).click(); await expect(page.getByRole('heading',{name:label === 'Overview' ? 'Your organization, at a glance' : label,exact:true})).toBeVisible(); }
async function accessible(page:Page) { expect((await new AxeBuilder({page}).withTags(['wcag2a','wcag2aa','wcag21a','wcag21aa']).analyze()).violations).toEqual([]); }

test('embedded signed-token console, real CRUD, revisions, accessibility and no token persistence', async ({page,request}) => {
  const errors:string[] = []; page.on('pageerror',error => errors.push(error.name));
  const shell = await request.get('/console/'); expect(shell.status()).toBe(200);
  expect(shell.headers()['content-security-policy']).toContain("script-src 'self'"); expect(shell.headers()['cache-control']).toContain('no-store');
  expect(shell.headers()['x-content-type-options']).toBe('nosniff');
  const html = await shell.text(); const js = html.match(/src="([^"]+\.js)"/)![1];
  const asset = await request.get(js); expect(asset.status()).toBe(200); expect(asset.headers()['cache-control']).toContain('immutable');
  expect((await request.get('/api/control/v1/users')).status()).toBe(401);
  await page.goto('/console/'); await accessible(page); await page.keyboard.press('Tab'); await expect(page.getByRole('link',{name:'Skip to content'})).toBeFocused();
  await login(page); await expect(page.getByRole('heading',{name:'Your organization, at a glance'})).toBeVisible();
  await expect(page.locator('.stat')).toHaveCount(6); await accessible(page);
  mkdirSync('../../build/ui',{recursive:true}); await page.screenshot({path:'../../build/ui/overview-desktop.png',fullPage:true});
  for (const section of ['Teams','Tools','Policies','Clients','Agents']) { await navigate(page,section); await expect(page.getByRole('table')).toBeVisible(); await accessible(page); }
  await navigate(page,'Policies'); await page.getByRole('button',{name:'Browser policy'}).click(); await expect(page.getByText('Stored decision',{exact:true})).toBeVisible(); await accessible(page);
  await navigate(page,'Users'); await page.getByRole('button',{name:'Add user',exact:true}).click();
  await expect(page.getByRole('heading',{name:'Add directory user'})).toBeFocused();
  await page.getByLabel('Identifier',{exact:true}).fill('browser:user/item'); await page.getByLabel('Display name').fill('Browser user'); await accessible(page);
  await page.getByRole('button',{name:'Save user'}).click(); await expect(page.getByRole('button',{name:'Browser user',exact:true})).toBeVisible();
  await page.getByRole('button',{name:'Browser user',exact:true}).click();
  // A second real writer updates the record before this editor saves.
  const concurrent = await request.put('/api/control/v1/users/browser%3Auser%2Fitem',{headers:{Authorization:`Bearer ${credentials.admin}`,'If-Match':'"1"','Idempotency-Key':'concurrent-browser-update'},data:{id:'browser:user/item',name:'Other writer',enabled:true,revision:1}});
  expect(concurrent.status()).toBe(200);
  await page.getByLabel('Display name').fill('My edit'); await page.getByRole('button',{name:'Save user'}).click();
  await expect(page.getByRole('alert')).toContainText('Refresh before trying again'); await accessible(page);
  await page.getByRole('button',{name:'Reload current record'}).click(); await expect(page.getByLabel('Display name')).toHaveValue('Other writer');
  await page.getByLabel('Display name').fill('Saved user'); await page.getByRole('button',{name:'Save user'}).click(); await expect(page.getByRole('button',{name:'Saved user',exact:true})).toBeVisible();
  await page.getByRole('button',{name:'Saved user',exact:true}).click(); await page.getByRole('button',{name:'Delete user',exact:true}).click(); await page.getByRole('button',{name:'Confirm delete'}).click(); await expect(page.getByRole('button',{name:'Saved user',exact:true})).toHaveCount(0);
  const storage = await page.evaluate(() => ({local:localStorage.length,session:sessionStorage.length,cookies:document.cookie})); expect(storage).toEqual({local:0,session:0,cookies:''});
  expect(new URL(page.url()).origin).toBe(origin); expect(page.url().includes(credentials.admin)).toBe(false);
  await page.setViewportSize({width:390,height:844}); await navigate(page,'Overview'); await expect(page.locator('.stat')).toHaveCount(6); await accessible(page);
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= window.innerWidth)).toBe(true);
  await page.screenshot({path:'../../build/ui/overview-mobile.png',fullPage:true});
  await page.reload(); await expect(page.getByLabel('Access token')).toBeVisible(); expect(errors).toEqual([]);
});

test('real reader denial, session expiration, wrong signature and empty directory', async ({page}) => {
  await login(page,'reader'); await expect(page.getByRole('navigation')).toBeVisible(); await navigate(page,'Users');
  await page.getByRole('button',{name:'Add user',exact:true}).click(); await page.getByLabel('Identifier',{exact:true}).fill('reader-cannot-create'); await page.getByLabel('Display name').fill('Reader user'); await page.getByRole('button',{name:'Save user'}).click(); await expect(page.getByRole('alert')).toContainText('permission'); await accessible(page);
  await page.getByRole('button',{name:'Disconnect'}).click(); await login(page,'invalid'); await expect(page.getByRole('alert')).toContainText('session ended');
  await login(page,'expired'); await expect(page.getByRole('alert')).toContainText('session ended');
  await login(page,'empty'); await expect(page.getByRole('navigation')).toBeVisible(); await navigate(page,'Teams'); await expect(page.getByRole('heading',{name:'No teams on this page'})).toBeVisible(); await accessible(page);
});

test('real cursor paging with bounded records', async ({page}) => {
  await login(page,'paging'); await expect(page.getByRole('navigation')).toBeVisible(); await navigate(page,'Users');
  await expect(page.getByText('50 records on this page',{exact:true})).toBeVisible();
  await page.getByRole('button',{name:'Next page'}).click(); await expect(page.getByText('1 records on this page',{exact:true})).toBeVisible();
  await expect(page.getByRole('button',{name:'Next page'})).toBeDisabled(); await page.getByRole('button',{name:'Previous page'}).click(); await expect(page.getByText('50 records on this page',{exact:true})).toBeVisible();
  await page.route('**/api/control/v1/users?*', route => route.continue({headers:{...route.request().headers(),authorization:`Bearer ${credentials.expired}`}}));
  await page.getByRole('button',{name:'Refresh',exact:true}).click(); await expect(page.getByLabel('Access token')).toBeVisible(); await expect(page.getByRole('alert')).toContainText('session ended');
});
