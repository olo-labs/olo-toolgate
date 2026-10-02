# Copyright 2026 OLO Labs
# SPDX-License-Identifier: Apache-2.0
"""Synchronize release metadata from the canonical contract and product versions."""
import argparse
import json
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
# Numeric prerelease identifiers follow the same no-leading-zero rule as the core.
# Release versions omit build metadata so every configured registry uses one tag.
NUMERIC = r'(?:0|[1-9][0-9]*)'
PRERELEASE = r'(?:0|[1-9][0-9]*|[0-9]*[A-Za-z-][0-9A-Za-z-]*)'
SEMVER = re.compile(rf'{NUMERIC}\.{NUMERIC}\.{NUMERIC}(?:-{PRERELEASE}(?:\.{PRERELEASE})*)?')


def expected():
    contracts = (ROOT/'packages/contracts/VERSION').read_text().strip()
    product = (ROOT/'VERSION').read_text().strip()
    if not SEMVER.fullmatch(contracts) or not SEMVER.fullmatch(product):
        raise ValueError('Release version must be SemVer core/prerelease without build metadata')
    outputs = {}
    manifest = ROOT/'packages/contracts/contract-set.yaml'
    outputs[manifest] = re.sub(r'^version: .*', f'version: {contracts}', manifest.read_text(), flags=re.M)
    cargo = ROOT/'Cargo.toml'
    outputs[cargo] = re.sub(r'^version = "[^"]+"', f'version = "{contracts}"', cargo.read_text(), flags=re.M)
    outputs[cargo] = re.sub(r'(olo-toolgate-contracts = \{[^\n]*version = ")[^"]+', lambda m: m[1]+contracts, outputs[cargo])
    for path in ('package.json','packages/contracts/typescript/package.json'):
        p = ROOT/path
        content = json.loads(p.read_text())
        content['version'] = contracts if 'contracts' in path else product
        outputs[p] = json.dumps(content, indent=2) + '\n'
    chart = ROOT/'deploy/helm/olo-toolgate/Chart.yaml'
    outputs[chart] = re.sub(r'^version: .*', f'version: {product}', chart.read_text(), flags=re.M)
    outputs[chart] = re.sub(r'^appVersion: .*', f'appVersion: "{product}"', outputs[chart], flags=re.M)
    return outputs


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--check', action='store_true')
    parser.add_argument('--set', dest='version')
    args = parser.parse_args()
    if args.version:
        if args.check or not SEMVER.fullmatch(args.version):
            parser.error('--set requires a valid SemVer and cannot accompany --check')
        for path in ('VERSION','packages/contracts/VERSION'):
            (ROOT/path).write_text(args.version+'\n', encoding='utf-8', newline='\n')
    drift = []
    for path, content in expected().items():
        if path.read_bytes() != content.encode('utf-8'):
            drift.append(path.relative_to(ROOT).as_posix())
            if not args.check:
                path.write_text(content, encoding='utf-8', newline='\n')
    if args.check and drift:
        raise SystemExit('Version drift: ' + ', '.join(drift))
    print('Version metadata verified' if args.check else 'Version metadata synchronized; regenerate bindings next')


if __name__ == '__main__':
    main()
