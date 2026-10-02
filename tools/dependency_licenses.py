# Copyright 2026 OLO Labs
# SPDX-License-Identifier: Apache-2.0
"""Audit resolved dependency license metadata across foundation ecosystems."""
import importlib.metadata
import json
import re
import urllib.request
import xml.etree.ElementTree as ET

from license_expression import get_spdx_licensing
from packaging.requirements import Requirement
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
ALLOWED = {'Apache-2.0','MIT','MIT-0','Zlib','BSD-2-Clause','BSD-3-Clause','ISC','0BSD','Unicode-3.0','MPL-2.0','EPL-2.0','Python-2.0','PSF-2.0','Unlicense','CC0-1.0'}
TOOL_ONLY = {'MPL-2.0','EPL-2.0','Python-2.0','PSF-2.0'}
LICENSING = get_spdx_licensing()


def cargo_license(expression):
    # Cargo's historical slash syntax predates SPDX OR. Normalize only the
    # reviewed MIT/Apache pair found in locked dependencies, retaining metadata.
    return {'MIT/Apache-2.0':'MIT OR Apache-2.0',
            'Apache-2.0/MIT':'Apache-2.0 OR MIT'}.get(expression, expression)


def accepted(expression, tooling=False):
    parsed = LICENSING.parse(expression, validate=True, strict=True)
    allowed = ALLOWED if tooling else ALLOWED - TOOL_ONLY
    # WITH is a compound symbol. It requires its own review and cannot inherit
    # approval merely because its underlying license is approved.
    decisions = {symbol: LICENSING.TRUE if getattr(symbol, 'key', str(symbol)) in allowed else LICENSING.FALSE for symbol in parsed.get_symbols()}
    return parsed.subs(decisions).simplify() == LICENSING.TRUE


def python_license(distribution):
    metadata = distribution.metadata
    expression = metadata.get('License-Expression')
    if expression: return expression
    text = metadata.get('License', '').strip()
    if text in ALLOWED: return text
    # Classifiers provide family evidence, not an exact SPDX revision.
    for classifier in metadata.get_all('Classifier', []):
        if 'License' not in classifier: continue
        for label, expression in [('Apache','Apache-2.0'),('MIT License','MIT'),('Mozilla Public License 2.0','MPL-2.0'),('Python Software Foundation','PSF-2.0')]:
            if label in classifier: return expression
        if 'BSD License' in classifier: return 'BSD family (package classifier)'
    raise ValueError(f'License metadata requires review: {distribution.metadata["Name"]}')


def maven_license(group, artifact, version, cache):
    coordinate = f'{group}:{artifact}:{version}'
    if coordinate in cache: return cache[coordinate]
    url = f'https://repo.maven.apache.org/maven2/{group.replace(".","/")}/{artifact}/{version}/{artifact}-{version}.pom'
    with urllib.request.urlopen(url, timeout=30) as response: pom = ET.fromstring(response.read())
    ns = {'m':'http://maven.apache.org/POM/4.0.0'}
    names = [node.text or '' for node in pom.findall('m:licenses/m:license/m:name', ns)]
    result = []
    for name in names:
        lowered = name.lower()
        if 'apache' in lowered and '2' in lowered: result.append('Apache-2.0')
        elif 'eclipse' in lowered and '2' in lowered: result.append('EPL-2.0')
        elif 'mit' in lowered: result.append('MIT')
        else: raise ValueError(f'Maven license requires review: {coordinate}: {name}')
    if not result:
        parent = pom.find('m:parent', ns)
        if parent is None: raise ValueError('No Maven license metadata: '+coordinate)
        result = [maven_license(*(parent.findtext('m:'+k, namespaces=ns) for k in ('groupId','artifactId','version')), cache)]
    cache[coordinate] = ' OR '.join(result)
    return cache[coordinate]


def audit(rust_metadata, output=None):
    inventory = []
    for package in rust_metadata['packages']:
        if package.get('source'):
            declared = package.get('license') or ''
            inventory.append({'ecosystem':'cargo','name':package['name'],'version':package['version'],'license':cargo_license(declared),'declaredLicense':declared})
    npm = json.loads((ROOT/'package-lock.json').read_text())
    for name, package in npm['packages'].items():
        if name.startswith('node_modules/') and not package.get('link'):
            inventory.append({'ecosystem':'npm','name':name.removeprefix('node_modules/'),'version':package['version'],'license':package.get('license') or ''})
    pending = [Requirement(line).name for line in (ROOT/'tools/requirements.txt').read_text().splitlines() if line and not line.startswith('#')]
    visited = set()
    while pending:
        distribution = importlib.metadata.distribution(pending.pop())
        normalized = re.sub(r'[-_.]+', '-', distribution.metadata['Name']).lower()
        if normalized in visited: continue
        visited.add(normalized)
        inventory.append({'ecosystem':'python-tools','name':normalized,'version':distribution.version,'license':python_license(distribution)})
        for spec in distribution.requires or []:
            requirement = Requirement(spec)
            if requirement.marker is None or requirement.marker.evaluate(): pending.append(requirement.name)
        if normalized == 'pip-audit': pending.extend(['filelock','platformdirs'])
    coordinates = set()
    for lock in ROOT.rglob('gradle.lockfile'):
        if any(part in ('.dev','.gradle','node_modules','target') for part in lock.relative_to(ROOT).parts): continue
        for line in lock.read_text().splitlines():
            if line and not line.startswith('#') and line.count(':') == 2:
                coordinates.add(line.split('=')[0])
    cache = {}
    for coordinate in sorted(coordinates):
        group, artifact, version = coordinate.split(':')
        inventory.append({'ecosystem':'maven','name':f'{group}:{artifact}','version':version,'license':maven_license(group,artifact,version,cache)})
    for dependency in inventory:
        expression = dependency['license']
        tooling = dependency['ecosystem'] in ('python-tools','npm') or dependency['name'].startswith(('org.junit','org.opentest4j','org.apiguardian'))
        if expression != 'BSD family (package classifier)' and not accepted(expression, tooling):
            raise ValueError(f'Dependency license rejected: {dependency["name"]}: {expression}')
    inventory.sort(key=lambda d:(d['ecosystem'],d['name']))
    output = output or ROOT/'build/release/dependency-licenses.json'
    output.parent.mkdir(parents=True, exist_ok=True)
    output.write_text(json.dumps({'policy':'Permissive runtime dependencies; MPL/EPL permitted for build/test tooling; BSD classifier evidence retained without inferring a revision','allowedSpdx':sorted(ALLOWED),'dependencies':inventory},indent=2)+'\n',encoding='utf-8',newline='\n')
    print(f'Resolved dependency license audit passed: {len(inventory)} entries; {output}')
