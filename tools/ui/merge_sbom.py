# Copyright 2026 OLO Labs
# SPDX-License-Identifier: Apache-2.0
"""Include embedded JavaScript libraries in the Control image CycloneDX inventory.

Trivy's image scan detects the JAR/OS graph but not libraries inside minified
browser assets. The separately verified production npm graph closes that gap.
"""
import argparse
import json
from pathlib import Path
ROOT=Path(__file__).resolve().parents[2]


def merge(image,ui):
    root=image['metadata']['component']['bom-ref']
    known={component.get('purl'):component['bom-ref'] for component in image['components'] if component.get('purl')}
    refs={ui['metadata']['component']['bom-ref']:root}
    for component in ui['components']:
        purl=component['purl']
        refs[component['bom-ref']]=known.get(purl,'toolgate-ui:'+component['bom-ref'])
        if purl not in known:
            image['components'].append({**component,'bom-ref':refs[component['bom-ref']]})
            known[purl]=refs[component['bom-ref']]
    edges={edge['ref']:edge for edge in image.setdefault('dependencies',[])}
    for edge in ui['dependencies']:
        ref=refs[edge['ref']];children=[refs[child] for child in edge.get('dependsOn',[])]
        if ref in edges:edges[ref]['dependsOn']=sorted(set(edges[ref].get('dependsOn',[])+children))
        else:
            translated={'ref':ref,'dependsOn':children};image['dependencies'].append(translated);edges[ref]=translated
    return image


def main():
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--image-sbom',type=Path,default=ROOT/'build/control/control-sbom.cdx.json')
    target=parser.parse_args().image_sbom
    image=json.loads(target.read_text(encoding='utf-8'));ui=json.loads((ROOT/'build/ui/ui-sbom.cdx.json').read_text(encoding='utf-8'))
    result=merge(image,ui)
    if not any(component.get('purl','').startswith('pkg:npm/react@') for component in result['components']):raise ValueError('Embedded React dependency missing from image SBOM')
    target.write_text(json.dumps(result,indent=2)+'\n',encoding='utf-8')
    print(f'Image SBOM includes embedded UI: {len(result["components"])} components')


if __name__=='__main__':main()
