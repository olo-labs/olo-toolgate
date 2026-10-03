# Copyright 2026 OLO Labs
# SPDX-License-Identifier: Apache-2.0
"""Extract exactly one verified client executable; never perform general archive extraction."""
import argparse
import hashlib
import json
import tarfile
import zipfile
from pathlib import Path
from package import ROOT, TARGETS, CROSS_TARGETS, verify_binary

def extract(source,target,output):
    if target not in TARGETS | CROSS_TARGETS:raise ValueError('Unsupported native target')
    manifest=json.loads((source/'manifest.json').read_text());artifact=next(a for a in manifest['artifacts'] if a['target']==target)
    expected=f'olo-toolgate-client-{(ROOT/"VERSION").read_text().strip()}-{target}.{"zip" if "windows" in target else "tar.gz"}'
    if artifact['filename']!=expected or not 0<artifact['bytes']<=104857600:raise ValueError('Invalid published artifact identity')
    path=source/artifact['filename'];data=path.read_bytes()
    if len(data)!=artifact['bytes'] or hashlib.sha256(data).hexdigest()!=artifact['sha256']:raise ValueError('Archive checksum mismatch')
    name='olo-toolgate-client.exe' if 'windows' in target else 'olo-toolgate-client'
    if 'windows' in target:
        with zipfile.ZipFile(path) as archive:raw=archive.read(name)
    else:
        with tarfile.open(path) as archive:
            entry=archive.getmember(name)
            if not entry.isfile():raise ValueError('Regular executable required')
            raw=archive.extractfile(entry).read()
    verify_binary(raw,target)
    output.parent.mkdir(parents=True,exist_ok=True);output.write_bytes(raw);output.chmod(0o755)
    print('Verified native executable extracted for integration tests')

if __name__=='__main__':
    parser=argparse.ArgumentParser(description=__doc__);parser.add_argument('--source',type=Path,default=ROOT/'deploy/client-assets/release');parser.add_argument('--target',required=True);parser.add_argument('--output',required=True,type=Path)
    args=parser.parse_args();extract(args.source,args.target,args.output)
