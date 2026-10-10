// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
// usage: node stdio_probe_legacy.mjs <cmd> [args...] -- 2025-11-25 handshake over stdio
import { spawn } from 'node:child_process';
const [cmd, ...args] = process.argv.slice(2);
const p = spawn(cmd, args, { stdio: ['pipe', 'pipe', 'inherit'] });
const reqs = [
  { jsonrpc: '2.0', id: 1, method: 'initialize', params: { protocolVersion: '2025-11-25', capabilities: {}, clientInfo: { name: 'p', version: '0' } } },
  { jsonrpc: '2.0', method: 'notifications/initialized' },
  { jsonrpc: '2.0', id: 2, method: 'tools/list', params: {} },
  { jsonrpc: '2.0', id: 3, method: 'acme/echo', params: { text: 'custom-legacy' } },
];
let buf = '';
p.stdout.on('data', d => { buf += d; let i; while ((i = buf.indexOf('\n')) >= 0) { const line = buf.slice(0, i); buf = buf.slice(i + 1); if (line.trim()) console.log('<<', line.slice(0, 500)); } });
for (const r of reqs) { console.log('>>', r.method); p.stdin.write(JSON.stringify(r) + '\n'); await new Promise(r => setTimeout(r, 800)); }
p.kill(); process.exit(0);
