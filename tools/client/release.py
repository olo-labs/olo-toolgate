# Copyright 2026 OLO Labs
# SPDX-License-Identifier: Apache-2.0
"""Publish exact verified native archives only from the protected, gated tagged release job."""
import hashlib
import json
import os
import subprocess
from pathlib import Path
from manifest import manifest, ROOT
from publication import public_asset

def main():
    version=(ROOT/'VERSION').read_text().strip();tag='v'+version
    if os.environ.get('GITHUB_ACTIONS')!='true' or os.environ.get('GITHUB_REF')!='refs/tags/'+tag:
        raise ValueError('Protected CI and exact VERSION tag required')
    directory=ROOT/'deploy/client-assets/release'
    expected=json.loads((directory/'manifest.json').read_text())
    actual=manifest(directory,directory)
    if expected!=actual:raise ValueError('Release manifest drift')
    files=[directory/'manifest.json']
    for artifact in actual['artifacts']:
        path=directory/artifact['filename']
        if hashlib.sha256(path.read_bytes()).hexdigest()!=artifact['sha256']:raise ValueError('Release asset drift')
        if artifact['platform']!='WINDOWS':files.extend([path,directory/(path.name+'.sha256')])
    if (directory/'installers.json').exists():
        installers=json.loads((directory/'installers.json').read_text())
        files.append(directory/'installers.json')
        for installer in installers['artifacts']:
            path=directory/installer['filename']
            files.extend([path,directory/(path.name+'.sha256')])
    if not (directory/'installers.json').exists():raise ValueError('Installer release required; archive-only publication is disabled')
    present=subprocess.run(['gh','release','view',tag],capture_output=True).returncode==0
    if not present:
        subprocess.run(['gh','release','create',tag,'--verify-tag',*(['--prerelease'] if '-' in version else []),'--title','OLO ToolGate '+version,
            '--generate-notes','--notes-file',str(ROOT/'RELEASE-NOTES.md')],check=True)
    # Existing immutable asset names are never overwritten.
    subprocess.run(['gh','release','upload',tag,*map(str,filter(public_asset,files))],check=True)

if __name__=='__main__':main()
