# Copyright 2026 OLO Labs
# SPDX-License-Identifier: Apache-2.0
"""Independent crypto and canonical builder/API boundary tests."""
import base64
import copy
import hashlib
import json
import sys
import unittest
from pathlib import Path
import yaml
from cryptography.hazmat.primitives import hashes
from cryptography.hazmat.primitives.asymmetric import rsa,padding
from cryptography.exceptions import InvalidSignature
ROOT=Path(__file__).resolve().parents[2]
sys.path.insert(0,str(ROOT/'tools'))
from builder.sign import sign_release
def decode(value):return base64.urlsafe_b64decode(value+'='*(-len(value)%4))
class BuilderContracts(unittest.TestCase):
    def test_lease_signature_and_exact_task_are_independently_verified(self):
        fixture=json.loads((ROOT/'tests/fixtures/builder/v1/signed.json').read_text())
        h,p,s=fixture['valid']['jws'].split('.')
        self.assertEqual(json.loads(decode(h)),{'alg':'RS256','typ':'toolgate-builder-test+jws','kid':'organization'})
        key=fixture['organizationKeys'][0];public=rsa.RSAPublicNumbers(int.from_bytes(decode(key['e']),'big'),int.from_bytes(decode(key['n']),'big')).public_key()
        public.verify(decode(s),(h+'.'+p).encode(),padding.PKCS1v15(),hashes.SHA256())
        self.assertEqual(json.loads(decode(p)),fixture['task'])
        with self.assertRaises(InvalidSignature):public.verify(decode(s),(h+'.'+p+'x').encode(),padding.PKCS1v15(),hashes.SHA256())
    def test_release_signer_preserves_exact_bytes_and_separate_authority(self):
        fixtures=json.loads((ROOT/'tests/fixtures/contracts/v1/valid.json').read_text())
        raw=json.dumps(fixtures['BuilderDraft']['packageDocument'],indent=2).encode()
        key=rsa.generate_private_key(public_exponent=65537,key_size=2048)
        release=sign_release(raw,key,'offline-release');h,p,s=release['release']['jws'].split('.')
        self.assertEqual(decode(p),raw);self.assertEqual(release['manifestDigest'],hashlib.sha256(raw).hexdigest())
        key.public_key().verify(decode(s),(h+'.'+p).encode(),padding.PKCS1v15(),hashes.SHA256())
        self.assertEqual(json.loads(decode(h))['typ'],'toolgate-package-release+jws')
        invalid=copy.deepcopy(fixtures['BuilderDraft']['packageDocument']);invalid['installScript']='untrusted'
        with self.assertRaises(Exception):sign_release(json.dumps(invalid).encode(),key,'offline-release')
        with self.assertRaises(ValueError):sign_release(b'x'*32769,key,'offline-release')
        with self.assertRaises(ValueError):sign_release(b'{"formatVersion":1,"formatVersion":1}',key,'offline-release')
    def test_builder_routes_require_explicit_admin_or_device_identity(self):
        api=yaml.safe_load((ROOT/'packages/contracts/openapi/control-v1.yaml').read_text())
        count=0
        for path,methods in api['paths'].items():
            if '/builder/' not in path:continue
            for operation in methods.values():
                count+=1;self.assertEqual(operation['security'],[{'deviceCertificate' if path.endswith(('tests/poll','tests/results')) else 'adminAccessToken':[]}])
                self.assertTrue('builder.schema.json' in json.dumps(operation) or 'fleet.schema.json' in json.dumps(operation))
        self.assertEqual(count,10)
if __name__=='__main__':unittest.main()
