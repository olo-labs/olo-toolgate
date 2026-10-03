// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
import { useEffect, useRef, useState } from 'react';
import type { ApprovalChoice, ApprovalDecisionRequest, ApprovalPage, ApprovalRecord, ApprovalState } from '@olo-labs/toolgate-contracts';
import { ApiError, ControlClient } from './api';
import { Failure } from './Failure';

const POLL_MS = 15000;
const labels: Record<ApprovalState, string> = {
  PENDING: 'Pending review', APPROVED_ONCE: 'Approved once', APPROVED_TEMPORARY: 'Approved temporarily',
  DENIED: 'Denied', EXPIRED: 'Expired', CONSUMED: 'Consumed',
};
const choiceLabels: Record<ApprovalChoice, string> = { APPROVE_ONCE: 'approve once', APPROVE_TEMPORARY: 'approve temporarily', DENY: 'deny' };

function Timestamp({ value }: { value: number }) {
  return <time dateTime={new Date(value).toISOString()}>{new Date(value).toLocaleString()}</time>;
}

/** The queue renders authoritative states; Control owns roles, expiry and grants. */
export function Approvals({ client }: { client: ControlClient }) {
  const [page, setPage] = useState<ApprovalPage>();
  const [cursor, setCursor] = useState<string>(); const [history, setHistory] = useState<(string | undefined)[]>([]);
  const [error, setError] = useState<unknown>(); const [attempt, setAttempt] = useState(0);
  const [filter, setFilter] = useState<'all' | 'pending'>('all'); const [selected, setSelected] = useState<string>();
  useEffect(() => {
    const abort = new AbortController(); let loading = false;
    setPage(undefined); setError(undefined);
    async function load() {
      if (loading) return; loading = true;
      try { const result = await client.approvals(cursor, abort.signal); if (!abort.signal.aborted) { setPage(result); setError(undefined); } }
      catch (failure) { if (!abort.signal.aborted) setError(failure); }
      finally { loading = false; }
    }
    void load(); const interval = window.setInterval(() => { void load(); }, POLL_MS);
    return () => { abort.abort(); window.clearInterval(interval); };
  }, [client, cursor, attempt]);
  const refresh = () => setAttempt(value => value + 1);
  const visible = page?.items.filter(record => filter === 'all' || record.state === 'PENDING') ?? [];
  return <><div className="page-heading"><div><p className="eyebrow">Human review</p><h1>Approvals</h1></div><button onClick={refresh}>Refresh approvals</button></div>
    <p className="intro">Review exact operations waiting for a human decision. Approval is limited to the displayed identities, resource, arguments digest and policy version. The requester retries through Gateway after your decision.</p>
    <div className="approval-filters" role="group" aria-label="Approval status filter"><button aria-pressed={filter === 'all'} onClick={() => setFilter('all')}>All states on this page</button><button aria-pressed={filter === 'pending'} onClick={() => setFilter('pending')}>Pending on this page</button></div>
    <p className="hint">The queue refreshes every 15 seconds, up to 50 records per page. Control verifies approver permission and current policy when you submit a decision.</p>
    {Boolean(error) && <Failure error={error} retry={refresh} />}
    {!page && !error ? <p role="status">Loading approvals…</p> : page && <>
      {visible.length === 0 ? <section className="empty"><h2>{filter === 'pending' ? 'No pending approvals on this page' : 'No approvals on this page'}</h2><p>Gateway creates requests when current policy requires human review.</p></section> :
        <div className="table-wrap"><table><caption className="sr-only">Approval requests</caption><thead><tr><th scope="col">Request</th><th scope="col">Operation</th><th scope="col">Requester</th><th scope="col">State</th><th scope="col">Expires</th></tr></thead><tbody>{visible.map(record => <tr key={record.id}>
          <th scope="row"><button className="record-link" onClick={() => setSelected(record.id)} aria-label={`Review approval ${record.id}`}><code>{record.id}</code></button></th>
          <td><code>{record.input.toolId}</code><br />{record.input.action}</td><td><code>{record.input.context.userId}</code></td><td><span className={`status approval-${record.state.toLowerCase()}`}>{labels[record.state]}</span></td><td><Timestamp value={record.expiresAtUnixMs} /></td>
        </tr>)}</tbody></table></div>}
      <div className="pagination"><span>{page.items.length} approval records on this page</span><div className="actions"><button disabled={!history.length} onClick={() => { setCursor(history.at(-1)); setHistory(history.slice(0, -1)); setSelected(undefined); }}>Previous approvals page</button><button disabled={!page.nextCursor} onClick={() => { setHistory([...history, cursor]); setCursor(page.nextCursor); setSelected(undefined); }}>Next approvals page</button></div></div>
    </>}
    {selected && <ApprovalDetails key={selected} client={client} id={selected} close={() => setSelected(undefined)} changed={refresh} />}
  </>;
}

function Scope({ record }: { record: ApprovalRecord }) {
  const input = record.input;
  return <dl className="policy-details approval-scope">
    <dt>Tenant</dt><dd><code>{input.context.tenantId}</code></dd>
    <dt>Requester</dt><dd><code>{input.context.userId}</code></dd>
    <dt>Agent</dt><dd><code>{input.context.agentId}</code></dd>
    <dt>Device</dt><dd>{input.context.deviceId ? <code>{input.context.deviceId}</code> : 'No device identity'}</dd>
    <dt>Tool</dt><dd><code>{input.toolId}</code></dd><dt>Action</dt><dd>{input.action}</dd>
    <dt>Resource kind</dt><dd>{input.resource.kind}</dd><dt>Exact resource</dt><dd><code>{input.resource.locator}</code></dd>
    <dt>Arguments digest</dt><dd><code>{input.argumentsDigest}</code><p className="hint">Only this digest is approved. Raw arguments and credentials are not stored in this record.</p></dd>
    <dt>Policy version</dt><dd><code>{record.policyVersion}</code></dd>
    <dt>Origin request</dt><dd><code>{input.context.requestId}</code><p className="hint">A retry keeps the exact operation binding; each execution permit binds its own request.</p></dd>
    <dt>Created</dt><dd><Timestamp value={record.createdAtUnixMs} /></dd><dt>Expires</dt><dd><Timestamp value={record.expiresAtUnixMs} /></dd>
    {record.decidedBy && <><dt>Decided by</dt><dd><code>{record.decidedBy}</code></dd></>}
    {record.decidedAtUnixMs !== undefined && <><dt>Decided at</dt><dd><Timestamp value={record.decidedAtUnixMs} /></dd></>}
  </dl>;
}

function ApprovalDetails({ client, id, close, changed }: { client: ControlClient; id: string; close: () => void; changed: () => void }) {
  const [record, setRecord] = useState<ApprovalRecord>(); const recordRef = useRef<ApprovalRecord | undefined>(undefined);
  const [error, setError] = useState<unknown>(); const [busy, setBusy] = useState(false); const busyRef = useRef(false);
  const [choice, setChoice] = useState<ApprovalChoice>(); const [duration, setDuration] = useState(600000); const [confirmed, setConfirmed] = useState(false);
  const [attempt, setAttempt] = useState(0); const alive = useRef(true); const heading = useRef<HTMLHeadingElement>(null);
  const pending = useRef<{ body: string; key: string } | undefined>(undefined); const serial = useRef(0);
  function applyRecord(result: ApprovalRecord) {
    if (recordRef.current && (recordRef.current.revision !== result.revision || recordRef.current.state !== result.state)) { setChoice(undefined); setConfirmed(false); pending.current = undefined; }
    recordRef.current = result; setRecord(result);
  }
  useEffect(() => {
    alive.current = true; heading.current?.focus(); const abort = new AbortController(); let loading = false;
    async function load() {
      if (loading || busyRef.current) return; loading = true; const request = ++serial.current;
      try { const result = await client.approval(id, abort.signal); if (!abort.signal.aborted && request === serial.current) { applyRecord(result); setError(undefined); } }
      catch (failure) { if (!abort.signal.aborted && request === serial.current) setError(failure); }
      finally { loading = false; }
    }
    void load(); const interval = window.setInterval(() => { void load(); }, POLL_MS);
    return () => { alive.current = false; abort.abort(); window.clearInterval(interval); };
  }, [client, id, attempt]);
  async function decide() {
    if (!record || !choice || !confirmed || busyRef.current) return;
    const body: ApprovalDecisionRequest = { decision: choice, expectedRevision: record.revision, ...(choice === 'APPROVE_TEMPORARY' ? { durationMs: duration } : {}) };
    const encoded = JSON.stringify(body); if (pending.current?.body !== encoded) pending.current = { body: encoded, key: crypto.randomUUID() };
    busyRef.current = true; setBusy(true); setError(undefined); ++serial.current;
    try {
      const result = await client.decideApproval(id, body, pending.current.key);
      if (alive.current) { applyRecord(result); setChoice(undefined); setConfirmed(false); pending.current = undefined; changed(); }
    } catch (failure) {
      if (alive.current) setError(failure);
      if (failure instanceof ApiError && failure.status === 409) {
        try { const latest = await client.approval(id); if (alive.current) { applyRecord(latest); setChoice(undefined); setConfirmed(false); pending.current = undefined; changed(); } }
        catch (refreshFailure) { if (alive.current) setError(refreshFailure); }
      }
    } finally { busyRef.current = false; if (alive.current) setBusy(false); }
  }
  function choose(value: ApprovalChoice) { setChoice(value); setConfirmed(false); setError(undefined); }
  return <section className="detail" aria-label="Approval details"><div className="page-heading"><h2 tabIndex={-1} ref={heading}>Review exact operation</h2><button onClick={close} disabled={busy}>Close approval</button></div>
    {Boolean(error) && <Failure error={error} />}
    {!record ? <>{!error && <p role="status">Loading approval details…</p>}<button onClick={() => setAttempt(value => value + 1)} disabled={busy}>Reload approval</button></> : <>
      <p>Approval <code>{record.id}</code> · Revision {record.revision} · <strong>{labels[record.state]}</strong></p>
      <Scope record={record} />
      {record.state === 'PENDING' ? <div className="approval-decision"><h3>Choose a decision</h3><p>Approve once covers one execution. Temporary approval covers repeated requests with the same exact binding until its deadline. Gateway still checks current policy and availability.</p>
        <div className="actions"><button disabled={busy} aria-pressed={choice === 'APPROVE_ONCE'} onClick={() => choose('APPROVE_ONCE')}>Approve once</button><button disabled={busy} aria-pressed={choice === 'APPROVE_TEMPORARY'} onClick={() => choose('APPROVE_TEMPORARY')}>Approve temporarily</button><button disabled={busy} className="danger" aria-pressed={choice === 'DENY'} onClick={() => choose('DENY')}>Deny request</button></div>
        {choice && <div className="approval-confirmation">
          {choice === 'APPROVE_TEMPORARY' && <><label htmlFor="approval-duration">Maximum approval duration</label><select id="approval-duration" value={duration} disabled={busy} onChange={event => { setDuration(Number(event.target.value)); setConfirmed(false); }}><option value={600000}>10 minutes</option><option value={1800000}>30 minutes</option></select><p className="hint">Control caps the deadline at current policy expiry.</p></>}
          <p>You are about to {choiceLabels[choice]} this exact operation.</p><label className="checkbox"><input type="checkbox" checked={confirmed} disabled={busy} onChange={event => setConfirmed(event.target.checked)} />I reviewed the identities, resource, arguments digest and policy version.</label>
          <div className="actions"><button className={choice === 'DENY' ? 'danger' : 'primary'} disabled={busy || !confirmed} onClick={() => { void decide(); }}>{busy ? 'Submitting decision…' : `Confirm ${choiceLabels[choice]}`}</button><button disabled={busy} onClick={() => { setChoice(undefined); setConfirmed(false); }}>Cancel decision</button></div>
        </div>}
      </div> : <div className="notice approval-result" role="status">{record.state === 'EXPIRED' ? 'This approval expired. The requester must make a fresh Gateway request.' : record.state === 'DENIED' ? 'This request was denied. No execution permission was granted.' : record.state === 'CONSUMED' ? 'The one-time approval has been consumed.' : 'The decision is recorded. The requester retries through Gateway; the current policy must still permit approval.'}</div>}
      <button disabled={busy} onClick={() => setAttempt(value => value + 1)}>Reload approval</button>
    </>}
  </section>;
}
