# Copyright 2026 OLO Labs
# SPDX-License-Identifier: Apache-2.0
"""Opt-in example configuration around the shared real endpoint supervisor."""
import json
from pathlib import Path
import secrets

import device_entrypoint

TOOLS = {'hotfolder.list', 'hotfolder.read_text', 'hotfolder.write_text', 'client.read_log_entry'}


def configure(path):
    settings = json.loads(path.read_text())
    state = Path(settings['stateDirectory'])
    folder = state / 'hotfolder'
    folder.mkdir(mode=0o700, exist_ok=True)
    # Seed only missing, fictional inputs. Existing inputs and generated reports are preserved.
    for fixture in Path('/opt/toolgate/fixtures').iterdir():
        target = folder / fixture.name
        if not target.exists():
            with target.open('x', encoding='utf-8') as stream:
                stream.write(fixture.read_text(encoding='utf-8'))
            target.chmod(0o600)
    profiles = json.loads(device_entrypoint.cli('authorization-profiles'))
    selected = [item for item in profiles if item['tool']['id'] in TOOLS]
    if {item['tool']['id'] for item in selected} != TOOLS:
        raise ValueError('Published Linux client is missing the example tools')
    # The service constructor requires a private token file. This inert value grants
    # no local IPC rights. Remote tasks authorize through enrolled device mTLS and permits.
    token = state / 'local-ipc-disabled-token'
    if not token.exists():
        with token.open('x', encoding='utf-8') as stream:
            stream.write(secrets.token_urlsafe(48))
        token.chmod(0o600)
    settings['tools'] = dict(hotfolder=dict(root=str(folder), maxFileBytes=65536,
        maxEntries=256, extensions=['txt', 'md', 'json', 'csv']),
        gatewayUrl=settings['serverUrl'], gatewayTokenPath=str(token),
        gatewayCaPath=settings['caCertificatePath'], deviceId='remote',
        authorizationProfiles=selected, webSearchTokenPath=None, webSearchAllowedDomains=[])
    path.write_text(json.dumps(settings), encoding='utf-8')


if __name__ == '__main__':
    device_entrypoint.main(configure=configure)
