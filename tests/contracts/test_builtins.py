# Copyright 2026 OLO Labs
# SPDX-License-Identifier: Apache-2.0
"""Additive protocol and fixed built-in catalog retain the frozen identity boundary."""
import json
import sys
import unittest
from pathlib import Path
from jsonschema import Draft202012Validator, ValidationError
from test_foundation import validators, schemas_at
ROOT=Path(__file__).resolve().parents[2]
sys.path.insert(0,str(ROOT/'tools/client'))
from package import verify_binary

class BuiltinContracts(unittest.TestCase):
    def test_fixed_closed_catalog_has_no_delete_or_generic_execution(self):
        catalog=json.loads((ROOT/'packages/contracts/tools/builtins.json').read_text())['tools']
        expected={'hotfolder.'+name for name in ['list','read_text','write_text','append_text','mkdir','file_info','search_text','watch_events','hash','move','copy','list_events']}
        expected.update(['client.read_log_entry','calculator.evaluate','text.transform','json.validate','hash.sha256','system.info','web.search'])
        self.assertEqual(expected,{tool['toolId'] for tool in catalog})
        for tool in catalog:
            Draft202012Validator.check_schema(tool['inputSchema'])
            self.assertFalse(tool['inputSchema']['additionalProperties'])
            with self.assertRaises(ValidationError):Draft202012Validator(tool['inputSchema']).validate({'command':'arbitrary','path':'../escape'})
    def test_protocol_one_does_not_accept_tools_and_two_is_closed(self):
        models=validators(schemas_at(ROOT/'packages/contracts/schemas/v1'))
        fixtures=json.loads((ROOT/'tests/fixtures/contracts/v1/valid.json').read_text())
        with self.assertRaises(ValidationError):models['ClientIpcRequest'].validate(fixtures['BuiltinIpcRequest'])
        with self.assertRaises(ValidationError):models['BuiltinIpcRequest'].validate(fixtures['ClientIpcRequest'])
        with self.assertRaises(ValidationError):models['BuiltinIpcRequest'].validate({**fixtures['BuiltinIpcRequest'],'token':'cannot-export'})
    def test_executable_names_and_short_fake_magic_are_insufficient(self):
        for target in ['x86_64-unknown-linux-gnu','x86_64-pc-windows-gnu','x86_64-apple-darwin']:
            with self.assertRaises(ValueError):verify_binary(b'fake-binary',target)
            with self.assertRaises(ValueError):verify_binary(b'MZ'+bytes(64),target)
    def test_public_api_is_anonymous_but_directory_api_keeps_security(self):
        import yaml
        document=yaml.safe_load((ROOT/'packages/contracts/openapi/control-v1.yaml').read_text())
        self.assertEqual([],document['paths']['/api/public/v1/clients']['get']['security'])
        self.assertEqual([],document['paths']['/api/public/v1/clients/{filename}']['get']['security'])
        self.assertNotEqual([],document['security'])

if __name__=='__main__':unittest.main()
