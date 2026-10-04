# Copyright 2026 OLO Labs
# SPDX-License-Identifier: Apache-2.0
"""Reproduce Actions b532605 stale notes in isolated checkouts, without dependencies."""
import contextlib
import io
from pathlib import Path
import tempfile
import unittest
from unittest.mock import patch
from tools.contracts import version


class ReleaseMetadataRegression(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)
        outputs = version.expected()
        sources = set(outputs) | {version.ROOT/'VERSION',
            version.ROOT/'packages/contracts/VERSION', version.ROOT/'CHANGELOG.md'}
        for source in sources:
            target = self.root/source.relative_to(version.ROOT)
            target.parent.mkdir(parents=True, exist_ok=True)
            target.write_bytes(outputs[source].encode('utf-8') if source in outputs else source.read_bytes())

    def run_version(self, check=True):
        with patch.object(version, 'ROOT', self.root), patch('sys.argv',
                ['version.py', '--check'] if check else ['version.py']), contextlib.redirect_stdout(io.StringIO()):
            version.main()

    def test_changelog_edit_fails_before_regeneration_and_repair_is_deterministic(self):
        self.run_version()
        notes = self.root/'RELEASE-NOTES.md'
        original = notes.read_bytes()
        changelog = self.root/'CHANGELOG.md'
        changelog.write_bytes(changelog.read_bytes()+b'\n- Regression: changed changelog without regenerated notes.\n')
        with self.assertRaisesRegex(SystemExit, r'Version drift: RELEASE-NOTES\.md'):
            self.run_version()
        self.assertEqual(original, notes.read_bytes(), 'Check mode must not repair or hide drift')
        self.run_version(check=False)
        self.run_version()
        repaired = notes.read_bytes()
        self.assertIn(b'Regression: changed changelog', repaired)
        self.run_version(check=False)
        self.assertEqual(repaired, notes.read_bytes())

    def test_missing_notes_and_byte_only_newline_drift_are_rejected(self):
        notes = self.root/'RELEASE-NOTES.md'
        original = notes.read_bytes()
        notes.unlink()
        with self.assertRaisesRegex(SystemExit, 'RELEASE-NOTES.md'):
            self.run_version()
        notes.write_bytes(original.replace(b'\n', b'\r\n'))
        with self.assertRaisesRegex(SystemExit, 'RELEASE-NOTES.md'):
            self.run_version()
        self.run_version(check=False)
        self.assertEqual(original, notes.read_bytes())

    def test_product_version_edit_requires_matching_release_note_identity(self):
        (self.root/'VERSION').write_text('0.10.1-dev\n', encoding='utf-8')
        with self.assertRaisesRegex(SystemExit, 'RELEASE-NOTES.md'):
            self.run_version()
        self.run_version(check=False)
        self.run_version()
        self.assertIn(b'# OLO ToolGate 0.10.1-dev release notes', (self.root/'RELEASE-NOTES.md').read_bytes())


if __name__ == '__main__':
    unittest.main()
