// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
import {useEffect,useRef,useState,type FormEvent} from 'react';
import type {EndpointManagedDevice} from '@olo-labs/toolgate-contracts';
import {ControlClient} from './api';
import {Failure} from './Failure';
import {ApprovalDuration,defaultDuration,durationBody,localDateTime,validDuration} from './ApprovalDuration';
import {DirectoryEditor} from './DirectoryEditor';
import {DeviceDetails} from './DeviceDetails';
import {DeviceMembership,PublishAccess} from './AccessMapping';
import {ScopePicker} from './ScopePicker';

function approved(row:EndpointManagedDevice,now:number){const device=row.endpointDevice;return Boolean(device&&device.state==='ACTIVE'&&device.connectionApproved!==false&&(device.connectionExpiresAtUnixMs===undefined||device.connectionExpiresAtUnixMs>now));}
function connection(row:EndpointManagedDevice,now:number){
  if(row.registeredUser?.enabled===false)return 'User disabled';
  if(row.systemExecutor)return row.directoryDevice?.enabled===false?'Disabled':row.systemAvailable===true?'Available':row.systemAvailable===false?'Unavailable':'Availability unknown';
  if(row.directoryDevice?.enabled===false)return 'Disabled';
  if(!approved(row,now))return row.endpointDevice?.state==='REVOKED'?'Revoked':'Needs approval';
  const device=row.endpointDevice!;const age=now-device.lastSeenUnixMs;
  return device.reportSequence>0&&device.lastSeenUnixMs>0&&age>=-5000&&age<120000?'Connected':'Offline';
}
function purpose(row:EndpointManagedDevice){return row.systemExecutorKind==='HOTFOLDER'?'Runs file tools inside the server’s configured HotFolder. Disabling this device blocks HotFolder operations.':row.systemExecutorKind==='REST_FORWARDING'?'The existing ToolGate gateway forwards tool calls to approved clients. Disabling this device blocks that forwarding.':row.systemExecutor?'Quickstart’s internal fixed executor runs compute and server tools. Disabling it blocks all server built-in operations, including HotFolder.':'An installed client with a protected device key. Both approval and enablement are required for access.';}
function details(row:EndpointManagedDevice,now:number){return `${purpose(row)}\nDevice: ${row.directoryDevice?.name??`${row.enrollment?.platform??'New'} client`} (${row.deviceId})\nSystem name: ${row.systemName??row.endpointDevice?.systemName??'Not reported'}\nIP address: ${row.ipAddress??row.endpointDevice?.ipAddress??'Not reported'}\nRegistered user: ${row.registeredUser?.name??row.directoryDevice?.ownerUserId??'Awaiting registration'}${row.registeredUser?` (${row.registeredUser.id})`:''}\nConnection: ${connection(row,now)}\n${row.systemExecutor?'Identity and approval: server managed':`Key fingerprint: ${row.endpointDevice?.keyFingerprint??row.enrollment?.keyFingerprint??'No enrolled key'}\nApproval: ${approved(row,now)?'Approved':'Needs approval'}\nLast check-in: ${row.endpointDevice?.lastSeenUnixMs?new Date(row.endpointDevice.lastSeenUnixMs).toLocaleString():'No check-in yet'}`}`;}

/** Single bounded snapshot includes enrolled keys, directory-only devices and pending requests. */
export function Devices({client}:{client:ControlClient}){
  const [items,setItems]=useState<readonly EndpointManagedDevice[]>();const [error,setError]=useState<unknown>();const [listError,setListError]=useState<unknown>();
  const [now,setNow]=useState(Date.now());const [refresh,setRefresh]=useState(0);const [busy,setBusy]=useState(false);
  const [selected,setSelected]=useState<EndpointManagedDevice>();const [creating,setCreating]=useState(false);const [editing,setEditing]=useState<EndpointManagedDevice>();
  const [duration,setDuration]=useState(defaultDuration);const [confirmed,setConfirmed]=useState(false);const [notice,setNotice]=useState('');
  const [membership,setMembership]=useState<string>();
  const [approvalGroups,setApprovalGroups]=useState<string[]>(['default-devices']);
  const [approvalGroupsReady,setApprovalGroupsReady]=useState(true);const approvalChoice=useRef(0);
  const alive=useRef(true);const guard=useRef(false);const heading=useRef<HTMLHeadingElement>(null);
  useEffect(()=>{heading.current?.focus();},[selected]);
  useEffect(()=>{alive.current=true;return()=>{alive.current=false;};},[]);
  useEffect(()=>{
    const controller=new AbortController();let timer:ReturnType<typeof setTimeout>;
    async function poll(){
      setNow(Date.now());
      try{const page=await client.managedDevices(controller.signal);if(!controller.signal.aborted){setItems(page.items);setListError(undefined);}}
      catch(failure){if(!controller.signal.aborted)setListError(failure);}
      if(!controller.signal.aborted)timer=setTimeout(()=>void poll(),2000);
    }
    void poll();return()=>{controller.abort();clearTimeout(timer);};
  },[client,refresh]);
  const reload=()=>setRefresh(value=>value+1);
  async function mutate(action:()=>Promise<unknown>,message:string){
    if(guard.current)return;guard.current=true;setBusy(true);setError(undefined);setNotice('');
    try{await action();if(alive.current){setSelected(undefined);setConfirmed(false);setNotice(message);reload();}}
    catch(failure){if(alive.current){setError(failure);setSelected(undefined);setConfirmed(false);reload();}}
    finally{guard.current=false;if(alive.current)setBusy(false);}
  }
  function choose(row:EndpointManagedDevice){
    const choice=++approvalChoice.current;setApprovalGroupsReady(!row.directoryDevice);
    setApprovalGroups(['default-devices']);
    if(row.directoryDevice)void client.memberships('devices',row.deviceId).then(value=>{if(alive.current&&choice===approvalChoice.current){setApprovalGroups([...value.groupIds]);setApprovalGroupsReady(true);}}).catch(failure=>{if(alive.current&&choice===approvalChoice.current)setError(failure);});
    setSelected(row);setCreating(false);setEditing(undefined);setConfirmed(false);setError(undefined);setNotice('');
    const deadline=row.endpointDevice?.connectionExpiresAtUnixMs;
    setDuration(row.endpointDevice&&deadline===undefined?{...defaultDuration(),unlimited:true}:deadline!==undefined&&deadline>Date.now()?{unlimited:false,until:localDateTime(deadline)}:defaultDuration());
  }
  function submit(event:FormEvent){
    event.preventDefault();if(!selected||!confirmed||!approvalGroupsReady||!approvalGroups.length||!validDuration(duration,Date.now()))return;
    const row=selected;const endpoint=row.endpointDevice;
    void mutate(async()=>{
      if(endpoint)await client.setDeviceApproval(row.deviceId,{expectedApprovalRevision:endpoint.approvalRevision??1,approved:true,...durationBody(duration)},crypto.randomUUID());
      else await client.decideEnrollment({userCode:row.enrollment!.userCode,keyFingerprint:row.enrollment!.keyFingerprint,choice:'APPROVE',...durationBody(duration)},crypto.randomUUID());
      const current=await client.memberships('devices',row.deviceId);
      if([...current.groupIds].sort().join('\n')!==[...approvalGroups].sort().join('\n'))await client.saveMemberships('devices',{...current,groupIds:approvalGroups},crypto.randomUUID());
    },'Device approved.');
  }
  return <><div className="page-heading"><div><p className="eyebrow">Device access</p><h1>Clients</h1></div><div className="actions"><button onClick={reload} disabled={busy}>Refresh</button><button onClick={()=>{setCreating(true);setEditing(undefined);setSelected(undefined);}} disabled={busy}>Add client</button></div></div>
    <p>All registered devices and requests waiting for approval. Enablement and approval are separate: both are required for client access. This list refreshes every two seconds.</p>
    {Boolean(error)&&<Failure error={error}/>}{Boolean(listError)&&<Failure error={listError}/>}{notice&&<p role="status">{notice}</p>}
    {!items&&!listError&&<p role="status">Loading clients…</p>}
    {items?.length===0&&<p>No devices or pending requests.</p>}
    {Boolean(items?.length)&&<div className="table-wrap"><table><caption className="sr-only">Clients and device approvals</caption><thead><tr><th scope="col">Device name</th><th scope="col">System name</th><th scope="col">IP address</th><th scope="col">Registered user</th><th scope="col">Identifier</th><th scope="col">Connection</th><th scope="col">Directory status</th><th scope="col">Approval</th><th scope="col">Allowed until</th><th scope="col">Actions</th></tr></thead>
      <tbody>{items?.map(row=>{
        const directory=row.directoryDevice,endpoint=row.endpointDevice;const isApproved=approved(row,now);const state=connection(row,now);
        return <tr key={row.deviceId} title={details(row,now)}><th scope="row">{directory?<button className="record-link" onClick={()=>{setEditing(row);setCreating(false);setSelected(undefined);}}>{directory.name}</button>:`${row.enrollment?.platform??'New'} client`} <DeviceDetails deviceId={row.deviceId} text={details(row,now)}/>{row.systemExecutor&&<small>Internal Quickstart device</small>}</th><td>{row.systemName??endpoint?.systemName??'Not reported'}</td><td><code>{row.ipAddress??endpoint?.ipAddress??'Not reported'}</code></td><td>{row.registeredUser?<>{row.registeredUser.name}<small>{row.registeredUser.id}{!row.registeredUser.enabled?' · Disabled user':''}</small></>:directory?.ownerUserId??endpoint?.userId??'Awaiting registration'}</td><td><code>{row.deviceId}</code></td>
          <td><span className={`client-connection ${!listError&&(state==='Connected'||state==='Available')?'online':'offline'}`}><span className="connection-dot" aria-hidden="true"/>{listError?'Status unavailable':state}</span></td>
          <td>{directory?.enabled===false?'Disabled':directory?'Enabled':'Awaiting enrollment'}</td>
          <td>{row.systemExecutor?'Built-in executor':isApproved?'Approved':endpoint?.state==='REVOKED'?'Revoked':endpoint&&endpoint.connectionApproved!==false&&endpoint.connectionExpiresAtUnixMs!==undefined&&endpoint.connectionExpiresAtUnixMs<=now?'Approval expired':row.enrollment||endpoint?'Needs approval':'Needs enrollment'}</td>
          <td>{row.systemExecutor?'Server managed':isApproved?(endpoint?.connectionExpiresAtUnixMs===undefined?'Unlimited':new Date(endpoint.connectionExpiresAtUnixMs).toLocaleString()):'—'}</td>
          <td><div className="actions">{directory&&<button disabled={busy} aria-label={`Groups for ${row.deviceId}`} onClick={()=>setMembership(row.deviceId)}>Groups</button>}<button disabled={busy} aria-label={`${directory?.enabled===false?'Enable':'Disable'} ${row.deviceId}`} onClick={()=>void mutate(()=>client.setDeviceEnabled(row.deviceId,{expectedRevision:directory?.revision??0,enabled:directory?.enabled===false},crypto.randomUUID()),directory?.enabled===false?'Device enabled.':'Device disabled.')}>{directory?.enabled===false?'Enable':'Disable'}</button>
            {!row.systemExecutor&&(endpoint||row.enrollment)&&<button disabled={busy||endpoint?.state==='REVOKED'} aria-label={`${isApproved?'Deapprove':'Approve'} ${row.deviceId}`} onClick={()=>isApproved?void mutate(()=>client.setDeviceApproval(row.deviceId,{expectedApprovalRevision:endpoint?.approvalRevision??1,approved:false},crypto.randomUUID()),'Device deapproved.'):choose(row)}>{isApproved?'Deapprove':'Approve'}</button>}
            {isApproved&&<button disabled={busy} onClick={()=>choose(row)} aria-label={`Change approval ${row.deviceId}`}>Change approval</button>}</div></td></tr>;
      })}</tbody></table></div>}
    {items?.some(row=>row.systemExecutor)&&<p className="hint">Quickstart’s internal devices are created automatically for the fixed executor, HotFolder, and REST call forwarding. They use server-managed identity and appear green while available and enabled. Installed clients require separate approval.</p>}
    {selected&&<section className="guidance"><h2 ref={heading} tabIndex={-1}>Approve {selected.deviceId}</h2><p>Owner: {selected.endpointDevice?.userId??selected.directoryDevice?.ownerUserId??'Your signed-in user'}</p><p>Key fingerprint: <code>{selected.endpointDevice?.keyFingerprint??selected.enrollment?.keyFingerprint}</code></p>
      {selected.enrollment&&<p>Enrollment code: <code>{selected.enrollment.userCode}</code></p>}
      <form onSubmit={submit}><ApprovalDuration value={duration} onChange={setDuration} now={now} disabled={busy}/>
        {!approvalGroupsReady&&<p role="status">Loading current device groups…</p>}
        <ScopePicker client={client} kind="deviceGroups" label="Device groups" value={approvalGroups} onChange={setApprovalGroups} disabled={busy||!approvalGroupsReady}/>
        <p className="hint">The device is approved first. A membership change creates a configuration draft and requires independent approval before its group permissions apply.</p>
        <label><input type="checkbox" checked={confirmed} onChange={event=>setConfirmed(event.target.checked)} disabled={busy}/> {selected.enrollment?'I compared the code and fingerprint on the requesting device.':'I confirm access for this enrolled device and owner.'}</label>
        <div className="actions"><button disabled={busy||!confirmed||!approvalGroupsReady||!approvalGroups.length||!validDuration(duration,now)||Boolean(selected.enrollment&&selected.enrollment.expiresAtUnixMs<=now)}>Approve device</button><button type="button" disabled={busy} onClick={()=>setSelected(undefined)}>Cancel</button></div></form></section>}
    {membership&&<DeviceMembership key={membership} client={client} id={membership} close={()=>setMembership(undefined)} saved={()=>{setNotice('Device membership saved. Publish access changes to update gateways.');reload();}}/>}
    <PublishAccess client={client}/>
    {(creating||editing?.directoryDevice)&&<DirectoryEditor client={client} kind="devices" record={editing?.directoryDevice} close={()=>{setCreating(false);setEditing(undefined);}} saved={reload}/>}</>;
}
