# Copyright 2026 OLO Labs
# SPDX-License-Identifier: Apache-2.0
"""Validate immutable client release packages, then assemble a public three-platform manifest."""
import argparse
import hashlib
import json
import shutil
import tarfile
import zipfile
from pathlib import Path
from package import TARGETS, CROSS_TARGETS, ROOT, verify_binary

def manifest(source,output):
    version=(ROOT/'VERSION').read_text().strip();artifacts=[];names=set()
    for target in sorted(TARGETS | CROSS_TARGETS):
        if target=='x86_64-pc-windows-gnu' and (source/f'olo-toolgate-client-{version}-x86_64-pc-windows-msvc.zip').exists():continue
        platform='WINDOWS' if 'windows' in target else 'MACOS' if 'apple' in target else 'LINUX'
        extension='zip' if platform=='WINDOWS' else 'tar.gz'
        name=f'olo-toolgate-client-{version}-{target}.{extension}';path=source/name
        if not path.exists(): continue
        if path.is_symlink() or not path.is_file() or not 0<path.stat().st_size<=104857600:raise ValueError('Invalid archive')
        executable='olo-toolgate-client.exe' if platform=='WINDOWS' else 'olo-toolgate-client'
        if platform=='WINDOWS':
            with zipfile.ZipFile(path) as archive:
                members=archive.namelist();raw=archive.read(executable);sbom=archive.read('sbom.cdx.json')
        else:
            with tarfile.open(path) as archive:
                members=archive.getnames();raw=archive.extractfile(executable).read();sbom=archive.extractfile('sbom.cdx.json').read()
        if len(set(members))!=len(members) or any(n.startswith('/') or '..' in n.split('/') for n in members):raise ValueError('Unsafe archive')
        verify_binary(raw,target)
        if json.loads(sbom)['bomFormat']!='CycloneDX':raise ValueError('Invalid dependency SBOM')
        digest=hashlib.sha256(path.read_bytes()).hexdigest()
        if (source/(name+'.sha256')).read_text().strip()!=digest+'  '+name:raise ValueError('Checksum mismatch')
        artifacts.append(dict(platform=platform,target=target,filename=name,sha256=digest,bytes=path.stat().st_size));names.add(platform)
    if names!={'WINDOWS','MACOS','LINUX'}:raise ValueError('A real Windows, macOS and Linux package is required')
    if len(artifacts)>6:raise ValueError('Choose at most six targets; do not mix two Windows toolchains for one release')
    output.mkdir(parents=True,exist_ok=True)
    for artifact in artifacts:
        for suffix in ('','.sha256'):
            origin=source/(artifact['filename']+suffix);destination=output/origin.name
            if origin.resolve()!=destination.resolve():shutil.copyfile(origin,destination)
    document=dict(version=version,artifacts=artifacts)
    (output/'manifest.json').write_text(json.dumps(document,indent=2)+'\n',encoding='utf-8',newline='\n')
    installers=[]
    for artifact in artifacts:
        extension={'WINDOWS':'setup.exe','MACOS':'dmg','LINUX':'run'}[artifact['platform']]
        name=f"olo-toolgate-client-{version}-{artifact['target']}.{extension}"
        path=source/name
        if not path.exists():continue
        if path.is_symlink() or not path.is_file() or not 0<path.stat().st_size<=104857600:raise ValueError('Invalid installer')
        checksum=hashlib.sha256(path.read_bytes()).hexdigest()
        if path.with_name(name+'.sha256').read_text().strip()!=checksum+'  '+name:raise ValueError('Installer checksum mismatch')
        for suffix in ('','.sha256'):
            origin=source/(name+suffix);destination=output/origin.name
            if origin.resolve()!=destination.resolve():shutil.copyfile(origin,destination)
        installers.append({**artifact,'filename':name,'sha256':checksum,'bytes':path.stat().st_size})
    if installers:
        if len(installers)!=len(artifacts):raise ValueError('All native targets require an installer')
        (output/'installers.json').write_text(json.dumps(dict(version=version,artifacts=installers),indent=2)+'\n',encoding='utf-8',newline='\n')
    print(f'Validated {len(artifacts)} native client packages for public distribution')
    return document

if __name__=='__main__':
    parser=argparse.ArgumentParser(description=__doc__);parser.add_argument('--source',type=Path,default=ROOT/'build/client/release');parser.add_argument('--output',type=Path,default=ROOT/'deploy/client-assets/release')
    arguments=parser.parse_args();manifest(arguments.source,arguments.output)
