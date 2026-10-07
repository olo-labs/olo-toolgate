// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
import {consoleOrigin} from './protocol.js';
try {
  const [tab] = await chrome.tabs.query({active:true,currentWindow:true});
  const origin = consoleOrigin(tab.url);
  document.querySelector('#origin').textContent = origin;
  const state = (await chrome.storage.local.get(origin))[origin];
  document.querySelector('#status').textContent = state?.phase === 'connecting'
    ? 'Installing or updating the gateway. Approve the Windows permission prompt if shown.'
    : state?.phase === 'ready' ? 'Client available. The console verifies enrollment and connection.'
    : 'Click Connect on your ToolGate page. Setup and gateway configuration run automatically.';
} catch { /* Open a ToolGate console to connect. */ }
