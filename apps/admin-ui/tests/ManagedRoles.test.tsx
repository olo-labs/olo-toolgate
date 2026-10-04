// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
import {afterEach,expect,it,vi} from 'vitest';
import {cleanup,fireEvent,render,screen,waitFor} from '@testing-library/react';
import {DirectoryEditor} from '../src/DirectoryEditor';
import {Audit} from '../src/Audit';
import {App} from '../src/App';
import {ControlClient} from '../src/api';
afterEach(()=>{cleanup();vi.unstubAllGlobals();window.location.hash='';});

it('attaches fixed templates and JSON scope to a role, preserving assignments and scope on reload',async()=>{
  const role={id:'cloud',name:'Cloud operator',enabled:true,revision:2,portalRole:'BASIC' as const,rules:{templateIds:['IT_CLOUD_ADMIN' as const],deviceScope:'GROUPS' as const,deviceGroupIds:['cloud-devices'],toolIds:[]}};
  const saved=vi.fn();const transport=vi.fn<typeof fetch>().mockImplementation(async(_url,options)=>new Response(JSON.stringify(options?.method==='PUT'?role:{...role,revision:3})));
  render(<DirectoryEditor client={new ControlClient('token',vi.fn(),transport)} kind="roles" record={role} saved={saved} close={vi.fn()}/>);
  expect((screen.getByLabelText('IT Cloud Admin') as HTMLInputElement).checked).toBe(true);
  fireEvent.click(screen.getByLabelText('Approval reviewer'));
  fireEvent.change(screen.getByLabelText('Permission scope (JSON)'),{target:{value:JSON.stringify({deviceScope:'ALL',deviceGroupIds:[],toolIds:[]})}});
  fireEvent.click(screen.getByRole('button',{name:'Save role'}));await waitFor(()=>expect(saved).toHaveBeenCalled());
  const body=JSON.parse(transport.mock.calls[0][1]?.body as string);
  expect(body.rules).toEqual({templateIds:['IT_CLOUD_ADMIN','APPROVER'],deviceScope:'ALL',deviceGroupIds:[],toolIds:[]});
  fireEvent.click(screen.getByRole('button',{name:'Reload current record'}));await waitFor(()=>expect((screen.getByLabelText('Approval reviewer') as HTMLInputElement).checked).toBe(false));
  expect((screen.getByLabelText('Permission scope (JSON)') as HTMLTextAreaElement).value).toContain('cloud-devices');
});
it('role scope rejects malformed JSON locally before issuing a mutation',async()=>{
  const transport=vi.fn<typeof fetch>();render(<DirectoryEditor client={new ControlClient('token',vi.fn(),transport)} kind="roles" saved={vi.fn()} close={vi.fn()}/>);
  fireEvent.change(screen.getByLabelText('Identifier'),{target:{value:'role'}});fireEvent.change(screen.getByLabelText('Display name'),{target:{value:'Role'}});
  fireEvent.change(screen.getByLabelText('Permission scope (JSON)'),{target:{value:'[]'}});fireEvent.click(screen.getByRole('button',{name:'Save role'}));
  await screen.findByRole('alert');expect(transport).not.toHaveBeenCalled();
});
it('groups all navigation into four expandable submenus and exposes Roles',async()=>{
  vi.stubGlobal('fetch',vi.fn().mockImplementation(async()=>new Response(JSON.stringify({items:[]}))));render(<App/>);
  fireEvent.change(screen.getByLabelText('Access token'),{target:{value:'token'}});fireEvent.click(screen.getByRole('button',{name:'Connect to workspace'}));
  await screen.findByRole('navigation');expect(document.querySelectorAll('nav details')).toHaveLength(4);
  expect([...document.querySelectorAll('nav summary')].map(element=>element.textContent)).toEqual(['Audit','Tools','Devices','Users']);
  expect(screen.getByRole('link',{name:'Roles'}).closest('details')?.querySelector('summary')?.textContent).toBe('Users');
  expect(screen.getByRole('link',{name:'Tool builder'}).closest('details')?.querySelector('summary')?.textContent).toBe('Tools');
  expect(screen.getByRole('link',{name:'Enroll device'}).closest('details')?.querySelector('summary')?.textContent).toBe('Devices');
  expect(screen.getByRole('link',{name:'Audit log'}).closest('details')?.querySelector('summary')?.textContent).toBe('Audit');
});
it('loads audit metadata, advances the cursor, refreshes and handles errors',async()=>{
  const event={sequence:1,tenantId:'tenant',actorId:'a'.repeat(64),operation:'UPDATE',target:'roles:cloud',revision:2,requestId:'request-1',occurredAt:'2026-10-04T00:00:00Z',requestDigest:'b'.repeat(64)};
  const transport=vi.fn<typeof fetch>().mockImplementation(async url=>new Response(JSON.stringify(String(url).includes('cursor=1')?{items:[]}:{items:[event],nextCursor:'1'})));
  render(<Audit client={new ControlClient('token',vi.fn(),transport)}/>);await screen.findByText('roles:cloud');
  fireEvent.click(screen.getByRole('button',{name:'Next page'}));await screen.findByText('No audit events on this page.');
  fireEvent.click(screen.getByRole('button',{name:'First page'}));await screen.findByText('roles:cloud');
  transport.mockResolvedValue(new Response('{}',{status:503}));fireEvent.click(screen.getByRole('button',{name:'Refresh'}));await screen.findByRole('alert');
  transport.mockResolvedValue(new Response(JSON.stringify({items:[]})));fireEvent.click(screen.getByRole('button',{name:'Try again'}));await screen.findByText('No audit events on this page.');
});
