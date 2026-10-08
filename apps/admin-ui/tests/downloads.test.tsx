// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
import { afterEach, expect, it, vi } from 'vitest';
import { cleanup, fireEvent, render, screen } from '@testing-library/react';
import { ClientDownloads } from '../src/ClientDownloads';
import type { ClientDownloadManifest } from '@olo-labs/toolgate-contracts';
afterEach(() => { cleanup(); vi.unstubAllGlobals(); });
const manifest: ClientDownloadManifest = { version:'0.7.0-dev', artifacts:[
  { platform:'WINDOWS',target:'x86_64-pc-windows-msvc',filename:'olo-toolgate-client-0.7.0-dev-x86_64-pc-windows-msvc.zip',sha256:'a'.repeat(64),bytes:123 },
  { platform:'MACOS',target:'x86_64-apple-darwin',filename:'olo-toolgate-client-0.7.0-dev-x86_64-apple-darwin.tar.gz',sha256:'b'.repeat(64),bytes:123 },
  { platform:'LINUX',target:'x86_64-unknown-linux-gnu',filename:'olo-toolgate-client-0.7.0-dev-x86_64-unknown-linux-gnu.tar.gz',sha256:'c'.repeat(64),bytes:123 },
] };
it('downloads all three platforms without a login credential and explains logged-out service operation', async () => {
  const fetcher = vi.fn().mockImplementation(() => Promise.resolve(new Response(JSON.stringify(manifest)))); vi.stubGlobal('fetch',fetcher);
  render(<ClientDownloads />);
  await screen.findByText(/Windows installers are unavailable/);
  expect(screen.queryByRole('link',{name:'Download Windows x64'})).toBeNull();
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
it('offers Windows EXE installers without exposing Windows ZIP archives', async () => {
  const installers = { ...manifest, artifacts: manifest.artifacts.map(a => ({ ...a,
    filename: a.filename.replace(/\.(zip|tar\.gz)$/, a.platform === 'WINDOWS' ? '.setup.exe' : a.platform === 'MACOS' ? '.dmg' : '.run') })) };
  vi.stubGlobal('fetch', vi.fn().mockImplementation((url: string) => Promise.resolve(new Response(JSON.stringify(url.endsWith('/installers') ? installers : manifest)))));
  render(<ClientDownloads />);
  expect((await screen.findByRole('link', { name: 'Install Windows x64' })).getAttribute('href')).toBe('/api/public/v1/clients/setup/x86_64-pc-windows-msvc');
  expect(screen.getByRole('link', { name: 'Install macOS x64' })).toBeTruthy();
  expect(screen.getByRole('link', { name: 'Install Linux x64' })).toBeTruthy();
  expect(screen.queryByRole('link', { name: 'Download Windows x64' })).toBeNull();
  expect(screen.getByText(/These development installers are unsigned/)).toBeTruthy();
});
it('shows the published gateway rather than the console origin and copies it for setup', async () => {
  const writeText = vi.fn().mockResolvedValue(undefined);
  vi.stubGlobal('navigator', { clipboard: { writeText } });
  const fetcher = vi.fn().mockImplementation((url: string) => Promise.resolve(new Response(JSON.stringify(
    url.endsWith('/configuration') ? { serverUrl: 'https://localhost:18450' } : manifest))));
  vi.stubGlobal('fetch', fetcher);
  render(<ClientDownloads />);
  const field = await screen.findByLabelText('Gateway URL') as HTMLInputElement;
  expect(field.value).toBe('https://localhost:18450');
  expect(field.readOnly).toBe(true);
  expect(screen.getByText('https://localhost:18450')).toBeTruthy();
  fireEvent.click(screen.getByRole('button', { name: 'Copy Gateway URL' }));
  expect(await screen.findByText('Gateway URL copied.')).toBeTruthy();
  expect(writeText).toHaveBeenCalledWith('https://localhost:18450');
  expect(fetcher.mock.calls.find(([url]) => url.endsWith('/configuration'))?.[1]).toMatchObject({ credentials: 'omit', redirect: 'error', cache: 'no-store' });
});
it('keeps the gateway selectable when clipboard access is unavailable', async () => {
  vi.stubGlobal('navigator', { clipboard: { writeText: vi.fn().mockRejectedValue(Error('Unavailable')) } });
  vi.stubGlobal('fetch', vi.fn().mockImplementation((url: string) => Promise.resolve(new Response(JSON.stringify(
    url.endsWith('/configuration') ? { serverUrl: 'https://gateway.example' } : manifest)))));
  render(<ClientDownloads />);
  await screen.findByLabelText('Gateway URL');
  fireEvent.click(screen.getByRole('button', { name: 'Copy Gateway URL' }));
  expect(await screen.findByText('Select the Gateway URL above and copy it.')).toBeTruthy();
});
it('does not advertise an invalid gateway as the installation address', async () => {
  vi.stubGlobal('fetch', vi.fn().mockImplementation((url: string) => Promise.resolve(new Response(JSON.stringify(
    url.endsWith('/configuration') ? { serverUrl: 'https://user:password@gateway.example' } : manifest)))));
  render(<ClientDownloads />);
  expect(await screen.findByText(/Gateway URL is unavailable/)).toBeTruthy();
  expect(screen.queryByLabelText('Gateway URL')).toBeNull();
  expect(screen.queryByRole('button', { name: 'Copy Gateway URL' })).toBeNull();
});
