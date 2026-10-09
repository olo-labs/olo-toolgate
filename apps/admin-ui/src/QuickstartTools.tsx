// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
import { useEffect, useRef, useState, type FormEvent } from 'react';
import type { BuiltinToolInfo,EnterpriseHumanOutcome } from '@olo-labs/toolgate-contracts';
import { ApiError, ControlClient } from './api';
import { Failure } from './Failure';

/** Presentation only: the fixed executor and Gateway validate every call. */
export function QuickstartTools({client}:{client:ControlClient}) {
  const [tools,setTools]=useState<BuiltinToolInfo[]>([]); const [tool,setTool]=useState('calculator.evaluate');
  const [arguments_,setArguments]=useState('{"expression":"2 + 3 * 4"}');
  const [result,setResult]=useState<unknown>(); const [error,setError]=useState<unknown>(); const [busy,setBusy]=useState(false);
  const [name,setName]=useState(''); const [secret,setSecret]=useState(''); const [names,setNames]=useState<string[]>([]);
  const retry=useRef<{body:string,key:string}|undefined>(undefined);const [invocationId,setInvocationId]=useState<string>();
  useEffect(()=>{let alive=true;client.quickstart<{tools:BuiltinToolInfo[]}>('tools').then(catalog=>{if(alive){setTools(catalog.tools);if(catalog.tools.length)setTool(catalog.tools[0].toolId);}}).catch(failure=>{if(alive)setError(failure);});client.quickstart<{names:string[]}>('vault').then(vault=>{if(alive)setNames(vault.names);}).catch(failure=>{if(alive)setError(failure);});return()=>{alive=false;};},[client]);
  async function invoke(event:FormEvent){event.preventDefault();if(busy)return;setBusy(true);setError(undefined);setResult(undefined);
    try{const argumentsValue:unknown=JSON.parse(arguments_);if(!argumentsValue||typeof argumentsValue!=='object'||Array.isArray(argumentsValue))throw new ApiError(400,'INVALID_REQUEST');const body={toolId:tool,arguments:argumentsValue};const exact=JSON.stringify(body);if(retry.current?.body!==exact)retry.current={body:exact,key:crypto.randomUUID()};const outcome=await client.quickstart<EnterpriseHumanOutcome>('invoke',body,retry.current.key);setInvocationId(outcome.invocation.id);setResult(outcome);}
    catch(failure){setError(failure instanceof SyntaxError?new ApiError(400,'INVALID_JSON'):failure);}finally{setBusy(false);}}
  async function store(event:FormEvent){event.preventDefault();if(busy)return;setBusy(true);setError(undefined);const value=secret;setSecret('');
    try{await client.quickstart('vault',{name,value});setNames((await client.quickstart<{names:string[]}>('vault')).names);setResult({stored:true});}catch(failure){setError(failure);}finally{setBusy(false);}}
  async function refresh(){if(!invocationId||busy)return;setBusy(true);setError(undefined);try{setResult(await client.humanToolResult(invocationId));}catch(failure){setError(failure);}finally{setBusy(false);}}
  return <><p className="eyebrow">Quickstart · Non-HA</p><h1>Try a protected local tool</h1><p>Enroll and approve the executor device, enable the reviewed tool definitions, and configure complete Team grants for the Tool and Device Groups. A fresh installation has no runtime grants.</p>
    {Boolean(error)&&<Failure error={error}/>}<form className="user-form" onSubmit={invoke}><label htmlFor="local-tool">Built-in tool</label><select id="local-tool" value={tool} onChange={e=>setTool(e.target.value)} disabled={busy}>{tools.filter(item=>item.enabled).map(item=><option key={item.toolId} value={item.toolId}>{item.toolId}</option>)}</select>
    <label htmlFor="local-args">Arguments (JSON object)</label><textarea id="local-args" rows={5} value={arguments_} maxLength={60000} onChange={e=>setArguments(e.target.value)} disabled={busy}/><button className="primary" disabled={busy||!tools.length}>Run protected tool</button></form>
    {result!==undefined&&<section aria-label="Tool result"><h2>Result</h2><pre>{JSON.stringify(result,null,2)}</pre></section>}
    {invocationId&&<><button disabled={busy} onClick={()=>void refresh()}>Refresh operation result</button><button disabled={busy} onClick={()=>{retry.current=undefined;setInvocationId(undefined);setResult(undefined);}}>Start a new operation</button></>}
    <p>For an ASK result, an independent reviewer uses <a className="inline-link" href="#approvals">Approvals</a>. After approval, submit the same operation again. Its retained request key prevents duplicate execution.</p>
    <section><h2>Encrypted local vault</h2><p>Secrets stay encrypted in /data. This console lists names only; values are never exported. Custom tool credential binding remains unavailable.</p><p>Stored references: {names.join(', ')||'None'}</p>
    <form className="user-form" onSubmit={store}><label htmlFor="vault-name">Secret name</label><input id="vault-name" required maxLength={128} value={name} onChange={e=>setName(e.target.value)} disabled={busy}/><label htmlFor="vault-secret">Secret value</label><input id="vault-secret" type="password" autoComplete="off" required maxLength={32768} value={secret} onChange={e=>setSecret(e.target.value)} disabled={busy}/><button disabled={busy}>Store encrypted secret</button></form></section></>;
}
