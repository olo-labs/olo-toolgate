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
});
