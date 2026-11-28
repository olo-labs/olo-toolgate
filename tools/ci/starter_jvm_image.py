# Copyright 2026 OLO Labs; SPDX-License-Identifier: Apache-2.0
"""Prepare one OCI JVM worker for CI packaging; this is not runtime qualification."""
import argparse
import hashlib
import json
from pathlib import Path
import re
import subprocess
import tarfile
import uuid

ROOT = Path(__file__).resolve().parents[2]
SOURCES = ('apps/tool-host/packaging/jvm/Containerfile',
           'apps/tool-host/packaging/jvm/ToolGateJvmProbe.java', 'LICENSE', 'NOTICE.md')
ARCHIVE = 'jvm-worker.oci.tar'


def digest(path):
    with path.open('rb') as stream:
        return 'sha256:' + hashlib.file_digest(stream, 'sha256').hexdigest()


def source_identity():
    return {name: digest(ROOT / name) for name in SOURCES}


def revision():
    return subprocess.check_output(['git', 'rev-parse', 'HEAD'], cwd=ROOT, text=True).strip()


def image_digest(path):
    """Read the single platform manifest, never confuse an index/config ID with it."""
    with tarfile.open(path, 'r:') as archive:
        names = archive.getnames()
        if len(names) != len(set(names)):
            raise ValueError('Duplicate OCI archive entries')

        def read(name):
            member = archive.getmember(name)
            if not member.isfile() or member.size > 1024 * 1024:
                raise ValueError('Invalid OCI metadata member')
            return archive.extractfile(member).read()

        if json.loads(read('oci-layout')) != {'imageLayoutVersion': '1.0.0'}:
            raise ValueError('Unsupported OCI layout')
        index = json.loads(read('index.json'))
        manifests = index.get('manifests', [])
        if index.get('schemaVersion') != 2 or len(manifests) != 1:
            raise ValueError('One platform image required; disable build attestations')
        descriptor = manifests[0]
        selected = descriptor.get('digest', '')
        if (descriptor.get('mediaType') != 'application/vnd.oci.image.manifest.v1+json'
                or not re.fullmatch(r'sha256:[a-f0-9]{64}', selected)):
            raise ValueError('An OCI image manifest digest is required')
        raw = read('blobs/sha256/' + selected[7:])
        if len(raw) != descriptor.get('size') or 'sha256:' + hashlib.sha256(raw).hexdigest() != selected:
            raise ValueError('OCI manifest bytes differ from their descriptor')
        manifest = json.loads(raw)
        config = manifest['config']['digest']
        if not re.fullmatch(r'sha256:[a-f0-9]{64}', config):
            raise ValueError('Invalid OCI configuration digest')
        raw_config = read('blobs/sha256/' + config[7:])
        if 'sha256:' + hashlib.sha256(raw_config).hexdigest() != config:
            raise ValueError('OCI configuration digest mismatch')
        platform = json.loads(raw_config)
        if platform.get('os') != 'linux' or platform.get('architecture') != 'amd64':
            raise ValueError('The starter worker requires linux/amd64')
        return selected


def verify(directory):
    report = json.loads((directory / 'image.json').read_text(encoding='utf-8'))
    path = directory / ARCHIVE
    if (path.is_symlink() or report.get('schemaVersion') != 1
            or report.get('revision') != revision() or report.get('sources') != source_identity()
            or report.get('archive') != ARCHIVE or report.get('archiveSha256') != digest(path)
            or report.get('imageDigest') != image_digest(path)
            or report.get('qualification') != 'not-qualified'):
        raise ValueError('JVM image artifact differs from the selected source or archive')
    return report['imageDigest']


def build(directory):
    directory.mkdir(parents=True, exist_ok=True)
    before = source_identity()
    commit = revision()
    builder = 'toolgate-starter-' + uuid.uuid4().hex
    subprocess.run(['docker', 'buildx', 'create', '--name', builder, '--driver', 'docker-container'], check=True)
    try:
        subprocess.run(['docker', 'buildx', 'build', '--builder', builder, '--platform', 'linux/amd64',
                        '--provenance=false', '--output', 'type=oci,dest=' + str((directory / ARCHIVE).resolve()),
                        '-f', SOURCES[0], '.'], cwd=ROOT, check=True)
    finally:
        subprocess.run(['docker', 'buildx', 'rm', builder], check=True)
    if before != source_identity() or commit != revision():
        raise ValueError('Worker source changed during image preparation')
    report = dict(schemaVersion=1, revision=commit, sources=before, archive=ARCHIVE,
                  archiveSha256=digest(directory / ARCHIVE), imageDigest=image_digest(directory / ARCHIVE),
                  qualification='not-qualified', scope='CI package input; no containment or execution acceptance')
    (directory / 'image.json').write_text(json.dumps(report, indent=2) + '\n', encoding='utf-8')
    return verify(directory)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('operation', choices=['build', 'verify'])
    parser.add_argument('--directory', type=Path, default=ROOT / 'build/ci/starter-jvm')
    parser.add_argument('--github-env', type=Path)
    args = parser.parse_args()
    selected = build(args.directory) if args.operation == 'build' else verify(args.directory)
    if args.github_env:
        with args.github_env.open('a', encoding='utf-8') as stream:
            stream.write('TOOLGATE_JVM_IMAGE_DIGEST=' + selected + '\n')
    print(selected)


if __name__ == '__main__':
    main()
