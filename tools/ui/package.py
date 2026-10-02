# Copyright 2026 OLO Labs
# SPDX-License-Identifier: Apache-2.0
"""Reproducible UI asset bundle; no credentials, source maps or test artifacts."""
import hashlib
import io
import json
import sys
import tarfile
import os
import subprocess
from pathlib import Path
sys.path.insert(0,str(Path(__file__).resolve().parents[1]))
from release.bundle import checksum_assets
ROOT=Path(__file__).resolve().parents[2]


def package_bytes():
    dist=ROOT/'apps/admin-ui/dist'; metadata=json.loads((dist/'release.json').read_text())
    if metadata['version'] != (ROOT/'VERSION').read_text().strip(): raise ValueError('UI artifact version mismatch')
    if metadata['contracts'] != (ROOT/'packages/contracts/VERSION').read_text().strip(): raise ValueError('UI contracts version mismatch')
    for asset in metadata['assets']:
        path=dist/asset['file']
        if path.parent != dist/'assets' or hashlib.sha256(path.read_bytes()).hexdigest() != asset['sha256']: raise ValueError('UI asset hash mismatch')
    output=io.BytesIO()
    with tarfile.open(fileobj=output,mode='w',format=tarfile.USTAR_FORMAT) as archive:
        for path in sorted(dist.rglob('*')):
            if not path.is_file() or '.vite' in path.parts: continue
            if path.suffix == '.map': raise ValueError('Source maps must not enter production assets')
            content=path.read_bytes();info=tarfile.TarInfo(path.relative_to(dist).as_posix());info.size=len(content);info.mode=0o644;info.mtime=0
            archive.addfile(info,io.BytesIO(content))
    return output.getvalue()


def main():
    output=ROOT/'build/ui';output.mkdir(parents=True,exist_ok=True)
    version=(ROOT/'VERSION').read_text().strip(); content=package_bytes()
    if content != package_bytes(): raise ValueError('UI bundle is not reproducible')
    (output/f'olo-toolgate-admin-ui-{version}.tar').write_bytes(content)
    npm='npm.cmd' if os.name=='nt' else 'npm'
    # npm's workspace SBOM omit selector can miss hoisted runtime nodes. Use
    # its full component metadata, then select the actual production graph from
    # npm ls and fail if even one resolved runtime dependency is absent.
    document=json.loads(subprocess.run([npm,'sbom','--sbom-format=cyclonedx'],cwd=ROOT,capture_output=True,check=True).stdout)
    graph=json.loads(subprocess.run([npm,'ls','--workspace','@olo-labs/toolgate-admin-ui','--omit=dev','--all','--json'],cwd=ROOT,capture_output=True,check=True).stdout)
    selected={document['metadata']['component']['bom-ref']}
    def visit(node):
        for name,dependency in node.get('dependencies',{}).items():
            selected.add(name+'@'+dependency['version']);visit(dependency)
    visit(graph)
    document['components']=[component for component in document['components'] if component['bom-ref'] in selected]
    present={component['bom-ref'] for component in document['components']}|{document['metadata']['component']['bom-ref']}
    if selected-present:raise ValueError('Production dependencies missing from npm SBOM: '+', '.join(sorted(selected-present)))
    document['dependencies']=[{**dependency,'dependsOn':[ref for ref in dependency.get('dependsOn',[]) if ref in selected]} for dependency in document['dependencies'] if dependency['ref'] in selected]
    if not any(component.get('name')=='react' for component in document.get('components',[])): raise ValueError('Production UI dependencies missing from SBOM')
    (output/'ui-sbom.cdx.json').write_text(json.dumps(document,indent=2)+'\n',encoding='utf-8')
    checksum_assets(output);print('Reproducible UI production bundle and checksums prepared')


if __name__=='__main__':main()
