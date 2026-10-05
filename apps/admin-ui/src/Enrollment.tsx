// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
import { useEffect, useState, type FormEvent } from 'react';
import type { EndpointEnrollmentReview, EnrollmentChoice } from '@olo-labs/toolgate-contracts';
import { ControlClient } from './api';
import { Failure } from './Failure';
import { Connect } from './Connect';

/** Human confirmation displays server-verified scope; backend owns identity and certificate issuance. */
export function Enrollment({ client }: { client: ControlClient }) {
  const initial = new URLSearchParams(window.location.hash.split('?')[1] ?? '').get('code') ?? '';
  const [code,setCode] = useState(/^[A-F0-9]{16}$/.test(initial) ? initial : '');
  const [review,setReview] = useState<EndpointEnrollmentReview>(); const [error,setError] = useState<unknown>();
  const [busy,setBusy] = useState(false); const [confirmed,setConfirmed] = useState(false);
  const [liveness,setLiveness]=useState('');
  useEffect(()=>{
    if(review?.state!=='APPROVED'){setLiveness('');return;}
    const controller=new AbortController();let timer:ReturnType<typeof setTimeout>;const deadline=Date.now()+600000;
    const deviceId=review.deviceId;
    async function poll(){
      if(controller.signal.aborted)return;
      try {
        const device=await client.endpointDevice(deviceId,controller.signal);
        if(controller.signal.aborted)return;
        if(device.state==='REVOKED'){setLiveness('Device revoked. Access remains blocked.');return;}
        const age=Date.now()-device.lastSeenUnixMs;
        if(device.deviceId===deviceId&&device.keyFingerprint===review?.keyFingerprint&&device.state==='ACTIVE'&&device.reportSequence>0&&age>=-5000&&age<120000){setLiveness('Connected: this device has checked in with ToolGate.');return;}
        setLiveness('Waiting for this device to check in…');
      }catch {if(!controller.signal.aborted)setLiveness('Unable to verify device liveness. Waiting to retry…');}
      if(Date.now()<deadline)timer=setTimeout(()=>void poll(),3000);else setLiveness('Device check-in timed out. Review the device again to retry.');
    }
    void poll();return()=>{controller.abort();clearTimeout(timer);};
  },[review,client]);
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
    <Connect onCode={value=>{setCode(value);setReview(undefined);setConfirmed(false);}} onCancel={()=>{setReview(undefined);setConfirmed(false);}} />
    <p>Confirm only a device you are enrolling. Compare the code and key fingerprint with the client on that device.</p>
    {liveness&&<p role="status">{liveness}</p>}
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
