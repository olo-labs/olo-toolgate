# Copyright 2026 OLO Labs
# SPDX-License-Identifier: Apache-2.0
"""Explicit, reviewed provisioning of this isolated Compose example."""
import argparse
import hashlib
import importlib.util
import json
import os
from pathlib import Path
import re
import secrets
import subprocess
import sys
import time
import urllib.request

HERE = Path(__file__).resolve().parent
ROOT = HERE.parents[1]
STATE = HERE / '.state'
spec = importlib.util.spec_from_file_location('debug_configuration', ROOT / 'debug/configure-debug-agent.py')
shared = importlib.util.module_from_spec(spec)
spec.loader.exec_module(shared)
TOOLS = {'hotfolder.list', 'hotfolder.read_text', 'hotfolder.write_text', 'client.read_log_entry'}
COMPOSE = ['docker', 'compose', '-p', 'toolgate-langchain', '-f', str(HERE / 'compose.yaml')]


def load_environment():
    path = HERE / '.env'
    if path.exists():
        for line in path.read_text(encoding='utf-8-sig').splitlines():
            if line.strip() and not line.lstrip().startswith('#'):
                key, value = line.split('=', 1)
                os.environ.setdefault(key.strip(), value.strip().strip('"').strip("'"))


def docker(service, arguments, input=None):
    result = subprocess.run(COMPOSE + ['exec', '-T', service] + arguments,
                            input=input, capture_output=True, text=True, timeout=60, cwd=HERE)
    if result.returncode:
        raise ValueError(f'{service} command failed; inspect docker compose logs (private output withheld)')
    return result.stdout.strip()


def wait_ready():
    origin = 'http://127.0.0.1:' + os.environ.get('TOOLGATE_HTTP_PORT', '18091')
    deadline = time.monotonic() + 150
    while time.monotonic() < deadline:
        try:
            if json.load(urllib.request.urlopen(origin + '/health/ready', timeout=3))['ready']:
                return origin
        except (OSError, ValueError):
            pass
        time.sleep(1)
    raise ValueError('Quickstart is not ready; inspect docker compose logs quickstart')


class Configuration(shared.Configuration):
    def __init__(self, origin, bootstrap):
        self.bootstrap = bootstrap
        super().__init__(origin=origin, credential_directory=STATE)
        if bootstrap:
            for number in (1, 2):
                key = f'reviewer-{number}'
                if key not in self.tokens:
                    self.login(key, os.environ.get(f'TOOLGATE_REVIEWER{number}_USERNAME', key),
                               os.environ.get(f'TOOLGATE_REVIEWER{number}_PASSWORD', ''))

    def login(self, key, username, password, replacement=None):
        if self.bootstrap:
            if username not in {'admin', 'reviewer-1', 'reviewer-2'}:
                raise ValueError('--bootstrap-local supports only this example\'s three installation identities')
            filename = 'bootstrap-password' + ('' if username == 'admin' else '-' + username)
            initial = docker('quickstart', ['/opt/quickstart-python/bin/python', '-c',
                'from pathlib import Path; p=Path("/data/' + filename + '"); print(p.read_text().strip() if p.exists() else "")'])
            path = STATE / 'console-passwords.json'
            saved = json.loads(path.read_text(encoding='utf-8')) if path.exists() else {}
            if initial:
                password = secrets.token_urlsafe(32)
                saved[username] = password
                shared.private_file(path, json.dumps(saved))
                replacement = password
                password = initial
            elif username in saved:
                password = saved[username]
            elif not password:
                raise ValueError('Installation password already changed; provide the existing credential in .env')
        super().login(key, username, password, replacement)


def linux_status():
    return json.loads(docker('linux-device', ['cat', '/run/olo-toolgate/device-status.json']))


def wait_device(config, device):
    deadline = time.monotonic() + 90
    while True:
        endpoint = config.api('/api/control/v1/endpoint/devices/' + device)
        if endpoint.get('state') == 'ACTIVE' and endpoint.get('connectionApproved') is not False \
                and int(time.time()*1000) - endpoint.get('lastSeenUnixMs', 0) < 120000:
            return
        if time.monotonic() >= deadline:
            raise ValueError('Approve and enable the selected device in this example\'s console first')
        time.sleep(2)


def provision(args):
    origin = wait_ready()
    config = Configuration(origin, args.bootstrap_local)
    if config.api('/api/control/v1/admin-session')['role'] != 'SUPER_ADMIN':
        raise ValueError('Initial example provisioning requires a Super Admin API login')
    config.presets()
    if args.target == 'linux':
        status = linux_status()
        device = status['deviceId']
        if args.approve_linux_device and status['state'] == 'PENDING':
            review = config.api('/api/control/v1/endpoint/enrollments/review?code=' + status['userCode'])
            if (review['deviceId'], review['keyFingerprint']) != (device, status['keyFingerprint']):
                raise ValueError('Enrollment fingerprint does not match the Compose device')
            config.api('/api/control/v1/endpoint/enrollments/decision',
                dict(userCode=status['userCode'], keyFingerprint=status['keyFingerprint'], choice='APPROVE',
                     unlimitedConnection=False, connectionExpiresAtUnixMs=int(time.time()*1000)+86400000))
        profiles = json.loads(docker('linux-device',
            ['/usr/local/lib/olo-toolgate/olo-toolgate-client', 'authorization-profiles']))
    else:
        if not args.device_id or not args.profiles:
            raise ValueError('Windows requires --device-id and --profiles from prepare-windows.ps1')
        device = args.device_id
        profiles = json.loads(args.profiles.read_text(encoding='utf-8-sig'))
    if not re.fullmatch(r'device-[a-f0-9]{32}', device):
        raise ValueError('Use an enrolled ToolGate device ID')
    wait_device(config, device)
    selected = [profile for profile in profiles if profile['tool']['id'] in TOOLS]
    if {profile['tool']['id'] for profile in selected} != TOOLS or len(selected) != len(TOOLS):
        raise ValueError('Export the four example profiles from the actual installed client')
    for profile in selected:
        config.upsert('extractors', profile['extractor'])
        config.upsert('tools', profile['tool'])
        config.memberships('tools', profile['tool']['id'], ['ReadAndWriteToolGroup'])
    name = 'langchain-' + args.target
    agent = name + '-agent'
    workload = name + '-workload'
    config.upsert('agents', dict(id=agent, name='LangChain ' + args.target + ' example', enabled=True,
                                revision=1, ownerUserId=config.username))
    config.memberships('agents', agent, ['ReadAndWriteAgentGroup'])
    config.memberships('devices', device, ['ReadAndWriteDeviceGroup'])
    binding_id = 'standard-ReadAndWrite-ReadAndWrite'
    binding = config.api('/api/control/v1/bindings/' + binding_id)
    binding['allowedPackageDigests'] = sorted(set(binding['allowedPackageDigests']) |
                                            {profile['tool']['packageDigest'] for profile in selected})
    config.upsert('bindings', binding)
    expires = int(time.time()*1000) + 86400000
    token = secrets.token_urlsafe(48)
    digest = hashlib.sha256(token.encode()).hexdigest()
    old = next((row for row in config.all('workload-bindings') if row['id'] == workload), None)
    epoch = old['credentialEpoch'] + 1 if old else 1
    config.upsert('workload-bindings', dict(id=workload, name='LangChain example credential', enabled=True,
        revision=1, agentId=agent, mode='SERVICE', issuer='gateway', subject=name, audience='gateway',
        credentialSha256=digest, credentialEpoch=epoch, expiresAtUnixMs=expires))
    credential = dict(tokenSha256=digest, expiresAtUnixMs=expires, context=dict(requestId=name,
        tenantId='quickstart', mode='SERVICE', agentId=agent, workloadBindingId=workload, chain=[],
        credentialEpoch=epoch, credentialSha256=digest, bindingId=binding_id, deviceId=device))
    # Install opaque bearer facts only. No private signing key or server-side verifier is read.
    docker('quickstart', ['/opt/quickstart-python/bin/python', '-c',
        "import sys,json,time,os;os.umask(0o077);sys.path.insert(0,'/opt/quickstart');import supervisor as s;"
        "p=s.DATA/'client-runtime-credentials.json';v=s.strict(p.read_bytes()) if p.exists() else [];c=json.load(sys.stdin);"
        "v=[x for x in v if x['expiresAtUnixMs']>int(time.time()*1000) and x['context'].get('workloadBindingId')!=c['context']['workloadBindingId']];"
        "assert len(v)<32;s.atomic(p,json.dumps(v+[c]))"], json.dumps(credential))
    directory = STATE / 'agents'
    shared.private_file(directory / (args.target + '.token'), token)
    descriptor = dict(deviceId=device, agentId=agent, bindingId=binding_id, expiresAtUnixMs=expires,
                      gateway='https://localhost:' + os.environ.get('TOOLGATE_TLS_PORT', '18451'))
    shared.private_file(directory / (args.target + '.json'), json.dumps(descriptor))
    shared.private_file(directory / 'gateway-ca.crt', docker('quickstart', ['cat', '/data/keys/device-ca.crt']))
    result = subprocess.run(COMPOSE + ['restart', 'quickstart'], capture_output=True, text=True, cwd=HERE)
    if result.returncode:
        raise ValueError('Could not restart this example\'s Quickstart to load its credential')
    wait_ready()
    if args.target == 'linux':
        # Clear retry backoff from the gateway restart; identity/files stay in volumes.
        result = subprocess.run(COMPOSE + ['restart', 'linux-device'], capture_output=True, text=True, cwd=HERE)
        if result.returncode:
            raise ValueError('Could not reconnect this example\'s Linux device')
    wait_device(config, device)
    print(f'Configured {args.target}: {device}. Group changes were independently reviewed. Credentials expire in 24 hours.')
    print('Private runner credentials: ' + str(directory))


def main():
    load_environment()
    parser = argparse.ArgumentParser(description=__doc__)
    sub = parser.add_subparsers(dest='command', required=True)
    setup = sub.add_parser('setup')
    setup.add_argument('--target', choices=['linux', 'windows'], default='linux')
    setup.add_argument('--bootstrap-local', action='store_true', help='Use/change the isolated stack\'s one-time installation passwords; retain independent reviewer identities')
    setup.add_argument('--approve-linux-device', action='store_true', help='Approve only the fingerprint-checked Compose Linux device for 24 hours')
    setup.add_argument('--device-id')
    setup.add_argument('--profiles', type=Path)
    sub.add_parser('status')
    args = parser.parse_args()
    STATE.mkdir(mode=0o700, exist_ok=True)
    if args.command == 'setup':
        provision(args)
    else:
        print(json.dumps(linux_status(), indent=2))


if __name__ == '__main__':
    try:
        main()
    except (OSError, ValueError, KeyError, subprocess.SubprocessError) as failure:
        print('FAILED: ' + (str(failure) if isinstance(failure, ValueError) else
              'Example setup failed; inspect Compose health and configuration'), file=sys.stderr)
        raise SystemExit(1)
