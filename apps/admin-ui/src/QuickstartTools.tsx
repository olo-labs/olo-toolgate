// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
import { useEffect, useState, type FormEvent } from 'react';
import type { BuiltinToolInfo } from '@olo-labs/toolgate-contracts';
import { ApiError, ControlClient } from './api';
import { Failure } from './Failure';

/** Presentation only: the fixed executor and Gateway validate every call. */
export function QuickstartTools({client}:{client:ControlClient}) {
  const [tools,setTools]=useState<BuiltinToolInfo[]>([]); const [tool,setTool]=useState('calculator.evaluate');
  const [arguments_,setArguments]=useState('{"expression":"2 + 3 * 4"}');
  const [result,setResult]=useState<unknown>(); const [error,setError]=useState<unknown>(); const [busy,setBusy]=useState(false);
  const [name,setName]=useState(''); const [secret,setSecret]=useState(''); const [names,setNames]=useState<string[]>([]);
  useEffect(()=>{let alive=true; Promise.all([client.quickstart<{tools:BuiltinToolInfo[]}>('tools'),client.quickstart<{names:string[]}>('vault')]).then(([catalog,vault])=>{if(alive){setTools(catalog.tools);setNames(vault.names);}}).catch(failure=>{if(alive)setError(failure);});return()=>{alive=false;};},[client]);
  async function invoke(event:FormEvent){event.preventDefault();if(busy)return;setBusy(true);setError(undefined);setResult(undefined);
    try{const argumentsValue:unknown=JSON.parse(arguments_);if(!argumentsValue||typeof argumentsValue!=='object'||Array.isArray(argumentsValue))throw new ApiError(400,'INVALID_REQUEST');setResult(await client.quickstart('invoke',{toolId:tool,arguments:argumentsValue}));}
    catch(failure){setError(failure instanceof SyntaxError?new ApiError(400,'INVALID_JSON'):failure);}finally{setBusy(false);}}
  async function store(event:FormEvent){event.preventDefault();if(busy)return;setBusy(true);setError(undefined);const value=secret;setSecret('');
    try{await client.quickstart('vault',{name,value});setNames((await client.quickstart<{names:string[]}>('vault')).names);setResult({stored:true});}catch(failure){setError(failure);}finally{setBusy(false);}}
  return <><p className="eyebrow">Quickstart · Non-HA</p><h1>Try a protected local tool</h1><p>Compute and welcome.txt reads are enabled. File writes require human approval. Other paths and deletion are blocked.</p>
    {Boolean(error)&&<Failure error={error}/>}<form className="user-form" onSubmit={invoke}><label htmlFor="local-tool">Built-in tool</label><select id="local-tool" value={tool} onChange={e=>setTool(e.target.value)} disabled={busy}>{tools.filter(item=>item.enabled).map(item=><option key={item.toolId} value={item.toolId}>{item.toolId}</option>)}</select>
    <label htmlFor="local-args">Arguments (JSON object)</label><textarea id="local-args" rows={5} value={arguments_} maxLength={60000} onChange={e=>setArguments(e.target.value)} disabled={busy}/><button className="primary" disabled={busy||!tools.length}>Run protected tool</button></form>
    {result!==undefined&&<section aria-label="Tool result"><h2>Result</h2><pre>{JSON.stringify(result,null,2)}</pre></section>}
    <p>For an ASK result, open <a className="inline-link" href="#approvals">Approvals</a>, approve the exact operation, then run the same arguments again.</p>
    <section><h2>Encrypted local vault</h2><p>Secrets stay encrypted in /data. This console lists names only; values are never exported. Custom tool credential binding remains unavailable.</p><p>Stored references: {names.join(', ')||'None'}</p>
    <form className="user-form" onSubmit={store}><label htmlFor="vault-name">Secret name</label><input id="vault-name" required maxLength={128} value={name} onChange={e=>setName(e.target.value)} disabled={busy}/><label htmlFor="vault-secret">Secret value</label><input id="vault-secret" type="password" autoComplete="off" required maxLength={32768} value={secret} onChange={e=>setSecret(e.target.value)} disabled={busy}/><button disabled={busy}>Store encrypted secret</button></form></section></>;
}
