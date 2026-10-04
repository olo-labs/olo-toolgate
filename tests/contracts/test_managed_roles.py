# Copyright 2026 OLO Labs
# SPDX-License-Identifier: Apache-2.0
"""Managed permission data has a closed, bounded canonical wire shape."""
import copy
import json
import unittest
from jsonschema import ValidationError
from test_foundation import ROOT, schemas_at, validators

class ManagedRoleContracts(unittest.TestCase):
    def setUp(self):
        self.models=validators(schemas_at(ROOT/'packages/contracts/schemas/v1'))
        self.fixture=json.loads((ROOT/'tests/fixtures/contracts/v1/valid.json').read_text())

    def test_fixed_templates_and_closed_permission_shape(self):
        rules=copy.deepcopy(self.fixture['RoleRules'])
        self.models['RoleRules'].validate(rules)
        for field,value in [('templateIds',['UNRESTRICTED_EXEC']),('deviceScope','CUSTOM'),('deviceGroupIds',['same','same']),('toolIds',['tool']*129),('script','run arbitrary code')]:
            invalid={**rules,field:value}
            with self.assertRaises(ValidationError):self.models['RoleRules'].validate(invalid)

    def test_role_assignments_and_old_snapshot_compatibility(self):
        user=copy.deepcopy(self.fixture['ControlUser'])
        user['access']['roleIds']=['r']*33
        with self.assertRaises(ValidationError):self.models['ControlUser'].validate(user)
        old=copy.deepcopy(self.fixture['ControlSnapshot']);old.pop('roles')
        self.models['ControlSnapshot'].validate(old)
        for model in ['ControlRole','ControlRolePage','RoleRules','RoleDeviceScope']:
            self.models[model].validate(self.fixture[model])

if __name__=='__main__':unittest.main()
