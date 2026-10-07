// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
import {test,expect} from '@playwright/test';
import AxeBuilder from '@axe-core/playwright';
import {readFileSync} from 'node:fs';

test.use({userAgent:'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/140.0.0.0 Safari/537.36'});
test('Connect guides installation, remains accessible and cancels without claiming liveness',async({page})=>{
  await page.route('**/api/public/v1/clients/extension',route=>route.fulfill({contentType:'application/json',body:JSON.stringify({protocol:1,version:'0.10.0-dev',chromeVersion:'0.10.0.23',extensionId:'emmemldedebhbloibichmmdlbpjakfkf',storeUrl:'',filename:'olo-toolgate-chrome-0.10.0-dev-23.zip',sha256:'a'.repeat(64),bytes:123})}));
  await page.addInitScript(()=>{
    window.addEventListener('message',event=>{
      if(event.source!==window||event.data?.channel!=='toolgate-connect-request')return;
      window.postMessage({channel:'toolgate-connect-response',id:event.data.id,protocol:1,version:'0.10.0-dev',chromeVersion:'0.10.0.23',phase:'connecting',client:null},location.origin);
    });
  });
  await page.goto('/console/');
  if(process.env.UI_PASSWORDLESS_TEST!=='1'){
    const credentials=JSON.parse(readFileSync(process.env.UI_TEST_CREDENTIALS!,'utf8'));
    await page.getByLabel('Access token').fill(credentials.admin);await page.getByRole('button',{name:'Connect to workspace'}).click();
  }
  await expect(page.getByRole('heading',{name:'Your organization, at a glance'})).toBeVisible();
  await page.goto('/console/#enroll');await page.getByRole('button',{name:'Connect',exact:true}).click();
  await expect(page.getByText(/Installing the verified client or updating its gateway URL/)).toBeVisible();
  expect((await new AxeBuilder({page}).analyze()).violations).toEqual([]);
  await expect(page.getByText(/^Connected:/)).toHaveCount(0);
  await page.getByRole('button',{name:'Cancel Connect'}).click();await expect(page.getByText(/Connect cancelled/)).toBeVisible();
});
