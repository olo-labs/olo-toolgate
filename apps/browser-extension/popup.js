// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
import {consoleOrigin, installer} from './protocol.js';
const status = document.querySelector('#status');
let origin, downloadId;
const button = id => document.querySelector('#'+id);
const say = value => { status.textContent = value; };
try {
  const [tab] = await chrome.tabs.query({active:true,currentWindow:true});
  const pending=(await chrome.storage.session.get('connectTab')).connectTab;
  origin = consoleOrigin(tab.url ?? (pending?.id===tab.id?pending.url:''));
  document.querySelector('#origin').textContent = origin;
  const state = (await chrome.storage.local.get(origin))[origin];
  downloadId = state?.downloadId;
  if (Number.isInteger(downloadId)) {
    const [item] = await chrome.downloads.search({id:downloadId});
    button('open').disabled = item?.state !== 'complete' || item.byExtensionId !== chrome.runtime.id;
  }
  say('Approve communication with this ToolGate site.');
} catch { button('approve').disabled = true; }
button('approve').onclick = async () => {
  try {
    if (!await chrome.permissions.request({origins:[origin+'/*']})) return say('Site permission denied.');
    await chrome.storage.local.set({[origin]:{approved:true,phase:'approved'}});
    button('install').disabled = false;
    button('enroll').disabled = false;
    try {
      const response = await chrome.runtime.sendNativeMessage('io.ololabs.toolgate.connect',{operation:'health'});
      if (!response.error && response.health) return say('Client found. Start enrollment if needed; the console verifies device liveness.');
    } catch { /* Bootstrap installation is explicitly user approved. */ }
    say('No reachable client. Downloading the verified installer…');
    void downloadInstaller();
  } catch { say('Connection unavailable. Retry from the console.'); }
};
async function downloadInstaller() {
  button('install').disabled = true;
  const controller=new AbortController();let cancellationTimer;
  try {
    const current=(await chrome.storage.local.get(origin))[origin];
    if(!current?.approved)throw Error();
    const operationId=crypto.randomUUID();
    await chrome.storage.local.set({[origin]:{...current,operationId,phase:'verifying'}});
    cancellationTimer=setInterval(async()=>{
      const latest=(await chrome.storage.local.get(origin))[origin];
      if(latest?.operationId!==operationId)controller.abort();
    },500);
    say('Verifying release and downloading… Keep this popup open.');
    const options = {credentials:'omit',redirect:'error',cache:'no-store',signal:AbortSignal.any([controller.signal,AbortSignal.timeout(120000)])};
    const response = await fetch(origin+'/api/public/v1/installers',options);
    if (!response.ok) throw Error();
    const text = await response.text(); if (text.length > 65536) throw Error();
    const artifact = installer(JSON.parse(text),button('target').value);
    const file = await fetch(origin+'/api/public/v1/clients/'+artifact.filename,options);
    if (!file.ok) throw Error();
    const reader = file.body.getReader(); let size=0; const chunks=[];
    try { for (;;) { const {value,done}=await reader.read();if(done)break;size+=value.length;if(size>artifact.bytes)throw Error();chunks.push(value); } }
    finally { await reader.cancel(); }
    if (size !== artifact.bytes) throw Error();
    const blob = new Blob(chunks,{type:'application/octet-stream'});
    const digest = [...new Uint8Array(await crypto.subtle.digest('SHA-256',await blob.arrayBuffer()))].map(x=>x.toString(16).padStart(2,'0')).join('');
    if (digest !== artifact.sha256) throw Error();
    if(controller.signal.aborted||(await chrome.storage.local.get(origin))[origin]?.operationId!==operationId)throw Error();
    const url = URL.createObjectURL(blob);
    downloadId = await chrome.downloads.download({url,filename:artifact.filename,saveAs:false});
    const latest=(await chrome.storage.local.get(origin))[origin];
    if(latest?.operationId!==operationId){await chrome.downloads.cancel(downloadId);URL.revokeObjectURL(url);throw Error();}
    await chrome.storage.local.set({[origin]:{...latest,phase:'downloading',downloadId}});
    say('Verified. Once saved, click Open installer.');
    const timer=setInterval(async()=>{
      const [item]=await chrome.downloads.search({id:downloadId});
      if(item?.state==='complete'){clearInterval(timer);URL.revokeObjectURL(url);button('open').disabled=false;}
      else if(item?.state==='interrupted'){clearInterval(timer);URL.revokeObjectURL(url);say('Download interrupted. Retry.');button('install').disabled=false;}
    },500);
  } catch { say(controller.signal.aborted?'Download cancelled. Approve this site again to retry.':'Release unavailable or checksum verification failed. No installer was launched.');button('install').disabled=false; }
  finally {clearInterval(cancellationTimer);}
}
button('install').onclick = downloadInstaller;
button('open').onclick = () => { chrome.downloads.open(downloadId).catch(()=>say('Open the verified installer from Chrome Downloads and approve Windows prompts.')); };
button('enroll').onclick = async () => {
  try {
    const response = await chrome.runtime.sendNativeMessage('io.ololabs.toolgate.connect',{operation:'enroll'});
    if (response.error || !response.challenge) throw Error();
    const previous=(await chrome.storage.local.get(origin))[origin];
    await chrome.storage.local.set({[origin]:{...previous,userCode:response.challenge.userCode}});
    say('Enrollment code: '+response.challenge.userCode+' — fingerprint: '+response.challenge.keyFingerprint+'. Compare these in your ToolGate console.');
  } catch { say('Enrollment unavailable. Confirm the service is installed and your Windows account is authorized for local IPC.'); }
};
