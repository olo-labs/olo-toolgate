# Copyright 2026 OLO Labs
# SPDX-License-Identifier: Apache-2.0
"""Group-only assignment and complete typed entitlement boundaries."""
import copy,json,unittest
from jsonschema import ValidationError
from test_foundation import ROOT,schemas_at,validators
class ManagedRoleContracts(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.models=validators(schemas_at(ROOT/'packages/contracts/schemas/v1'))
        cls.fixture=json.loads((ROOT/'tests/fixtures/contracts/v1/valid.json').read_text(encoding='utf-8'))
    def test_individual_records_reject_direct_access_assignments(self):
        for model,fields in {'ControlUser':['access','roleIds','deviceGroupIds'],'ControlAgent':['allowedToolIds','roleIds'],'ControlDevice':['access','roleIds'],'ControlTool':['deviceGroupId','access','userIds']}.items():
            for field in fields:
                with self.subTest(model=model,field=field),self.assertRaises(ValidationError):self.models[model].validate({**self.fixture[model],field:[]})
    def test_role_rules_are_typed_and_group_scoped(self):
        self.models['ControlRole'].validate(self.fixture['ControlRole'])
        self.assertNotIn('RoleRules',self.models)
        for field,value in [('roleType','ADMIN'),('rules',{}),('userIds',['user'])]:
            with self.assertRaises(ValidationError):self.models['ControlRole'].validate({**self.fixture['ControlRole'],field:value})
        rule=copy.deepcopy(self.fixture['EnterpriseManagementRule'])
        for field,value in [('groupType','USER'),('groups',{'ids':[]}),('script','run')]:
            with self.assertRaises(ValidationError):self.models['EnterpriseManagementRule'].validate({**rule,field:value})
    def test_snapshot_requires_every_group_and_entitlement_collection(self):
        snapshot=copy.deepcopy(self.fixture['ControlSnapshot']);self.models['ControlSnapshot'].validate(snapshot)
        for field in ['teams','agentGroups','toolGroups','deviceGroups','grants','roles','delegations','agentDelegations','bindings','extractors','workloadBindings','identityBindings','deviceEvidence']:
            invalid=copy.deepcopy(snapshot);invalid.pop(field)
            with self.subTest(field=field),self.assertRaises(ValidationError):self.models['ControlSnapshot'].validate(invalid)
        with self.assertRaises(ValidationError):self.models['ControlSnapshot'].validate({**snapshot,'formatVersion':1})
    def test_resource_wildcard_and_empty_selection_are_explicit(self):
        self.models['GroupSelection'].validate({'ids':[],'all':False})
        with self.assertRaises(ValidationError):self.models['GroupSelection'].validate({'ids':[]})
        for name in ['EnterpriseScope','ControlAccessGrant','ControlDelegation','ControlExecutionBinding','InstalledAuthorizationProfile','EnterprisePermitConsumption']:
            self.models[name].validate(self.fixture[name])
            with self.assertRaises(ValidationError):self.models[name].validate({**self.fixture[name],'individualOverride':'user'})
if __name__=='__main__':unittest.main()
