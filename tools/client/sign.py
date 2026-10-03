# Copyright 2026 OLO Labs
# SPDX-License-Identifier: Apache-2.0
"""Native signing hook for protected CI. Keys stay in OS/keychain/KMS custody; no local secrets bundled."""
import argparse
import os
import platform
import re
import subprocess
from pathlib import Path

def sign(binary):
    if os.environ.get('GITHUB_ACTIONS')!='true' or not binary.is_file():raise ValueError('Protected native CI and tested binary required')
    system=platform.system()
    if system=='Windows':
        thumbprint=os.environ.get('CLIENT_SIGNING_CERT_THUMBPRINT','')
        tool=Path(os.environ.get('CLIENT_SIGNTOOL_PATH',''))
        if not re.fullmatch('[A-Fa-f0-9]{40}',thumbprint) or not tool.is_absolute() or not tool.is_file():raise ValueError('External certificate-store identity and trusted signtool required')
        commands=[[str(tool),'sign','/sha1',thumbprint,'/fd','SHA256','/tr','https://timestamp.digicert.com','/td','SHA256',str(binary)],[str(tool),'verify','/pa',str(binary)]]
    elif system=='Darwin':
        identity=os.environ.get('CLIENT_SIGNING_IDENTITY','')
        if not identity.startswith('Developer ID Application: ') or len(identity)>256 or any(ord(c)<32 for c in identity):raise ValueError('External Developer ID keychain identity required')
        commands=[['/usr/bin/codesign','--force','--options','runtime','--timestamp','--sign',identity,str(binary)],['/usr/bin/codesign','--verify','--strict',str(binary)]]
    else:
        key=os.environ.get('CLIENT_SIGNING_KEY_REF','')
        if not key.startswith(('gcpkms://','awskms://','azurekms://','hashivault://')):raise ValueError('External KMS reference required')
        commands=[['cosign','sign-blob','--yes','--key',key,'--output-signature',str(binary)+'.sig',str(binary)]]
    for command in commands:subprocess.run(command,check=True,capture_output=True)
    print('Native signing hook completed; private key material remains external')

if __name__=='__main__':
    parser=argparse.ArgumentParser(description=__doc__);parser.add_argument('--binary',required=True,type=Path);sign(parser.parse_args().binary)
