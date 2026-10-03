# Copyright 2026 OLO Labs
# SPDX-License-Identifier: Apache-2.0
"""Approval wire compatibility, explicit trust and published deployment contracts."""
import copy
import base64
import json
import unittest
from pathlib import Path
import yaml
from jsonschema import Draft7Validator, ValidationError
from cryptography.hazmat.primitives import hashes
from cryptography.hazmat.primitives.asymmetric import padding, rsa
from test_foundation import schemas_at, validators, compatible

ROOT=Path(__file__).resolve().parents[2]


class ApprovalContractTests(unittest.TestCase):
    def test_public_format_two_vectors_have_genuine_signatures_and_hashes(self):
        corpus=json.loads((ROOT/'tests/fixtures/approval/signed-v2.json').read_text())
        models=validators(schemas_at(ROOT/'packages/contracts/schemas/v1'))
        key=corpus['keyring']['keys'][0]
        decode=lambda value:base64.urlsafe_b64decode(value+'==')
        public=rsa.RSAPublicNumbers(65537,int.from_bytes(decode(key['modulus']),'big')).public_key()
        import hashlib
        for signed in corpus['bundles'].values():
            parts=signed['jws'].split('.')
            public.verify(decode(parts[2]),(parts[0]+'.'+parts[1]).encode(),padding.PKCS1v15(),hashes.SHA256())
            payload=json.loads(decode(parts[1]));models['ApprovalBundlePayload'].validate(payload)
            policy=decode(payload['policy'])
            self.assertEqual(payload['policySha256'],hashlib.sha256(policy).hexdigest())
            models['ApprovalCompiledPolicy'].validate(json.loads(policy))
    def test_new_types_preserve_frozen_wire_contracts(self):
        current=schemas_at(ROOT/'packages/contracts/schemas/v1')
        for name,old in schemas_at(ROOT/'tests/fixtures/contracts/v1/schemas').items():
            compatible(old,current[name])
        models=validators(current);fixtures=json.loads((ROOT/'tests/fixtures/contracts/v1/valid.json').read_text())
        self.assertEqual('ASK',fixtures['ApprovalBundleRule']['effect'])
        models['ApprovalCompiledPolicy'].validate(fixtures['ApprovalCompiledPolicy'])
        with self.assertRaises(ValidationError):models['CompiledPolicy'].validate(fixtures['ApprovalCompiledPolicy'])
        with self.assertRaises(ValidationError):models['BundlePayload'].validate(fixtures['ApprovalBundlePayload'])
    def test_permit_and_policy_trust_are_not_interchangeable(self):
        models=validators(schemas_at(ROOT/'packages/contracts/schemas/v1'))
        fixtures=json.loads((ROOT/'tests/fixtures/contracts/v1/valid.json').read_text())
        for name,wrong in [('ExecutionPermitHeader','BundleHeader'),('BundleHeader','ExecutionPermitHeader'),
                           ('ExecutionPermitClaims','DeploymentAssignment'),('ApprovalSubmission','RequestContext')]:
            with self.assertRaises(ValidationError):models[name].validate(fixtures[wrong])
        for change in ({'alg':'none'},{'typ':'JWT'},{'jku':'https://evil.invalid/keys'}):
            with self.assertRaises(ValidationError):models['ExecutionPermitHeader'].validate({**fixtures['ExecutionPermitHeader'],**change})
    def test_durations_and_secret_references_are_bounded(self):
        chart=ROOT/'deploy/helm/olo-toolgate';schema=json.loads((chart/'values.schema.json').read_text())
        values=yaml.safe_load((chart/'values.yaml').read_text())
        for component in ('gateway','control'):
            bad=copy.deepcopy(values);bad[component]['approval']['enabled']=True
            with self.assertRaises(ValidationError):Draft7Validator(schema).validate(bad)
        for field,value in [('permitLifetimeMs',10001),('privateKey','forbidden')]:
            bad=copy.deepcopy(values);bad['gateway']['approval'][field]=value
            with self.assertRaises(ValidationError):Draft7Validator(schema).validate(bad)
    def test_versioned_api_reuses_shared_models(self):
        control=yaml.safe_load((ROOT/'packages/contracts/openapi/control-v1.yaml').read_text())
        for path,model in [('resolve','ApprovalSubmission'),('permits/consume','ApprovalPermitUse')]:
            operation=control['paths']['/api/control/v1/approvals/'+path]['post']
            self.assertTrue(operation['requestBody']['content']['application/json']['schema']['$ref'].endswith('/'+model))
        gateway=yaml.safe_load((ROOT/'packages/contracts/openapi/gateway-v1.yaml').read_text())
        self.assertTrue(gateway['paths']['/v2/authorize']['post']['responses']['200']['content']['application/json']['schema']['$ref'].endswith('/AuthorizationOutcome'))
        workflow=yaml.safe_load((ROOT/'.github/workflows/policy.yml').read_text())
        steps=workflow['jobs']['policy-compatibility']['steps']
        self.assertTrue(any('tools/approval/check.py --build' in step.get('run','') for step in steps))


if __name__=='__main__':unittest.main()
