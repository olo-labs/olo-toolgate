// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
import { useEffect, useState, type FormEvent } from 'react';
import type { EndpointEnrollmentReview, EnrollmentChoice } from '@olo-labs/toolgate-contracts';
import { ControlClient } from './api';
import { Failure } from './Failure';
import { Connect } from './Connect';

function localDateTime(time:number) {
  const date=new Date(time);
  return new Date(time-date.getTimezoneOffset()*60000).toISOString().slice(0,16);
}

/** Human confirmation displays server-verified scope; backend owns identity and certificate issuance. */
export function Enrollment({ client }: { client: ControlClient }) {
  const initial = new URLSearchParams(window.location.hash.split('?')[1] ?? '').get('code') ?? '';
  const [code,setCode] = useState(/^[A-F0-9]{16}$/.test(initial) ? initial : '');
  const [review,setReview] = useState<EndpointEnrollmentReview>(); const [error,setError] = useState<unknown>();
  const [busy,setBusy] = useState(false); const [confirmed,setConfirmed] = useState(false);
  const [pending,setPending]=useState<readonly EndpointEnrollmentReview[]>();const [listError,setListError]=useState<unknown>();
  const [refresh,setRefresh]=useState(0);const [refreshing,setRefreshing]=useState(false);
  const [until,setUntil]=useState(()=>localDateTime(Date.now()+86400000));const [timeError,setTimeError]=useState('');
  const [now,setNow]=useState(Date.now());const [liveness,setLiveness]=useState('');
  useEffect(()=>{
    const controller=new AbortController();let timer:ReturnType<typeof setTimeout>;
    async function poll(){
      setRefreshing(true);setNow(Date.now());
      try {const page=await client.pendingEnrollments(controller.signal);if(!controller.signal.aborted){setPending(page.items);setListError(undefined);}}
      catch(failure){if(!controller.signal.aborted)setListError(failure);}
      finally {if(!controller.signal.aborted){setRefreshing(false);timer=setTimeout(()=>void poll(),5000);}}
    }
    void poll();return()=>{controller.abort();clearTimeout(timer);};
  },[client,refresh]);
  useEffect(()=>{
    if(review?.state!=='APPROVED'){setLiveness('');return;}
    const controller=new AbortController();let timer:ReturnType<typeof setTimeout>;const deadline=Date.now()+600000;
    const deviceId=review.deviceId;let connected=false;
    async function poll(){
      if(controller.signal.aborted)return;
      if(review?.connectionExpiresAtUnixMs!==undefined&&Date.now()>=review.connectionExpiresAtUnixMs){setLiveness('Connection approval expired. Device access is blocked.');return;}
      try {
        const device=await client.endpointDevice(deviceId,controller.signal);
        if(controller.signal.aborted)return;
        if(device.state==='REVOKED'){setLiveness('Device revoked. Access remains blocked.');return;}
        if(device.connectionExpiresAtUnixMs!==undefined&&Date.now()>=device.connectionExpiresAtUnixMs){setLiveness('Connection approval expired. Device access is blocked.');return;}
        const age=Date.now()-device.lastSeenUnixMs;
        if(device.deviceId===deviceId&&device.keyFingerprint===review?.keyFingerprint&&device.state==='ACTIVE'&&device.reportSequence>0&&age>=-5000&&age<120000){connected=true;setLiveness('Connected: this device has checked in with ToolGate.');}
        else setLiveness('Waiting for this device to check in…');
      }catch {if(!controller.signal.aborted)setLiveness('Unable to verify device liveness. Waiting to retry…');}
      if(connected||Date.now()<deadline)timer=setTimeout(()=>void poll(),3000);else setLiveness('Device check-in timed out. Review the device again to retry.');
    }
    void poll();return()=>{controller.abort();clearTimeout(timer);};
  },[review,client]);
  function clearReview(){setReview(undefined);setConfirmed(false);setTimeError('');}
  async function load(selectedCode:string) {
    if (busy) return;setBusy(true);setError(undefined);clearReview();setCode(selectedCode);
    try {setReview(await client.enrollment(selectedCode));setUntil(localDateTime(Date.now()+86400000));}
    catch (failure) {setError(failure);} finally {setBusy(false);}
  }
  function submitReview(event:FormEvent){event.preventDefault();void load(code);}
  async function decide(choice: EnrollmentChoice) {
    if (!review || busy || !confirmed) return;
    const connectionExpiresAtUnixMs=new Date(until).getTime();
    if(choice==='APPROVE'&&(!Number.isSafeInteger(connectionExpiresAtUnixMs)||connectionExpiresAtUnixMs<=Date.now())){setTimeError('Choose a future date and time.');return;}
    setBusy(true);setError(undefined);setTimeError('');
    try {
      setReview(await client.decideEnrollment({userCode:review.userCode,keyFingerprint:review.keyFingerprint,choice,
        ...(choice==='APPROVE'?{connectionExpiresAtUnixMs}:{})},crypto.randomUUID()));
      setConfirmed(false);setRefresh(value=>value+1);
    }catch (failure) {setError(failure);clearReview();setRefresh(value=>value+1);} finally {setBusy(false);}
  }
  const validUntil=Number.isSafeInteger(new Date(until).getTime())&&new Date(until).getTime()>now;
  return <><p className="eyebrow">Endpoint identity</p><h1>Enroll this device</h1>
    <Connect onCode={value=>{setCode(value);clearReview();}} onCancel={clearReview} />
    <p>Devices waiting for approval appear below. Compare the code and key fingerprint with the client on that device before approving access.</p>
    <section aria-labelledby="pending-devices"><h2 id="pending-devices">Devices waiting for approval</h2>
      <p>Requests refresh every 5 seconds and expire after 10 minutes.</p>
      <button onClick={()=>setRefresh(value=>value+1)} disabled={refreshing}>Refresh requests</button>
      {Boolean(listError)&&<Failure error={listError}/>}
      {pending===undefined&&!listError&&<p role="status">Loading device requests…</p>}
      {pending?.length===0&&!listError&&<p role="status">No devices are waiting for approval.</p>}
      {Boolean(pending?.length)&&<div className="table-wrap"><table><thead><tr><th scope="col">Device</th><th scope="col">Platform</th><th scope="col">Code</th><th scope="col">Request expires</th><th scope="col">Action</th></tr></thead>
        <tbody>{pending?.map(device=><tr key={device.enrollmentId}><th scope="row">{device.deviceId}</th><td>{device.platform}</td><td><code>{device.userCode}</code></td>
          <td>{new Date(device.expiresAtUnixMs).toLocaleString()}</td><td><button onClick={()=>void load(device.userCode)} disabled={busy||Boolean(listError)||device.expiresAtUnixMs<=now} aria-label={`Review ${device.deviceId}`}>Review</button></td></tr>)}</tbody></table></div>}
    </section>
    {liveness&&<p role="status">{liveness}</p>}
    {Boolean(error) && <Failure error={error} />}
    <form onSubmit={submitReview}><label htmlFor="enrollment-code">Enrollment code</label>
      <input id="enrollment-code" value={code} onChange={event => {setCode(event.target.value.toUpperCase());clearReview();}} maxLength={16} pattern="[A-F0-9]{16}" required disabled={busy} autoComplete="off" />
      <button disabled={busy}>Review device</button></form>
    {busy && <p role="status">Checking enrollment with Control…</p>}
    {review && <section><h2>Device confirmation</h2><dl>
      <dt>Device</dt><dd>{review.deviceId}</dd><dt>Platform</dt><dd>{review.platform}</dd>
      <dt>Code</dt><dd>{review.userCode}</dd><dt>Key fingerprint</dt><dd><code>{review.keyFingerprint}</code></dd>
      <dt>Status</dt><dd>{review.state}</dd><dt>Request expires</dt><dd>{new Date(review.expiresAtUnixMs).toLocaleString()}</dd>
      {review.connectionExpiresAtUnixMs!==undefined&&<><dt>Connection allowed until</dt><dd>{new Date(review.connectionExpiresAtUnixMs).toLocaleString()}</dd></>}</dl>
      {review.state === 'PENDING' && <><label htmlFor="connection-until">Allow connection until</label>
        <input id="connection-until" type="datetime-local" required value={until} min={localDateTime(now)} onChange={event=>{setUntil(event.target.value);setTimeError('');}} disabled={busy} aria-describedby="connection-until-help" />
        <p id="connection-until-help">Your local time ({Intl.DateTimeFormat().resolvedOptions().timeZone}). Access ends automatically at this time. Default: 24 hours.</p>
        {(timeError||!validUntil)&&<p role="alert">{timeError||'Choose a future date and time.'}</p>}
        <label><input type="checkbox" checked={confirmed} onChange={event => setConfirmed(event.target.checked)} disabled={busy} /> I compared the code and fingerprint on my device.</label>
        <button onClick={() => void decide('APPROVE')} disabled={busy || !confirmed || !validUntil || review.expiresAtUnixMs<=now}>Enroll device</button>
        <button onClick={() => void decide('DENY')} disabled={busy || !confirmed || review.expiresAtUnixMs<=now}>Deny enrollment</button></>}
      {review.state === 'APPROVED' && <p role="status">Approved. The protected client will retrieve its certificate and check in.</p>}
    </section>}</>;
}
