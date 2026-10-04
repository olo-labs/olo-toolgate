// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
import { useEffect, useRef, useState, type FormEvent } from 'react';
import type { DirectoryKind, DirectoryRecords } from './operations.generated';
import { ControlClient } from './api';
import type { UserRole, UserPrivilegeTemplate } from '@olo-labs/toolgate-contracts';
import { Failure } from './Failure';

type Kind = Exclude<DirectoryKind,'users'>;
const titles:Record<Kind,string>={teams:'team',roles:'role',tools:'tool',policies:'policy',agents:'agent',devices:'client'};
/** Field editing only; Control remains responsible for schema, references and permissions. */
export function DirectoryEditor({client,kind,record,close,saved}:{client:ControlClient;kind:Kind;record?:DirectoryRecords[Kind];close:()=>void;saved:()=>void}) {
  const [id,setId]=useState(record?.id??''),[name,setName]=useState(record?.name??''),[enabled,setEnabled]=useState(record?.enabled??false);
  const [owner,setOwner]=useState(record&&'ownerUserId' in record?record.ownerUserId:''),[members,setMembers]=useState(record&&'userIds' in record?record.userIds.join(', '):'');
  const [roles,setRoles]=useState(record&&'roleIds' in record?(record.roleIds??[]).join(', '):'');
  const [portalRole,setPortalRole]=useState<UserRole>(record&&'portalRole' in record?record.portalRole:'BASIC');
  const [templates,setTemplates]=useState<ReadonlyArray<UserPrivilegeTemplate>>(record&&'rules' in record?record.rules.templateIds:[]);
  const defaults=kind==='tools'?{definition:{id:'',name:'',description:'',actions:[{name:'invoke',resourceKinds:['CUSTOM']}],inputSchema:{type:'object'},outputSchema:{type:'object'}}}:kind==='policies'?{toolId:'',action:'invoke',resource:{kind:'CUSTOM',locator:''},decision:'BLOCK',userIds:[],teamIds:[],agentIds:[],deviceIds:[]}:kind==='roles'?{deviceScope:'NONE',deviceGroupIds:[],toolIds:[]}:{};
  const [config,setConfig]=useState(JSON.stringify(record&&'rules' in record?Object.fromEntries(Object.entries(record.rules).filter(([key])=>key!=='templateIds')):record?Object.fromEntries(Object.entries(record).filter(([key])=>!['id','name','enabled','revision','ownerUserId','userIds'].includes(key))):defaults,null,2));
  const [busy,setBusy]=useState(false),[error,setError]=useState<unknown>(),[confirm,setConfirm]=useState(false);
  const [current,setCurrent]=useState(record);
  const [devices,setDevices]=useState(record&&'deviceIds' in record?(record.deviceIds??[]).join(', '):'');
  const heading=useRef<HTMLHeadingElement>(null);
  useEffect(()=>{heading.current?.focus();},[]);
  const pending=useRef<{body:string;key:string}|undefined>(undefined);
  function mutationKey(body:string){if(pending.current?.body!==body)pending.current={body,key:crypto.randomUUID()};return pending.current.key;}
  async function save(event:FormEvent) {event.preventDefault();if(busy)return;setBusy(true);setError(undefined);
    try {
      const extra=kind==='tools'||kind==='policies'||kind==='roles'?JSON.parse(config):{};
      if(!extra||typeof extra!=='object'||Array.isArray(extra))throw new Error('Object required');
      if(kind==='tools') extra.definition={...extra.definition,id,name};
      const body={...(kind==='roles'?{portalRole,rules:{...extra,templateIds:templates}}:extra),id,name,enabled,revision:current?.revision??1,...(kind==='teams'?{userIds:members.split(',').map(v=>v.trim()).filter(Boolean),deviceIds:devices.split(',').map(v=>v.trim()).filter(Boolean),roleIds:roles.split(',').map(v=>v.trim()).filter(Boolean)}:kind==='agents'||kind==='devices'?{ownerUserId:owner}:kind==='policies'?{userIds:members.split(',').map(v=>v.trim()).filter(Boolean)}:{})} as DirectoryRecords[Kind];
      await client.saveRecord(kind,body,!!current,mutationKey(JSON.stringify(body)));saved();
    }catch(failure){setError(failure);}finally{setBusy(false);}
  }
  async function reload(){if(!current)return;setBusy(true);setError(undefined);try{const value=await client.record(kind,current.id);setCurrent(value);setName(value.name);setEnabled(value.enabled);if('ownerUserId' in value)setOwner(value.ownerUserId);if('userIds' in value)setMembers(value.userIds.join(', '));if('roleIds' in value)setRoles((value.roleIds??[]).join(', '));if('rules' in value){setPortalRole(value.portalRole);setTemplates(value.rules.templateIds);}if('deviceIds' in value)setDevices((value.deviceIds??[]).join(', '));setConfig(JSON.stringify('rules' in value?Object.fromEntries(Object.entries(value.rules).filter(([key])=>key!=='templateIds')):Object.fromEntries(Object.entries(value).filter(([key])=>!['id','name','enabled','revision','ownerUserId','userIds'].includes(key))),null,2));}catch(failure){setError(failure);}finally{setBusy(false);}}
  async function remove(){if(!current||busy)return;setBusy(true);setError(undefined);try{await client.deleteRecord(kind,current,mutationKey(`delete:${current.id}:${current.revision}`));saved();}catch(failure){setError(failure);}finally{setBusy(false);}}
  const title=titles[kind];
  return <section className="detail" aria-label={`${title} editor`}><div className="page-heading"><h2 ref={heading} tabIndex={-1}>{current?`Edit ${title}`:`Add ${title}`}</h2><button disabled={busy} onClick={close}>Close editor</button></div>
    {current&&'decision' in current&&<dl className="policy-details"><dt>Who</dt><dd>{[...current.userIds,...current.teamIds,...current.agentIds,...current.deviceIds].join(', ')}</dd><dt>Can use</dt><dd>{current.toolId} · {current.action}</dd><dt>Where</dt><dd><code>{JSON.stringify(current.resource)}</code></dd><dt>Stored decision</dt><dd>{current.decision}</dd></dl>}
    {error!==undefined&&<Failure error={error}/>}
    <form className="user-form" onSubmit={save}>
      <label htmlFor="entity-id">Identifier</label><input id="entity-id" required maxLength={128} disabled={busy||!!current} value={id} onChange={e=>setId(e.target.value)}/>
      <label htmlFor="entity-name">Display name</label><input id="entity-name" required maxLength={128} disabled={busy} value={name} onChange={e=>setName(e.target.value)}/>
      {(kind==='teams'||kind==='policies')&&<><label htmlFor="entity-members">User IDs, separated by commas</label><textarea id="entity-members" value={members} disabled={busy} onChange={e=>setMembers(e.target.value)}/></>}
      {kind==='teams'&&<><label htmlFor="entity-devices">Device IDs, separated by commas</label><textarea id="entity-devices" value={devices} disabled={busy} onChange={e=>setDevices(e.target.value)}/></>}
      {kind==='teams'&&<><label htmlFor="entity-roles">Role IDs, separated by commas</label><input id="entity-roles" value={roles} disabled={busy} onChange={e=>setRoles(e.target.value)}/><p className="hint">Enabled roles apply to enabled members of this team.</p></>}
      {kind==='roles'&&<><label htmlFor="role-portal">Portal classification</label><select id="role-portal" value={portalRole} disabled={busy} onChange={e=>setPortalRole(e.target.value as UserRole)}><option value="BASIC">Basic user</option><option value="ADMINISTRATOR">Administrator</option><option value="SUPER_ADMIN">Super Admin</option></select><fieldset disabled={busy}><legend>Fixed privilege templates</legend>{(['TOOL_USER','IT_CLOUD_ADMIN','APPROVER'] as const).map(template=><label className="checkbox" key={template}><input type="checkbox" checked={templates.includes(template)} onChange={e=>setTemplates(e.target.checked?[...templates,template]:templates.filter(value=>value!==template))}/>{template==='TOOL_USER'?'Tool user':template==='IT_CLOUD_ADMIN'?'IT Cloud Admin': 'Approval reviewer'}</label>)}</fieldset><label htmlFor="role-rules">Permission scope (JSON)</label><textarea id="role-rules" rows={12} maxLength={65536} value={config} disabled={busy} onChange={e=>setConfig(e.target.value)}/><p className="hint">deviceScope is NONE, ALL or GROUPS. GROUPS requires deviceGroupIds. An empty toolIds list uses the tools allowed by policy. Templates are fixed; scope only narrows their permissions. Only Super Admin can manage roles.</p></>}
      {(kind==='agents'||kind==='devices')&&<><label htmlFor="entity-owner">Owner user ID</label><input id="entity-owner" required value={owner} disabled={busy} onChange={e=>setOwner(e.target.value)}/></>}
      {(kind==='tools'||kind==='policies')&&<><label htmlFor="entity-config">{kind==='tools'?'Tool definition':'Policy scope and decision'} (JSON)</label><textarea id="entity-config" rows={12} maxLength={65536} value={config} disabled={busy} onChange={e=>setConfig(e.target.value)}/></>}
      <label className="checkbox"><input type="checkbox" checked={enabled} disabled={busy} onChange={e=>setEnabled(e.target.checked)}/>Enabled in directory</label>
      <p className="hint">New records start disabled. Enabling is not a policy grant; publish the applicable policy bundle before runtime access.</p>
      <div className="actions"><button disabled={busy} className="primary">Save {title}</button>{current&&<button type="button" disabled={busy} onClick={()=>void reload()}>Reload current record</button>}</div>
    </form>
    {current&&<div className="delete-area">{confirm?<><p>Delete this record? Referenced records cannot be deleted, and identifiers cannot be reused.</p><button disabled={busy} onClick={()=>void remove()} className="danger">Confirm delete</button><button disabled={busy} onClick={()=>setConfirm(false)}>Cancel delete</button></>:<button disabled={busy} className="danger" onClick={()=>setConfirm(true)}>Delete {title}</button>}</div>}
  </section>;
}
