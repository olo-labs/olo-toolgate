// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
import type {EnterpriseScope,GroupSelection,EnterpriseResourceRule} from '@olo-labs/toolgate-contracts';
import {ControlClient} from './api';
import {ScopePicker} from './ScopePicker';
import type {DirectoryKind} from './operations.generated';

export const emptyConditions={notBeforeUnixMs:0,expiresAtUnixMs:0,networkCidrs:[],devicePosture:[],regions:[],hoursUtc:[],requireOnline:true,highRisk:false} as const;
export const emptyScope:EnterpriseScope={toolGroups:{ids:[],all:false},deviceGroups:{ids:[],all:false},actions:[],allActions:false,resources:[],conditions:emptyConditions};

const object=(v:unknown):v is Record<string,unknown>=>Boolean(v)&&typeof v==='object'&&!Array.isArray(v);
const strings=(v:unknown)=>Array.isArray(v)&&v.every(s=>typeof s==='string');
export const groupShape=(v:unknown)=>object(v)&&typeof v.all==='boolean'&&strings(v.ids);
export const conditionsShape=(v:unknown)=>object(v)&&typeof v.notBeforeUnixMs==='number'&&typeof v.expiresAtUnixMs==='number'&&typeof v.requireOnline==='boolean'&&typeof v.highRisk==='boolean'&&['networkCidrs','devicePosture','regions'].every(k=>strings(v[k]))&&Array.isArray(v.hoursUtc)&&v.hoursUtc.every(h=>object(h)&&['dayOfWeek','startMinute','endMinute'].every(k=>typeof h[k]==='number'));
export const scopeShape=(v:unknown)=>object(v)&&groupShape(v.toolGroups)&&groupShape(v.deviceGroups)&&strings(v.actions)&&typeof v.allActions==='boolean'&&conditionsShape(v.conditions)&&Array.isArray(v.resources)&&v.resources.every(r=>object(r)&&['FILE','URL','DATABASE','CUSTOM'].includes(String(r.kind))&&typeof r.locator==='string'&&['ANY','EXACT','PREFIX'].includes(String(r.match)));

export function GroupSelector({client,kind,label,value,change,disabled=false}:{client:ControlClient;kind:DirectoryKind;label:string;value:GroupSelection;change:(value:GroupSelection)=>void;disabled?:boolean}) {
  if(!groupShape(value))return <p role="alert">Complete the group selection in advanced JSON.</p>;
  return <fieldset disabled={disabled}><legend>{label}</legend><label className="checkbox"><input type="checkbox" checked={value.all} onChange={e=>change({all:e.target.checked,ids:[]})}/>Explicitly select all groups</label>
    {!value.all&&<ScopePicker client={client} kind={kind} label={label} value={value.ids} onChange={ids=>change({ids,all:false})} disabled={disabled}/>}
    <p className="hint">{value.all?'Includes future groups. Control requires a grantable wildcard ceiling.':value.ids.length?'Only these groups are selected.':'No groups selected; this scope grants nothing.'}</p></fieldset>;
}

/** Guided and advanced editors persist the same complete canonical scope. */
export function EnterpriseScopeEditor({client,value,change,disabled=false}:{client:ControlClient;value:EnterpriseScope;change:(value:EnterpriseScope)=>void;disabled?:boolean}) {
  if(!scopeShape(value))return <p role="alert">Complete the permission scope in advanced JSON.</p>;
  const update=(field:keyof EnterpriseScope,next:unknown)=>change({...value,[field]:next});
  const resource=(index:number,next:EnterpriseResourceRule)=>update('resources',value.resources.map((v,i)=>i===index?next:v));
  return <fieldset disabled={disabled} className="enterprise-scope"><legend>Complete permission scope</legend>
    <GroupSelector client={client} kind="toolGroups" label="Tool groups" value={value.toolGroups} change={v=>update('toolGroups',v)} disabled={disabled}/>
    <GroupSelector client={client} kind="deviceGroups" label="Device groups" value={value.deviceGroups} change={v=>update('deviceGroups',v)} disabled={disabled}/>
    <label className="checkbox"><input type="checkbox" checked={value.allActions} onChange={e=>change({...value,allActions:e.target.checked,actions:[]})}/>Explicitly allow all actions</label>
    {!value.allActions&&<label>Actions, separated by commas<input value={value.actions.join(', ')} onChange={e=>update('actions',[...new Set(e.target.value.split(',').map(v=>v.trim()).filter(Boolean))])}/></label>}
    <p className="hint">An empty action or resource selection grants nothing. Tool and device scopes remain one tuple.</p>
    <fieldset><legend>Resources</legend>{value.resources.map((r,index)=><div className="scope-resource" key={index}>
      <label>Resource kind<select value={r.kind} onChange={e=>resource(index,{...r,kind:e.target.value as EnterpriseResourceRule['kind']})}>{['FILE','URL','DATABASE','DEVICE','CUSTOM'].map(k=><option key={k}>{k}</option>)}</select></label>
      <label>Match<select value={r.match} onChange={e=>resource(index,{...r,match:e.target.value as EnterpriseResourceRule['match'],locator:e.target.value==='ANY'?'':r.locator})}><option value="EXACT">Exact resource</option><option value="PREFIX">Within path prefix</option><option value="ANY">Explicitly any resource of this kind</option></select></label>
      {r.match!=='ANY'&&<label>Resource locator<input required maxLength={4096} value={r.locator} onChange={e=>resource(index,{...r,locator:e.target.value})}/></label>}
      <button type="button" onClick={()=>update('resources',value.resources.filter((_,i)=>i!==index))}>Remove resource {index+1}</button></div>)}
      <button type="button" disabled={disabled||value.resources.length>=64} onClick={()=>update('resources',[...value.resources,{kind:'FILE',match:'EXACT',locator:''}])}>Add resource rule</button></fieldset>
    <fieldset><legend>Conditions</legend><label className="checkbox"><input type="checkbox" checked={value.conditions.requireOnline} onChange={e=>update('conditions',{...value.conditions,requireOnline:e.target.checked})}/>Require current online authority</label>
      <label className="checkbox"><input type="checkbox" checked={value.conditions.highRisk} onChange={e=>update('conditions',{...value.conditions,highRisk:e.target.checked})}/>High-risk protected effects</label>
      <label>Valid from (Unix milliseconds; 0 is unbounded)<input type="number" min={0} step={1} value={value.conditions.notBeforeUnixMs} onChange={e=>update('conditions',{...value.conditions,notBeforeUnixMs:Number(e.target.value)})}/></label>
      <label>Expires at (Unix milliseconds; 0 is unbounded)<input type="number" min={0} step={1} value={value.conditions.expiresAtUnixMs} onChange={e=>update('conditions',{...value.conditions,expiresAtUnixMs:Number(e.target.value)})}/></label>
      <label>Trusted network CIDRs<input value={value.conditions.networkCidrs.join(', ')} onChange={e=>update('conditions',{...value.conditions,networkCidrs:e.target.value.split(',').map(v=>v.trim()).filter(Boolean)})}/></label>
      <label>Required device posture<input value={value.conditions.devicePosture.join(', ')} onChange={e=>update('conditions',{...value.conditions,devicePosture:e.target.value.split(',').map(v=>v.trim()).filter(Boolean)})}/></label>
      <label>Allowed regions<input value={value.conditions.regions.join(', ')} onChange={e=>update('conditions',{...value.conditions,regions:e.target.value.split(',').map(v=>v.trim()).filter(Boolean)})}/></label>
      <label>Maximum amount in minor currency units<input type="number" min={0} value={value.conditions.maxAmountMinorUnits??''} onChange={e=>{const next={...value.conditions};if(e.target.value==='')delete next.maxAmountMinorUnits;else next.maxAmountMinorUnits=Number(e.target.value);update('conditions',next);}}/></label>
      <label>Maximum invocations per minute<input type="number" min={1} value={value.conditions.maxInvocationsPerMinute??''} onChange={e=>{const next={...value.conditions};if(e.target.value==='')delete next.maxInvocationsPerMinute;else next.maxInvocationsPerMinute=Number(e.target.value);update('conditions',next);}}/></label>
      {value.conditions.hoursUtc.map((hours,index)=><div key={index}><label>UTC day (Sunday is 0)<input type="number" min={0} max={6} value={hours.dayOfWeek} onChange={e=>update('conditions',{...value.conditions,hoursUtc:value.conditions.hoursUtc.map((h,i)=>i===index?{...h,dayOfWeek:Number(e.target.value)}:h)})}/></label><label>Start minute UTC<input type="number" min={0} max={1439} value={hours.startMinute} onChange={e=>update('conditions',{...value.conditions,hoursUtc:value.conditions.hoursUtc.map((h,i)=>i===index?{...h,startMinute:Number(e.target.value)}:h)})}/></label><label>End minute UTC<input type="number" min={1} max={1440} value={hours.endMinute} onChange={e=>update('conditions',{...value.conditions,hoursUtc:value.conditions.hoursUtc.map((h,i)=>i===index?{...h,endMinute:Number(e.target.value)}:h)})}/></label><button type="button" onClick={()=>update('conditions',{...value.conditions,hoursUtc:value.conditions.hoursUtc.filter((_,i)=>i!==index)})}>Remove UTC interval {index+1}</button></div>)}
      <button type="button" onClick={()=>update('conditions',{...value.conditions,hoursUtc:[...value.conditions.hoursUtc,{dayOfWeek:1,startMinute:540,endMinute:1020}]})}>Add UTC hours</button>
      <p className="hint">Missing required trusted evidence denies access.</p></fieldset>
  </fieldset>;
}
