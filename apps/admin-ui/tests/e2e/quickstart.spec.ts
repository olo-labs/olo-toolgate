// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
import { test, expect } from '@playwright/test';
import AxeBuilder from '@axe-core/playwright';

test('real Quickstart password, non-HA status, protected compute and vault names',async({page,request})=>{
  test.skip(!process.env.QUICKSTART_PASSWORD,'Requires actual composed Quickstart fixture');
  await page.goto('/console/');await expect(page.getByRole('status')).toContainText('Non-HA');
  const configuration=await request.get('/api/public/v1/clients/configuration');expect(configuration.status()).toBe(200);
  await expect(page.getByLabel('Gateway URL',{exact:true})).toHaveValue((await configuration.json()).serverUrl);
  await expect(page.getByRole('button',{name:'Copy Gateway URL'})).toBeVisible();
  await page.getByLabel('Password',{exact:true}).fill(process.env.QUICKSTART_PASSWORD!);
  await page.getByRole('button',{name:'Connect to workspace'}).click();
  await expect(page.getByRole('heading',{name:'Your organization, at a glance'})).toBeVisible();
  await page.locator('nav summary').filter({hasText:'Devices'}).click();
  await page.getByRole('link',{name:'Clients',exact:true}).click();
  await expect(page.getByRole('columnheader',{name:'Connection',exact:true})).toBeVisible();
  const checkedInClient=page.getByRole('row').filter({hasText:'quickstart-test-device'});
  await expect(checkedInClient.locator('.client-connection.online')).toHaveText('Connected');
  expect((await new AxeBuilder({page}).analyze()).violations).toEqual([]);
  await page.locator('nav summary').filter({hasText:'Tools'}).click();
  await page.getByRole('link',{name:'Built-in tools and vault'}).click();
  await page.getByRole('button',{name:'Run protected tool'}).click();
  await expect(page.getByRole('region',{name:'Tool result'})).toContainText('14');
  await expect(page.getByText('Stored references: demo/token')).toBeVisible();
  const result=await new AxeBuilder({page}).analyze();expect(result.violations).toEqual([]);
  expect(await page.evaluate(()=>localStorage.length+sessionStorage.length)).toBe(0);
  await page.getByRole('button',{name:'Disconnect'}).click();await expect(page.getByLabel('Password',{exact:true})).toHaveValue('');
});
