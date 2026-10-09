# Copyright 2026 OLO Labs
# SPDX-License-Identifier: Apache-2.0
"""Scan exact local runtime images and preserve vulnerability reports and SBOMs."""
import argparse
import json
from pathlib import Path
import subprocess

ROOT = Path(__file__).resolve().parents[2]


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('images', nargs='+')
    args = parser.parse_args()
    evidence = ROOT / 'build/enterprise/images'
    evidence.mkdir(parents=True, exist_ok=True)
    results = []
    for image in args.images:
        info = json.loads(subprocess.check_output(['docker', 'image', 'inspect', image]))[0]
        name = image.replace(':', '-').replace('/', '-')
        prefix = ['docker', 'run', '--rm', '-v', '/var/run/docker.sock:/var/run/docker.sock',
                  '-v', str(evidence) + ':/evidence', 'aquasec/trivy:0.61.1', 'image', '--no-progress']
        scan = subprocess.run(prefix + ['--scanners', 'vuln', '--exit-code', '1', '--severity',
            'HIGH,CRITICAL', '--format', 'json', '--output', '/evidence/' + name + '.json', image])
        subprocess.run(prefix + ['--format', 'cyclonedx', '--output',
                                 '/evidence/' + name + '.cdx.json', image], check=True)
        results.append(dict(image=image, imageId=info['Id'], passed=scan.returncode == 0))
    (evidence / 'report.json').write_text(json.dumps(results, indent=2) + '\n', encoding='utf-8')
    if not all(item['passed'] for item in results):
        raise SystemExit('Runtime image vulnerabilities require review; see build/enterprise/images')


if __name__ == '__main__':
    main()
