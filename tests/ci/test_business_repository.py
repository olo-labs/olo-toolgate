# Copyright 2026 OLO Labs; SPDX-License-Identifier: Apache-2.0
"""Real POSIX directory-descriptor intake boundaries, executed in the processor image."""
from copy import deepcopy
import hashlib
import json
import os
from pathlib import Path
import tempfile
import unittest
from unittest.mock import patch

from bot.business.repository import Repository


@unittest.skipUnless(os.name=='posix' and hasattr(os,'O_NOFOLLOW'),'Run in the Linux processor image')
class BusinessRepositoryTests(unittest.TestCase):
    def setUp(self):
        temporary=tempfile.TemporaryDirectory();self.addCleanup(temporary.cleanup)
        self.root=Path(temporary.name);self.folder=self.root/'sample';self.folder.mkdir()
        self.raw=b'Organization: Fictional company'
        self.manifest=dict(schemaVersion=1,useCase='ocr',request={},documents=[dict(
            id='document',file='source.txt',mediaType='text/plain',bytes=len(self.raw),
            sha256='sha256:'+hashlib.sha256(self.raw).hexdigest())])
        self.write_manifest();(self.folder/'source.txt').write_bytes(self.raw)
        self.repository=Repository(self.root);self.addCleanup(self.repository.close)

    def write_manifest(self,value=None):
        (self.folder/'manifest.json').write_text(json.dumps(value if value is not None else self.manifest),encoding='utf-8')

    def test_only_declared_sources_are_released_and_private_filenames_are_hidden(self):
        value=self.repository.manifest('sample')
        self.assertNotIn('file',value['documents'][0])
        self.assertEqual((self.raw,'text/plain'),self.repository.document('sample','document'))
        (self.folder/'private.txt').write_bytes(b'not declared')
        with self.assertRaises(FileNotFoundError):self.repository.document('sample','private')
        self.assertEqual(self.raw,(self.folder/'source.txt').read_bytes())

    def test_source_bytes_changed_after_manifest_are_refused(self):
        self.repository.manifest('sample')
        (self.folder/'source.txt').write_bytes(b'another source')
        with self.assertRaisesRegex(ValueError,'changed'):self.repository.document('sample','document')

    def test_traversal_and_symlinks_cannot_escape_the_intake(self):
        outside=self.root/'outside';outside.mkdir();(outside/'secret.txt').write_bytes(b'private')
        for name in ('../outside','sample/../outside','/outside','sample%2f..'):
            with self.subTest(name=name),self.assertRaises(ValueError):self.repository.manifest(name)
        (self.root/'linked').symlink_to(outside,target_is_directory=True)
        with self.assertRaises(OSError):self.repository.manifest('linked')
        (self.folder/'source.txt').unlink();(self.folder/'source.txt').symlink_to(outside/'secret.txt')
        with self.assertRaises(OSError):self.repository.document('sample','document')
        (self.folder/'manifest.json').unlink();(self.folder/'manifest.json').symlink_to(outside/'secret.txt')
        with self.assertRaises(OSError):self.repository.manifest('sample')

    def test_open_intake_directory_is_anchored_during_path_replacement(self):
        original=self.repository._manifest
        def replace_after_open(descriptor):
            observed=original(descriptor)
            self.folder.rename(self.root/'opened-intake')
            self.folder.mkdir();(self.folder/'source.txt').write_bytes(b'replacement attacker source')
            return observed
        with patch.object(self.repository,'_manifest',side_effect=replace_after_open):
            self.assertEqual((self.raw,'text/plain'),self.repository.document('sample','document'))

    def test_fifo_is_refused_without_waiting_for_a_writer(self):
        (self.folder/'source.txt').unlink();os.mkfifo(self.folder/'source.txt')
        with self.assertRaisesRegex(ValueError,'Regular source file'):self.repository.document('sample','document')

    def test_current_manifest_types_limits_and_hashes_are_strict(self):
        mutations=[]
        for key,value in [('schemaVersion',True),('schemaVersion',2),('request',{'documents':[]}),('documents',[None])]:
            bad=deepcopy(self.manifest);bad[key]=value;mutations.append(bad)
        for key,value in [('bytes',True),('bytes',2*1024*1024+1),('file','../private'),('sha256','invalid'),('id',12)]:
            bad=deepcopy(self.manifest);bad['documents'][0][key]=value;mutations.append(bad)
        bad=deepcopy(self.manifest);bad['documents']*=2;mutations.append(bad)
        for manifest in mutations:
            with self.subTest(manifest=manifest):
                self.write_manifest(manifest)
                with self.assertRaises(ValueError):self.repository.manifest('sample')
        for raw in ('{"schemaVersion":1,"schemaVersion":1}',
                    '{"schemaVersion":1,"useCase":"ocr","documents":[],"request":{"value":NaN}}'):
            (self.folder/'manifest.json').write_text(raw,encoding='utf-8')
            with self.subTest(raw=raw),self.assertRaises(ValueError):self.repository.manifest('sample')
