// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
import {useState} from 'react';
import type {ControlSnapshot} from '@olo-labs/toolgate-contracts';
import {initialConfiguration} from './initialConfiguration';
import {ControlClient} from './api';
import {Failure} from './Failure';

/** Existing installations add missing presets through the independently reviewed import. */
export function StandardPresets({client}:{client:ControlClient}) {
  const [busy,setBusy]=useState(false);const [error,setError]=useState<unknown>();const [notice,setNotice]=useState('');
  async function install(){
    if(busy)return;setBusy(true);setError(undefined);setNotice('');
    try{
      const current=await client.exportConfig();const snapshot={...current};let missing=0;
      for(const name of Object.keys(initialConfiguration) as (keyof typeof initialConfiguration)[]){
        const existing=current[name];const additions=initialConfiguration[name].filter(record=>!existing.some(row=>row.id===record.id));
        missing+=additions.length;Object.assign(snapshot,{[name]:[...existing,...additions]});
      }
      if(!missing){setNotice('All standard presets are already available. Existing memberships and administrator edits are preserved.');return;}
      await client.installPresets(snapshot as ControlSnapshot,crypto.randomUUID());
    }catch(failure){setError(failure);}finally{setBusy(false);}
  }
  return <section className="guidance"><h2>Standard access levels</h2>
    <p>ReadOnly permits reads and inspection. ReadAndWrite adds resource changes. Admin adds administration; sensitive execution requires review. Tool and device group levels both constrain execution bindings. Default groups provide membership without runtime access.</p>
    <p>Fresh installations include these presets. Existing installations can add missing groups, roles, grants, and policies with independent approval. No user, agent, tool, or device is assigned automatically.</p>
    <button disabled={busy} onClick={()=>void install()}>Prepare missing standard presets</button>
    {error!==undefined&&<Failure error={error}/>} {notice&&<p role="status">{notice}</p>}
  </section>;
}
