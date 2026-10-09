// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
import {afterEach,expect,it,vi} from 'vitest';
import {cleanup,fireEvent,render,screen,waitFor} from '@testing-library/react';
import type {EnterpriseInvocation,EnterpriseScope} from '@olo-labs/toolgate-contracts';
import {EffectOutcomes,AccessSimulation} from '../src/EffectOutcomes';
import {EnterpriseScopeEditor} from '../src/EnterpriseScopeEditor';
import {ControlClient} from '../src/api';

afterEach(cleanup);
const invocation={id:'effect-1',revision:7,state:'OUTCOME_UNKNOWN',completedResources:[],evaluation:{toolId:'payment',argumentsDigest:'a'.repeat(64),context:{deviceId:'device-1'},resources:[{kind:'CUSTOM',locator:'account/123'}]}} as unknown as EnterpriseInvocation;
it('proposes an exact unknown outcome with downstream evidence and retains its retry key',async()=>{
  const transport=vi.fn<typeof fetch>().mockImplementation(async(_url,options)=>new Response(JSON.stringify(options?.method==='POST'?{code:'DEPENDENCY_UNAVAILABLE'}:{items:[invocation]}),{status:options?.method==='POST'?503:200}));
  render(<EffectOutcomes client={new ControlClient('secret-token',vi.fn(),transport)}/>);
  fireEvent.click(await screen.findByRole('button',{name:'effect-1'}));
  fireEvent.change(screen.getByLabelText('External evidence SHA-256'),{target:{value:'b'.repeat(64)}});
  fireEvent.click(screen.getByRole('button',{name:'Submit reconciliation for independent review'}));
  await screen.findByRole('alert');
  fireEvent.click(screen.getByRole('button',{name:'Submit reconciliation for independent review'}));
  await waitFor(()=>expect(transport.mock.calls.filter(c=>c[1]?.method==='POST')).toHaveLength(2));
  const posts=transport.mock.calls.filter(c=>c[1]?.method==='POST');
  expect(JSON.parse(posts[0][1]?.body as string)).toEqual({expectedRevision:7,state:'FAILED',completedResources:[],evidenceDigest:'b'.repeat(64)});
  expect(new Headers(posts[0][1]?.headers).get('Idempotency-Key')).toBe(new Headers(posts[1][1]?.headers).get('Idempotency-Key'));
  expect(document.body.textContent).not.toContain('secret-token');
});
it('invalid simulation JSON fails locally without issuing an effect request',async()=>{
  const transport=vi.fn<typeof fetch>();render(<AccessSimulation client={new ControlClient('token',vi.fn(),transport)}/>);
  fireEvent.change(screen.getByLabelText('Complete evaluation JSON'),{target:{value:'{bad json'}});
  fireEvent.click(screen.getByRole('button',{name:'Evaluate access'}));await screen.findByRole('alert');expect(transport).not.toHaveBeenCalled();
});
it('malformed advanced scope JSON remains editable without crashing the structured editor',()=>{
  render(<EnterpriseScopeEditor client={new ControlClient('token',vi.fn())} value={{resources:null} as unknown as EnterpriseScope} change={vi.fn()}/>);
  expect(screen.getByRole('alert')).toBeTruthy();expect(screen.queryByRole('button',{name:'Add resource rule'})).toBeNull();
});
