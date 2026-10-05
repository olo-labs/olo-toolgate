// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
import { afterEach, describe, expect, it, vi } from 'vitest';
import { cleanup, fireEvent, render, screen } from '@testing-library/react';
import { Enrollment } from '../src/Enrollment';
import { ControlClient } from '../src/api';
afterEach(() => { cleanup(); window.location.hash = ''; });
const review = {enrollmentId:'enrollment-1', userCode:'ABCDEF0123456789', deviceId:'device-1', platform:'LINUX',
  keyFingerprint:'a'.repeat(64), state:'PENDING', expiresAtUnixMs:1900000000000};
describe('Endpoint enrollment', () => {
  it('requires explicit fingerprint comparison and binds the decision to the reviewed key', async () => {
    window.location.hash = '#enroll?code=ABCDEF0123456789';
    const transport = vi.fn<typeof fetch>().mockResolvedValueOnce(new Response(JSON.stringify(review)))
      .mockResolvedValueOnce(new Response(JSON.stringify({...review,state:'APPROVED'})));
    render(<Enrollment client={new ControlClient('test-only-token',vi.fn(),transport)} />);
    fireEvent.click(screen.getByRole('button',{name:'Review device'}));
    const approve = await screen.findByRole('button',{name:'Enroll device'});
    expect((approve as HTMLButtonElement).disabled).toBe(true);
    fireEvent.click(screen.getByRole('checkbox')); fireEvent.click(approve);
    await screen.findByText(/protected client will retrieve/);
    const options = transport.mock.calls[1][1]!;
    expect(JSON.parse(String(options.body))).toEqual({userCode:review.userCode,keyFingerprint:review.keyFingerprint,choice:'APPROVE'});
    expect(String(options.body)).not.toContain('deviceCode');
  });
  it('clears the reviewed identity after a competing decision and requires review again', async () => {
    window.location.hash = '#enroll?code=ABCDEF0123456789';
    const transport = vi.fn<typeof fetch>().mockResolvedValueOnce(new Response(JSON.stringify(review)))
      .mockResolvedValueOnce(new Response(JSON.stringify({code:'CONFLICT',message:'Changed',requestId:'request-1'}),{status:409}));
    render(<Enrollment client={new ControlClient('test-only-token',vi.fn(),transport)} />);
    fireEvent.click(screen.getByRole('button',{name:'Review device'}));
    await screen.findByRole('checkbox'); fireEvent.click(screen.getByRole('checkbox'));
    fireEvent.click(screen.getByRole('button',{name:'Deny enrollment'}));
    await screen.findByRole('alert'); expect(screen.queryByRole('button',{name:'Enroll device'})).toBeNull();
  });
  it('confirms only a fresh authenticated check-in for the reviewed device and key',async()=>{
    window.location.hash='#enroll?code=ABCDEF0123456789';
    const transport=vi.fn<typeof fetch>().mockResolvedValueOnce(new Response(JSON.stringify(review)))
      .mockResolvedValueOnce(new Response(JSON.stringify({...review,state:'APPROVED'})))
      .mockResolvedValueOnce(new Response(JSON.stringify({deviceId:review.deviceId,keyFingerprint:review.keyFingerprint,state:'ACTIVE',reportSequence:1,lastSeenUnixMs:Date.now()})));
    render(<Enrollment client={new ControlClient('test-only-token',vi.fn(),transport)}/>);
    fireEvent.click(screen.getByRole('button',{name:'Review device'}));await screen.findByRole('checkbox');fireEvent.click(screen.getByRole('checkbox'));
    fireEvent.click(screen.getByRole('button',{name:'Enroll device'}));await screen.findByText(/Connected: this device/);
    expect(transport.mock.calls[2][0]).toBe('/api/control/v1/endpoint/devices/device-1');
  });
  it.each([{lastSeenUnixMs:1},{deviceId:'another-device'},{keyFingerprint:'b'.repeat(64)},{reportSequence:0},{state:'REVOKED'}])('does not declare a mismatched, stale or revoked device connected (%j)',async(change)=>{
    window.location.hash='#enroll?code=ABCDEF0123456789';
    const transport=vi.fn<typeof fetch>().mockResolvedValueOnce(new Response(JSON.stringify(review)))
      .mockResolvedValueOnce(new Response(JSON.stringify({...review,state:'APPROVED'})))
      .mockResolvedValueOnce(new Response(JSON.stringify({deviceId:review.deviceId,keyFingerprint:review.keyFingerprint,state:'ACTIVE',reportSequence:1,lastSeenUnixMs:Date.now(),...change})));
    render(<Enrollment client={new ControlClient('test-only-token',vi.fn(),transport)}/>);
    fireEvent.click(screen.getByRole('button',{name:'Review device'}));await screen.findByRole('checkbox');fireEvent.click(screen.getByRole('checkbox'));
    fireEvent.click(screen.getByRole('button',{name:'Enroll device'}));
    await screen.findByText(change.state==='REVOKED'?/Device revoked/:/Waiting for this device/);
    expect(screen.queryByText(/Connected: this device/)).toBeNull();
  });
});
