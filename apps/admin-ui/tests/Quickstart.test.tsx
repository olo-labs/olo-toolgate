// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
import { afterEach, describe, expect, it, vi } from 'vitest';
import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react';
import { App } from '../src/App';
import { QuickstartTools } from '../src/QuickstartTools';
import { ControlClient } from '../src/api';
const response=(body:unknown,status=200)=>new Response(JSON.stringify(body),{status});
afterEach(()=>{cleanup();document.querySelector('meta[name=toolgate-mode]')?.remove();vi.unstubAllGlobals();window.location.hash='';});
describe('Quickstart',()=>{
  it('automatically enters only when the server explicitly disables password login',async()=>{
    const meta=document.createElement('meta');meta.name='toolgate-mode';meta.content='quickstart';document.head.append(meta);
    const transport=vi.fn(async(input:string)=>input.endsWith('/status')?response({passwordRequired:false}):input.endsWith('/login')?response({accessToken:'local-auto-session'}):input.includes('/api/public/')?response({},503):response({items:[]}));
    vi.stubGlobal('fetch',transport);render(<App/>);
    await screen.findByRole('heading',{name:'Your organization, at a glance'});
    expect(screen.queryByLabelText('Password')).toBeNull();
    expect(localStorage.length).toBe(0);expect(sessionStorage.length).toBe(0);
    expect(document.body.textContent).not.toContain('local-auto-session');
  });
  it('exchanges bootstrap and replacement passwords without persisting credentials',async()=>{
    const meta=document.createElement('meta');meta.name='toolgate-mode';meta.content='quickstart';document.head.append(meta);
    const transport=vi.fn(async(input:string)=>input.endsWith('/login')?response({accessToken:'session-secret'}):input.includes('/api/public/')?response({},503):response({items:[]}));vi.stubGlobal('fetch',transport);
    render(<App/>);fireEvent.change(screen.getByLabelText('Password'),{target:{value:'bootstrap-secret'}});fireEvent.change(screen.getByLabelText('New password (required on first login)'),{target:{value:'a-different-strong-password'}});fireEvent.click(screen.getByRole('button',{name:'Connect to workspace'}));
    await screen.findByRole('heading',{name:'Your organization, at a glance'});expect(document.body.textContent).toContain('Quickstart · Non-HA');expect(localStorage.length).toBe(0);expect(sessionStorage.length).toBe(0);expect(document.body.textContent).not.toContain('session-secret');
    fireEvent.click(screen.getByRole('button',{name:'Disconnect'}));expect((screen.getByLabelText('Password') as HTMLInputElement).value).toBe('');
  });
  it('renders bootstrap failure safely',async()=>{
    const meta=document.createElement('meta');meta.name='toolgate-mode';meta.content='quickstart';document.head.append(meta);
    vi.stubGlobal('fetch',vi.fn(async()=>response({message:'secret-detail'},400)));render(<App/>);fireEvent.change(screen.getByLabelText('Password'),{target:{value:'bad-password'}});fireEvent.click(screen.getByRole('button',{name:'Connect to workspace'}));expect((await screen.findByRole('alert')).textContent).not.toContain('secret-detail');
  });
  it('routes protected invocation, ASK and vault writes through the authoritative API',async()=>{
    const transport=vi.fn(async(input:string,init?:RequestInit)=>input.endsWith('/tools')?response({tools:[{toolId:'calculator.evaluate',enabled:true}]}):input.endsWith('/invoke')?response({decision:'ASK',approvalId:'ask-1'},202):init?.method==='POST'?response({stored:true},201):response({names:['provider/token']}));
    vi.stubGlobal('fetch',transport);render(<QuickstartTools client={new ControlClient('test-session',()=>{})}/>);
    await waitFor(()=>expect((screen.getByRole('button',{name:'Run protected tool'}) as HTMLButtonElement).disabled).toBe(false));fireEvent.click(screen.getByRole('button',{name:'Run protected tool'}));await screen.findByRole('region',{name:'Tool result'});expect(document.body.textContent).toContain('ask-1');
    fireEvent.change(screen.getByLabelText('Secret name'),{target:{value:'provider/token'}});fireEvent.change(screen.getByLabelText('Secret value'),{target:{value:'private-provider-value'}});fireEvent.click(screen.getByRole('button',{name:'Store encrypted secret'}));await waitFor(()=>expect((screen.getByLabelText('Secret value') as HTMLInputElement).value).toBe(''));expect(document.body.textContent).not.toContain('private-provider-value');
  });
  it('rejects malformed JSON and shows dependency errors',async()=>{
    vi.stubGlobal('fetch',vi.fn(async(input:string)=>input.endsWith('/tools')?response({tools:[{toolId:'calculator.evaluate',enabled:true}]}):input.endsWith('/vault')?response({names:[]}):response({},503)));
    render(<QuickstartTools client={new ControlClient('test-session',()=>{})}/>);await waitFor(()=>expect((screen.getByRole('button',{name:'Run protected tool'}) as HTMLButtonElement).disabled).toBe(false));
    fireEvent.change(screen.getByLabelText('Arguments (JSON object)'),{target:{value:'broken'}});fireEvent.click(screen.getByRole('button',{name:'Run protected tool'}));await screen.findByRole('alert');
    fireEvent.change(screen.getByLabelText('Arguments (JSON object)'),{target:{value:'{}'}});fireEvent.click(screen.getByRole('button',{name:'Run protected tool'}));await waitFor(()=>expect(screen.getByRole('alert').textContent).toContain('could not complete'));
  });
});
