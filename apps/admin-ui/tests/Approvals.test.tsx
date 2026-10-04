// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
import { afterEach, describe, expect, it, vi } from 'vitest';
import { act, cleanup, fireEvent, render, screen, waitFor, within } from '@testing-library/react';
import type { ApprovalRecord, ApprovalState } from '@olo-labs/toolgate-contracts';
import { Approvals } from '../src/Approvals';
import { App } from '../src/App';
import { ControlClient } from '../src/api';

afterEach(() => { cleanup(); vi.useRealTimers(); vi.unstubAllGlobals(); window.location.hash = ''; });
const json = (value: unknown, status = 200) => new Response(JSON.stringify(value),{status});
function record(id = 'approval-one', state: ApprovalState = 'PENDING'): ApprovalRecord {
  return {id,revision:1,state,policyVersion:'1.0.8',createdAtUnixMs:Date.now()-10000,expiresAtUnixMs:Date.now()+300000,
    input:{context:{requestId:'request-one',tenantId:'tenant-one',userId:'requester-one',agentId:'agent-one'},toolId:'tool-one',action:'read',resource:{kind:'FILE',locator:'/exact/path'},argumentsDigest:'a'.repeat(64)}};
}
function renderQueue(fetcher: typeof fetch) { vi.stubGlobal('fetch',fetcher); return render(<Approvals client={new ControlClient('private-token',vi.fn())} />); }
async function review(id = 'approval-one') { fireEvent.click(await screen.findByRole('button',{name:`Review approval ${id}`})); return screen.findByRole('heading',{name:'Choose a decision'}); }
function confirm(choice = 'approve once') { fireEvent.click(screen.getByLabelText('I reviewed the identities, resource, arguments digest and policy version.')); fireEvent.click(screen.getByRole('button',{name:`Confirm ${choice}`})); }

describe('Exact-operation human review', () => {
  it('supports an approver-only session verified by the server rather than decoded claims', async () => {
    const fetcher = vi.fn().mockImplementation(async (url:string) => url.includes('/admin-session') ? json({code:'FORBIDDEN'},403) : json({items:[]}));
    vi.stubGlobal('fetch',fetcher); render(<App />);
    fireEvent.change(screen.getByLabelText('Access token'),{target:{value:'opaque-approver-token'}}); fireEvent.click(screen.getByRole('button',{name:'Connect to workspace'}));
    await screen.findByRole('heading',{name:'Approvals'}); await screen.findByRole('heading',{name:'No approvals on this page'});
    expect(window.location.hash).toBe('#approvals'); expect(screen.getByRole('link',{name:'Approvals'}).getAttribute('aria-current')).toBe('page');
    expect(fetcher.mock.calls.some(([url]) => url.includes('/approvals?limit=50'))).toBe(true);
    expect(localStorage.length).toBe(0); expect(document.body.textContent).not.toContain('opaque-approver-token');
  });
  it('does not accept a forbidden session when both server capabilities are denied', async () => {
    vi.stubGlobal('fetch',vi.fn().mockImplementation(async () => json({code:'FORBIDDEN',message:'private-detail'},403))); render(<App />);
    fireEvent.change(screen.getByLabelText('Access token'),{target:{value:'opaque-token'}}); fireEvent.click(screen.getByRole('button',{name:'Connect to workspace'}));
    expect((await screen.findByRole('alert')).textContent).toContain('permission'); expect(screen.queryByRole('navigation')).toBeNull();
    expect(document.body.textContent).not.toContain('private-detail');
  });
  it('renders pending filters, bounded cursor paging, empty and loading states', async () => {
    const fetcher = vi.fn().mockImplementation(async (url:string) => url.includes('cursor=next%2F%2B') ? json({items:[]}) : json({items:[record(),record('old','DENIED')],nextCursor:'next/+'}));
    renderQueue(fetcher); expect(screen.getByRole('status').textContent).toContain('Loading approvals');
    await screen.findByRole('button',{name:'Review approval old'}); fireEvent.click(screen.getByRole('button',{name:'Pending on this page'}));
    expect(screen.queryByRole('button',{name:'Review approval old'})).toBeNull(); expect(screen.getByText('2 approval records on this page')).toBeTruthy();
    fireEvent.click(screen.getByRole('button',{name:'Next approvals page'})); await screen.findByRole('heading',{name:'No pending approvals on this page'});
    expect(screen.getByRole('button',{name:'Next approvals page'}).hasAttribute('disabled')).toBe(true);
    fireEvent.click(screen.getByRole('button',{name:'Previous approvals page'})); await screen.findByRole('button',{name:'Review approval approval-one'});
    fireEvent.click(screen.getByRole('button',{name:'All states on this page'})); await screen.findByRole('button',{name:'Review approval old'});
  });
  it('shows safe queue errors and recovers through explicit refresh', async () => {
    const fetcher = vi.fn().mockResolvedValueOnce(json({code:'DEPENDENCY_UNAVAILABLE',requestId:'safe-reference',message:'private-token'},503)).mockImplementation(async () => json({items:[]}));
    renderQueue(fetcher); expect((await screen.findByRole('alert')).textContent).toContain('safe-reference'); expect(document.body.textContent).not.toContain('private-token');
    fireEvent.click(screen.getByRole('button',{name:'Try again'})); await screen.findByRole('heading',{name:'No approvals on this page'});
  });
  it('shows every exact binding as text, requires human confirmation and prevents a double submission', async () => {
    const base = record(); const input = {...base,input:{...base.input,resource:{...base.input.resource,locator:'<img src=x onerror=alert(1)>'}}};
    let resolve!: (value:Response) => void;
    const bodies:unknown[] = [];
    const fetcher = vi.fn().mockImplementation(async (url:string,options:RequestInit) => {
      if (options.method === 'POST') { bodies.push(JSON.parse(options.body as string)); return new Promise<Response>(done => {resolve=done;}); }
      return json(url.includes('?') ? {items:[input]} : input);
    });
    renderQueue(fetcher); await review();
    const details = screen.getByRole('region',{name:'Approval details'});
    for (const value of ['tenant-one','requester-one','agent-one','No device identity','tool-one','read','FILE','<img src=x onerror=alert(1)>','a'.repeat(64),'1.0.8','request-one']) expect(within(details).getByText(value,{exact:true})).toBeTruthy();
    expect(document.querySelector('img')).toBeNull(); expect(screen.getByRole('heading',{name:'Review exact operation'})).toBe(document.activeElement);
    fireEvent.click(screen.getByRole('button',{name:'Approve once'})); const submit=screen.getByRole('button',{name:'Confirm approve once'});
    expect(submit.hasAttribute('disabled')).toBe(true); confirm(); fireEvent.click(submit);
    await waitFor(() => expect(bodies.length).toBe(1)); expect(bodies[0]).toEqual({decision:'APPROVE_ONCE',expectedRevision:1});
    expect(screen.getByRole('button',{name:'Close approval'}).hasAttribute('disabled')).toBe(true);
    resolve(json({...input,state:'APPROVED_ONCE',revision:2,decidedBy:'approver-one',decidedAtUnixMs:Date.now()}));
    await waitFor(() => expect(within(details).getByText('Approved once',{exact:true})).toBeTruthy());
    expect(within(details).queryByRole('button',{name:'Approve once'})).toBeNull(); expect(within(details).getByText('approver-one')).toBeTruthy();
    expect(document.body.textContent).not.toContain('ALLOW');
  });
  it('retains exact retry keys and resets confirmation when the choice or duration changes', async () => {
    const keys:string[] = []; const bodies:unknown[] = [];
    renderQueue(vi.fn().mockImplementation(async (url:string,options:RequestInit) => {
      if (options.method==='POST') { keys.push(new Headers(options.headers).get('Idempotency-Key')!); bodies.push(JSON.parse(options.body as string)); return json({},503); }
      return json(url.includes('?') ? {items:[record()]} : record());
    }));
    await review(); fireEvent.click(screen.getByRole('button',{name:'Approve once'})); confirm(); await screen.findByRole('alert');
    fireEvent.click(screen.getByRole('button',{name:'Confirm approve once'})); await waitFor(() => expect(keys.length).toBe(2)); await screen.findByRole('alert'); expect(keys[1]).toBe(keys[0]);
    fireEvent.click(screen.getByRole('button',{name:'Approve temporarily'})); expect(screen.getByRole('button',{name:'Confirm approve temporarily'}).hasAttribute('disabled')).toBe(true);
    confirm('approve temporarily'); await waitFor(() => expect(keys.length).toBe(3)); await screen.findByRole('alert'); expect(keys[2]).not.toBe(keys[0]);
    expect(bodies[2]).toEqual({decision:'APPROVE_TEMPORARY',expectedRevision:1,durationMs:600000});
    fireEvent.change(screen.getByLabelText('Maximum approval duration'),{target:{value:'1800000'}}); expect(screen.getByRole('button',{name:'Confirm approve temporarily'}).hasAttribute('disabled')).toBe(true);
    confirm('approve temporarily'); await waitFor(() => expect(keys.length).toBe(4)); expect(keys[3]).not.toBe(keys[2]); expect(bodies[3]).toEqual({decision:'APPROVE_TEMPORARY',expectedRevision:1,durationMs:1800000});
    fireEvent.click(screen.getByRole('button',{name:'Cancel decision'})); expect(screen.queryByLabelText('Maximum approval duration')).toBeNull();
  });
  it('records denial without granting runtime permission and supports closing details', async () => {
    const denied={...record(),state:'DENIED',revision:2} as const;
    renderQueue(vi.fn().mockImplementation(async (url:string,options:RequestInit) => options.method === 'POST' ? json(denied) : json(url.includes('?') ? {items:[record()]} : record())));
    await review(); fireEvent.click(screen.getByRole('button',{name:'Deny request'})); confirm('deny');
    await screen.findByText('This request was denied. No execution permission was granted.');
    fireEvent.click(screen.getByRole('button',{name:'Close approval'})); expect(screen.queryByRole('region',{name:'Approval details'})).toBeNull();
  });
  it('reloads a conflict and removes stale decision controls using the authoritative state', async () => {
    let reads=0; const latest={...record(),state:'EXPIRED',revision:2} as const;
    renderQueue(vi.fn().mockImplementation(async (url:string,options:RequestInit) => {
      if (options.method==='POST') return json({code:'CONFLICT',requestId:'conflict-reference',message:'private-detail'},409);
      return json(url.includes('?') ? {items:[record()]} : ++reads === 1 ? record() : latest);
    }));
    await review(); fireEvent.click(screen.getByRole('button',{name:'Approve once'})); confirm();
    await screen.findByText('This approval expired. The requester must make a fresh Gateway request.');
    expect(screen.getByRole('alert').textContent).toContain('Refresh before trying again'); expect(reads).toBe(2);
    expect(screen.queryByRole('button',{name:'Approve once'})).toBeNull(); expect(document.body.textContent).not.toContain('private-detail');
  });
  it('shows wrong-approver denial safely and does not automatically retry a mutation', async () => {
    const fetcher = vi.fn().mockImplementation(async (url:string,options:RequestInit) => options.method==='POST' ? json({code:'FORBIDDEN',message:'private-detail'},403) : json(url.includes('?') ? {items:[record()]} : record()));
    renderQueue(fetcher); await review(); fireEvent.click(screen.getByRole('button',{name:'Approve once'})); confirm();
    expect((await screen.findByRole('alert')).textContent).toContain('permission');
    expect(fetcher.mock.calls.filter(([,options]) => options.method==='POST').length).toBe(1); expect(document.body.textContent).not.toContain('private-detail');
  });
  it.each(['EXPIRED','CONSUMED','APPROVED_TEMPORARY'] as const)('shows terminal %s without decision controls', async state => {
    const stored = {...record('terminal',state),input:{...record().input,context:{...record().input.context,deviceId:'device-one'}}};
    renderQueue(vi.fn().mockImplementation(async (url:string) => json(url.includes('?') ? {items:[stored]} : stored)));
    fireEvent.click(await screen.findByRole('button',{name:'Review approval terminal'})); await screen.findByText('device-one');
    expect(screen.queryByRole('heading',{name:'Choose a decision'})).toBeNull(); expect(screen.getAllByRole('status').length).toBe(1);
  });
  it('reloads failed details and refreshes authoritative expiry while a reviewer is reading', async () => {
    vi.useFakeTimers({toFake:['setInterval','clearInterval']});
    let reads=0;
    const fetcher=vi.fn().mockImplementation(async (url:string) => url.includes('?') ? json({items:[record()]}) : ++reads === 1 ? json({},503) : json(reads > 2 ? {...record(),revision:2,state:'EXPIRED'} : record()));
    renderQueue(fetcher); fireEvent.click(await screen.findByRole('button',{name:'Review approval approval-one'}));
    await screen.findByRole('alert'); fireEvent.click(screen.getByRole('button',{name:'Reload approval'})); await screen.findByRole('heading',{name:'Choose a decision'});
    fireEvent.click(screen.getByRole('button',{name:'Approve once'}));
    await act(async () => { await vi.advanceTimersByTimeAsync(15000); });
    await screen.findByText('This approval expired. The requester must make a fresh Gateway request.');
    expect(screen.queryByRole('button',{name:'Confirm approve once'})).toBeNull();
  });
  it('keeps one polling request in flight and stops polling on unmount', async () => {
    vi.useFakeTimers({toFake:['setInterval','clearInterval']}); let resolve!: (value:Response) => void;
    const fetcher=vi.fn().mockImplementationOnce(() => new Promise<Response>(done => {resolve=done;})).mockImplementation(async () => json({items:[]}));
    const view=renderQueue(fetcher);
    await act(async () => { await vi.advanceTimersByTimeAsync(45000); }); expect(fetcher).toHaveBeenCalledOnce();
    await act(async () => { resolve(json({items:[]})); }); await screen.findByRole('heading',{name:'No approvals on this page'});
    await act(async () => { await vi.advanceTimersByTimeAsync(15000); }); expect(fetcher).toHaveBeenCalledTimes(2);
    view.unmount(); await act(async () => { await vi.advanceTimersByTimeAsync(30000); }); expect(fetcher).toHaveBeenCalledTimes(2);
  });
  it('aborts outstanding list/detail reads when the component closes', async () => {
    const signals:AbortSignal[]=[];
    const fetcher=vi.fn().mockImplementation(async (url:string,options:RequestInit) => { signals.push(options.signal!); return json(url.includes('?') ? {items:[record()]} : record()); });
    const view=renderQueue(fetcher); await review(); view.unmount(); expect(signals.length).toBe(2); expect(signals.every(signal => signal.aborted)).toBe(true);
  });
});
