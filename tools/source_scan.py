# Copyright 2026 OLO Labs
# SPDX-License-Identifier: Apache-2.0
"""Scan all release source and untracked contributor source in an isolated Linux snapshot.

Git's source inventory excludes ignored dependency caches/build outputs. Actual
runtime artifacts have separate image/SBOM scans. Every selected working file is
copied, including modifications; no source paths or license findings are waived.
"""
import subprocess
import tarfile
import tempfile
from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]


def scan():
    names=subprocess.check_output(['git','ls-files','--cached','--others','--exclude-standard','-z'],cwd=ROOT).decode('utf-8').split('\0')
    with tempfile.TemporaryDirectory(prefix='source-scan-',dir=ROOT/'.dev') as temporary:
        archive=Path(temporary)/'source.tar'
        count=0
        with tarfile.open(archive,'w') as output:
            for name in sorted(set(names)-{''}):
                path=ROOT/name
                if not path.exists():continue  # Working-tree deletions are absent from the release.
                if not path.resolve().is_relative_to(ROOT.resolve()) or not path.is_file():
                    raise ValueError('Source snapshot path must be a workspace file')
                # Preserve source bytes but never produce symlink/traversal entries.
                with path.open('rb') as content:
                    entry=tarfile.TarInfo(Path(name).as_posix());entry.size=path.stat().st_size
                    entry.mode=0o644;entry.mtime=0;output.addfile(entry,content)
                count+=1
        print(f'Scanning {count} working source files; Linux snapshot avoids slow Windows mount traversal',flush=True)
        command='mkdir /source && tar -xf /source.tar -C /source && exec trivy fs --no-progress --timeout 15m --include-dev-deps --scanners vuln,license --license-full --exit-code 1 --severity HIGH,CRITICAL /source'
        subprocess.run(['docker','run','--rm','-v',f'{archive.as_posix()}:/source.tar:ro','--entrypoint','sh','aquasec/trivy:0.61.1','-c',command],cwd=ROOT,check=True)


if __name__=='__main__':scan()
