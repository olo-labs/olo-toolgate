// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
import { useState, type FormEvent } from 'react';
import type { EndpointEnrollmentReview, EnrollmentChoice } from '@olo-labs/toolgate-contracts';
import { ControlClient } from './api';
import { Failure } from './Failure';

/** Human confirmation displays server-verified scope; backend owns identity and certificate issuance. */
export function Enrollment({ client }: { client: ControlClient }) {
  const initial = new URLSearchParams(window.location.hash.split('?')[1] ?? '').get('code') ?? '';
  const [code,setCode] = useState(/^[A-F0-9]{16}$/.test(initial) ? initial : '');
  const [review,setReview] = useState<EndpointEnrollmentReview>(); const [error,setError] = useState<unknown>();
  const [busy,setBusy] = useState(false); const [confirmed,setConfirmed] = useState(false);
  async function load(event: FormEvent) {
    event.preventDefault(); if (busy) return; setBusy(true);setError(undefined);setReview(undefined);setConfirmed(false);
    try { setReview(await client.enrollment(code)); } catch (failure) { setError(failure); } finally { setBusy(false); }
  }
  async function decide(choice: EnrollmentChoice) {
    if (!review || busy || !confirmed) return;setBusy(true);setError(undefined);
    try { setReview(await client.decideEnrollment({ userCode: review.userCode, keyFingerprint: review.keyFingerprint, choice },crypto.randomUUID()));setConfirmed(false); }
    catch (failure) { setError(failure);setReview(undefined);setConfirmed(false); } finally { setBusy(false); }
  }
  return <><p className="eyebrow">Endpoint identity</p><h1>Enroll this device</h1>
    <p>Confirm only a device you are enrolling. Compare the code and key fingerprint with the client on that device.</p>
    {Boolean(error) && <Failure error={error} />}
    <form onSubmit={load}><label htmlFor="enrollment-code">Enrollment code</label>
      <input id="enrollment-code" value={code} onChange={event => {setCode(event.target.value.toUpperCase());setReview(undefined);setConfirmed(false);}} maxLength={16} pattern="[A-F0-9]{16}" required disabled={busy} autoComplete="off" />
      <button disabled={busy}>Review device</button></form>
    {busy && <p role="status">Checking enrollment with Control…</p>}
    {review && <section><h2>Device confirmation</h2><dl>
      <dt>Device</dt><dd>{review.deviceId}</dd><dt>Platform</dt><dd>{review.platform}</dd>
      <dt>Code</dt><dd>{review.userCode}</dd><dt>Key fingerprint</dt><dd><code>{review.keyFingerprint}</code></dd>
      <dt>Status</dt><dd>{review.state}</dd><dt>Expires</dt><dd>{new Date(review.expiresAtUnixMs).toISOString()}</dd></dl>
      {review.state === 'PENDING' && <><label><input type="checkbox" checked={confirmed} onChange={event => setConfirmed(event.target.checked)} disabled={busy} /> I compared the code and fingerprint on my device.</label>
        <button onClick={() => void decide('APPROVE')} disabled={busy || !confirmed}>Enroll device</button>
        <button onClick={() => void decide('DENY')} disabled={busy || !confirmed}>Deny enrollment</button></>}
      {review.state === 'APPROVED' && <p role="status">Approved. The protected client will retrieve its certificate and check in.</p>}
    </section>}</>;
}
