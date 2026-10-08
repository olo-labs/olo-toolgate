// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
import {act,render,screen,cleanup,fireEvent} from '@testing-library/react';
import {afterEach,expect,test,vi} from 'vitest';
import {RemoteRequests} from '../src/RemoteRequests';
import {ControlClient} from '../src/api';
import type {RemoteToolRecord} from '@olo-labs/toolgate-contracts';
afterEach(()=>{cleanup();vi.useRealTimers();});
const request:RemoteToolRecord={requestId:'request-one',toolId:'local.tool',agentId:'agent-one',deviceId:'client-one',state:'WAITING_FOR_POLL',receivedAtUnixMs:1,expiresAtUnixMs:30001};
test('polls progress and retains completed handoffs without request contents',async()=>{
  vi.useFakeTimers();const poll=vi.fn().mockResolvedValueOnce({items:[request]}).mockResolvedValue({items:[{...request,state:'DONE',submittedAtUnixMs:2,responseAtUnixMs:3,completedAtUnixMs:4}]});
  const client={remoteRequests:poll} as unknown as ControlClient;
  render(<RemoteRequests client={client}/>);await act(async()=>{});
  expect(screen.getByText('Agent request received for tool local.tool')).toBeTruthy();
  expect(screen.getAllByText('Waiting for next client poll').length).toBeGreaterThan(0);
  await act(async()=>{await vi.advanceTimersByTimeAsync(2000);});
  expect(poll).toHaveBeenCalledTimes(2);expect(screen.getByText('Request submitted to client')).toBeTruthy();expect(screen.getByText('Response received')).toBeTruthy();expect(screen.getAllByText('Done').length).toBeGreaterThan(0);
  cleanup();await act(async()=>{await vi.advanceTimersByTimeAsync(5000);});expect(poll).toHaveBeenCalledTimes(2);
});
test('inspects a completed response as text without rendering returned HTML',async()=>{
  const completed={...request,state:'DONE' as const,completedAtUnixMs:4};
  const transport=vi.fn<typeof fetch>().mockImplementation(async path=>new Response(JSON.stringify(String(path).endsWith('/request-one')?{record:completed,output:{text:'<script>do not execute</script>'}}:{items:[completed]})));
  render(<RemoteRequests client={new ControlClient('test-token',vi.fn(),transport)}/>);
  const region=screen.getByRole('region',{name:'Client tool requests'});expect(region.className).toBe('remote-requests');
  fireEvent.click(await screen.findByRole('button',{name:'View response request-one'}));
  expect(await screen.findByText(/<script>do not execute<\/script>/)).toBeTruthy();
  expect(screen.getByRole('region',{name:'Client response'}).querySelector('script')).toBeNull();
  expect(transport.mock.calls.some(call=>call[0]==='/api/control/v1/mcp/requests/request-one')).toBe(true);
  fireEvent.click(screen.getByRole('button',{name:'Close response'}));expect(screen.queryByRole('region',{name:'Client response'})).toBeNull();
});
test('compact dashboard keeps its bounded card without response fetches',async()=>{
  const inspect=vi.fn();render(<RemoteRequests compact client={{remoteRequests:vi.fn().mockResolvedValue({items:[request]}),remoteRequest:inspect} as unknown as ControlClient}/>);
  await screen.findByText('Latest 10 client requests');expect(screen.getByRole('region',{name:'Client tool requests'}).className).toBe('guidance');
  expect(screen.queryByRole('button',{name:/View response/})).toBeNull();expect(inspect).not.toHaveBeenCalled();
});
