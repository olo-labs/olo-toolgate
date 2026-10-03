// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
import {afterEach,expect,it,vi} from 'vitest';
import {cleanup,fireEvent,render,screen,waitFor} from '@testing-library/react';
import {Fleet} from '../src/Fleet';
import {ControlClient} from '../src/api';
import axe from 'axe-core';
afterEach(()=>{cleanup();vi.unstubAllGlobals();});
const json=(value:unknown,status=200)=>new Response(JSON.stringify(value),{status});
it('shows loading, empty and accessible actions',async()=>{
 vi.stubGlobal('fetch',vi.fn(async()=>json({items:[]})));const {container}=render(<Fleet client={new ControlClient('token',vi.fn())}/>);
 expect(screen.getByRole('status').textContent).toContain('Loading');await screen.findByText('No deployments yet.');
 expect((await axe.run(container,{rules:{"color-contrast":{enabled:false}}})).violations).toEqual([]);
});
it('renders authoritative status and retains server revision on advance',async()=>{
 const rollout={id:'canary',packageId:'tool',version:'1.0.0',desiredPresence:true,percentage:10,revision:3,createdAtUnixMs:1,members:[{deviceId:'device',generation:2}]};
 const fetcher=vi.fn(async(url:string,init?:RequestInit)=>init?.method==='POST'?json(rollout):json({items:url.includes('rollouts')?[{rollout,ready:0,failed:0,offline:1,waiting:0,pending:0,superseded:0}]:[]}));
 vi.stubGlobal('fetch',fetcher);render(<Fleet client={new ControlClient('token',vi.fn())}/>);fireEvent.click(await screen.findByRole('button',{name:'Advance canary to 100%'}));
 await waitFor(()=>expect(fetcher.mock.calls.some(([url,init])=>url.endsWith('/canary/advance')&&JSON.parse(String(init?.body)).expectedRevision===3)).toBe(true));
});
it('redacts error bodies and permits refresh',async()=>{
 vi.stubGlobal('fetch',vi.fn(async()=>json({code:'FORBIDDEN',message:'private-detail'},403)));render(<Fleet client={new ControlClient('token',vi.fn())}/>);
 expect((await screen.findByRole('alert')).textContent).toContain('permission');expect(document.body.textContent).not.toContain('private-detail');
 fireEvent.click(screen.getByRole('button',{name:'Refresh fleet status'}));
});
