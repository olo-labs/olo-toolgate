// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
import {act,render,screen,cleanup} from '@testing-library/react';
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
