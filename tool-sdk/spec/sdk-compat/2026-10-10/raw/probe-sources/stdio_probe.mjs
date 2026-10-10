// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
// usage: node stdio_probe.mjs <cmd> [args...]  -- sends 2026-07-28 stateless requests over stdio, prints responses
import { spawn } from 'node:child_process';
const [cmd, ...args] = process.argv.slice(2);
const p = spawn(cmd, args, { stdio: ['pipe', 'pipe', 'inherit'] });
const meta = { 'io.modelcontextprotocol/protocolVersion': '2026-07-28', 'io.modelcontextprotocol/clientCapabilities': {}, 'io.modelcontextprotocol/clientInfo': { name: 'probe', version: '0' } };
const reqs = [
  { jsonrpc: '2.0', id: 1, method: 'server/discover', params: { _meta: meta } },
  { jsonrpc: '2.0', id: 2, method: 'tools/list', params: { _meta: meta } },
  { jsonrpc: '2.0', id: 3, method: 'tools/call', params: { _meta: meta, name: 'echo', arguments: { text: 'hi' } } },
  { jsonrpc: '2.0', id: 4, method: 'acme/echo', params: { _meta: meta, text: 'custom' } },
  { jsonrpc: '2.0', id: 5, method: 'initialize', params: { protocolVersion: '2025-11-25', capabilities: {}, clientInfo: { name: 'p', version: '0' } } },
];
let buf = ''; const got = new Map();
p.stdout.on('data', d => { buf += d; let i; while ((i = buf.indexOf('\n')) >= 0) { const line = buf.slice(0, i); buf = buf.slice(i + 1); if (!line.trim()) continue; try { const m = JSON.parse(line); console.log('<<', JSON.stringify(m).slice(0, 600)); if (m.id) got.set(m.id, m); } catch { console.log('<< (non-json)', line.slice(0, 200)); } } });
for (const r of reqs) { console.log('>>', r.method); p.stdin.write(JSON.stringify(r) + '\n'); await new Promise(r => setTimeout(r, 800)); }
p.kill(); process.exit(0);
