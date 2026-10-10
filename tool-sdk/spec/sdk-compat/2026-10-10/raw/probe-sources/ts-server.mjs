// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
import { McpServer } from '@modelcontextprotocol/server';
import { serveStdio } from '@modelcontextprotocol/server/stdio';
import * as z from 'zod';
serveStdio(() => {
  const s = new McpServer({ name: 'probe-ts', version: '1.0.0' }, { capabilities: { tools: {} } });
  s.registerTool('echo', { description: 'echo', inputSchema: z.object({ text: z.string() }) }, async ({ text }) => ({ content: [{ type: 'text', text }] }));
  s.server.setRequestHandler('acme/echo', { params: z.object({ text: z.string() }).passthrough() }, async (params) => ({ echoed: params.text }));
  return s;
});
