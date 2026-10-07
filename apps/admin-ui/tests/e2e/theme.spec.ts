// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
import { test, expect } from '@playwright/test';
import AxeBuilder from '@axe-core/playwright';
import { mkdirSync } from 'node:fs';

test('OLO branding, dark default, saved theme, system preference and mobile accessibility', async ({page}) => {
  await page.goto('/console/');
  await expect(page.getByLabel('Theme')).toBeVisible();
  await expect(page.locator('html')).toHaveAttribute('data-theme','dark');
  await expect(page.getByRole('img',{name:'OLO'})).toBeVisible();
  expect(await page.getByRole('img',{name:'OLO'}).evaluate((image:HTMLImageElement)=>image.naturalWidth)).toBeGreaterThan(0);
  const favicon = await page.locator('link[rel=icon]').getAttribute('href');
  expect(favicon).toContain('/console/assets/olo-');
  expect((await page.request.get(favicon!)).status()).toBe(200);
  const alpha = await page.evaluate(async (url) => {
    const image = new Image(); image.src = url; await image.decode();
    const canvas = document.createElement('canvas'); canvas.width=image.width; canvas.height=image.height;
    const context=canvas.getContext('2d')!; context.drawImage(image,0,0);
    const rowAlpha=(y:number)=>Array.from(context.getImageData(0,y,canvas.width,1).data).filter((_,index)=>index%4===3);
    return {width:image.width,height:image.height,corner:context.getImageData(0,0,1,1).data[3],
      center:Array.from(context.getImageData(canvas.width/2,canvas.height/2,1,1).data),
      top:Math.max(...rowAlpha(0)),bottom:Math.max(...rowAlpha(canvas.height-1))};
  },favicon!);
  expect(alpha.corner).toBe(0); expect(alpha.center).toEqual([255,255,255,255]);
  expect(alpha.width).toBe(alpha.height);expect(alpha.top).toBe(0);expect(alpha.bottom).toBe(0);
  if (process.env.UI_PASSWORDLESS_TEST === '1') {
    await expect(page.getByRole('navigation')).toBeVisible();
    await expect(page.locator('.stat')).toHaveCount(6);
  }
  const surface=process.env.UI_PASSWORDLESS_TEST==='1' ? 'workspace' : 'login';
  if (surface==='login') {
    await page.setViewportSize({width:1440,height:1000});
    const panel=await page.locator('.connect-page').boundingBox();
    expect(panel?.width).toBe(1440);
  }
  mkdirSync('build/ui',{recursive:true});
  for (const theme of ['dark','light']) {
    await page.getByLabel('Theme').selectOption(theme);
    await expect(page.locator('html')).toHaveAttribute('data-theme',theme);
    if (surface==='workspace') await expect(page.locator('.stat')).toHaveCount(6);
    expect((await new AxeBuilder({page}).withTags(['wcag2a','wcag2aa','wcag21aa']).analyze()).violations).toEqual([]);
    await page.screenshot({path:`build/ui/theme-${surface}-${theme}-desktop.png`,fullPage:true});
    await page.reload();
    await expect(page.getByLabel('Theme')).toHaveValue(theme);
    await expect(page.locator('html')).toHaveAttribute('data-theme',theme);
  }
  await page.getByLabel('Theme').selectOption('system');
  await page.emulateMedia({colorScheme:'light'});
  await expect(page.locator('html')).toHaveAttribute('data-theme','light');
  await page.emulateMedia({colorScheme:'dark'});
  await expect(page.locator('html')).toHaveAttribute('data-theme','dark');
  await page.getByLabel('Theme').selectOption('dark');
  await page.setViewportSize({width:390,height:844});
  expect(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth)).toBe(true);
  expect((await new AxeBuilder({page}).withTags(['wcag2a','wcag2aa','wcag21aa']).analyze()).violations).toEqual([]);
  await page.screenshot({path:`build/ui/theme-${surface}-dark-mobile.png`,fullPage:true});
});
