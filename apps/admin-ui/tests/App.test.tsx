// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
import { afterEach, describe, expect, it, vi } from 'vitest';
import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react';
import { App } from '../src/App';

afterEach(() => { cleanup(); vi.unstubAllGlobals(); window.location.hash = ''; });
function response(items: unknown[] = []) { return new Response(JSON.stringify({items})); }
async function connect() { fireEvent.change(screen.getByLabelText('Access token'),{target:{value:'test-only-secret'}}); fireEvent.click(screen.getByRole('button',{name:'Connect to workspace'})); await screen.findByRole('navigation'); }

describe('Management shell states', () => {
  it('has labeled masked authentication and loading state, without storing credentials', async () => {
    let resolve!: (value:Response) => void;
    vi.stubGlobal('fetch',vi.fn().mockImplementationOnce(() => new Promise<Response>(done => {resolve=done;})).mockImplementation(async () => response()));
    render(<App />); expect(screen.getByLabelText('Access token').getAttribute('type')).toBe('password');
    fireEvent.change(screen.getByLabelText('Access token'),{target:{value:'test-only-secret'}}); fireEvent.click(screen.getByRole('button',{name:'Connect to workspace'}));
    expect(screen.getByRole('status').textContent).toContain('Verifying'); expect((screen.getByLabelText('Access token') as HTMLInputElement).value).toBe('');
    resolve(response()); await screen.findByRole('navigation');
    expect(localStorage.length).toBe(0); expect(sessionStorage.length).toBe(0); expect(window.location.href).not.toContain('test-only-secret');
    fireEvent.click(screen.getByRole('button',{name:'Disconnect'})); expect(screen.getByLabelText('Access token')).toBeTruthy();
  });
  it('shows authentication failure and permission errors without raw response text', async () => {
    vi.stubGlobal('fetch',vi.fn().mockImplementation(async () => new Response(JSON.stringify({code:'UNAUTHORIZED',requestId:'req-auth',message:'private-detail'}),{status:401})));
    render(<App />); fireEvent.change(screen.getByLabelText('Access token'),{target:{value:'bad-token'}}); fireEvent.click(screen.getByRole('button',{name:'Connect to workspace'}));
    expect((await screen.findByRole('alert')).textContent).toContain('session ended'); expect(document.body.textContent).not.toContain('private-detail');
  });
  it('supports all navigation and bounded dashboard links', async () => {
    vi.stubGlobal('fetch',vi.fn().mockImplementation(async () => response())); render(<App />); await connect();
    await waitFor(() => expect(screen.getAllByRole('link',{name:/View directory/}).length).toBe(6));
    window.location.hash = '#devices'; fireEvent(window,new HashChangeEvent('hashchange'));
    await screen.findByRole('heading',{name:'Clients'}); await screen.findByRole('heading',{name:'No clients on this page'});
    expect(screen.getByRole('link',{name:'Clients'}).getAttribute('aria-current')).toBe('page');
  });
  it('renders directory loading/error/retry states', async () => {
    window.location.hash = '#teams'; const directory = vi.fn().mockResolvedValueOnce(response()).mockResolvedValueOnce(new Response('{}',{status:503})).mockResolvedValue(response()); const fetcher = vi.fn().mockImplementation((url:string) => url.startsWith('/api/public/') ? Promise.resolve(new Response('{}',{status:503})) : directory()); vi.stubGlobal('fetch',fetcher);
    render(<App />); await connect(); expect((await screen.findByRole('alert')).textContent).toContain('could not complete');
    fireEvent.click(screen.getByRole('button',{name:'Try again'})); await screen.findByRole('heading',{name:'No teams on this page'});
  });
  it('renders untrusted record labels as text without executable HTML', async () => {
    window.location.hash='#users';const name='<img src=x onerror=alert(1)>';
    vi.stubGlobal('fetch',vi.fn().mockImplementation(async () => response([{id:'untrusted-label',name,enabled:true,revision:1}])));
    render(<App />);await connect();await screen.findByRole('button',{name});expect(document.querySelector('main img')).toBeNull();
  });
  it('submits a user via server validation and shows a reader denial', async () => {
    window.location.hash = '#users'; vi.stubGlobal('fetch',vi.fn().mockImplementation(async (_url, options) => options.method === 'POST' ? new Response('{}',{status:403}) : response()));
    render(<App />); await connect(); await screen.findByRole('heading',{name:'No users on this page'});
    fireEvent.click(screen.getByRole('button',{name:'Add user'})); fireEvent.change(screen.getByLabelText('Identifier'),{target:{value:'new-user'}}); fireEvent.change(screen.getByLabelText('Display name'),{target:{value:'New user'}}); fireEvent.click(screen.getByRole('button',{name:'Save user'}));
    expect((await screen.findByRole('alert')).textContent).toContain('permission'); expect(screen.getByRole('heading',{name:'Add directory user'})).toBe(document.activeElement);
  });
  it('retains an idempotency key for an exact retry and replaces it when values change', async () => {
    const keys:string[]=[]; window.location.hash='#users';
    vi.stubGlobal('fetch',vi.fn().mockImplementation(async (_url,options) => { if(options.method==='POST') {keys.push(new Headers(options.headers).get('Idempotency-Key')!);return new Response('{}',{status:503});}return response(); }));
    render(<App />);await connect();await screen.findByRole('heading',{name:'No users on this page'});
    fireEvent.click(screen.getByRole('button',{name:'Add user'}));fireEvent.change(screen.getByLabelText('Identifier'),{target:{value:'retry-user'}});fireEvent.change(screen.getByLabelText('Display name'),{target:{value:'Retry user'}});
    fireEvent.click(screen.getByRole('button',{name:'Save user'}));await screen.findByRole('alert');
    fireEvent.click(screen.getByRole('button',{name:'Save user'}));await waitFor(() => expect(keys.length).toBe(2));await screen.findByRole('alert');expect(keys[1]).toBe(keys[0]);
    fireEvent.change(screen.getByLabelText('Display name'),{target:{value:'Changed user'}});fireEvent.click(screen.getByRole('button',{name:'Save user'}));await waitFor(() => expect(keys.length).toBe(3));expect(keys[2]).not.toBe(keys[0]);
  });
});
