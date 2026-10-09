// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
import { afterEach, expect, it, vi } from 'vitest';
import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react';
import type { EnterpriseApproval, EnterpriseInvocation } from '@olo-labs/toolgate-contracts';
import { Approvals } from '../src/Approvals';
import { ControlClient } from '../src/api';
afterEach(() => { cleanup(); vi.unstubAllGlobals(); });
const digest = 'a'.repeat(64);
const approval = (): EnterpriseApproval => ({ id: 'approval', approvalType: 'OPERATION', invocationId: 'invocation', requestDigest: digest, authorizationEpoch: 7, directoryRevision: 7, obligationIds: ['review-source', 'review-destination'], reviews: [], state: 'PENDING', revision: 1, expiresAtUnixMs: Date.now() + 60000 });
const invocation = (): EnterpriseInvocation => ({ id: 'invocation', requestDigest: digest, state: 'PENDING_APPROVAL', revision: 1, authorizationEpoch: 7, expiresAtUnixMs: Date.now() + 60000, completedResources: [], diagnosticId: 'diagnostic', evaluation: { context: { requestId: 'invocation', tenantId: 'tenant', mode: 'DELEGATED', userId: 'alice', agentId: 'agent', workloadBindingId: 'workload', chain: [], sessionEpoch: 1, credentialEpoch: 1, bindingId: 'binding', deviceId: 'device' }, toolId: 'copy', action: 'copy', argumentsDigest: digest, resources: [{ kind: 'FILE', locator: 'data/source' }, { kind: 'FILE', locator: '<img src=x onerror=alert(1)>' }], toolDigest: digest, packageDigest: digest, nowUnixMs: Date.now(), authorityRevision: 7, online: true } });
const json = (value: unknown, status = 200) => new Response(JSON.stringify(value), { status });
function queue(post?: (url: string, options: RequestInit) => Promise<Response>) {
  const fetcher = vi.fn<typeof fetch>().mockImplementation(async (url, options) => {
    if (options?.method === 'POST' && post) return post(String(url), options);
    return json(String(url).includes('/access/invocations/') ? invocation() : String(url).includes('?') ? { items: [approval()] } : approval());
  }); render(<Approvals client={new ControlClient('private-token', vi.fn(), fetcher)} />); return fetcher;
}
async function open() { fireEvent.click(await screen.findByRole('button', { name: 'Review approval approval' })); await screen.findByText(/data\/source/); }
function confirm() { fireEvent.click(screen.getByLabelText('I reviewed the complete resource set, identities, versions and execution target.')); fireEvent.click(screen.getByRole('button', { name: 'Confirm decision' })); }
it('shows every affected resource, exact target and digests as text before enabling review', async () => {
  queue(); await open(); expect(screen.getByText(/<img src=x onerror=alert/) ).toBeTruthy(); expect(document.querySelector('img')).toBeNull(); expect(screen.getByText('binding / device')).toBeTruthy();
  expect(screen.getByRole('button', { name: 'Confirm decision' }).hasAttribute('disabled')).toBe(true); expect(screen.getAllByText(digest).length).toBe(4);
  expect(screen.getByLabelText('Policy obligation').querySelectorAll('option')).toHaveLength(2);
});
it('sends one exact obligation with a revision and prevents double submission', async () => {
  let resolve!: (response: Response) => void; const fetcher = queue(async () => new Promise(done => { resolve = done; })); await open(); confirm(); fireEvent.click(screen.getByRole('button', { name: 'Submitting decision…' }));
  await waitFor(() => expect(fetcher.mock.calls.filter(c => c[1]?.method === 'POST')).toHaveLength(1));
  const post = fetcher.mock.calls.find(c => c[1]?.method === 'POST')!; expect(JSON.parse(post[1]?.body as string)).toEqual({ expectedRevision: 1, obligationId: 'review-source', decision: 'APPROVE' });
  expect(new Headers(post[1]?.headers).get('Idempotency-Key')).toBeTruthy(); resolve(json({ ...approval(), revision: 2 }));
  await waitFor(() => expect(screen.getByRole('button', { name: 'Confirm decision' }).hasAttribute('disabled')).toBe(true));
});
it('keeps an exact retry key after a network failure and changes it when the decision changes', async () => {
  const fetcher = queue(async () => json({ code: 'DEPENDENCY_UNAVAILABLE', message: 'private-token' }, 503)); await open(); confirm(); await screen.findByRole('alert');
  fireEvent.click(screen.getByRole('button', { name: 'Confirm decision' })); await waitFor(() => expect(fetcher.mock.calls.filter(c => c[1]?.method === 'POST')).toHaveLength(2));
  let posts = fetcher.mock.calls.filter(c => c[1]?.method === 'POST'); expect(new Headers(posts[0][1]?.headers).get('Idempotency-Key')).toBe(new Headers(posts[1][1]?.headers).get('Idempotency-Key'));
  fireEvent.change(screen.getByLabelText('Decision'), { target: { value: 'DENY' } }); expect(screen.getByRole('button', { name: 'Confirm decision' }).hasAttribute('disabled')).toBe(true); confirm();
  await waitFor(() => expect(fetcher.mock.calls.filter(c => c[1]?.method === 'POST')).toHaveLength(3)); posts = fetcher.mock.calls.filter(c => c[1]?.method === 'POST'); expect(new Headers(posts[2][1]?.headers).get('Idempotency-Key')).not.toBe(new Headers(posts[0][1]?.headers).get('Idempotency-Key')); expect(document.body.textContent).not.toContain('private-token');
});
it('cancels using the current invocation revision instead of the approval revision', async () => {
  const fetcher = queue(async () => json(invocation())); await open(); fireEvent.click(screen.getByLabelText('I reviewed the complete resource set, identities, versions and execution target.')); fireEvent.click(screen.getByRole('button', { name: 'Cancel invocation' }));
  await waitFor(() => expect(fetcher.mock.calls.some(c => String(c[0]).endsWith('/invocation/cancel'))).toBe(true)); const call = fetcher.mock.calls.find(c => String(c[0]).endsWith('/invocation/cancel'))!; expect(new Headers(call[1]?.headers).get('If-Match')).toBe('"1"');
});
it('does not offer decisions when server scope lookup fails', async () => {
  const fetcher = vi.fn<typeof fetch>().mockImplementation(async url => String(url).includes('/access/invocations/') ? json({ code: 'FORBIDDEN', message: 'private-detail' }, 403) : json(String(url).includes('?') ? { items: [approval()] } : approval()));
  render(<Approvals client={new ControlClient('token', vi.fn(), fetcher)} />); fireEvent.click(await screen.findByRole('button', { name: 'Review approval approval' })); await screen.findByRole('alert'); expect(screen.queryByRole('button', { name: 'Confirm decision' })).toBeNull(); expect(document.body.textContent).not.toContain('private-detail');
});
it('aborts details when closed and keeps tokens out of the rendered queue', async () => { queue(); await open(); fireEvent.click(screen.getByRole('button', { name: 'Close approval' })); expect(screen.queryByRole('region', { name: 'Approval details' })).toBeNull(); expect(document.body.textContent).not.toContain('private-token'); });
