// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
import { afterEach, expect, it, vi } from 'vitest';
import { cleanup, render, screen } from '@testing-library/react';
import { ClientDownloads } from '../src/ClientDownloads';
import type { ClientDownloadManifest } from '@olo-labs/toolgate-contracts';
afterEach(() => { cleanup(); vi.unstubAllGlobals(); });
const manifest: ClientDownloadManifest = { version:'0.7.0-dev', artifacts:[
  { platform:'WINDOWS',target:'x86_64-pc-windows-msvc',filename:'olo-toolgate-client-0.7.0-dev-x86_64-pc-windows-msvc.zip',sha256:'a'.repeat(64),bytes:123 },
  { platform:'MACOS',target:'x86_64-apple-darwin',filename:'olo-toolgate-client-0.7.0-dev-x86_64-apple-darwin.tar.gz',sha256:'b'.repeat(64),bytes:123 },
  { platform:'LINUX',target:'x86_64-unknown-linux-gnu',filename:'olo-toolgate-client-0.7.0-dev-x86_64-unknown-linux-gnu.tar.gz',sha256:'c'.repeat(64),bytes:123 },
] };
it('downloads all three platforms without a login credential and explains logged-out service operation', async () => {
  const fetcher = vi.fn().mockResolvedValue(new Response(JSON.stringify(manifest))); vi.stubGlobal('fetch',fetcher);
  render(<ClientDownloads />);
  expect((await screen.findByRole('link',{name:'Download Windows x64'})).getAttribute('href')).toBe(`/api/public/v1/clients/${manifest.artifacts[0].filename}`);
  expect(screen.getByRole('link',{name:'Download macOS x64'})).toBeTruthy(); expect(screen.getByRole('link',{name:'Download Linux x64'})).toBeTruthy();
  const options = fetcher.mock.calls[0][1]; expect(options.credentials).toBe('omit'); expect(options.headers).toBeUndefined();
  expect(screen.getByText(/keeps running when you lock the screen or log out/)).toBeTruthy();
});
it('reports missing releases without broken download links', async () => {
  vi.stubGlobal('fetch',vi.fn().mockResolvedValue(new Response('{}',{status:503}))); render(<ClientDownloads />);
  expect(await screen.findByText(/Client downloads are unavailable/)).toBeTruthy(); expect(screen.queryByRole('link')).toBeNull();
});
it('rejects untrusted artifact URLs and script-bearing filenames', async () => {
  vi.stubGlobal('fetch',vi.fn().mockResolvedValue(new Response(JSON.stringify({...manifest,artifacts:manifest.artifacts.map(a=>({...a,filename:'../../secret'}))}))));
  render(<ClientDownloads />); expect(await screen.findByText(/Client downloads are unavailable/)).toBeTruthy(); expect(screen.queryByRole('link')).toBeNull();
});
