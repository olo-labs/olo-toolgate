# Copyright 2026 OLO Labs; SPDX-License-Identifier: Apache-2.0
import hashlib
import io
import json
from pathlib import Path
import tarfile
import tempfile
import unittest

import yaml
from tools.ci import starter_jvm_image as image


class StarterJvmImageTests(unittest.TestCase):
    def setUp(self):
        self.temporary = tempfile.TemporaryDirectory()
        self.addCleanup(self.temporary.cleanup)
        self.directory = Path(self.temporary.name)

    def fixture(self, architecture='amd64', duplicate=False):
        config = json.dumps(dict(os='linux', architecture=architecture)).encode()
        config_hash = hashlib.sha256(config).hexdigest()
        manifest = json.dumps(dict(schemaVersion=2, config=dict(digest='sha256:' + config_hash), layers=[])).encode()
        manifest_hash = hashlib.sha256(manifest).hexdigest()
        index = dict(schemaVersion=2, manifests=[dict(digest='sha256:' + manifest_hash, size=len(manifest),
                     mediaType='application/vnd.oci.image.manifest.v1+json')])
        entries = {'oci-layout': b'{"imageLayoutVersion":"1.0.0"}',
                   'index.json': json.dumps(index).encode(),
                   'blobs/sha256/' + manifest_hash: manifest, 'blobs/sha256/' + config_hash: config}
        with tarfile.open(self.directory / image.ARCHIVE, 'w:') as archive:
            for name, raw in list(entries.items()) + ([('index.json', entries['index.json'])] if duplicate else []):
                member = tarfile.TarInfo(name);member.size = len(raw)
                archive.addfile(member, io.BytesIO(raw))
        report = dict(schemaVersion=1, revision=image.revision(), sources=image.source_identity(),
                      archive=image.ARCHIVE, archiveSha256=image.digest(self.directory / image.ARCHIVE),
                      imageDigest='sha256:' + manifest_hash, qualification='not-qualified')
        self.write_report(report)
        return report

    def write_report(self, report):
        (self.directory / 'image.json').write_text(json.dumps(report), encoding='utf-8')

    def test_verified_digest_is_the_actual_platform_manifest_not_the_archive_or_config(self):
        report = self.fixture()
        self.assertEqual(report['imageDigest'], image.verify(self.directory))
        self.assertNotEqual(report['archiveSha256'], report['imageDigest'])

    def test_changed_archive_is_rejected_before_exporting_a_digest(self):
        self.fixture()
        with (self.directory / image.ARCHIVE).open('ab') as stream:
            stream.write(b'changed')
        with self.assertRaises(ValueError):image.verify(self.directory)

    def test_another_revision_source_pin_or_qualification_claim_is_rejected(self):
        for field, value in [('revision', '0' * 40), ('sources', {}), ('qualification', 'qualified'),
                             ('imageDigest', 'sha256:' + 'a' * 64), ('archive', '../other.tar')]:
            with self.subTest(field=field):
                report = self.fixture();report[field] = value;self.write_report(report)
                with self.assertRaises(ValueError):image.verify(self.directory)

    def test_wrong_platform_and_duplicate_archive_entries_are_rejected(self):
        for options in [dict(architecture='arm64'), dict(duplicate=True)]:
            with self.subTest(options=options):
                self.fixture(**options)
                with self.assertRaises(ValueError):image.verify(self.directory)

    def test_both_consumers_use_the_same_producing_job_and_archive(self):
        workflow = yaml.safe_load((image.ROOT / '.github/workflows/foundation.yml').read_text())['jobs']
        for name in ('examples', 'core-mcp'):
            with self.subTest(job=name):
                self.assertIn('starter-jvm', workflow[name]['needs'])
                steps = workflow[name]['steps']
                download = next(i for i, step in enumerate(steps)
                                if step.get('with', {}).get('name') == 'foundation-stage-starter-jvm')
                self.assertEqual('build/ci/starter-jvm', steps[download]['with']['path'])
                verify = next(i for i, step in enumerate(steps)
                              if 'starter_jvm_image.py verify --github-env' in step.get('run', ''))
                self.assertLess(download, verify)
        aggregate = workflow['check']['steps']
        packages = next(step for step in aggregate
                        if step.get('with', {}).get('name') == 'foundation-stage-tool-packages')
        self.assertEqual('.', packages['with']['path'])
        self.assertTrue(any(step.get('with', {}).get('name') == 'foundation-stage-starter-jvm'
                            for step in aggregate))


if __name__ == '__main__':unittest.main()
