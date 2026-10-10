// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
import {useEffect,useRef,useState} from 'react';
import type {ControlServerSettings} from '@olo-labs/toolgate-contracts';
import {ControlClient} from './api';
import {Failure} from './Failure';

export const DEFAULT_AUTO_APPROVE_DAYS=30;
type Draft={autoApproveDevices:boolean;autoApproveDurationDays:string;autoApproveOwnerUserId:string};
const draftOf=(settings:ControlServerSettings):Draft=>({autoApproveDevices:settings.autoApproveDevices,autoApproveDurationDays:String(settings.autoApproveDurationDays),autoApproveOwnerUserId:settings.autoApproveOwnerUserId??''});
const days=(value:string)=>/^[0-9]{1,4}$/.test(value)&&Number(value)>=1&&Number(value)<=3650?Number(value):undefined;
const identifier=(value:string)=>/^[a-zA-Z0-9][a-zA-Z0-9._:-]{0,127}$/.test(value);

/** Parses an exported settings file; the revision in the file is ignored on import. */
export function importedSettings(value:unknown):Draft{
  if(!value||typeof value!=='object')throw new Error('Invalid settings');
  const settings=value as Partial<ControlServerSettings>;
  if(settings.formatVersion!==1||typeof settings.autoApproveDevices!=='boolean'||!Number.isSafeInteger(settings.autoApproveDurationDays)||days(String(settings.autoApproveDurationDays))===undefined
    ||(settings.autoApproveOwnerUserId!==undefined&&(typeof settings.autoApproveOwnerUserId!=='string'||!identifier(settings.autoApproveOwnerUserId))))throw new Error('Invalid settings');
  return draftOf(settings as ControlServerSettings);
}

/** Server settings, including bounded device auto-approval, with settings-file import and export. */
export function ServerSettings({client}:{client:ControlClient}){
  const [saved,setSaved]=useState<ControlServerSettings>();const [draft,setDraft]=useState<Draft>();
  const [busy,setBusy]=useState(false);const [error,setError]=useState<unknown>();const [notice,setNotice]=useState('');const [attempt,setAttempt]=useState(0);
  const locked=useRef(false);const pending=useRef<{body:string;key:string}|undefined>(undefined);
  useEffect(()=>{const abort=new AbortController();setError(undefined);
    client.serverSettings(abort.signal).then(settings=>{if(!abort.signal.aborted){setSaved(settings);setDraft(draftOf(settings));}},failure=>{if(!abort.signal.aborted)setError(failure);});
    return()=>abort.abort();},[client,attempt]);
  const duration=draft&&days(draft.autoApproveDurationDays);
  const owner=draft?.autoApproveOwnerUserId.trim()??'';
  const valid=Boolean(draft)&&duration!==undefined&&(owner===''?!draft?.autoApproveDevices:identifier(owner));
  const changed=Boolean(saved&&draft)&&JSON.stringify(draftOf(saved!))!==JSON.stringify({...draft!,autoApproveOwnerUserId:owner});
  const update=(change:Partial<Draft>)=>{setDraft(current=>current&&{...current,...change});setNotice('');};
  async function save(){if(!saved||!draft||!valid||locked.current)return;
    const body:ControlServerSettings={formatVersion:1,revision:saved.revision,autoApproveDevices:draft.autoApproveDevices,autoApproveDurationDays:duration!,...(owner?{autoApproveOwnerUserId:owner}:{})};
    const encoded=JSON.stringify(body);if(pending.current?.body!==encoded)pending.current={body:encoded,key:crypto.randomUUID()};
    locked.current=true;setBusy(true);setError(undefined);setNotice('');
    try{const result=await client.saveServerSettings(body,saved.revision,pending.current.key);pending.current=undefined;setSaved(result);setDraft(draftOf(result));setNotice('Server settings saved.');}
    catch(failure){setError(failure);}finally{locked.current=false;setBusy(false);}}
  function exportSettings(){if(!saved)return;
    const url=URL.createObjectURL(new Blob([JSON.stringify(saved,null,2)+'\n'],{type:'application/json'}));
    const link=document.createElement('a');link.href=url;link.download=`toolgate-server-settings-r${saved.revision}.json`;link.click();setTimeout(()=>URL.revokeObjectURL(url),0);
    setNotice('Server settings exported. Mount the file as server-settings.json in the configuration import folder to apply it during bring-up.');}
  async function importSettings(file:File|undefined){if(!file)return;setError(undefined);
    if(file.size>16384){setNotice('Choose a settings file no larger than 16 KiB.');return;}
    try{setDraft(importedSettings(JSON.parse(await file.text()) as unknown));setNotice('Imported settings loaded. Review them, then save.');}
    catch{setNotice('The file is not an exported ToolGate server settings document.');}}
  return <section className="guidance" aria-label="Server settings"><h2>Server settings</h2>
    {!draft&&!error&&<p role="status">Loading server settings…</p>}
    {draft&&<fieldset disabled={busy}><legend>Device approval</legend>
      <label className="checkbox"><input type="checkbox" checked={draft.autoApproveDevices} onChange={event=>update({autoApproveDevices:event.target.checked})}/> Auto approve devices</label>
      <p className="hint">New devices that start enrollment are approved immediately, without an administrator decision, for the duration below. Existing devices are not changed. Leave this off unless every device that can reach this server should get access.</p>
      <label htmlFor="auto-approve-duration">Auto approve duration (days)</label>
      <input id="auto-approve-duration" type="number" inputMode="numeric" min={1} max={3650} required value={draft.autoApproveDurationDays} onChange={event=>update({autoApproveDurationDays:event.target.value})}/>
      {duration===undefined&&<p role="alert">Enter between 1 and 3650 days. Default: {DEFAULT_AUTO_APPROVE_DAYS} days.</p>}
      <label htmlFor="auto-approve-owner">Owner user ID for auto-approved devices</label>
      <input id="auto-approve-owner" value={draft.autoApproveOwnerUserId} maxLength={128} onChange={event=>update({autoApproveOwnerUserId:event.target.value})} aria-describedby="auto-approve-owner-help"/>
      <p id="auto-approve-owner-help" className="hint">Required while auto-approval is on. The user must exist and be enabled; devices fall back to manual approval if it is later disabled.</p>
      {draft.autoApproveDevices&&!owner&&<p role="alert">Enter the owner user ID to turn on auto-approval.</p>}
      <button className="primary" disabled={!valid||!changed} onClick={()=>void save()}>Save server settings</button>
    </fieldset>}
    {saved&&<><h3>Import and export settings</h3>
      <button disabled={busy} onClick={exportSettings}>Export server settings</button>
      <label>Server settings JSON file<input type="file" accept=".json,application/json" disabled={busy} onChange={event=>{void importSettings(event.target.files?.[0]);event.target.value='';}}/></label>
      <p className="hint">At bring-up, the server also imports server-settings.json from the folder in TOOLGATE_CONFIG_IMPORT_DIRECTORY, then applies TOOLGATE_SETTINGS_AUTO_APPROVE_DEVICES, TOOLGATE_SETTINGS_AUTO_APPROVE_DURATION_DAYS and TOOLGATE_SETTINGS_AUTO_APPROVE_OWNER. Startup import only seeds settings once unless TOOLGATE_CONFIG_IMPORT_OVERWRITE is true.</p></>}
    {notice&&<p role="status">{notice}</p>}{error!==undefined&&<><Failure error={error}/><button disabled={busy} onClick={()=>setAttempt(value=>value+1)}>Reload server settings</button></>}
  </section>;
}
