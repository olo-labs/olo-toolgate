// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
import { consoleOrigin, protocol } from './protocol.js';
chrome.runtime.onMessage.addListener((message, sender, reply) => {
  (async () => {
    let origin;
    try { origin = consoleOrigin(sender.tab?.url ?? ''); } catch { return {error:'INVALID_ORIGIN'}; }
    const result = {protocol,version:chrome.runtime.getManifest().version_name,chromeVersion:chrome.runtime.getManifest().version,client:null};
    if(message.operation==='hello')return {...result,phase:'approval'};
    if(message.operation==='connect') {
      await chrome.storage.session.set({connectTab:{id:sender.tab.id,url:sender.tab.url}});
      try {await chrome.action.openPopup();} catch { /* Chrome can require opening the toolbar popup manually. */ }
      return {...result,phase:'approval'};
    }
    const stored = await chrome.storage.local.get(origin);
    const state = stored[origin];
    if (!state?.approved) return {...result,phase:'approval'};
    if (message.operation === 'cancel') {
      if (Number.isInteger(state.downloadId)) {
        const [download] = await chrome.downloads.search({id:state.downloadId});
        if (download?.byExtensionId === chrome.runtime.id && download.state === 'in_progress') await chrome.downloads.cancel(download.id);
      }
      await chrome.storage.local.remove(origin);
      return {...result,phase:'cancelled'};
    }
    if (message.operation !== 'status') return {error:'INVALID_OPERATION'};
    // Chrome terminates one-shot host after its response. Host itself enforces a 20s IPC deadline.
    try {
      result.client = await chrome.runtime.sendNativeMessage('io.ololabs.toolgate.connect',{operation:'health'});
    } catch { /* Missing host, denied OS peer or stopped client remains unavailable. */ }
    let phase = state.phase ?? 'approved';
    if (Number.isInteger(state.downloadId)) {
      const [download] = await chrome.downloads.search({id:state.downloadId});
      if (download?.byExtensionId === chrome.runtime.id) phase = download.state === 'complete' ? 'open-installer' : download.state === 'interrupted' ? 'download-failed' : 'downloading';
    }
    return {...result,phase,userCode:state.userCode};
  })().then(reply).catch(() => reply({error:'UNAVAILABLE'}));
  return true;
});
