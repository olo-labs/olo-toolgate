# Copyright 2026 OLO Labs
# SPDX-License-Identifier: Apache-2.0
"""Validate contributor input and the existing release-document integration."""
from pathlib import Path
import shutil
import subprocess
import sys
import tempfile
import unittest
from unittest.mock import patch

import yaml
from tools.contracts import version
from tools.release import notes, prepare

ROOT = Path(__file__).resolve().parents[2]


class ReleaseFragmentTests(unittest.TestCase):
    def setUp(self):
        temporary = tempfile.TemporaryDirectory()
        self.addCleanup(temporary.cleanup)
        self.root = Path(temporary.name)
        self.folder = self.root/'.changes/v0.1.0-beta.1'
        self.folder.mkdir(parents=True)
        shutil.copyfile(ROOT/'.changes/schema.json', self.root/'.changes/schema.json')
        self.fragment = yaml.safe_load((ROOT/'.changes/examples/pr-123-release-note-fragments.yaml').read_text())
        self.path = self.folder/'pr-123-release-notes.yaml'
        self.write()

    def write(self, data=None, path=None):
        (path or self.path).write_text(yaml.safe_dump(self.fragment if data is None else data), encoding='utf-8')

    def compile(self):
        return notes.compile_notes('v0.1.0-beta.1', self.root)

    def test_example_and_repository_validate(self):
        notes.validate_all(ROOT)
        self.assertIn('Author: Rahul Nigam', self.compile())
        self.assertEqual(self.compile(), notes.compile_notes('0.1.0-beta.1', self.root))
        del self.fragment['component']
        self.write()
        self.compile()

    def test_schema_rejects_missing_extra_and_incorrect_types(self):
        for field in ('author', 'category', 'summary', 'details', 'breaking', 'references'):
            with self.subTest(missing=field):
                data = self.fragment.copy()
                del data[field]
                self.write(data)
                with self.assertRaises(ValueError): self.compile()
        for field, value in [('extra', 'typo'), ('category', 'feature'), ('author', ' '),
                             ('summary', ' '), ('summary', 'two\nlines'), ('summary', 'trailing\n'), ('summary', 'x'*201),
                             ('details', ''), ('breaking', 'false'), ('references', []),
                             ('references', ['#123', '#123']), ('references', [123]),
                             ('references', ['http://example.com']), ('component', 'Upper Case')]:
            with self.subTest(field=field, value=value):
                self.write({**self.fragment, field: value})
                with self.assertRaisesRegex(ValueError, 'pr-123-release-notes.yaml'): self.compile()

    def test_rejects_ambiguous_or_unsafe_yaml(self):
        for text in ('author: Rahul\nauthor: Someone else\n', '[]', 'null', 'summary: [',
                     '!!python/object/apply:os.system [echo unsafe]',
                     'author: Rahul\n---\nauthor: Other\n'):
            with self.subTest(text=text):
                self.path.write_text(text)
                with self.assertRaises(ValueError): self.compile()

    def test_invalid_filename_duplicate_id_and_nested_directory(self):
        for name in ('Rahul Nigam.yaml', '1-rahul.yml', 'pr-0-change.yaml', 'pr-01-change.yaml',
                     'pr-123-another.yml', 'pr-999-UPPER.yaml', 'notes.json'):
            with self.subTest(name=name):
                path = self.folder/name
                self.write(path=path)
                with self.assertRaises(ValueError): self.compile()
                path.unlink()
        nested = self.folder/'nested'
        nested.mkdir()
        with self.assertRaises(ValueError): self.compile()

    def test_missing_empty_and_path_traversal_versions_fail(self):
        for invalid in ('../v0.1.0-beta.1', 'v01.0.0', 'v1.0.0+metadata', 'v1.0.0-beta.01'):
            with self.subTest(version=invalid), self.assertRaises(ValueError):
                notes.compile_notes(invalid, self.root)
        with self.assertRaisesRegex(ValueError, 'Missing'): notes.compile_notes('9.0.0', self.root)
        self.path.unlink()
        (self.folder/'.gitkeep').touch()
        notes.validate_all(self.root)
        with self.assertRaisesRegex(ValueError, 'No release-note fragments'): self.compile()

    def test_deterministic_order_breaking_and_version_isolation(self):
        self.write({**self.fragment, 'category': 'fixed', 'breaking': True})
        self.write({**self.fragment, 'summary': 'Earlier category.'}, self.folder/'issue-5-category.yml')
        self.write({**self.fragment, 'summary': 'Lexically first.'}, self.folder/'pr-10-first.yaml')
        other = self.root/'.changes/v9.0.0'
        other.mkdir()
        (other/'invalid.txt').touch()
        first = self.compile()
        self.assertEqual(first.encode('utf-8'), self.compile().encode('utf-8'))
        self.assertLess(first.index('### Added'), first.index('### Fixed'))
        self.assertLess(first.index('Earlier category.'), first.index('Lexically first.'))
        self.assertIn('**BREAKING** `release`:', first)
        self.assertIn('References: #123', first)
        self.assertNotIn('v9.0.0', first)
        with self.assertRaises(ValueError): notes.validate_all(self.root)

    def test_changelog_update_preserves_history_and_is_idempotent(self):
        changelog = self.root/'CHANGELOG.md'
        changelog.write_text('# Changelog\n\nExisting history.\n')
        notes.update_changelog('0.1.0-beta.1', self.root)
        first = changelog.read_bytes()
        notes.update_changelog('0.1.0-beta.1', self.root)
        self.assertEqual(first, changelog.read_bytes())
        self.assertTrue(first.endswith(b'# Changelog\n\nExisting history.\n'))
        self.fragment['summary'] = 'Updated contribution.'
        self.write()
        notes.update_changelog('0.1.0-beta.1', self.root)
        self.assertEqual(changelog.read_text().count('## v0.1.0-beta.1'), 1)
        self.assertIn('Updated contribution.', changelog.read_text())
        notes.update_changelog('9.0.0', self.root)
        self.assertIn('Updated contribution.', changelog.read_text())

    def test_prepare_compiles_before_existing_generators_and_failures_stop_stamping(self):
        (self.root/'CHANGELOG.md').write_text('History.\n')
        info = {'version': '0.1.0-beta.1'}
        with patch.object(prepare.identity, 'validate'), patch.object(prepare, 'stamp_versions') as stamp:
            stamp.side_effect = lambda root, _: self.assertIn('Rahul Nigam', (root/'CHANGELOG.md').read_text())
            prepare.stamp(self.root, info)
            stamp.assert_called_once()
            stamp.reset_mock()
            self.write({**self.fragment, 'breaking': 'yes'})
            with self.assertRaises(ValueError): prepare.stamp(self.root, info)
            stamp.assert_not_called()

    def test_malformed_changelog_markers_stop_before_writing(self):
        path = self.root/'CHANGELOG.md'
        text = '<!-- release-fragments:v0.1.0-beta.1 -->\nHistory.\n'
        path.write_text(text)
        with self.assertRaisesRegex(ValueError, 'Malformed'): notes.update_changelog('0.1.0-beta.1', self.root)
        self.assertEqual(path.read_text(), text)

    def test_symlinked_fragment_is_rejected(self):
        path = self.folder/'pr-456-link.yaml'
        try:
            path.symlink_to(self.path)
        except OSError:
            self.skipTest('Creating symlinks requires privileges on this platform')
        with self.assertRaises(ValueError): self.compile()

    def test_compiled_changelog_flows_through_canonical_document_generator(self):
        # expected() reads the real source metadata but substitutes this changelog.
        compiled = self.compile()
        read_text = Path.read_text
        def read(path, *args, **kwargs):
            return compiled if path == ROOT/'CHANGELOG.md' else read_text(path, *args, **kwargs)
        with patch.object(Path, 'read_text', read):
            document = version.expected()[ROOT/'RELEASE-NOTES.md']
        self.assertIn('Rahul Nigam', document)
        self.assertIn(compiled, document)

    def test_cli_invalid_version_does_not_create_output(self):
        output = self.root/'notes.md'
        result = subprocess.run([sys.executable, str(ROOT/'tools/release/notes.py'), 'compile',
                                 '--version', '../../escape', '--output', str(output)], capture_output=True, text=True)
        self.assertNotEqual(result.returncode, 0)
        self.assertFalse(output.exists())
        self.assertIn('Version must be SemVer', result.stderr)


if __name__ == '__main__':
    unittest.main()
