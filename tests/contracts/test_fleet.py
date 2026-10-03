# Copyright 2026 OLO Labs
# SPDX-License-Identifier: Apache-2.0
"""Public genuine cross-language vectors and additive fleet API/security boundaries."""
import base64
import hashlib
import json
import unittest
from pathlib import Path
import yaml
from cryptography.hazmat.primitives import hashes
from cryptography.hazmat.primitives.asymmetric import padding,rsa
ROOT=Path(__file__).resolve().parents[2]
class FleetContracts(unittest.TestCase):
    def test_release_and_desired_have_independent_genuine_signatures(self):
        f=json.loads((ROOT/'tests/fixtures/fleet/v1/signed.json').read_text())
        def decode(value):return base64.urlsafe_b64decode(value+'='*(-len(value)%4))
        def verify(envelope,keys,typ):
            h,p,s=envelope['jws'].split('.');header=json.loads(decode(h));self.assertEqual(header,{'alg':'RS256','typ':typ,'kid':keys[0]['kid']})
            key=keys[0];public=rsa.RSAPublicNumbers(int.from_bytes(decode(key['e']),'big'),int.from_bytes(decode(key['n']),'big')).public_key()
            public.verify(decode(s),(h+'.'+p).encode(),padding.PKCS1v15(),hashes.SHA256());return decode(p)
        raw=verify(f['release']['release'],f['releaseKeys'],'toolgate-package-release+jws')
        self.assertEqual(hashlib.sha256(raw).hexdigest(),f['release']['manifestDigest']);self.assertEqual(len(raw),f['release']['sizeBytes']);self.assertEqual(json.loads(raw),f['document'])
        desired=json.loads(verify(f['signedDesired'],f['organizationKeys'],'toolgate-fleet-desired+jws'));self.assertEqual(desired,f['desired']);self.assertNotEqual(f['releaseKeys'][0]['n'],f['organizationKeys'][0]['n'])
    def test_api_uses_additive_canonical_models_and_direct_device_identity(self):
        api=yaml.safe_load((ROOT/'packages/contracts/openapi/control-v1.yaml').read_text())
        self.assertEqual(api['components']['securitySchemes']['deviceCertificate']['type'],'mutualTLS')
        for path,methods in api['paths'].items():
            if '/fleet/' not in path:continue
            for operation in methods.values():
                self.assertIn('fleet.schema.json',json.dumps(operation))
                if path.endswith(('desired','artifact-grants','download')):self.assertEqual(operation['security'],[{'deviceCertificate':[]}])
                else:self.assertEqual(operation['security'],[{'adminAccessToken':[]}])
    def test_closed_bounded_descriptors_cannot_contain_install_scripts_or_urls(self):
        schema=json.loads((ROOT/'packages/contracts/schemas/v1/fleet.schema.json').read_text())['$defs']
        for name,value in schema.items():
            if value.get('type')=='object':self.assertFalse(value['additionalProperties'],name)
        package=schema['FleetPackageDocument']['properties']
        self.assertNotIn('installScript',package);self.assertNotIn('artifactUrl',package)
        self.assertEqual(schema['FleetPackageRelease']['properties']['sizeBytes']['maximum'],32768)
        self.assertEqual(schema['FleetDesiredSnapshot']['properties']['assignments']['maxItems'],16)
if __name__=='__main__':unittest.main()
