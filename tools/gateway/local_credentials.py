# Copyright 2026 OLO Labs
# SPDX-License-Identifier: Apache-2.0
"""Generate development credentials under ignored .dev; never print a token."""
import hashlib
import json
import secrets
import time
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]


def prepare(folder=None):
    folder = folder or ROOT/'.dev/gateway'
    folder.mkdir(parents=True, exist_ok=True)
    token = secrets.token_hex(32)
    (folder/'token.secret').write_text(token,encoding='utf-8')
    (folder/'token.secret').chmod(0o600)
    credentials = [{'tokenSha256':hashlib.sha256(token.encode()).hexdigest(),'tenantId':'tenant-demo','userId':'user-demo','agentId':'agent-demo','deviceId':None,'expiresAtUnixMs':int(time.time()*1000)+3600000}]
    (folder/'credentials.json').write_text(json.dumps(credentials)+'\n',encoding='utf-8',newline='\n')
    (folder/'credentials.json').chmod(0o600)
    print('Development credentials generated (one hour); token is in ignored token.secret')
    return token, folder


if __name__ == '__main__': prepare()
