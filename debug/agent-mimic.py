# Copyright 2026 OLO Labs
# SPDX-License-Identifier: Apache-2.0
"""Discover device-scoped MCP tools, write the client file, then read one client log entry."""
import argparse
import json
import os
from pathlib import Path
import secrets
import ssl
import sys
import urllib.error
import urllib.parse
import urllib.request

ROOT = Path(__file__).resolve().parents[1]
VERSION = '2026-07-28'
TEXT = 'My Name is Rahul Nigam'

class NoRedirect(urllib.request.HTTPRedirectHandler):
    def redirect_request(self, *args, **kwargs):
        return None

class Agent:
    def __init__(self, gateway, token_file, ca_file=None):
        origin = urllib.parse.urlsplit(gateway)
        if origin.scheme != 'https' or not origin.hostname or origin.username or origin.password or origin.query or origin.fragment or origin.path not in ('', '/'):
            raise ValueError('Gateway must be an HTTPS origin, for example https://localhost:18450')
        token = Path(token_file).read_text(encoding='utf-8').strip()
        if not 32 <= len(token) <= 256 or any(c not in 'abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789-_.~' for c in token):
            raise ValueError('Invalid agent token file')
        self.token = token
        self.url = gateway.rstrip('/') + '/mcp'
        context = ssl.create_default_context(cafile=str(ca_file) if ca_file else None)
        self.opener = urllib.request.build_opener(urllib.request.ProxyHandler({}), NoRedirect(), urllib.request.HTTPSHandler(context=context))
        self.sequence = 0
        self.run_id = secrets.token_hex(12)

    def rpc(self, method, parameters=None):
        self.sequence += 1
        rpc_id = f'mimic-{self.run_id}-{self.sequence}'
        params = dict(parameters or {})
        params['_meta'] = {'io.modelcontextprotocol/protocolVersion': VERSION, 'io.modelcontextprotocol/clientCapabilities': {'tools': {}}}
        body = {'jsonrpc': '2.0', 'id': rpc_id, 'method': method, 'params': params}
        headers = {'Authorization': 'Bearer ' + self.token, 'Content-Type': 'application/json', 'Accept': 'application/json, text/event-stream', 'MCP-Protocol-Version': VERSION, 'MCP-Method': method, 'X-Request-ID': secrets.token_hex(16)}
        if method == 'tools/call': headers['MCP-Name'] = params['name']
        print(f'SEND {method} id={rpc_id}' + (f' tool={params["name"]}' if method == 'tools/call' else ''), flush=True)
        request = urllib.request.Request(self.url, json.dumps(body).encode(), headers, method='POST')
        try:
            with self.opener.open(request, timeout=35) as response:
                raw = response.read(131073)
                server_id = response.headers.get('X-Request-ID', '')
        except urllib.error.HTTPError as response:
            # Print only the fixed code, never raw server/error bodies or headers.
            try: code = json.loads(response.read(131073)).get('code', 'MCP_REJECTED')
            except (ValueError, AttributeError): code = 'MCP_REJECTED'
            raise ValueError(f'MCP HTTP {response.code}: {code}; check enrollment, agent binding and permissions') from None
        if len(raw) > 131072: raise ValueError('MCP response exceeds the limit')
        reply = json.loads(raw)
        print(f'RECEIVE {method} id={rpc_id} serverRequestId={server_id}', flush=True)
        if not isinstance(reply, dict) or reply.get('jsonrpc') != '2.0' or reply.get('id') != rpc_id:
            raise ValueError('MCP response identity mismatch')
        if 'error' in reply:
            data = reply['error'].get('data') or {}
            raise ValueError(f'MCP error {reply["error"].get("code")}: {data.get("code", "REQUEST_REJECTED")}')
        result = reply.get('result')
        if not isinstance(result, dict) or result.get('isError'): raise ValueError('MCP tool failed')
        return result

def mimic(agent, file_path):
    discovery = agent.rpc('server/discover')
    if VERSION not in discovery.get('supportedVersions', []): raise ValueError('Gateway MCP version mismatch')
    catalog = agent.rpc('tools/list')
    tools = catalog.get('tools', [])
    names = [tool['name'] for tool in tools]
    print('Available tools: ' + (', '.join(names) if names else '(none)'), flush=True)
    for name in ('hotfolder.write_text', 'client.read_log_entry'):
        if names.count(name) != 1:
            raise ValueError(f'{name} unavailable: enroll the client, wait for its poll, and allow the tool for this agent/device')
    write = agent.rpc('tools/call', {'name': 'hotfolder.write_text', 'arguments': {'path': file_path, 'text': TEXT}})
    if write.get('structuredContent', {}).get('success') is not True: raise ValueError('Client did not confirm file creation')
    print(f'Created on client: {file_path}\n{TEXT}', flush=True)
    log = agent.rpc('tools/call', {'name': 'client.read_log_entry', 'arguments': {}})
    content = log.get('structuredContent', {})
    if content.get('file') != 'packets.jsonl' or not isinstance(content.get('entry'), dict): raise ValueError('Client did not return one diagnostic log entry')
    print('One entry from client packets.jsonl:', flush=True)
    print(json.dumps(content['entry'], ensure_ascii=True, indent=2), flush=True)
    return content['entry']

def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--gateway', default='https://localhost:18450')
    parser.add_argument('--token-file', type=Path, default=Path(os.environ.get('TOOLGATE_AGENT_TOKEN_PATH', ROOT / '.dev/debug/client-agent-token')))
    parser.add_argument('--ca-file', type=Path)
    parser.add_argument('--file', default='rahul-nigam.txt')
    args = parser.parse_args()
    ca = args.ca_file
    local_ca = ROOT / '.dev/debug/toolgate-quickstart-ca.crt'
    if ca is None and args.gateway.rstrip('/') == 'https://localhost:18450' and local_ca.is_file(): ca = local_ca
    try: mimic(Agent(args.gateway, args.token_file, ca), args.file)
    except (OSError, ValueError, KeyError, TypeError) as failure:
        # Exceptions from OS/TLS can include private filenames; keep those generic.
        print('FAILED: ' + (str(failure) if isinstance(failure, ValueError) else 'Cannot connect or load the agent credential/CA; check configuration and Gateway health'), file=sys.stderr)
        return 1
    return 0

if __name__ == '__main__': sys.exit(main())
