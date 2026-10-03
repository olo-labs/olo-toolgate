# Copyright 2026 OLO Labs
# SPDX-License-Identifier: Apache-2.0
"""Deterministic native release skeleton; consumes an already tested/signed binary."""
import argparse
import hashlib
import io
import json
import subprocess
import tarfile
import zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
TARGETS = {
    'x86_64-unknown-linux-gnu', 'aarch64-unknown-linux-gnu',
    'x86_64-pc-windows-msvc', 'aarch64-pc-windows-msvc',
    'x86_64-apple-darwin', 'aarch64-apple-darwin',
}

def package(binary, target, output):
    if target not in TARGETS or not binary.is_file(): raise ValueError('Tested native binary and supported target required')
    version = (ROOT/'VERSION').read_text().strip()
    metadata = json.loads(subprocess.check_output(['cargo','metadata','--locked','--format-version','1'], cwd=ROOT))
    nodes = {n['id']: n for n in metadata['resolve']['nodes']}
    root = next(p['id'] for p in metadata['packages'] if p['name'] == 'olo-toolgate-client')
    reachable, pending = set(), [root]
    while pending:
        node = pending.pop()
        if node not in reachable:
            reachable.add(node); pending.extend(nodes[node]['dependencies'])
    sbom = {'bomFormat':'CycloneDX','specVersion':'1.6','version':1,'components':[
        {'type':'library','name':p['name'],'version':p['version'],
         'licenses':[{'expression':p['license'].replace('/', ' OR ')}]}
        for p in sorted(metadata['packages'], key=lambda p:p['id']) if p['id'] in reachable
    ]}
    executable = 'olo-toolgate-client.exe' if 'windows' in target else 'olo-toolgate-client'
    files = {executable:binary.read_bytes(), 'LICENSE':(ROOT/'LICENSE').read_bytes(),
             'README.md':(ROOT/'apps/endpoint-client/README.md').read_bytes(),
             'sbom.cdx.json':(json.dumps(sbom,sort_keys=True,indent=2)+'\n').encode()}
    for path in sorted((ROOT/'apps/endpoint-client/packaging').iterdir()): files['packaging/'+path.name] = path.read_bytes()
    output.mkdir(parents=True, exist_ok=True)
    archive = output/f'olo-toolgate-client-{version}-{target}.{"zip" if "windows" in target else "tar.gz"}'
    if 'windows' in target:
        with zipfile.ZipFile(archive,'w',compression=zipfile.ZIP_DEFLATED) as writer:
            for name,data in sorted(files.items()):
                info = zipfile.ZipInfo(name, (1980,1,1,0,0,0)); info.compress_type = zipfile.ZIP_DEFLATED
                info.external_attr = (0o755 if name == executable else 0o644) << 16
                writer.writestr(info,data)
    else:
        import gzip
        with archive.open('wb') as stream, gzip.GzipFile(fileobj=stream,mode='wb',filename='',mtime=0) as compressed, tarfile.open(fileobj=compressed,mode='w') as writer:
            for name,data in sorted(files.items()):
                info = tarfile.TarInfo(name); info.size = len(data); info.mtime = 0; info.mode = 0o755 if name == executable else 0o644
                writer.addfile(info,io.BytesIO(data))
    (output/(archive.name+'.sha256')).write_text(hashlib.sha256(archive.read_bytes()).hexdigest()+'  '+archive.name+'\n')
    print(archive)

if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--binary',type=Path,required=True); parser.add_argument('--target',required=True)
    parser.add_argument('--output',type=Path,default=ROOT/'build/client/release')
    args = parser.parse_args(); package(args.binary,args.target,args.output)
