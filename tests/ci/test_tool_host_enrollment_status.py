# Copyright 2026 OLO Labs; SPDX-License-Identifier: Apache-2.0
import hashlib
import importlib.util
import json
import os
from pathlib import Path
import tempfile
import unittest
from cryptography.hazmat.primitives import serialization
from cryptography.hazmat.primitives.asymmetric import ec

ROOT = Path(__file__).resolve().parents[2]
spec = importlib.util.spec_from_file_location('enrollment_status', ROOT/'apps/tool-host/packaging/runtime-container/enrollment-status.py')
helper = importlib.util.module_from_spec(spec)
spec.loader.exec_module(helper)


@unittest.skipUnless(os.name == 'posix', 'Linux runtime helper uses the packaged OpenSSL executable')
class EnrollmentStatusTest(unittest.TestCase):
    def test_status_exposes_only_public_evidence_and_refuses_foreign_or_unsupported_state(self):
        key = ec.generate_private_key(ec.SECP256R1())
        private = key.private_bytes(serialization.Encoding.PEM, serialization.PrivateFormat.PKCS8, serialization.NoEncryption())
        fingerprint = hashlib.sha256(key.public_key().public_bytes(serialization.Encoding.DER, serialization.PublicFormat.SubjectPublicKeyInfo)).hexdigest()
        config = dict(hostId='host',control=dict(tenantId='tenant-a',url='https://control.test'))
        with tempfile.TemporaryDirectory() as name:
            directory = Path(name)
            (directory/'device-key.pem').write_bytes(private)
            state = dict(formatVersion=3,tenantId='tenant-a',serverUrl='https://control.test',challenge=dict(userCode='TEST-CODE'))
            journal = directory/'device.json'; journal.write_text(json.dumps(state))
            self.assertEqual(dict(hostId='host',tenantId='tenant-a',userCode='TEST-CODE',keyFingerprint=fingerprint),helper.status(directory,config))
            for field,value in (('formatVersion',2),('tenantId','other'),('serverUrl','https://other.test')):
                invalid = dict(state); invalid[field] = value; journal.write_text(json.dumps(invalid)); before = journal.read_bytes()
                with self.assertRaises(SystemExit): helper.status(directory,config)
                self.assertEqual(before,journal.read_bytes())
                self.assertEqual(private,(directory/'device-key.pem').read_bytes())
