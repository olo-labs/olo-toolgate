// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
import {useEffect, useRef, useState} from 'react';
import type {ClientInstallerManifest} from '@olo-labs/toolgate-contracts';

type ExtensionRelease = {protocol:number;version:string;chromeVersion:string;extensionId:string;storeUrl:string;filename:string;sha256:string;bytes:number};
export type ConnectStatus = {protocol:number;version:string;chromeVersion:string;phase:string;userCode?:string;client?:{health?:{ready:boolean};error?:unknown}};

/** Bounded, same-window status bridge. Local presence never grants authorization. */
export function extensionStatus(signal:AbortSignal,operation:'hello'|'connect'|'status'|'cancel'='status'):Promise<ConnectStatus> {
  return new Promise((resolve,reject)=>{
    const id=crypto.randomUUID();
    const cleanup=()=>{clearTimeout(timer);window.removeEventListener('message',receive);signal.removeEventListener('abort',abort);};
    const abort=()=>{cleanup();reject(new DOMException('Cancelled','AbortError'));};
    const receive=(event:MessageEvent)=>{
      if(event.source!==window||event.origin!==location.origin||event.data?.channel!=='toolgate-connect-response'||event.data.id!==id)return;
      cleanup();resolve(event.data as ConnectStatus);
    };
    const timer=setTimeout(()=>{cleanup();reject(Error('Extension unavailable'));},operation==='hello'?2000:25000);
    window.addEventListener('message',receive);signal.addEventListener('abort',abort,{once:true});
    if(signal.aborted){abort();return;}
    window.postMessage({channel:'toolgate-connect-request',id,operation},location.origin);
  });
}
export function Connect({onCode,onCancel}:{onCode:(code:string)=>void;onCancel?:()=>void}) {
  const [message,setMessage]=useState('Connect checks the local client through the Chrome extension, then guides installation if needed.');
  const [busy,setBusy]=useState(false),[release,setRelease]=useState<ExtensionRelease>();
  const [installer,setInstaller]=useState<string>();
  const active=useRef<AbortController|undefined>(undefined);
  useEffect(()=>()=>active.current?.abort(),[]);
  const supported=/Chrome\//.test(navigator.userAgent)&&/Windows/.test(navigator.userAgent)&&!/Edg\/|OPR\//.test(navigator.userAgent);
  async function start() {
    active.current?.abort();const controller=new AbortController();active.current=controller;
    setBusy(true);setRelease(undefined);setInstaller(undefined);setMessage('Checking Chrome Connect release…');
    try {
      if(!supported)throw Error('Currently supported: Chrome on Windows. Use the client downloads below for other browsers and systems.');
      const response=await fetch('/api/public/v1/clients/extension',{credentials:'omit',redirect:'error',cache:'no-store',signal:AbortSignal.any([controller.signal,AbortSignal.timeout(10000)])});
      if(!response.ok)throw Error('Connect extension release is unavailable. Use the client downloads below or contact your administrator.');
      const text=await response.text();if(text.length>65536)throw Error('Invalid extension release.');
      const value=JSON.parse(text) as ExtensionRelease;
      if(value.protocol!==1||value.extensionId!=='emmemldedebhbloibichmmdlbpjakfkf'||typeof value.version!=='string'
        ||!/^\d{1,5}(\.\d{1,5}){3}$/.test(value.chromeVersion)||!/^olo-toolgate-chrome-[0-9A-Za-z._-]+\.zip$/.test(value.filename)
        ||!/^[a-f0-9]{64}$/.test(value.sha256)||!['','https://chromewebstore.google.com/detail/'+value.extensionId].includes(value.storeUrl))throw Error('Invalid extension release.');
      setRelease(value);
      try {
        const setupResponse=await fetch('/api/public/v1/installers',{credentials:'omit',redirect:'error',cache:'no-store',signal:AbortSignal.any([controller.signal,AbortSignal.timeout(10000)])});
        if(setupResponse.ok) {
          const document=await setupResponse.text();
          if(document.length<=65536) {
            const setup=JSON.parse(document) as ClientInstallerManifest;
            const target=/ARM|aarch64/i.test(navigator.userAgent)?'aarch64-pc-windows-msvc':'x86_64-pc-windows-msvc';
            const asset=Array.isArray(setup.artifacts)?setup.artifacts.find(a=>a.platform==='WINDOWS'&&a.target===target):undefined;
            if(setup.version===value.version&&asset&&asset.filename===`olo-toolgate-client-${value.version}-${target}.setup.exe`
              &&/^[a-f0-9]{64}$/.test(asset.sha256)&&Number.isSafeInteger(asset.bytes)&&asset.bytes>0&&asset.bytes<=104857600)
              setInstaller(`olo-toolgate-client-${target}.setup.exe`);
          }
        }
      } catch { /* Missing setup must not turn an extension ZIP into an installer. */ }
      const deadline=Date.now()+600000;let first=true;let previousCode='';
      while(!controller.signal.aborted&&Date.now()<deadline) {
        let status:ConnectStatus|undefined;
        try { status=await extensionStatus(controller.signal,first?'hello':'status'); } catch { if(controller.signal.aborted)break; }
        const openPopup=first;
        first=false;
        if(controller.signal.aborted)break;
        if(!status) setMessage('Install the Chrome extension below, then reload this page and click Connect again.');
        else if(status.protocol!==1||status.chromeVersion!==value.chromeVersion||status.version!==value.version)
          setMessage('Upgrade the Chrome extension below, then reload this page and click Connect again.');
        else {
          if(openPopup)void extensionStatus(controller.signal,'connect').catch(()=>{});
          if(typeof status.userCode==='string'&&/^[A-F0-9]{16}$/.test(status.userCode)&&status.userCode!==previousCode){previousCode=status.userCode;onCode(status.userCode);}
          setMessage(status.client?.health&&!status.client.error
            ? 'Client detected. Use Start enrollment in the extension if needed, then review the code and fingerprint below. Server check-in confirms this device is connected.'
            : status.phase==='open-installer' ? 'Download complete. Open the Chrome extension and click Open installer. Accept the license and Windows permission prompt. Then click Start enrollment.'
            : status.phase==='downloading' ? 'Downloading the verified client installer…'
            : status.phase==='download-failed' ? 'Download failed or was cancelled. Retry in the Chrome extension.'
            : 'Open ToolGate Connect from Chrome Extensions. Approve this site, then download the installer if the client is unavailable.');
        }
        await new Promise<void>(resolve=>{
          const finish=()=>{clearTimeout(timer);controller.signal.removeEventListener('abort',finish);resolve();};
          const timer=setTimeout(finish,3000);controller.signal.addEventListener('abort',finish,{once:true});
        });
      }
      if(!controller.signal.aborted)setMessage('Connect timed out. Retry when the extension and client are ready.');
    } catch(error) { if(!controller.signal.aborted)setMessage(error instanceof Error?error.message:'Connect unavailable. Retry.'); }
    finally {if(active.current===controller)setBusy(false);}
  }
  function cancel(){active.current?.abort();onCancel?.();setBusy(false);setMessage('Connect cancelled. Close any open Windows installer separately.');const controller=new AbortController();void extensionStatus(controller.signal,'cancel').catch(()=>{});}
  return <section aria-labelledby="connect-title"><h2 id="connect-title">Connect this computer</h2>
    <button onClick={()=>void start()}>{busy?'Retry Connect':'Connect'}</button>{busy&&<button onClick={cancel}>Cancel Connect</button>}
    <p role="status">{message}</p>
    {release&&<><p>Chrome extension {release.chromeVersion} · Client {release.version}</p>
      {release.storeUrl?<a href={release.storeUrl} target="_blank" rel="noopener noreferrer">Install or upgrade Chrome extension</a>
        : <>{installer?<a href={`/api/public/v1/clients/${installer}`} download>Download Chrome extension</a>:<p>Windows setup is not published for this client release.</p>}
          <p>The download is a single Windows setup EXE containing the client and Chrome native bridge. Open it, accept the license and approve Windows permission. Chrome extension approval is a separate step.</p>
          <p>A Chrome Web Store listing is not configured yet. After installing the current Windows setup, open chrome://extensions, enable Developer mode and choose Load unpacked. Select the chrome-extension folder under Program Files\OLO\ToolGateSetup. To upgrade, run the updated installer and reload the extension.</p></>}
      <details><summary>Extension SHA-256</summary><code>{release.sha256}</code></details></>}
  </section>;
}
