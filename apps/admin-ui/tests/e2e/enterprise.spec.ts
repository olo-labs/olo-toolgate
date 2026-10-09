// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
import {test,expect} from '@playwright/test';
import AxeBuilder from '@axe-core/playwright';
import {mkdirSync} from 'node:fs';

test.skip(!process.env.TOOLGATE_ENTERPRISE_ORIGIN,'Requires the isolated enterprise Quickstart gate');
test('real group administration, inherited provenance and review impact are accessible',async({page})=>{
  const errors:string[]=[];page.on('pageerror',e=>errors.push(e.name));
  const origin=process.env.TOOLGATE_ENTERPRISE_ORIGIN!;
  await page.goto(origin+'/console/');
  await page.getByLabel('Password',{exact:true}).fill(process.env.TOOLGATE_ENTERPRISE_PASSWORD!);
  await page.getByRole('button',{name:'Connect to workspace',exact:true}).click();
  await expect(page.getByRole('heading',{name:'Your organization, at a glance'})).toBeVisible();
  for(const [route,title] of [['users','Users'],['agents','Agents'],['agentGroups','Agent groups'],['toolGroups','Tool groups'],['deviceGroups','Device groups'],['grants','Access grants'],['bindings','Execution bindings'],['outcomes','Effect outcomes'],['simulation','Access simulation'],['configuration','Configuration reviews']] as const){
    await page.goto(origin+'/console/#'+route);
    await expect(page.getByRole('heading',{name:title,exact:true})).toBeVisible();
    await expect(page.getByRole('status').filter({hasText:'Loading'})).toHaveCount(0);
    expect((await new AxeBuilder({page}).analyze()).violations).toEqual([]);
  }
  await page.getByRole('button',{name:/^config-/}).first().click();
  await expect(page.getByRole('heading',{name:'Review configuration impact'})).toBeVisible();
  await expect(page.getByText('Exact change digest',{exact:true})).toBeVisible();
  await expect(page.getByRole('table',{name:'Exact record changes',exact:true})).toBeVisible();
  expect((await new AxeBuilder({page}).analyze()).violations).toEqual([]);
  mkdirSync('../../build/ui',{recursive:true});
  await page.screenshot({path:'../../build/ui/enterprise-configuration-desktop.png',fullPage:true});
  await page.goto(origin+'/console/#users');
  await page.getByRole('button',{name:'Quickstart admin',exact:true}).click();
  await expect(page.getByText(/Inherited access/).first()).toBeVisible();
  expect((await new AxeBuilder({page}).analyze()).violations).toEqual([]);
  await page.setViewportSize({width:390,height:844});
  expect(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth)).toBe(true);
  await page.screenshot({path:'../../build/ui/enterprise-user-mobile.png',fullPage:true});
  expect(await page.evaluate(()=>({local:localStorage.length,session:sessionStorage.length,cookies:document.cookie}))).toEqual({local:0,session:0,cookies:''});
  expect(errors).toEqual([]);
});
