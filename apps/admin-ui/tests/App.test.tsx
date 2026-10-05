// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
import { afterEach, describe, expect, it, vi } from 'vitest';
import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react';
import { App } from '../src/App';
import axe from 'axe-core';

afterEach(() => { cleanup(); vi.unstubAllGlobals(); window.location.hash = ''; });
function response(items: unknown[] = []) { return new Response(JSON.stringify({items})); }
async function connect() { fireEvent.change(screen.getByLabelText('Access token'),{target:{value:'test-only-secret'}}); fireEvent.click(screen.getByRole('button',{name:'Connect to workspace'})); await screen.findByRole('navigation'); }

describe('Management shell states', () => {
  it('keeps navigation controls in landmarks and main outside nested landmarks', async () => {
    vi.stubGlobal('fetch',vi.fn().mockImplementation(async()=>response()));
    render(<App/>); await connect();
    const rules = {runOnly:{type:'rule' as const,values:['region','landmark-main-is-top-level']}};
    expect((await axe.run(document,rules)).violations).toEqual([]);
    // Prove the fast scan rejects the exact CI regression, rather than passing vacuously.
    const controls=screen.getByRole('region',{name:'Navigation controls'});
    controls.removeAttribute('role');
    expect((await axe.run(document,rules)).violations.some(rule=>rule.id==='region')).toBe(true);
    controls.setAttribute('role','region');
    fireEvent.click(screen.getByRole('button',{name:'Collapse navigation'}));
    expect((await axe.run(document,rules)).violations).toEqual([]);
    localStorage.clear();
  });

  it('defaults to Audit expanded and supports bounded keyboard resizing and collapse', async () => {
    localStorage.clear();
    vi.stubGlobal('fetch',vi.fn().mockImplementation(async()=>response()));
    render(<App/>); await connect();
    expect([...document.querySelectorAll<HTMLDetailsElement>('nav details')].map(group=>group.open)).toEqual([true,false,false,false]);
    const resize = screen.getByRole('separator',{name:'Resize navigation'});
    expect(screen.getByRole('region',{name:'Navigation controls'}).contains(resize)).toBe(true);
    fireEvent.keyDown(resize,{key:'End'}); expect(resize.getAttribute('aria-valuenow')).toBe('420');
    fireEvent.keyDown(resize,{key:'ArrowRight'}); expect(resize.getAttribute('aria-valuenow')).toBe('420');
    fireEvent.keyDown(resize,{key:'Home'}); expect(resize.getAttribute('aria-valuenow')).toBe('190');
    fireEvent.click(screen.getByRole('button',{name:'Collapse navigation'}));
    expect(screen.getByRole('navigation')).toBeTruthy();
    expect(screen.getByRole('button',{name:'Audit'}).querySelector('svg')).toBeTruthy();
    expect(screen.getByRole('button',{name:'Expand navigation'}).getAttribute('aria-expanded')).toBe('false');
    fireEvent.click(screen.getByRole('button',{name:'Expand navigation'}));
    expect(screen.getByRole('navigation')).toBeTruthy();
    expect(screen.getByRole('separator').getAttribute('aria-valuenow')).toBe('190');
    localStorage.clear();
  });

  it('shows endpoint downloads only on login and device enrollment',async()=>{
    vi.stubGlobal('fetch',vi.fn().mockImplementation(async()=>response()));
    render(<App/>);expect(screen.getByRole('heading',{name:'Install ToolGate on your computer'})).toBeTruthy();
    await connect();await screen.findByRole('heading',{name:'Your organization, at a glance'});
    expect(screen.queryByRole('heading',{name:'Install ToolGate on your computer'})).toBeNull();
    window.location.hash='#enroll';fireEvent(window,new HashChangeEvent('hashchange'));
    await screen.findByRole('heading',{name:'Install ToolGate on your computer'});
    window.location.hash='#users';fireEvent(window,new HashChangeEvent('hashchange'));
    await screen.findByRole('heading',{name:'Users'});
    expect(screen.queryByRole('heading',{name:'Install ToolGate on your computer'})).toBeNull();
  });
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
    await waitFor(() => expect(screen.getAllByRole('link',{name:/View directory/}).length).toBe(7));
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
  it('submits managed role assignments without user-level template overrides',async()=>{
    let submitted:Record<string,unknown>|undefined;
    window.location.hash='#users';
    vi.stubGlobal('fetch',vi.fn().mockImplementation(async (_url,options)=>{
      if(options.method==='POST'){submitted=JSON.parse(options.body);return new Response('{}',{status:403});}
      return response();
    }));
    render(<App/>);await connect();await screen.findByRole('heading',{name:'No users on this page'});
    fireEvent.click(screen.getByRole('button',{name:'Add user'}));
    fireEvent.change(screen.getByLabelText('Identifier'),{target:{value:'cloud-user'}});fireEvent.change(screen.getByLabelText('Display name'),{target:{value:'Cloud user'}});
    expect((screen.getByLabelText('Enabled in directory') as HTMLInputElement).checked).toBe(false);
    expect(screen.queryByText('Privilege templates (combine as needed)')).toBeNull();
    fireEvent.change(screen.getByLabelText('Role IDs, separated by commas'),{target:{value:'cloud-role, reviewer-role'}});
    fireEvent.click(screen.getByRole('button',{name:'Save user'}));await screen.findByRole('alert');
    expect(submitted?.access).toEqual({role:'BASIC',templateIds:[],deviceGroupIds:[],roleIds:['cloud-role','reviewer-role']});
    expect(screen.getByRole('heading',{name:'Add directory user'})).toBeTruthy();
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
